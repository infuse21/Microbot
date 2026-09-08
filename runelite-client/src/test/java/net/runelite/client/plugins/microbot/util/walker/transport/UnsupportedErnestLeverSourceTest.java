package net.runelite.client.plugins.microbot.util.walker.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UnsupportedErnestLeverSourceTest
{
	private static final String RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";
	private static final String SOURCE =
		"# 3096 3357 0\t3098 3358 0\tPull;Lever;160\t\t\t\t\t\t\t\t6";

	@Test
	public void puzzleControlIsDisabledWithoutLosingItsSourceShape()
		throws IOException
	{
		assertFalse(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.anyMatch(row -> row.getObjectId() == 160));
		InputStream resource = UnsupportedErnestLeverSourceTest.class
			.getResourceAsStream(RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			String text = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
			assertTrue(text.contains("hauntedleverup is an Ernest the Chicken puzzle-state control"));
			assertTrue(text.contains(SOURCE));
			assertEquals(1, text.lines().filter(line -> line.equals(SOURCE)).count());
		}
	}
}
