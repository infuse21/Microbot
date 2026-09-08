package net.runelite.client.plugins.microbot.util.walker.obstacle;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import net.runelite.api.Client;
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
		return templateLocation(Microbot.getClient(), object);
	}

	static WorldPoint templateLocation(Client client, TileObject object)
	{
		if (object == null || object.getWorldLocation() == null)
		{
			return null;
		}
		WorldView worldView = object.getWorldView();
		if (worldView == null || !worldView.isInstance())
		{
			return object.getWorldLocation();
		}
		return WorldPoint.fromLocalInstance(client, object.getLocalLocation(),
			object.getPlane());
	}

	/** Returns every live scene coordinate represented by one template route coordinate. */
	public static Collection<WorldPoint> sceneLocations(WorldPoint templateLocation)
	{
		return sceneLocations(Microbot.getClient().getTopLevelWorldView(), templateLocation);
	}

	static Collection<WorldPoint> sceneLocations(WorldView worldView,
		WorldPoint templateLocation)
	{
		if (templateLocation == null)
		{
			return Collections.emptyList();
		}
		if (worldView == null || !worldView.isInstance())
		{
			return Collections.singletonList(templateLocation);
		}
		return WorldPoint.toLocalInstance(worldView, templateLocation);
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
