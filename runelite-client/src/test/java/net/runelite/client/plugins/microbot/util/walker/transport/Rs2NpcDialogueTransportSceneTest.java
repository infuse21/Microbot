package net.runelite.client.plugins.microbot.util.walker.transport;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Rs2NpcDialogueTransportSceneTest
{
	@Test
	public void exactOptionAlwaysWinsOverContainment()
	{
		List<String> molch = Arrays.asList("Molch Island", "Molch", "Battlefront", "Shayzien");

		assertEquals(1, Rs2NpcDialogueTransportScene.matchOptionIndex(molch, "Molch"));
		assertEquals(0, Rs2NpcDialogueTransportScene.matchOptionIndex(molch, "Molch Island"));
	}

	@Test
	public void sentencePhrasedMenuMatchesUniqueContainingOption()
	{
		List<String> magoro = Arrays.asList("Where am I exactly?",
			"Can you take me to Port Piscarilius please?",
			"Can you take me to Port Sarim please?", "Nothing.");

		assertEquals(2, Rs2NpcDialogueTransportScene.matchOptionIndex(magoro, "Port Sarim"));
		assertEquals(1, Rs2NpcDialogueTransportScene.matchOptionIndex(magoro,
			"Port Piscarilius"));
	}

	@Test
	public void ambiguousContainmentIsRefused()
	{
		List<String> ambiguous = Arrays.asList("Sail to Molch Island now",
			"Return from Molch Island later");

		assertEquals(-1, Rs2NpcDialogueTransportScene.matchOptionIndex(ambiguous, "Molch Island"));
		assertEquals(-1, Rs2NpcDialogueTransportScene.matchOptionIndex(ambiguous, "Karamja"));
		assertEquals(-1, Rs2NpcDialogueTransportScene.matchOptionIndex(null, "Molch"));
		assertEquals(-1, Rs2NpcDialogueTransportScene.matchOptionIndex(ambiguous, null));
	}

	@Test
	public void taggedAndSpacedOptionTextStillMatches()
	{
		List<String> tagged = Arrays.asList("<col=0000ff>Port  Sarim</col>", "Nothing.");

		assertEquals(0, Rs2NpcDialogueTransportScene.matchOptionIndex(tagged, "Port Sarim"));
	}

	@Test
	public void fareConfirmationMatchesTheSingleAffirmativeOption()
	{
		assertEquals(0, Rs2NpcDialogueTransportScene.matchConfirmIndex(
			Arrays.asList("Yes please.", "No, thank you.")));
		assertEquals(1, Rs2NpcDialogueTransportScene.matchConfirmIndex(
			Arrays.asList("No thanks.", "Ok")));
		assertEquals(-1, Rs2NpcDialogueTransportScene.matchConfirmIndex(
			Arrays.asList("Yes please.", "Yes, and don't ask again.")));
		assertEquals(-1, Rs2NpcDialogueTransportScene.matchConfirmIndex(
			Arrays.asList("Where am I?", "Nothing.")));
		assertEquals(-1, Rs2NpcDialogueTransportScene.matchConfirmIndex(null));
	}
}
