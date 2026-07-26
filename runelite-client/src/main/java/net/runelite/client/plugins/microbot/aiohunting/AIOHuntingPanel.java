package net.runelite.client.plugins.microbot.aiohunting;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingMethod;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

public class AIOHuntingPanel extends PluginPanel
{
	private final AIOHuntingPlugin plugin;
	private final JLabel levelValue = new JLabel("--");
	private final JLabel activeValue = new JLabel("-");
	private final JLabel routeValue = new JLabel("-");
	private final JToggleButton autoMode = new JToggleButton("Auto");
	private final JToggleButton manualMode = new JToggleButton("Manual");
	private final JComboBox<HuntingMethod> methodPicker =
		new JComboBox<>(HuntingMethod.values());

	private int level = 1;
	private HuntingMethod active = HuntingMethod.CRIMSON_SWIFT;
	private boolean automatic = true;

	@Inject
	public AIOHuntingPanel(AIOHuntingPlugin plugin)
	{
		this.plugin = plugin;
		setBorder(new EmptyBorder(10, 8, 10, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(buildHeader());
		add(buildModeRow());
		add(buildStatus());
		add(buildMethodPicker());
		add(buildControls());
		rebuild();
	}

	public void update(int level, HuntingMethod active, boolean automatic)
	{
		this.level = level;
		this.active = active == null ? HuntingMethod.CRIMSON_SWIFT : active;
		this.automatic = automatic;
		SwingUtilities.invokeLater(this::rebuild);
	}

	private JPanel buildHeader()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(0, 0, 10, 0));
		JLabel title = new JLabel("AIO HUNTING");
		title.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
		title.setForeground(ColorScheme.BRAND_ORANGE);
		JLabel subtitle = new JLabel("Progression \u00b7 routes \u00b7 trap safety");
		subtitle.setFont(FontManager.getRunescapeSmallFont());
		subtitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		panel.add(title, BorderLayout.NORTH);
		panel.add(subtitle, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel buildModeRow()
	{
		JPanel row = new JPanel(new DynamicGridLayout(1, 2, 5, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		autoMode.addActionListener(event -> setAutomatic(true));
		manualMode.addActionListener(event -> setAutomatic(false));
		row.add(autoMode);
		row.add(manualMode);
		return row;
	}

	private JPanel buildStatus()
	{
		JPanel card = new JPanel(new DynamicGridLayout(0, 1, 0, 5));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0,
				ColorScheme.PROGRESS_COMPLETE_COLOR),
			new EmptyBorder(8, 8, 8, 8)));
		card.add(row("Hunter level", levelValue));
		card.add(row("Active", activeValue));
		card.add(row("Route", routeValue));
		return card;
	}

	private JPanel buildMethodPicker()
	{
		JPanel panel = new JPanel(new BorderLayout(0, 4));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(10, 0, 5, 0));
		JLabel label = new JLabel("MANUAL METHOD");
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setFont(FontManager.getRunescapeSmallFont());
		methodPicker.addActionListener(this::selectMethod);
		panel.add(label, BorderLayout.NORTH);
		panel.add(methodPicker, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel buildControls()
	{
		JPanel panel = new JPanel(new DynamicGridLayout(0, 1, 0, 5));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(8, 0, 0, 0));
		JButton start = new JButton("Start");
		JButton pause = new JButton("Pause / Resume");
		JButton stop = new JButton("Stop");
		start.addActionListener(event -> plugin.startScript());
		pause.addActionListener(event -> plugin.togglePause());
		stop.addActionListener(event -> plugin.stopScript());
		panel.add(start);
		panel.add(pause);
		panel.add(stop);
		panel.add(Box.createRigidArea(new Dimension(0, 5)));
		return panel;
	}

	private JPanel row(String label, JLabel value)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JLabel key = new JLabel(label);
		key.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		key.setFont(FontManager.getRunescapeSmallFont());
		value.setForeground(Color.WHITE);
		value.setFont(FontManager.getRunescapeSmallFont());
		row.add(key, BorderLayout.WEST);
		row.add(value, BorderLayout.EAST);
		return row;
	}

	private void setAutomatic(boolean enabled)
	{
		plugin.getConfigManager().setConfiguration(
			AIOHuntingConfig.GROUP, "autoProgress", enabled);
		automatic = enabled;
		rebuild();
	}

	private void selectMethod(ActionEvent event)
	{
		HuntingMethod selected = (HuntingMethod) methodPicker.getSelectedItem();
		if (selected != null)
		{
			plugin.getConfigManager().setConfiguration(
				AIOHuntingConfig.GROUP, "manualMethod", selected);
			if (!automatic)
			{
				active = selected;
			}
			rebuild();
		}
	}

	private void rebuild()
	{
		autoMode.setSelected(automatic);
		manualMode.setSelected(!automatic);
		methodPicker.setEnabled(!automatic);
		if (methodPicker.getSelectedItem() != active && !automatic)
		{
			methodPicker.setSelectedItem(active);
		}
		levelValue.setText(Integer.toString(level));
		activeValue.setText(active.getDisplayName());
		routeValue.setText(active.getStyle().getDisplayName());
		revalidate();
		repaint();
	}
}
