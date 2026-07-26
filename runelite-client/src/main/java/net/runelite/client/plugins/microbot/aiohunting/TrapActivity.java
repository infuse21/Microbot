package net.runelite.client.plugins.microbot.aiohunting;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingMethod;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingStyle;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.AxeType;
import net.runelite.client.plugins.microbot.api.tileitem.models.Rs2TileItemModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

/**
 * The four trap-based Hunter methods: bird snare, box trap, net trap and deadfall.
 * They share one loop — check/reset finished traps, recover failed ones, pick up dropped
 * traps, then lay up to the level-based cap in the current cluster.
 *
 * <p>Trap object recognition lives in {@link TrapData}; trap ownership tracking lives in
 * {@link AIOHuntingPlugin}. Cluster/location movement is still owned by the orchestrator
 * and reached through hooks, since it is shared with travel and the chase methods.</p>
 */
final class TrapActivity
{
	private static final EnumSet<HuntingStyle> TRAP_STYLES = EnumSet.of(
		HuntingStyle.BIRD_SNARE, HuntingStyle.BOX_TRAP,
		HuntingStyle.NET_TRAP, HuntingStyle.DEADFALL);

	private static final int ROPE_ID = ItemID.ROPE;
	private static final int SMALL_FISHING_NET_ID = 303;
	// Deadfalls chop logs from the on-site trees rather than banking them. Cut a small buffer
	// (each trap uses one log) so hunting is only interrupted to chop on every second set-up.
	private static final int DEADFALL_LOG_BUFFER = 2;
	// Evergreen tree - the closest wood at some deadfall spots (e.g. Wild kebbits). Gives regular
	// Logs like normal trees, so we prefer it over the farther "Tree" objects.
	private static final int EVERGREEN_TREE_ID = 2091;
	private static final long PORTABLE_TRAP_RETRY_MS = 1200L;
	private static final int PORTABLE_TRAP_RETRIES_BEFORE_MOVE = 3;
	private static final int SNARE_WALK_ATTEMPTS = 5;

	private final AIOHuntingConfig config;
	private final AIOHuntingScript script;
	private final AIOHuntingPlugin plugin;

	private long lastPortableTrapAttemptAt;
	private int portableTrapFailures;

	/** Bird-spawn tiles being camped for bird snares; each snare is pinned to and re-laid on its tile. */
	private final List<WorldPoint> snareAnchors = new ArrayList<>();
	/** Snare tiles we could not stand on (blocked/unreachable); skipped until we leave the area. */
	private final Set<WorldPoint> blockedSnareTiles = new HashSet<>();
	private WorldPoint pendingSnareTile;
	private int snareWalkFailures;
	/** True while topping up the deadfall log buffer, so we chop to the buffer before setting again. */
	private boolean gatheringDeadfallLogs;
	/** The boulder we're working; once committed we keep returning to it rather than the fallback. */
	private WorldPoint activeDeadfallBoulder;

	TrapActivity(AIOHuntingConfig config, AIOHuntingScript script, AIOHuntingPlugin plugin)
	{
		this.config = config;
		this.script = script;
		this.plugin = plugin;
	}

	void reset()
	{
		lastPortableTrapAttemptAt = 0L;
		portableTrapFailures = 0;
		snareAnchors.clear();
		blockedSnareTiles.clear();
		pendingSnareTile = null;
		snareWalkFailures = 0;
		gatheringDeadfallLogs = false;
		activeDeadfallBoulder = null;
	}

	boolean isTrapMethod()
	{
		return TRAP_STYLES.contains(style());
	}

	int countActiveTraps()
	{
		if (!isTrapMethod())
		{
			return 0;
		}
		return Microbot.getRs2TileObjectCache().query()
			.where(object -> TrapData.ALL_TRAP_OBJECTS.contains(object.getId()))
			.where(object -> script.isInsideArea(object.getWorldLocation()))
			.where(object -> plugin.ownsTrapNear(object.getWorldLocation()))
			.count();
	}

	int maxTraps()
	{
		// Deadfalls are the exception to the level-based trap cap: only one can be set at a time.
		if (style() == HuntingStyle.DEADFALL)
		{
			return 1;
		}
		// Every other trap capacity is purely Hunter-level based in OSRS (max 5 at level 80).
		int level = Rs2Player.getRealSkillLevel(Skill.HUNTER);
		return level >= 80 ? 5 : level >= 60 ? 4 : level >= 40 ? 3 : level >= 20 ? 2 : 1;
	}

	boolean hasOutstandingTrap()
	{
		return isTrapMethod()
			&& (countActiveTraps() > 0 || plugin.hasPendingTrapPlacement()
				|| findOwnedDroppedTrap() != null);
	}

	boolean hasSupplies()
	{
		int active = countActiveTraps() + (plugin.hasPendingTrapPlacement() ? 1 : 0);
		int recoverablePortableTrap =
			(style() == HuntingStyle.BIRD_SNARE || style() == HuntingStyle.BOX_TRAP)
				&& findOwnedDroppedTrap() != null ? 1 : 0;
		int required = maxTraps();
		switch (style())
		{
			case BIRD_SNARE:
			case BOX_TRAP:
				return Rs2Inventory.itemQuantity(method().getPrimaryToolId())
					+ active + recoverablePortableTrap >= required;
			case NET_TRAP:
				return Rs2Inventory.itemQuantity(SMALL_FISHING_NET_ID) + active >= required
					&& Rs2Inventory.itemQuantity(ROPE_ID) + active >= required;
			case DEADFALL:
				return Rs2Inventory.hasItem(ItemID.KNIFE)
					&& AxeType.bestHeld() != null;
			default:
				return false;
		}
	}

	boolean withdraw()
	{
		int targetTraps = maxTraps() + config.spareTraps();
		switch (style())
		{
			case BIRD_SNARE:
			case BOX_TRAP:
				return script.withdrawDeficit(method().getPrimaryToolId(), targetTraps);
			case NET_TRAP:
				return script.withdrawDeficit(SMALL_FISHING_NET_ID, targetTraps)
					&& script.withdrawDeficit(ROPE_ID, targetTraps);
			case DEADFALL:
				// No logs are banked - they are chopped from the on-site dead trees while hunting.
				return script.withdrawDeficit(ItemID.KNIFE, 1)
					&& ensureAxe();
			default:
				return false;
		}
	}

	void collectRetained(Set<Integer> keep)
	{
		switch (style())
		{
			case BIRD_SNARE:
				keep.add(ItemID.HUNTING_OJIBWAY_BIRD_SNARE);
				break;
			case BOX_TRAP:
				keep.add(ItemID.HUNTING_BOX_TRAP);
				break;
			case NET_TRAP:
				keep.add(ROPE_ID);
				keep.add(SMALL_FISHING_NET_ID);
				break;
			case DEADFALL:
				keep.add(ItemID.KNIFE);
				for (AxeType axe : AxeType.values())
				{
					keep.add(axe.getItemId());
				}
				keep.add(ItemID.LOGS);
				break;
			default:
				break;
		}
	}

	void step()
	{
		if (script.hasPendingMethod())
		{
			drainTrapsForMethodChange();
			return;
		}
		if (script.hasPendingClusterMove())
		{
			if (hasOutstandingTrap())
			{
				script.setStatus("Clearing traps before changing cluster");
				drainTrapsForMethodChange();
				return;
			}
			script.activatePendingCluster();
			return;
		}
		if (style() == HuntingStyle.DEADFALL)
		{
			adoptExistingDeadfall();
		}
		Rs2TileObjectModel completed = findOwnedTrap(TrapData.fullIds(style()));
		if (completed != null)
		{
			if (script.clickFirstAvailable(completed, completionActions()))
			{
				script.markInteraction();
			}
			return;
		}

		// Deadfall's "failed" object IS the un-set base boulder, so the generic Set-trap recovery
		// would re-arm it directly and skip log gathering (setting with no wood). Route deadfall
		// through setDeadfall() below instead, which chops the logs first.
		if (style() != HuntingStyle.DEADFALL)
		{
			Rs2TileObjectModel failed = findOwnedTrap(TrapData.failedIds(style()));
			if (failed != null)
			{
				if (script.clickFirstAvailable(failed, recoveryActions()))
				{
					script.markInteraction();
				}
				return;
			}
		}

		Rs2TileItemModel droppedTrap = findOwnedDroppedTrap();
		if (droppedTrap != null)
		{
			droppedTrap.pickup();
			return;
		}

		if (script.shouldRotateStalledTrapCluster())
		{
			script.queueNextCluster();
			return;
		}

		if (countActiveTraps() >= maxTraps())
		{
			// Deadfall idles while the trap is armed - use that time to cut the buffer back up once
			// we've hit zero logs, then return to wait right next to our boulder.
			if (style() == HuntingStyle.DEADFALL)
			{
				gatherDeadfallLogs();
				returnToDeadfallBoulder();
			}
			return;
		}

		switch (style())
		{
			case BIRD_SNARE:
			case BOX_TRAP:
				layPortableTrap();
				break;
			case NET_TRAP:
				setFixedTrap(method().getSecondaryId(), "Set-trap");
				break;
			case DEADFALL:
				setDeadfall();
				break;
			default:
				break;
		}
	}

	private void drainTrapsForMethodChange()
	{
		Rs2TileObjectModel completed = findOwnedTrap(TrapData.fullIds(style()));
		if (completed != null)
		{
			script.clickFirstAvailable(completed, "Check", "Dismantle", "Reset");
			return;
		}
		Rs2TileObjectModel failed = findOwnedTrap(TrapData.failedIds(style()));
		if (failed != null)
		{
			script.clickFirstAvailable(failed, "Dismantle", "Check", "Reset");
			return;
		}
		Rs2TileObjectModel open = findOwnedTrap(TrapData.openIds(style()));
		if (open != null)
		{
			script.clickFirstAvailable(open, "Dismantle", "Check");
			return;
		}
		Rs2TileItemModel dropped = findOwnedDroppedTrap();
		if (dropped != null)
		{
			dropped.pickup();
		}
	}

	private void layPortableTrap()
	{
		int toolId = method().getPrimaryToolId();
		if (!Rs2Inventory.hasItem(toolId))
		{
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastPortableTrapAttemptAt < PORTABLE_TRAP_RETRY_MS)
		{
			return;
		}
		WorldPoint player = Rs2Player.getWorldLocation();
		if (player == null)
		{
			return;
		}
		// For bird snares, aim the trap at a bird's spawn tile and camp it. Walking onto that
		// tile spans several ticks; only fall through to lay once we are standing on it.
		WorldPoint tile = resolveSnareTile(player);
		if (!tile.equals(player) && !walkOntoSnareTile(tile))
		{
			return;
		}
		// Never stack a snare on an occupied tile: if we ended up standing on a trap, let the
		// resolver pick a free tile (or walk us forward off it) on the next tick instead.
		if (hasTrapAt(tile))
		{
			return;
		}
		lastPortableTrapAttemptAt = now;
		int activeBefore = countActiveTraps();
		plugin.expectTrapAt(tile);
		if (!Rs2Inventory.interact(toolId, "Lay"))
		{
			plugin.cancelExpectedTrap();
			handlePortableTrapFailure();
			return;
		}
		if (script.awaitCondition(() -> countActiveTraps() > activeBefore || script.isPaused(), 3000)
			&& !script.isPaused())
		{
			portableTrapFailures = 0;
			pendingSnareTile = null;
			snareWalkFailures = 0;
			script.markInteraction();
			return;
		}
		handlePortableTrapFailure();
	}

	/**
	 * The tile to lay the next portable trap on. For bird snares with spawn-camping enabled this is
	 * a bird's tile (an approximate spawn), remembered per trap so the snare is always re-laid on the
	 * same tile; otherwise (and as a fallback when no bird is nearby) it is the player's own tile,
	 * preserving the legacy lay-where-you-stand behaviour.
	 */
	private WorldPoint resolveSnareTile(WorldPoint player)
	{
		if (style() != HuntingStyle.BIRD_SNARE || !config.campBirdSpawns())
		{
			return player;
		}
		blockedSnareTiles.removeIf(tile -> !script.isInsideArea(tile));
		// 1. Curated exact tiles for this method when we are camped in that area (best case).
		// Occupancy is tested on the exact tile - the curated tiles sit right next to each other,
		// so a radius check would wrongly treat a free tile as taken by its neighbour's trap.
		for (WorldPoint spot : SnareSpotData.spots(method()))
		{
			if (!blockedSnareTiles.contains(spot) && script.isInsideArea(spot)
				&& !hasTrapAt(spot))
			{
				return spot;
			}
		}
		// 2. Otherwise fall back to dynamically-learned bird-spawn tiles.
		snareAnchors.removeIf(anchor -> !script.isInsideArea(anchor) || blockedSnareTiles.contains(anchor));
		for (WorldPoint anchor : snareAnchors)
		{
			if (!hasTrapAt(anchor))
			{
				return anchor;
			}
		}
		if (snareAnchors.size() < maxTraps())
		{
			WorldPoint birdTile = nearestUncoveredBirdTile();
			if (birdTile != null)
			{
				snareAnchors.add(birdTile);
				return birdTile;
			}
		}
		return player;
	}

	/** @return true once standing exactly on {@code tile} (ready to lay), false while still routing. */
	private boolean walkOntoSnareTile(WorldPoint tile)
	{
		WorldPoint player = Rs2Player.getWorldLocation();
		if (tile.equals(player))
		{
			pendingSnareTile = null;
			snareWalkFailures = 0;
			return true;
		}
		if (Rs2Player.isMoving())
		{
			return false;
		}
		if (tile.equals(pendingSnareTile))
		{
			// Stopped short of the tile without arriving: it may be blocked/unreachable. Give it a
			// few tries, then abandon this anchor and let the caller lay where it stands instead.
			if (++snareWalkFailures >= SNARE_WALK_ATTEMPTS)
			{
				blockedSnareTiles.add(tile);
				snareAnchors.remove(tile);
				pendingSnareTile = null;
				snareWalkFailures = 0;
				return false;
			}
		}
		else
		{
			pendingSnareTile = tile;
			snareWalkFailures = 0;
		}
		Rs2Walker.walkFastCanvas(tile);
		return false;
	}

	private WorldPoint nearestUncoveredBirdTile()
	{
		Rs2NpcModel bird = Microbot.getRs2NpcCache().query()
			.withName(method().getTargetName())
			.where(npc -> script.isInsideArea(npc.getWorldLocation()))
			.where(npc -> !isSnareTileCovered(npc.getWorldLocation()))
			.nearestOnClientThread();
		return bird == null ? null : bird.getWorldLocation();
	}

	/** True when a tile already has one of our traps or an adjacent camp anchor claiming it. */
	private boolean isSnareTileCovered(WorldPoint tile)
	{
		if (tile == null)
		{
			return true;
		}
		if (plugin.ownsTrapNear(tile) || blockedSnareTiles.contains(tile))
		{
			return true;
		}
		return snareAnchors.stream().anyMatch(anchor -> anchor.getPlane() == tile.getPlane()
			&& anchor.distanceTo(tile) <= 1);
	}

	/** True when a hunter trap object sits on exactly this tile (radius-0, so adjacent tiles differ). */
	private boolean hasTrapAt(WorldPoint tile)
	{
		if (tile == null)
		{
			return false;
		}
		return Microbot.getRs2TileObjectCache().query()
			.where(object -> TrapData.ALL_TRAP_OBJECTS.contains(object.getId()))
			.where(object -> tile.equals(object.getWorldLocation()))
			.count() > 0;
	}

	private void handlePortableTrapFailure()
	{
		portableTrapFailures++;
		if (portableTrapFailures < PORTABLE_TRAP_RETRIES_BEFORE_MOVE)
		{
			script.setStatus("Retrying trap placement");
			return;
		}
		portableTrapFailures = 0;
		plugin.cancelExpectedTrap();
		script.queueNextCluster();
	}

	private void setFixedTrap(int baseObjectId, String action)
	{
		Rs2TileObjectModel base = Microbot.getRs2TileObjectCache().query()
			.withId(baseObjectId)
			.where(object -> script.isInsideArea(object.getWorldLocation()))
			.nearestOnClientThread();
		if (base == null)
		{
			script.setRecoveryReason("No usable trap point found nearby");
			return;
		}
		armFixedTrap(base, action);
	}

	private void armFixedTrap(Rs2TileObjectModel base, String action)
	{
		plugin.expectTrapAt(base.getWorldLocation());
		if (base.click(action))
		{
			script.markInteraction();
			script.waitForHuntingAnimation();
		}
	}

	/**
	 * The deadfall boulder to set next. When the method has a priority list (see {@link DeadfallSpotData})
	 * we walk it in order and pick the first boulder whose base object is still present - a missing base
	 * means someone has armed that boulder, so we fall through to the next. Falls back to the nearest
	 * boulder in the area for methods without a list.
	 */
	private Rs2TileObjectModel resolveDeadfallBoulder()
	{
		// Already committed to a boulder (ours): keep returning to it. A missing base object there
		// means our own trap is armed/caught (or it's out of view) - not that it's someone else's,
		// so we must NOT fall through to a different boulder. Return null and the caller walks back.
		if (activeDeadfallBoulder != null)
		{
			return boulderAt(activeDeadfallBoulder);
		}
		List<WorldPoint> preferred = DeadfallSpotData.boulders(method());
		if (!preferred.isEmpty())
		{
			for (WorldPoint tile : preferred)
			{
				Rs2TileObjectModel boulder = boulderAt(tile);
				if (boulder != null)
				{
					return boulder;
				}
			}
			// Every preferred boulder is currently in use - wait for one to free up.
			return null;
		}
		return Microbot.getRs2TileObjectCache().query()
			.withId(ObjectID.HUNTING_DEADFALL_BOULDER)
			.where(object -> script.isInsideArea(object.getWorldLocation()))
			.nearestOnClientThread();
	}

	/** The base deadfall boulder object at exactly {@code tile}, or null (in use / out of view). */
	private Rs2TileObjectModel boulderAt(WorldPoint tile)
	{
		return Microbot.getRs2TileObjectCache().query()
			.withId(ObjectID.HUNTING_DEADFALL_BOULDER)
			.where(object -> tile.equals(object.getWorldLocation()))
			.nearestOnClientThread();
	}

	/**
	 * Claim a deadfall the player set manually (before the script started) so we don't walk off to
	 * set a second one. RuneLite's HunterPlugin only records traps it saw spawn while enabled, so it
	 * can't help for a pre-existing trap; instead we read the current scene. Deadfalls are fixed,
	 * single-occupancy boulders, so an armed/full one in our area is safe to adopt as ours.
	 */
	private void adoptExistingDeadfall()
	{
		Set<Integer> open = TrapData.openIds(HuntingStyle.DEADFALL);
		Set<Integer> full = TrapData.fullIds(HuntingStyle.DEADFALL);
		Rs2TileObjectModel existing = Microbot.getRs2TileObjectCache().query()
			.where(object -> open.contains(object.getId()) || full.contains(object.getId()))
			.where(object -> script.isInsideArea(object.getWorldLocation()))
			.where(object -> !plugin.ownsTrapNear(object.getWorldLocation()))
			.nearestOnClientThread();
		if (existing != null)
		{
			plugin.adoptTrapAt(existing.getWorldLocation());
			// Commit to the boulder the player chose so we keep returning to it, not a fallback.
			activeDeadfallBoulder = existing.getWorldLocation();
		}
	}

	private void setDeadfall()
	{
		// Free the inventory slot: wield the axe now the bank is closed (couldn't be done at the bank).
		equipAxeIfNeeded();
		if (Rs2Inventory.itemQuantity(ItemID.LOGS) >= 1)
		{
			// Have a log - set the trap now, spending the spare. The buffer is topped back up while
			// the trap is armed (step()'s wait branch), so we never idle without a trap out.
			gatheringDeadfallLogs = false;
			Rs2TileObjectModel boulder = resolveDeadfallBoulder();
			if (boulder == null)
			{
				if (activeDeadfallBoulder != null)
				{
					// Our boulder isn't a base right now (armed/caught, or we walked off to the
					// trees and it's out of view) - go back to it, don't set a second trap elsewhere.
					script.walkNear(activeDeadfallBoulder, 2);
				}
				else
				{
					script.setRecoveryReason("Waiting for a deadfall boulder to free up");
				}
				return;
			}
			activeDeadfallBoulder = boulder.getWorldLocation();
			armFixedTrap(boulder, "Set-trap");
			return;
		}
		// No logs and no armed trap to gather behind (e.g. the very first set) - cut enough to set.
		gatherDeadfallLogs();
	}

	/**
	 * Once the log buffer is full, wait right next to our boulder instead of idling out by the trees,
	 * so we can check and re-set the moment it catches. Only walks when we've actually strayed.
	 */
	private void returnToDeadfallBoulder()
	{
		if (gatheringDeadfallLogs || activeDeadfallBoulder == null)
		{
			return;
		}
		WorldPoint player = Rs2Player.getWorldLocation();
		if (player != null && player.distanceTo(activeDeadfallBoulder) > 2)
		{
			script.walkNear(activeDeadfallBoulder, 1);
		}
	}

	/**
	 * Keeps the deadfall log buffer topped up: once we hit zero logs we chop back up to the buffer
	 * (latched so we don't stop after one), a log per tree, preferring the nearest tree. Called both
	 * while the trap is armed (to pre-cut the spares) and when we have no logs at all to set with.
	 */
	private void gatherDeadfallLogs()
	{
		int logs = Rs2Inventory.itemQuantity(ItemID.LOGS);
		if (logs == 0)
		{
			gatheringDeadfallLogs = true;
		}
		else if (logs >= DEADFALL_LOG_BUFFER)
		{
			gatheringDeadfallLogs = false;
		}
		if (gatheringDeadfallLogs)
		{
			chopDeadfallLogs();
		}
	}

	/** @return true if we're chopping / walking to a tree, false if none is available to chop. */
	private boolean chopDeadfallLogs()
	{
		if (Rs2Player.isAnimating() || Rs2Player.isMoving())
		{
			return true;
		}
		// Prefer the nearest evergreen (closer at spots like Wild kebbits), then fall back to any
		// normal tree. Both give regular Logs for the deadfall.
		Rs2TileObjectModel tree = Microbot.getRs2TileObjectCache().query()
			.withId(EVERGREEN_TREE_ID)
			.where(object -> script.isInsideArea(object.getWorldLocation()))
			.nearestOnClientThread();
		if (tree == null)
		{
			tree = Microbot.getRs2TileObjectCache().query()
				.withNames("Tree", "Dead tree")
				.where(object -> script.isInsideArea(object.getWorldLocation()))
				.nearestOnClientThread();
		}
		if (tree == null)
		{
			return false;
		}
		tree.click("Chop down");
		return true;
	}

	private Rs2TileObjectModel findOwnedTrap(Set<Integer> ids)
	{
		return Microbot.getRs2TileObjectCache().query()
			.where(object -> ids.contains(object.getId()))
			.where(object -> script.isInsideArea(object.getWorldLocation()))
			.where(object -> plugin.ownsTrapNear(object.getWorldLocation()))
			.nearestOnClientThread();
	}

	private Rs2TileItemModel findOwnedDroppedTrap()
	{
		int trapId;
		if (style() == HuntingStyle.BIRD_SNARE)
		{
			trapId = ItemID.HUNTING_OJIBWAY_BIRD_SNARE;
		}
		else if (style() == HuntingStyle.BOX_TRAP)
		{
			trapId = ItemID.HUNTING_BOX_TRAP;
		}
		else if (style() == HuntingStyle.NET_TRAP)
		{
			Rs2TileItemModel rope = findOwnedGroundItem(ROPE_ID);
			return rope != null ? rope : findOwnedGroundItem(SMALL_FISHING_NET_ID);
		}
		else
		{
			return null;
		}
		return findOwnedGroundItem(trapId);
	}

	private Rs2TileItemModel findOwnedGroundItem(int itemId)
	{
		return Microbot.getRs2TileItemCache().query()
			.withId(itemId)
			.where(item -> item.isOwned() && script.isInsideArea(item.getWorldLocation()))
			.where(item -> plugin.ownsTrapNear(item.getWorldLocation()))
			.nearest();
	}

	private String[] completionActions()
	{
		switch (style())
		{
			case BIRD_SNARE:
			case DEADFALL:
				return new String[]{"Check", "Dismantle"};
			case BOX_TRAP:
			case NET_TRAP:
				return new String[]{"Reset", "Check"};
			default:
				return new String[0];
		}
	}

	private String[] recoveryActions()
	{
		switch (style())
		{
			case BIRD_SNARE:
				return new String[]{"Dismantle", "Check"};
			case BOX_TRAP:
			case NET_TRAP:
				return new String[]{"Reset", "Dismantle"};
			case DEADFALL:
				return new String[]{"Set-trap"};
			default:
				return new String[0];
		}
	}

	private boolean ensureAxe()
	{
		// Already wielding a woodcutting axe: nothing to do.
		if (isWearingAxe())
		{
			return true;
		}
		// Equip the axe straight from the bank (opcode-9 wield works while the bank is open) so it
		// never eats an inventory slot. Whatever weapon is currently worn gets bumped into the
		// inventory when the axe takes the slot, so bank it (e.g. a noose wand left on from tracking).
		Rs2ItemModel priorWeapon = Rs2Equipment.get(EquipmentInventorySlot.WEAPON);
		boolean equipped;
		AxeType heldAxe = AxeType.bestHeld();
		if (heldAxe != null)
		{
			equipped = Rs2Bank.wearItem(heldAxe.getItemId());
		}
		else
		{
			equipped = withdrawBestAxeAndEquip();
		}
		if (equipped && priorWeapon != null && !isAxeId(priorWeapon.getId()))
		{
			script.awaitCondition(() -> Rs2Inventory.hasItem(priorWeapon.getId()), 1200);
			Rs2Bank.depositOne(priorWeapon.getId());
		}
		return equipped || isWearingAxe();
	}

	/** Withdraws the best woodcutting axe in the bank we meet the level for (best first) and equips it. */
	private boolean withdrawBestAxeAndEquip()
	{
		AxeType[] axes = AxeType.values();
		for (int index = axes.length - 1; index >= 0; index--)
		{
			AxeType axe = axes[index];
			if (axe.meetsLevel() && Rs2Bank.hasItem(axe.getItemId()))
			{
				return Rs2Bank.withdrawAndEquip(axe.getItemId());
			}
		}
		return false;
	}

	/**
	 * Fallback wield when the axe ended up in the inventory rather than equipped (run with the bank
	 * closed, e.g. once at the hunting area). The primary path equips at the bank in {@link #ensureAxe}.
	 */
	private void equipAxeIfNeeded()
	{
		AxeType axe = AxeType.bestHeld();
		if (axe != null && !isWearingAxe() && Rs2Inventory.hasItem(axe.getItemId()))
		{
			Rs2Inventory.wield(axe.getItemId());
		}
	}

	private boolean isWearingAxe()
	{
		Rs2ItemModel weapon = Rs2Equipment.get(EquipmentInventorySlot.WEAPON);
		return weapon != null && isAxeId(weapon.getId());
	}

	private static boolean isAxeId(int id)
	{
		for (AxeType axe : AxeType.values())
		{
			if (axe.getItemId() == id)
			{
				return true;
			}
		}
		return false;
	}

	private HuntingMethod method()
	{
		return script.getActiveMethod();
	}

	private HuntingStyle style()
	{
		return method().getStyle();
	}
}
