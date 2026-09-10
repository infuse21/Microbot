package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentObservation;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentTransaction;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentPreparation;
import java.util.function.BooleanSupplier;

/** The only Phase 3 boundary permitted to send movement input. */
public interface WalkerActions
{
	/** Read outside the runtime mutex; null means the equipment snapshot is unavailable. */
	default SpellEquipmentObservation observeEquipment() { return null; }
	default SpellEquipmentPreparation observeSpellEquipment(RouteInteraction spell,
		SpellEquipmentTransaction retained) { return null; }

	/** Runs outside the runtime mutex; recheck permission immediately before input. */
	default boolean interactEquipment(RouteInteraction interaction, SpellEquipmentTransaction transaction,
		BooleanSupplier permitted) { return false; }

	/** @return true only when a minimap or canvas walk command was actually issued. */
	boolean clickTile(WorldPoint target);

	/** Selection-aware movement hook; adapters may specialize short interaction crossings. */
	default boolean clickTile(WorldPoint target, String selection)
	{
		return clickTile(target);
	}

	/** @return true only when the route interaction was actually issued. */
	default boolean interact(RouteInteraction interaction)
	{
		return false;
	}

	/** Diagnostic label for the most recent click attempt. Lambdas retain a neutral default. */
	default String getLastActionType()
	{
		return "adapter";
	}

	/** @return true when input prepared the interaction but did not issue its crossing command. */
	default boolean interactionPreparedOnly()
	{
		return false;
	}
}
