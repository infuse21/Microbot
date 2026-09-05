package net.runelite.client.plugins.microbot.shortestpath.pathfinder;

import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FossilRowboatGateTest
{
	private static final WorldPoint CAMP = new WorldPoint(3724, 3807, 0);
	private static final WorldPoint NORTH = new WorldPoint(3734, 3893, 0);
	private static final WorldPoint SEA = new WorldPoint(3763, 3899, 0);
	private static final List<String> LOCKED = List.of("Row to the barge.",
		"Row to the barge and travel to the Digsite.", "Cancel.");

	@Test
	public void lockedMenuExcludesBothCampRoutesButNotDiscoveryDirections() throws Exception
	{
		PathfinderConfig config = config();
		Field boats = PathfinderConfig.class.getDeclaredField("useBoats");
		boats.setAccessible(true);
		boats.setBoolean(config, true);
		assertTrue(config.recordFossilRowboatMenu(CAMP, LOCKED));
		assertFalse(config.recordFossilRowboatMenu(CAMP, LOCKED));
		assertFalse(config.isFossilRowboatRouteEnabled(CAMP, NORTH));
		assertFalse(config.isFossilRowboatRouteEnabled(CAMP, SEA));
		assertTrue(config.isFossilRowboatRouteEnabled(NORTH, SEA));
		assertTrue(config.isFossilRowboatRouteEnabled(NORTH, CAMP));
		assertTrue(config.isFossilRowboatRouteEnabled(CAMP, new WorldPoint(3362, 3445, 0)));
		Method usable = PathfinderConfig.class.getDeclaredMethod("useTransport", Transport.class);
		usable.setAccessible(true);
		int checked = 0;
		for (Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				if (row.getType() == TransportType.BOAT && row.getObjectId() == 30914)
				{
					assertFalse((boolean) usable.invoke(config, row));
					checked++;
				}
			}
		}
		assertEquals(2, checked);
	}

	@Test
	public void laterMenuCanUnlockEitherDestinationAndLoginResetForgetsLocks()
	{
		PathfinderConfig config = config();
		assertTrue(config.isFossilRowboatRouteEnabled(CAMP, NORTH));
		config.recordFossilRowboatMenu(CAMP, LOCKED);
		assertTrue(config.recordFossilRowboatMenu(CAMP, List.of(LOCKED.get(0), LOCKED.get(1),
			"Row to the north of the island.", "Cancel.")));
		assertTrue(config.isFossilRowboatRouteEnabled(CAMP, NORTH));
		assertFalse(config.isFossilRowboatRouteEnabled(CAMP, SEA));
		assertTrue(config.recordFossilRowboatMenu(CAMP, List.of(LOCKED.get(0), LOCKED.get(1),
			"Row out to sea.", "Cancel.")));
		assertFalse(config.isFossilRowboatRouteEnabled(CAMP, NORTH));
		assertTrue(config.isFossilRowboatRouteEnabled(CAMP, SEA));
		config.clearFossilRowboatMenu();
		assertTrue(config.isFossilRowboatRouteEnabled(CAMP, NORTH));
		assertTrue(config.isFossilRowboatRouteEnabled(CAMP, SEA));
	}

	@Test
	public void foreignPartialAndRemoteMenusCannotChangeLocks()
	{
		PathfinderConfig config = config();
		assertFalse(config.recordFossilRowboatMenu(CAMP, List.of("Yes.", "No.")));
		assertFalse(config.recordFossilRowboatMenu(CAMP, LOCKED.subList(0, 2)));
		assertFalse(config.recordFossilRowboatMenu(NORTH, LOCKED));
		assertFalse(config.recordFossilRowboatMenu(new WorldPoint(3724, 3807, 1), LOCKED));
		assertFalse(config.recordFossilRowboatMenu(null, LOCKED));
		assertFalse(config.recordFossilRowboatMenu(CAMP, null));
		assertTrue(config.isFossilRowboatRouteEnabled(CAMP, NORTH));
	}

	@Test
	public void unlockingDoesNotBypassDisabledBoats() throws Exception
	{
		PathfinderConfig config = config();
		config.recordFossilRowboatMenu(CAMP, List.of(LOCKED.get(0), LOCKED.get(1),
			"Row to the north of the island.", "Row out to sea.", "Cancel."));
		Field flag = PathfinderConfig.class.getDeclaredField("useBoats");
		flag.setAccessible(true);
		flag.setBoolean(config, false);
		Method usable = PathfinderConfig.class.getDeclaredMethod("useTransport", Transport.class);
		usable.setAccessible(true);
		Transport row = new Transport(CAMP, NORTH, "test", TransportType.BOAT, true, 4);
		assertFalse((boolean) usable.invoke(config, row));
	}

	private static PathfinderConfig config()
	{
		Client client = mock(Client.class);
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));
		return new PathfinderConfig(null, Collections.emptyMap(), Collections.emptyList(), client, null);
	}
}
