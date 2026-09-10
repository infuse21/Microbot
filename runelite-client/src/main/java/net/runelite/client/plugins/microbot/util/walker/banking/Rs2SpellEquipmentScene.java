package net.runelite.client.plugins.microbot.util.walker.banking;

import java.util.HashSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.Rs2Staff;
import net.runelite.client.plugins.microbot.util.magic.RuneFilter;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spells;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;

/** Equipment restoration adapter; tab input and item input are separate commands. */
public final class Rs2SpellEquipmentScene
{
	private Rs2SpellEquipmentScene() { }

	/** Bank stock is advisory until the coordinator refreshes and confirms withdrawals. */
	public static BankedSpellEquipmentPlanner.Plan plan(List<Map<Runes, Integer>> casts, boolean includeBank)
	{
		if (Microbot.getClient() == null || Microbot.getClientThread() == null) return null;
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			SpellEquipmentObservation equipment = observe();
			if (equipment == null) return null;
			boolean members = Microbot.getClient().getWorldType().contains(WorldType.MEMBERS);
			Map<Runes, Integer> physical = new EnumMap<>(Runes.class);
			physical.putAll(Rs2Magic.getRunes(RuneFilter.builder().includeEquipment(false)
				.includeBank(false).includeComboRunes(false).build()));
			if (members) Rs2Magic.getRs2Tome(equipment.getOffhandId()).getRunes()
				.forEach(rune -> physical.put(rune, Integer.MAX_VALUE));
			Set<Integer> staffIds = new HashSet<>(equipment.getInventoryIds());
			Map<Runes, Integer> bankRunes = new EnumMap<>(Runes.class);
			if (includeBank)
			{
				for (Rs2ItemModel item : Rs2Bank.bankItems())
				{
					if (item.getQuantity() <= 0 || item.isNoted()) continue;
					if (Rs2Magic.getRs2Staff(item.getId()) != Rs2Staff.NONE) staffIds.add(item.getId());
					Runes rune = Runes.byItemId(item.getId());
					if (rune != null) bankRunes.merge(rune, item.getQuantity(),
						(left, right) -> (int) Math.min(Integer.MAX_VALUE, (long) left + right));
				}
			}
			return BankedSpellEquipmentPlanner.choose(casts, physical, bankRunes, staffIds,
				Rs2Magic.getRs2Staff(equipment.getWeaponId()),
				Microbot.getClient().getRealSkillLevel(Skill.ATTACK),
				Microbot.getClient().getRealSkillLevel(Skill.MAGIC), members);
		}).orElse(null);
	}

	public static SpellEquipmentObservation observe()
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			if (Microbot.getClient().getGameState() != GameState.LOGGED_IN) return null;
			// Read both containers in one client-thread call; independent event mirrors can lag.
			ItemContainer equipment = Microbot.getClient().getItemContainer(InventoryID.WORN);
			ItemContainer inventory = Microbot.getClient().getItemContainer(InventoryID.INV);
			if (equipment == null || inventory == null) return null;
			Item[] worn = equipment.getItems();
			Item[] carried = inventory.getItems();
			if (worn == null || carried == null) return null;
			Set<Integer> ids = new HashSet<>();
			int occupied = 0;
			for (Item item : carried)
			{
				if (item != null && item.getId() > 0 && item.getQuantity() > 0)
				{
					ids.add(item.getId());
					occupied++;
				}
			}
			Widget widget = Microbot.getClient().getWidget(ComponentID.INVENTORY_CONTAINER);
			boolean inventoryReady = Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY)
				&& widget != null && !widget.isHidden() && widget.getChildren() != null;
			int weaponSlot = EquipmentInventorySlot.WEAPON.getSlotIdx();
			int shieldSlot = EquipmentInventorySlot.SHIELD.getSlotIdx();
			int weaponId = weaponSlot < worn.length && worn[weaponSlot] != null ? worn[weaponSlot].getId() : -1;
			int shieldId = shieldSlot < worn.length && worn[shieldSlot] != null ? worn[shieldSlot].getId() : -1;
			return new SpellEquipmentObservation(weaponId, shieldId, ids, 28 - occupied,
				inventoryReady, Rs2Tab.isCurrentTab(InterfaceTab.EQUIPMENT));
		}).orElse(null);
	}

	public static SpellEquipmentPreparation prepare(RouteInteraction spell, SpellEquipmentTransaction retained)
	{
		if (spell == null || spell.getKind() != RouteInteraction.Kind.SIMPLE_TELEPORT
			|| spell.getObjectId() != TransportType.TELEPORTATION_SPELL.ordinal()) return null;
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			SpellEquipmentObservation equipment = observe();
			if (equipment == null) return retained == null ? null
				: new SpellEquipmentPreparation(spell, retained, null, false);
			SpellEquipmentTransaction transaction = retained;
			if (transaction == null)
			{
				Rs2Spells magic = Rs2Magic.getRs2Spell(spell.getAction().split(":")[0].trim());
				if (magic == null) return null;
				BankedSpellEquipmentPlanner.Plan selected = plan(List.of(Rs2Magic.getRequiredRunes(magic, 1)), false);
				if (selected == null || selected.getStaff() == Rs2Staff.NONE
					|| selected.getStaff().getItemID() == equipment.getWeaponId()) return null;
				transaction = new SpellEquipmentTransaction(selected.getStaff(), equipment.getWeaponId(), equipment.getOffhandId());
			}
			boolean equipable = transaction.getStaff().canEquip(Microbot.getClient().getRealSkillLevel(Skill.ATTACK),
				Microbot.getClient().getRealSkillLevel(Skill.MAGIC),
				Microbot.getClient().getWorldType().contains(WorldType.MEMBERS));
			return new SpellEquipmentPreparation(spell, transaction, equipment, equipable);
		}).orElse(null);
	}

	public static boolean dispatch(RouteInteraction interaction, SpellEquipmentTransaction transaction)
	{
		return dispatch(interaction, transaction, () -> true);
	}

	public static boolean dispatch(RouteInteraction interaction, SpellEquipmentTransaction transaction,
		BooleanSupplier permitted)
	{
		if (!permitted.getAsBoolean() || interaction == null || transaction == null
			|| interaction.getKind() != RouteInteraction.Kind.SPELL_EQUIPMENT) return false;
		SpellEquipmentObservation current = observe();
		if (current == null) return false;
		boolean preparing = SpellEquipmentTransaction.Action.EQUIP_STAFF.name().equals(interaction.getAction())
			|| SpellEquipmentTransaction.Action.OPEN_INVENTORY.name().equals(interaction.getAction())
				&& interaction.getObjectId() == transaction.getStaff().getItemID()
				&& transaction.getOriginalWeaponId() != transaction.getStaff().getItemID();
		boolean equipable = !preparing || Microbot.getClientThread().runOnClientThreadOptional(() ->
			transaction.getStaff().canEquip(Microbot.getClient().getRealSkillLevel(Skill.ATTACK),
				Microbot.getClient().getRealSkillLevel(Skill.MAGIC),
				Microbot.getClient().getWorldType().contains(WorldType.MEMBERS))).orElse(false);
		SpellEquipmentTransaction.Action action = preparing ? current.preparationAction(transaction, equipable)
			: current.restorationAction(transaction);
		int expectedId = action == SpellEquipmentTransaction.Action.RESTORE_WEAPON
			|| action == SpellEquipmentTransaction.Action.OPEN_INVENTORY && !preparing
			? transaction.getOriginalWeaponId() : transaction.getStaff().getItemID();
		if (!action.name().equals(interaction.getAction()) || expectedId != interaction.getObjectId()) return false;
		if (action == SpellEquipmentTransaction.Action.OPEN_INVENTORY
			|| action == SpellEquipmentTransaction.Action.OPEN_EQUIPMENT)
		{
			return Microbot.getClientThread().runOnClientThreadOptional(() ->
			{
				if (!permitted.getAsBoolean()) return false;
				Microbot.getClient().runScript(915, (action == SpellEquipmentTransaction.Action.OPEN_INVENTORY
					? InterfaceTab.INVENTORY : InterfaceTab.EQUIPMENT).getVarcIntIndex());
				return true;
			}).orElse(false);
		}
		if (action == SpellEquipmentTransaction.Action.REMOVE_STAFF)
			return permitted.getAsBoolean() && Rs2Equipment.unEquip(expectedId);
		if (action != SpellEquipmentTransaction.Action.RESTORE_WEAPON
			&& action != SpellEquipmentTransaction.Action.EQUIP_STAFF) return false;
		Rs2ItemModel item = Rs2Inventory.get(expectedId);
		if (item == null || item.isNoted()) return false;
		String equipAction = equipAction(item.getInventoryActions());
		return equipAction != null && permitted.getAsBoolean() && Rs2Inventory.interact(item, equipAction);
	}

	static String equipAction(String[] actions)
	{
		if (actions == null) return null;
		for (String action : actions)
			if ("Wield".equalsIgnoreCase(action) || "Wear".equalsIgnoreCase(action)
				|| "Equip".equalsIgnoreCase(action)) return action;
		return null;
	}

}
