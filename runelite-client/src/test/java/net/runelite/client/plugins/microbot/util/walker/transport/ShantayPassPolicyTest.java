package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShantayPassPolicyTest
{
	@Test
	public void publishesExactlyTheFourteenAuditedRows()
	{
		List<Transport> rows = rows();
		assertEquals(14, rows.size());
		assertTrue(rows.stream().allMatch(ShantayPassPolicy::isEligible));
		assertEquals(6, rows.stream().filter(ShantayPassPolicy::requiresPass).count());
		assertEquals(2, rows.stream().filter(ShantayPassPolicy::isCoinTwin).count());
		assertEquals(6, rows.stream().filter(ShantayPassPolicy::isFreeReturn).count());
	}

	@Test
	public void stagesMainGatePurchaseButNeverInventsAnUnkahVendor()
	{
		Transport main = rows().stream().filter(ShantayPassPolicy::requiresPass)
			.filter(row -> row.getObjectId() == 4031).findFirst().orElseThrow(AssertionError::new);
		Transport unkah = rows().stream().filter(ShantayPassPolicy::requiresPass)
			.filter(row -> row.getObjectId() == 41326).findFirst().orElseThrow(AssertionError::new);
		Transport coinTwin = rows().stream().filter(ShantayPassPolicy::isCoinTwin)
			.findFirst().orElseThrow(AssertionError::new);

		assertEquals(ShantayPassPolicy.Stage.BUY_PASS,
			ShantayPassPolicy.nextStage(main, false, 5, false, true, null));
		assertEquals(ShantayPassPolicy.Stage.UNAVAILABLE,
			ShantayPassPolicy.nextStage(unkah, false, 5, false, true, null));
		assertEquals(ShantayPassPolicy.Stage.BUY_PASS,
			ShantayPassPolicy.nextStage(coinTwin, false, 5, false, true, null));
		assertEquals(ShantayPassPolicy.Stage.GO_THROUGH,
			ShantayPassPolicy.nextStage(unkah, true, 0, false, false, null));
		assertEquals(ShantayPassPolicy.Stage.GO_THROUGH,
			ShantayPassPolicy.nextStage(unkah, false, 0, true, false, null));
	}

	@Test
	public void pendingGateStageSurvivesConsumablePassRemoval()
	{
		Transport main = rows().stream().filter(ShantayPassPolicy::requiresPass)
			.filter(row -> row.getObjectId() == 4031).findFirst().orElseThrow(AssertionError::new);
		assertEquals(ShantayPassPolicy.Stage.GO_THROUGH,
			ShantayPassPolicy.nextStage(main, false, 0, false, false,
				ShantayPassPolicy.GO_THROUGH_ACTION));
	}

	@Test
	public void crossingProofUsesTheDirectedGateAxis()
	{
		assertTrue(ShantayPassPolicy.hasCrossed(4031,
			new WorldPoint(3303, 3117, 0), new WorldPoint(3304, 3115, 0),
			new WorldPoint(3302, 3115, 0)));
		assertFalse(ShantayPassPolicy.hasCrossed(4031,
			new WorldPoint(3303, 3117, 0), new WorldPoint(3304, 3115, 0),
			new WorldPoint(3304, 3116, 0)));
		assertTrue(ShantayPassPolicy.hasCrossed(41326,
			new WorldPoint(3193, 2843, 0), new WorldPoint(3196, 2843, 0),
			new WorldPoint(3196, 2841, 0)));
		assertFalse(ShantayPassPolicy.hasCrossed(41326,
			new WorldPoint(3193, 2843, 0), new WorldPoint(3196, 2843, 0),
			new WorldPoint(3195, 2843, 0)));
	}

	@Test
	public void rejectsLookalikesAndMutatedRequirements()
	{
		Transport row = rows().get(0);
		assertFalse(ShantayPassPolicy.isEligible(new Transport(row.getOrigin(), row.getDestination(),
			"test", row.getType(), false, "Open", row.getName(), row.getObjectId())));
		assertFalse(ShantayPassPolicy.isEligible(new Transport(row.getOrigin(), row.getDestination(),
			"test", row.getType(), false, row.getAction(), row.getName(), 99999)));
	}

	private static List<Transport> rows()
	{
		List<Transport> rows = new ArrayList<>();
		for (Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				if ((row.getObjectId() == 4031 || row.getObjectId() == 41326)
					&& "Shantay pass".equals(row.getName()))
				{
					rows.add(row);
				}
			}
		}
		return rows;
	}
}
