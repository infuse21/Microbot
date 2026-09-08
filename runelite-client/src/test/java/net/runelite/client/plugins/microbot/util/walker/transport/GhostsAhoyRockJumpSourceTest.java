package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GhostsAhoyRockJumpSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";

	@Test
	public void unsafeRockJumpsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> row.getObjectId() == 16115));
	}

	@Test
	public void unsafeRockJumpsRemainAsExactSourceEvidence()
		throws IOException
	{
		Set<String> expected = Set.of(
			"3604 3550 0>3602 3550 0", "3602 3550 0>3604 3550 0",
			"3599 3552 0>3597 3552 0", "3597 3552 0>3599 3552 0",
			"3595 3554 0>3595 3556 0", "3595 3556 0>3595 3554 0",
			"3597 3559 0>3597 3561 0", "3597 3561 0>3597 3559 0",
			"3599 3564 0>3601 3564 0", "3601 3564 0>3599 3564 0");
		InputStream resource = GhostsAhoyRockJumpSourceTest.class
			.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Set<String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ") && line.contains("Jump-To;Rock;16115"))
				.map(line -> line.substring(2).split("\\t"))
				.map(columns -> columns[0] + ">" + columns[1])
				.collect(Collectors.toSet());
			assertEquals(expected, disabled);
		}
	}
}
