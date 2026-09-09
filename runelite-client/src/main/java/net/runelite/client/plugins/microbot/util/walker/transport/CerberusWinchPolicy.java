package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarPlayer;

/** Exact access contract for the three Cerberus Iron Winches. */
public final class CerberusWinchPolicy
{
	public static final int OBJECT_ID = 23104;
	public static final int REQUIRED_SLAYER_LEVEL = 91;
	private static final int BOSS_TASK_TARGET_ID = 98;
	private static final Set<String> ROUTES = Set.of(
		"1292,1253,0->1240,1226,0",
		"1291,1253,0->1240,1226,0",
		"1309,1269,0->1304,1290,0",
		"1328,1253,0->1368,1226,0",
		"1329,1253,0->1368,1226,0");

	private CerberusWinchPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& transport.getObjectId() == OBJECT_ID
			&& "turn".equals(normalize(transport.getAction()))
			&& "iron winch".equals(normalize(transport.getName()))
			&& transport.isMembers() && transport.getDuration() == 1
			&& !transport.isConsumable() && transport.getCurrencyAmount() == 0
			&& transport.getItemIdRequirements().isEmpty()
			&& transport.getQuests().isEmpty() && transport.getVarbits().isEmpty()
			&& hasOnlySlayerLevel(transport)
			&& hasActiveTaskCountGate(transport)
			&& ROUTES.contains(point(transport.getOrigin()) + "->"
				+ point(transport.getDestination()));
	}

	public static boolean requiresExactLanding(int objectId)
	{
		return objectId == OBJECT_ID;
	}

	/** Read once on the client thread and reuse for every winch row in a refresh pass. */
	public static AccessSnapshot readAccessSnapshot(Client client)
	{
		if (client == null)
		{
			return AccessSnapshot.unavailable();
		}
		int boostedLevel = client.getBoostedSkillLevel(Skill.SLAYER);
		int taskCount = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
		int taskTargetId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
		int bossTargetId = client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID);
		String taskName = resolveTaskName(client, taskTargetId, bossTargetId);
		return new AccessSnapshot(boostedLevel, taskCount, taskTargetId, bossTargetId,
			taskName, hasAccess(boostedLevel, taskCount, taskName));
	}

	public static boolean liveAccessAvailable()
	{
		if (Microbot.getClient() == null)
		{
			return false;
		}
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			readAccessSnapshot(Microbot.getClient()).isAvailable()).orElse(false);
	}

	static boolean hasAccess(int boostedLevel, int taskCount, String taskName)
	{
		if (boostedLevel < REQUIRED_SLAYER_LEVEL || taskCount <= 0 || taskName == null)
		{
			return false;
		}
		String normalized = normalize(taskName);
		return "hellhound".equals(normalized) || "hellhounds".equals(normalized)
			|| "cerberus".equals(normalized);
	}

	private static String resolveTaskName(Client client, int taskTargetId, int bossTargetId)
	{
		if (taskTargetId <= 0)
		{
			return null;
		}
		List<Integer> rows;
		if (taskTargetId == BOSS_TASK_TARGET_ID)
		{
			rows = client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID,
				DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID, 0, bossTargetId);
			if (rows.isEmpty())
			{
				return null;
			}
			Object[] taskRows = client.getDBTableField(rows.get(0),
				DBTableID.SlayerTaskSublist.COL_TASK, 0);
			if (taskRows.length == 0 || !(taskRows[0] instanceof Integer))
			{
				return null;
			}
			rows = List.of((Integer) taskRows[0]);
		}
		else
		{
			rows = client.getDBRowsByValue(DBTableID.SlayerTask.ID,
				DBTableID.SlayerTask.COL_ID, 0, taskTargetId);
			if (rows.isEmpty())
			{
				return null;
			}
		}
		Object[] names = client.getDBTableField(rows.get(0),
			DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0);
		return names.length > 0 && names[0] instanceof String ? (String) names[0] : null;
	}

	private static boolean hasOnlySlayerLevel(Transport transport)
	{
		int[] levels = transport.getSkillLevels();
		for (int index = 0; index < levels.length; index++)
		{
			int expected = index == Skill.SLAYER.ordinal() ? REQUIRED_SLAYER_LEVEL : 0;
			if (levels[index] != expected)
			{
				return false;
			}
		}
		return true;
	}

	private static boolean hasActiveTaskCountGate(Transport transport)
	{
		if (transport.getVarplayers().size() != 1)
		{
			return false;
		}
		TransportVarPlayer gate = transport.getVarplayers().iterator().next();
		return gate.getVarplayerId() == VarPlayerID.SLAYER_COUNT && gate.getValue() == 0
			&& gate.getOperator() == TransportVarPlayer.Operator.GREATER_THAN;
	}

	private static String point(WorldPoint point)
	{
		return point.getX() + "," + point.getY() + "," + point.getPlane();
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	public static final class AccessSnapshot
	{
		private final int boostedLevel;
		private final int taskCount;
		private final int taskTargetId;
		private final int bossTargetId;
		private final String taskName;
		private final boolean available;

		AccessSnapshot(int boostedLevel, int taskCount, int taskTargetId, int bossTargetId,
			String taskName, boolean available)
		{
			this.boostedLevel = boostedLevel;
			this.taskCount = taskCount;
			this.taskTargetId = taskTargetId;
			this.bossTargetId = bossTargetId;
			this.taskName = taskName;
			this.available = available;
		}

		public static AccessSnapshot unavailable()
		{
			return new AccessSnapshot(0, 0, 0, 0, null, false);
		}

		public int getBoostedLevel()
		{
			return boostedLevel;
		}

		public int getTaskCount()
		{
			return taskCount;
		}

		public int getTaskTargetId()
		{
			return taskTargetId;
		}

		public int getBossTargetId()
		{
			return bossTargetId;
		}

		public String getTaskName()
		{
			return taskName;
		}

		public boolean isAvailable()
		{
			return available;
		}
	}
}
