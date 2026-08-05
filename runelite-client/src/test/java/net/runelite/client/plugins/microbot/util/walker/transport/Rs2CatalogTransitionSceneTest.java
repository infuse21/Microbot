package net.runelite.client.plugins.microbot.util.walker.transport;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class Rs2CatalogTransitionSceneTest
{
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
}
