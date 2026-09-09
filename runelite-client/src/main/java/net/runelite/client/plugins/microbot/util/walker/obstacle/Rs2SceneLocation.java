package net.runelite.client.plugins.microbot.util.walker.obstacle;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;

/** Coordinate conversions at the boundary between template routes and live scene objects. */
public final class Rs2SceneLocation
{
	private Rs2SceneLocation()
	{
	}

	/** Returns an object's anchor in the template coordinate space used by pathfinder routes. */
	public static WorldPoint templateLocation(TileObject object)
	{
		if (object == null)
		{
			return null;
		}
		try
		{
			return Microbot.getClientThread().runOnClientThreadOptional(() ->
			{
				WorldPoint worldLocation = object.getWorldLocation();
				if (worldLocation == null)
				{
					return null;
				}
				WorldView worldView = object.getWorldView();
				if (worldView == null || !worldView.isInstance())
				{
					return worldLocation;
				}
				return WorldPoint.fromLocalInstance(Microbot.getClient(),
					object.getLocalLocation(), object.getPlane());
			}).orElse(null);
		}
		catch (RuntimeException ex)
		{
			if (Thread.currentThread().isInterrupted() || clientThreadUnavailable(ex))
			{
				return null;
			}
			throw ex;
		}
	}

	/** Returns every live scene coordinate represented by one template route coordinate. */
	public static Collection<WorldPoint> sceneLocations(WorldPoint templateLocation)
	{
		if (templateLocation == null)
		{
			return Collections.emptyList();
		}
		try
		{
			return Microbot.getClientThread().runOnClientThreadOptional(() ->
			{
				WorldView worldView = Microbot.getClient().getTopLevelWorldView();
				if (worldView == null || !worldView.isInstance())
				{
					return Collections.singletonList(templateLocation);
				}
				return WorldPoint.toLocalInstance(worldView, templateLocation);
			}).orElse(Collections.emptyList());
		}
		catch (RuntimeException ex)
		{
			if (Thread.currentThread().isInterrupted() || clientThreadUnavailable(ex))
			{
				return Collections.emptyList();
			}
			throw ex;
		}
	}

	/** True when a cancelled scene snapshot surfaced as a client-thread wait timeout. */
	public static boolean clientThreadUnavailable(Throwable throwable)
	{
		for (Throwable current = throwable; current != null; current = current.getCause())
		{
			if (current instanceof TimeoutException)
			{
				return true;
			}
		}
		return false;
	}
}
