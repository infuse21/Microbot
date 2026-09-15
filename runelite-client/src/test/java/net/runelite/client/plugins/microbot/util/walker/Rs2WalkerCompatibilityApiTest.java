package net.runelite.client.plugins.microbot.util.walker;

import java.util.function.BooleanSupplier;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Protects the plugin-facing walker facade while its implementation is decomposed. */
public class Rs2WalkerCompatibilityApiTest
{
	@Test
	public void establishedWalkingEntryPointsKeepTheirSignatures() throws Exception
	{
		assertReturn(boolean.class, "walkTo", int.class, int.class, int.class);
		assertReturn(boolean.class, "walkTo", int.class, int.class, int.class, int.class);
		assertReturn(boolean.class, "walkTo", WorldPoint.class);
		assertReturn(boolean.class, "walkTo", WorldPoint.class, int.class);
		assertReturn(boolean.class, "walkUntil", WorldPoint.class, int.class, BooleanSupplier.class);
		assertReturn(WalkerState.class, "walkWithState", WorldPoint.class);
		assertReturn(WalkerState.class, "walkWithState", WorldPoint.class, int.class);
		assertReturn(WalkerState.class, "walkWithStateUntil",
			WorldPoint.class, int.class, BooleanSupplier.class);
		assertReturn(WalkerState.class, "walkWithStateTry",
			WorldPoint.class, int.class, long.class);
		assertReturn(WalkerState.class, "walkStep", WorldPoint.class, int.class);
		assertReturn(boolean.class, "walkFastCanvas", WorldPoint.class);
		assertReturn(boolean.class, "walkFastCanvas", WorldPoint.class, boolean.class);
		assertReturn(WorldPoint.class, "walkCanvas", WorldPoint.class);
		assertReturn(void.class, "walkFastLocal", LocalPoint.class);
		assertReturn(boolean.class, "walkMiniMap", WorldPoint.class);
		assertReturn(boolean.class, "walkMiniMap", WorldPoint.class, double.class);
		assertReturn(boolean.class, "walkWithBankedTransports", WorldPoint.class);
		assertReturn(boolean.class, "walkWithBankedTransports", WorldPoint.class, boolean.class);
		assertReturn(boolean.class, "walkWithBankedTransports",
			WorldPoint.class, int.class, boolean.class);
		assertReturn(WalkerState.class, "walkWithBankedTransportsAndState",
			WorldPoint.class, int.class, boolean.class);
	}

	private static void assertReturn(Class<?> expected, String name, Class<?>... parameters)
		throws Exception
	{
		assertEquals(name, expected, Rs2Walker.class.getMethod(name, parameters).getReturnType());
	}
}
