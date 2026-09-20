package net.runelite.client.plugins.microbot.api.tileitem.models;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.IEntity;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.reflection.Rs2Reflection;

import java.awt.*;
import java.util.Arrays;
import java.util.function.Supplier;

@Slf4j
public class Rs2TileItemModel implements TileItem, IEntity {

    @Getter
    private final Tile tile;
    @Getter
    private final TileItem tileItem;

    private final WorldView worldView;

    public Rs2TileItemModel(Tile tileObject, TileItem tileItem) {
        this(tileObject, tileItem, Microbot.getClientThread().invoke(() -> {
            LocalPoint local = tileObject.getLocalLocation();
            return local == null ? null : Microbot.getClient().getWorldView(local.getWorldView());
        }));
    }

    public Rs2TileItemModel(Tile tileObject, TileItem tileItem, WorldView worldView) {
        this.worldView = worldView;
        this.tile = tileObject;
        this.tileItem = tileItem;
    }
    
    @Override
    public int getId() {
        return Microbot.getClientThread().invoke(() -> tileItem.getId());
    }

    @Override
    public int getQuantity() {
        return Microbot.getClientThread().invoke(() -> tileItem.getQuantity());
    }

    @Override
    public int getVisibleTime() {
        return Microbot.getClientThread().invoke(() -> tileItem.getVisibleTime());
    }

    @Override
    public int getDespawnTime() {
        return Microbot.getClientThread().invoke(() -> tileItem.getDespawnTime());
    }

    @Override
    public int getOwnership() {
        return Microbot.getClientThread().invoke(() -> tileItem.getOwnership());
    }

    @Override
    public boolean isPrivate() {
        return Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>) () -> tileItem.isPrivate());
    }

    @Override
    public Model getModel() {
        return Microbot.getClientThread().invoke(() -> tileItem.getModel());
    }

    @Override
    public int getModelHeight() {
        return Microbot.getClientThread().invoke(() -> tileItem.getModelHeight());
    }

    @Override
    public void setModelHeight(int modelHeight) {
        Microbot.getClientThread().runOnClientThreadOptional(() -> {
            tileItem.setModelHeight(modelHeight);
            return null;
        });
    }

    @Override
    public int getAnimationHeightOffset() {
        return Microbot.getClientThread().invoke(() -> tileItem.getAnimationHeightOffset());
    }

    @Override
    public int getRenderMode() {
        return Microbot.getClientThread().invoke(() -> tileItem.getRenderMode());
    }

    @Override
    public Node getNext() {
        return Microbot.getClientThread().invoke(() -> tileItem.getNext());
    }

    @Override
    public Node getPrevious() {
        return Microbot.getClientThread().invoke(() -> tileItem.getPrevious());
    }

    @Override
    public long getHash() {
        return Microbot.getClientThread().invoke(() -> tileItem.getHash());
    }

    public String getName() {
        return Microbot.getClientThread().invoke(() -> {
            ItemComposition itemComposition = Microbot.getClient().getItemDefinition(tileItem.getId());
            return itemComposition.getName();
        });
    }

    public WorldPoint getWorldLocation() {
        return Microbot.getClientThread().invoke(tile::getWorldLocation);
    }

    public LocalPoint getLocalLocation() {
        return Microbot.getClientThread().invoke(tile::getLocalLocation);
    }

    @Override
    public WorldView getWorldView() {
        return worldView;
    }

    public boolean isNoted() {
        return Microbot.getClientThread().invoke((Supplier<Boolean>) () -> {
            ItemComposition itemComposition = Microbot.getClient().getItemDefinition(tileItem.getId());
            return itemComposition.getNote() == 799;
        });
    }


    public boolean isStackable() {
        return Microbot.getClientThread().invoke((Supplier<Boolean>) () -> {
            ItemComposition itemComposition = Microbot.getClient().getItemDefinition(tileItem.getId());
            return itemComposition.isStackable();
        });
    }

    /** Profit over the market price, excluding rune/consumable costs. Break-even is false. */
    public boolean isProfitableToHighAlch() {
        return isProfitableToHighAlch(0);
    }

    public boolean isProfitableToHighAlch(long consumableCost) {
        if (consumableCost < 0) throw new IllegalArgumentException("Negative consumable cost");
        return Microbot.getClientThread().invoke((Supplier<Boolean>) () -> {
            ItemComposition definition = Microbot.getClient().getItemDefinition(tileItem.getId());
            long proceeds = definition.getPrice() * 60L / 100;
            long margin = proceeds - Microbot.getItemManager().getItemPrice(definition.getId());
            return margin > consumableCost;
        });
    }

    public boolean willDespawnWithin(int ticks) {
        return Microbot.getClientThread().runOnClientThreadOptional(() ->
                tileItem.getDespawnTime() - Microbot.getClient().getTickCount() <= ticks).orElse(false);
    }

    public boolean isLootAble() {
        return getOwnership() != TileItem.OWNERSHIP_OTHER;
    }

    public boolean isOwned() {
        return getOwnership() == TileItem.OWNERSHIP_SELF;
    }

    public boolean isDespawned() {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            int despawnTime = tileItem.getDespawnTime();
            return despawnTime != -1 && despawnTime <= Microbot.getClient().getTickCount();
        }).orElse(true);
    }

    /** @deprecated Use getTotalGeValueLong(); saturates at Integer.MAX_VALUE. */
    @Deprecated
    public int getTotalGeValue() {
        return (int) Math.min(Integer.MAX_VALUE, getTotalGeValueLong());
    }

    /** Stack market value, without transaction fees. */
    public long getTotalGeValueLong() {
        return Microbot.getClientThread().invoke(() -> (long) Microbot.getItemManager()
                .getItemPrice(tileItem.getId()) * tileItem.getQuantity());
    }

    /** Stack definition/base value, not the market price. */
    public long getTotalBaseValueLong() {
        return Microbot.getClientThread().invoke(() -> (long) Microbot.getClient()
                .getItemDefinition(tileItem.getId()).getPrice() * tileItem.getQuantity());
    }

    /** Stack high-alchemy proceeds, excluding consumables. */
    public long getTotalHighAlchValueLong() {
        return Microbot.getClientThread().invoke(() -> (Microbot.getClient()
                .getItemDefinition(tileItem.getId()).getPrice() * 60L / 100) * tileItem.getQuantity());
    }

    public boolean isTradeable()  {
        return Microbot.getClientThread().invoke((Supplier<Boolean>) () -> {
            ItemComposition itemComposition = Microbot.getClient().getItemDefinition(tileItem.getId());
            return itemComposition.isTradeable();
        });
    }

    public boolean isMembers()  {
        return Microbot.getClientThread().invoke((Supplier<Boolean>) () -> {
            ItemComposition itemComposition = Microbot.getClient().getItemDefinition(tileItem.getId());
            return itemComposition.isMembers();
        });
    }

    /** @deprecated Use getTotalGeValueLong(); saturates at Integer.MAX_VALUE. */
    @Deprecated
    public int getTotalValue() {
        return getTotalGeValue();
    }

    public long getTotalValueLong() {
        return getTotalGeValueLong();
    }

    public boolean click() {
        return click("Take");
    }

    /**
     * Picks up this ground item (equivalent to clicking "Take").
     *
     * @return true if the interaction was dispatched successfully
     */
    public boolean pickup() {
        return click("Take");
    }

    public boolean click(String action) {
        return net.runelite.client.plugins.microbot.util.grounditem.GroundItemPickup.click(this, action);
    }
}
