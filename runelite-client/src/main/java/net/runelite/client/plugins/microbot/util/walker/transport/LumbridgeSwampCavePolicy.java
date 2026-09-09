package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.questhelper.collections.ItemCollections;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;

/** Permanently roped, gas-safe entry contract for the Lumbridge Swamp Caves. */
public final class LumbridgeSwampCavePolicy
{
	public static final int DARK_HOLE_ID = 5947;
	private static final Set<Integer> GAS_IGNITING_LIGHTS = Set.of(
		ItemID.TORCH_LIT,
		ItemID.LIT_CANDLE,
		ItemID.LIT_BLACK_CANDLE,
		ItemID.OIL_LAMP_LIT);
	private static final Set<Integer> GAS_SAFE_LIGHTS = ItemCollections.LIGHT_SOURCES.getItems()
		.stream()
		.filter(itemId -> !GAS_IGNITING_LIGHTS.contains(itemId))
		.collect(Collectors.toUnmodifiableSet());
	private static final Set<String> ROUTES = Set.of(
		"3169,3171,0->3169,9571,0",
		"3168,3172,0->3168,9572,0",
		"3170,3172,0->3170,9572,0",
		"3169,3173,0->3167,9573,0");

	private LumbridgeSwampCavePolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& transport.getObjectId() == DARK_HOLE_ID
			&& "Climb-down".equalsIgnoreCase(transport.getAction())
			&& "Dark hole".equalsIgnoreCase(transport.getName())
			&& transport.isMembers() && transport.getDuration() == 2
			&& !transport.isConsumable() && transport.getCurrencyAmount() == 0
			&& transport.getItemIdRequirements().isEmpty()
			&& transport.getQuests().isEmpty() && transport.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(transport.getSkillLevels()).allMatch(level -> level == 0)
			&& hasInstalledRope(transport)
			&& ROUTES.contains(point(transport.getOrigin()) + "->"
				+ point(transport.getDestination()));
	}

	public static Set<Integer> gasSafeLightSourceIds(Transport transport)
	{
		return isEligible(transport) ? GAS_SAFE_LIGHTS : Set.of();
	}

	public static boolean requiresExactLanding(int objectId)
	{
		return objectId == DARK_HOLE_ID;
	}

	private static boolean hasInstalledRope(Transport transport)
	{
		if (transport.getVarbits().size() != 1)
		{
			return false;
		}
		TransportVarbit gate = transport.getVarbits().iterator().next();
		return gate.getVarbitId() == VarbitID.SWAMP_CAVES_ROPED_ENTRANCE
			&& gate.getValue() == 1 && gate.getOperator() == TransportVarbit.Operator.EQUAL;
	}

	private static String point(WorldPoint point)
	{
		return point.getX() + "," + point.getY() + "," + point.getPlane();
	}
}
