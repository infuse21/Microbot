package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;

/** Exact post-unlock contract for the Grim Tales witch-house manhole. */
public final class GrimTalesManholePolicy
{
	public static final int MANHOLE_ID = ObjectID.GRIM_MANHOLE;
	private static final WorldPoint LANDING = new WorldPoint(2901, 9867, 0);
	private static final Set<WorldPoint> ORIGINS = Set.of(
		new WorldPoint(2898, 3469, 0),
		new WorldPoint(2899, 3469, 0),
		new WorldPoint(2899, 3470, 0),
		new WorldPoint(2899, 3468, 0));

	private GrimTalesManholePolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& ORIGINS.contains(transport.getOrigin()) && LANDING.equals(transport.getDestination())
			&& transport.getObjectId() == MANHOLE_ID
			&& "Enter".equalsIgnoreCase(transport.getAction())
			&& "Manhole".equalsIgnoreCase(transport.getName())
			&& transport.isMembers() && transport.getDuration() == 1
			&& !transport.isConsumable() && transport.getCurrencyAmount() == 0
			&& transport.getItemIdRequirements().isEmpty()
			&& transport.getQuests().isEmpty() && transport.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(transport.getSkillLevels()).allMatch(level -> level == 0)
			&& hasPermanentUnlock(transport);
	}

	public static boolean requiresExactLanding(int objectId)
	{
		return objectId == MANHOLE_ID;
	}

	private static boolean hasPermanentUnlock(Transport transport)
	{
		if (transport.getVarbits().size() != 1)
		{
			return false;
		}
		TransportVarbit gate = transport.getVarbits().iterator().next();
		return gate.getVarbitId() == VarbitID.GRIM_MANHOLE_OPEN
			&& gate.getValue() == 1 && gate.getOperator() == TransportVarbit.Operator.EQUAL;
	}
}
