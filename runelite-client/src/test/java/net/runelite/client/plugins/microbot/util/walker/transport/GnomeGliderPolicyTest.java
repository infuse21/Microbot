package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GnomeGliderPolicyTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(2465, 3501, 3);
	private static final WorldPoint DESTINATION = new WorldPoint(3284, 3210, 0);

	@Test
	public void acceptsDirectedCatalogGliderAndBuildsDestinationAction()
	{
		Transport glider = transport("Kar-Hewo", 10467);

		assertTrue(GnomeGliderPolicy.isEligible(glider));
		assertEquals("Kar-Hewo", GnomeGliderPolicy.destinationName(glider));
		assertEquals("gnome-glider-destination:Kar-Hewo",
			GnomeGliderPolicy.destinationAction("Kar-Hewo"));
	}

	@Test
	public void rejectsMalformedRows()
	{
		assertFalse(GnomeGliderPolicy.isEligible(transport("", 10467)));
		assertFalse(GnomeGliderPolicy.isEligible(transport("Kar-Hewo", 0)));
		assertFalse(GnomeGliderPolicy.isEligible(new Transport(ORIGIN, DESTINATION,
			"Kar-Hewo", TransportType.GNOME_GLIDER, true,
			"Travel", "Captain Errdo", 10467)));
	}

	@Test
	public void hiddenDestinationButtonIsUnavailable()
	{
		assertTrue(GnomeGliderPolicy.isDestinationSelectable(false));
		assertFalse(GnomeGliderPolicy.isDestinationSelectable(true));
	}

	@Test
	public void transformedCaptainRequiresExactIdentityActionAndOrigin()
	{
		Transport glider = transport("Kar-Hewo", 10459);
		assertTrue(GnomeGliderPolicy.isLiveNpcMatch(glider, 10461,
			"Captain Errdo", new String[]{"Talk-to", "Glider"}, ORIGIN));
		assertFalse(GnomeGliderPolicy.isLiveNpcMatch(glider, 10461,
			"Captain Errdo", new String[]{"Talk-to"}, ORIGIN));
		assertFalse(GnomeGliderPolicy.isLiveNpcMatch(glider, 10461,
			"Other captain", new String[]{"Glider"}, ORIGIN));
		assertFalse(GnomeGliderPolicy.isLiveNpcMatch(glider, 10461,
			"Captain Errdo", new String[]{"Glider"},
			new WorldPoint(ORIGIN.getX() + 7, ORIGIN.getY(), ORIGIN.getPlane())));
	}

	private static Transport transport(String displayInfo, int npcId)
	{
		return new Transport(ORIGIN, DESTINATION, displayInfo,
			TransportType.GNOME_GLIDER, true, "Glider", "Captain Errdo", npcId);
	}
}
