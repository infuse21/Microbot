package net.runelite.client.plugins.microbot.questing;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Exercises the shipped wiki corpus against the failure it was built to fix.
 *
 * <p>Talking to Wydin in Pirate's Treasure offers three shop options and one quest option. The quest
 * data declares the wording from a <i>different</i> menu (the one you get by trying the back door
 * first), so nothing matched and the bot clicked option 1 — which opens the shop.
 */
public class QuestDialogueCorpusTest
{
	private static final String QUEST = "Pirate's Treasure";

	/** The live menu, in the order the game presents it. */
	private static final List<String> WYDIN = Arrays.asList(
			"Yes please.",
			"No, thank you.",
			"What can you recommend?",
			"Can I get a job here?");

	@Test
	public void corpusIsOnTheClasspath()
	{
		assertTrue("quest-dialogue.tsv should ship as a resource", QuestDialogueCorpus.hasQuest(QUEST));
	}

	@Test
	public void resolvesTheMenuThatUsedToOpenTheShop()
	{
		assertEquals("Can I get a job here?", QuestDialogueCorpus.answerForMenu(QUEST, WYDIN));
	}

	@Test
	public void optionOrderDoesNotMatter()
	{
		List<String> shuffled = Arrays.asList(
				"Can I get a job here?",
				"What can you recommend?",
				"Yes please.",
				"No, thank you.");
		assertEquals("Can I get a job here?", QuestDialogueCorpus.answerForMenu(QUEST, shuffled));
	}

	@Test
	public void toleratesColourTagsAndPunctuation()
	{
		List<String> tagged = Arrays.asList(
				"<col=0000ff>Yes please.</col>",
				"<col=0000ff>No, thank you.</col>",
				"<col=0000ff>What can you recommend?</col>",
				"<col=0000ff>Can I get a job here?</col>");
		// the live string is handed back, so the caller can index straight into the menu
		assertEquals("<col=0000ff>Can I get a job here?</col>",
				QuestDialogueCorpus.answerForMenu(QUEST, tagged));
	}

	@Test
	public void marksShopOptionsAsUnrelatedToTheQuest()
	{
		assertTrue(QuestDialogueCorpus.isNonQuestOption(QUEST, "Yes please."));
		assertFalse(QuestDialogueCorpus.isNonQuestOption(QUEST, "Can I get a job here?"));
	}

	@Test
	public void questRelatedOptionsExcludesTheShopOptions()
	{
		assertEquals(Collections.singletonList("Can I get a job here?"),
				QuestDialogueCorpus.questRelatedOptions(QUEST, WYDIN));
	}

	/**
	 * A menu with more than one quest option is a real branch — which gang, which reward — and is
	 * deliberately absent from the corpus so it can never be auto-answered.
	 */
	@Test
	public void doesNotAnswerUnknownMenus()
	{
		assertNull(QuestDialogueCorpus.answerForMenu(QUEST,
				Arrays.asList("Something nobody ever said.", "Nor this.")));
	}

	@Test
	public void ignoresSingleOptionMenus()
	{
		assertNull(QuestDialogueCorpus.answerForMenu(QUEST, Collections.singletonList("Yes please.")));
	}

	@Test
	public void isSilentAboutQuestsItDoesNotKnow()
	{
		assertFalse(QuestDialogueCorpus.hasQuest("Not A Real Quest"));
		assertNull(QuestDialogueCorpus.answerForMenu("Not A Real Quest", WYDIN));
		assertFalse(QuestDialogueCorpus.isNonQuestOption("Not A Real Quest", "Yes please."));
		assertTrue(QuestDialogueCorpus.questRelatedOptions("Not A Real Quest", WYDIN).isEmpty());
	}

	@Test
	public void handlesNullsWithoutThrowing()
	{
		assertNull(QuestDialogueCorpus.answerForMenu(null, WYDIN));
		assertNull(QuestDialogueCorpus.answerForMenu(QUEST, null));
		assertFalse(QuestDialogueCorpus.isNonQuestOption(QUEST, null));
	}

	/** Quest names differ in punctuation between the wiki and the client. */
	@Test
	public void questNamesMatchOnLettersOnly()
	{
		assertEquals(QuestDialogueCorpus.questKey("Recipe for Disaster - Wartface & Bentnoze"),
				QuestDialogueCorpus.questKey("recipe for disaster  wartface and bentnoze"));
	}
}
