package net.runelite.client.plugins.microbot.aiohunting;

import java.util.Set;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;

/**
 * Base archetype for the "chase" Hunter methods: butterflies, falconry and implings.
 * These share a moving-target loop — find the nearest catchable creature inside the
 * active area, interact with it, and roam to another spot when the area is empty.
 *
 * <p>Subclasses supply their own tools/supplies and may hook {@link #preCatch()} for
 * pre-interaction steps (e.g. renting a falcon) or override {@link #onNoTarget()} to opt
 * out of roaming.</p>
 */
abstract class ChaseActivity
{
	protected final AIOHuntingConfig config;
	protected final AIOHuntingScript script;

	ChaseActivity(AIOHuntingConfig config, AIOHuntingScript script)
	{
		this.config = config;
		this.script = script;
	}

	abstract int areaRadius();

	abstract boolean hasSupplies();

	abstract boolean withdraw();

	abstract void collectRetained(Set<Integer> keep);

	void step()
	{
		if (preCatch())
		{
			return;
		}
		Rs2NpcModel target = findTarget();
		if (target != null && target.click("Catch"))
		{
			onCaught();
		}
		else if (target == null)
		{
			onNoTarget();
		}
	}

	protected Rs2NpcModel findTarget()
	{
		return Microbot.getRs2NpcCache().query()
			.withName(script.getActiveMethod().getTargetName())
			.where(npc -> script.isInsideArea(npc.getWorldLocation()))
			.nearestOnClientThread();
	}

	/** @return true to short-circuit this tick (nothing catchable yet). */
	protected boolean preCatch()
	{
		return false;
	}

	protected void onCaught()
	{
		script.setStatus("Catching " + script.getActiveMethod().getDisplayName());
		script.resetRoaming();
		script.markInteraction();
		script.waitForHuntingAnimation();
	}

	protected void onNoTarget()
	{
		script.considerRoamingClusterMove();
	}
}
