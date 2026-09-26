package net.runelite.client.plugins.microbot.api.tileobject.models;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.IEntity;
import net.runelite.client.plugins.microbot.api.boat.Rs2BoatCache;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.AbstractMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

@Slf4j
public class Rs2TileObjectModel implements TileObject, IEntity {

    public Rs2TileObjectModel(GameObject gameObject) {
        this.tileObject = gameObject;
        this.tileObjectType = TileObjectType.GAME;
    }

    public Rs2TileObjectModel(DecorativeObject tileObject) {
        this.tileObject = tileObject;
        this.tileObjectType = TileObjectType.DECORATIVE;
    }

    public Rs2TileObjectModel(WallObject tileObject) {
        this.tileObject = tileObject;
        this.tileObjectType = TileObjectType.WALL;
    }

    public Rs2TileObjectModel(GroundObject tileObject) {
        this.tileObject = tileObject;
        this.tileObjectType = TileObjectType.GROUND;
    }

    public Rs2TileObjectModel(TileObject tileObject) {
        this.tileObject = tileObject;
        this.tileObjectType = TileObjectType.GENERIC;
    }

    @Getter
    private final TileObjectType tileObjectType;
    private final TileObject tileObject;
    private String[] actions;

    public TileObject getRawTileObject() {
        return tileObject;
    }


    @Override
    public long getHash() {
        return Microbot.getClientThread().invoke(tileObject::getHash);
    }

    @Override
    public int getX() {
        return Microbot.getClientThread().invoke(tileObject::getX);
    }

    @Override
    public int getY() {
        return Microbot.getClientThread().invoke(tileObject::getY);
    }

    @Override
    public int getZ() {
        return Microbot.getClientThread().invoke(tileObject::getZ);
    }

    @Override
    public int getPlane() {
        return Microbot.getClientThread().invoke(tileObject::getPlane);
    }

    @Override
    public WorldView getWorldView() {
        return Microbot.getClientThread().invoke(tileObject::getWorldView);
    }

    public int getId() {
        return Microbot.getClientThread().invoke(tileObject::getId);
    }

    public int getSizeX() {
        return tileObject instanceof GameObject
                ? Microbot.getClientThread().runOnClientThreadOptional(
                        () -> ((GameObject) tileObject).sizeX()).orElse(1)
                : 1;
    }

    public int getSizeY() {
        return tileObject instanceof GameObject
                ? Microbot.getClientThread().runOnClientThreadOptional(
                        () -> ((GameObject) tileObject).sizeY()).orElse(1)
                : 1;
    }

    @Override
    public @NotNull WorldPoint getWorldLocation() {
        return Microbot.getClientThread().invoke(() -> {
            WorldPoint worldLocation = tileObject.getWorldLocation();
            if (!(tileObject instanceof GameObject)) return worldLocation;

            GameObject gameObject = (GameObject) tileObject;
            WorldView view = tileObject.getWorldView();
            Point sceneMin = gameObject.getSceneMinLocation();
            if (view == null || sceneMin == null) return worldLocation;
            return WorldPoint.fromScene(view, sceneMin.getX(), sceneMin.getY(), view.getPlane());
        });
    }

    public String getName() {
        return Microbot.getClientThread().invoke(() -> {
            ObjectComposition composition = Microbot.getClient().getObjectDefinition(tileObject.getId());
            if (composition == null) return null;
            if (composition.getImpostorIds() != null) {
                composition = composition.getImpostor();
            }
            if (composition == null)
                return null;
            return Rs2UiHelper.stripColTags(composition.getName());
        });
    }

    @Override
    public @NotNull LocalPoint getLocalLocation() {
        return Microbot.getClientThread().invoke(tileObject::getLocalLocation);
    }

    @Override
    public @Nullable Point getCanvasLocation() {
        return Microbot.getClientThread().invoke((Supplier<Point>) tileObject::getCanvasLocation);
    }

    @Override
    public @Nullable Point getCanvasLocation(int zOffset) {
        return Microbot.getClientThread().invoke((Supplier<Point>) tileObject::getCanvasLocation);
    }

    @Override
    public @Nullable Polygon getCanvasTilePoly() {
        return Microbot.getClientThread().invoke(tileObject::getCanvasTilePoly);
    }

    @Override
    public @Nullable Point getCanvasTextLocation(Graphics2D graphics, String text, int zOffset) {
        return Microbot.getClientThread().invoke(() -> tileObject.getCanvasTextLocation(graphics, text, zOffset));
    }

    @Override
    public @Nullable Point getMinimapLocation() {
        return Microbot.getClientThread().invoke(tileObject::getMinimapLocation);
    }

    @Override
    public @Nullable Shape getClickbox() {
        return Microbot.getClientThread().invoke(tileObject::getClickbox);
    }

    @Override
    public @Nullable String getOpOverride(int index) {
        return Microbot.getClientThread().invoke(() -> tileObject.getOpOverride(index));
    }

    @Override
    public boolean isOpShown(int index) {
        return Microbot.getClientThread().invoke((Supplier<Boolean>) () -> tileObject.isOpShown(index));
    }

    public ObjectComposition getObjectComposition() {
        return Microbot.getClientThread().invoke(() -> {
            ObjectComposition composition = Microbot.getClient().getObjectDefinition(tileObject.getId());
            if (composition == null) return null;
            if (composition.getImpostorIds() != null) {
                composition = composition.getImpostor();
            }
            return composition;
        });
    }

    @Override
    public boolean isReachable() {
        WorldView objectWorldView = getWorldView();
        if (objectWorldView == null) {
            return false;
        }

        WorldView playerWorldView = Microbot.getClientThread().runOnClientThreadOptional(() -> {
            Player player = Microbot.getClient().getLocalPlayer();
            return player != null ? player.getWorldView() : null;
        }).orElse(null);

        if (playerWorldView == null) {
            return false;
        }

        if (objectWorldView.getId() == playerWorldView.getId()) {
            return true;
        }

        return IEntity.super.isReachable();
    }

    public boolean click() {
        return click("");
    }

    /**
     * @param action the action to perform (e.g., "Open", "Climb")
     * @return true if the interaction was submitted, false otherwise
     */
    public boolean click(String action) {
        try {
            String requestedAction = action == null ? "" : action;
            String name = getName();
            if (name == null) return false;
            if (name.toLowerCase(Locale.ROOT).contains("train cart")) {
                if (Microbot.getClient().isClientThread()) return false;
                Rs2Equipment.unEquip(EquipmentInventorySlot.WEAPON);
                Rs2Equipment.unEquip(EquipmentInventorySlot.SHIELD);
                if (!sleepUntil(() -> Rs2Equipment.get(EquipmentInventorySlot.WEAPON) == null
                        && Rs2Equipment.get(EquipmentInventorySlot.SHIELD) == null, 5000)) return false;
            }

            LocalPoint location = getLocalLocation();
            if (location == null) return false;
            if (!Rs2Camera.isTileOnScreen(location)) {
                if (Microbot.getClient().isClientThread()) return false;
                Rs2Camera.turnTo(tileObject);
            }

            Map.Entry<NewMenuEntry, Rectangle> dispatch = Microbot.getClientThread()
                    .runOnClientThreadOptional(() -> resolveClick(requestedAction)).orElse(null);
            if (dispatch == null || Thread.currentThread().isInterrupted()) return false;
            Microbot.status = requestedAction + " " + name;
            return Microbot.tryDoInvoke(dispatch.getKey(), dispatch.getValue());
        } catch (Exception ex) {
            log.error("Failed to interact with object: ", ex);
            return false;
        }
    }

    private Map.Entry<NewMenuEntry, Rectangle> resolveClick(String action) {
        Client client = Microbot.getClient();
        WorldView view = tileObject.getWorldView();
        if (client.getGameState() != GameState.LOGGED_IN || view == null
                || client.getWorldView(view.getId()) != view || !isCurrentInScene(view)) return null;

        ObjectComposition composition = client.getObjectDefinition(tileObject.getId());
        if (composition == null) return null;
        if (composition.getImpostorIds() != null) composition = composition.getImpostor();
        if (composition == null) return null;

        boolean widgetSelected = client.isWidgetSelected();
        String[] actions = composition.getActions();
        int index = 0;
        if (!widgetSelected && !action.isBlank()) {
            index = -1;
            if (actions != null) {
                for (int i = 0; i < actions.length; i++) {
                    if (actions[i] != null && action.equalsIgnoreCase(Rs2UiHelper.stripColTags(actions[i]))) {
                        index = i;
                        break;
                    }
                }
            }
            if (index < 0) return null;
        }
        if (index > 4) return null;

        MenuAction menuAction;
        if (widgetSelected) {
            menuAction = MenuAction.WIDGET_TARGET_ON_GAME_OBJECT;
        } else {
            switch (index) {
                case 0: menuAction = MenuAction.GAME_OBJECT_FIRST_OPTION; break;
                case 1: menuAction = MenuAction.GAME_OBJECT_SECOND_OPTION; break;
                case 2: menuAction = MenuAction.GAME_OBJECT_THIRD_OPTION; break;
                case 3: menuAction = MenuAction.GAME_OBJECT_FOURTH_OPTION; break;
                case 4: menuAction = MenuAction.GAME_OBJECT_FIFTH_OPTION; break;
                default: return null;
            }
        }

        LocalPoint location = tileObject.getLocalLocation();
        if (location == null) return null;
        int param0 = location.getSceneX();
        int param1 = location.getSceneY();
        if (tileObject instanceof GameObject) {
            GameObject gameObject = (GameObject) tileObject;
            if (gameObject.sizeX() > 1) param0 -= gameObject.sizeX() / 2;
            if (gameObject.sizeY() > 1) param1 -= gameObject.sizeY() / 2;
        }

        NewMenuEntry entry = new NewMenuEntry()
                .param0(param0).param1(param1).opcode(menuAction.getId())
                .identifier(tileObject.getId()).itemId(-1).option(action)
                .target(composition.getName()).setWorldViewId(view.getId())
                .gameObject(tileObject);
        return new AbstractMap.SimpleImmutableEntry<>(entry, Rs2UiHelper.getObjectClickbox(tileObject));
    }

    private boolean isCurrentInScene(WorldView view) {
        Scene scene = view.getScene();
        if (scene == null) return false;

        Point point;
        if (tileObject instanceof GameObject) {
            point = ((GameObject) tileObject).getSceneMinLocation();
        } else {
            LocalPoint location = tileObject.getLocalLocation();
            point = location == null ? null : new Point(location.getSceneX(), location.getSceneY());
        }
        if (point == null) return false;

        Tile[][][] tiles = scene.getTiles();
        int plane = view.getPlane();
        int x = point.getX();
        int y = point.getY();
        if (tiles == null || plane < 0 || plane >= tiles.length || tiles[plane] == null
                || x < 0 || x >= tiles[plane].length || tiles[plane][x] == null
                || y < 0 || y >= tiles[plane][x].length) return false;
        Tile tile = tiles[plane][x][y];
        if (tile == null) return false;

        if (tileObject instanceof GameObject) {
            GameObject[] objects = tile.getGameObjects();
            if (objects != null) {
                for (GameObject object : objects) {
                    if (object == tileObject) return true;
                }
            }
            return false;
        }
        if (tileObject instanceof GroundObject) return tile.getGroundObject() == tileObject;
        if (tileObject instanceof WallObject) return tile.getWallObject() == tileObject;
        if (tileObject instanceof DecorativeObject) return tile.getDecorativeObject() == tileObject;
        return true;
    }

}
