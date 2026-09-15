package net.runelite.client.plugins.microbot.util.leaguetransport;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.ItemTeleport;

/** One observed map stage per engine command; no menu waits or retained widget references. */
public final class Rs2MapOfAlacrityScene
{
	static final String OPEN = "alacrity-open:";
	static final String READ = "alacrity-read:";
	static final String REGION = "alacrity-region:";
	static final String DESTINATION = "alacrity-destination:";
	private static final String UNAVAILABLE = "alacrity-unavailable:";

	private Rs2MapOfAlacrityScene() { }

	public static boolean owns(String command)
	{
		return command != null && command.startsWith("alacrity-");
	}

	public static ItemTeleport observe(Collection<Transport> rows, String pending)
	{
		Transport transport = find(rows, pending);
		if (transport == null) return null;
		if (!Rs2MapOfAlacrityTransport.isAvailable(transport)) return unavailable(transport).item;
		// A destination input owns the voyage even if its source menu remains visible briefly.
		if (pending != null && pending.startsWith(DESTINATION)) return waiting(transport, pending).item;
		Selection selection = snapshot(transport, pending);
		if (selection != null && selection.lock != 0)
		{
			Rs2MapOfAlacrityTransport.markUnavailable(transport, selection.lock == 1);
		}
		return selection == null ? null : selection.item;
	}

	public static boolean dispatch(RouteInteraction interaction)
	{
		if (Thread.currentThread().isInterrupted() || interaction == null
			|| interaction.getObjectId() != Rs2MapOfAlacrityTransport.ITEM_ID) return false;
		String command = interaction.getAction();
		Transport transport = find(TransportEdgeMatcher.find(Rs2PathApi.getTransports(),
			interaction.getFrom(), interaction.getTo()), command);
		if (transport == null || !Rs2MapOfAlacrityTransport.isAvailable(transport)) return false;
		// Re-observe the destination page using the preceding region stage, not the voyage wait.
		String context = command.startsWith(DESTINATION) ? REGION + transport.getDisplayInfo()
			: command.startsWith(READ) ? null : command;
		Selection selection = snapshot(transport, context);
		if (selection == null || !selection.inputReady || !selection.item.isAvailable()
			|| !command.equals(selection.item.command()) || Thread.currentThread().isInterrupted()) return false;
		if (command.startsWith(OPEN))
		{
			return Microbot.getClientThread().runOnClientThreadOptional(() ->
			{
				Microbot.getClient().runScript(915, InterfaceTab.INVENTORY.getVarcIntIndex());
				return true;
			}).orElse(false);
		}
		if (command.startsWith(READ))
		{
			return Rs2Inventory.interact(Rs2MapOfAlacrityTransport.ITEM_ID, selection.item.getAction());
		}
		if (selection.hotkey != null) Rs2Keyboard.keyPress(selection.hotkey);
		else if (selection.bounds != null) Microbot.getMouse().click(selection.bounds);
		else return false;
		if (command.startsWith(DESTINATION)) Rs2LeaguesTransport.recordTransportAttempt(transport);
		return true;
	}

	private static Transport find(Collection<Transport> rows, String pending)
	{
		return rows.stream().filter(Rs2MapOfAlacrityTransport::isStagedRoute)
			.filter(row -> pending == null || owns(pending) && pending.endsWith(":" + row.getDisplayInfo()))
			.findFirst().orElse(null);
	}

	private static Selection snapshot(Transport transport, String pending)
	{
		if (Thread.currentThread().isInterrupted()) return null;
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Rs2MapOfAlacrityTransport.Destination destination =
				Rs2MapOfAlacrityTransport.parseDestination(transport.getDisplayInfo());
			Widget root = Microbot.getClient().getWidget(Rs2MapOfAlacrityTransport.WIDGET_GROUP,
				Rs2MapOfAlacrityTransport.LIST_CHILD);
			if (root != null && !root.isHidden())
			{
				List<Entry> entries = new ArrayList<>();
				Set<Widget> seen = Collections.newSetFromMap(new IdentityHashMap<>());
				for (Widget[] children : new Widget[][]{root.getDynamicChildren(), root.getNestedChildren(), root.getStaticChildren()})
				{
					if (children == null) continue;
					for (Widget child : children)
					{
						if (child != null && seen.add(child) && !child.isHidden())
							entries.add(new Entry(child.getText(), child.getBounds()));
					}
				}
				Rectangle viewport = root.getBounds();
				if (pending != null && pending.startsWith(REGION))
				{
					Entry row = uniqueEntry(entries, destination.name);
					if (row != null) return selection(transport, row, viewport, DESTINATION, 2);
				}
				Entry region = uniqueEntry(entries, destination.region);
				if (region != null) return selection(transport, region, viewport, REGION, 1);
				// Empty/loading or foreign pages do not authorize another Read or a guessed hotkey.
				return waiting(transport, pending);
			}
			if (pending != null && (pending.startsWith(READ) || pending.startsWith(REGION)))
			{
				return waiting(transport, pending);
			}
			Rs2ItemModel item = Rs2Inventory.get(Rs2MapOfAlacrityTransport.ITEM_ID);
			if (item == null) return unavailable(transport);
			String[] actions = item.getInventoryActions();
			if (actions == null || java.util.Arrays.stream(actions).noneMatch("Read"::equalsIgnoreCase))
				return unavailable(transport);
			String action = "Read";
			Widget inventory = Microbot.getClient().getWidget(ComponentID.INVENTORY_CONTAINER);
			boolean ready = Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY) && inventory != null
				&& !inventory.isHidden() && inventory.getChildren() != null;
			return new Selection(new ItemTeleport(Rs2MapOfAlacrityTransport.ITEM_ID, action,
				(ready ? READ : OPEN) + transport.getDisplayInfo()), null, null, true, 0);
		}).orElse(null);
	}

	private static Selection selection(Transport transport, Entry row, Rectangle viewport, String stage, int lock)
	{
		String text = row.text;
		if (text != null && text.toLowerCase(Locale.ROOT).contains("<str>"))
		{
			return new Selection(unavailable(transport).item, null, null, false, lock);
		}
		Character key = Rs2MapOfAlacrityTransport.extractHotkey(text);
		Rectangle bounds = row.bounds;
		boolean visible = bounds != null && bounds.width > 0 && bounds.height > 0
			&& viewport != null && viewport.contains(bounds);
		return new Selection(new ItemTeleport(Rs2MapOfAlacrityTransport.ITEM_ID, text,
			stage + transport.getDisplayInfo()), key, visible ? new Rectangle(bounds) : null,
			key != null || visible, 0);
	}

	private static Entry uniqueEntry(List<Entry> entries, String requested)
	{
		Entry match = null;
		for (Entry row : entries)
		{
			if (Rs2MapOfAlacrityTransport.normalizedTextContainsAllTokens(row.text, requested))
			{
				if (match != null) return null;
				match = row;
			}
		}
		return match;
	}

	private static Selection waiting(Transport transport, String pending)
	{
		return new Selection(new ItemTeleport(Rs2MapOfAlacrityTransport.ITEM_ID, "",
			pending == null ? READ + transport.getDisplayInfo() : pending), null, null, false, 0);
	}

	private static Selection unavailable(Transport transport)
	{
		return new Selection(new ItemTeleport(Rs2MapOfAlacrityTransport.ITEM_ID, "",
			UNAVAILABLE + transport.getDisplayInfo(), false), null, null, false, 0);
	}

	private static final class Entry
	{
		final String text;
		final Rectangle bounds;

		Entry(String text, Rectangle bounds)
		{
			this.text = text;
			this.bounds = bounds == null ? null : new Rectangle(bounds);
		}
	}

	private static final class Selection
	{
		final ItemTeleport item;
		final Character hotkey;
		final Rectangle bounds;
		final boolean inputReady;
		final int lock;

		Selection(ItemTeleport item, Character hotkey, Rectangle bounds, boolean inputReady, int lock)
		{
			this.item = item;
			this.hotkey = hotkey;
			this.bounds = bounds;
			this.inputReady = inputReady;
			this.lock = lock;
		}
	}
}
