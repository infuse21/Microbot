package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Map;
import java.util.Set;

/** Exact audited inventory/submenu contracts; no generic activation or dialogue fallback. */
public final class ItemTeleportPolicy
{
	private static final Map<String, Set<Integer>> ITEMS = Map.ofEntries(
		Map.entry("Master Scroll Book", Set.of(21389)),
		Map.entry("Ardougne cloak", Set.of(13121, 13122, 13123, 13124, 20760)),
		Map.entry("Book of the dead", Set.of(25818)),
		Map.entry("Drakan's medallion", Set.of(22400)),
		Map.entry("Enchanted lyre", Set.of(3691, 6125, 6126, 6127, 13079)),
		Map.entry("Enchanted lyre(i)", Set.of(23458)),
		Map.entry("Eternal teleport crystal", Set.of(23946)),
		Map.entry("Icy basalt", Set.of(22599)),
		Map.entry("Karamja gloves", Set.of(11140, 13103)),
		Map.entry("Kharedst's memoirs", Set.of(21760)),
		Map.entry("Morytania legs", Set.of(13112, 13113, 13114, 13115)),
		Map.entry("Pharaoh's sceptre", Set.of(26948)),
		Map.entry("Rada's blessing", Set.of(22941, 22943, 22945, 22947)),
		Map.entry("Stony basalt", Set.of(22601)),
		Map.entry("Teleport crystal", Set.of(6099, 6100, 6101, 6102, 13102)),
		Map.entry("Varrock tablet", Set.of(8007)),
		Map.entry("Watchtower tablet", Set.of(8012)),
		Map.entry("Achievement diary cape", Set.of(13069, 19476)),
		Map.entry("Amulet of glory", Set.of(1706, 1708, 1710, 1712, 10354, 10356, 10358, 10360, 11964, 11966, 11976, 11978, 19707)),
		Map.entry("Amulet of the eye", Set.of(26914)),
		Map.entry("Combat bracelet", Set.of(11118, 11120, 11122, 11124, 11972, 11974)),
		Map.entry("Construction cape", Set.of(9789, 9790)),
		Map.entry("Crafting cape", Set.of(9780, 9781)),
		Map.entry("Desert amulet", Set.of(13134, 13135, 13136)),
		Map.entry("Digsite pendant", Set.of(11190, 11191, 11192, 11193, 11194)),
		Map.entry("Explorer's ring", Set.of(13126, 13127, 13128)),
		Map.entry("Farming cape", Set.of(9810, 9811)),
		Map.entry("Fishing cape", Set.of(9798, 9799)),
		Map.entry("Games necklace", Set.of(3853, 3855, 3857, 3859, 3861, 3863, 3865, 3867)),
		Map.entry("Giantsoul Amulet", Set.of(30638)),
		Map.entry("Hunter cape", Set.of(9948, 9949)),
		Map.entry("Music cape", Set.of(13221, 13222)),
		Map.entry("Necklace of passage", Set.of(21146, 21149, 21151, 21153, 21155)),
		Map.entry("Pendant of ates", Set.of(29893)),
		Map.entry("Quest point cape", Set.of(9813, 13068)),
		Map.entry("Ring of dueling", Set.of(2552, 2554, 2556, 2558, 2560, 2562, 2564, 2566)),
		Map.entry("Ring of shadows", Set.of(28327)),
		Map.entry("Ring of the elements", Set.of(26818)),
		Map.entry("Ring of wealth", Set.of(11980, 11982, 11984, 11986, 11988, 20786, 20787, 20788, 20789, 20790)),
		Map.entry("Sailors' amulet", Set.of(32399)),
		Map.entry("Skills necklace", Set.of(11105, 11107, 11109, 11111, 11968, 11970)),
		Map.entry("Slayer ring", Set.of(11866, 11867, 11868, 11869, 11870, 11871, 11872, 11873, 21268)),
		Map.entry("Strength cape", Set.of(9750, 9751)),
		Map.entry("Xeric's talisman", Set.of(13393)));
	private static final Map<String, String> ACTIONS = Map.ofEntries(
		Map.entry("Ardougne cloak: Monastery", "Monastery Teleport"),
		Map.entry("Book of the dead: A Dark Disposition", "A Dark Disposition"),
		Map.entry("Book of the dead: History and Hearsay", "History and Hearsay"),
		Map.entry("Book of the dead: Jewellery of Jubilation", "Jewellery of Jubilation"),
		Map.entry("Book of the dead: Lunch by the Lancalliums", "Lunch by the Lancalliums"),
		Map.entry("Book of the dead: The Fisher's Flute", "The Fisher's Flute"),
		Map.entry("Drakan's medallion: Darkmeyer", "Darkmeyer"),
		Map.entry("Drakan's medallion: Ver Sinhaza", "Ver Sinhaza"),
		Map.entry("Enchanted lyre: Jatizso", "Jatiszo"),
		Map.entry("Enchanted lyre: Neitiznot", "Neitiznot"),
		Map.entry("Enchanted lyre: Rellekka", "Rellekka"),
		Map.entry("Enchanted lyre: Waterbirth Island", "Waterbirth Island"),
		Map.entry("Enchanted lyre(i): Jatizso", "Jatiszo"),
		Map.entry("Enchanted lyre(i): Neitiznot", "Neitiznot"),
		Map.entry("Enchanted lyre(i): Rellekka", "Rellekka"),
		Map.entry("Enchanted lyre(i): Waterbirth Island", "Waterbirth Island"),
		Map.entry("Eternal teleport crystal: Lletya", "Lletya"),
		Map.entry("Eternal teleport crystal: Prifddinas", "Prifddinas"),
		Map.entry("Icy basalt: Weiss", "Weiss"),
		Map.entry("Karamja gloves: Gem Mine", "Gem Mine"),
		Map.entry("Karamja gloves: Slayer Master", "Slayer Master"),
		Map.entry("Kharedst's memoirs: A Dark Disposition", "A Dark Disposition"),
		Map.entry("Kharedst's memoirs: History and Hearsay", "History and Hearsay"),
		Map.entry("Kharedst's memoirs: Jewellery of Jubilation", "Jewellery of Jubilation"),
		Map.entry("Kharedst's memoirs: Lunch by the Lancalliums", "Lunch by the Lancalliums"),
		Map.entry("Kharedst's memoirs: The Fisher's Flute", "The Fisher's Flute"),
		Map.entry("Morytania legs: Burgh Teleport", "Burgh Teleport"),
		Map.entry("Morytania legs: Ecto Teleport", "Ecto Teleport"),
		Map.entry("Pharaoh's sceptre: Jaldraocht", "Jaldraocht"),
		Map.entry("Pharaoh's sceptre: Jaleustrophos", "Jaleustrophos"),
		Map.entry("Pharaoh's sceptre: Jalsavrah", "Jalsavrah"),
		Map.entry("Rada's blessing: Kourend Woodland", "Kourend Woodland"),
		Map.entry("Rada's blessing: Mount Karuulm", "Mount Karuulm"),
		Map.entry("Stony basalt: Troll Stronghold", "Troll Stronghold"),
		Map.entry("Teleport crystal: Lletya", "Lletya"),
		Map.entry("Teleport crystal: Prifddinas", "Prifddinas"),
		Map.entry("Varrock tablet: Grand exchange", "Grand Exchange"),
		Map.entry("Watchtower tablet: Yanille", "Yanille"),
		Map.entry("Achievement diary cape: Elder Gnome child", "Western Provinces"),
		Map.entry("Achievement diary cape: Elise", "Kourend & Kebos"),
		Map.entry("Achievement diary cape: Flax keeper", "Kandarin"),
		Map.entry("Achievement diary cape: Hatius Cosaintus", "Lumbridge & Draynor"),
		Map.entry("Achievement diary cape: Jarr", "Desert"),
		Map.entry("Achievement diary cape: Le-sabrè", "Morytania"),
		Map.entry("Achievement diary cape: Lesser Fanatic", "Wilderness"),
		Map.entry("Achievement diary cape: Pirate Jackie the Fruit", "Karamja"),
		Map.entry("Achievement diary cape: Sir Rebral", "Falador"),
		Map.entry("Achievement diary cape: Thorodin", "Fremennik"),
		Map.entry("Achievement diary cape: Toby", "Varrock"),
		Map.entry("Achievement diary cape: Twiggy O'Korn", "Twiggy O'Korn"),
		Map.entry("Achievement diary cape: Two-pints", "Ardougne"),
		Map.entry("Amulet of glory: Al Kharid", "Al Kharid"),
		Map.entry("Amulet of glory: Draynor Village", "Draynor Village"),
		Map.entry("Amulet of glory: Edgeville", "Edgeville"),
		Map.entry("Amulet of glory: Karamja", "Karamja"),
		Map.entry("Amulet of the eye: Teleport", "Teleport"),
		Map.entry("Combat bracelet: Champions' Guild", "Champions' Guild"),
		Map.entry("Combat bracelet: Monastery", "Monastery"),
		Map.entry("Combat bracelet: Ranging Guild", "Ranging Guild"),
		Map.entry("Combat bracelet: Warriors' Guild", "Warriors' Guild"),
		Map.entry("Construction cape: Aldarin", "Aldarin"),
		Map.entry("Construction cape: Brimhaven", "Brimhaven"),
		Map.entry("Construction cape: Hosidius", "Hosidius"),
		Map.entry("Construction cape: Pollnivneach", "Pollnivneach"),
		Map.entry("Construction cape: Prifddinas", "Prifddinas"),
		Map.entry("Construction cape: Rellekka", "Rellekka"),
		Map.entry("Construction cape: Rimmington", "Rimmington"),
		Map.entry("Construction cape: Taverley", "Taverley"),
		Map.entry("Construction cape: Yanille", "Yanille"),
		Map.entry("Crafting cape: Teleport", "Teleport"),
		Map.entry("Desert amulet: Kalphite Cave", "Kalphite Cave"),
		Map.entry("Desert amulet: Nardah", "Nardah"),
		Map.entry("Desert amulet: Teleport", "Teleport"),
		Map.entry("Digsite pendant: Digsite", "Digsite"),
		Map.entry("Digsite pendant: Fossil Island", "Fossil Island"),
		Map.entry("Digsite pendant: Lithkren", "Lithkren Dungeon"),
		Map.entry("Explorer's ring: Teleport", "Teleport"),
		Map.entry("Farming cape: Teleport", "Teleport"),
		Map.entry("Fishing cape: Fishing Guild", "Fishing Guild"),
		Map.entry("Fishing cape: Otto's Grotto", "Otto's Grotto"),
		Map.entry("Games necklace: Barbarian Outpost", "Barbarian Outpost"),
		Map.entry("Games necklace: Burthorpe", "Burthorpe"),
		Map.entry("Games necklace: Corporeal Beast", "Corporeal Beast"),
		Map.entry("Games necklace: Tears of Guthix", "Tears of Guthix"),
		Map.entry("Games necklace: Wintertodt Camp", "Wintertodt Camp"),
		Map.entry("Giantsoul Amulet: Branda and Eldric", "Branda and Eldric"),
		Map.entry("Giantsoul Amulet: Bryophyta", "Bryophyta"),
		Map.entry("Giantsoul Amulet: Obor", "Obor"),
		Map.entry("Hunter cape: Feldip Hills", "Carnivorous Chinchompas"),
		Map.entry("Music cape: Teleport", "Teleport"),
		Map.entry("Necklace of passage: Eagles' Eyrie", "Eagles' Eyrie"),
		Map.entry("Necklace of passage: The Outpost", "The Outpost"),
		Map.entry("Necklace of passage: Wizards' Tower", "Wizards' Tower"),
		Map.entry("Pendant of ates: Darkfrost", "Darkfrost"),
		Map.entry("Pendant of ates: Kastori", "Kastori"),
		Map.entry("Pendant of ates: Nemus Retreat", "Nemus Retreat"),
		Map.entry("Pendant of ates: North Aldarin", "North Aldarin"),
		Map.entry("Pendant of ates: Ralos' Rise", "Ralos' Rise"),
		Map.entry("Pendant of ates: Twilight Temple", "Twilight Temple"),
		Map.entry("Quest point cape: Teleport", "Teleport"),
		Map.entry("Ring of dueling: Castle Wars", "Castle Wars"),
		Map.entry("Ring of dueling: Emir's Arena", "Emir's Arena"),
		Map.entry("Ring of dueling: Ferox Enclave", "Ferox Enclave"),
		Map.entry("Ring of shadows: Ancient Vault", "The Ancient Vault"),
		Map.entry("Ring of shadows: Ghorrock Dungeon", "Ghorrock Dungeon"),
		Map.entry("Ring of shadows: Lassar Undercity", "Lassar Undercity"),
		Map.entry("Ring of shadows: The Scar", "The Scar"),
		Map.entry("Ring of shadows: The Stranglewood", "The Stranglewood"),
		Map.entry("Ring of the elements: Air Altar", "Air Altar"),
		Map.entry("Ring of the elements: Earth Altar", "Earth Altar"),
		Map.entry("Ring of the elements: Fire Altar", "Fire Altar"),
		Map.entry("Ring of the elements: Water Altar", "Water Altar"),
		Map.entry("Ring of wealth: Dondakan", "Dondakan"),
		Map.entry("Ring of wealth: Falador", "Falador"),
		Map.entry("Ring of wealth: Grand Exchange", "Grand Exchange"),
		Map.entry("Ring of wealth: Miscellania", "Miscellania"),
		Map.entry("Sailors' amulet: Deepfin Point", "Deepfin Point"),
		Map.entry("Sailors' amulet: Port Roberts", "Port Roberts"),
		Map.entry("Sailors' amulet: The Pandemonium", "The Pandemonium"),
		Map.entry("Skills necklace: Cooking Guild", "Cooking Guild"),
		Map.entry("Skills necklace: Crafting Guild", "Crafting Guild"),
		Map.entry("Skills necklace: Farming Guild", "Farming Guild"),
		Map.entry("Skills necklace: Fishing Guild", "Fishing Guild"),
		Map.entry("Skills necklace: Mining Guild", "Mining Guild"),
		Map.entry("Skills necklace: Woodcutting Guild", "Woodcutting Guild"),
		Map.entry("Slayer ring: Dark Beasts", "Dark Beasts"),
		Map.entry("Slayer ring: Fremennik", "Fremennik Dungeon"),
		Map.entry("Slayer ring: Slayer Tower", "Slayer Tower"),
		Map.entry("Slayer ring: Stronghold Slayer Cave", "Stronghold"),
		Map.entry("Slayer ring: Tarn's Lair", "Tarn's Lair"),
		Map.entry("Strength cape: Warriors' Guild", "Teleport"),
		Map.entry("Xeric's talisman: Xeric's Glade", "Xeric's Glade"),
		Map.entry("Xeric's talisman: Xeric's Heart", "Xeric's Heart"),
		Map.entry("Xeric's talisman: Xeric's Inferno", "Xeric's Inferno"),
		Map.entry("Xeric's talisman: Xeric's Lookout", "Xeric's Lookout"));
	private static final Map<String, Integer> MASTER_SCROLL_BOOK_WIDGETS = Map.ofEntries(
		Map.entry("Master Scroll Book: Nardah", InterfaceID.Bookofscrolls.TELEPORTSCROLL_NARDAH),
		Map.entry("Master Scroll Book: Digsite", InterfaceID.Bookofscrolls.TELEPORTSCROLL_DIGSITE),
		Map.entry("Master Scroll Book: Feldip hills", InterfaceID.Bookofscrolls.TELEPORTSCROLL_FELDIP),
		Map.entry("Master Scroll Book: Lunar isle", InterfaceID.Bookofscrolls.TELEPORTSCROLL_LUNARISLE),
		Map.entry("Master Scroll Book: Mort'ton", InterfaceID.Bookofscrolls.TELEPORTSCROLL_MORTTON),
		Map.entry("Master Scroll Book: Pest control", InterfaceID.Bookofscrolls.TELEPORTSCROLL_PESTCONTROL),
		Map.entry("Master Scroll Book: Piscatoris", InterfaceID.Bookofscrolls.TELEPORTSCROLL_PISCATORIS),
		Map.entry("Master Scroll Book: Tai bwo wannai", InterfaceID.Bookofscrolls.TELEPORTSCROLL_TAIBWO),
		Map.entry("Master Scroll Book: Iorwerth camp", InterfaceID.Bookofscrolls.TELEPORTSCROLL_ELF),
		Map.entry("Master Scroll Book: Mos le'harmless", InterfaceID.Bookofscrolls.TELEPORTSCROLL_MOSLES),
		Map.entry("Master Scroll Book: Lumberyard", InterfaceID.Bookofscrolls.TELEPORTSCROLL_LUMBERYARD),
		Map.entry("Master Scroll Book: Zul-andra", InterfaceID.Bookofscrolls.TELEPORTSCROLL_ZULANDRA),
		Map.entry("Master Scroll Book: Key master", InterfaceID.Bookofscrolls.TELEPORTSCROLL_CERBERUS),
		Map.entry("Master Scroll Book: Revenant cave", InterfaceID.Bookofscrolls.TELEPORTSCROLL_REVENANTS),
		Map.entry("Master Scroll Book: Watson", InterfaceID.Bookofscrolls.TELEPORTSCROLL_WATSON),
		Map.entry("Master Scroll Book: Spider cave", InterfaceID.Bookofscrolls.TELEPORTSCROLL_SPIDERCAVE),
		Map.entry("Master Scroll Book: Colossal wyrm", InterfaceID.Bookofscrolls.TELEPORTSCROLL_COLOSSAL_WYRM),
		Map.entry("Master Scroll Book: Chasm of fire", InterfaceID.Bookofscrolls.TELEPORTSCROLL_CHASMOFFIRE));

	private ItemTeleportPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TELEPORTATION_ITEM
			|| transport.getOrigin() != null || transport.getDestination() == null
			|| transport.getCurrencyAmount() != 0 || transport.getItemIdRequirements().isEmpty()
			|| (!ACTIONS.containsKey(transport.getDisplayInfo())
				&& !MASTER_SCROLL_BOOK_WIDGETS.containsKey(transport.getDisplayInfo())))
		{
			return false;
		}
		Set<Integer> ids = ITEMS.get(transport.getDisplayInfo().split(":", 2)[0]);
		if ("Stony basalt: Troll Stronghold".equals(transport.getDisplayInfo())
			&& !transport.getDestination().equals(new WorldPoint(2845, 3694, 0))
			&& !transport.getDestination().equals(new WorldPoint(2837, 3695, 0)))
		{
			return false;
		}
		return transport.getItemIdRequirements().stream()
			.allMatch(group -> !group.isEmpty() && ids.containsAll(group));
	}

	public static String inventoryAction(Transport transport)
	{
		if (!isEligible(transport))
		{
			return null;
		}
		if ("Stony basalt: Troll Stronghold".equals(transport.getDisplayInfo()))
		{
			return transport.getDestination().equals(new WorldPoint(2837, 3695, 0))
				? "Troll Stronghold roof" : "Troll Stronghold entrance";
		}
		return isMasterScrollBook(transport) ? "Open" : ACTIONS.get(transport.getDisplayInfo());
	}

	public static boolean isMasterScrollBook(Transport transport)
	{
		return transport != null && MASTER_SCROLL_BOOK_WIDGETS.containsKey(transport.getDisplayInfo());
	}

	public static int masterScrollBookWidget(Transport transport)
	{
		return isMasterScrollBook(transport) ? MASTER_SCROLL_BOOK_WIDGETS.get(transport.getDisplayInfo()) : -1;
	}

	public static String destination(Transport transport)
	{
		return isMasterScrollBook(transport)
			? transport.getDisplayInfo().substring("Master Scroll Book: ".length()) : null;
	}

	public static String equipmentAction(Transport transport)
	{
		if (!isEligible(transport))
		{
			return null;
		}
		if ("Strength cape: Warriors' Guild".equals(transport.getDisplayInfo()))
		{
			return "Warriors' Guild";
		}
		if ("Ring of shadows: Ancient Vault".equals(transport.getDisplayInfo()))
		{
			return "Ancient Vault";
		}
		String family = transport.getDisplayInfo().split(":", 2)[0];
		switch (family)
		{
			case "Master Scroll Book":
			case "Teleport crystal":
			case "Eternal teleport crystal":
			case "Icy basalt":
			case "Stony basalt":
			case "Varrock tablet":
			case "Watchtower tablet":
				return null;
			case "Ardougne cloak":
				return "Kandarin Monastery";
			case "Morytania legs":
				return "Morytania legs: Ecto Teleport".equals(transport.getDisplayInfo())
					? "Ectofuntus Pit" : "Burgh de Rott";
			default:
				return inventoryAction(transport);
		}
	}
}
