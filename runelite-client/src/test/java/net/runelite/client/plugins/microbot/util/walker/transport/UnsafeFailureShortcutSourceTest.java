package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UnsafeFailureShortcutSourceTest
{
	private static final Set<Integer> OBJECT_IDS = Set.of(4615, 4616, 23644);

	@Test
	public void failureProneRowsHaveAnExactRetrySafeContract()
	{
		List<Transport> rows = rows();
		assertEquals(4, rows.size());
		assertEquals(Set.of(
			"2598 3608 0>2596 3608 0", "2596 3608 0>2598 3608 0",
			"2910 3049 0>2906 3049 0", "2906 3049 0>2910 3049 0"),
			rows.stream().map(row -> point(row.getOrigin()) + ">" + point(row.getDestination()))
				.collect(Collectors.toSet()));
		for (Transport row : rows)
		{
			assertTrue(row.isMembers());
			assertTrue(onlyAgility(row, 1));
			assertTrue(row.getQuests().isEmpty());
			assertTrue(row.getVarbits().isEmpty());
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertEquals(row.getObjectId() == 23644 ? 5 : 2, row.getDuration());
			assertTrue(CatalogTransitionPolicy.isFailureRetryShortcut(row));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
		}
	}

	@Test
	public void failedAttemptAtTheSourceDoesNotClearPendingInteraction()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows())
		{
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(),
				row.getDestination(), row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION,
				RouteInteraction.Status.AVAILABLE, row.getAction(), true, row.getObjectId(),
				row.getOrigin(), row.getDestination());
			CatalogTransition stage = new CatalogTransition(null, row.getOrigin(),
				row.getObjectId(), row.getAction(), row.getAction(), row.getOrigin(),
				row.getDestination());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, row.getOrigin(), edge -> stage, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> stage, 13).getStatus());
		}
	}

	@Test
	public void mutatedFailureShortcutIsNotEngineOwned()
	{
		Transport row = rows().get(0);
		row.getSkillLevels()[Skill.AGILITY.ordinal()] = 2;
		assertFalse(CatalogTransitionPolicy.isFailureRetryShortcut(row));
		assertFalse(CatalogTransitionPolicy.isEligible(row));
	}

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> OBJECT_IDS.contains(row.getObjectId()))
			.collect(Collectors.toList());
	}

	private static boolean onlyAgility(Transport transport, int agility)
	{
		int[] levels = transport.getSkillLevels();
		for (int i = 0; i < levels.length; i++)
		{
			if (levels[i] != (i == Skill.AGILITY.ordinal() ? agility : 0))
			{
				return false;
			}
		}
		return true;
	}

	private static String point(WorldPoint point)
	{
		return point.getX() + " " + point.getY() + " " + point.getPlane();
	}
}
