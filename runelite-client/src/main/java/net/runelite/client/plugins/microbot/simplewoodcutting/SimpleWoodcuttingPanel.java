package net.runelite.client.plugins.microbot.simplewoodcutting;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.TreeLocation;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.TreeStage;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Map;

/**
 * Sidebar panel showing the woodcutting progression ladder: every stage, where you can
 * chop it, and which one you're on right now.
 *
 * <p>It is also the control surface for the plugin's mode: the Auto/Manual toggle and,
 * in manual mode, the tree selection. Both write straight back to the config group, so
 * the script (which reads config every loop) picks changes up live.</p>
 */
public class SimpleWoodcuttingPanel extends PluginPanel {

    static final String CONFIG_GROUP = "SimpleWoodcutting";

    private static final Color ACTIVE = ColorScheme.PROGRESS_COMPLETE_COLOR;
    private static final Color UNLOCKED = ColorScheme.BRAND_ORANGE;
    private static final Color LOCKED = ColorScheme.MEDIUM_GRAY_COLOR;
    private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;
    private static final Color CARD_BG = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color CARD_HOVER_BG = ColorScheme.DARKER_GRAY_HOVER_COLOR;

    private final ConfigManager configManager;

    private final JPanel stageList = new JPanel(new DynamicGridLayout(0, 1, 0, 6));
    private final JPanel modeRow = new JPanel(new DynamicGridLayout(1, 2, 4, 0));
    private final JLabel autoTab = new JLabel("Auto", SwingConstants.CENTER);
    private final JLabel manualTab = new JLabel("Manual", SwingConstants.CENTER);
    private final JLabel hintLabel = new JLabel(" ");
    private final JLabel levelValue = new JLabel("--");
    private final JLabel activeValue = new JLabel("-");
    private final JLabel nextValue = new JLabel("-");
    private final ProgressBar progress = new ProgressBar();

    private int woodcuttingLevel = 1;
    private TreeStage activeStage = TreeStage.TREE;
    private boolean membersWorld = true;
    private boolean autoProgress = true;
    /** Stage -> why it's locked. Absent means available. */
    private Map<TreeStage, String> lockReasons = Collections.emptyMap();
    /** Location name pinned for the manual tree; empty means "use nearest". */
    private String pinnedLocation = "";

    @Inject
    public SimpleWoodcuttingPanel(ConfigManager configManager) {
        this.configManager = configManager;

        setBorder(new EmptyBorder(10, 8, 10, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildHeader());
        add(buildModeToggle());
        add(buildStatusCard());
        add(sectionLabel("PROGRESSION"));

        stageList.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(stageList);

        rebuild();
    }

    /** Push fresh game/config state in from the plugin (called on the client thread). */
    public void update(int woodcuttingLevel, TreeStage activeStage, boolean membersWorld,
                       boolean autoProgress, Map<TreeStage, String> lockReasons,
                       String pinnedLocation) {
        this.woodcuttingLevel = woodcuttingLevel;
        this.activeStage = activeStage;
        this.membersWorld = membersWorld;
        this.autoProgress = autoProgress;
        this.lockReasons = lockReasons == null ? Collections.emptyMap() : lockReasons;
        this.pinnedLocation = pinnedLocation == null ? "" : pinnedLocation;
        SwingUtilities.invokeLater(this::rebuild);
    }

    // ------------------------------------------------------------ static chrome

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel title = new JLabel("SIMPLE WC");
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(UNLOCKED);

        JLabel subtitle = new JLabel("Trees · locations");
        subtitle.setFont(FontManager.getRunescapeSmallFont());
        subtitle.setForeground(MUTED);

        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.CENTER);
        return header;
    }

    /** Segmented Auto | Manual control. */
    private JPanel buildModeToggle() {
        modeRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        modeRow.setBorder(new EmptyBorder(0, 0, 6, 0));
        styleTab(autoTab, true);
        styleTab(manualTab, false);
        modeRow.add(autoTab);
        modeRow.add(manualTab);
        return modeRow;
    }

    private void styleTab(JLabel tab, boolean auto) {
        tab.setFont(FontManager.getRunescapeSmallFont());
        tab.setOpaque(true);
        tab.setBorder(new EmptyBorder(6, 0, 6, 0));
        tab.setCursor(new Cursor(Cursor.HAND_CURSOR));
        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setAutoProgress(auto);
            }
        });
    }

    private JPanel buildStatusCard() {
        JPanel card = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, ACTIVE),
                new EmptyBorder(8, 8, 8, 8)));

        JPanel levelRow = new JPanel(new BorderLayout());
        levelRow.setBackground(CARD_BG);
        JLabel levelCaption = new JLabel("WOODCUTTING LEVEL");
        levelCaption.setFont(FontManager.getRunescapeSmallFont());
        levelCaption.setForeground(MUTED);
        levelValue.setFont(FontManager.getRunescapeBoldFont().deriveFont(Font.BOLD, 20f));
        levelValue.setForeground(Color.WHITE);
        levelValue.setHorizontalAlignment(SwingConstants.RIGHT);
        levelRow.add(levelCaption, BorderLayout.WEST);
        levelRow.add(levelValue, BorderLayout.EAST);

        card.add(levelRow);
        card.add(progress);
        card.add(kvRow("Chopping", activeValue, ACTIVE));
        card.add(kvRow("Next", nextValue, MUTED));
        return card;
    }

    private JPanel kvRow(String key, JLabel valueLabel, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(CARD_BG);
        JLabel k = new JLabel(key);
        k.setFont(FontManager.getRunescapeSmallFont());
        k.setForeground(MUTED);
        valueLabel.setFont(FontManager.getRunescapeSmallFont());
        valueLabel.setForeground(valueColor);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(k, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setForeground(MUTED);
        label.setBorder(new EmptyBorder(10, 2, 2, 0));
        return label;
    }

    // ------------------------------------------------------------- config writes

    private void setAutoProgress(boolean auto) {
        if (auto == autoProgress) {
            return;
        }
        autoProgress = auto;
        configManager.setConfiguration(CONFIG_GROUP, "autoProgress", auto);
        rebuild();
    }

    private void selectStage(TreeStage stage) {
        configManager.setConfiguration(CONFIG_GROUP, "manualStage", stage);
        // Location names are per-stage, so a pin from the previous tree is meaningless.
        configManager.setConfiguration(CONFIG_GROUP, "manualLocation", "");
        pinnedLocation = "";
        activeStage = stage;
        rebuild();
    }

    /** Pin a specific location for the selected fish; empty string means "nearest". */
    private void selectLocation(String locationName) {
        configManager.setConfiguration(CONFIG_GROUP, "manualLocation", locationName);
        pinnedLocation = locationName;
        rebuild();
    }

    /** One row of the location picker: a radio-style bullet, name and note. */
    private JPanel locationPickRow(String label, String note, boolean chosen, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(CARD_BG);
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel name = new JLabel((chosen ? "(o) " : "( ) ") + label);
        name.setFont(FontManager.getRunescapeSmallFont());
        name.setForeground(chosen ? ACTIVE : ColorScheme.TEXT_COLOR);
        row.add(name, BorderLayout.WEST);

        if (note != null && !note.isEmpty()) {
            JLabel noteLabel = new JLabel(note);
            noteLabel.setFont(FontManager.getRunescapeSmallFont());
            noteLabel.setForeground(LOCKED);
            noteLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(noteLabel, BorderLayout.EAST);
        }

        MouseAdapter click = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectLocation(value);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                name.setForeground(UNLOCKED);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                name.setForeground(chosen ? ACTIVE : ColorScheme.TEXT_COLOR);
            }
        };
        row.addMouseListener(click);
        name.addMouseListener(click);
        return row;
    }

    // ------------------------------------------------------------- dynamic body

    private void rebuild() {
        // Mode tabs
        paintTab(autoTab, autoProgress);
        paintTab(manualTab, !autoProgress);
        hintLabel.setText(autoProgress ? "Best tree picked for your level" : "Pick a tree below");

        levelValue.setText(String.valueOf(woodcuttingLevel));
        activeValue.setText(activeStage.getDisplayName());

        TreeStage next = autoProgress ? activeStage.next(membersWorld) : null;
        if (!autoProgress) {
            nextValue.setText("manual");
            progress.setFraction(1f);
        } else if (next == null) {
            nextValue.setText("max stage");
            progress.setFraction(1f);
        } else {
            nextValue.setText(next.getDisplayName() + " @ " + next.getMinLevel());
            int span = next.getMinLevel() - activeStage.getMinLevel();
            int done = woodcuttingLevel - activeStage.getMinLevel();
            progress.setFraction(span <= 0 ? 1f : Math.max(0f, Math.min(1f, (float) done / span)));
        }

        stageList.removeAll();
        JLabel hint = new JLabel(autoProgress
                ? "Auto ladder - switches as you level"
                : "Click a tree to select it");
        hint.setFont(FontManager.getRunescapeSmallFont());
        hint.setForeground(autoProgress ? MUTED : UNLOCKED);
        hint.setBorder(new EmptyBorder(0, 2, 4, 0));
        stageList.add(hint);

        // Auto mode shows only the lean ladder; manual mode shows the full catalogue.
        for (TreeStage stage : TreeStage.values()) {
            if (autoProgress && !stage.isInAutoLadder()) {
                continue;
            }
            stageList.add(buildStageCard(stage));
        }
        stageList.revalidate();
        stageList.repaint();
        revalidate();
        repaint();
    }

    private void paintTab(JLabel tab, boolean selected) {
        tab.setBackground(selected ? UNLOCKED : CARD_BG);
        tab.setForeground(selected ? Color.BLACK : MUTED);
    }

    private JPanel buildStageCard(TreeStage stage) {
        boolean isActive = stage == activeStage;
        String lock = lockReasons.get(stage);
        boolean unlocked = lock == null;
        // Only stages whose level/members/quest requirements are all met can be picked.
        boolean selectable = !autoProgress && unlocked && !isActive;

        Color accent = isActive ? ACTIVE : (unlocked ? UNLOCKED : LOCKED);
        Color nameColor = isActive ? ACTIVE : (unlocked ? Color.WHITE : LOCKED);

        JPanel card = new JPanel(new DynamicGridLayout(0, 1, 0, 3));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
                new EmptyBorder(7, 8, 7, 8)));

        // Title row: name + level badge
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(CARD_BG);
        JLabel name = new JLabel((isActive ? "▶ " : "") + stage.getDisplayName());
        name.setFont(FontManager.getRunescapeBoldFont());
        name.setForeground(nameColor);
        JLabel badge = new JLabel(badgeText(stage, isActive, lock));
        badge.setFont(FontManager.getRunescapeSmallFont());
        badge.setForeground(isActive ? ACTIVE : (unlocked ? MUTED : LOCKED));
        badge.setHorizontalAlignment(SwingConstants.RIGHT);
        titleRow.add(name, BorderLayout.WEST);
        titleRow.add(badge, BorderLayout.EAST);
        card.add(titleRow);

        // Method row
        StringBuilder meta = new StringBuilder(stage.getAction());
        if (stage.isMembersOnly()) {
            meta.append("  ·  members");
        }
        JLabel method = new JLabel(meta.toString());
        method.setFont(FontManager.getRunescapeSmallFont());
        method.setForeground(unlocked ? MUTED : LOCKED);
        card.add(method);

        // Explain quest/skill gates rather than just greying the card out.
        if (lock != null && stage.getRequirement().hasQuest()) {
            // ASCII only - the RuneScape pixel font has no glyph for symbols like key/lock.
            JLabel questRow = new JLabel("! " + lock);
            questRow.setFont(FontManager.getRunescapeSmallFont());
            questRow.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
            card.add(questRow);
        }

        // On the selected manual tree the locations become a picker: choose exactly where
        // to chop instead of letting nearest-wins decide.
        boolean pickable = !autoProgress && isActive && unlocked;
        if (pickable) {
            card.add(locationPickRow("Nearest", "auto", "".equals(pinnedLocation), ""));
        }

        // Locations
        for (TreeLocation location : stage.getLocations()) {
            if (pickable) {
                card.add(locationPickRow(location.getName(),
                        location.hasNote() ? location.getNote() : "",
                        location.getName().equalsIgnoreCase(pinnedLocation),
                        location.getName()));
                continue;
            }
            JPanel row = new JPanel(new BorderLayout());
            row.setBackground(CARD_BG);
            JLabel locName = new JLabel("• " + location.getName());
            locName.setFont(FontManager.getRunescapeSmallFont());
            locName.setForeground(unlocked ? ColorScheme.TEXT_COLOR : LOCKED);
            row.add(locName, BorderLayout.WEST);
            if (location.hasNote()) {
                JLabel note = new JLabel(location.getNote());
                note.setFont(FontManager.getRunescapeSmallFont());
                note.setForeground(LOCKED);
                note.setHorizontalAlignment(SwingConstants.RIGHT);
                row.add(note, BorderLayout.EAST);
            }
            card.add(row);
        }

        if (selectable) {
            attachSelection(card, stage);
        }
        return card;
    }

    private String badgeText(TreeStage stage, boolean isActive, String lock) {
        if (isActive) {
            return autoProgress ? "ACTIVE" : "SELECTED";
        }
        if (lock != null) {
            // Quest gates get their own row below, so keep the badge to the level.
            return stage.getRequirement().hasQuest() ? "Lv " + stage.getMinLevel() : lock;
        }
        return autoProgress ? "Lv " + stage.getMinLevel() : "select";
    }

    /** Makes a whole card behave as one clickable target, including its children. */
    private void attachSelection(JPanel card, TreeStage stage) {
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectStage(stage);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setCardBackground(card, CARD_HOVER_BG);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCardBackground(card, CARD_BG);
            }
        };
        addRecursively(card, adapter);
    }

    private void addRecursively(java.awt.Container container, MouseAdapter adapter) {
        container.addMouseListener(adapter);
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof java.awt.Container) {
                addRecursively((java.awt.Container) child, adapter);
            } else {
                child.addMouseListener(adapter);
            }
        }
    }

    private void setCardBackground(JPanel card, Color color) {
        card.setBackground(color);
        for (java.awt.Component child : card.getComponents()) {
            child.setBackground(color);
            if (child instanceof java.awt.Container) {
                for (java.awt.Component grandChild : ((java.awt.Container) child).getComponents()) {
                    grandChild.setBackground(color);
                }
            }
        }
        card.repaint();
    }

    /** Slim rounded progress bar showing how far through the current stage you are. */
    private static final class ProgressBar extends JPanel {
        private float fraction = 0f;

        private ProgressBar() {
            setBackground(CARD_BG);
            setPreferredSize(new Dimension(0, 6));
        }

        private void setFraction(float fraction) {
            this.fraction = fraction;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(ColorScheme.SCROLL_TRACK_COLOR);
                g2.fillRoundRect(0, 0, w, h, h, h);
                int filled = Math.round(w * fraction);
                if (filled > 0) {
                    g2.setColor(ACTIVE);
                    g2.fillRoundRect(0, 0, Math.max(filled, h), h, h, h);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
