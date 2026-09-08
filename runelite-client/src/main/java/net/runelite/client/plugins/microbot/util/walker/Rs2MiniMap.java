package net.runelite.client.plugins.microbot.util.walker;

import java.awt.geom.AffineTransform;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.api.Varbits;
import net.runelite.client.plugins.microbot.util.coords.Rs2LocalPoint;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Rs2MiniMap {
	private static final class MinimapSnapshot
	{
		private final Rectangle bounds;
		private final boolean resized;

		private MinimapSnapshot(Rectangle bounds, boolean resized)
		{
			this.bounds = bounds;
			this.resized = resized;
		}
	}

    /**
     * Converts a {@link LocalPoint} to a minimap coordinate {@link Point}.
     *
     * @param localPoint The local point to convert.
     * @return The corresponding minimap point, or {@code null} if conversion fails.
     */
    @Nullable
    public static Point localToMinimap(LocalPoint localPoint) {
        if (localPoint == null) return null;
        return Microbot.getClientThread().runOnClientThreadOptional(() -> Perspective.localToMinimap(Microbot.getClient(), localPoint))
                .orElse(null);
    }

    /**
     * Converts a {@link WorldPoint} to a minimap coordinate {@link Point}.
     *
     * @param worldPoint The world point to convert.
     * @return The corresponding minimap point, or {@code null} if conversion fails.
     */
    @Nullable
    public static Point worldToMinimap(WorldPoint worldPoint) {
        if (worldPoint == null) return null;

        LocalPoint localPoint = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), worldPoint);

        if (Microbot.getClient().getTopLevelWorldView().isInstance() && localPoint == null) {
            localPoint = Rs2LocalPoint.fromWorldInstance(worldPoint);
        }

        if (localPoint == null) {
            return null;
        }

        final LocalPoint lp = localPoint;
        return Microbot.getClientThread().runOnClientThreadOptional(() -> Perspective.localToMinimap(Microbot.getClient(), lp))
                .orElse(null);
    }

    /**
     * Retrieves the minimap draw widget based on the current game view mode.
     *
     * @return The minimap draw widget, or {@code null} if not found.
     */
    public static Widget getMinimapDrawWidget() {
        if (Microbot.getClient().isResized()) {
            if (Microbot.getVarbitValue(Varbits.SIDE_PANELS) == 1) {
                return Rs2Widget.getWidget(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_DRAW_AREA);
            }
            return Rs2Widget.getWidget(ComponentID.RESIZABLE_VIEWPORT_MINIMAP_DRAW_AREA);
        }
        return Rs2Widget.getWidget(ComponentID.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
    }

    /**
     * Returns a simple elliptical clip area for the minimap.
     *
     * @return A {@link Shape} representing the minimap clip area.
     */
    static Shape createMinimapClipArea(Rectangle bounds, double scale) {
        if (bounds == null || bounds.isEmpty()) {
            return null;
        }
        Shape clipArea = new Ellipse2D.Double(
            bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        return scale == 1.0 ? clipArea : shrinkShape(clipArea, scale);
    }

	/**
	 * Retrieves a conservative minimap clipping area from one widget-bounds snapshot.
	 *
	 * @param scale The scale factor to shrink the polygon (e.g., 0.94 for 94% of the original size).
	 * @return A {@link Shape} representing the scaled minimap clickable area.
	 */
	public static Shape getMinimapClipArea(double scale) {
		MinimapSnapshot snapshot = getMinimapSnapshot();
		if (snapshot == null) {
			return null;
		}
		return createMinimapClipArea(snapshot.bounds, scale);
	}

	/**
	 * Retrieves the minimap clipping area as a {@link Shape}, scaled to slightly reduce its size.
	 * <p>
	 * This is useful for rendering overlays within the minimap without overlapping UI elements such as the globe icon.
	 *
	 * @return A {@link Shape} representing the scaled minimap clip area, or {@code null} if the minimap widget is unavailable.
	 */
	public static Shape getMinimapClipArea() {
		MinimapSnapshot snapshot = getMinimapSnapshot();
		if (snapshot == null) {
			return null;
		}
		return createMinimapClipArea(snapshot.bounds, snapshot.resized ? 0.94 : 1.0);
	}

	private static MinimapSnapshot getMinimapSnapshot()
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Widget minimapWidget = getMinimapDrawWidget();
			if (minimapWidget == null || minimapWidget.isHidden())
			{
				return null;
			}
			return new MinimapSnapshot(new Rectangle(minimapWidget.getBounds()),
				Microbot.getClient().isResized());
		}).orElse(null);
	}

	/**
	 * Shrinks the given shape toward its center by the specified scale factor.
	 *
	 * @param shape The original shape to shrink.
	 * @param scale The scale factor (e.g., 0.94 = 94% size). Must be > 0 and < 1 to reduce the shape.
	 * @return A new {@link Shape} that is scaled inward toward its center.
	 */
	private static Shape shrinkShape(Shape shape, double scale) {
		Rectangle bounds = shape.getBounds();
		double centerX = bounds.getCenterX();
		double centerY = bounds.getCenterY();

		AffineTransform shrink = AffineTransform.getTranslateInstance(centerX, centerY);
		shrink.scale(scale, scale);
		shrink.translate(-centerX, -centerY);

		return shrink.createTransformedShape(shape);
	}

    /**
     * Checks if a given point is inside the minimap clipping area.
     *
     * @param point The point to check.
     * @return {@code true} if the point is within the minimap bounds, {@code false} otherwise.
     */
    public static boolean isPointInsideMinimap(Point point) {
        Shape minimapClipArea = getMinimapClipArea();
        return minimapClipArea != null && minimapClipArea.contains(point.getX(), point.getY());
    }
}
