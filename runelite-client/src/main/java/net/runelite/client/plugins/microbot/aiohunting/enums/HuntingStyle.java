package net.runelite.client.plugins.microbot.aiohunting.enums;

public enum HuntingStyle
{
	BIRD_SNARE("Bird snare"),
	BUTTERFLY("Butterfly net"),
	BOX_TRAP("Box trap"),
	NET_TRAP("Net trap"),
	DEADFALL("Deadfall"),
	FALCONRY("Falconry"),
	BIRD_HOUSE("Birdhouse run"),
	HERBIBOAR("Herbiboar"),
	IMPLING("Impling"),
	TRACKING("Tracking");

	private final String displayName;

	HuntingStyle(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}
}
