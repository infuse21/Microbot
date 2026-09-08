package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.ItemID;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.shortestpath.PurchasableItemCatalog;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.policy.TransportRequirementPolicy;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Cache-backed live resolver for direct catalog scene transitions. */
public final class Rs2CatalogTransitionScene implements CatalogTransitionScene
{
	public enum DispatchResult
	{
		REJECTED,
		PREPARED,
		ISSUED
	}

	@Override
	public CatalogTransition find(PlannedEdge edge)
	{
		return observe(edge, null);
	}

	@Override
	public CatalogTransition observe(PlannedEdge edge, String pendingAction)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		for (Transport transport : TransportEdgeMatcher.find(Rs2PathApi.getTransports(),
			edge.from(), edge.to()))
		{
			if (!CatalogTransitionPolicy.isEligible(transport))
			{
				continue;
			}
			CatalogTransition transition = find(transport, pendingAction);
			if (transition != null)
			{
				return transition;
			}
		}
		return null;
	}

	private static CatalogTransition find(Transport transport)
	{
		return find(transport, null);
	}

	private static CatalogTransition find(Transport transport, String pendingAction)
	{
		if (ShantayPassPolicy.isEligible(transport))
		{
			CatalogTransition transition = shantayTransition(transport, pendingAction);
			if (transition == null || ShantayPassPolicy.BUY_PASS_ACTION.equals(transition.getAction()))
			{
				return transition;
			}
		}
		if (ZanarisEntrancePolicy.isEligible(transport))
		{
			String action = zanarisAction(transport, pendingAction);
			if (action == null) return null;
			if (!"Open".equals(action))
			{
				return new CatalogTransition(null, transport.getOrigin(), transport.getObjectId(),
					action, transport.getAction(), transport.getOrigin(), transport.getDestination());
			}
		}
		if (CatalogTransitionPolicy.isShadowDungeonLadder(transport))
		{
			CatalogTransition preparation = Microbot.getClientThread().runOnClientThreadOptional(() ->
				visibilityRingPreparation(transport, hasVisibilityRingEquipped(), inventoryReady())).orElse(null);
			if (preparation != null)
			{
				return preparation;
			}
		}
		boolean pohPortal = CatalogTransitionPolicy.isPohPortal(transport);
		boolean floorboardJump = CatalogTransitionPolicy.isFloorboardJump(transport);
		boolean tarnsJump = CatalogTransitionPolicy.isTarnsJump(transport);
		List<Rs2TileObjectModel> candidates = pohPortal
			? Microbot.getRs2TileObjectCache().query().withId(transport.getObjectId()).toList()
			: Microbot.getRs2TileObjectCache().query().within(transport.getOrigin(),
				floorboardJump ? 5 : ShantayPassPolicy.isEligible(transport) ? 3 : 2).toList();
		Rs2TileObjectModel direct = candidates.stream()
			.filter(candidate -> !(floorboardJump || tarnsJump || CatalogTransitionPolicy.isShortAgilityCrossing(transport)
				|| CatalogTransitionPolicy.isIsafdarCrossing(transport)
				|| CatalogTransitionPolicy.isFremennikSurfaceBridge(transport)
				|| CatalogTransitionPolicy.isAuditedAgilityTraversal(transport)
				|| CatalogTransitionPolicy.isMeiyerditchFloor(transport)
				|| CatalogTransitionPolicy.isMeiyerditchCourseTraversal(transport)
				|| CatalogTransitionPolicy.isMeiyerditchPreparedFloor(transport)
				|| CatalogTransitionPolicy.isMeiyerditchTunnel(transport)
				|| CatalogTransitionPolicy.isMeiyerditchPostQuestAccess(transport)
				|| CatalogTransitionPolicy.isAbyssPassage(transport)
				|| CatalogTransitionPolicy.isRunecraftingExitPortal(transport)
				|| CatalogTransitionPolicy.isEnakhraSecretEntrance(transport)
				|| CatalogTransitionPolicy.isSwanSongHole(transport)
				|| CatalogTransitionPolicy.isMolchLizardTempleTransition(transport)
				|| ZanarisEntrancePolicy.isEligible(transport)
				|| CatalogTransitionPolicy.isWaterfallThroneDoor(transport))
				|| candidate.getId() == transport.getObjectId())
			.filter(candidate -> candidate.getWorldLocation().getPlane()
				== transport.getOrigin().getPlane())
			.filter(candidate -> candidate.getId() == transport.getObjectId()
				|| matchesCatalogIdentity(candidate, transport))
			.min(Comparator.comparingInt(candidate ->
				(candidate.getId() == transport.getObjectId() ? 0 : 100)
					+ candidate.getWorldLocation().distanceTo2D(transport.getOrigin())))
			.orElse(null);
		if (direct != null)
		{
			String action = resolveLiveAction(direct, transport);
			if (action != null)
			{
				return transition(direct, transport, action, pohPortal);
			}
			if (CatalogTransitionPolicy.isKalphiteRopeSetup(transport))
			{
				return transition(direct, transport,
					CatalogTransitionPolicy.ATTACH_ROPE_ACTION, false);
			}
		}
		if (!CatalogTransitionPolicy.supportsClosedVariant(transport.getAction()))
		{
			return null;
		}
		Rs2TileObjectModel closed = candidates.stream()
			.filter(candidate -> isClosedEntrance(candidate, transport.getOrigin()))
			.min(Comparator.comparingInt(candidate ->
				candidate.getWorldLocation().distanceTo2D(transport.getOrigin())))
			.orElse(null);
		return closed == null ? null : transition(closed, transport, "Open", false);
	}

	private static CatalogTransition transition(Rs2TileObjectModel object, Transport transport,
		String action, boolean logicalOrigin)
	{
		WorldPoint objectTile = logicalOrigin ? transport.getOrigin() : object.getWorldLocation();
		return new CatalogTransition(object, objectTile, transport.getObjectId(),
			action, transport.getAction(), transport.getOrigin(), transport.getDestination());
	}

	private static boolean matchesCatalogIdentity(Rs2TileObjectModel object, Transport transport)
	{
		ObjectComposition composition = object.getObjectComposition();
		return composition != null && sameText(composition.getName(), transport.getName())
			&& resolveLiveAction(composition.getActions(), transport) != null;
	}

	/** Dispatches one non-blocking preparation or object command for an exact route edge. */
	public static DispatchResult dispatch(PlannedEdge edge, String action, int catalogObjectId)
	{
		Transport transport = findTransport(edge, action, catalogObjectId);
		if (transport == null)
		{
			return DispatchResult.REJECTED;
		}
		if (ShantayPassPolicy.isEligible(transport))
		{
			CatalogTransition expected = find(transport, action);
			if (expected == null || expected.getCatalogObjectId() != catalogObjectId
				|| !expected.getAction().equalsIgnoreCase(action))
			{
				return DispatchResult.REJECTED;
			}
			if (ShantayPassPolicy.BUY_PASS_ACTION.equalsIgnoreCase(action))
			{
				Rs2NpcModel vendor = shantayVendor(transport);
				return vendor != null && vendor.click(action)
					? DispatchResult.ISSUED : DispatchResult.REJECTED;
			}
			return expected.getObject() != null && expected.getObject().click(action)
				? DispatchResult.ISSUED : DispatchResult.REJECTED;
		}
		if (ZanarisEntrancePolicy.isEligible(transport))
		{
			String expected = zanarisAction(transport, null);
			if (!java.util.Objects.equals(action, expected) || expected == null)
			{
				return DispatchResult.REJECTED;
			}
			if (ZanarisEntrancePolicy.OPEN_INVENTORY.equals(action))
			{
				return Microbot.getClientThread().runOnClientThreadOptional(() ->
				{
					Microbot.getClient().runScript(915, InterfaceTab.INVENTORY.getVarcIntIndex());
					return DispatchResult.ISSUED;
				}).orElse(DispatchResult.REJECTED);
			}
			if (ZanarisEntrancePolicy.WIELD_STAFF.equals(action))
			{
				return Rs2Inventory.interact(staffIds(transport), "Wield")
					? DispatchResult.ISSUED : DispatchResult.REJECTED;
			}
			if (ZanarisEntrancePolicy.SELECT_DESTINATION.equals(action))
			{
				int index = Microbot.getClientThread().runOnClientThreadOptional(() ->
					ZanarisEntrancePolicy.destinationIndex(zanarisOptions())).orElse(-1);
				return index >= 0 && Rs2Dialogue.keyPressForDialogueOption(index + 1)
					? DispatchResult.ISSUED : DispatchResult.REJECTED;
			}
		}
		if (CatalogTransitionPolicy.isShadowDungeonLadder(transport))
		{
			DispatchResult preparation = dispatchVisibilityRing(transport, action);
			if (preparation != null)
			{
				return preparation;
			}
		}
		if (EnergyBarrierPolicy.isEligible(transport)
			&& !TransportRequirementPolicy.itemIdRequirements(transport).isEmpty()
			&& !Rs2Equipment.isWearing(TransportRequirementPolicy.ghostspeakItemIds().stream()
				.mapToInt(Integer::intValue).toArray()))
		{
			return Rs2Inventory.interact(TransportRequirementPolicy.ghostspeakItemIds().stream()
				.mapToInt(Integer::intValue).toArray(), "Wear")
				? DispatchResult.PREPARED : DispatchResult.REJECTED;
		}
		if (requiresRopePreparation(transport, action))
		{
			CatalogTransition transition = find(transport);
			if (transition == null || transition.getCatalogObjectId() != catalogObjectId
				|| !transition.getAction().equalsIgnoreCase(action)
				|| transition.getObject() == null)
			{
				return DispatchResult.REJECTED;
			}
			if (Rs2Inventory.getSelectedItemId() != CatalogTransitionPolicy.ROPE_ITEM_ID)
			{
				return Rs2Inventory.use(CatalogTransitionPolicy.ROPE_ITEM_ID)
					? DispatchResult.PREPARED : DispatchResult.REJECTED;
			}
			return transition.getObject().click("Use")
				? DispatchResult.ISSUED : DispatchResult.REJECTED;
		}
		CatalogTransition transition = find(transport);
		boolean issued = transition != null && transition.getCatalogObjectId() == catalogObjectId
			&& transition.getAction().equalsIgnoreCase(action) && transition.getObject() != null
			&& transition.getObject().click(action);
		return issued ? DispatchResult.ISSUED : DispatchResult.REJECTED;
	}

	private static Transport findTransport(PlannedEdge edge, String action, int catalogObjectId)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return findTransport(TransportEdgeMatcher.find(Rs2PathApi.getTransports(),
			edge.from(), edge.to()), action, catalogObjectId);
	}

	static Transport findTransport(java.util.Collection<Transport> candidates, String action,
		int catalogObjectId)
	{
		return candidates.stream()
			.filter(CatalogTransitionPolicy::isEligible)
			.filter(candidate -> candidate.getObjectId() == catalogObjectId)
			.filter(candidate -> !CatalogTransitionPolicy.ATTACH_ROPE_ACTION.equalsIgnoreCase(action)
				|| requiresRopePreparation(candidate, action))
			.findFirst().orElse(null);
	}

	private static CatalogTransition shantayTransition(Transport transport, String pendingAction)
	{
		boolean passCarried = Rs2Inventory.hasItem(ShantayPassPolicy.PASS_ITEM_ID);
		boolean eliteDiary = TransportRequirementPolicy.freeShantayEntry(transport);
		boolean gateAlreadyIssued = ShantayPassPolicy.GO_THROUGH_ACTION.equalsIgnoreCase(pendingAction);
		boolean needsVendor = !passCarried && !eliteDiary
			&& !ShantayPassPolicy.isFreeReturn(transport) && !gateAlreadyIssued;
		PurchasableItemCatalog.PurchasableItem purchasable = needsVendor
			? PurchasableItemCatalog.forTransport(transport) : null;
		Rs2NpcModel vendor = purchasable == null ? null : shantayVendor(purchasable);
		ShantayPassPolicy.Stage stage = ShantayPassPolicy.nextStage(transport,
			passCarried,
			Rs2Inventory.itemQuantity(ItemID.COINS),
			eliteDiary,
			vendor != null, pendingAction);
		if (stage == ShantayPassPolicy.Stage.UNAVAILABLE)
		{
			return null;
		}
		if (stage == ShantayPassPolicy.Stage.BUY_PASS)
		{
			return new CatalogTransition(null, purchasable.vendorLocation, transport.getObjectId(),
				ShantayPassPolicy.BUY_PASS_ACTION, transport.getAction(),
				transport.getOrigin(), transport.getDestination());
		}
		return new CatalogTransition(null, transport.getOrigin(), transport.getObjectId(),
			ShantayPassPolicy.GO_THROUGH_ACTION, transport.getAction(),
			transport.getOrigin(), transport.getDestination());
	}

	private static Rs2NpcModel shantayVendor(Transport transport)
	{
		PurchasableItemCatalog.PurchasableItem purchasable =
			PurchasableItemCatalog.forTransport(transport);
		return purchasable == null ? null : shantayVendor(purchasable);
	}

	private static Rs2NpcModel shantayVendor(PurchasableItemCatalog.PurchasableItem purchasable)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			Microbot.getRs2NpcCache().query().withId(purchasable.vendorNpcId)
				.within(purchasable.vendorLocation, purchasable.radius).toList().stream()
				.filter(npc -> hasAction(npc, purchasable.vendorAction))
				.min(Comparator.comparingInt(npc ->
					npc.getWorldLocation().distanceTo2D(purchasable.vendorLocation)))
				.orElse(null)).orElse(null);
	}

	private static boolean hasAction(Rs2NpcModel npc, String action)
	{
		NPCComposition composition = npc.getNpc().getTransformedComposition();
		if (composition == null)
		{
			composition = npc.getNpc().getComposition();
		}
		if (composition == null || composition.getActions() == null)
		{
			return false;
		}
		for (String candidate : composition.getActions())
		{
			if (candidate != null && candidate.equalsIgnoreCase(action))
			{
				return true;
			}
		}
		return false;
	}

	static CatalogTransition visibilityRingPreparation(Transport transport, boolean equipped,
		boolean inventoryVisible)
	{
		if (!CatalogTransitionPolicy.isShadowDungeonLadder(transport) || equipped)
		{
			return null;
		}
		return new CatalogTransition(null, transport.getOrigin(), transport.getObjectId(),
			inventoryVisible ? CatalogTransitionPolicy.VISIBILITY_RING_WEAR
				: CatalogTransitionPolicy.VISIBILITY_RING_OPEN,
			transport.getAction(), transport.getOrigin(), transport.getDestination());
	}

	private static int[] staffIds(Transport transport)
	{
		return transport.getItemIdRequirements().stream().flatMap(java.util.Collection::stream)
			.mapToInt(Integer::intValue).toArray();
	}

	private static List<String> zanarisOptions()
	{
		List<Widget> options = Rs2Dialogue.getDialogueOptions();
		return options == null ? java.util.Collections.emptyList() : options.stream()
			.map(option -> option == null ? "" : option.getText())
			.collect(java.util.stream.Collectors.toList());
	}

	private static String zanarisAction(Transport transport, String pendingAction)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			ZanarisEntrancePolicy.nextAction(!transport.getItemIdRequirements().isEmpty(),
				Rs2Equipment.isWearing(staffIds(transport)), inventoryReady(),
				Rs2Dialogue.hasSelectAnOption() || Rs2Dialogue.hasContinue(),
				zanarisOptions(), pendingAction)).orElse(null);
	}

	private static boolean hasVisibilityRingEquipped()
	{
		return Rs2Equipment.isWearing(CatalogTransitionPolicy.VISIBILITY_RING_IDS.stream()
			.mapToInt(Integer::intValue).toArray());
	}

	private static boolean inventoryReady()
	{
		Widget inventory = Microbot.getClient().getWidget(ComponentID.INVENTORY_CONTAINER);
		return Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY) && inventory != null
			&& !inventory.isHidden() && inventory.getChildren() != null;
	}

	private static DispatchResult dispatchVisibilityRing(Transport transport, String action)
	{
		String expected = Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			CatalogTransition preparation = visibilityRingPreparation(transport,
				hasVisibilityRingEquipped(), inventoryReady());
			return preparation == null ? "" : preparation.getAction();
		}).orElse(null);
		if (expected == null)
		{
			return DispatchResult.REJECTED;
		}
		if (expected.isEmpty())
		{
			return null;
		}
		if (!expected.equals(action))
		{
			return DispatchResult.REJECTED;
		}
		if (CatalogTransitionPolicy.VISIBILITY_RING_OPEN.equals(action))
		{
			return Microbot.getClientThread().runOnClientThreadOptional(() ->
			{
				Microbot.getClient().runScript(915, InterfaceTab.INVENTORY.getVarcIntIndex());
				return DispatchResult.ISSUED;
			}).orElse(DispatchResult.REJECTED);
		}
		return Rs2Inventory.interact(CatalogTransitionPolicy.VISIBILITY_RING_IDS.stream()
			.mapToInt(Integer::intValue).toArray(), "Wear")
			? DispatchResult.ISSUED : DispatchResult.REJECTED;
	}

	static boolean requiresRopePreparation(Transport transport, String action)
	{
		return CatalogTransitionPolicy.ATTACH_ROPE_ACTION.equalsIgnoreCase(action)
			&& CatalogTransitionPolicy.isKalphiteRopeSetup(transport);
	}

	private static String resolveLiveAction(Rs2TileObjectModel object, Transport transport)
	{
		ObjectComposition composition = object.getObjectComposition();
		return composition == null ? null : resolveLiveAction(composition.getActions(), transport);
	}

	static String resolveLiveAction(String[] actions, Transport transport)
	{
		return resolveLiveAction(actions, transport,
			TransportRequirementPolicy.currencyAmount(transport) == 0);
	}

	static String resolveLiveAction(String[] actions, Transport transport,
		boolean freeEnergyBarrier)
	{
		String direct = resolveAction(actions, transport.getAction());
		if (direct == null && transport.getObjectId() == 23609
			&& CatalogTransitionPolicy.isKalphiteRopeDescentRoute(transport))
		{
			direct = resolveAction(actions, "Climb-down (normal)");
		}
		if (direct != null || !EnergyBarrierPolicy.isEligible(transport)
			|| !freeEnergyBarrier)
		{
			return direct;
		}
		return resolveAction(actions, "Pass");
	}

	private static boolean isClosedEntrance(Rs2TileObjectModel object, WorldPoint origin)
	{
		if (object.getWorldLocation().getPlane() != origin.getPlane()
			|| object.getWorldLocation().distanceTo2D(origin) > 1)
		{
			return false;
		}
		ObjectComposition composition = object.getObjectComposition();
		if (composition == null || resolveAction(composition.getActions(), "Open") == null)
		{
			return false;
		}
		String name = normalize(composition.getName());
		return name.contains("trapdoor") || name.contains("manhole")
			|| name.contains("grate") || name.contains("hatch");
	}

	static String resolveAction(String[] actions, String catalogAction)
	{
		if (actions == null || catalogAction == null)
		{
			return null;
		}
		String expected = normalizeAction(catalogAction);
		for (String action : actions)
		{
			if (action != null && normalizeAction(action).equals(expected))
			{
				return action;
			}
		}
		return null;
	}

	private static boolean sameText(String left, String right)
	{
		return left != null && right != null && normalize(left).equals(normalize(right));
	}

	private static String normalizeAction(String value)
	{
		return normalize(value).replace("-", "").replace(" ", "");
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
