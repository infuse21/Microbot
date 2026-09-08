package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.util.walker.transport.ItemTeleportPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.ItemTeleportRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.SimpleTeleportPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.model.ItemTeleport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PohOutsideTabletTransportTest
{
	private static final String DISPLAY = "Teleport to House tablet: Outside";
	private static final Map<Integer, WorldPoint> HOUSE_EXTERIORS = Map.of(
		1, new WorldPoint(2952, 3224, 0),
		2, new WorldPoint(2892, 3465, 0),
		3, new WorldPoint(3339, 3001, 0),
		4, new WorldPoint(2669, 3629, 0),
		5, new WorldPoint(2756, 3176, 0),
		6, new WorldPoint(2545, 3097, 0),
		7, new WorldPoint(3239, 6077, 0),
		8, new WorldPoint(1740, 3517, 0));

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> DISPLAY.equals(row.getDisplayInfo())).collect(Collectors.toList());
	}

	@Test
	public void eightHouseLocationsUseOnlyTheExactOutsideAction()
	{
		List<Transport> rows = rows();
		assertEquals(8, rows.size());
		for (Transport row : rows)
		{
			assertTrue(row.isMembers());
			assertTrue(row.isConsumable());
			assertEquals(19, row.getMaxWildernessLevel());
			assertEquals(4, row.getDuration());
			assertEquals(Set.of(Set.of(8013)), row.getItemIdRequirements());
			assertTrue(row.getQuests().isEmpty());
			assertTrue(row.getVarplayers().isEmpty());
			assertTrue(java.util.Arrays.stream(row.getSkillLevels()).allMatch(level -> level == 0));
			assertEquals(1, row.getVarbits().size());
			TransportVarbit gate = row.getVarbits().iterator().next();
			assertEquals(2187, gate.getVarbitId());
			assertEquals(TransportVarbit.Operator.EQUAL, gate.getOperator());
			assertEquals(HOUSE_EXTERIORS.get(gate.getValue()), row.getDestination());
			assertTrue(ItemTeleportPolicy.isEligible(row));
			assertFalse(SimpleTeleportPolicy.isEligible(row));
			assertEquals("Outside", ItemTeleportPolicy.inventoryAction(row));
			assertNull(ItemTeleportPolicy.equipmentAction(row));
			assertEquals(RouteEdge.Kind.ITEM_TELEPORT,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
		}
	}

	@Test
	public void consumedTabletRetainsOwnershipUntilItsExteriorLanding()
	{
		Transport row = rows().get(0);
		WorldPoint from = new WorldPoint(3200, 3200, 0);
		WorldPoint to = row.getDestination();
		ItemTeleportRouteScanner scanner = new ItemTeleportRouteScanner();
		RouteInteraction use = new RouteInteraction(1, 0, from, to, from,
			RouteInteraction.Kind.ITEM_TELEPORT, RouteInteraction.Status.AVAILABLE,
			"item-use:inventory:Outside", true, 8013, from, to);
		assertEquals(RouteInteraction.Status.AVAILABLE,
			scanner.observePending(use, from, edge -> null).getStatus());
		WorldPoint wrongExterior = HOUSE_EXTERIORS.entrySet().stream()
			.filter(entry -> !entry.getValue().equals(to)).map(Map.Entry::getValue)
			.findFirst().orElseThrow(AssertionError::new);
		assertEquals(RouteInteraction.Status.AVAILABLE,
			scanner.observePending(use, wrongExterior, edge -> null).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(use, to, edge -> null).getStatus());
		ItemTeleport observed = new ItemTeleport(8013, "Outside", false, true);
		assertEquals("item-use:inventory:Outside", observed.command());
	}
}
