package net.runelite.client.plugins.microbot.util.walker;

import net.runelite.client.plugins.microbot.util.walker.state.WalkExit;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Characterization of {@link WalkExit} against the string predicates it replaced.
 *
 * <p>This test exists to make the {@code String exitReason} → enum refactor <em>provably inert</em>.
 * For every constant it asserts that the enum's three flags agree with the legacy predicates
 * evaluated on that constant's wire name. Green means the refactor changed no behaviour.
 *
 * <p>When a classification is deliberately corrected, the expectation moves <em>here</em>, and the
 * diff to this file is the record of exactly what changed — which is precisely what the string
 * version could never provide. Do not "fix" a failure by editing the enum until you have written
 * down why the new answer is the right one.
 */
public class WalkExitTest
{
	/** Detail used when exercising the parameterized reason; the legacy form always had a suffix. */
	private static final String OFF_PATH_DETAIL = "recent-click";

	private static String legacyWireName(WalkExit exit)
	{
		return exit == WalkExit.OFF_PATH_DEFERRED ? exit.wireName(OFF_PATH_DETAIL) : exit.wireName();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void progressClassificationMatchesTheLegacyPredicate()
	{
		for (WalkExit exit : WalkExit.values())
		{
			String wire = legacyWireName(exit);
			assertEquals(exit.name() + " (\"" + wire + "\") changed its route-progress meaning",
				Rs2Walker.isRouteProgressExit(wire), exit.isProgress());
		}
	}

	@Test
	@SuppressWarnings("deprecation")
	public void tailExemptionMatchesTheLegacyPredicate()
	{
		for (WalkExit exit : WalkExit.values())
		{
			String wire = legacyWireName(exit);
			assertEquals(exit.name() + " (\"" + wire + "\") changed its tail-exemption meaning",
				Rs2Walker.isTailExemptExit(wire), exit.isTailExempt());
		}
	}

	@Test
	@SuppressWarnings("deprecation")
	public void doorLikeClassificationMatchesTheLegacyPredicate()
	{
		for (WalkExit exit : WalkExit.values())
		{
			String wire = legacyWireName(exit);
			assertEquals(exit.name() + " (\"" + wire + "\") changed its door-like meaning",
				Rs2Walker.shouldCanvasNudgeAfterDoorLikeExit(wire), exit.isDoorLike());
		}
	}

	/**
	 * The whole point of the enum is that the set of reasons is enumerable. Two of them
	 * ({@code door-edge-resolved-after-wait}, {@code door-edge-waiting-retry}) were produced inside a
	 * ternary and never appeared in a search for {@code exitReason = "…"}, so the reason set could not
	 * be recovered by reading the code. Pin the full set so a new value has to be added here too.
	 */
	@Test
	public void theReasonSetIsComplete()
	{
		Set<String> expected = new HashSet<>(Arrays.asList(
			"end-of-path",
			"door-handled",
			"door-handled-before-minimap-click",
			"door-handled-during-interim",
			"door-handled-local-reachability",
			"door-handled-local-reachability-raw-scan",
			"door-handled-nearby-route-door",
			"door-handled-path-adj-scan",
			"path-blocker-handled",
			"rockfall-handled",
			"transport-handled",
			"current-tile-transport-handled",
			"post-click-current-tile-transport-handled",
			"raw-path-scene-object-handled",
			"post-click-raw-path-scene-object-handled",
			"frontier-obstacle-handled",
			"transport-handled-local-reachability",
			"local-recovery-click",
			"local-reachability-miss-no-click",
			"recent-door-edge-nudge",
			"door-suppressed-approach-click",
			"door-recovery-suppressed",
			"recovery-position-stale",
			"recovery-click-preempted-by-action",
			"recovery-target-walled-replan",
			"recovery-target-walled-waiting",
			"door-edge-resolved-fast-click",
			"door-edge-resolved-after-wait",
			"door-edge-resolved-after-nearby-wait",
			"door-edge-waiting-retry",
			"door-edge-nearby-waiting-retry",
			"interim-in-flight",
			"recovery-move-in-flight",
			"route-move-in-flight",
			"door-settling-yield",
			"door-traversal-pending-yield",
			"transport-settling-yield",
			"route-fold-continuation-click",
			"route-fold-continuation-pending",
			"off-path-deferred",
			"not-near-path",
			"click-failed-off-minimap",
			"player-location-null"));

		Set<String> actual = new HashSet<>();
		for (WalkExit exit : WalkExit.values())
		{
			actual.add(exit.wireName());
		}
		assertEquals("the set of walker exit reasons changed", expected, actual);
		assertEquals("wire names must be unique", WalkExit.values().length, actual.size());
	}

	/** The parameterized reason has to rebuild the exact string the log consumers expect. */
	@Test
	public void offPathDeferredKeepsItsDetailSuffix()
	{
		assertEquals("off-path-deferred:recent-click",
			WalkExit.OFF_PATH_DEFERRED.wireName("recent-click"));
		assertEquals("off-path-deferred:", WalkExit.OFF_PATH_DEFERRED.wireName(null));
		assertTrue(WalkExit.OFF_PATH_DEFERRED.wireName("x").startsWith("off-path-deferred:"));
	}

	/** A detail on any other reason is meaningless and must not corrupt its wire name. */
	@Test
	public void detailIsIgnoredForNonParameterizedReasons()
	{
		assertEquals("door-handled", WalkExit.DOOR_HANDLED.wireName("ignored"));
	}
}
