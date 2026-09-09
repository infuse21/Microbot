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
	private static final int ROPE_ITEM_ID = ItemID.ROPE;
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
			&& transport.getCurrencyAmount() == 0
			&& transport.getQuests().isEmpty() && transport.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(transport.getSkillLevels()).allMatch(level -> level == 0)
			&& hasRopeVariant(transport)
			&& ROUTES.contains(point(transport.getOrigin()) + "->"
				+ point(transport.getDestination()));
	}

	public static Set<Integer> gasSafeLightSourceIds(Transport transport)
	{
		return isEligible(transport) && requiresLight(transport) ? GAS_SAFE_LIGHTS : Set.of();
	}

	public static boolean requiresLight(Transport transport)
	{
		return hasExactVarbit(transport, VarbitID.MY2ARM_FIRE_LUMB, 0);
	}

	public static boolean isRopeSetup(Transport transport)
	{
		return isEligible(transport) && transport.isConsumable();
	}

	public static boolean requiresExactLanding(int objectId)
	{
		return objectId == DARK_HOLE_ID;
	}

	private static boolean hasRopeVariant(Transport transport)
	{
		if (transport.getVarbits().size() != 2
			|| !(requiresLight(transport)
				|| hasExactVarbit(transport, VarbitID.MY2ARM_FIRE_LUMB, 1)))
		{
			return false;
		}
		boolean setup = transport.isConsumable()
			&& transport.getItemIdRequirements().equals(Set.of(Set.of(ROPE_ITEM_ID)))
			&& hasExactVarbit(transport, VarbitID.SWAMP_CAVES_ROPED_ENTRANCE, 0);
		boolean installed = !transport.isConsumable()
			&& transport.getItemIdRequirements().isEmpty()
			&& hasExactVarbit(transport, VarbitID.SWAMP_CAVES_ROPED_ENTRANCE, 1);
		return setup || installed;
	}

	private static boolean hasExactVarbit(Transport transport, int varbitId, int value)
	{
		return transport != null && transport.getVarbits().stream().anyMatch(gate ->
			gate.getVarbitId() == varbitId && gate.getValue() == value
				&& gate.getOperator() == TransportVarbit.Operator.EQUAL);
	}

	private static String point(WorldPoint point)
	{
		return point.getX() + "," + point.getY() + "," + point.getPlane();
	}
}
