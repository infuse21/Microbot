package net.runelite.client.plugins.microbot.questing;

import net.runelite.api.CollisionDataFlag;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Whether the player is standing against an object is what decides if a quest step can click it.
 * Line of sight cannot answer this — it refuses to see into a tile the object itself fills — and raw
 * collision flags cannot either, since the object's own footprint and a wall look identical.
 */
public class ObjectAdjacencyTest
{
	/** The Blue Moon Inn chest: two tiles wide, approached from the south. */
	private static final WorldArea CHEST = new WorldArea(3219, 3396, 2, 1, 1);

	private static boolean against(int x, int y, int plane, WorldArea area)
	{
		return QuestingScript.isOrthogonallyAgainst(new WorldPoint(x, y, plane), area);
	}

	@Test
	public void acceptsTheTileTheQuestActuallyStoodOn()
	{
		assertTrue(against(3219, 3395, 1, CHEST));
	}

	@Test
	public void acceptsEveryOrthogonalTileAlongAMultiTileFootprint()
	{
		assertTrue("south of the far half", against(3220, 3395, 1, CHEST));
		assertTrue("west end", against(3218, 3396, 1, CHEST));
		assertTrue("east end", against(3221, 3396, 1, CHEST));
		assertTrue("north side", against(3219, 3397, 1, CHEST));
	}

	@Test
	public void rejectsDiagonals()
	{
		assertFalse(against(3218, 3395, 1, CHEST));
		assertFalse(against(3221, 3397, 1, CHEST));
	}

	@Test
	public void rejectsTilesInsideTheFootprint()
	{
		assertFalse(against(3219, 3396, 1, CHEST));
		assertFalse(against(3220, 3396, 1, CHEST));
	}

	@Test
	public void rejectsAnythingFurtherThanOneTile()
	{
		assertFalse(against(3219, 3394, 1, CHEST));
		assertFalse(against(3222, 3396, 1, CHEST));
	}

	/** A staircase on the floor above is not clickable from below, however close it looks in 2D. */
	@Test
	public void rejectsAnotherPlane()
	{
		assertFalse(against(3219, 3395, 0, CHEST));
	}

	/**
	 * The edge test is what separates "adjacent and usable" from "adjacent through a wall". A wall sets
	 * a directional sight bit on the edge it occupies; a solid object — chest, crate, ladder — blocks
	 * movement only, so its own tile carries no such bit.
	 */
	@Test
	public void wallOnTheSharedEdgeBlocksSightFromEitherSide()
	{
		int north = CollisionDataFlag.BLOCK_LINE_OF_SIGHT_NORTH;
		int south = CollisionDataFlag.BLOCK_LINE_OF_SIGHT_SOUTH;

		// wall recorded on the tile we stand on, facing the object
		assertTrue(QuestingScript.edgeBlocksSight(north, 0, 0, 1));
		// same wall recorded on the object's tile instead
		assertTrue(QuestingScript.edgeBlocksSight(0, south, 0, 1));
		// nothing on this edge — a solid object we are simply pressed against
		assertFalse(QuestingScript.edgeBlocksSight(0, 0, 0, 1));
	}

	/** A bit for a different edge must not be mistaken for this one. */
	@Test
	public void doesNotConfuseOneEdgeForAnother()
	{
		int east = CollisionDataFlag.BLOCK_LINE_OF_SIGHT_EAST;
		assertFalse("east wall says nothing about the northern edge",
				QuestingScript.edgeBlocksSight(east, 0, 0, 1));
		assertTrue("east wall does block the eastern edge",
				QuestingScript.edgeBlocksSight(east, 0, 1, 0));
	}

	/**
	 * BLOCK_LINE_OF_SIGHT_FULL is deliberately ignored: a large object can set it on its own tile, and
	 * rejecting a target for being solid is the mistake this whole test replaces.
	 */
	@Test
	public void ignoresTheFullSightBlockOnTheTargetTile()
	{
		assertFalse(QuestingScript.edgeBlocksSight(0, CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL, 0, 1));
	}

	@Test
	public void nearestFootprintTileIsTheOneWeInteractAcross()
	{
		// two-tile chest, approached from below its western half
		assertEquals(new WorldPoint(3219, 3396, 1),
				QuestingScript.nearestFootprintTile(new WorldPoint(3219, 3395, 1), CHEST));
		// ...and from below its eastern half
		assertEquals(new WorldPoint(3220, 3396, 1),
				QuestingScript.nearestFootprintTile(new WorldPoint(3220, 3395, 1), CHEST));
		// from the west end, the nearest tile is the western one
		assertEquals(new WorldPoint(3219, 3396, 1),
				QuestingScript.nearestFootprintTile(new WorldPoint(3218, 3396, 1), CHEST));
	}

	@Test
	public void handlesSingleTileObjectsAndNulls()
	{
		WorldArea crate = new WorldArea(3009, 3207, 1, 1, 0);
		assertTrue(against(3010, 3207, 0, crate));
		assertTrue(against(3008, 3207, 0, crate));
		assertFalse(against(3010, 3208, 0, crate));
		assertFalse(QuestingScript.isOrthogonallyAgainst(null, crate));
		assertFalse(QuestingScript.isOrthogonallyAgainst(new WorldPoint(3010, 3207, 0), null));
	}
}
