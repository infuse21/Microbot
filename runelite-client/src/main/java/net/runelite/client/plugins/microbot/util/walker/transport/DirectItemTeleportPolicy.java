package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Map;
import java.util.Set;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

/** Audited direct-item action contracts; route requirements remain owned by the catalog. */
public final class DirectItemTeleportPolicy
{
	private static final Map<String, Set<Integer>> ITEMS = Map.ofEntries(
		Map.entry("Fremennik sea boots", Set.of(13129, 13130, 13131, 13132)),
		Map.entry("Kandarin headgear", Set.of(13139, 13140)),
		Map.entry("Wilderness sword", Set.of(13110, 13111)),
		Map.entry("Western banner", Set.of(13143, 13144)),
		Map.entry("Varrock tablet", Set.of(8007)),
		Map.entry("Falador tablet", Set.of(8009)),
		Map.entry("Lumbridge tablet", Set.of(8008)),
		Map.entry("Camelot tablet", Set.of(8010)),
		Map.entry("Ardougne tablet", Set.of(8011)),
		Map.entry("Watchtower tablet", Set.of(8012)),
		Map.entry("Rimmington tablet", Set.of(11741)),
		Map.entry("Taverley tablet", Set.of(11742)),
		Map.entry("Pollnivneach tablet", Set.of(11743)),
		Map.entry("Rellekka tablet", Set.of(11744)),
		Map.entry("Brimhaven tablet", Set.of(11745)),
		Map.entry("Yanille tablet", Set.of(11746)),
		Map.entry("Trollheim tablet", Set.of(11747)),
		Map.entry("Hosidius tablet", Set.of(19651)),
		Map.entry("Prifddinas tablet", Set.of(23771)),
		Map.entry("Paddewwa tablet", Set.of(12781)),
		Map.entry("Senntisten tablet", Set.of(12782)),
		Map.entry("Kharyrll tablet", Set.of(12779)),
		Map.entry("Lassar tablet", Set.of(12780)),
		Map.entry("Dareeyak tablet", Set.of(12777)),
		Map.entry("Carrallanger tablet", Set.of(12776)),
		Map.entry("Annakarl tablet", Set.of(12775)),
		Map.entry("Ghorrock tablet", Set.of(12778)),
		Map.entry("Moonclan tablet", Set.of(24949)),
		Map.entry("Ourania tablet", Set.of(24951)),
		Map.entry("Waterbirth tablet", Set.of(24953)),
		Map.entry("Barbarian tablet", Set.of(24955)),
		Map.entry("Khazard tablet", Set.of(24957)),
		Map.entry("Fishing guild tablet", Set.of(24959)),
		Map.entry("Catherby tablet", Set.of(24961)),
		Map.entry("Ice plateau tablet", Set.of(24963)),
		Map.entry("Arceuus Library tablet", Set.of(19613)),
		Map.entry("Draynor Manor tablet", Set.of(19615)),
		Map.entry("Battlefront tablet", Set.of(22949)),
		Map.entry("Mind Altar tablet", Set.of(19617)),
		Map.entry("Salve Graveyard tablet", Set.of(19619)),
		Map.entry("Fenkenstrain's Castle tablet", Set.of(19621)),
		Map.entry("West Ardougne tablet", Set.of(19623)),
		Map.entry("Harmony Island tablet", Set.of(19625)),
		Map.entry("Cemetery tablet", Set.of(19627)),
		Map.entry("Barrows tablet", Set.of(19629)),
		Map.entry("Ape Atoll tablet", Set.of(19631)),
		Map.entry("Volcanic Mine tablet", Set.of(21541)),
		Map.entry("Wilderness Crabs tablet", Set.of(24251)),
		Map.entry("Civitas illa Fortis tablet", Set.of(28824)),
		Map.entry("Nardah teleport", Set.of(12402)),
		Map.entry("Digsite teleport", Set.of(12403)),
		Map.entry("Feldip hills teleport", Set.of(12404)),
		Map.entry("Lunar isle teleport", Set.of(12405)),
		Map.entry("Mort'ton teleport", Set.of(12406)),
		Map.entry("Pest control teleport", Set.of(12407)),
		Map.entry("Piscatoris teleport", Set.of(12408)),
		Map.entry("Tai bwo wannai teleport", Set.of(12409)),
		Map.entry("Iorwerth camp teleport", Set.of(12410)),
		Map.entry("Mos le'harmless teleport", Set.of(12411)),
		Map.entry("Lumberyard teleport", Set.of(12642)),
		Map.entry("Zul-andra teleport", Set.of(12938)),
		Map.entry("Key master teleport", Set.of(13249)),
		Map.entry("Revenant cave teleport", Set.of(21802)),
		Map.entry("Watson teleport", Set.of(23387)),
		Map.entry("Spider cave teleport", Set.of(29782)),
		Map.entry("Colossal wyrm teleport", Set.of(30040)),
		Map.entry("Chasm teleport", Set.of(30775)),
		Map.entry("Ectophial", Set.of(4251)),
		Map.entry("Royal seed pod", Set.of(19564)),
		Map.entry("Grand seed pod", Set.of(9469)),
		Map.entry("Skull sceptre", Set.of(9013, 21276)),
		Map.entry("Hallowed crystal shard", Set.of(24709)),
		Map.entry("Cowbell amulet", Set.of(33104)));

	private DirectItemTeleportPolicy() { }

	public static boolean isEligible(Transport row)
	{
		if (row == null || row.getType() != TransportType.TELEPORTATION_ITEM
			|| row.getOrigin() != null || row.getDestination() == null || row.getCurrencyAmount() != 0
			|| row.getItemIdRequirements() == null || row.getItemIdRequirements().isEmpty()) return false;
		Set<Integer> allowed = ITEMS.get(row.getDisplayInfo());
		return allowed != null && row.getItemIdRequirements().stream()
			.allMatch(group -> !group.isEmpty() && allowed.containsAll(group));
	}

	public static String action(Transport row)
	{
		if (!isEligible(row)) return null;
		String name = row.getDisplayInfo();
		if ("Varrock tablet".equals(name)) return "Varrock";
		if ("Watchtower tablet".equals(name)) return "Watchtower";
		if (name.endsWith("tablet")) return "Break";
		if (name.endsWith("teleport")) return "Teleport";
		switch (name)
		{
			case "Royal seed pod": return "Commune";
			case "Grand seed pod": return "Squash";
			case "Skull sceptre": return "Invoke";
			case "Hallowed crystal shard": return "Activate";
			case "Cowbell amulet": return "Ring";
			default: return "Teleport";
		}
	}
}
