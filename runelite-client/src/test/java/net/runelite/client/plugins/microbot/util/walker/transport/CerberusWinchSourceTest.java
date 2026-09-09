package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarPlayer;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CerberusWinchSourceTest
{
	@Test
	public void fiveCanonicalRowsCarryTheDeclarativeAccessRequirements()
	{
		List<Transport> rows = rows();
		assertEquals(5, rows.size());
		assertEquals(Set.of(
			"1292 1253 0>1240 1226 0", "1291 1253 0>1240 1226 0",
			"1309 1269 0>1304 1290 0", "1328 1253 0>1368 1226 0",
			"1329 1253 0>1368 1226 0"),
			rows.stream().map(CerberusWinchSourceTest::route).collect(Collectors.toSet()));
		for (Transport row : rows)
		{
			assertEquals("Turn", row.getAction());
			assertEquals("Iron Winch", row.getName());
			assertTrue(row.isMembers());
			assertFalse(row.isConsumable());
			assertEquals(1, row.getDuration());
			assertEquals(91, row.getSkillLevels()[Skill.SLAYER.ordinal()]);
			assertEquals(91, java.util.Arrays.stream(row.getSkillLevels()).sum());
			assertEquals(1, row.getVarplayers().size());
			TransportVarPlayer gate = row.getVarplayers().iterator().next();
			assertEquals(VarPlayerID.SLAYER_COUNT, gate.getVarplayerId());
			assertEquals(0, gate.getValue());
			assertEquals(TransportVarPlayer.Operator.GREATER_THAN, gate.getOperator());
			assertTrue(CerberusWinchPolicy.isEligible(row));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
		}
	}

	@Test
	public void accessRequiresBoostedLevelPositiveCountAndMatchingTask()
	{
		assertTrue(CerberusWinchPolicy.hasAccess(91, 1, "Hellhounds"));
		assertTrue(CerberusWinchPolicy.hasAccess(99, 12, "Cerberus"));
		assertFalse(CerberusWinchPolicy.hasAccess(90, 1, "Hellhounds"));
		assertFalse(CerberusWinchPolicy.hasAccess(91, 0, "Hellhounds"));
		assertFalse(CerberusWinchPolicy.hasAccess(91, 1, "Black demons"));
		assertFalse(CerberusWinchPolicy.hasAccess(91, 1, null));
	}

	@Test
	public void accessSnapshotResolvesOrdinaryHellhoundTask()
	{
		Client client = mock(Client.class);
		when(client.getBoostedSkillLevel(Skill.SLAYER)).thenReturn(91);
		when(client.getVarpValue(VarPlayerID.SLAYER_COUNT)).thenReturn(12);
		when(client.getVarpValue(VarPlayerID.SLAYER_TARGET)).thenReturn(41);
		when(client.getDBRowsByValue(DBTableID.SlayerTask.ID,
			DBTableID.SlayerTask.COL_ID, 0, 41)).thenReturn(List.of(7000));
		when(client.getDBTableField(7000, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0))
			.thenReturn(new Object[]{"Hellhounds"});
		CerberusWinchPolicy.AccessSnapshot snapshot =
			CerberusWinchPolicy.readAccessSnapshot(client);
		assertTrue(snapshot.isAvailable());
		assertEquals(41, snapshot.getTaskTargetId());
		assertEquals("Hellhounds", snapshot.getTaskName());
	}

	@Test
	public void accessSnapshotResolvesCerberusBossSubtype()
	{
		Client client = mock(Client.class);
		when(client.getBoostedSkillLevel(Skill.SLAYER)).thenReturn(96);
		when(client.getVarpValue(VarPlayerID.SLAYER_COUNT)).thenReturn(3);
		when(client.getVarpValue(VarPlayerID.SLAYER_TARGET)).thenReturn(98);
		when(client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID)).thenReturn(17);
		when(client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID,
			DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID, 0, 17))
			.thenReturn(List.of(6190));
		when(client.getDBTableField(6190, DBTableID.SlayerTaskSublist.COL_TASK, 0))
			.thenReturn(new Object[]{7001});
		when(client.getDBTableField(7001, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0))
			.thenReturn(new Object[]{"Cerberus"});
		CerberusWinchPolicy.AccessSnapshot snapshot =
			CerberusWinchPolicy.readAccessSnapshot(client);
		assertTrue(snapshot.isAvailable());
		assertEquals(17, snapshot.getBossTargetId());
		assertEquals("Cerberus", snapshot.getTaskName());
	}

	@Test
	public void winchRequiresTheExactLairLanding()
	{
		Transport row = rows().get(0);
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(),
			row.getDestination(), row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION,
			RouteInteraction.Status.AVAILABLE, row.getAction(), true, row.getObjectId(),
			row.getOrigin(), row.getDestination());
		CatalogTransition transition = new CatalogTransition(null, row.getOrigin(),
			row.getObjectId(), row.getAction(), row.getAction(), row.getOrigin(),
			row.getDestination());
		WorldPoint near = new WorldPoint(row.getDestination().getX() + 1,
			row.getDestination().getY(), row.getDestination().getPlane());
		assertEquals(RouteInteraction.Status.AVAILABLE,
			scanner.observePending(pending, near, edge -> transition, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(pending, row.getDestination(), edge -> transition, 13)
				.getStatus());
	}

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == CerberusWinchPolicy.OBJECT_ID)
			.collect(Collectors.toList());
	}

	private static String route(Transport row)
	{
		return point(row.getOrigin()) + ">" + point(row.getDestination());
	}

	private static String point(WorldPoint point)
	{
		return point.getX() + " " + point.getY() + " " + point.getPlane();
	}
}
