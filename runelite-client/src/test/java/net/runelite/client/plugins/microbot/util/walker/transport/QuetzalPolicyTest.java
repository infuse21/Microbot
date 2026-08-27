package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuetzalPolicyTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(1697, 3140, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(1437, 3171, 0);

	@Test
	public void acceptsDirectedCatalogRouteAndBuildsDestinationAction()
	{
		Transport quetzal = transport("The Teomat");

		assertTrue(QuetzalPolicy.isEligible(quetzal));
		assertEquals("The Teomat", QuetzalPolicy.destinationName(quetzal));
		assertEquals("quetzal-destination:The Teomat",
			QuetzalPolicy.destinationAction("The Teomat"));
	}

	@Test
	public void rejectsMalformedOrDifferentTransportRows()
	{
		assertFalse(QuetzalPolicy.isEligible(transport("")));
		assertFalse(QuetzalPolicy.isEligible(new Transport(ORIGIN, DESTINATION,
			"The Teomat", TransportType.GNOME_GLIDER, true, 6)));
		assertFalse(QuetzalPolicy.isEligible(new Transport(ORIGIN, ORIGIN,
			"The Teomat", TransportType.QUETZAL, true, 6)));
	}

	@Test
	public void transformedRenuRequiresExactNameActionPlaneAndOrigin()
	{
		Transport quetzal = transport("The Teomat");
		assertTrue(QuetzalPolicy.isLiveNpcMatch(quetzal, "Renu",
			new String[]{"Travel", "Examine"}, ORIGIN));
		assertFalse(QuetzalPolicy.isLiveNpcMatch(quetzal, "Renu",
			new String[]{"Feed", "Examine"}, ORIGIN));
		assertFalse(QuetzalPolicy.isLiveNpcMatch(quetzal, "Other",
			new String[]{"Travel"}, ORIGIN));
		assertFalse(QuetzalPolicy.isLiveNpcMatch(quetzal, "Renu",
			new String[]{"Travel"}, new WorldPoint(ORIGIN.getX(), ORIGIN.getY(), 1)));
		assertFalse(QuetzalPolicy.isLiveNpcMatch(quetzal, "Renu",
			new String[]{"Travel"}, new WorldPoint(ORIGIN.getX() + 9,
				ORIGIN.getY(), ORIGIN.getPlane())));
	}

	private static Transport transport(String displayInfo)
	{
		return new Transport(ORIGIN, DESTINATION, displayInfo,
			TransportType.QUETZAL, true, 6);
	}
}
