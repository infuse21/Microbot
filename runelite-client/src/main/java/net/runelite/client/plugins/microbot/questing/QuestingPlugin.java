package net.runelite.client.plugins.microbot.questing;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.questhelper.QuestHelperConfig;
import net.runelite.client.plugins.microbot.questhelper.QuestHelperPlugin;

import javax.inject.Inject;

/**
 * Automation on top of the Quest Helper plugin.
 *
 * <p>The Quest Helper package is vendored upstream content and is treated as read-only: it supplies
 * the quest model (steps, conditions, requirements) and this plugin executes it. Keeping the executor
 * in its own package means upstream Quest Helper can be pulled wholesale without merge conflicts —
 * see {@code docs/questing-executor-extraction.md}.
 *
 * <p>Requires the Quest Helper plugin to be enabled as well: the selected quest and its current step
 * come from there.
 */
@PluginDescriptor(
	name = "Microbot Questing",
	description = "Automates the quest selected in Quest Helper",
	tags = { "quest", "microbot", "automation" },
	enabledByDefault = false
)
@Slf4j
public class QuestingPlugin extends Plugin
{
	@Inject
	private QuestingScript questingScript;

	@Inject
	private ConfigManager configManager;

	@Override
	protected void startUp()
	{
		QuestHelperPlugin questHelperPlugin = findQuestHelperPlugin();
		if (questHelperPlugin == null)
		{
			log.warn("Quest Helper plugin not found — enable it; the questing automation reads the selected quest from it.");
			return;
		}
		questingScript.run(configManager.getConfig(QuestHelperConfig.class), questHelperPlugin);
	}

	@Override
	protected void shutDown()
	{
		questingScript.shutdown();
	}

	/** Forwarded so the script's "I can't reach that!" recovery can trigger. */
	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		questingScript.onChatMessage(chatMessage);
	}

	private QuestHelperPlugin findQuestHelperPlugin()
	{
		return (QuestHelperPlugin) Microbot.getPluginManager().getPlugins().stream()
			.filter(x -> x instanceof QuestHelperPlugin)
			.findFirst()
			.orElse(null);
	}
}
