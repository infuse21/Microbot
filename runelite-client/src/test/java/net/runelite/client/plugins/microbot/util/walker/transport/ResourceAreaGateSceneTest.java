package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class ResourceAreaGateSceneTest
{
	@Test
	public void sceneScopesThePaymentToThePendingGateAndRechecksBeforeDispatch()
	{
		Transport row = Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(candidate -> candidate.getObjectId() == 26760 && candidate.getCurrencyAmount() == 7500)
			.findFirst().orElseThrow(AssertionError::new);
		Client client = mock(Client.class);
		ClientThread thread = mock(ClientThread.class);
		when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
			Optional.ofNullable(((Callable<?>) call.getArgument(0)).call()));
		Widget menu = mock(Widget.class);
		Widget title = mock(Widget.class);
		Widget yes = mock(Widget.class);
		Widget no = mock(Widget.class);
		when(client.getWidget(InterfaceID.DIALOG_OPTION, 1)).thenReturn(menu);
		when(menu.getDynamicChildren()).thenReturn(new Widget[]{title, yes, no});
		when(title.getText()).thenReturn("Pay 7500 coins to enter?");
		when(yes.getText()).thenReturn("Yes");
		when(no.getText()).thenReturn("No");
		try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
			MockedStatic<Rs2PathApi> path = mockStatic(Rs2PathApi.class);
			MockedStatic<Rs2Player> player = mockStatic(Rs2Player.class);
			MockedStatic<Rs2Inventory> inventory = mockStatic(Rs2Inventory.class);
			MockedStatic<Rs2Dialogue> dialogue = mockStatic(Rs2Dialogue.class))
		{
			microbot.when(Microbot::getClient).thenReturn(client);
			microbot.when(Microbot::getClientThread).thenReturn(thread);
			path.when(Rs2PathApi::getTransports).thenReturn(Map.of(row.getOrigin(), Set.of(row)));
			player.when(Rs2Player::getWorldLocation).thenReturn(row.getOrigin());
			inventory.when(() -> Rs2Inventory.itemQuantity(ItemID.COINS)).thenReturn(7500);
			dialogue.when(Rs2Dialogue::hasSelectAnOption).thenReturn(true);
			dialogue.when(Rs2Dialogue::getDialogueOptions).thenReturn(List.of(yes, no));
			dialogue.when(() -> Rs2Dialogue.keyPressForDialogueOption(1)).thenReturn(true);
			PlannedEdge edge = new PlannedEdge(row.getOrigin(), row.getDestination());
			Rs2CatalogTransitionScene scene = new Rs2CatalogTransitionScene();
			assertNull(scene.observe(edge, null));
			CatalogTransition confirmation = scene.observe(edge, "Open");
			assertNotNull(confirmation);
			assertEquals(ResourceAreaGatePolicy.CONFIRM, confirmation.getAction());
			assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
				Rs2CatalogTransitionScene.dispatch(edge, confirmation.getAction(), 26760));
			assertNull(scene.observe(edge, ResourceAreaGatePolicy.CONFIRM));
			when(title.getText()).thenReturn("Pay 6000 coins to enter?");
			assertNull(scene.observe(edge, "Open"));
			assertEquals(Rs2CatalogTransitionScene.DispatchResult.REJECTED,
				Rs2CatalogTransitionScene.dispatch(edge, confirmation.getAction(), 26760));
			when(title.getText()).thenReturn("Pay 7500 coins to enter?");
			inventory.when(() -> Rs2Inventory.itemQuantity(ItemID.COINS)).thenReturn(7499);
			assertNull(scene.observe(edge, "Open"));
			assertEquals(Rs2CatalogTransitionScene.DispatchResult.REJECTED,
				Rs2CatalogTransitionScene.dispatch(edge, confirmation.getAction(), 26760));
			dialogue.verify(() -> Rs2Dialogue.keyPressForDialogueOption(1), times(1));
		}
	}
}
