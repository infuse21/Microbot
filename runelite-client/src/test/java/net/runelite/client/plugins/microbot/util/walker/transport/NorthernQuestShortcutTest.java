package net.runelite.client.plugins.microbot.util.walker.transport;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NorthernQuestShortcutTest
{
	static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(row -> NorthernQuestShortcutPolicy.ownsObject(row.getObjectId())).collect(Collectors.toList());
	}

	@Test
	public void fourteenExactDirectionsPinRequirementsAndLanding()
	{
		List<Transport> rows = rows();
		assertEquals(14, rows.size());
		assertEquals(10, rows.stream().filter(NorthernQuestShortcutPolicy::isWeiss).count());
		for (Transport row : rows)
		{
			NorthernQuestShortcutPolicy.Entry entry = NorthernQuestShortcutPolicy.entry(row);
			assertNotNull(entry);
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertFalse(AdjacentTransportPolicy.isEligible(row));
			assertEquals(entry.ascending ? 68 : 0, row.getSkillLevels()[Skill.AGILITY.ordinal()]);
			assertEquals(entry.varbit == 0 ? 0 : 1, row.getVarbits().size());
			row.getVarbits().forEach(bit ->
			{
				assertFalse(bit.matches(entry.threshold));
				assertTrue(bit.matches(entry.threshold + 1));
			});
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
				row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				row.getAction(), true, row.getObjectId(), row.getOrigin(), row.getDestination());
			CatalogTransition transition = new CatalogTransition(null, row.getOrigin(), row.getObjectId(),
				row.getAction(), row.getAction(), row.getOrigin(), row.getDestination());
			CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
			assertTrue(scanner.observePending(pending, row.getOrigin(), edge -> transition, 6).isReady());
			WorldPoint nearby = new WorldPoint(row.getOrigin().getX() + 1, row.getOrigin().getY(), 0);
			assertFalse(scanner.observePending(pending, nearby, edge -> transition, 6).isReady());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> null, 6).getStatus());
			assertFalse(NorthernQuestShortcutPolicy.fellToEarlierStage(pending, row.getDestination()));
			assertFalse(NorthernQuestShortcutPolicy.fellToEarlierStage(pending, row.getOrigin()));
			if (entry.varbit != 0) row.getVarbits().clear();
			else row.getSkillLevels()[Skill.AGILITY.ordinal()] = entry.ascending ? 0 : 68;
			assertFalse(CatalogTransitionPolicy.isEligible(row));
		}
	}
}
