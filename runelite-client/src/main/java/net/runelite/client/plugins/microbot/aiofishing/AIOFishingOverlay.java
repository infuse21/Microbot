package net.runelite.client.plugins.microbot.aiofishing;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingLocation;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingStage;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fancy, clickable status overlay. Draws itself rather than using OverlayPanel so it can
 * own real Start/Stop and Pause buttons: it records the button rectangles each frame and
 * {@link #onClick(Point)} hit-tests them against the overlay's rendered bounds.
 */
@Slf4j
public class AIOFishingOverlay extends Overlay {

    private static final int WIDTH = 236;
    private static final int PAD = 9;
    private static final int ROW_H = 14;
    private static final int BTN_H = 22;

    private static final Color BG = new Color(18, 18, 18, 225);
    private static final Color BORDER = new Color(60, 60, 60);
    private static final Color DIVIDER = new Color(48, 48, 48);
    private static final Color LABEL = new Color(150, 150, 150);
    private static final Color VALUE = new Color(225, 225, 225);
    private static final Color ACCENT = new Color(220, 138, 0);
    private static final Color GREEN = new Color(55, 200, 70);
    private static final Color ORANGE = new Color(230, 150, 30);
    private static final Color RED = new Color(215, 45, 45);
    private static final Color GRAY = new Color(90, 90, 90);

    private static final NumberFormat NUM = NumberFormat.getIntegerInstance();

    private final AIOFishingPlugin plugin;
    private final ItemManager itemManager;

    /** Button rects in overlay-local coordinates, refreshed every render. */
    private final Rectangle runButton = new Rectangle();
    private final Rectangle pauseButton = new Rectangle();
    private int hovered = -1; // 0 = run, 1 = pause

    @Inject
    AIOFishingOverlay(AIOFishingPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setNaughty();
    }

    /**
     * Snapshot of everything the overlay draws. Gathering (which touches the client) is
     * kept separate from painting so the drawing code is pure and can be exercised
     * without a live game client.
     */
    public static final class Stats {
        public boolean running;
        public boolean paused;
        public String task = "-";
        public String mode = "-";
        public String fish = "-";
        public String method = "-";
        public String location = "-";
        public int level = 1;
        public float levelFraction = 0f;
        public int xpToLevel = 0;
        public int xpGained = 0;
        public int xpPerHour = 0;
        public final List<CatchStat> catches = new ArrayList<>();
        public String timeToLevel = "-";
        public String inventory = "-";
        public String runtime = "00:00:00";
    }

    public static final class CatchStat {
        public BufferedImage icon;
        public int count;
    }

    @Override
    public Dimension render(Graphics2D g) {
        try {
            return paint(g, gather());
        } catch (Exception ex) {
            log.debug("AIOFishingOverlay render error", ex);
            return new Dimension(WIDTH, 40);
        }
    }

    /** Reads live state. Runs on the client thread as part of overlay rendering. */
    private Stats gather() {
        Stats s = new Stats();
        s.running = plugin.isScriptRunning();
        s.paused = plugin.isScriptPaused();

        int level = Rs2Player.getRealSkillLevel(Skill.FISHING);
        FishingStage stage = plugin.resolveDisplayStage(level);

        // When stopped, show WHY rather than a bare dash.
        String stopReason = plugin.getScript().getStopReason();
        s.task = s.running
                ? plugin.getScript().getState().name()
                : (stopReason != null ? stopReason : "-");
        s.mode = plugin.getConfig().autoProgress() ? "Auto progression" : "Manual";
        s.fish = stage.getDisplayName();
        s.method = stage.getMethod().getDisplayName();
        s.location = locationName(stage);
        s.level = level;
        s.levelFraction = levelFraction(level);
        s.xpToLevel = xpToNextLevel(level);
        s.xpGained = plugin.getXpGained();
        s.xpPerHour = plugin.getXpPerHour();
        for (Map.Entry<Integer, Integer> caught : plugin.getFishCaughtByItem().entrySet()) {
            CatchStat catchStat = new CatchStat();
            catchStat.icon = itemManager.getImage(caught.getKey());
            catchStat.count = caught.getValue();
            s.catches.add(catchStat);
        }
        if (s.catches.isEmpty()) {
            CatchStat empty = new CatchStat();
            empty.icon = itemManager.getImage(stage.getSpot().getFishSpriteId());
            s.catches.add(empty);
        }
        s.timeToLevel = timeToLevel(level);
        s.inventory = inventoryText();
        s.runtime = plugin.getFormattedRuntime();
        return s;
    }

    /** Pure drawing - no client access, so it can be rendered standalone for review. */
    public Dimension paint(Graphics2D g, Stats s) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int height = measureHeight(s);
        g.setColor(BG);
        g.fillRoundRect(0, 0, WIDTH, height, 8, 8);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(0, 0, WIDTH - 1, height - 1, 8, 8);

        int y = PAD + 10;
        y = drawTitle(g, y, s.running, s.paused);
        y = divider(g, y);

        // --- Task block ---
        y = row(g, y, "Task", s.task, VALUE);
        y = row(g, y, "Mode", s.mode, VALUE);
        y = row(g, y, "Fish", s.fish, ACCENT);
        y = row(g, y, "Method", s.method, VALUE);
        y = row(g, y, "Location", s.location, VALUE);
        y = divider(g, y);

        // --- Progress block ---
        y = drawLevelRow(g, y, s.level, s.levelFraction);
        y = row(g, y, "XP to level", NUM.format(s.xpToLevel), VALUE);
        y = row(g, y, "XP gained", NUM.format(s.xpGained), GREEN);
        y = row(g, y, "XP / hour", NUM.format(s.xpPerHour), GREEN);
        y = row(g, y, "Time to level", s.timeToLevel, VALUE);
        y = divider(g, y);

        // --- Session block ---
        y = drawCaughtRows(g, y, s.catches);
        y = row(g, y, "Inventory", s.inventory, VALUE);
        y = row(g, y, "Runtime", s.runtime, VALUE);

        // --- Buttons ---
        y += 4;
        drawButtons(g, y, s.running, s.paused);

        return new Dimension(WIDTH, height);
    }

    /** The session block grows by one row for each distinct fish caught. */
    private int measureHeight(Stats s) {
        int catchRows = Math.max(1, s.catches.size());
        int rows = 12 + catchRows;  // all label/value + level rows
        int dividers = 3;
        return PAD + 10 + 6         // title
                + rows * ROW_H
                + dividers * 7
                + 4 + BTN_H + PAD;
    }

    // ------------------------------------------------------------- draw helpers

    private int drawTitle(Graphics2D g, int y, boolean running, boolean paused) {
        g.setFont(FontManager.getRunescapeBoldFont());
        g.setColor(ACCENT);
        g.drawString("AIO FISHING", PAD, y);

        String status = !running ? "STOPPED" : (paused ? "PAUSED" : "RUNNING");
        Color statusColor = !running ? RED : (paused ? ORANGE : GREEN);

        g.setFont(FontManager.getRunescapeSmallFont());
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(status);
        int pillWidth = textWidth + 12;
        int pillX = WIDTH - PAD - pillWidth;
        int pillY = y - 9;

        g.setColor(new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), 45));
        g.fillRoundRect(pillX, pillY, pillWidth, 13, 7, 7);
        g.setColor(statusColor);
        g.drawRoundRect(pillX, pillY, pillWidth, 13, 7, 7);
        g.drawString(status, pillX + 6, pillY + 10);

        return y + 6;
    }

    private int divider(Graphics2D g, int y) {
        g.setColor(DIVIDER);
        g.drawLine(PAD, y + 3, WIDTH - PAD, y + 3);
        return y + 7;
    }

    private int row(Graphics2D g, int y, String label, String value, Color valueColor) {
        g.setFont(FontManager.getRunescapeSmallFont());
        int baseline = y + ROW_H - 3;

        g.setColor(LABEL);
        g.drawString(label, PAD, baseline);

        String shown = fit(g, value, WIDTH - (PAD * 2) - g.getFontMetrics().stringWidth(label) - 8);
        g.setColor(valueColor);
        int valueWidth = g.getFontMetrics().stringWidth(shown);
        g.drawString(shown, WIDTH - PAD - valueWidth, baseline);

        return y + ROW_H;
    }

    private int drawCaughtRows(Graphics2D g, int y, List<CatchStat> catches) {
        List<CatchStat> shown = catches.isEmpty() ? List.of(new CatchStat()) : catches;
        for (int i = 0; i < shown.size(); i++) {
            CatchStat caught = shown.get(i);
            g.setFont(FontManager.getRunescapeSmallFont());
            int baseline = y + ROW_H - 3;
            if (i == 0) {
                g.setColor(LABEL);
                g.drawString("Fish caught", PAD, baseline);
            }

            String countText = "x " + NUM.format(caught.count);
            int countWidth = g.getFontMetrics().stringWidth(countText);
            int iconSize = ROW_H;
            int countX = WIDTH - PAD - countWidth;
            if (caught.icon != null) {
                g.drawImage(caught.icon, countX - iconSize - 4, y, iconSize, iconSize, null);
            }
            g.setColor(ACCENT);
            g.drawString(countText, countX, baseline);
            y += ROW_H;
        }
        return y;
    }

    /** Level row with an inline XP-progress bar underneath the number. */
    private int drawLevelRow(Graphics2D g, int y, int level, float fraction) {
        g.setFont(FontManager.getRunescapeSmallFont());
        int baseline = y + ROW_H - 3;
        g.setColor(LABEL);
        g.drawString("Fishing level", PAD, baseline);

        String levelText = String.valueOf(level);
        g.setFont(FontManager.getRunescapeBoldFont().deriveFont(Font.BOLD, 12f));
        int lw = g.getFontMetrics().stringWidth(levelText);
        g.setColor(Color.WHITE);
        g.drawString(levelText, WIDTH - PAD - lw, baseline);

        // progress bar for the current level
        int barY = y + ROW_H - 4;
        int barW = WIDTH - (PAD * 2);
        g.setColor(new Color(35, 35, 35));
        g.fillRoundRect(PAD, barY, barW, 4, 4, 4);
        int filled = Math.round(barW * fraction);
        if (filled > 0) {
            g.setColor(GREEN);
            g.fillRoundRect(PAD, barY, Math.max(filled, 4), 4, 4, 4);
        }
        return y + ROW_H;
    }

    private void drawButtons(Graphics2D g, int y, boolean running, boolean paused) {
        int gap = 6;
        int w = (WIDTH - (PAD * 2) - gap) / 2;
        runButton.setBounds(PAD, y, w, BTN_H);
        pauseButton.setBounds(PAD + w + gap, y, w, BTN_H);

        drawButton(g, runButton, running ? "STOP" : "START", running ? RED : GREEN, true, hovered == 0);
        String pauseText = paused ? "RESUME" : "PAUSE";
        drawButton(g, pauseButton, pauseText, paused ? GREEN : ORANGE, running, hovered == 1);
    }

    private void drawButton(Graphics2D g, Rectangle r, String text, Color color, boolean enabled, boolean hover) {
        Color fill = enabled ? color : GRAY;
        int alpha = enabled ? (hover ? 90 : 55) : 30;

        g.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), alpha));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
        g.setColor(enabled ? fill : GRAY);
        g.setStroke(new BasicStroke(hover && enabled ? 2f : 1f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 6, 6);

        g.setFont(FontManager.getRunescapeBoldFont());
        FontMetrics fm = g.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(text)) / 2;
        int ty = r.y + (r.height + fm.getAscent()) / 2 - 2;
        g.setColor(enabled ? fill : GRAY);
        g.drawString(text, tx, ty);
    }

    private String fit(Graphics2D g, String text, int maxWidth) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String truncated = text;
        while (truncated.length() > 1 && fm.stringWidth(truncated + "..") > maxWidth) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        return truncated + "..";
    }

    // -------------------------------------------------------------- data helpers

    private float levelFraction(int level) {
        if (level >= 99) {
            return 1f;
        }
        int current = Microbot.getClient().getSkillExperience(Skill.FISHING);
        int start = Experience.getXpForLevel(level);
        int end = Experience.getXpForLevel(level + 1);
        if (end <= start) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, (float) (current - start) / (end - start)));
    }

    private int xpToNextLevel(int level) {
        if (level >= 99) {
            return 0;
        }
        return Math.max(0, Experience.getXpForLevel(level + 1)
                - Microbot.getClient().getSkillExperience(Skill.FISHING));
    }

    private String timeToLevel(int level) {
        if (level >= 99) {
            return "maxed";
        }
        int perHour = plugin.getXpPerHour();
        if (perHour <= 0) {
            return "-";
        }
        double hours = (double) xpToNextLevel(level) / perHour;
        long totalSeconds = (long) (hours * 3600);
        return String.format("%02d:%02d:%02d",
                totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);
    }

    private String inventoryText() {
        try {
            return Rs2Inventory.count() + " / 28";
        } catch (Exception e) {
            return "-";
        }
    }

    private String locationName(FishingStage stage) {
        try {
            // Prefer the script's cached decision - it's path-aware. Fall back to the cheap
            // straight-line pick; never pathfind here, this runs on the client thread.
            FishingLocation chosen = plugin.getScript().getDisplayLocation();
            if (chosen == null) {
                chosen = stage.getClosestLocation(Rs2Player.getWorldLocation(),
                        AIOFishingPlugin.isMembersWorld(plugin.getConfig()));
            }
            return chosen == null ? "-" : chosen.getName();
        } catch (Exception e) {
            return "-";
        }
    }

    // ------------------------------------------------------------ mouse handling

    /**
     * @return true when the point landed on a button (and the click was handled), so the
     * caller can consume the event and stop it reaching the game canvas.
     */
    public boolean onClick(Point canvasPoint) {
        Rectangle bounds = getBounds();
        if (bounds == null || !bounds.contains(canvasPoint)) {
            return false;
        }
        Point local = new Point(canvasPoint.x - bounds.x, canvasPoint.y - bounds.y);

        if (runButton.contains(local)) {
            if (plugin.isScriptRunning()) {
                plugin.stopScript();
            } else {
                plugin.startScript();
            }
            return true;
        }
        if (pauseButton.contains(local) && plugin.isScriptRunning()) {
            plugin.togglePause();
            return true;
        }
        return false;
    }

    /** Updates hover state; returns true if the pointer is over a button. */
    public boolean onMove(Point canvasPoint) {
        Rectangle bounds = getBounds();
        int previous = hovered;
        hovered = -1;
        if (bounds != null && bounds.contains(canvasPoint)) {
            Point local = new Point(canvasPoint.x - bounds.x, canvasPoint.y - bounds.y);
            if (runButton.contains(local)) {
                hovered = 0;
            } else if (pauseButton.contains(local) && plugin.isScriptRunning()) {
                hovered = 1;
            }
        }
        return hovered != -1 || previous != -1;
    }
}
