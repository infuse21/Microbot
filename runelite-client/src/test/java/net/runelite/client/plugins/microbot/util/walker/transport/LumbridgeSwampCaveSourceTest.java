package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LumbridgeSwampCaveSourceTest
{
	@Test
	public void eachEntranceHasSetupInstalledLightAndPermanentFireVariants()
	{
		List<Transport> rows = rows();
		assertEquals(16, rows.size());
		Map<WorldPoint, List<Transport>> byOrigin = rows.stream()
			.collect(Collectors.groupingBy(Transport::getOrigin));
		assertEquals(4, byOrigin.size());
		assertTrue(byOrigin.values().stream().allMatch(variants -> variants.size() == 4));
		assertEquals(8, rows.stream().filter(LumbridgeSwampCavePolicy::requiresLight).count());

		for (Transport row : rows)
		{
			assertEquals("Climb-down", row.getAction());
			assertEquals("Dark hole", row.getName());
			assertTrue(row.isMembers());
			assertEquals(2, row.getDuration());
			assertEquals(2, row.getVarbits().size());
			assertTrue(hasVarbit(row, VarbitID.SWAMP_CAVES_ROPED_ENTRANCE,
				row.isConsumable() ? 0 : 1));
			assertTrue(hasVarbit(row, VarbitID.MY2ARM_FIRE_LUMB,
				LumbridgeSwampCavePolicy.requiresLight(row) ? 0 : 1));
			assertTrue(LumbridgeSwampCavePolicy.isEligible(row));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			if (row.isConsumable())
			{
				assertEquals(Set.of(Set.of(ItemID.ROPE)), row.getItemIdRequirements());
				assertTrue(LumbridgeSwampCavePolicy.isRopeSetup(row));
				assertTrue(Rs2CatalogTransitionScene.requiresRopePreparation(row, "Use rope"));
			}
		}
	}

	@Test
	public void onlyGasSafeLightsQualifyForTheBankableVariant()
	{
		Transport lightRequired = rows().stream()
			.filter(LumbridgeSwampCavePolicy::requiresLight)
			.findFirst().orElseThrow(() -> new AssertionError("light-required variant missing"));
		Set<Integer> lights = LumbridgeSwampCavePolicy.gasSafeLightSourceIds(lightRequired);
		assertTrue(lights.contains(ItemID.CANDLE_LANTERN_LIT));
		assertTrue(lights.contains(ItemID.BULLSEYE_LANTERN_LIT));
		assertFalse(lights.contains(ItemID.TORCH_LIT));
		assertFalse(lights.contains(ItemID.LIT_CANDLE));
		assertFalse(lights.contains(ItemID.LIT_BLACK_CANDLE));
		assertFalse(lights.contains(ItemID.OIL_LAMP_LIT));

		Transport permanentFire = rows().stream()
			.filter(row -> !LumbridgeSwampCavePolicy.requiresLight(row))
			.findFirst().orElseThrow(() -> new AssertionError("permanent-fire variant missing"));
		assertTrue(LumbridgeSwampCavePolicy.gasSafeLightSourceIds(permanentFire).isEmpty());
	}

	@Test
	public void entranceClearsOnlyAtItsExactUndergroundLanding()
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

	private static boolean hasVarbit(Transport row, int id, int value)
	{
		return row.getVarbits().stream().anyMatch(gate -> gate.getVarbitId() == id
			&& gate.getValue() == value && gate.getOperator() == TransportVarbit.Operator.EQUAL);
	}

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == LumbridgeSwampCavePolicy.DARK_HOLE_ID)
			.collect(Collectors.toList());
	}
}
