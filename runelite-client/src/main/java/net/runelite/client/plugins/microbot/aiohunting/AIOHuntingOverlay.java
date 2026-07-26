package net.runelite.client.plugins.microbot.aiohunting;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingMethod;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class AIOHuntingOverlay extends OverlayPanel
{
	private static final Color ACCENT = new Color(220, 138, 0);
	private static final Color ACTIVE = new Color(70, 205, 90);

	private final AIOHuntingPlugin plugin;

	@Inject
	AIOHuntingOverlay(AIOHuntingPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		panelComponent.setPreferredSize(new Dimension(220, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("AIO HUNTING")
			.color(ACCENT)
			.build());

		AIOHuntingScript script = plugin.getScript();
		boolean running = plugin.isScriptRunning();
		String state = !running ? "Stopped"
			: plugin.isScriptPaused() ? "Paused"
			: script.getStatus();
		HuntingMethod method = plugin.resolveDisplayMethod();

		addLine("Status", state, running ? ACTIVE : Color.LIGHT_GRAY);
		addLine("Mode", plugin.getConfig().autoProgress() ? "Auto progression" : "Manual",
			Color.WHITE);
		addLine("Target", method == null ? "-" : method.getDisplayName(), ACCENT);
		addLine("Method", method == null ? "-" : method.getStyle().getDisplayName(),
			Color.WHITE);
		addLine("Hunter", Integer.toString(Rs2Player.getRealSkillLevel(Skill.HUNTER)),
			Color.WHITE);
		addLine("XP gained", Integer.toString(plugin.getXpGained()), ACTIVE);
		addLine("Caught", Integer.toString(plugin.getTotalCatches()), ACTIVE);

		for (Map.Entry<HuntingMethod, Integer> catchEntry : plugin.getCatches().entrySet())
		{
			addLine("  " + catchEntry.getKey().getDisplayName(),
				Integer.toString(catchEntry.getValue()), Color.WHITE);
		}
		addLine("Runtime", formatRuntime(plugin.getRuntimeMillis()), Color.WHITE);
		return super.render(graphics);
	}

	private void addLine(String left, String right, Color color)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left(left)
			.right(right)
			.rightColor(color)
			.build());
	}

	private static String formatRuntime(long millis)
	{
		long hours = millis / 3_600_000L;
		long minutes = millis % 3_600_000L / 60_000L;
		long seconds = millis % 60_000L / 1000L;
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}
}
