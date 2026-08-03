package net.runelite.client.plugins.microbot.aiohunting.enums;

import java.util.Arrays;
import java.util.Comparator;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

/**
 * Curated, land-side destinations and requirements for repeatable Hunter methods.
 *
 * <p>Progression deliberately uses a smaller stable subset than manual mode. Unlocking
 * a method does not immediately make it a sensible training upgrade.</p>
 */
@Getter
public enum HuntingMethod
{
	CRIMSON_SWIFT("Crimson swift", 1, HuntingStyle.BIRD_SNARE,
		new WorldPoint(2613, 2883, 0), ItemID.HUNTING_OJIBWAY_BIRD_SNARE, 0,
		"Crimson swift", false, true),
	GOLDEN_WARBLER("Golden warbler", 5, HuntingStyle.BIRD_SNARE,
		new WorldPoint(3424, 3109, 0), ItemID.HUNTING_OJIBWAY_BIRD_SNARE, 0,
		"Golden warbler", false, false),
	POLAR_KEBBIT("Polar kebbit", 1, HuntingStyle.TRACKING,
		new WorldPoint(2717, 3819, 1), ItemID.NOOSE_WAND, 0,
		"Polar kebbit", false, false),
	COMMON_KEBBIT("Common kebbit", 3, HuntingStyle.TRACKING,
		new WorldPoint(2331, 3576, 0), ItemID.NOOSE_WAND, 0,
		"Common kebbit", false, false),
	FELDIP_WEASEL("Feldip weasel", 7, HuntingStyle.TRACKING,
		new WorldPoint(2525, 2889, 0), ItemID.NOOSE_WAND, 0,
		"Feldip weasel", false, false),
	DESERT_DEVIL("Desert devil", 13, HuntingStyle.TRACKING,
		new WorldPoint(3396, 3106, 0), ItemID.NOOSE_WAND, 0,
		"Desert devil", false, false),
	COPPER_LONGTAIL("Copper longtail", 9, HuntingStyle.BIRD_SNARE,
		new WorldPoint(2340, 3591, 0), ItemID.HUNTING_OJIBWAY_BIRD_SNARE, 0,
		"Copper longtail", false, true),
	CERULEAN_TWITCH("Cerulean twitch", 11, HuntingStyle.BIRD_SNARE,
		new WorldPoint(2728, 3778, 0), ItemID.HUNTING_OJIBWAY_BIRD_SNARE, 0,
		"Cerulean twitch", false, false),
	RUBY_HARVEST("Ruby harvest", 15, HuntingStyle.BUTTERFLY,
		new WorldPoint(2328, 3598, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.BUTTERFLY_JAR, "Ruby harvest", false, false),
	TROPICAL_WAGTAIL("Tropical wagtail", 19, HuntingStyle.BIRD_SNARE,
		new WorldPoint(2542, 2887, 0), ItemID.HUNTING_OJIBWAY_BIRD_SNARE, 0,
		"Tropical wagtail", false, true),
	WILD_KEBBIT("Wild kebbit", 23, HuntingStyle.DEADFALL,
		new WorldPoint(2328, 3554, 0), ItemID.KNIFE, ObjectID.HUNTING_DEADFALL_BOULDER,
		"Wild kebbit", false, true),
	SAPPHIRE_GLACIALIS("Sapphire glacialis", 25, HuntingStyle.BUTTERFLY,
		new WorldPoint(2730, 3778, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.BUTTERFLY_JAR, "Sapphire glacialis", false, false),
	FERRET("Ferret", 27, HuntingStyle.BOX_TRAP,
		new WorldPoint(2311, 3519, 0), ItemID.HUNTING_BOX_TRAP, 0,
		"Ferret", false, false),
	SWAMP_LIZARD("Swamp lizard", 29, HuntingStyle.NET_TRAP,
		new WorldPoint(3549, 3449, 0), 303, ObjectID.HUNTING_SAPLING_UP_SWAMP,
		"Swamp lizard", false, true),
	SPINED_LARUPIA("Spined larupia", 31, HuntingStyle.PITFALL,
		new WorldPoint(2558, 2896, 0), ItemID.HUNTING_TEASING_STICK, 0,
		"Spined larupia", false, false),
	BARB_TAILED_KEBBIT("Barb-tailed kebbit", 33, HuntingStyle.DEADFALL,
		new WorldPoint(2574, 2913, 0), ItemID.KNIFE, ObjectID.HUNTING_DEADFALL_BOULDER,
		"Barb-tailed kebbit", false, false),
	SNOWY_KNIGHT("Snowy knight", 35, HuntingStyle.BUTTERFLY,
		new WorldPoint(2737, 3778, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.BUTTERFLY_JAR, "Snowy knight", false, false),
	PRICKLY_KEBBIT("Prickly kebbit", 37, HuntingStyle.DEADFALL,
		new WorldPoint(2321, 3614, 0), ItemID.KNIFE, ObjectID.HUNTING_DEADFALL_BOULDER,
		"Prickly kebbit", false, false),
	EMBERTAILED_JERBOA("Embertailed jerboa", 39, HuntingStyle.BOX_TRAP,
		new WorldPoint(1519, 3048, 0), ItemID.HUNTING_BOX_TRAP, 0,
		"Embertailed jerboa", false, false),
	SPOTTED_KEBBIT("Spotted kebbit", 43, HuntingStyle.FALCONRY,
		new WorldPoint(2379, 3599, 0), 0, 0, "Spotted kebbit", false, false),
	BLACK_WARLOCK("Black warlock", 45, HuntingStyle.BUTTERFLY,
		new WorldPoint(2540, 2898, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.BUTTERFLY_JAR, "Black warlock", false, false),
	RAZOR_BACKED_KEBBIT("Razor-backed kebbit", 49, HuntingStyle.TRACKING,
		new WorldPoint(2353, 3595, 0), ItemID.NOOSE_WAND, 0,
		"Razor-backed kebbit", false, false),
	ORANGE_SALAMANDER("Orange salamander", 47, HuntingStyle.NET_TRAP,
		new WorldPoint(3410, 3095, 0), 303, ObjectID.HUNTING_SAPLING_UP_ORANGE,
		"Orange salamander", false, true),
	SABRE_TOOTHED_KEBBIT("Sabre-toothed kebbit", 51, HuntingStyle.DEADFALL,
		new WorldPoint(2712, 3775, 0), ItemID.KNIFE, ObjectID.HUNTING_DEADFALL_BOULDER,
		"Sabre-toothed kebbit", false, false),
	CHINCHOMPA("Chinchompa", 53, HuntingStyle.BOX_TRAP,
		new WorldPoint(2354, 3540, 0), ItemID.HUNTING_BOX_TRAP, 0,
		"Chinchompa", false, true),
	DARK_KEBBIT("Dark kebbit", 57, HuntingStyle.FALCONRY,
		new WorldPoint(2379, 3599, 0), 0, 0, "Dark kebbit", false, false),
	PYRE_FOX("Pyre fox", 57, HuntingStyle.DEADFALL,
		new WorldPoint(1615, 2999, 0), ItemID.KNIFE, ObjectID.HUNTING_DEADFALL_BOULDER,
		"Pyre fox", false, false),
	RED_SALAMANDER("Red salamander", 59, HuntingStyle.NET_TRAP,
		new WorldPoint(2472, 3239, 0), 303, ObjectID.HUNTING_SAPLING_UP_RED,
		"Red salamander", false, true),
	CARNIVOROUS_CHINCHOMPA("Carnivorous chinchompa", 63, HuntingStyle.BOX_TRAP,
		new WorldPoint(2556, 2914, 0), ItemID.HUNTING_BOX_TRAP, 0,
		"Carnivorous chinchompa", false, true),
	SUNLIGHT_MOTH("Sunlight moth", 65, HuntingStyle.BUTTERFLY,
		new WorldPoint(1584, 3021, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.BUTTERFLY_JAR, "Sunlight Moth", false, false),
	BLACK_SALAMANDER("Black salamander", 67, HuntingStyle.NET_TRAP,
		new WorldPoint(3296, 3675, 0), 303, ObjectID.HUNTING_SAPLING_UP_BLACK,
		"Black salamander", true, false),
	DASHING_KEBBIT("Dashing kebbit", 69, HuntingStyle.FALCONRY,
		new WorldPoint(2379, 3599, 0), 0, 0, "Dashing kebbit", false, false),
	BLACK_CHINCHOMPA("Black chinchompa", 73, HuntingStyle.BOX_TRAP,
		new WorldPoint(3154, 3773, 0), ItemID.HUNTING_BOX_TRAP, 0,
		"Black chinchompa", true, false),
	MOONLIGHT_MOTH("Moonlight moth", 75, HuntingStyle.BUTTERFLY,
		new WorldPoint(1562, 9441, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.BUTTERFLY_JAR, "Moonlight moth", false, false),
	TECU_SALAMANDER("Tecu salamander", 79, HuntingStyle.NET_TRAP,
		new WorldPoint(1473, 3096, 0), 303, ObjectID.HUNTING_SAPLING_UP_MOUNTAIN,
		"Tecu salamander", false, true),
	BIRD_HOUSE_RUN("Birdhouse run", 5, HuntingStyle.BIRD_HOUSE,
		new WorldPoint(3763, 3755, 0), 0, 0,
		"Birdhouse", false, false),
	HERBIBOAR("Herbiboar", 80, HuntingStyle.HERBIBOAR,
		new WorldPoint(3705, 3840, 0), 0, 0,
		"Herbiboar", false, false),
	BABY_IMPLING("Baby impling", 17, HuntingStyle.IMPLING,
		new WorldPoint(2592, 4320, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.II_IMPLING_JAR, "Baby impling", false, false),
	YOUNG_IMPLING("Young impling", 22, HuntingStyle.IMPLING,
		new WorldPoint(2592, 4320, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.II_IMPLING_JAR, "Young impling", false, false),
	GOURMET_IMPLING("Gourmet impling", 28, HuntingStyle.IMPLING,
		new WorldPoint(2592, 4320, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.II_IMPLING_JAR, "Gourmet impling", false, false),
	EARTH_IMPLING("Earth impling", 36, HuntingStyle.IMPLING,
		new WorldPoint(2592, 4320, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.II_IMPLING_JAR, "Earth impling", false, false),
	ESSENCE_IMPLING("Essence impling", 42, HuntingStyle.IMPLING,
		new WorldPoint(2592, 4320, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.II_IMPLING_JAR, "Essence impling", false, false),
	ECLECTIC_IMPLING("Eclectic impling", 50, HuntingStyle.IMPLING,
		new WorldPoint(2592, 4320, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.II_IMPLING_JAR, "Eclectic impling", false, false),
	NATURE_IMPLING("Nature impling", 58, HuntingStyle.IMPLING,
		new WorldPoint(2592, 4320, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.II_IMPLING_JAR, "Nature impling", false, false),
	MAGPIE_IMPLING("Magpie impling", 65, HuntingStyle.IMPLING,
		new WorldPoint(2592, 4320, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.II_IMPLING_JAR, "Magpie impling", false, false),
	NINJA_IMPLING("Ninja impling", 74, HuntingStyle.IMPLING,
		new WorldPoint(2592, 4320, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.II_IMPLING_JAR, "Ninja impling", false, false),
	DRAGON_IMPLING("Dragon impling", 83, HuntingStyle.IMPLING,
		new WorldPoint(2592, 4320, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.II_IMPLING_JAR, "Dragon impling", false, false),
	LUCKY_IMPLING("Lucky impling", 89, HuntingStyle.IMPLING,
		new WorldPoint(2592, 4320, 0), ItemID.HUNTING_BUTTERFLY_NET,
		ItemID.II_IMPLING_JAR, "Lucky impling", false, false);

	private final String displayName;
	private final int level;
	private final HuntingStyle style;
	private final WorldPoint location;
	private final int primaryToolId;
	private final int secondaryId;
	private final String targetName;
	private final boolean dangerous;
	private final boolean progressive;

	HuntingMethod(
		String displayName,
		int level,
		HuntingStyle style,
		WorldPoint location,
		int primaryToolId,
		int secondaryId,
		String targetName,
		boolean dangerous,
		boolean progressive)
	{
		this.displayName = displayName;
		this.level = level;
		this.style = style;
		this.location = location;
		this.primaryToolId = primaryToolId;
		this.secondaryId = secondaryId;
		this.targetName = targetName;
		this.dangerous = dangerous;
		this.progressive = progressive;
	}

	public static HuntingMethod bestFor(int level)
	{
		return Arrays.stream(values())
			.filter(HuntingMethod::isProgressive)
			.filter(method -> method.level <= level)
			.max(Comparator.comparingInt(HuntingMethod::getLevel))
			.orElse(CRIMSON_SWIFT);
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
