package net.runelite.client.plugins.microbot.util.magic.api;

import net.runelite.client.plugins.microbot.util.magic.Rs2Staff;
import net.runelite.client.plugins.microbot.util.magic.Rs2Tome;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.RuneFilter;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class MagicRuneProviderTest
{
	@Test
	public void equippedChargedStaffDoesNotBecomeInfinite()
	{
		Rs2ItemModel item = mock(Rs2ItemModel.class);
		when(item.getId()).thenReturn(ItemID.NATURE_STAFF_CHARGED);
		try (MockedStatic<Rs2Equipment> equipment = mockStatic(Rs2Equipment.class))
		{
			equipment.when(() -> Rs2Equipment.get(EquipmentInventorySlot.WEAPON)).thenReturn(item);
			RuneFilter filter = RuneFilter.builder().includeInventory(false).includeRunePouch(false).build();
			assertEquals(Integer.valueOf(1), Rs2Magic.getRunes(filter).get(Runes.NATURE));
		}
	}

	@Test
	public void namedCastUsesExactSpellbookWidgetAndRejectsClientThread()
	{
		Client client = mock(Client.class);
		String priorStatus = Microbot.status;
		try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
			MockedStatic<Rs2Tab> tabs = mockStatic(Rs2Tab.class);
			MockedStatic<Rs2Widget> widgets = mockStatic(Rs2Widget.class))
		{
			microbot.when(Microbot::getClient).thenReturn(client);
			tabs.when(Rs2Tab::getCurrentTab).thenReturn(InterfaceTab.MAGIC);
			widgets.when(() -> Rs2Widget.clickWidget("Home Teleport", Optional.of(218), 3, true))
				.thenReturn(true);
			assertTrue(Rs2Magic.quickCast("Home Teleport"));
			widgets.verify(() -> Rs2Widget.clickWidget("Home Teleport", Optional.of(218), 3, true));
			widgets.clearInvocations();
			when(client.isClientThread()).thenReturn(true);
			assertFalse(Rs2Magic.quickCast("Home Teleport"));
			assertFalse(Rs2Magic.quickCast((String) null));
			assertFalse(Rs2Magic.quickCast(" "));
			widgets.verifyNoInteractions();
		}
		finally
		{
			Microbot.status = priorStatus;
		}
	}

	@Test
	public void providerIdsIncludeNewStaffsAndRequireBothComboElements()
	{
		Set<Integer> dust = Rs2Staff.itemIdsProviding(Runes.DUST);
		assertTrue(dust.contains(ItemID.DUST_BATTLESTAFF));
		assertTrue(dust.contains(ItemID.SHADOWFLAME_QUADRANT));
		assertFalse(dust.contains(ItemID.STAFF_OF_AIR));
		assertEquals(Set.of(ItemID.NATURE_STAFF_CHARGED), Rs2Staff.itemIdsProviding(Runes.NATURE));
		assertEquals(Set.of(ItemID.TOME_OF_FIRE), Rs2Tome.itemIdsProviding(Runes.FIRE));
		assertTrue(Rs2Tome.itemIdsProviding(Runes.LAVA).isEmpty());
		assertTrue(Rs2Staff.itemIdsProviding(null).isEmpty());
		assertTrue(Rs2Tome.itemIdsProviding(null).isEmpty());
		assertFalse(Rs2Staff.BRYOPHYTAS_STAFF.isInfiniteSupply());
		assertTrue(Rs2Staff.SHADOWFLAME_QUADRANT.isInfiniteSupply());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void providerIdsCannotBeModified()
	{
		Rs2Staff.itemIdsProviding(Runes.AIR).clear();
	}

	@Test
	public void staffProvidesBothElementsForCombinationRunes()
	{
		assertTrue(Rs2Staff.DUST_BATTLESTAFF.provides(Runes.AIR));
		assertTrue(Rs2Staff.DUST_BATTLESTAFF.provides(Runes.EARTH));
		assertTrue(Rs2Staff.DUST_BATTLESTAFF.provides(Runes.DUST));
		assertFalse(Rs2Staff.STAFF_OF_AIR.provides(Runes.DUST));
		assertFalse(Rs2Staff.DUST_BATTLESTAFF.provides(Runes.LAW));
		assertFalse(Rs2Staff.DUST_BATTLESTAFF.provides(null));
		assertFalse(Rs2Staff.NONE.provides(Runes.AIR));
	}

	@Test
	public void tomeProvidesOnlyItsSupportedElement()
	{
		assertTrue(Rs2Tome.TOME_OF_FIRE.provides(Runes.FIRE));
		assertFalse(Rs2Tome.TOME_OF_FIRE.provides(Runes.WATER));
		assertFalse(Rs2Tome.TOME_OF_FIRE.provides(Runes.LAVA));
		assertFalse(Rs2Tome.TOME_OF_FIRE.provides(null));
		assertFalse(Rs2Tome.NONE.provides(Runes.FIRE));
	}
}
