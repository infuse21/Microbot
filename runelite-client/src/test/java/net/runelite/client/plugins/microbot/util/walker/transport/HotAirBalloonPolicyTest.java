package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HotAirBalloonPolicyTest
{
	@Test
	public void acceptsUnlockedStationBasketVariants()
	{
		assertTrue(HotAirBalloonPolicy.isBasketObjectId(19128));
		assertTrue(HotAirBalloonPolicy.isBasketObjectId(19129));
		assertTrue(HotAirBalloonPolicy.isBasketObjectId(19133));
		assertTrue(HotAirBalloonPolicy.isBasketObjectId(19135));
		assertTrue(HotAirBalloonPolicy.isBasketObjectId(19137));
		assertTrue(HotAirBalloonPolicy.isBasketObjectId(19139));
		assertTrue(HotAirBalloonPolicy.isBasketObjectId(19141));
		assertTrue(HotAirBalloonPolicy.isBasketObjectId(19143));
		assertFalse(HotAirBalloonPolicy.isBasketObjectId(19134));
	}

	@Test
	public void acceptsAllTwoHundredTwentyFiveGeneratedCatalogEdges()
	{
		List<Transport> balloons = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(transport -> transport.getType() == TransportType.HOT_AIR_BALLOON)
			.collect(Collectors.toList());

		assertEquals(225, balloons.size());
		assertTrue(balloons.stream().allMatch(HotAirBalloonPolicy::isEligible));
		assertTrue(balloons.stream().allMatch(Transport::isConsumable));
	}

	@Test
	public void destinationDeterminesTheConsumedLog()
	{
		assertEquals(ItemID.LOGS,
			HotAirBalloonPolicy.requiredLogId("Entrana").intValue());
		assertEquals(ItemID.LOGS,
			HotAirBalloonPolicy.requiredLogId("Taverley").intValue());
		assertEquals(ItemID.OAK_LOGS,
			HotAirBalloonPolicy.requiredLogId("Crafting Guild").intValue());
		assertEquals(ItemID.WILLOW_LOGS,
			HotAirBalloonPolicy.requiredLogId("Varrock").intValue());
		assertEquals(ItemID.YEW_LOGS,
			HotAirBalloonPolicy.requiredLogId("Castle Wars").intValue());
		assertEquals(ItemID.MAGIC_LOGS,
			HotAirBalloonPolicy.requiredLogId("Grand Tree").intValue());
	}

	@Test
	public void rejectsMalformedOrRequirementFreeRows()
	{
		Transport missingRequirement = new Transport(
			new net.runelite.api.coords.WorldPoint(2939, 3423, 0),
			new net.runelite.api.coords.WorldPoint(3299, 3482, 0), "Varrock",
			TransportType.HOT_AIR_BALLOON, true, "Use", "Basket", 19129);

		assertFalse(HotAirBalloonPolicy.isEligible(missingRequirement));
		missingRequirement.setItemIdRequirements(Set.of(Set.of(ItemID.WILLOW_LOGS)));
		assertFalse("the required log must also be marked consumable",
			HotAirBalloonPolicy.isEligible(missingRequirement));
		assertFalse(HotAirBalloonPolicy.isDestinationAction("Use"));
		assertTrue(HotAirBalloonPolicy.isDestinationAction(
			HotAirBalloonPolicy.destinationAction("Varrock")));
	}
}
