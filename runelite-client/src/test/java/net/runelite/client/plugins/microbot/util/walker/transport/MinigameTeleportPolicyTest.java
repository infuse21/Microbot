package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MinigameTeleportPolicyTest
{
	@Test
	public void acceptsEveryActivePackagedMinigameRow()
	{
		List<Transport> minigames = minigames();

		assertEquals(20, minigames.size());
		assertTrue(minigames.stream().allMatch(MinigameTeleportPolicy::isEligible));
		assertEquals(3, minigames.stream().filter(candidate ->
			!MinigameTeleportPolicy.destinationOption(candidate.getDisplayInfo()).isEmpty())
			.count());
	}

	@Test
	public void parsesRatPitsAndRejectsUnknownShapes()
	{
		Transport castleWars = minigames().stream()
			.filter(candidate -> "Castle Wars".equals(candidate.getDisplayInfo()))
			.findFirst().orElseThrow();
		Transport wrongType = new Transport(null, castleWars.getDestination(),
			"Castle Wars", TransportType.TRANSPORT, true, "", "", -1);
		Transport unknown = new Transport(null, castleWars.getDestination(),
			"Future Minigame", TransportType.TELEPORTATION_MINIGAME, true,
			"", "", -1);
		Transport objectBacked = new Transport(null, castleWars.getDestination(),
			"Castle Wars", TransportType.TELEPORTATION_MINIGAME, true,
			"Use", "Portal", 1);

		assertEquals("Rat Pits", MinigameTeleportPolicy.activityName(
			"Rat Pits: Port Sarim"));
		assertEquals("Port Sarim", MinigameTeleportPolicy.destinationOption(
			"Rat Pits: Port Sarim"));
		assertEquals("", MinigameTeleportPolicy.destinationOption("Castle Wars"));
		assertTrue(MinigameTeleportPolicy.activityMatches(
			"Giant's Foundry", "Giants' Foundry"));
		assertFalse(MinigameTeleportPolicy.activityMatches("Clan Wars", "Castle Wars"));
		assertFalse(MinigameTeleportPolicy.isEligible(wrongType));
		assertFalse(MinigameTeleportPolicy.isEligible(unknown));
		assertFalse(MinigameTeleportPolicy.isEligible(objectBacked));
	}

	private static List<Transport> minigames()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TELEPORTATION_MINIGAME)
			.collect(Collectors.toList());
	}
}
