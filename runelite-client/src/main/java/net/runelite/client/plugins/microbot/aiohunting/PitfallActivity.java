package net.runelite.client.plugins.microbot.aiohunting;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingMethod;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.AxeType;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

/**
 * Pitfall hunting (Spined larupia to start): the active tease-and-jump method. The mechanic (per the
 * OSRS wiki + live capture off the agent server):
 * <ol>
 *   <li>Set spiked pits with logs (cut on-site) - the pit object is "Pit"/[Trap] empty, becomes
 *       "Spiked pit"/[Jump,Dismantle] when armed.</li>
 *   <li>Tease the creature (teasing stick from the INVENTORY, not equipped) so it chases you.</li>
 *   <li>Jump a spiked pit ("Jump") with the creature right behind - it then falls in (catch,
 *       probabilistic on Hunter level) or jumps over (fail). A fail leaves the pit set; the creature
 *       won't jump the SAME pit twice in a row, so lure it to another armed pit. Keep two armed.</li>
 *   <li>On a catch the pit becomes "Collapsed trap" - Dismantle it to collect the loot, then re-set.
 *       An unused armed pit also collapses on a timeout (same Dismantle handling).</li>
 * </ol>
 * Combat-adjacent - teasing deals melee damage, so it eats/prays and retreats when unsafe.
 *
 * <p>This is the hardest Hunter method; the positioning/timing of the tease-and-jump and keeping the
 * prey from getting stuck still need live tuning.</p>
 */
final class PitfallActivity
{
	private static final int TEASING_STICK_ID = ItemID.HUNTING_TEASING_STICK;
	/** The five Feldip larupia pit spots (impostor objects; name/actions change with state). */
	private static final Set<Integer> PIT_IDS = Set.of(
		ObjectID.HUNTING_PITFALL_7, ObjectID.HUNTING_PITFALL_8, ObjectID.HUNTING_PITFALL_9,
		ObjectID.HUNTING_PITFALL_10, ObjectID.HUNTING_PITFALL_11);
	private static final String EMPTY_NAME = "Pit";
	private static final String ARMED_NAME = "Spiked pit";
	private static final String COLLAPSED_NAME = "Collapsed trap";
	/** Keep as many pits armed as the Hunter level allows (2 at 31 up to 5 at 80) - it alternates
	 * between them since the prey won't jump the same one twice in a row. */
	private static final int DESIRED_PITS = 5;
	/** Larupia gets stuck easily; if it hasn't moved for this many checks, nudge toward it. */
	private static final int STUCK_CHECKS = 3;
	/** Ideal lead: keep the larupia ~3 tiles behind - close enough it crosses, far enough it can't bite. */
	private static final int LARUPIA_CLOSE = 3;
	/** The two tiles you jump between sit exactly 3 apart (pit origin -1 and +2 along its axis). */
	private static final int PIT_SIDE_SPAN = 3;
	/** Larupia animation when it falls into the pit (a catch). */
	private static final int LARUPIA_FALLS_IN_ANIM = 5234;
	/** Larupia animation when it jumps over the pit (a failed attempt). */
	private static final int LARUPIA_JUMPS_OVER_ANIM = 5231;
	/** Give the larupia time to cross after a jump - re-jumping drags it back off the pit. */
	private static final long JUMP_COOLDOWN_MS = 3500L;
	/** Tow hop length - short enough that the larupia's greedy chase can follow every hop. */
	private static final int HOP_TILES = 3;
	private static final int BREADCRUMB_CAP = 32;

	private final AIOHuntingConfig config;
	private final AIOHuntingScript script;
	private final AIOHuntingPlugin plugin;

	/** Index of the single larupia we teased - it follows for ages, so we don't re-tease it. */
	private int teasedIndex = -1;
	private WorldPoint lastJumpedPit;
	/** Larupia tile last check; genuinely stuck = FAR from us AND this hasn't changed (not moving). */
	private WorldPoint lastLarupiaPos;
	private int larupiaFrozenChecks;
	/** Learned per pit (persisted): the two tiles you jump between, 3 apart across the pit's axis. */
	private final PitSideMemory pitSides;
	/** Pits this larupia has already jumped over - it won't take the same one twice in a row. */
	private final Set<WorldPoint> refusedPits = new HashSet<>();
	private WorldPoint jumpPitPending;
	private WorldPoint posBeforeJump;
	private long lastJumpAt;
	/** Which pit we last lured at, and which side the larupia was on when we jumped it. If that side
	 * later flips the larupia actually CROSSED (a real attempt), so that pit is spent for it. */
	private WorldPoint luredPit;
	private WorldPoint larupiaSideAtJump;
	/** Trail of our recent tiles while towing - the larupia followed us through these, so they are a
	 * proven NPC-followable path to retrace when it wedges. */
	private final ArrayDeque<WorldPoint> breadcrumbs = new ArrayDeque<>();
	/** Footprint size of the teased larupia (the big cats are 2x2), for the chase simulation. */
	private int teasedNpcSize = 2;

	PitfallActivity(AIOHuntingConfig config, AIOHuntingScript script, AIOHuntingPlugin plugin)
	{
		this.config = config;
		this.script = script;
		this.plugin = plugin;
		this.pitSides = new PitSideMemory(plugin.getConfigManager());
	}

	void reset()
	{
		teasedIndex = -1;
		lastJumpedPit = null;
		lastLarupiaPos = null;
		larupiaFrozenChecks = 0;
		refusedPits.clear();
		luredPit = null;
		larupiaSideAtJump = null;
		jumpPitPending = null;
		posBeforeJump = null;
		lastJumpAt = 0L;
		breadcrumbs.clear();
	}

	int areaRadius()
	{
		// The five pits + larupia spawns span ~15 tiles from the centre, so cover them comfortably.
		return Math.max(20, config.huntingRadius());
	}

	boolean hasSupplies()
	{
		// Teasing stick is used from the inventory (not equipped). Logs are cut on-site, so we just
		// need the stick, a knife and an axe (worn or in the inventory).
		return Rs2Inventory.hasItem(TEASING_STICK_ID)
			&& Rs2Inventory.hasItem(ItemID.KNIFE)
			&& AxeType.bestHeld() != null;
	}

	boolean withdraw()
	{
		if (!script.withdrawDeficit(TEASING_STICK_ID, 1) || !script.withdrawDeficit(ItemID.KNIFE, 1))
		{
			return false;
		}
		if (AxeType.bestHeld() == null && !withdrawBestAxe())
		{
			return false;
		}
		String food = foodName();
		if (food != null && !Rs2Inventory.hasItem(food) && Rs2Bank.hasBankItem(food, 1))
		{
			Rs2Bank.withdrawX(food, 10);
		}
		return true;
	}

	private boolean withdrawBestAxe()
	{
		AxeType[] axes = AxeType.values();
		for (int index = axes.length - 1; index >= 0; index--)
		{
			AxeType axe = axes[index];
			if (axe.meetsLevel() && Rs2Bank.hasItem(axe.getItemId()))
			{
				return Rs2Bank.withdrawOne(axe.getItemId());
			}
		}
		return false;
	}

	void collectRetained(Set<Integer> keep)
	{
		keep.add(TEASING_STICK_ID);
		keep.add(ItemID.KNIFE);
		keep.add(ItemID.LOGS);
		for (AxeType axe : AxeType.values())
		{
			keep.add(axe.getItemId());
		}
	}

	void step()
	{
		if (handleCombatSafety())
		{
			return;
		}
		// A collapsed trap is either a catch (Dismantle collects the loot) or a timed-out pit
		// (Dismantle clears it); either way dismantle it - it reverts to an empty "Pit" to re-set.
		if (dismantleCollapsedPit())
		{
			return;
		}
		if (ensurePitsArmed())
		{
			return;
		}
		runCatchSequence();
	}

	/**
	 * Combat safety - teasing provokes melee. Keeps Protect from Melee up, eats, and HARD-BLOCKS the
	 * whole loop (so it never just stands there dying) when there's no safety net at all. @return true
	 * if it acted or the loop should not proceed to teasing.
	 */
	private boolean handleCombatSafety()
	{
		String food = foodName();
		boolean hasFood = food != null && Rs2Inventory.hasItem(food);
		boolean prayerProtected = config.pitfallProtectMelee() && hasPrayerPoints();

		if (prayerProtected && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE))
		{
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
		}

		double hp = Rs2Player.getHealthPercentage();
		boolean hpKnown = hp > 0;
		if (hasFood && hpKnown && hp <= config.pitfallEatBelow())
		{
			Rs2Inventory.interact(food, "Eat");
			script.markInteraction();
			return true;
		}
		if (hpKnown && hp <= config.pitfallRetreatBelow() && !hasFood)
		{
			script.setStatus("Low HP - retreating from pitfall");
			script.walkNear(method().getLocation(), 10);
			return true;
		}
		return false;
	}

	private boolean dismantleCollapsedPit()
	{
		Rs2TileObjectModel collapsed = findPitNamed(COLLAPSED_NAME);
		if (collapsed == null)
		{
			return false;
		}
		if (collapsed.click("Dismantle"))
		{
			script.markInteraction();
			script.waitForHuntingAnimation();
		}
		return true;
	}

	/** Keep {@link #DESIRED_PITS} pits armed, cutting logs on-site when out. @return true if it acted. */
	private boolean ensurePitsArmed()
	{
		if (!Rs2Inventory.hasItem(ItemID.KNIFE) || countArmedPits() >= desiredPits())
		{
			return false;
		}
		Rs2TileObjectModel empty = findPitNamed(EMPTY_NAME);
		if (empty == null)
		{
			return false;
		}
		if (!Rs2Inventory.hasItem(ItemID.LOGS))
		{
			chopLogs();
			return true;
		}
		plugin.expectTrapAt(empty.getWorldLocation());
		if (empty.click("Trap"))
		{
			script.markInteraction();
			script.waitForHuntingAnimation();
		}
		return true;
	}

	/**
	 * Tease ONE larupia once (it follows for ages), then keep moving - jump between the armed pits so
	 * it chases across them (won't jump the same one twice in a row). Never idle: standing still both
	 * takes damage and lets the larupia get stuck; when it does get stuck, moving toward it re-paths it.
	 */
	private void runCatchSequence()
	{
		learnPitSides();

		// Stick with the larupia we teased, but drop it once it's caught or stops chasing us, so we
		// always go and tease a fresh one instead of shadowing a larupia that is no longer ours.
		Rs2NpcModel larupia = teasedIndex >= 0 ? findLarupia(teasedIndex) : null;
		if (larupia != null && (larupia.getAnimation() == LARUPIA_FALLS_IN_ANIM
			|| plugin.getLastAnimation(teasedIndex) == LARUPIA_FALLS_IN_ANIM))
		{
			plugin.clearLastAnimation(teasedIndex);
			// It's falling into the pit - stand still and let it die, then the collapsed trap gets
			// dismantled (loot) and re-armed. Jumping now would drag it back off the pit.
			teasedIndex = -1;
			return;
		}
		if (larupia != null && !larupia.isInteracting())
		{
			// Lost aggro (or it was never ours) - re-tease.
			larupia = null;
		}
		if (larupia != null && !script.isInsideArea(larupia.getWorldLocation()))
		{
			// It wandered out of the pit area - drop it rather than chasing it across Feldip.
			teasedIndex = -1;
			larupia = null;
		}
		if (larupia == null)
		{
			larupia = nearestLarupia();
			if (larupia == null)
			{
				teasedIndex = -1;
				return;
			}
			if (larupia.click("Tease"))
			{
				teasedIndex = larupia.getIndex();
				teasedNpcSize = npcSize(larupia);
				// A fresh larupia has no history - any pit is fair game again, and the old trail
				// proves nothing about it.
				refusedPits.clear();
				lastJumpedPit = null;
				luredPit = null;
				larupiaSideAtJump = null;
				breadcrumbs.clear();
				script.markInteraction();
			}
			return;
		}

		WorldPoint player = Rs2Player.getWorldLocation();
		WorldPoint larupiaLoc = larupia.getWorldLocation();
		if (player == null || larupiaLoc == null)
		{
			return;
		}
		// Breadcrumbs: the tiles we tow it through are, by definition, a path it can follow - keep
		// the recent trail so a wedge can be recovered by retracing it.
		if (breadcrumbs.isEmpty() || !player.equals(breadcrumbs.peekLast()))
		{
			breadcrumbs.addLast(player);
			while (breadcrumbs.size() > BREADCRUMB_CAP)
			{
				breadcrumbs.removeFirst();
			}
		}
		// Wedged = far from us AND its tile frozen across several checks (an NPC that stopped fires
		// no animation event, so this is positional by necessity). Recover by retracing the trail.
		if (player.distanceTo(larupiaLoc) > LARUPIA_CLOSE && larupiaLoc.equals(lastLarupiaPos))
		{
			if (++larupiaFrozenChecks >= STUCK_CHECKS)
			{
				larupiaFrozenChecks = 0;
				recoverStuckLarupia(larupiaLoc);
				return;
			}
		}
		else
		{
			larupiaFrozenChecks = 0;
		}
		lastLarupiaPos = larupiaLoc;
		// Outcome comes straight from the event-driven animation record, so a one-tick state is never
		// missed: JUMPED OVER means that pit is spent for this larupia and we must lead it to another.
		int outcome = plugin.getLastAnimation(teasedIndex);
		if (outcome == LARUPIA_JUMPS_OVER_ANIM)
		{
			plugin.clearLastAnimation(teasedIndex);
			if (luredPit != null)
			{
				refusedPits.add(luredPit);
				luredPit = null;
				larupiaSideAtJump = null;
			}
		}

		Rs2TileObjectModel pit = pickArmedPit();
		if (pit == null)
		{
			return;
		}
		WorldPoint pitLoc = pit.getWorldLocation();
		WorldPoint[] sides = pitSides.get(pitLoc);
		if (sides == null)
		{
			// Geometry not learned yet: jump once (the action pathfinds) and learn the two side tiles
			// from where we start and land.
			if (player.distanceTo(larupiaLoc) > LARUPIA_CLOSE)
			{
				// Close the gap but STOP SHORT - walking onto its tile just donates free bites.
				Rs2Walker.walkFastCanvas(standOffFrom(larupiaLoc, player));
				return;
			}
			jumpPit(pit, pitLoc, player);
			return;
		}

		// Known geometry. Every observed catch had player, pit and larupia COLLINEAR: stand on the
		// larupia's side, let it close in behind, then jump to the far side - it paths straight at us
		// THROUGH the pit and falls in.
		WorldPoint nearSide = nearestSide(larupiaLoc, sides);
		if (!player.equals(sides[0]) && !player.equals(sides[1]))
		{
			// Towing leg: move to the larupia's side of the pit in short, validated hops. Wait for it
			// to close within the tether first - outrunning it is how it wedges on scenery.
			if (player.distanceTo(larupiaLoc) > LARUPIA_CLOSE)
			{
				return;
			}
			hopTowards(nearSide, player, larupiaLoc);
			return;
		}
		if (player.equals(nearSide))
		{
			// Same side as the larupia: wait for it to line up right behind, then jump away so it
			// paths straight at us through the pit.
			if (player.distanceTo(larupiaLoc) > LARUPIA_CLOSE)
			{
				return;
			}
			jumpPit(pit, pitLoc, player);
			return;
		}
		// The larupia pathed to the OPPOSITE edge. Rather than walk around, jump across to its side -
		// next tick we jump straight back, which forces it to either jump the pit or fall in.
		jumpPit(pit, pitLoc, player);
	}

	private void jumpPit(Rs2TileObjectModel pit, WorldPoint pitLoc, WorldPoint player)
	{
		// Don't spam jumps - the larupia needs a few seconds to walk onto the pit after we cross.
		if (System.currentTimeMillis() - lastJumpAt < JUMP_COOLDOWN_MS)
		{
			// ...but never stand still in biting range while waiting: back off to the other side tile.
			WorldPoint[] sides = pitSides.get(pitLoc);
			Rs2NpcModel tracked = teasedIndex >= 0 ? findLarupia(teasedIndex) : null;
			WorldPoint mob = tracked == null ? null : tracked.getWorldLocation();
			if (sides != null && mob != null && player.distanceTo(mob) <= 1)
			{
				WorldPoint away = nearestSide(mob, sides).equals(sides[0]) ? sides[1] : sides[0];
				Rs2Walker.walkFastCanvas(away);
			}
			return;
		}
		posBeforeJump = player;
		jumpPitPending = pitLoc;
		lastJumpedPit = pitLoc;
		if (pit.click("Jump"))
		{
			lastJumpAt = System.currentTimeMillis();
			// Remember which side the larupia was on, so we can tell later whether it really crossed.
			WorldPoint[] sides = pitSides.get(pitLoc);
			Rs2NpcModel larupia = teasedIndex >= 0 ? findLarupia(teasedIndex) : null;
			WorldPoint loc = larupia == null ? null : larupia.getWorldLocation();
			if (sides != null && loc != null)
			{
				luredPit = pitLoc;
				larupiaSideAtJump = nearestSide(loc, sides);
			}
			script.markInteraction();
		}
	}

	/** Which of the pit's two side tiles {@code point} is on. */
	private static WorldPoint nearestSide(WorldPoint point, WorldPoint[] sides)
	{
		return point.distanceTo(sides[0]) <= point.distanceTo(sides[1]) ? sides[0] : sides[1];
	}

	/**
	 * Learns a pit's two jump-from tiles by watching a completed jump - they sit exactly
	 * {@link #PIT_SIDE_SPAN} apart along the pit's axis (the pit occupies the two tiles between).
	 * Self-learning, so no per-pit coordinates need hard-coding.
	 */
	private void learnPitSides()
	{
		if (jumpPitPending == null || posBeforeJump == null)
		{
			return;
		}
		WorldPoint now = Rs2Player.getWorldLocation();
		if (now == null || now.equals(posBeforeJump))
		{
			return;
		}
		int dx = Math.abs(now.getX() - posBeforeJump.getX());
		int dy = Math.abs(now.getY() - posBeforeJump.getY());
		if ((dx == PIT_SIDE_SPAN && dy == 0) || (dy == PIT_SIDE_SPAN && dx == 0))
		{
			pitSides.record(jumpPitPending, posBeforeJump, now);
		}
		jumpPitPending = null;
		posBeforeJump = null;
	}

	/**
	 * Tow the larupia toward {@code destination} in short hops, each validated with the chase
	 * simulator so its dumb greedy pathing can follow every hop. One long click outruns it straight
	 * into scenery wedges - it can only follow while we stay close and on a followable line.
	 */
	private void hopTowards(WorldPoint destination, WorldPoint player, WorldPoint larupiaLoc)
	{
		NpcChaseSim.Snapshot sim = NpcChaseSim.capture();
		if (sim == null)
		{
			Rs2Walker.walkFastCanvas(destination);
			return;
		}
		List<WorldPoint> path = sim.playerPath(player, destination, 25);
		if (path.size() < 2)
		{
			Rs2Walker.walkFastCanvas(destination);
			return;
		}
		for (int hop = Math.min(HOP_TILES, path.size() - 1); hop >= 1; hop--)
		{
			WorldPoint tile = path.get(hop);
			if (sim.canNpcReach(larupiaLoc, teasedNpcSize, tile, 24))
			{
				Rs2Walker.walkFastCanvas(tile);
				return;
			}
		}
		// Even a one-tile hop would wedge it - reel it back in along the trail instead.
		recoverStuckLarupia(larupiaLoc);
	}

	/**
	 * The larupia stopped following (wedged on scenery its greedy pathing cannot round - e.g. the
	 * vine pockets in Feldip). Retrace our breadcrumb trail to the newest tile it can provably reach
	 * (it followed us through those tiles, so they are a known-good route) and resume towing.
	 */
	private void recoverStuckLarupia(WorldPoint larupiaLoc)
	{
		NpcChaseSim.Snapshot sim = NpcChaseSim.capture();
		if (sim != null)
		{
			for (Iterator<WorldPoint> trail = breadcrumbs.descendingIterator(); trail.hasNext(); )
			{
				WorldPoint crumb = trail.next();
				if (crumb.distanceTo(larupiaLoc) <= 10
					&& sim.canNpcReach(larupiaLoc, teasedNpcSize, crumb, 24))
				{
					Rs2Walker.walkFastCanvas(crumb);
					return;
				}
			}
		}
		// No trail tile works - stand right next to it; an adjacent target always frees the greedy step.
		script.walkNear(larupiaLoc, 1);
	}

	/** Footprint size of the NPC (the larupia-family cats are 2x2), read once on the client thread. */
	private static int npcSize(Rs2NpcModel npc)
	{
		Integer size = Microbot.getClientThread().runOnClientThreadOptional(() -> {
			NPCComposition composition = npc.getNpc().getTransformedComposition();
			if (composition == null)
			{
				composition = npc.getNpc().getComposition();
			}
			return composition == null ? null : composition.getSize();
		}).orElse(null);
		return size == null ? 2 : size;
	}

	/** A tile about {@link #LARUPIA_CLOSE} tiles from {@code mob}, on {@code player}'s side of it. */
	private static WorldPoint standOffFrom(WorldPoint mob, WorldPoint player)
	{
		int dx = player.getX() - mob.getX();
		int dy = player.getY() - mob.getY();
		double len = Math.max(1.0, Math.sqrt((double) dx * dx + (double) dy * dy));
		return new WorldPoint(
			mob.getX() + (int) Math.round(dx / len * LARUPIA_CLOSE),
			mob.getY() + (int) Math.round(dy / len * LARUPIA_CLOSE),
			mob.getPlane());
	}

	/** The orthogonally-adjacent tile to {@code from} in the direction of {@code toward}. */
	private static WorldPoint adjacentTowards(WorldPoint from, WorldPoint toward)
	{
		int dx = toward.getX() - from.getX();
		int dy = toward.getY() - from.getY();
		if (Math.abs(dx) >= Math.abs(dy))
		{
			int step = dx == 0 ? 1 : Integer.signum(dx);
			return new WorldPoint(from.getX() + step, from.getY(), from.getPlane());
		}
		return new WorldPoint(from.getX(), from.getY() + Integer.signum(dy), from.getPlane());
	}

	private static boolean isOrthogonallyAdjacent(WorldPoint a, WorldPoint b)
	{
		return a.getPlane() == b.getPlane()
			&& Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) == 1;
	}

	private Rs2NpcModel nearestLarupia()
	{
		return Microbot.getRs2NpcCache().query()
			.withName(method().getTargetName())
			.where(npc -> script.isInsideArea(npc.getWorldLocation()))
			.nearestOnClientThread();
	}

	private Rs2NpcModel findLarupia(int index)
	{
		return Microbot.getRs2NpcCache().query()
			.withName(method().getTargetName())
			.where(npc -> npc.getIndex() == index)
			.nearestOnClientThread();
	}

	/** Nearest armed pit that isn't the one the prey last refused; falls back to any armed pit. */
	private Rs2TileObjectModel pickArmedPit()
	{
		// A larupia will only take a given pit once - after an attempt you MUST lead it to another pit
		// (and back again after that one). So never re-use the pit we just jumped, and skip any this
		// larupia has already refused. Relying on spotting the jump-over animation is not enough: it
		// lasts a tick and the loop misses it, which left it jumping the same pit back and forth.
		Rs2TileObjectModel pit = Microbot.getRs2TileObjectCache().query()
			.where(object -> PIT_IDS.contains(object.getId()))
			.where(object -> script.isInsideArea(object.getWorldLocation()))
			.where(object -> isPitNamed(object, ARMED_NAME))
			.where(object -> !refusedPits.contains(object.getWorldLocation()))
			.nearestOnClientThread();
		if (pit != null)
		{
			return pit;
		}
		// Nothing else armed (e.g. the other pit just collapsed) - clear refusals and reuse.
		refusedPits.clear();
		return findPitNamed(ARMED_NAME);
	}

	private void chopLogs()
	{
		if (Rs2Player.isAnimating() || Rs2Player.isMoving())
		{
			return;
		}
		Rs2TileObjectModel tree = Microbot.getRs2TileObjectCache().query()
			.withNames("Tree", "Dead tree", "Jungle tree")
			.where(object -> script.isInsideArea(object.getWorldLocation()))
			.nearestOnClientThread();
		if (tree != null)
		{
			// Feldip trees use both "Chop down" and "Chop-down" spellings.
			script.clickFirstAvailable(tree, "Chop down", "Chop-down");
		}
	}

	private Rs2TileObjectModel findPitNamed(String name)
	{
		return Microbot.getRs2TileObjectCache().query()
			.where(object -> PIT_IDS.contains(object.getId()))
			.where(object -> script.isInsideArea(object.getWorldLocation()))
			.where(object -> isPitNamed(object, name))
			.nearestOnClientThread();
	}

	private int countArmedPits()
	{
		return Microbot.getRs2TileObjectCache().query()
			.where(object -> PIT_IDS.contains(object.getId()))
			.where(object -> script.isInsideArea(object.getWorldLocation()))
			.where(object -> isPitNamed(object, ARMED_NAME))
			.count();
	}

	/** Reads the current (impostor-aware) name of a pit object. */
	private static boolean isPitNamed(Rs2TileObjectModel object, String name)
	{
		ObjectComposition composition = object.getObjectComposition();
		return composition != null && name.equalsIgnoreCase(composition.getName());
	}

	private int desiredPits()
	{
		return Math.min(DESIRED_PITS, maxPits());
	}

	private int maxPits()
	{
		int level = Rs2Player.getRealSkillLevel(Skill.HUNTER);
		return level >= 80 ? 5 : level >= 60 ? 4 : level >= 40 ? 3 : level >= 20 ? 2 : 1;
	}

	private boolean hasPrayerPoints()
	{
		return Rs2Player.getBoostedSkillLevel(Skill.PRAYER) > 0;
	}

	private String foodName()
	{
		String food = config.pitfallFood();
		if (food == null)
		{
			return null;
		}
		String trimmed = food.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private HuntingMethod method()
	{
		return script.getActiveMethod();
	}
}
