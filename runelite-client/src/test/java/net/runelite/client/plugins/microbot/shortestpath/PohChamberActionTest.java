package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.GameObject;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.poh.data.PohPortal;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

public class PohChamberActionTest
{
	@Test
	public void everyChamberDispatchUsesItsDestinationAction()
	{
		for (PohPortal portal : PohPortal.values())
		{
			String expected;
			switch (portal)
			{
				case VARROCK: expected = "Varrock"; break;
				case GRAND_EXCHANGE: expected = "Grand Exchange"; break;
				case CAMELOT: expected = "Camelot"; break;
				case WATCHTOWER: expected = "Watchtower"; break;
				default: expected = "Enter";
			}
			assertEquals(expected, portal.getAction());
			GameObject object = mock(GameObject.class);
			try (MockedStatic<Rs2GameObject> objects = mockStatic(Rs2GameObject.class))
			{
				objects.when(() -> Rs2GameObject.getGameObject(portal.getObjectIds())).thenReturn(object);
				objects.when(() -> Rs2GameObject.interact(object, expected)).thenReturn(true);
				assertTrue(portal.name(), portal.execute());
				objects.verify(() -> Rs2GameObject.interact(object, expected));
			}
		}
	}
}
