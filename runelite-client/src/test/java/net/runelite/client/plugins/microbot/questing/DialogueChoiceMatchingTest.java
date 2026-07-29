package net.runelite.client.plugins.microbot.questing;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Quest data drifts away from the game's wording, so matching a declared dialogue answer against the
 * live options is fuzzy. It is also the one decision in a quest that cannot be undone — the wrong
 * option can burn an item, lock content, or change the account permanently — so the tolerance has to
 * be wide enough to survive drift and no wider.
 */
public class DialogueChoiceMatchingTest
{
	private static boolean matches(String liveOption, String questChoice)
	{
		return QuestingScript.dialogueChoiceMatches(liveOption, questChoice);
	}

	@Test
	public void matchesExactly()
	{
		assertTrue(matches("Can I get a job here?", "Can I get a job here?"));
	}

	@Test
	public void ignoresCasePunctuationAndTags()
	{
		assertTrue(matches("<col=0000ff>Can I get a job here?</col>", "can i get a job here"));
	}

	@Test
	public void matchesWhenTheLiveOptionHasTrailingExtras()
	{
		assertTrue(matches("Yes please. I'll take two.", "Yes please."));
	}

	/** Pirate's Treasure: the quest declares "Well, can I get a job here?"; the game dropped "Well,". */
	@Test
	public void matchesWhenTheQuestDataHasLeadingFiller()
	{
		assertTrue(matches("Can I get a job here?", "Well, can I get a job here?"));
	}

	/**
	 * The guard on that tolerance. A bare suffix rule would let a short, common option satisfy a
	 * declared answer that means something different — answering "Thank you." to a step that wanted
	 * "No, thank you." inverts the reply.
	 */
	@Test
	public void doesNotMatchAShortSuffixOfTheDeclaredAnswer()
	{
		assertFalse(matches("Thank you.", "No, thank you."));
		assertFalse(matches("Yes.", "Ask about yes."));
	}

	@Test
	public void doesNotMatchUnrelatedOptions()
	{
		assertFalse(matches("What can you recommend?", "Can I get a job here?"));
		assertFalse(matches("No, thank you.", "Yes please."));
	}

	@Test
	public void handlesNullAndEmptyInput()
	{
		assertFalse(matches(null, "Can I get a job here?"));
		assertFalse(matches("Can I get a job here?", null));
		assertFalse(matches("Can I get a job here?", ""));
	}
}
