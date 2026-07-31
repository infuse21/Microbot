package net.runelite.client.plugins.microbot.aiofishing;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.microbot.aiofishing.enums.AerialCatch;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingActivity;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingLocation;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingMethod;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingStage;
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
 * Sidebar panel showing the fishing progression ladder: every stage, where you can
 * fish it, and which one you're on right now.
 *
 * <p>It is also the control surface for the plugin's mode: the Auto/Manual toggle and,
 * in manual mode, the fish selection. Both write straight back to the config group, so
 * the script (which reads config every loop) picks changes up live.</p>
 */
public class AIOFishingPanel extends PluginPanel {

    static final String CONFIG_GROUP = "AIOFishing";

    private static final Color ACTIVE = ColorScheme.PROGRESS_COMPLETE_COLOR;
    private static final Color UNLOCKED = ColorScheme.BRAND_ORANGE;
    private static final Color LOCKED = ColorScheme.MEDIUM_GRAY_COLOR;
    private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;
    private static final Color CARD_BG = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color CARD_HOVER_BG = ColorScheme.DARKER_GRAY_HOVER_COLOR;

    private final ConfigManager configManager;

    private final JPanel stageList = new JPanel(new DynamicGridLayout(0, 1, 0, 6));
    /** Method-icon key, shown in manual mode between the status card and the stage list. */
    private final JPanel legendRow = new JPanel(new java.awt.GridLayout(0, 6, 2, 2));
    /** Top-level activity tabs: Progression | Aerial. */
    private final JPanel activityRow = new JPanel(new DynamicGridLayout(1, 2, 4, 0));
    private final JLabel progressionTab = new JLabel("Progression", SwingConstants.CENTER);
    private final JLabel aerialTab = new JLabel("Aerial", SwingConstants.CENTER);
    /** Everything specific to the aerial activity; swapped in place of the ladder. */
    private final JPanel aerialPage = new JPanel(new DynamicGridLayout(0, 1, 0, 6));
    private final JPanel modeRow = new JPanel(new DynamicGridLayout(1, 2, 4, 0));
    private final JLabel autoTab = new JLabel("Auto", SwingConstants.CENTER);
    private final JLabel manualTab = new JLabel("Manual", SwingConstants.CENTER);
    private final JLabel hintLabel = new JLabel(" ");
    private final JLabel levelValue = new JLabel("--");
    private final JLabel activeValue = new JLabel("-");
    private final JLabel nextValue = new JLabel("-");
    private final ProgressBar progress = new ProgressBar();
    /**
     * Exactly one page is mounted at a time.
     *
     * <p>DynamicGridLayout sizes rows from getComponentCount() and never checks isVisible(),
     * so merely hiding the other page's widgets would still reserve their rows and push the
     * mounted page off-screen. Swapping the child is the only thing that actually works.</p>
     */
    private final JPanel pageHolder = new JPanel(new DynamicGridLayout(0, 1, 0, 3));
    private final JPanel progressionBody = new JPanel(new DynamicGridLayout(0, 1, 0, 3));
    private JPanel statusCard;
    private JLabel progressionLabel;

    private int fishingLevel = 1;
    private FishingStage activeStage = FishingStage.SHRIMP;
    private boolean membersWorld = true;
    private boolean autoProgress = true;
    /** Stage -> why it's locked. Absent means available. */
    private Map<FishingStage, String> lockReasons = Collections.emptyMap();
    /** Location name pinned for the manual fish; empty means "use nearest". */
    private String pinnedLocation = "";
    /**
     * Method the legend is filtering the list by, or null for "show everything".
     *
     * <p>Deliberately not persisted to config: it is a way of looking at the catalogue, not a
     * setting, and a filter silently surviving a restart would look like missing fish.</p>
     */
    private FishingMethod methodFilter;
    private FishingActivity activity = FishingActivity.PROGRESSION;
    /** Aerial readiness reason from the plugin; null when good to go. */
    private String aerialUnmet;
    private int hunterLevel = 1;

    @Inject
    public AIOFishingPanel(ConfigManager configManager) {
        this.configManager = configManager;

        setBorder(new EmptyBorder(10, 8, 10, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildHeader());
        add(buildActivityTabs());

        progressionBody.setBackground(ColorScheme.DARK_GRAY_COLOR);
        progressionBody.add(buildModeToggle());
        statusCard = buildStatusCard();
        progressionBody.add(statusCard);
        legendRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        legendRow.setBorder(new EmptyBorder(6, 2, 0, 2));
        progressionBody.add(legendRow);
        progressionLabel = sectionLabel("PROGRESSION");
        progressionBody.add(progressionLabel);
        stageList.setBackground(ColorScheme.DARK_GRAY_COLOR);
        progressionBody.add(stageList);

        aerialPage.setBackground(ColorScheme.DARK_GRAY_COLOR);

        pageHolder.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(pageHolder);

        rebuild();
    }

    /** Push fresh game/config state in from the plugin (called on the client thread). */
    public void update(int fishingLevel, FishingStage activeStage, boolean membersWorld,
                       boolean autoProgress, Map<FishingStage, String> lockReasons,
                       String pinnedLocation) {
        update(fishingLevel, activeStage, membersWorld, autoProgress, lockReasons,
                pinnedLocation, FishingActivity.PROGRESSION, 1, null);
    }

    public void update(int fishingLevel, FishingStage activeStage, boolean membersWorld,
                       boolean autoProgress, Map<FishingStage, String> lockReasons,
                       String pinnedLocation, FishingActivity activity, int hunterLevel,
                       String aerialUnmet) {
        this.activity = activity == null ? FishingActivity.PROGRESSION : activity;
        this.hunterLevel = hunterLevel;
        this.aerialUnmet = aerialUnmet;
        this.fishingLevel = fishingLevel;
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

        JLabel title = new JLabel("AIO FISHING");
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(UNLOCKED);

        JLabel subtitle = new JLabel("Progression · locations");
        subtitle.setFont(FontManager.getRunescapeSmallFont());
        subtitle.setForeground(MUTED);

        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.CENTER);
        return header;
    }

    /** Segmented Progression | Aerial control - the plugin's top-level activity. */
    private JPanel buildActivityTabs() {
        activityRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        activityRow.setBorder(new EmptyBorder(0, 0, 6, 0));
        styleActivityTab(progressionTab, FishingActivity.PROGRESSION);
        styleActivityTab(aerialTab, FishingActivity.AERIAL);
        activityRow.add(progressionTab);
        activityRow.add(aerialTab);
        return activityRow;
    }

    private void styleActivityTab(JLabel tab, FishingActivity target) {
        tab.setFont(FontManager.getRunescapeBoldFont());
        tab.setOpaque(true);
        tab.setBorder(new EmptyBorder(6, 0, 6, 0));
        tab.setCursor(new Cursor(Cursor.HAND_CURSOR));
        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setActivity(target);
            }
        });
    }

    private void setActivity(FishingActivity target) {
        if (target == activity) {
            return;
        }
        activity = target;
        configManager.setConfiguration(CONFIG_GROUP, "activity", target);
        rebuild();
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
        JLabel levelCaption = new JLabel("FISHING LEVEL");
        levelCaption.setFont(FontManager.getRunescapeSmallFont());
        levelCaption.setForeground(MUTED);
        levelValue.setFont(FontManager.getRunescapeBoldFont().deriveFont(Font.BOLD, 20f));
        levelValue.setForeground(Color.WHITE);
        levelValue.setHorizontalAlignment(SwingConstants.RIGHT);
        levelRow.add(levelCaption, BorderLayout.WEST);
        levelRow.add(levelValue, BorderLayout.EAST);

        card.add(levelRow);
        card.add(progress);
        card.add(kvRow("Fishing", activeValue, ACTIVE));
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

    private void selectStage(FishingStage stage) {
        configManager.setConfiguration(CONFIG_GROUP, "manualStage", stage);
        // Location names are per-stage, so a pin from the previous fish is meaningless.
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
        boolean aerial = activity == FishingActivity.AERIAL;
        paintTab(progressionTab, !aerial);
        paintTab(aerialTab, aerial);

        // Mount the page for this activity. The status card is all stage/next talk, so it
        // travels with the progression body rather than being shared.
        JPanel wanted = aerial ? aerialPage : progressionBody;
        if (pageHolder.getComponentCount() != 1 || pageHolder.getComponent(0) != wanted) {
            pageHolder.removeAll();
            pageHolder.add(wanted);
        }
        if (aerial) {
            buildAerialPage();
            pageHolder.revalidate();
            revalidate();
            repaint();
            return;
        }

        // Mode tabs
        paintTab(autoTab, autoProgress);
        paintTab(manualTab, !autoProgress);
        hintLabel.setText(autoProgress ? "Best fish picked for your level" : "Pick a fish below");

        levelValue.setText(String.valueOf(fishingLevel));
        activeValue.setText(activeStage.getDisplayName());

        FishingStage next = autoProgress ? activeStage.next(membersWorld) : null;
        if (!autoProgress) {
            nextValue.setText("manual");
            progress.setFraction(1f);
        } else if (next == null) {
            nextValue.setText("max stage");
            progress.setFraction(1f);
        } else {
            nextValue.setText(next.getDisplayName() + " @ " + next.getMinLevel());
            int span = next.getMinLevel() - activeStage.getMinLevel();
            int done = fishingLevel - activeStage.getMinLevel();
            progress.setFraction(span <= 0 ? 1f : Math.max(0f, Math.min(1f, (float) done / span)));
        }

        buildLegend();

        stageList.removeAll();
        JLabel hint = new JLabel(autoProgress
                ? "Auto ladder - switches as you level"
                : methodFilter != null
                        ? "Filtered: " + methodFilter.getDisplayName() + " (click icon to clear)"
                        : "Click a fish to select it");
        hint.setFont(FontManager.getRunescapeSmallFont());
        hint.setForeground(autoProgress ? MUTED : UNLOCKED);
        hint.setBorder(new EmptyBorder(0, 2, 4, 0));
        stageList.add(hint);

        // Auto mode shows only the lean ladder; manual mode shows the full catalogue.
        int shown = 0;
        for (FishingStage stage : FishingStage.values()) {
            if (autoProgress && !stage.isAutoStage(membersWorld)) {
                continue;
            }
            // The filter is a manual-mode tool; auto mode shows its own short ladder.
            if (!autoProgress && methodFilter != null && stage.getMethod() != methodFilter) {
                continue;
            }
            stageList.add(buildStageCard(stage));
            shown++;
        }
        if (shown == 0) {
            JLabel empty = new JLabel("No fish use that method");
            empty.setFont(FontManager.getRunescapeSmallFont());
            empty.setForeground(LOCKED);
            empty.setBorder(new EmptyBorder(4, 2, 0, 0));
            stageList.add(empty);
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

    private JPanel buildStageCard(FishingStage stage) {
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
        String badgeString = badgeText(stage, isActive, lock);
        JLabel badge = new JLabel(badgeString);
        badge.setFont(FontManager.getRunescapeSmallFont());
        JLabel name = fittedLabel((isActive ? "▶ " : "") + stage.getDisplayName(),
                badge.getFontMetrics(badge.getFont()).stringWidth(badgeString) + 8);
        name.setForeground(nameColor);
        badge.setForeground(isActive ? ACTIVE : (unlocked ? MUTED : LOCKED));
        badge.setHorizontalAlignment(SwingConstants.RIGHT);
        titleRow.add(name, BorderLayout.WEST);
        titleRow.add(badge, BorderLayout.EAST);
        card.add(titleRow);

        // Method row
        StringBuilder meta = new StringBuilder(stage.getMethod().getDisplayName());
        if (stage.isMembersOnly()) {
            meta.append("  ·  members");
        }
        // Reserve the icon's width so a long "method · members" string is truncated with a
        // tooltip rather than being clipped mid-word by the panel edge.
        JLabel method = fittedLabel(meta.toString(), FontManager.getRunescapeSmallFont(), 24);
        method.setIcon(MethodIcons.of(stage.getMethod()));
        method.setIconTextGap(4);
        method.setForeground(unlocked ? MUTED : LOCKED);
        card.add(method);

        // Explain unlock gates (quest or miniquest-chapter varbit) rather than just greying
        // the card out - otherwise barbarian fishing shows as locked with no reason why.
        if (lock != null && stage.getRequirement().hasUnlockGate()) {
            // ASCII only - the RuneScape pixel font has no glyph for symbols like key/lock.
            JLabel questRow = new JLabel("! " + lock);
            questRow.setFont(FontManager.getRunescapeSmallFont());
            questRow.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
            card.add(questRow);
        }

        // On the selected manual fish the locations become a picker: choose exactly where
        // to fish instead of letting nearest-wins decide.
        boolean pickable = !autoProgress && isActive && unlocked;
        if (pickable) {
            card.add(locationPickRow("Nearest", "auto", "".equals(pinnedLocation), ""));
        }

        // Locations. Filtered by world type so a free-to-play account is never shown - or
        // worse, allowed to pin - a members spot it could never walk to.
        for (FishingLocation location : stage.availableLocations(membersWorld)) {
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

    private String badgeText(FishingStage stage, boolean isActive, String lock) {
        if (isActive) {
            return autoProgress ? "ACTIVE" : "SELECTED";
        }
        if (lock != null) {
            // Unlock gates get their own row below, so keep the badge to the level.
            return stage.getRequirement().hasUnlockGate() ? "Lv " + stage.getMinLevel() : lock;
        }
        return autoProgress ? "Lv " + stage.getMinLevel() : "select";
    }

    /** Makes a whole card behave as one clickable target, including its children. */
    private void attachSelection(JPanel card, FishingStage stage) {
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

    /**
     * The aerial page: a readiness line, then the catch table.
     *
     * <p>There is nothing to choose here - which fish the cormorant brings back is rolled
     * from your Fishing and Hunter levels - so the page reports state rather than offering
     * selections.</p>
     */
    private void buildAerialPage() {
        aerialPage.removeAll();

        boolean ready = aerialUnmet == null;
        JPanel status = new JPanel(new DynamicGridLayout(0, 1, 0, 3));
        status.setBackground(CARD_BG);
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, ready ? ACTIVE : ColorScheme.PROGRESS_ERROR_COLOR),
                new EmptyBorder(7, 8, 7, 8)));

        JLabel title = new JLabel(ready ? "Ready" : "Not ready");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(ready ? ACTIVE : ColorScheme.PROGRESS_ERROR_COLOR);
        status.add(title);

        JLabel detail = fittedLabel(ready ? "Lake Molch - cormorant fishing"
                : "! " + aerialUnmet, FontManager.getRunescapeSmallFont(), 4);
        detail.setForeground(ready ? MUTED : ColorScheme.PROGRESS_ERROR_COLOR);
        status.add(detail);

        JLabel levels = new JLabel("Fishing " + fishingLevel + "  ·  Hunter " + hunterLevel);
        levels.setFont(FontManager.getRunescapeSmallFont());
        levels.setForeground(MUTED);
        status.add(levels);
        aerialPage.add(status);

        aerialPage.add(sectionLabel("CATCHES"));
        JLabel hint = new JLabel("Rolled from Fishing + Hunter");
        hint.setFont(FontManager.getRunescapeSmallFont());
        hint.setForeground(MUTED);
        hint.setBorder(new EmptyBorder(0, 2, 4, 0));
        aerialPage.add(hint);

        for (AerialCatch catchType : AerialCatch.values()) {
            boolean unlocked = fishingLevel >= catchType.getFishingLevel()
                    && hunterLevel >= catchType.getHunterLevel();
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(CARD_BG);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, unlocked ? UNLOCKED : LOCKED),
                    new EmptyBorder(6, 8, 6, 8)));

            JLabel name = new JLabel(catchType.getDisplayName());
            name.setFont(FontManager.getRunescapeBoldFont());
            name.setForeground(unlocked ? Color.WHITE : LOCKED);

            JLabel req = new JLabel(catchType.getFishingLevel() + " Fish / "
                    + catchType.getHunterLevel() + " Hunt");
            req.setFont(FontManager.getRunescapeSmallFont());
            req.setForeground(unlocked ? MUTED : LOCKED);
            req.setHorizontalAlignment(SwingConstants.RIGHT);

            card.add(name, BorderLayout.WEST);
            card.add(req, BorderLayout.EAST);
            aerialPage.add(card);
        }

        JLabel note = new JLabel("Catch is knifed into offcuts, reused as bait.");
        note.setFont(FontManager.getRunescapeSmallFont());
        note.setForeground(LOCKED);
        note.setBorder(new EmptyBorder(6, 2, 0, 0));
        aerialPage.add(note);

        aerialPage.revalidate();
        aerialPage.repaint();
    }

    /**
     * Rebuilds the method-icon key. Only shown in manual mode, where the full catalogue is
     * listed and the icons are the quickest way to tell the methods apart; in auto mode the
     * ladder is short and the extra row would just be noise.
     */
    private void buildLegend() {
        legendRow.removeAll();
        legendRow.setVisible(!autoProgress);
        if (autoProgress) {
            return;
        }
        for (FishingMethod method : FishingMethod.values()) {
            boolean selected = method == methodFilter;
            boolean dimmed = methodFilter != null && !selected;

            JLabel cell = new JLabel(MethodIcons.of(method));
            cell.setHorizontalAlignment(SwingConstants.CENTER);
            cell.setToolTipText(selected
                    ? method.getDisplayName() + " - click to clear filter"
                    : "Show only " + method.getDisplayName());
            cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cell.setOpaque(selected);
            if (selected) {
                cell.setBackground(CARD_HOVER_BG);
                cell.setBorder(BorderFactory.createLineBorder(UNLOCKED));
            } else {
                cell.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            }
            // Unselected icons fade while a filter is active, so the chosen one stands out
            // against ten near-identical shapes.
            if (dimmed) {
                cell.setIcon(MethodIcons.dimmed(method));
            }
            cell.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setMethodFilter(selected ? null : method);
                }
            });
            legendRow.add(cell);
        }
    }

    /** Clicking a legend icon narrows the list; clicking the active one clears it. */
    private void setMethodFilter(FishingMethod method) {
        methodFilter = method;
        rebuild();
    }

    /**
     * A label that will not overrun a sibling in the same BorderLayout row.
     *
     * <p>BorderLayout gives WEST its full preferred width, so a long stage name used to draw
     * straight over the EAST badge. Truncating to the space actually left over keeps any
     * future name safe without having to police the data.</p>
     */
    private static JLabel fittedLabel(String text, int reservedForSibling) {
        return fittedLabel(text, FontManager.getRunescapeBoldFont(), reservedForSibling);
    }

    private static JLabel fittedLabel(String text, Font font, int reservedForSibling) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        int available = PluginPanel.PANEL_WIDTH - 34 - reservedForSibling;
        java.awt.FontMetrics fm = label.getFontMetrics(label.getFont());
        if (fm.stringWidth(text) <= available) {
            return label;
        }
        String truncated = text;
        while (truncated.length() > 1 && fm.stringWidth(truncated + "..") > available) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        label.setText(truncated + "..");
        label.setToolTipText(text);
        return label;
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
