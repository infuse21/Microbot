package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DraynorBookcaseAndArdougneDoorTest
{
	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> AdjacentTransportPolicy.isDraynorBookcase(row)
				|| AdjacentTransportPolicy.isEastArdougnePickLockDoor(row))
			.collect(Collectors.toList());
	}

	@Test
	public void allSixRowsHaveExactAdjacentOwnershipContracts()
	{
		List<Transport> rows = rows();
		assertEquals(6, rows.size());
		assertEquals(4, rows.stream().filter(AdjacentTransportPolicy::isDraynorBookcase).count());
		assertEquals(2, rows.stream()
			.filter(AdjacentTransportPolicy::isEastArdougnePickLockDoor).count());
		assertTrue(rows.stream().allMatch(AdjacentTransportPolicy::isEligible));
		assertTrue(rows.stream().filter(row -> row.getObjectId() <= 156)
			.allMatch(row -> !row.isMembers() && row.getDuration() == 3));
		assertTrue(rows.stream().filter(row -> row.getObjectId() >= 11719)
			.allMatch(row -> row.isMembers() && row.getDuration() == 2));
	}

	@Test
	public void southDoorRequiresSixteenThievingWhileNorthDoorRequiresNone()
	{
		Transport south = rows().stream().filter(row -> row.getObjectId() == 11720)
			.findFirst().orElseThrow(AssertionError::new);
		Transport north = rows().stream().filter(row -> row.getObjectId() == 11719)
			.findFirst().orElseThrow(AssertionError::new);
		assertEquals(16, south.getSkillLevels()[Skill.THIEVING.ordinal()]);
		assertFalse(AdjacentTransportPolicy.hasRequiredEastArdougneThieving(south, 15));
		assertTrue(AdjacentTransportPolicy.hasRequiredEastArdougneThieving(south, 16));
		assertEquals(0, north.getSkillLevels()[Skill.THIEVING.ordinal()]);
		assertTrue(AdjacentTransportPolicy.hasRequiredEastArdougneThieving(north, 0));
	}

	@Test
	public void searchedBookcaseTransformationClearsThePendingEdge()
	{
		Transport bookcase = rows().stream().filter(AdjacentTransportPolicy::isDraynorBookcase)
			.findFirst().orElseThrow(AssertionError::new);
		RouteInteraction pending = new RouteInteraction(1, 0, bookcase.getOrigin(),
			bookcase.getDestination(), bookcase.getOrigin(),
			RouteInteraction.Kind.ADJACENT_TRANSPORT, RouteInteraction.Status.AVAILABLE,
			bookcase.getAction(), true, bookcase.getObjectId(), bookcase.getOrigin(),
			bookcase.getDestination());
		assertEquals(RouteInteraction.Status.CLEARED,
			new AdjacentTransportRouteScanner().observePending(pending, bookcase.getOrigin(),
				edge -> null, 13).getStatus());
	}
}
