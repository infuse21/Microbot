package net.runelite.client.plugins.microbot.aiohunting.enums;

import java.util.Arrays;
import java.util.Comparator;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

@Getter
public enum BirdhouseLog
{
	LOGS("Logs", ItemID.LOGS, 5, 5),
	OAK("Oak logs", ItemID.OAK_LOGS, 14, 15),
	WILLOW("Willow logs", ItemID.WILLOW_LOGS, 24, 25),
	TEAK("Teak logs", ItemID.TEAK_LOGS, 34, 35),
	MAPLE("Maple logs", ItemID.MAPLE_LOGS, 44, 45),
	MAHOGANY("Mahogany logs", ItemID.MAHOGANY_LOGS, 49, 50),
	YEW("Yew logs", ItemID.YEW_LOGS, 59, 60),
	MAGIC("Magic logs", ItemID.MAGIC_LOGS, 74, 75),
	REDWOOD("Redwood logs", ItemID.REDWOOD_LOGS, 89, 90);

	private final String displayName;
	private final int itemId;
	private final int hunterLevel;
	private final int craftingLevel;

	BirdhouseLog(String displayName, int itemId, int hunterLevel, int craftingLevel)
	{
		this.displayName = displayName;
		this.itemId = itemId;
		this.hunterLevel = hunterLevel;
		this.craftingLevel = craftingLevel;
	}

	public static BirdhouseLog bestFor(int hunterLevel, int craftingLevel)
	{
		return Arrays.stream(values())
			.filter(log -> log.hunterLevel <= hunterLevel)
			.filter(log -> log.craftingLevel <= craftingLevel)
			.max(Comparator.comparingInt(BirdhouseLog::getHunterLevel))
			.orElse(LOGS);
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
