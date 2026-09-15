package net.runelite.client.plugins.microbot.util.walker.transport.model;

/** Immutable observation of a verified item action and its containing tab. */
public final class ItemTeleport
{
	private final int itemId;
	private final String action;
	private final boolean equipped;
	private final boolean tabReady;
	private final String command;
	private final boolean available;

	public ItemTeleport(int itemId, String action, boolean equipped, boolean tabReady)
	{
		this.itemId = itemId;
		this.action = action;
		this.equipped = equipped;
		this.tabReady = tabReady;
		this.available = true;
		this.command = (tabReady ? "item-use:" : "item-open:")
			+ (equipped ? "equipment:" : "inventory:") + action;
	}

	public ItemTeleport(int itemId, String action, String command)
	{
		this(itemId, action, command, true);
	}

	public ItemTeleport(int itemId, String action, String command, boolean available)
	{
		this.itemId = itemId;
		this.action = action;
		this.equipped = false;
		this.tabReady = true;
		this.command = command;
		this.available = available;
	}

	public int getItemId() { return itemId; }
	public String getAction() { return action; }
	public boolean isEquipped() { return equipped; }
	public boolean isTabReady() { return tabReady; }
	public boolean isAvailable() { return available; }

	public String command()
	{
		return command;
	}
}
