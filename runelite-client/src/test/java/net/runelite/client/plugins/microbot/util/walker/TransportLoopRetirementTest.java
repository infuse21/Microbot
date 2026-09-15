package net.runelite.client.plugins.microbot.util.walker;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathConfig;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class TransportLoopRetirementTest
{
	@Test
	public void retiredDispatchLoopsAndBookkeepingCannotBeReintroduced()
	{
		Set<String> methods = Arrays.stream(Rs2Walker.class.getDeclaredMethods())
			.map(java.lang.reflect.Method::getName).collect(Collectors.toSet());
		for (String name : Set.of("handleTransports", "handleTransportsInRawSegment",
			"handleCurrentTileTransportTowardPath", "finishHandledTransport",
			"waitForPostHandleObjectLanding", "primeExpectedTransportDestinations",
			"consumeExpectedTransportDestination", "markCurrentTileTransportAttempt",
			"markRangedTransportEdgeFailed", "handleTeleportItem",
			"handleInventoryTeleports", "handleWearableTeleports",
			"handleTeleportSpell", "handleSeasonalTransport",
			"attemptObservedWithoutAttemptRecord", "logLegacyLockOnce"))
		{
			assertFalse(name, methods.contains(name));
		}
		Set<String> fields = Arrays.stream(Rs2Walker.class.getDeclaredFields())
			.map(java.lang.reflect.Field::getName).collect(Collectors.toSet());
		for (String name : Set.of("expectedTransportDestinations", "recentCurrentTileTransportByEdge",
			"failedRangedTransportEdges")) assertFalse(name, fields.contains(name));
	}

	@Test
	public void retiredDeveloperToggleIsNotAConfigItem() throws Exception
	{
		assertNull(ShortestPathConfig.class.getMethod("navigationEngineOrdinaryWalking").getAnnotation(ConfigItem.class));
	}
}
