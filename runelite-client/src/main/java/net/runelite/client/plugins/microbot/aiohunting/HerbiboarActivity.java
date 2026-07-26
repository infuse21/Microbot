package net.runelite.client.plugins.microbot.aiohunting;

import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

/**
 * Self-contained Herbiboar activity. Follows a trail of search spots to a tunnel,
 * flushes the Herbiboar and harvests it. Needs no supplies or banking of its own; it
 * only chooses which trail object to interact with next based on the trail varbits.
 *
 * <p>Second module of the AIO Hunting modular refactor, mirroring {@link BirdhouseActivity}.</p>
 */
final class HerbiboarActivity
{
	static final int AREA_RADIUS = 75;
	private static final int TRAIL_WALK_DISTANCE = 8;

	private final AIOHuntingConfig config;
	private final AIOHuntingScript script;

	HerbiboarActivity(AIOHuntingConfig config, AIOHuntingScript script)
	{
		this.config = config;
		this.script = script;
	}

	int areaRadius()
	{
		return AREA_RADIUS;
	}

	void step()
	{
		if (Rs2Player.isAnimating() || Rs2Player.isMoving())
		{
			return;
		}

		Rs2NpcModel herbiboar = Microbot.getRs2NpcCache().query()
			.withName("Herbiboar")
			.where(npc -> script.isInsideArea(npc.getWorldLocation()))
			.nearestOnClientThread();
		if (herbiboar != null)
		{
			script.setStatus("Harvesting Herbiboar");
			herbiboar.click("Harvest");
			return;
		}

		int finishId = HerbiboarData.getFinishId();
		if (finishId > 0 && finishId <= HerbiboarData.END_LOCATIONS.size())
		{
			WorldPoint finish = HerbiboarData.END_LOCATIONS.get(finishId - 1);
			if (!script.walkNear(finish, TRAIL_WALK_DISTANCE))
			{
				return;
			}
			Rs2TileObjectModel tunnel =
				script.findObjectAtWithActions(finish, 1, "Attack", "Search");
			if (tunnel != null)
			{
				script.setStatus("Flushing Herbiboar");
				String action = AIOHuntingScript.hasAction(tunnel.getObjectComposition(), "Attack")
					? "Attack" : "Search";
				tunnel.click(action);
			}
			return;
		}

		WorldPoint nextSpot = HerbiboarData.findNextSearchSpot();
		if (nextSpot != null)
		{
			if (!script.walkNear(nextSpot, TRAIL_WALK_DISTANCE))
			{
				return;
			}
			Rs2TileObjectModel trailObject =
				script.findObjectAtWithActions(nextSpot, 1, "Search");
			if (trailObject != null)
			{
				script.setStatus("Following Herbiboar trail");
				trailObject.click("Search");
			}
			return;
		}

		Rs2TileObjectModel start = Microbot.getRs2TileObjectCache().query()
			.where(object -> HerbiboarData.START_OBJECT_IDS.contains(object.getId()))
			.where(object -> script.isInsideArea(object.getWorldLocation()))
			.nearestOnClientThread();
		if (start != null)
		{
			if (!script.walkNear(start.getWorldLocation(), TRAIL_WALK_DISTANCE))
			{
				return;
			}
			script.setStatus("Starting Herbiboar trail");
			start.click("Search");
		}
	}

	void collectRetained(Set<Integer> keep)
	{
		if (config.keepHerbSack())
		{
			keep.add(ItemID.SLAYER_HERB_SACK);
			keep.add(ItemID.SLAYER_HERB_SACK_OPEN);
		}
		if (config.keepMagicSecateurs())
		{
			keep.add(ItemID.FAIRY_ENCHANTED_SECATEURS);
		}
	}
}
