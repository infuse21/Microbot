package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SaradominRopeDescentTransportTest
{
	@Test
	public void eachDescentHasSetupAndInstalledVariants()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(row -> row.getObjectId() == 26561 || row.getObjectId() == 26562)
			.collect(Collectors.toList());
		assertEquals(4, rows.size());
		for (int objectId : Set.of(26561, 26562))
		{
			List<Transport> variants = rows.stream().filter(row -> row.getObjectId() == objectId)
				.collect(Collectors.toList());
			assertEquals(2, variants.size());
			assertTrue(variants.stream().allMatch(CatalogTransitionPolicy::isSaradominRopeDescent));
			assertTrue(variants.stream().allMatch(row ->
				row.getSkillLevels()[Skill.AGILITY.ordinal()] == 70));
			Transport setup = variants.stream().filter(Transport::isConsumable).findFirst().orElseThrow();
			assertEquals(Set.of(Set.of(954)), setup.getItemIdRequirements());
			assertTrue(Rs2CatalogTransitionScene.requiresRopePreparation(setup, "Use rope"));
			assertTrue(variants.stream().anyMatch(row -> !row.isConsumable()
				&& row.getItemIdRequirements().isEmpty()));
		}
	}

	@Test
	public void descentRequiresExactDirectedLanding()
	{
		Transport row = Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(CatalogTransitionPolicy::isSaradominRopeDescent).findFirst().orElseThrow();
		RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
			row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION,
			RouteInteraction.Status.AVAILABLE, row.getAction(), true, row.getObjectId(),
			row.getOrigin(), row.getDestination());
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		WorldPoint near = new WorldPoint(row.getDestination().getX() + 1,
			row.getDestination().getY(), row.getDestination().getPlane());
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, near, edge -> null, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(pending, row.getDestination(), edge -> null, 13).getStatus());
	}
}
