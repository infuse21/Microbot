package net.runelite.client.plugins.microbot.util.walker;

import java.lang.reflect.Field;
import java.util.function.Supplier;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.mouse.Mouse;
import net.runelite.client.plugins.microbot.util.walker.navigation.WalkerActions;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

public class NavigationMinimapCancellationTest
{
	@Test
	public void rejectedMinimapInputCannotFallThroughToCanvas() throws Exception
	{
		WorldPoint target = new WorldPoint(3250, 3430, 0);
		Point projected = new Point(1500, 100);
		Rs2MiniMap.NavigationProjection projection = new Rs2MiniMap.NavigationProjection(projected, 2d);
		Mouse mouse = mock(Mouse.class);
		try (MockedStatic<Microbot> api = mockStatic(Microbot.class);
			 MockedStatic<Rs2MiniMap> minimap = mockStatic(Rs2MiniMap.class))
		{
			api.when(Microbot::getMouse).thenReturn(mouse);
			minimap.when(() -> Rs2MiniMap.getNavigationProjection(target)).thenReturn(projection);
			WalkerActions actions = productionActions();
			assertFalse(actions.clickTile(target));
			assertEquals("minimap-route-tile-cancelled", actions.getLastActionType());
			api.verify(Microbot::getClient, never());
		}
	}

	@Test
	public void playerMovementCanRefreshProjectionWithoutCanvasFallback() throws Exception
	{
		WorldPoint target = new WorldPoint(3250, 3430, 0);
		Point projected = new Point(1500, 100);
		Rs2MiniMap.NavigationProjection initial = new Rs2MiniMap.NavigationProjection(projected, 2d);
		Rs2MiniMap.NavigationProjection shifted = new Rs2MiniMap.NavigationProjection(new Point(1490, 100), 2d);
		Mouse mouse = mock(Mouse.class);
		try (MockedStatic<Microbot> api = mockStatic(Microbot.class);
			 MockedStatic<Rs2MiniMap> minimap = mockStatic(Rs2MiniMap.class))
		{
			api.when(Microbot::getMouse).thenReturn(mouse);
			minimap.when(() -> Rs2MiniMap.getNavigationProjection(target))
				.thenReturn(initial, shifted);
			when(mouse.tryClickProjected(eq(projected), any(Supplier.class)))
				.thenAnswer(call -> ((Supplier<?>) call.getArgument(1)).get() != null);
			assertTrue(productionActions().clickTile(target));
			api.verify(Microbot::getClient, never());
		}
	}

	@Test
	public void zoomChangeCancelsWithoutCanvasFallback() throws Exception
	{
		WorldPoint target = new WorldPoint(3250, 3430, 0);
		Point projected = new Point(1500, 100);
		Rs2MiniMap.NavigationProjection initial = new Rs2MiniMap.NavigationProjection(projected, 2d);
		Rs2MiniMap.NavigationProjection changed = new Rs2MiniMap.NavigationProjection(projected, 4d);
		Mouse mouse = mock(Mouse.class);
		try (MockedStatic<Microbot> api = mockStatic(Microbot.class);
			 MockedStatic<Rs2MiniMap> minimap = mockStatic(Rs2MiniMap.class))
		{
			api.when(Microbot::getMouse).thenReturn(mouse);
			minimap.when(() -> Rs2MiniMap.getNavigationProjection(target))
				.thenReturn(initial, changed);
			when(mouse.tryClickProjected(eq(projected), any(Supplier.class)))
				.thenAnswer(call -> ((Supplier<?>) call.getArgument(1)).get() != null);
			assertFalse(productionActions().clickTile(target));
			api.verify(Microbot::getClient, never());
		}
	}

	@Test
	public void stableProjectionStillIssuesMinimapCommand() throws Exception
	{
		WorldPoint target = new WorldPoint(3250, 3430, 0);
		Point projected = new Point(1500, 100);
		Rs2MiniMap.NavigationProjection projection = new Rs2MiniMap.NavigationProjection(projected, 2d);
		Mouse mouse = mock(Mouse.class);
		try (MockedStatic<Microbot> api = mockStatic(Microbot.class);
			 MockedStatic<Rs2MiniMap> minimap = mockStatic(Rs2MiniMap.class))
		{
			api.when(Microbot::getMouse).thenReturn(mouse);
			minimap.when(() -> Rs2MiniMap.getNavigationProjection(target)).thenReturn(projection);
			when(mouse.tryClickProjected(eq(projected), any(Supplier.class)))
				.thenAnswer(call -> ((Supplier<?>) call.getArgument(1)).get() != null);
			WalkerActions actions = productionActions();
			assertTrue(actions.clickTile(target));
			assertEquals("minimap-route-tile", actions.getLastActionType());
		}
	}

	private static WalkerActions productionActions() throws Exception
	{
		Field field = Rs2Walker.class.getDeclaredField("NAVIGATION_WALKER_ACTIONS");
		field.setAccessible(true);
		return (WalkerActions) field.get(null);
	}
}
