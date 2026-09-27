package net.runelite.client.plugins.microbot.util.magic;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Rs2TomeTest
{
	@Test
	public void byItemIdIsPublicPluginApi() throws NoSuchMethodException
	{
		assertTrue(Modifier.isPublic(Rs2Tome.class.getDeclaredMethod("byItemId", int.class).getModifiers()));
	}

	@Test
	public void byItemIdReturnsKnownTomesAndNoneForUnknownItems()
	{
		assertEquals(Rs2Tome.TOME_OF_FIRE, Rs2Tome.byItemId(ItemID.TOME_OF_FIRE));
		assertEquals(Rs2Tome.NONE, Rs2Tome.byItemId(-1));
	}
}
