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
	public void ordinaryMushtreeRowsCannotBypassTheNetworkToggle() throws Exception
	{
		assertShadowRows("Magic Mushtree", "useMagicMushtrees", Set.of(30920, 30924), 12);
	}

	@Test
	public void ordinaryFairyRingRowsCannotBypassTheNetworkToggle() throws Exception
	{
		assertShadowRows("Fairy ring", "useFairyRings", Set.of(12003, 12094), 2);
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
