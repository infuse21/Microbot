package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.NpcDialogueTransport;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Resolves the actor, continue, and destination-option stages of a dialogue-menu route. */
public final class Rs2NpcDialogueTransportScene implements NpcDialogueTransportScene
{
	@Override
	public NpcDialogueTransport find(PlannedEdge edge)
	{
		Transport transport = findTransport(edge);
		return transport == null ? null : actorStage(transport);
	}

	@Override
	public NpcDialogueTransport observe(PlannedEdge edge, String pendingAction)
	{
		Transport transport = findTransport(edge);
		if (transport == null)
		{
			return null;
		}
		if (Rs2Dialogue.hasSelectAnOption())
		{
			if (destinationOptionVisible(transport.getDisplayInfo()))
			{
				return stage(transport, transport.getOrigin(),
					NpcDialogueTransport.Stage.DESTINATION);
			}
			// Paid rows confirm the fare through a single affirmative option; a menu that
			// is neither the declared destination nor a lone affirmative is foreign and
			// never receives input.
			if (transport.getCurrencyAmount() > 0 && confirmOptionVisible())
			{
				return stage(transport, transport.getOrigin(),
					NpcDialogueTransport.Stage.CONFIRM);
			}
			return null;
		}
		if (Rs2Dialogue.hasContinue())
		{
			return stage(transport, transport.getOrigin(), NpcDialogueTransport.Stage.CONTINUE);
		}
		if (NpcDialogueTransportPolicy.isStageAction(pendingAction))
		{
			// The dialogue closed after a stage command: the voyage is starting or has
			// started. Preserve the exact landing predicate until the deadline.
			return null;
		}
		return actorStage(transport);
	}

	public static boolean destinationOptionVisible(String destinationOption)
	{
		return matchOptionIndex(optionTexts(), destinationOption) >= 0;
	}

	/** Clicks the declared destination option using the same matching as observation. */
	public static boolean selectDestinationOption(String destinationOption)
	{
		int index = matchOptionIndex(optionTexts(), destinationOption);
		return index >= 0 && Rs2Dialogue.keyPressForDialogueOption(index + 1);
	}

	public static boolean confirmOptionVisible()
	{
		return matchConfirmIndex(optionTexts()) >= 0;
	}

	/** Clicks the single affirmative fare-confirmation option. */
	public static boolean selectConfirmOption()
	{
		int index = matchConfirmIndex(optionTexts());
		return index >= 0 && Rs2Dialogue.keyPressForDialogueOption(index + 1);
	}

	/**
	 * Payment prompts phrase the affirmative differently per captain ({@code Yes please.},
	 * {@code Ok}); the negative always reads as a refusal. Accept exactly one option whose
	 * text starts with an affirmative word, and refuse ambiguity.
	 */
	static int matchConfirmIndex(List<String> options)
	{
		if (options == null)
		{
			return -1;
		}
		int affirmative = -1;
		for (int i = 0; i < options.size(); i++)
		{
			String text = normalizeText(options.get(i));
			if (text.startsWith("yes") || text.startsWith("ok"))
			{
				if (affirmative >= 0)
				{
					return -1;
				}
				affirmative = i;
			}
		}
		return affirmative;
	}

	/**
	 * An exact option always wins, so prefixed destinations such as Molch and Molch
	 * Island cannot be confused. Some NPC menus phrase the option as a sentence
	 * ({@code Can you take me to Port Sarim please?}), so with no exact match a single
	 * option containing the destination is accepted; ambiguous containment is refused.
	 */
	static int matchOptionIndex(List<String> options, String destinationOption)
	{
		String needle = normalizeText(destinationOption);
		if (options == null || needle.isEmpty())
		{
			return -1;
		}
		for (int i = 0; i < options.size(); i++)
		{
			if (normalizeText(options.get(i)).equals(needle))
			{
				return i;
			}
		}
		int containing = -1;
		for (int i = 0; i < options.size(); i++)
		{
			if (normalizeText(options.get(i)).contains(needle))
			{
				if (containing >= 0)
				{
					return -1;
				}
				containing = i;
			}
		}
		return containing;
	}

	private static List<String> optionTexts()
	{
		List<Widget> options = Rs2Dialogue.getDialogueOptions();
		if (options == null)
		{
			return java.util.Collections.emptyList();
		}
		return options.stream()
			.map(option -> option == null ? null : option.getText())
			.collect(Collectors.toList());
	}

	private static String normalizeText(String text)
	{
		return text == null ? "" : Rs2UiHelper.stripTagsToSpace(text).trim()
			.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
	}

	private static Transport findTransport(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(NpcDialogueTransportPolicy::isEligible).findFirst().orElse(null);
	}

	private static NpcDialogueTransport actorStage(Transport transport)
	{
		Rs2NpcModel npc = findActorNpc(transport);
		if (npc != null)
		{
			return stage(transport, npc.getWorldLocation(), NpcDialogueTransport.Stage.ACTOR);
		}
		Rs2TileObjectModel object = findActorObject(transport);
		return object == null ? null
			: stage(transport, object.getWorldLocation(), NpcDialogueTransport.Stage.ACTOR);
	}

	public static Rs2NpcModel findActorNpc(Transport transport)
	{
		if (!NpcDialogueTransportPolicy.isEligible(transport))
		{
			return null;
		}
		return Rs2Npc.getNpcs(npc -> NpcDialogueTransportPolicy.isLiveNpcMatch(transport,
			npc.getName(), actions(npc), npc.getWorldLocation()))
			.min(Comparator
				.comparingInt((Rs2NpcModel npc) -> npc.getId() == transport.getObjectId() ? 0 : 1)
				.thenComparingInt(npc -> npc.getWorldLocation()
					.distanceTo2D(transport.getOrigin())))
			.orElse(null);
	}

	public static Rs2TileObjectModel findActorObject(Transport transport)
	{
		if (!NpcDialogueTransportPolicy.isEligible(transport))
		{
			return null;
		}
		return Microbot.getRs2TileObjectCache().query()
			.within(transport.getOrigin(),
				NpcDialogueTransportPolicy.LIVE_ACTOR_ORIGIN_TOLERANCE)
			.toList().stream()
			.filter(candidate -> NpcDialogueTransportPolicy.isLiveObjectMatch(transport,
				candidate.getId(), compositionName(candidate),
				compositionActions(candidate), candidate.getWorldLocation()))
			.min(Comparator.comparingInt((Rs2TileObjectModel candidate) ->
				(candidate.getId() == transport.getObjectId() ? 0 : 100)
					+ candidate.getWorldLocation().distanceTo2D(transport.getOrigin())))
			.orElse(null);
	}

	public static String resolveLiveObjectAction(Rs2TileObjectModel object, String catalogAction)
	{
		return object == null ? null
			: NpcTransportPolicy.matchAction(compositionActions(object), catalogAction);
	}

	/** Resolves the live NPC action, tolerating destination-named quest-state variants. */
	public static String resolveLiveNpcAction(Rs2NpcModel npc, Transport transport)
	{
		return npc == null || transport == null ? null
			: NpcDialogueTransportPolicy.matchLiveNpcAction(actions(npc),
				transport.getAction(), transport.getDisplayInfo());
	}

	private static List<String> actions(Rs2NpcModel npc)
	{
		return Stream.of(npc.getComposition(), npc.getTransformedComposition())
			.filter(java.util.Objects::nonNull)
			.map(NPCComposition::getActions)
			.filter(java.util.Objects::nonNull)
			.flatMap(Arrays::stream)
			.filter(java.util.Objects::nonNull)
			.collect(Collectors.toList());
	}

	private static String compositionName(Rs2TileObjectModel object)
	{
		ObjectComposition composition = object.getObjectComposition();
		return composition == null ? null : composition.getName();
	}

	private static String[] compositionActions(Rs2TileObjectModel object)
	{
		ObjectComposition composition = object.getObjectComposition();
		return composition == null ? null : composition.getActions();
	}

	private static NpcDialogueTransport stage(Transport transport, WorldPoint tile,
		NpcDialogueTransport.Stage stage)
	{
		return new NpcDialogueTransport(transport.getOrigin(), transport.getDestination(),
			transport.getObjectId(), transport.getName(), transport.getAction(),
			transport.getDisplayInfo(), transport.getCurrencyAmount(), tile, stage);
	}
}
