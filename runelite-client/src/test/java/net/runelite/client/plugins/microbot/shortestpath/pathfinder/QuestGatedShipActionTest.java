package net.runelite.client.plugins.microbot.shortestpath.pathfinder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Veos/Captain Magoro rows are rewritten at refresh time by Client of Kourend state. The
 * rewrite must be reversible: one early quest-state misread previously left the shared row
 * on Talk-to for the whole session, legacy-locking routes on accounts that finished the quest.
 */
public class QuestGatedShipActionTest
{
	@Test
	public void unfinishedQuestForcesTalkTo()
	{
		assertEquals("Talk-to",
			PathfinderConfig.questGatedShipAction("Port Sarim", "Port Sarim", false));
		assertEquals("Talk-to",
			PathfinderConfig.questGatedShipAction("Talk-to", "Port Sarim", false));
	}

	@Test
	public void finishedQuestRestoresDirectActionFromDisplayInfo()
	{
		assertEquals("Port Sarim",
			PathfinderConfig.questGatedShipAction("Talk-to", "Port Sarim", true));
		assertEquals("Land's End",
			PathfinderConfig.questGatedShipAction("Talk-to", "Land's End", true));
	}

	@Test
	public void finishedQuestLeavesUnmutatedRowAlone()
	{
		assertEquals("Port Sarim",
			PathfinderConfig.questGatedShipAction("Port Sarim", "Port Sarim", true));
		assertEquals("Talk-to",
			PathfinderConfig.questGatedShipAction("Talk-to", null, true));
		assertEquals("Talk-to",
			PathfinderConfig.questGatedShipAction("Talk-to", "", true));
	}
}
