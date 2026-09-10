package net.runelite.client.plugins.microbot.util.walker.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ResourceAreaGateSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";

	@Test
	public void allFiveRowsAreCatalogOwnedAndOnlyPaidRowsNeedBankPlanning()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == 26760).collect(Collectors.toList());
		assertEquals(5, rows.size());
		for (Transport row : rows)
		{
			assertTrue(ResourceAreaGatePolicy.isEligible(row));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			org.junit.Assert.assertFalse(AdjacentTransportPolicy.isEligible(row));
			assertEquals(row.getCurrencyAmount() > 0,
				net.runelite.client.plugins.microbot.util.walker.banking.Rs2WalkerBankingPlanner.requiresBankPlanning(row));
		}
	}

	@Test
	public void everyDiaryCombinationSelectsExactlyOneEntryAndKeepsTheExitFree()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream).filter(row -> row.getObjectId() == 26760)
			.collect(Collectors.toList());
		for (int mask = 0; mask < 8; mask++)
		{
			Map<Integer, Integer> diary = Map.of(4467, mask & 1, 4468, (mask >> 1) & 1,
				4469, (mask >> 2) & 1);
			java.util.List<Transport> eligible = rows.stream().filter(row -> row.getVarbits().stream()
				.allMatch(bit -> bit.matches(diary.get(bit.getVarbitId())))).collect(Collectors.toList());
			assertEquals(2, eligible.size());
			Transport entry = eligible.stream().filter(row -> ResourceAreaGatePolicy.OUTSIDE.equals(row.getOrigin()))
				.findFirst().orElseThrow(AssertionError::new);
			int expectedFare = (mask & 4) != 0 ? 0 : (mask & 2) != 0 ? 3750 : (mask & 1) != 0 ? 6000 : 7500;
			assertEquals(expectedFare, entry.getCurrencyAmount());
			Transport exit = eligible.stream().filter(row -> ResourceAreaGatePolicy.INSIDE.equals(row.getOrigin()))
				.findFirst().orElseThrow(AssertionError::new);
			assertEquals(0, exit.getCurrencyAmount());
			assertTrue(exit.getVarbits().isEmpty());
		}
	}

	@Test
	public void allFiveRowsRetainTheirDirectedFareDiaryAndMembersContract()
		throws IOException
	{
		Map<String, String> expected = Map.of(
			"3184 3944 0>3184 3945 0", "||Y|2",
			"3184 3945 0>3184 3944 0", "7500 Coins|4469=0;4468=0;4467=0;|Y|2",
			"3184 3945 0>3184 3944 0#6000", "6000 Coins|4469=0;4468=0;4467=1|Y|2",
			"3184 3945 0>3184 3944 0#3750", "3750 Coins|4469=0;4468=1|Y|2",
			"3184 3945 0>3184 3944 0#elite", "|4469=1|Y|2");
		InputStream resource = ResourceAreaGateSourceTest.class.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Map<String, String> disabled = reader.lines()
				.filter(line -> !line.startsWith("#") && line.contains(";26760"))
				.map(line -> line.split("\\t", -1))
				.collect(Collectors.toMap(ResourceAreaGateSourceTest::key,
					columns -> value(columns, 5) + "|" + value(columns, 7)
						+ "|" + value(columns, 9) + "|" + value(columns, 10)));
			assertEquals(expected, disabled);
		}
	}

	private static String key(String[] columns)
	{
		String base = columns[0] + ">" + columns[1];
		String fare = value(columns, 5);
		String diary = value(columns, 7);
		if ("6000 Coins".equals(fare))
		{
			return base + "#6000";
		}
		if ("3750 Coins".equals(fare))
		{
			return base + "#3750";
		}
		return "4469=1".equals(diary) ? base + "#elite" : base;
	}

	private static String value(String[] columns, int index)
	{
		return index < columns.length ? columns[index].trim() : "";
	}
}
