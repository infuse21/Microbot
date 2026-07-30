package net.runelite.client.plugins.microbot.aiofishing;

import net.runelite.client.plugins.microbot.aiofishing.enums.FishingMethod;

import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Small hand-drawn icons for each {@link FishingMethod}.
 *
 * <p>They exist so a stage card can show its method without spending horizontal space on
 * text - the long "Big net: ..." style names were colliding with the card's status badge.</p>
 *
 * <p>Drawn programmatically rather than shipped as PNGs so there are no binary assets to
 * package, and so they stay legible against the panel's dark background. Icons are grouped
 * into shape families (net / rod / cage / spear / vessel) and separated within a family by
 * an accent colour, which is what makes them distinguishable at 16px.</p>
 */
public final class MethodIcons {

    private static final int SIZE = 16;

    private static final Color STEEL = new Color(185, 190, 200);
    private static final Color WOOD = new Color(150, 105, 60);
    private static final Color MESH = new Color(120, 205, 235);
    private static final Color BAIT_BROWN = new Color(170, 120, 70);
    private static final Color FEATHER = new Color(240, 240, 240);
    private static final Color OIL = new Color(105, 170, 105);
    private static final Color WORM = new Color(230, 140, 160);
    private static final Color POT_ORANGE = new Color(220, 130, 60);
    private static final Color POT_DARK = new Color(140, 120, 165);
    private static final Color VESSEL = new Color(205, 175, 120);
    private static final Color BARB_ACCENT = new Color(225, 175, 90);

    private static final Map<FishingMethod, ImageIcon> CACHE = new EnumMap<>(FishingMethod.class);

    private MethodIcons() {
    }

    /** Cached icon for a method. Safe to call repeatedly while rebuilding the panel. */
    public static synchronized ImageIcon of(FishingMethod method) {
        return CACHE.computeIfAbsent(method, m -> new ImageIcon(draw(m)));
    }

    private static BufferedImage draw(FishingMethod method) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            switch (method) {
                case NET:            smallNet(g); break;
                case BIG_NET:        bigNet(g); break;
                case BAIT:           rod(g, BAIT_BROWN, false, false); break;
                case LURE:           rod(g, FEATHER, true, false); break;
                case OILY_ROD:       rod(g, OIL, false, false); break;
                case SANDWORMS:      rod(g, WORM, false, false); break;
                case BARBARIAN_ROD:  rod(g, BARB_ACCENT, true, true); break;
                case CAGE:           cage(g, POT_ORANGE); break;
                case DARK_CRAB_CAGE: cage(g, POT_DARK); break;
                case HARPOON:        harpoon(g); break;
                case KARAMBWAN_VESSEL: vessel(g); break;
                default:             rod(g, STEEL, false, false); break;
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    /**
     * Small net: a compact mesh head on a handle. The handle is the cue that separates it
     * from {@link #bigNet} - at 16px an extra row of mesh is invisible, so the two nets
     * differ in silhouette rather than in mesh density.
     */
    private static void smallNet(Graphics2D g) {
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(WOOD);
        g.drawLine(2, 14, 7, 9); // handle
        g.setStroke(new BasicStroke(1f));
        g.setColor(MESH);
        // Mesh head: a small bag in the top-right corner.
        g.drawLine(6, 8, 10, 3);
        g.drawLine(10, 3, 14, 6);
        g.drawLine(14, 6, 10, 10);
        g.drawLine(10, 10, 6, 8);
        g.drawLine(8, 5, 12, 8); // mesh
        g.drawLine(8, 9, 12, 4);
    }

    /** Big net: a wide seine net filling the tile, no handle. */
    private static void bigNet(Graphics2D g) {
        g.setStroke(new BasicStroke(1f));
        g.setColor(MESH);
        int topY = 2;
        int botY = 14;
        int topL = 1;
        int topR = 15;
        int botL = 5;
        int botR = 11;
        g.drawLine(topL, topY, botL, botY);
        g.drawLine(topR, topY, botR, botY);
        g.drawLine(topL, topY, topR, topY); // head rope
        g.drawLine(botL, botY, botR, botY);
        for (int i = 1; i <= 3; i++) {
            float t = i / 4f;
            int y = Math.round(topY + t * (botY - topY));
            int l = Math.round(topL + t * (botL - topL));
            int r = Math.round(topR + t * (botR - topR));
            g.drawLine(l, y, r, y);
        }
        g.drawLine(8, topY, 8, botY); // centre brace
    }

    /**
     * A rod held diagonally with a line hanging from the tip and an accent at the end -
     * the accent tells bait / fly / oily / sandworm apart.
     *
     * @param feathered draw a feather at the line's end instead of a bait blob
     * @param crude     thicker, darker shaft with a knot - reads as the barbarian rod, so it
     *                  is not mistaken for the slim fly rod which also carries a feather
     */
    private static void rod(Graphics2D g, Color accent, boolean feathered, boolean crude) {
        g.setStroke(new BasicStroke(crude ? 2.6f : 1.5f));
        g.setColor(crude ? WOOD.darker() : WOOD);
        g.drawLine(2, 14, 12, 4); // shaft, butt bottom-left to tip top-right
        if (crude) {
            g.setColor(BARB_ACCENT.darker());
            g.fillOval(6, 8, 3, 3); // knot, to sell the "crude branch" look
        }
        g.setStroke(new BasicStroke(1f));
        g.setColor(STEEL);
        g.drawLine(12, 4, 13, 8); // line dropping from the tip
        g.setColor(accent);
        if (feathered) {
            feather(g, accent);
        } else {
            g.fillOval(11, 8, 5, 5); // bait blob
        }
    }

    /** A quill with barbs, drawn solidly enough to read at 16px. */
    private static void feather(Graphics2D g, Color accent) {
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(accent);
        g.drawLine(13, 8, 11, 14); // quill
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i < 3; i++) {
            int y = 9 + i * 2;
            int x = 13 - (int) Math.round(i * 0.7);
            g.drawLine(x, y, x - 3, y + 1); // left barbs
            g.drawLine(x, y, x + 2, y - 1); // right barbs
        }
    }

    /** A domed trap with bars - lobster pot family. */
    private static void cage(Graphics2D g, Color color) {
        g.setStroke(new BasicStroke(1f));
        g.setColor(color);
        g.drawArc(2, 4, 12, 14, 0, 180); // dome
        g.drawLine(2, 11, 14, 11);       // base
        g.drawLine(5, 5, 5, 11);         // bars
        g.drawLine(8, 4, 8, 11);
        g.drawLine(11, 5, 11, 11);
    }

    /** A three-pronged spear. */
    private static void harpoon(Graphics2D g) {
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(WOOD);
        g.drawLine(8, 8, 8, 14); // shaft
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(STEEL);
        g.drawLine(8, 8, 8, 2);  // centre prong
        g.drawLine(4, 3, 5, 8);  // left prong
        g.drawLine(12, 3, 11, 8); // right prong
        g.drawLine(5, 8, 11, 8); // cross piece
    }

    /** A round-bellied vessel - karambwan. */
    private static void vessel(Graphics2D g) {
        g.setStroke(new BasicStroke(1f));
        g.setColor(VESSEL);
        g.fillOval(4, 6, 8, 8);   // belly
        g.setColor(WOOD);
        g.drawOval(4, 6, 8, 8);
        g.drawLine(5, 5, 11, 5);  // rim
        g.drawLine(7, 3, 9, 3);   // neck
        g.drawLine(7, 3, 5, 5);
        g.drawLine(9, 3, 11, 5);
    }
}
