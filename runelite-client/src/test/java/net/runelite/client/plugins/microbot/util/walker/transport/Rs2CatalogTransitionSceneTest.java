package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class Rs2CatalogTransitionSceneTest
{
	@Test
	public void installedLiveActionContinuesWhileCatalogStillContainsOnlySetupVariant()
	{
		Transport setup = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(CatalogTransitionPolicy::isKalphiteRopeSetup)
			.filter(row -> row.getObjectId() == 23609)
			.findFirst().orElseThrow(AssertionError::new);
		java.util.List<Transport> staleCatalog = java.util.Collections.singletonList(setup);
		assertSame(setup, Rs2CatalogTransitionScene.findTransport(staleCatalog, "Use rope", 23609));
		assertTrue(Rs2CatalogTransitionScene.requiresRopePreparation(setup, "Use rope"));
		for (String liveAction : new String[]{"Climb-down", "Climb-down (normal)"})
		{
			String observed = Rs2CatalogTransitionScene.resolveLiveAction(new String[]{liveAction}, setup);
			assertEquals(liveAction, observed);
			assertSame(setup, Rs2CatalogTransitionScene.findTransport(staleCatalog, observed, 23609));
			assertFalse(Rs2CatalogTransitionScene.requiresRopePreparation(setup, observed));
		}
		assertNull(Rs2CatalogTransitionScene.findTransport(staleCatalog, "Use rope", 3827));
	}

	@Test
	public void catalogAndLiveClimbActionFormattingCanDiffer()
	{
		assertEquals("Climb up", Rs2CatalogTransitionScene.resolveAction(
			new String[]{"Climb up", null}, "Climb-up"));
		assertEquals("Climb-down", Rs2CatalogTransitionScene.resolveAction(
			new String[]{"Climb-down"}, "Climb down"));
	}

	@Test
	public void unrelatedActionCannotResolve()
	{
		assertNull(Rs2CatalogTransitionScene.resolveAction(
			new String[]{"Search", "Open"}, "Climb-up"));
	}

	@Test
	public void kalphiteNormalDescentCanResolveWithoutSelectingPrivateInstance()
	{
		Transport tunnel = new Transport(new WorldPoint(3508, 9498, 2),
			new WorldPoint(3508, 9493, 0), "", TransportType.TRANSPORT,
			true, "Climb-down", "Tunnel entrance", 23609);

		assertEquals("Climb-down (normal)", Rs2CatalogTransitionScene.resolveLiveAction(
			new String[]{"Climb-down (normal)", "Climb-down (private)", "Look-inside"}, tunnel));
		Transport unrelated = new Transport(new WorldPoint(100, 100, 0),
			new WorldPoint(100, 100, 1), "", TransportType.TRANSPORT,
			true, "Climb-down", "Tunnel entrance", 23609);
		assertNull(Rs2CatalogTransitionScene.resolveLiveAction(
			new String[]{"Climb-down (normal)", "Climb-down (private)"}, unrelated));
	}
}
