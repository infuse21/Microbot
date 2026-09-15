package net.runelite.client.plugins.microbot.util.walker.transport;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.runelite.api.MenuAction;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.SimpleTeleport;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

/** One observed preparation or cast input; the engine owns all waits and landing. */
public final class Rs2SpellTeleportScene
{
	private static final String PREFIX = "spell-prepare:";
	private Rs2SpellTeleportScene() { }

	public static boolean isPreparation(String command)
	{
		return command != null && command.startsWith(PREFIX);
	}

	public static String display(String command)
	{
		return isPreparation(command) ? command.substring(command.indexOf(':', PREFIX.length()) + 1) : command;
	}

	public static SimpleTeleport observe(SimpleTeleport teleport, String pending)
	{
		Selection selection = snapshot(teleport, pending);
		return selection == null ? null : selection.teleport;
	}

	public static boolean dispatch(RouteInteraction interaction)
	{
		if (interaction == null || Thread.currentThread().isInterrupted()
			|| interaction.getObjectId() != TransportType.TELEPORTATION_SPELL.ordinal()) return false;
		Transport transport = TransportEdgeMatcher.find(Rs2PathApi.getTransports(), interaction.getFrom(), interaction.getTo())
			.stream().filter(SimpleTeleportPolicy::isEligible).filter(Rs2SimpleTeleportScene::matchesHousePreference)
			.filter(row -> row.getType() == TransportType.TELEPORTATION_SPELL
				&& row.getDisplayInfo().equalsIgnoreCase(display(interaction.getAction()))).findFirst().orElse(null);
		if (transport == null) return false;
		if (net.runelite.client.plugins.microbot.util.player.Rs2Pvp.isInWilderness())
		{
			net.runelite.api.coords.WorldPoint player = net.runelite.client.plugins.microbot.util.player.Rs2Player.getWorldLocation();
			if (player == null || net.runelite.client.plugins.microbot.util.player.Rs2Pvp.getWildernessLevelFrom(player)
				> transport.getMaxWildernessLevel() + 1) return false;
		}
		Selection selection = snapshot(new SimpleTeleport(interaction.getFrom(), interaction.getTo(),
			transport.getType(), transport.getDisplayInfo()), null);
		if (selection == null || !selection.teleport.isAvailable() || !selection.ready
			|| !selection.teleport.getCommand().equals(interaction.getAction()) || Thread.currentThread().isInterrupted()) return false;
		if (selection.teleport.getCommand().startsWith(PREFIX + "open:"))
		{
			return Microbot.getClientThread().runOnClientThreadOptional(() ->
			{
				Microbot.getClient().runScript(915, InterfaceTab.MAGIC.getVarcIntIndex());
				return true;
			}).orElse(false);
		}
		if (isPreparation(selection.teleport.getCommand()))
		{
			if (selection.teleport.getCommand().startsWith(PREFIX + "enable-filters:"))
			{
				Microbot.doInvoke(new NewMenuEntry().option("Enable spell filtering").target("")
					.param0(-1).param1(selection.id).opcode(MenuAction.CC_OP.getId()).identifier(2), selection.bounds);
				return true;
			}
			Microbot.getMouse().click(selection.bounds);
			return true;
		}
		Microbot.doInvoke(new NewMenuEntry().option(SimpleTeleportPolicy.spellOption(transport))
			.target(SimpleTeleportPolicy.spellName(transport)).param0(selection.index).param1(selection.id)
			.opcode(MenuAction.CC_OP.getId()).identifier(SimpleTeleportPolicy.spellIdentifier(transport)).itemId(-1), selection.bounds);
		return true;
	}

	private static Selection snapshot(SimpleTeleport teleport, String pending)
	{
		if (Thread.currentThread().isInterrupted()) return null;
		String name = teleport.getDisplayInfo().split(":", 2)[0];
		MagicAction spell = Arrays.stream(MagicAction.values())
			.filter(value -> value.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
		if (spell == null) return stage(teleport, "unavailable", false, false, null);
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			if (Microbot.getClient().getVarbitValue(VarbitID.SPELLBOOK) != spell.getSpellbook().getValue())
				return stage(teleport, "unavailable", false, false, null);
			if (!Rs2Tab.isCurrentTab(InterfaceTab.MAGIC)) return stage(teleport, "open", true, true, null);
			Widget root = Microbot.getClient().getWidget(218, 0);
			if (root == null || root.isHidden()) return waiting(teleport, pending);
			List<Widget> queue = new ArrayList<>();
			Set<Widget> seen = Collections.newSetFromMap(new IdentityHashMap<>());
			queue.add(root);
			List<Entry> entries = new ArrayList<>();
			for (int i = 0; i < queue.size() && i < 512; i++)
			{
				Widget widget = queue.get(i);
				if (widget == null || !seen.add(widget) || widget.isHidden()) continue;
				entries.add(new Entry(widget.getId(), widget.getIndex(), widget.getSpriteId(), widget.getText(), widget.getBounds()));
				for (Widget[] children : new Widget[][]{widget.getStaticChildren(), widget.getDynamicChildren(), widget.getNestedChildren()})
					if (children != null) Collections.addAll(queue, children);
			}
			if (queue.size() > 512) return waiting(teleport, pending);
			Entry filters = uniqueText(entries, "Filters");
			Entry showTeleports = uniqueText(entries, "Show Teleport spells");
			if (showTeleports != null)
			{
				return Microbot.getClient().getVarbitValue(6609) != 0
					? stage(teleport, "show-teleports", true, true, showTeleports)
					: filters == null ? waiting(teleport, pending) : stage(teleport, "close-filters", true, true, filters);
			}
			List<Entry> spells = new ArrayList<>();
			for (Entry entry : entries) if (entry.sprite == spell.getSprite()) spells.add(entry);
			if (spells.size() == 1)
			{
				Entry entry = spells.get(0);
				return new Selection(teleport, entry, validBounds(entry));
			}
			if (spells.size() > 1) return waiting(teleport, pending);
			for (Entry entry : entries)
				if (entry.id == 14286852 && validBounds(entry)) return stage(teleport, "close-enchant", true, true, entry);
			if (Microbot.getClient().getVarbitValue(6609) != 0 && filters != null)
				return stage(teleport, "open-filters", true, true, filters);
			if (Microbot.getClient().getVarbitValue(6609) != 0
				&& Microbot.getClient().getVarbitValue(VarbitID.MAGIC_SPELLBOOK_HIDEFILTERBUTTON) != 0)
			{
				int tabId = !Microbot.getClient().isResized() ? InterfaceID.Toplevel.STONE6
					: Microbot.getClient().getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1
					? InterfaceID.ToplevelPreEoc.STONE6 : InterfaceID.ToplevelOsrsStretch.STONE6;
				Widget tab = Microbot.getClient().getWidget(tabId);
				if (tab != null && !tab.isHidden())
					return stage(teleport, "enable-filters", true, true,
						new Entry(tab.getId(), -1, -1, "", tab.getBounds()));
			}
			return waiting(teleport, pending);
		}).orElse(null);
	}

	private static Selection waiting(SimpleTeleport teleport, String pending)
	{
		return new Selection(new SimpleTeleport(teleport.getRouteOrigin(), teleport.getDestination(), teleport.getType(),
			teleport.getDisplayInfo(), pending == null ? PREFIX + "open:" + teleport.getDisplayInfo() : pending, true), null, false);
	}

	private static Selection stage(SimpleTeleport teleport, String stage, boolean available, boolean ready, Entry entry)
	{
		return new Selection(new SimpleTeleport(teleport.getRouteOrigin(), teleport.getDestination(), teleport.getType(),
			teleport.getDisplayInfo(), PREFIX + stage + ":" + teleport.getDisplayInfo(), available), entry,
			ready && (entry == null || validBounds(entry)));
	}

	private static Entry uniqueText(List<Entry> entries, String text)
	{
		Entry match = null;
		for (Entry entry : entries)
		{
			if (entry.text != null && text.equalsIgnoreCase(entry.text.replaceAll("<[^>]*>", "").trim()))
			{
				if (match != null) return null;
				match = entry;
			}
		}
		return match;
	}

	private static boolean validBounds(Entry entry)
	{
		return entry.bounds != null && entry.bounds.width > 0 && entry.bounds.height > 0;
	}

	private static final class Entry
	{
		final int id, index, sprite;
		final String text;
		final Rectangle bounds;
		Entry(int id, int index, int sprite, String text, Rectangle bounds)
		{
			this.id = id; this.index = index; this.sprite = sprite; this.text = text;
			this.bounds = bounds == null ? null : new Rectangle(bounds);
		}
	}

	private static final class Selection
	{
		final SimpleTeleport teleport;
		final int id, index;
		final Rectangle bounds;
		final boolean ready;
		Selection(SimpleTeleport teleport, Entry entry, boolean ready)
		{
			this.teleport = teleport; this.ready = ready;
			id = entry == null ? -1 : entry.id; index = entry == null ? -1 : entry.index;
			bounds = entry == null ? null : entry.bounds;
		}
	}
}
