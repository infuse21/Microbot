package net.runelite.client.plugins.microbot.util.walker.transport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnsafeFailureShortcutSourceTest
{
	private static final Set<Integer> OBJECT_IDS = Set.of(4615, 4616, 23644);

	@Test
	public void failureProneRowsRemainAsSourceEvidence() throws IOException
	{
		String source = new String(getClass().getResourceAsStream(
			"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv").readAllBytes(),
			StandardCharsets.UTF_8);
		assertTrue(source.contains("# 2598 3608 0\t2596 3608 0\tCross;Broken bridge;4616"));
		assertTrue(source.contains("# 2596 3608 0\t2598 3608 0\tCross;Broken bridge;4615"));
		assertTrue(source.contains("# 2910 3049 0\t2906 3049 0\tCross;A wooden log;23644"));
		assertTrue(source.contains("# 2906 3049 0\t2910 3049 0\tCross;A wooden log;23644"));
	}

	@Test
	public void unstagedFailureShortcutsAreNotLoaded()
	{
		long loaded = Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> OBJECT_IDS.contains(row.getObjectId()))
			.count();
		assertEquals(0, loaded);
	}
}
