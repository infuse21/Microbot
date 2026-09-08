package net.runelite.client.plugins.microbot.shortestpath.pathfinder;

import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathConfig;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransportFeatureToggleTest
{
	@Test
	public void farmableSpiritTreeDestinationsDefaultOff()
	{
		ShortestPathConfig config = new ShortestPathConfig() { };

		assertFalse(config.spiritTreeEtceteria());
		assertFalse(config.spiritTreeBrimhaven());
		assertFalse(config.spiritTreePortSarim());
		assertFalse(config.spiritTreeHosidius());
		assertFalse(config.spiritTreeFarmingGuild());
	}

	@Test
	public void unsupportedRandomWildernessObelisksDefaultOff()
	{
		assertFalse(new ShortestPathConfig() { }.useWildernessObelisks());
	}

	@Test
	public void dedicatedTransportFamiliesRespectBothToggleStates() throws Exception
	{
		Map<TransportType, String> toggles = Map.ofEntries(
			Map.entry(TransportType.AGILITY_SHORTCUT, "useAgilityShortcuts"),
			Map.entry(TransportType.GRAPPLE_SHORTCUT, "useGrappleShortcuts"),
			Map.entry(TransportType.BOAT, "useBoats"),
			Map.entry(TransportType.CANOE, "useCanoes"),
			Map.entry(TransportType.CHARTER_SHIP, "useCharterShips"),
			Map.entry(TransportType.SHIP, "useShips"),
			Map.entry(TransportType.FAIRY_RING, "useFairyRings"),
			Map.entry(TransportType.GNOME_GLIDER, "useGnomeGliders"),
			Map.entry(TransportType.MINECART, "useMinecarts"),
			Map.entry(TransportType.NPC, "useNpcs"),
			Map.entry(TransportType.POH, "usePoh"),
			Map.entry(TransportType.QUETZAL, "useQuetzals"),
			Map.entry(TransportType.SPIRIT_TREE, "useSpiritTrees"),
			Map.entry(TransportType.TELEPORTATION_MINIGAME, "useTeleportationMinigames"),
			Map.entry(TransportType.TELEPORTATION_LEVER, "useTeleportationLevers"),
			Map.entry(TransportType.TELEPORTATION_PORTAL, "useTeleportationPortals"),
			Map.entry(TransportType.TELEPORTATION_SPELL, "useTeleportationSpells"),
			Map.entry(TransportType.MAGIC_CARPET, "useMagicCarpets"),
			Map.entry(TransportType.HOT_AIR_BALLOON, "useHotAirBalloons"),
			Map.entry(TransportType.MAGIC_MUSHTREE, "useMagicMushtrees"),
			Map.entry(TransportType.SEASONAL_TRANSPORT, "useSeasonalTransports"),
			Map.entry(TransportType.WILDERNESS_OBELISK, "useWildernessObelisks"));
		PathfinderConfig config = config();
		for (Map.Entry<TransportType, String> entry : toggles.entrySet())
		{
			Transport row = new Transport(new WorldPoint(3000, 3000, 0),
				new WorldPoint(3100, 3100, 0), "test", entry.getKey(), true, 5);
			assertToggle(config, row, entry.getValue());
		}
	}

	@Test
	public void redundantOrdinaryMushtreeRowsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> row.getType() == TransportType.TRANSPORT
				&& Set.of(30920, 30924).contains(row.getObjectId())));
	}

	@Test
	public void ordinaryFairyRingRowsCannotBypassTheNetworkToggle() throws Exception
	{
		assertShadowRows("Fairy ring", "useFairyRings", Set.of(12003, 12094), 2);
	}

	@Test
	public void isafdarLogsRespectAgilityToggle() throws Exception
	{
		assertShadowRows("Log balance", "useAgilityShortcuts", Set.of(3931, 3932, 3933), 6);
	}

	@Test
	public void neitiznotMineBridgeRespectsAgilityToggle() throws Exception
	{
		assertShadowRows("Rope bridge", "useAgilityShortcuts", Set.of(21314, 21315), 2);
	}

	@Test
	public void auditedOrdinaryShortcutsRespectAgilityToggle() throws Exception
	{
		assertShadowRows("Pipe", "useAgilityShortcuts", Set.of(21727), 2);
		assertShadowRows("Ropeswing", "useAgilityShortcuts", Set.of(23568, 23569), 2);
		assertShadowRows("Crevice", "useAgilityShortcuts",
			Set.of(15186, 15187, 15194, 15195), 4);
		assertShadowRows("Little crack", "useAgilityShortcuts", Set.of(26382), 2);
		assertShadowRows("Mausoleum Door", "useAgilityShortcuts", Set.of(38574), 1);
	}

	@Test
	public void auditedOrdinaryAgilityTraversalsRespectToggle() throws Exception
	{
		assertShadowRows("Bridge", "useAgilityShortcuts", Set.of(3522), 8);
		assertShadowRows("Climbing rocks", "useAgilityShortcuts", Set.of(11948, 11949), 14);
		assertShadowRows("Rocky handholds", "useAgilityShortcuts", Set.of(19846, 19847, 26405), 6);
		assertShadowRows("Tunnel", "useAgilityShortcuts", Set.of(30174), 2);
		assertShadowRows("Gap", "useAgilityShortcuts", Set.of(29326), 6);
	}

	@Test
	public void meiyerditchCourseTraversalsRespectToggle() throws Exception
	{
		assertShadowRows("Floorboards", "useAgilityShortcuts", Set.of(
			18070, 18071, 18072, 18073, 18089, 18090, 18093, 18094, 18097, 18098,
			18109, 18110, 18111, 18112, 18113, 18114, 18117, 18118), 18);
		assertShadowRows("Floor", "useAgilityShortcuts",
			Set.of(18129, 18130, 18132, 18133, 18135, 18136), 6);
		assertShadowRows("Floor", "useAgilityShortcuts", Set.of(18122, 18124), 7);
		assertShadowRows("Rock", "useAgilityShortcuts", Set.of(17958, 17959, 17960), 3);
		assertShadowRows("Wall rubble", "useAgilityShortcuts", Set.of(18037, 18038), 3);
		assertShadowRows("Wall", "useAgilityShortcuts", Set.of(18078, 18088), 4);
		assertShadowRows("Shelf", "useAgilityShortcuts", Set.of(
			18086, 18087, 18095, 18096, 18105, 18106, 18107, 18108), 8);
		assertShadowRows("Washing line", "useAgilityShortcuts", Set.of(18099, 18100), 2);
		assertShadowRows("Barricade", "useAgilityShortcuts", Set.of(18054), 2);
		assertShadowRows("Trapdoor tunnel", "useAgilityShortcuts", Set.of(18083), 1);
		assertShadowRows("Tunnel", "useAgilityShortcuts", Set.of(18085), 2);
		assertShadowRows("Wall", "useAgilityShortcuts", Set.of(39172, 39173), 2);
	}

	@Test
	public void revenantPillarsHaveOnlySkillGatedToggleAwareVariants() throws Exception
	{
		PathfinderConfig config = config();
		int found = 0;
		for (Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				if (row.getObjectId() != 31561 && row.getObjectId() != 31562) continue;
				assertEquals(31561, row.getObjectId());
				assertEquals(TransportType.AGILITY_SHORTCUT, row.getType());
				assertTrue(Set.of(65, 75, 89).contains(row.getSkillLevels()[net.runelite.api.Skill.AGILITY.ordinal()]));
				assertToggle(config, row, "useAgilityShortcuts");
				found++;
			}
		}
		assertEquals(10, found);
	}

	private static void assertShadowRows(String name, String toggle, Set<Integer> ids,
		int expected) throws Exception
	{
		PathfinderConfig config = config();
		int found = 0;
		for (Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				if (row.getType() == TransportType.TRANSPORT && name.equalsIgnoreCase(row.getName())
					&& ids.contains(row.getObjectId()))
				{
					assertToggle(config, row, toggle);
					found++;
				}
			}
		}
		assertEquals(expected, found);
	}

	private static void assertToggle(PathfinderConfig config, Transport row, String name) throws Exception
	{
		Field field = PathfinderConfig.class.getDeclaredField(name);
		field.setAccessible(true);
		Method enabled = PathfinderConfig.class.getDeclaredMethod("isFeatureEnabled", Transport.class);
		enabled.setAccessible(true);
		field.setBoolean(config, false);
		assertFalse(name + " must reject " + row.getType(), (boolean) enabled.invoke(config, row));
		field.setBoolean(config, true);
		assertTrue(name + " must admit " + row.getType(), (boolean) enabled.invoke(config, row));
	}

	private static PathfinderConfig config()
	{
		Client client = mock(Client.class);
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));
		return new PathfinderConfig(null, Collections.emptyMap(), Collections.emptyList(), client, null);
	}
}
