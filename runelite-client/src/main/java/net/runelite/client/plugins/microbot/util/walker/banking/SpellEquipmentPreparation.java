package net.runelite.client.plugins.microbot.util.walker.banking;

import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;

/** Immutable selection for one observed spell edge. */
public final class SpellEquipmentPreparation
{
	private final RouteInteraction spell;
	private final SpellEquipmentTransaction transaction;
	private final SpellEquipmentObservation equipment;
	private final boolean equipable;

	public SpellEquipmentPreparation(RouteInteraction spell, SpellEquipmentTransaction transaction,
		SpellEquipmentObservation equipment, boolean equipable)
	{
		this.spell = spell;
		this.transaction = transaction;
		this.equipment = equipment;
		this.equipable = equipable;
	}

	public boolean matches(RouteInteraction interaction)
	{
		return interaction != null && spell != null && interaction.getKind() == spell.getKind()
			&& interaction.getGeneration() == spell.getGeneration()
			&& interaction.getRawEdgeIndex() == spell.getRawEdgeIndex()
			&& interaction.getObjectId() == spell.getObjectId()
			&& interaction.getFrom().equals(spell.getFrom()) && interaction.getTo().equals(spell.getTo())
			&& interaction.getAction().equals(spell.getAction());
	}
	public SpellEquipmentTransaction getTransaction() { return transaction; }
	public SpellEquipmentObservation getEquipment() { return equipment; }
	public boolean isEquipable() { return equipable; }
}
