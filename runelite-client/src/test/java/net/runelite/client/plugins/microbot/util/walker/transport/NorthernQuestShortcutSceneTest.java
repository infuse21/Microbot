package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2LiveScene;
import org.junit.Test;
import org.mockito.MockedStatic;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NorthernQuestShortcutSceneTest
{
	@Test
	public void allDirectionsRecheckExactObjectsUnlocksAndAscentRequirements()
	{
		for (Transport row : NorthernQuestShortcutTest.rows())
		{
			NorthernQuestShortcutPolicy.Entry entry = NorthernQuestShortcutPolicy.entry(row);
			Client client = mock(Client.class);
			Player actor = mock(Player.class);
			WorldView world = mock(WorldView.class);
			when(client.getLocalPlayer()).thenReturn(actor);
			when(actor.getWorldView()).thenReturn(world);
			ClientThread thread = mock(ClientThread.class);
			when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
				Optional.ofNullable(((Callable<?>) call.getArgument(0)).call()));
			when(thread.invoke(any(Supplier.class))).thenAnswer(call -> ((Supplier<?>) call.getArgument(0)).get());
			Rs2TileObjectModel object = mock(Rs2TileObjectModel.class);
			when(object.getId()).thenReturn(entry.id);
			when(object.getWorldLocation()).thenReturn(entry.anchor);
			when(object.getWorldView()).thenReturn(world);
			ObjectComposition definition = mock(ObjectComposition.class);
			when(object.getObjectComposition()).thenReturn(definition);
			when(definition.getName()).thenReturn(entry.name);
			when(definition.getActions()).thenReturn(new String[]{entry.action});
			when(object.click(entry.action)).thenReturn(true);
			Rs2TileObjectCache cache = mock(Rs2TileObjectCache.class);
			when(cache.query()).thenCallRealMethod();
			when(cache.getStream()).thenAnswer(call -> List.of(object).stream());
			try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
				MockedStatic<Rs2PathApi> path = mockStatic(Rs2PathApi.class);
				MockedStatic<Rs2Player> player = mockStatic(Rs2Player.class);
				MockedStatic<Rs2LiveScene> live = mockStatic(Rs2LiveScene.class))
			{
				microbot.when(Microbot::getClient).thenReturn(client);
				microbot.when(Microbot::getClientThread).thenReturn(thread);
				microbot.when(Microbot::getRs2TileObjectCache).thenReturn(cache);
				microbot.when(() -> Microbot.getVarbitValue(entry.varbit)).thenReturn(entry.threshold + 1);
				path.when(Rs2PathApi::getTransports).thenReturn(Map.of(entry.from, Set.of(row)));
				player.when(Rs2Player::getWorldLocation).thenReturn(entry.from);
				player.when(() -> Rs2Player.getBoostedSkillLevel(Skill.AGILITY)).thenReturn(entry.ascending ? 68 : 1);
				player.when(() -> Rs2Player.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(entry.ascending ? 16 : 1);
				PlannedEdge edge = new PlannedEdge(entry.from, entry.to);
				Rs2CatalogTransitionScene scene = new Rs2CatalogTransitionScene();
				assertSame(object, scene.find(edge).getObject());
				when(object.getId()).thenReturn(entry.liveId);
				assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
					Rs2CatalogTransitionScene.dispatch(edge, entry.action, entry.id));
				if (entry.ascending)
				{
					player.when(() -> Rs2Player.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(15);
					assertNull(scene.find(edge));
					assertEquals(Rs2CatalogTransitionScene.DispatchResult.REJECTED,
						Rs2CatalogTransitionScene.dispatch(edge, entry.action, entry.id));
					player.when(() -> Rs2Player.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(16);
					player.when(() -> Rs2Player.getBoostedSkillLevel(Skill.AGILITY)).thenReturn(67);
					assertNull(scene.find(edge));
					player.when(() -> Rs2Player.getBoostedSkillLevel(Skill.AGILITY)).thenReturn(68);
				}
				if (entry.varbit != 0)
				{
					microbot.when(() -> Microbot.getVarbitValue(entry.varbit)).thenReturn(entry.threshold);
					assertNull(scene.find(edge));
					microbot.when(() -> Microbot.getVarbitValue(entry.varbit)).thenReturn(entry.threshold + 1);
				}
				when(object.getId()).thenReturn(999);
				assertNull(scene.find(edge));
				when(object.getId()).thenReturn(entry.id);
				when(definition.getActions()).thenReturn(new String[]{"Examine"});
				assertNull(scene.find(edge));
				when(definition.getActions()).thenReturn(new String[]{entry.action});
				when(object.getWorldLocation()).thenReturn(entry.to);
				assertNull(scene.find(edge));
				verify(object, times(1)).click(entry.action);
			}
		}
	}
}
