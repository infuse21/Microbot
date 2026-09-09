package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.FairyRing;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.Arrays;
import java.util.List;

/** Cache-backed live adapter for the non-blocking fairy-ring interaction stages. */
public final class Rs2FairyRingScene implements FairyRingScene
{
	private static final int[] STAFF_IDS = {
		ItemID.DRAMEN_STAFF, ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF
	};
	private static final int TAB_SWITCH_SCRIPT = 915;
	private static final String CONFIGURE = "Configure";
	private static final String RING_CONFIGURE = "Ring-configure";

	@Override
	public FairyRing find(PlannedEdge edge)
	{
		Transport transport = findTransport(edge);
		return transport == null ? null : stage(transport, null, -1);
	}

	@Override
	public FairyRing observe(PlannedEdge edge, String pendingAction, int originalWeaponId)
	{
		Transport transport = findTransport(edge);
		return transport == null ? null : stage(transport, pendingAction, originalWeaponId);
	}

	@Override
	public FairyRing restore(PlannedEdge edge, String pendingAction,
		int originalWeaponId, WorldPoint player)
	{
		// Equipping the required staff can remove this row from the usable
		// transport snapshot because the staff moved out of inventory. Restoration
		// is owned by the immutable pending edge and must not rediscover the row.
		if (edge == null || edge.from() == null || edge.to() == null || player == null
			|| originalWeaponId <= 0
			|| restorationComplete(originalWeaponId,
				Rs2Equipment.isWearing(originalWeaponId)))
		{
			return null;
		}
		boolean inventoryOpened = FairyRingPolicy.isRestoreOpenAction(pendingAction);
		boolean equipPending = FairyRingPolicy.isRestoreAction(pendingAction);
		boolean inventoryReady = inventoryContainerReady();
		// Inventory/equipment caches can briefly expose neither weapon while the
		// teleport and staff swap settle. Absence is not completion: keep the
		// restore-open stage pending until the original item is observable.
		boolean originalWeaponReady = Rs2Inventory.hasItem(originalWeaponId);
		String action = equipPending || inventoryOpened && inventoryReady && originalWeaponReady
			? FairyRingPolicy.restoreAction(originalWeaponId)
			: FairyRingPolicy.restoreOpenAction(originalWeaponId);
		FairyRing.Stage restoreStage = FairyRingPolicy.isRestoreOpenAction(action)
			? FairyRing.Stage.RESTORE_INVENTORY : FairyRing.Stage.RESTORE_WEAPON;
		return new FairyRing(edge.from(), edge.to(), "", player,
			-1, action, restoreStage, originalWeaponId);
	}

	static boolean restorationComplete(int originalWeaponId, boolean originalWeaponWorn)
	{
		return originalWeaponId <= 0 || originalWeaponWorn;
	}

	public static boolean interactObject(PlannedEdge edge, String expectedAction)
	{
		Transport transport = findTransport(edge);
		int sequenceStep = FairyRingPolicy.sequenceStep(expectedAction);
		WorldPoint anchor = transport == null ? null : FairyRingPolicy.sequenceRingAnchor(
			transport.getOrigin(), sequenceStep);
		RingObject ring = transport == null ? null : findRingObject(transport, anchor);
		String requestedAction = sequenceStep < 0 ? expectedAction
			: FairyRingPolicy.sequenceObjectLiveAction(expectedAction);
		String liveAction = ring == null ? null : exactAction(ring.actions, requestedAction);
		return ring != null && liveAction != null && ring.object.click(liveAction);
	}

	private static FairyRing stage(Transport transport, String pendingAction,
		int originalWeaponId)
	{
		WorldPoint origin = transport.getOrigin();
		boolean sequence = FairyRingPolicy.isHideoutSequence(transport);
		int sequenceStep = sequence ? Math.max(0, FairyRingPolicy.sequenceStep(pendingAction)) : -1;
		if (sequence && FairyRingPolicy.isSequenceTeleportAction(pendingAction))
		{
			DialState previous = dialState(FairyRingPolicy.sequenceCode(sequenceStep));
			if (!FairyRingPolicy.sequenceTeleportCompleted(sequenceStep,
				Rs2Player.getWorldLocation(), previous.visible))
			{
				return null;
			}
			sequenceStep++;
		}
		String code = sequence ? FairyRingPolicy.sequenceCode(sequenceStep)
			: FairyRingPolicy.normalizeCode(transport.getDisplayInfo());
		if (!hasStaffRequirementWaiver() && !isWearingStaff())
		{
			int staffId = inventoryStaffId();
			if (staffId <= 0)
			{
				return null;
			}
			int rememberedWeapon = originalWeaponId > 0
				? originalWeaponId : equippedWeaponId();
			return new FairyRing(origin, transport.getDestination(), code, origin, -1,
				FairyRingPolicy.equipAction(staffId), FairyRing.Stage.EQUIP_STAFF,
				rememberedWeapon);
		}

		DialState dials = dialState(code);
		if (dials.visible)
		{
			if (dials.rotationWidgetId > 0)
			{
				String action = sequence
					? FairyRingPolicy.sequenceRotateAction(sequenceStep,
						dials.rotationWidgetId, dials.observedRotation)
					: FairyRingPolicy.rotateAction(dials.rotationWidgetId,
						dials.observedRotation);
				return new FairyRing(origin, transport.getDestination(), code, origin, -1,
					action, FairyRing.Stage.ROTATE, originalWeaponId);
			}
			String action = sequence ? FairyRingPolicy.sequenceTeleportAction(sequenceStep)
				: FairyRingPolicy.TELEPORT_ACTION;
			return new FairyRing(origin, transport.getDestination(), code, origin, -1,
				action, FairyRing.Stage.TELEPORT,
				originalWeaponId);
		}

		if (FairyRingPolicy.TELEPORT_ACTION.equals(pendingAction)
			|| FairyRingPolicy.isRotateAction(pendingAction)
			|| isDirectTravelAction(pendingAction, code))
		{
			return null;
		}

		WorldPoint ringAnchor = sequence
			? FairyRingPolicy.sequenceRingAnchor(origin, sequenceStep) : origin;
		RingObject ring = findRingObject(transport, ringAnchor);
		if (ring == null)
		{
			return null;
		}
		String direct = sequence ? null
			: exactAction(ring.actions, "last-destination (" + code + ")");
		if (!sequence && direct == null)
		{
			direct = exactAction(ring.actions, "Ring-last-destination (" + code + ")");
		}
		String action = direct != null ? direct : exactAction(ring.actions, CONFIGURE);
		if (action == null)
		{
			action = exactAction(ring.actions, RING_CONFIGURE);
		}
		if (action == null)
		{
			return null;
		}
		String publishedAction = sequence
			? FairyRingPolicy.sequenceObjectAction(sequenceStep, action) : action;
		return new FairyRing(origin, transport.getDestination(), code, ring.tile,
			ring.object.getId(), publishedAction, FairyRing.Stage.OBJECT, originalWeaponId);
	}

	private static Transport findTransport(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(FairyRingPolicy::isEligible).findFirst().orElse(null);
	}

	private static RingObject findRingObject(Transport transport)
	{
		return findRingObject(transport, transport.getOrigin());
	}

	private static RingObject findRingObject(Transport transport, WorldPoint anchor)
	{
		if (anchor == null)
		{
			return null;
		}
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			List<Rs2TileObjectModel> candidates = Microbot.getRs2TileObjectCache().query()
				.within(anchor, 2).toList();
			RingObject best = null;
			int bestDistance = Integer.MAX_VALUE;
			for (Rs2TileObjectModel object : candidates)
			{
				ObjectComposition composition = object.getObjectComposition();
				String[] actions = composition == null ? null : composition.getActions();
				WorldPoint tile = object.getWorldLocation();
				if (tile == null || actions == null || Arrays.stream(actions)
					.filter(java.util.Objects::nonNull)
					.noneMatch(Rs2FairyRingScene::isFairyRingAction))
				{
					continue;
				}
				int distance = tile.distanceTo2D(anchor);
				if (distance < bestDistance)
				{
					best = new RingObject(object, tile, actions.clone());
					bestDistance = distance;
				}
			}
			return best;
		}).orElse(null);
	}

	private static DialState dialState(String code)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Widget confirm = Microbot.getClient().getWidget(InterfaceID.Fairyrings.CONFIRM);
			if (confirm == null || confirm.isHidden())
			{
				return DialState.hidden();
			}
			int[] slots = {
				InterfaceID.Fairyrings.ROOT_MODEL3,
				InterfaceID.Fairyrings.ROOT_MODEL4,
				InterfaceID.Fairyrings.ROOT_MODEL5
			};
			int[] clockwise = {
				InterfaceID.Fairyrings._1_CLOCKWISE,
				InterfaceID.Fairyrings._2_CLOCKWISE,
				InterfaceID.Fairyrings._3_CLOCKWISE
			};
			int[] anticlockwise = {
				InterfaceID.Fairyrings._1_ANTICLOCKWISE,
				InterfaceID.Fairyrings._2_ANTICLOCKWISE,
				InterfaceID.Fairyrings._3_ANTICLOCKWISE
			};
			for (int i = 0; i < slots.length; i++)
			{
				Widget slot = Microbot.getClient().getWidget(slots[i]);
				if (slot == null)
				{
					return DialState.hidden();
				}
				int current = slot.getRotationY();
				int desired = FairyRingPolicy.desiredRotation(code.charAt(i));
				if (current != desired)
				{
					int clockwiseTurns = (current - desired + 2048) % 2048;
					int anticlockwiseTurns = (desired - current + 2048) % 2048;
					int widget = clockwiseTurns <= anticlockwiseTurns
						? clockwise[i] : anticlockwise[i];
					return DialState.rotate(widget, current);
				}
			}
			return DialState.ready();
		}).orElse(DialState.hidden());
	}

	private static boolean hasStaffRequirementWaiver()
	{
		return Microbot.getVarbitValue(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) == 1;
	}

	private static boolean isWearingStaff()
	{
		return Rs2Equipment.isWearing(STAFF_IDS);
	}

	private static int inventoryStaffId()
	{
		Rs2ItemModel staff = Rs2Inventory.get(STAFF_IDS);
		return staff == null ? -1 : staff.getId();
	}

	private static int equippedWeaponId()
	{
		Rs2ItemModel weapon = Rs2Equipment.get(net.runelite.api.EquipmentInventorySlot.WEAPON);
		return weapon == null || Arrays.stream(STAFF_IDS).anyMatch(id -> id == weapon.getId())
			? -1 : weapon.getId();
	}

	private static boolean isDirectTravelAction(String action, String code)
	{
		return action != null && (action.equalsIgnoreCase("last-destination (" + code + ")")
			|| action.equalsIgnoreCase("Ring-last-destination (" + code + ")"));
	}

	private static boolean isFairyRingAction(String action)
	{
		return action != null && (action.equalsIgnoreCase(CONFIGURE)
			|| action.equalsIgnoreCase(RING_CONFIGURE)
			|| action.toLowerCase(java.util.Locale.ROOT).startsWith("last-destination (")
			|| action.toLowerCase(java.util.Locale.ROOT).startsWith("ring-last-destination ("));
	}

	private static String exactAction(String[] actions, String expected)
	{
		if (actions == null || expected == null)
		{
			return null;
		}
		return Arrays.stream(actions).filter(java.util.Objects::nonNull)
			.filter(action -> action.equalsIgnoreCase(expected)).findFirst().orElse(null);
	}

	public static boolean clickStage(String action)
	{
		if (FairyRingPolicy.isEquipAction(action))
		{
			int itemId = FairyRingPolicy.equipItemId(action);
			return itemId > 0 && Rs2Inventory.equip(itemId);
		}
		if (FairyRingPolicy.isRestoreOpenAction(action))
		{
			return forceOpenInventory();
		}
		if (FairyRingPolicy.isRestoreAction(action))
		{
			int itemId = FairyRingPolicy.restoreItemId(action);
			return itemId > 0 && Rs2Inventory.equip(itemId);
		}
		if (FairyRingPolicy.isRotateAction(action))
		{
			int widgetId = FairyRingPolicy.rotationWidgetId(action);
			return widgetId > 0 && Rs2Widget.clickWidget(widgetId);
		}
		return (FairyRingPolicy.TELEPORT_ACTION.equals(action)
			|| FairyRingPolicy.isSequenceTeleportAction(action))
			&& Rs2Widget.clickWidget(InterfaceID.Fairyrings.CONFIRM);
	}

	private static boolean forceOpenInventory()
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Microbot.getClient().runScript(TAB_SWITCH_SCRIPT,
				InterfaceTab.INVENTORY.getVarcIntIndex());
			return true;
		}).orElse(false);
	}

	private static boolean inventoryContainerReady()
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Widget inventory = Microbot.getClient().getWidget(ComponentID.INVENTORY_CONTAINER);
			return inventory != null && !inventory.isHidden()
				&& inventory.getChildren() != null;
		}).orElse(false);
	}

	private static final class RingObject
	{
		private final Rs2TileObjectModel object;
		private final WorldPoint tile;
		private final String[] actions;

		private RingObject(Rs2TileObjectModel object, WorldPoint tile, String[] actions)
		{
			this.object = object;
			this.tile = tile;
			this.actions = actions;
		}
	}

	private static final class DialState
	{
		private final boolean visible;
		private final int rotationWidgetId;
		private final int observedRotation;

		private DialState(boolean visible, int rotationWidgetId, int observedRotation)
		{
			this.visible = visible;
			this.rotationWidgetId = rotationWidgetId;
			this.observedRotation = observedRotation;
		}

		private static DialState hidden() { return new DialState(false, -1, -1); }
		private static DialState ready() { return new DialState(true, -1, -1); }
		private static DialState rotate(int widgetId, int rotation)
		{
			return new DialState(true, widgetId, rotation);
		}
	}
}
