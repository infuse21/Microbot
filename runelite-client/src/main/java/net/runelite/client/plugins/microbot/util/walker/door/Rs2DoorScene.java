package net.runelite.client.plugins.microbot.util.walker.door;

import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.walker.door.model.OrdinaryDoor;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2LiveScene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One-pass live snapshot for exact-edge classification of standard doors and gates. */
public final class Rs2DoorScene implements DoorScene
{
	private static final List<String> ORDINARY_ACTIONS =
		List.of("walk-through", "go-through", "open", "pass");

	private final WorldPoint player;
	private final int range;
	private List<Candidate> candidates;

	public Rs2DoorScene(WorldPoint player, int range)
	{
		this.player = player;
		this.range = range;
	}

	@Override
	public OrdinaryDoor findOrdinaryDoor(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null
			|| edge.from().getPlane() != edge.to().getPlane())
		{
			return null;
		}
		for (Candidate candidate : candidates())
		{
			boolean blocks = candidate.wall
				? Rs2DoorGeometry.wallDoorTouchesSegment(candidate.location,
					candidate.orientationA, candidate.orientationB, edge.from(), edge.to())
				: candidate.location.equals(edge.from()) || candidate.location.equals(edge.to());
			if (blocks)
			{
				return new OrdinaryDoor(candidate.object, candidate.location, candidate.action);
			}
		}
		return null;
	}

	private List<Candidate> candidates()
	{
		if (candidates != null)
		{
			return candidates;
		}
		if (player == null || range <= 0 || Rs2LiveScene.isInInstance())
		{
			candidates = Collections.emptyList();
			return candidates;
		}
		candidates = Microbot.getClientThread().runOnClientThreadOptional(() -> {
			List<Candidate> found = new ArrayList<>();
			for (WallObject wall : Rs2GameObject.getWallObjects(object -> true, player, range + 2))
			{
				addCandidate(found, wall, true);
			}
			for (GameObject object : Rs2GameObject.getGameObjects(gameObject -> true, player, range + 2))
			{
				addCandidate(found, object, false);
			}
			return found;
		}).orElse(Collections.emptyList());
		return candidates;
	}

	private static void addCandidate(List<Candidate> candidates, TileObject object, boolean wall)
	{
		Microbot.getClientThread().runOnClientThreadOptional(() -> {
			if (object == null || object.getWorldLocation() == null
				|| DoorInteractionOwnership.isStrongholdSecurityRegion(object.getWorldLocation())
				|| Rs2DoorProbe.isCatalogTransportObject(object))
			{
				return false;
			}
			ObjectComposition composition = Rs2DoorDetection.resolveCompositionForDoorProbe(object);
			if (!Rs2DoorClassifier.isDoorComposition(composition, ORDINARY_ACTIONS)
				|| !Rs2DoorClassifier.isDoorLikeGameObjectName(composition.getName()))
			{
				return false;
			}
			String action = Rs2DoorClassifier.getDoorAction(composition, ORDINARY_ACTIONS);
			if (action == null)
			{
				return false;
			}
			WallObject wallObject = wall ? (WallObject) object : null;
			int[] orientations = Rs2DoorGeometry.wallOrientations(wallObject);
			candidates.add(new Candidate(object, object.getWorldLocation(), action,
				orientations[0], orientations[1]));
			return true;
		});
	}

	private static final class Candidate
	{
		private final TileObject object;
		private final WorldPoint location;
		private final String action;
		private final boolean wall;
		private final int orientationA;
		private final int orientationB;

		private Candidate(TileObject object, WorldPoint location, String action,
			int orientationA, int orientationB)
		{
			this.object = object;
			this.location = location;
			this.action = action;
			this.wall = object instanceof WallObject;
			this.orientationA = orientationA;
			this.orientationB = orientationB;
		}
	}
}
