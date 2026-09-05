package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Locale;
import java.util.Map;

/** Exact catalog boundary for deterministic Port Phasmatys energy barriers. */
public final class EnergyBarrierPolicy
{
	private static final int OBJECT_ID = 16105;

	private EnergyBarrierPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getObjectId() != OBJECT_ID
			|| !"paytoll(2ecto)".equals(normalizeAction(transport.getAction()))
			|| !"energy barrier".equals(normalize(transport.getName()))
			|| transport.getOrigin().getPlane() != transport.getDestination().getPlane()
			|| transport.getOrigin().getPlane() != 0
			|| transport.getOrigin().distanceTo2D(transport.getDestination()) < 2
			|| transport.getOrigin().distanceTo2D(transport.getDestination()) > 3
			|| transport.isConsumable() || !transport.getItemIdRequirements().isEmpty())
		{
			return false;
		}
		if (transport.getCurrencyAmount() != 0
			|| !normalize(transport.getCurrencyName()).isEmpty())
		{
			return false;
		}
		return transport.getQuests().equals(
			Map.of(Quest.GHOSTS_AHOY, QuestState.FINISHED))
			|| transport.getQuests().isEmpty() && transport.getDuration() == 2;
	}

	private static String normalizeAction(String value)
	{
		return normalize(value).replace("-", "").replace(" ", "");
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
