package net.runelite.client.plugins.microbot.util.walker.door;

import net.runelite.api.coords.WorldPoint;

import java.util.Set;

/**
 * Region policy for Stronghold doors that can open a question dialogue.
 *
 * <p>NavigationEngine remains the route owner; the region check selects the specialised
 * dialogue-aware door interaction.</p>
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
}
