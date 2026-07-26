package net.runelite.client.plugins.microbot.aiohunting;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.plugins.microbot.aiohunting.enums.BirdhouseLog;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingMethod;

@ConfigGroup(AIOHuntingConfig.GROUP)
public interface AIOHuntingConfig extends Config
{
	String GROUP = "AIOHunting";

	@ConfigSection(
		name = "Progression",
		description = "Method selection and target level",
		position = 0
	)
	String PROGRESSION = "progression";

	@ConfigSection(
		name = "Inventory",
		description = "Banking, dropping and supplies",
		position = 1
	)
	String INVENTORY = "inventory";

	@ConfigSection(
		name = "Safety",
		description = "Area and Wilderness safeguards",
		position = 2
	)
	String SAFETY = "safety";

	@ConfigSection(
		name = "Birdhouse run",
		description = "Birdhouse runner options (used when the Birdhouse method is selected)",
		position = 3,
		closedByDefault = true
	)
	String BIRDHOUSE = "birdhouse";

	@ConfigSection(
		name = "Special activities",
		description = "Herbiboar and impling options",
		position = 4,
		closedByDefault = true
	)
	String SPECIAL = "special";

	@ConfigSection(
		name = "Bird snares",
		description = "Bird snare placement (used when a bird snare method is selected)",
		position = 5,
		closedByDefault = true
	)
	String BIRD_SNARE = "birdSnare";

	@ConfigItem(
		keyName = "autoProgress",
		name = "Auto progression",
		description = "Use the stable automatic progression route",
		position = 0,
		section = PROGRESSION,
		hidden = true
	)
	default boolean autoProgress()
	{
		return true;
	}

	@ConfigItem(
		keyName = "manualMethod",
		name = "Manual method",
		description = "Hunter target to use when automatic progression is disabled",
		position = 1,
		section = PROGRESSION,
		hidden = true
	)
	default HuntingMethod manualMethod()
	{
		return HuntingMethod.CRIMSON_SWIFT;
	}

	@Range(min = 2, max = 99)
	@ConfigItem(
		keyName = "targetLevel",
		name = "Target level",
		description = "Stop acting once this real Hunter level is reached",
		position = 2,
		section = PROGRESSION
	)
	default int targetLevel()
	{
		return 99;
	}

	@ConfigItem(
		keyName = "useBank",
		name = "Bank loot",
		description = "Bank catches when space is low. When disabled, known Hunter loot is dropped",
		position = 0,
		section = INVENTORY
	)
	default boolean useBank()
	{
		return true;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = "spareTraps",
		name = "Spare traps",
		description = "Extra bird snares, box traps, ropes and nets to carry",
		position = 1,
		section = INVENTORY
	)
	default int spareTraps()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "buryBones",
		name = "Bury bones",
		description = "Bury bones from bird, deadfall and falconry catches",
		position = 2,
		section = INVENTORY
	)
	default boolean buryBones()
	{
		return true;
	}

	@ConfigItem(
		keyName = "keepFurs",
		name = "Keep furs",
		description = "Bank kebbit furs instead of dropping them",
		position = 3,
		section = INVENTORY
	)
	default boolean keepFurs()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dropMeat",
		name = "Drop meat",
		description = "Drop raw bird and beast meat as it's caught instead of banking it, for fewer bank trips",
		position = 4,
		section = INVENTORY
	)
	default boolean dropMeat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "useHuntsmanKit",
		name = "Use Huntsman's kit",
		description = "Use carried supplies normally; kit integration is used when a compatible kit is present",
		position = 5,
		section = INVENTORY
	)
	default boolean useHuntsmanKit()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dropItems",
		name = "Drop list",
		description = "Comma-separated item names to drop while hunting (e.g. Kebbit claws, Bones). "
			+ "Hunting supplies and anything on the keep list are never dropped.",
		position = 6,
		section = INVENTORY
	)
	default String dropItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "keepItems",
		name = "Keep list",
		description = "Comma-separated item names to always keep - never dropped or banked away "
			+ "(in addition to coins and hunting supplies).",
		position = 7,
		section = INVENTORY
	)
	default String keepItems()
	{
		return "";
	}

	@Range(min = 8, max = 30)
	@ConfigItem(
		keyName = "huntingRadius",
		name = "Hunting radius",
		description = "Only interact with targets and owned traps inside this radius of the curated spot",
		position = 0,
		section = SAFETY
	)
	default int huntingRadius()
	{
		return 16;
	}

	@ConfigItem(
		keyName = "allowWilderness",
		name = "Allow Wilderness",
		description = "Required for black salamanders and black chinchompas",
		position = 1,
		section = SAFETY
	)
	default boolean allowWilderness()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hopAroundPlayers",
		name = "Hop around players",
		description = "World hop when another player remains inside the hunting area",
		position = 2,
		section = SAFETY
	)
	default boolean hopAroundPlayers()
	{
		return false;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = "playersBeforeHop",
		name = "Players before hop",
		description = "Number of other players in the area that triggers a hop",
		position = 3,
		section = SAFETY
	)
	default int playersBeforeHop()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "automaticBirdhouseLog",
		name = "Best birdhouse",
		description = "Use the best birdhouse supported by both Hunter and Crafting",
		position = 0,
		section = BIRDHOUSE
	)
	default boolean automaticBirdhouseLog()
	{
		return true;
	}

	@ConfigItem(
		keyName = "birdhouseLog",
		name = "Birdhouse logs",
		description = "Logs you have available (used when Best birdhouse is disabled)",
		position = 1,
		section = BIRDHOUSE
	)
	default BirdhouseLog birdhouseLog()
	{
		return BirdhouseLog.LOGS;
	}

	@ConfigItem(
		keyName = "emptyBirdNests",
		name = "Search bird nests",
		description = "Search collected bird nests after completing a run",
		position = 2,
		section = BIRDHOUSE
	)
	default boolean emptyBirdNests()
	{
		return true;
	}

	@ConfigItem(
		keyName = "keepHerbSack",
		name = "Keep herb sack",
		description = "Retain a herb sack while banking Herbiboar loot",
		position = 3,
		section = SPECIAL
	)
	default boolean keepHerbSack()
	{
		return true;
	}

	@ConfigItem(
		keyName = "keepMagicSecateurs",
		name = "Keep magic secateurs",
		description = "Retain magic secateurs while banking Herbiboar loot",
		position = 4,
		section = SPECIAL
	)
	default boolean keepMagicSecateurs()
	{
		return true;
	}

	@Range(min = 5, max = 26)
	@ConfigItem(
		keyName = "implingJars",
		name = "Impling jars",
		description = "Number of empty impling jars to carry into Puro-Puro",
		position = 5,
		section = SPECIAL
	)
	default int implingJars()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "campBirdSpawns",
		name = "Camp bird spawns",
		description = "Lay bird snares on the tiles where target birds are, so respawned birds land back on the trap, "
			+ "instead of laying wherever you happen to be standing",
		position = 0,
		section = BIRD_SNARE
	)
	default boolean campBirdSpawns()
	{
		return true;
	}
}
