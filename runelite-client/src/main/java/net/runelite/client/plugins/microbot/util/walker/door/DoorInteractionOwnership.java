package net.runelite.client.plugins.microbot.util.walker.door;

import net.runelite.api.coords.WorldPoint;

import java.util.Set;

/**
 * Ownership boundary between ordinary engine doors and specialised interaction handlers.
 *
 * <p>Stronghold of Security doors may present a question dialogue after an otherwise ordinary
 * door action. Until dialogue progression is represented as non-blocking navigation state, those
 * regions must remain legacy-owned.</p>
 */
public final class DoorInteractionOwnership
{
	private static final Set<Integer> STRONGHOLD_OF_SECURITY_REGIONS = Set.of(
		7505, 7504, 7760, 7503, 7759, 7758, 7757, 8013, 7756, 8012, 8017, 8530, 9297);

	private DoorInteractionOwnership()
	{
	}

	public static boolean isStrongholdSecurityRegion(WorldPoint point)
	{
		return point != null && STRONGHOLD_OF_SECURITY_REGIONS.contains(point.getRegionID());
	}

	public static boolean ordinaryEngineAllowed(WorldPoint start, Set<WorldPoint> targets)
	{
		if (isStrongholdSecurityRegion(start))
		{
			return false;
		}
		return targets == null || targets.stream().noneMatch(DoorInteractionOwnership::isStrongholdSecurityRegion);
	}
}
