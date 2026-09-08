package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RunecraftingExitPortalSourceTest
{
	private static final Set<Integer> OBJECT_IDS = Set.of(34748, 34749, 34753, 34755, 34758);

	@Test
	public void wrongActionDuplicatesRemainAsSourceEvidence() throws IOException
	{
		String source = new String(getClass().getResourceAsStream(
			"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv").readAllBytes(),
			StandardCharsets.UTF_8);
		assertTrue(source.contains("Runecrafting exit portals expose Use, not Enter"));
		for (int objectId : OBJECT_IDS)
		{
			assertTrue(source.contains("Enter;Portal;" + objectId));
		}
	}

	@Test
	public void wrongActionDuplicatesAreNotLoaded()
	{
		Set<Transport> loaded = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(transport -> OBJECT_IDS.contains(transport.getObjectId()))
			.filter(transport -> "Enter".equals(transport.getAction()))
			.collect(Collectors.toSet());
		assertEquals(0, loaded.size());
	}
}
