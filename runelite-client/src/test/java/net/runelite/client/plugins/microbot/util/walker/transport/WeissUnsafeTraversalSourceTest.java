package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WeissUnsafeTraversalSourceTest
{
	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(row -> Set.of(33190, 33327, 33328).contains(row.getObjectId())).collect(Collectors.toList());
	}

	@Test
	public void restoredRowsRequireExactAscentAndInstalledRopeContracts()
	{
		List<Transport> rows = rows();
		assertEquals(4, rows.size());
		for (Transport row : rows)
		{
			NorthernQuestShortcutPolicy.Entry entry = NorthernQuestShortcutPolicy.entry(row);
			assertNotNull(entry);
			assertTrue(row.isMembers());
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertFalse(AdjacentTransportPolicy.isEligible(row));
			assertEquals(entry.ascending ? 68 : 0, row.getSkillLevels()[Skill.AGILITY.ordinal()]);
			if (row.getObjectId() == 33327 || row.getObjectId() == 33328)
			{
				assertEquals(1, row.getVarbits().size());
				assertEquals(6528, row.getVarbits().iterator().next().getVarbitId());
				assertFalse(row.getVarbits().iterator().next().matches(44));
				assertTrue(row.getVarbits().iterator().next().matches(45));
				row.getVarbits().clear();
			}
			else row.getSkillLevels()[Skill.AGILITY.ordinal()] = entry.ascending ? 0 : 68;
			assertFalse(CatalogTransitionPolicy.isEligible(row));
		}
	}

	@Test
	public void allFourRowsRetainTheirExactDirectedSourceGeometry()
	{
		assertEquals(Set.of(
			"2855 3964 0>2853 3961 0:Climb;Rope;33328",
			"2853 3961 0>2855 3964 0:Climb;Roped tree;33327",
			"2853 3961 0>2857 3961 0:Cross;Ledge;33190",
			"2857 3961 0>2853 3961 0:Cross;Ledge;33190"),
			rows().stream().map(row -> point(row.getOrigin()) + ">" + point(row.getDestination())
				+ ":" + row.getAction() + ";" + row.getName() + ";" + row.getObjectId())
				.collect(Collectors.toSet()));
	}

	private static String point(WorldPoint point)
	{
		return point.getX() + " " + point.getY() + " " + point.getPlane();
	}
}
