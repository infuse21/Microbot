package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JungleObstaclePolicyTest
{
	@Test
	public void acceptsOnlyDeterministicProductionInputs()
	{
		List<Transport> jungle = jungleTransports();
		List<Transport> eligible = jungle.stream()
			.filter(JungleObstaclePolicy::isEligible).collect(Collectors.toList());
		List<Transport> ambiguous = jungle.stream()
			.filter(JungleObstaclePolicy::isAmbiguousInput).collect(Collectors.toList());
		Map<String, Set<WorldPoint>> destinationsByInput = eligible.stream()
			.collect(Collectors.groupingBy(JungleObstaclePolicyTest::inputKey,
				Collectors.mapping(Transport::getDestination, Collectors.toSet())));

		assertEquals(92, jungle.size());
		assertEquals(76, eligible.size());
		assertEquals(16, ambiguous.size());
		assertEquals(76, destinationsByInput.size());
		assertTrue(destinationsByInput.values().stream()
			.allMatch(destinations -> destinations.size() == 1));
		assertTrue(jungle.stream().allMatch(candidate ->
			candidate.getItemIdRequirements().size() == 1));
	}

	@Test
	public void acceptsEveryExactBrimhavenVineRow()
	{
		List<Transport> vines = brimhavenVines();

		assertEquals(10, vines.size());
		assertTrue(vines.stream().allMatch(JungleObstaclePolicy::isEligible));
		assertTrue(vines.stream().allMatch(candidate ->
			candidate.getOrigin().distanceTo2D(candidate.getDestination()) == 2));
		Set<Integer> axes = Set.of(
			1351, 1349, 1353, 1361, 1355, 1357, 1359, 6739, 20011, 13241,
			23673, 25066, 28222);
		assertTrue(vines.stream().allMatch(candidate ->
			candidate.getItemIdRequirements().stream().flatMap(Set::stream)
				.collect(Collectors.toSet()).equals(axes)));
	}

	@Test
	public void rejectsWrongIdentityGeometryRequirementsAndAmbiguousInput()
	{
		WorldPoint origin = new WorldPoint(2800, 2940, 0);
		WorldPoint destination = new WorldPoint(2800, 2938, 0);
		assertFalse(JungleObstaclePolicy.isEligible(transport(origin, destination,
			"Cut", "Jungle Bush", 2892, Set.of(6313, 6315, 6317, 975))));
		assertFalse(JungleObstaclePolicy.isEligible(transport(origin, destination,
			"Chop-down", "Bush", 2892, Set.of(6313, 6315, 6317, 975))));
		assertFalse(JungleObstaclePolicy.isEligible(transport(origin, destination,
			"Chop-down", "Jungle Bush", 2891, Set.of(6313, 6315, 6317, 975))));
		assertFalse(JungleObstaclePolicy.isEligible(transport(origin, origin.dy(-1),
			"Chop-down", "Jungle Bush", 2892, Set.of(6313, 6315, 6317, 975))));
		assertFalse(JungleObstaclePolicy.isEligible(transport(origin, destination,
			"Chop-down", "Jungle Bush", 2892, Set.of(1351))));

		Transport ambiguous = transport(new WorldPoint(2866, 2936, 0),
			new WorldPoint(2866, 2934, 0), "Chop-down", "Jungle Bush", 2892,
			Set.of(6313, 6315, 6317, 975));
		assertTrue(JungleObstaclePolicy.isAmbiguousInput(ambiguous));
		assertFalse(JungleObstaclePolicy.isEligible(ambiguous));
	}

	@Test
	public void liveIdentityRequiresExactObjectNameActionAndOrigin()
	{
		Transport obstacle = jungleTransports().stream()
			.filter(JungleObstaclePolicy::isEligible).findFirst()
			.orElseThrow(AssertionError::new);
		assertTrue(JungleObstaclePolicy.isLiveObjectMatch(obstacle,
			obstacle.getObjectId(), obstacle.getName(),
			Arrays.asList("Chop down", "Examine"), obstacle.getOrigin().dx(1)));
		assertFalse(JungleObstaclePolicy.isLiveObjectMatch(obstacle,
			obstacle.getObjectId() + 1, obstacle.getName(),
			Collections.singletonList("Chop-down"), obstacle.getOrigin()));
		assertFalse(JungleObstaclePolicy.isLiveObjectMatch(obstacle,
			obstacle.getObjectId(), "Jungle", Collections.singletonList("Chop-down"),
			obstacle.getOrigin()));
		assertFalse(JungleObstaclePolicy.isLiveObjectMatch(obstacle,
			obstacle.getObjectId(), obstacle.getName(), Collections.singletonList("Cut"),
			obstacle.getOrigin()));
		assertFalse(JungleObstaclePolicy.isLiveObjectMatch(obstacle,
			obstacle.getObjectId(), obstacle.getName(),
			Collections.singletonList("Chop-down"), obstacle.getOrigin().dx(3)));
	}

	private static List<Transport> jungleTransports()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Chop-down".equals(candidate.getAction()))
			.filter(candidate -> "Jungle Bush".equals(candidate.getName())
				|| "Jungle tree".equals(candidate.getName()))
			.filter(candidate -> candidate.getObjectId() == 2889
				|| candidate.getObjectId() == 2890 || candidate.getObjectId() == 2892
				|| candidate.getObjectId() == 2893)
			.collect(Collectors.toList());
	}

	private static List<Transport> brimhavenVines()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Chop-down".equals(candidate.getAction()))
			.filter(candidate -> "Vines".equals(candidate.getName()))
			.filter(candidate -> candidate.getObjectId() >= 21731
				&& candidate.getObjectId() <= 21735)
			.collect(Collectors.toList());
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination,
		String action, String name, int objectId, Set<Integer> tools)
	{
		Transport transport = new Transport(origin, destination, "test",
			TransportType.TRANSPORT, false, action, name, objectId);
		transport.setItemIdRequirements(Set.of(tools));
		return transport;
	}

	private static String inputKey(Transport transport)
	{
		return transport.getOrigin() + ":" + transport.getObjectId();
	}
}
