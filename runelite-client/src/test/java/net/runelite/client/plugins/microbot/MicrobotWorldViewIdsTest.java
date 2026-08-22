package net.runelite.client.plugins.microbot;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The entity caches read every world view in {@code worldViewIds}, and that set is maintained purely by
 * WorldViewLoaded / WorldViewUnloaded events — so a view that loaded before MicrobotPlugin subscribed is
 * never in it. The top-level view is the one that loads first, which made this the common case.
 *
 * <p>It failed silently, which is what made it expensive: an empty id set yields an empty scene rather
 * than an error. Measured in House Thieving — 18 NPCs in the raw RuneLite scene, 0 through Rs2NpcCache,
 * and the plugin sat forever on a null Aurelia.
 */
public class MicrobotWorldViewIdsTest
{
	@Test
	public void theTopLevelViewIsReadableEvenWhenNoLoadEventWasEverSeen()
	{
		assertEquals("an empty tracked set must still read the top-level view",
			Set.of(0), Microbot.worldViewIdsWithTopLevel(Set.of(), 0));
		assertEquals(Set.of(-1), Microbot.worldViewIdsWithTopLevel(null, -1));
	}

	@Test
	public void trackedViewsSurviveAlongsideTheTopLevelOne()
	{
		Set<Integer> tracked = new LinkedHashSet<>(Set.of(7, 9));
		Set<Integer> active = Microbot.worldViewIdsWithTopLevel(tracked, 0);

		assertTrue("an instance view must not be dropped", active.containsAll(Set.of(7, 9)));
		assertTrue("the top-level view must be added", active.contains(0));
		assertEquals(3, active.size());
	}

	@Test
	public void anAlreadyTrackedTopLevelViewIsNotDuplicatedAndTheSetIsNotCopied()
	{
		Set<Integer> tracked = new LinkedHashSet<>(Set.of(0, 5));
		Set<Integer> active = Microbot.worldViewIdsWithTopLevel(tracked, 0);

		assertEquals(2, active.size());
		assertTrue("the live set is returned as-is when it already covers the top-level view",
			tracked == active);
	}

	@Test
	public void noTopLevelViewLeavesTheTrackedSetUntouched()
	{
		Set<Integer> tracked = new LinkedHashSet<>(Set.of(3));
		assertEquals(tracked, Microbot.worldViewIdsWithTopLevel(tracked, null));
		assertEquals(Set.of(), Microbot.worldViewIdsWithTopLevel(null, null));
	}
}
