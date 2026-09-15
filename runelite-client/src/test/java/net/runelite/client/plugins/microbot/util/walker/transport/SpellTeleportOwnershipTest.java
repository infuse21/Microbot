package net.runelite.client.plugins.microbot.util.walker.transport;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.PohPanel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.mouse.Mouse;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.navigation.*;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SpellTeleportOwnershipTest
{
	private static final WorldPoint FROM = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint TO = new WorldPoint(3213, 3424, 0);

	@Test
	public void tabOpeningAndCastingAreSeparateCommandsAndLandingRetainsOwnership()
	{
		try (Fixture f = new Fixture())
		{
			NavigationEngine engine = engine();
			RouteInteraction open = f.scan();
			assertEquals("spell-prepare:open:Varrock Teleport", open.getAction());
			f.issue(engine, open, 1);
			assertEquals(NavigationDecision.Type.WAIT, engine.observe(observation(2, f.pending(open), FROM)).getType());
			f.showSpell();
			RouteInteraction cast = f.pending(open);
			assertEquals("Varrock Teleport", cast.getAction());
			f.issue(engine, cast, 3);
			f.tab.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.MAGIC)).thenReturn(false);
			f.paths.when(Rs2PathApi::getTransports).thenReturn(Map.of());
			assertEquals(cast.getAction(), f.pending(cast).getAction());
			assertEquals(NavigationDecision.Type.WAIT, engine.observe(observation(4, f.pending(cast), FROM)).getType());
			assertEquals(RouteInteraction.Status.CLEARED, f.scanner.observePending(cast, TO, f.scene).getStatus());
			f.microbot.verify(() -> Microbot.doInvoke(any(), any(Rectangle.class)), times(1));
			verify(f.client, times(1)).runScript(915, InterfaceTab.MAGIC.getVarcIntIndex());
			f.magic.verifyNoInteractions();
		}
	}

	@Test
	public void missingTabAcknowledgementTimesOutWithoutAnotherInput()
	{
		try (Fixture f = new Fixture())
		{
			NavigationEngine engine = engine();
			RouteInteraction open = f.scan();
			f.issue(engine, open, 1);
			assertEquals(NavigationDecision.Type.WAIT, engine.observe(observation(3999, f.pending(open), FROM)).getType());
			NavigationDecision timeout = engine.observe(observation(4001, f.pending(open), FROM));
			assertEquals(NavigationDecision.Type.REQUEST_REPLAN, timeout.getType());
			assertEquals("spell-preparation-not-acknowledged", timeout.getReason());
			verify(f.client, times(1)).runScript(915, InterfaceTab.MAGIC.getVarcIntIndex());
		}
	}

	@Test
	public void staleCastAndInterruptedPreparationDoNotIssueInput()
	{
		try (Fixture f = new Fixture())
		{
			f.showSpell();
			RouteInteraction cast = f.scan();
			f.tab.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.MAGIC)).thenReturn(false);
			assertFalse(Rs2SpellTeleportScene.dispatch(cast));
			RouteInteraction open = f.scan();
			try
			{
				Thread.currentThread().interrupt();
				assertFalse(Rs2SpellTeleportScene.dispatch(open));
			}
			finally { Thread.interrupted(); }
			verify(f.client, never()).runScript(anyInt(), any());
			f.microbot.verify(() -> Microbot.doInvoke(any(), any(Rectangle.class)), never());
		}
	}

	@Test
	public void wrongSpellbookCannotAuthorizeTheSameSprite()
	{
		try (Fixture f = new Fixture())
		{
			f.showSpell();
			RouteInteraction cast = f.scan();
			when(f.client.getVarbitValue(net.runelite.api.gameval.VarbitID.SPELLBOOK)).thenReturn(1);
			assertFalse(Rs2SpellTeleportScene.dispatch(cast));
			assertEquals(RouteInteraction.Status.UNAVAILABLE, f.scan().getStatus());
			f.microbot.verify(() -> Microbot.doInvoke(any(), any(Rectangle.class)), never());
		}
	}

	@Test
	public void teleportFilterAndClosingItsPageAreSeparateObservedStages()
	{
		try (Fixture f = new Fixture())
		{
			f.tab.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.MAGIC)).thenReturn(true);
			when(f.client.getVarbitValue(6609)).thenReturn(1);
			f.page(f.entry("Filters", -1));
			RouteInteraction open = f.scan();
			assertEquals("spell-prepare:open-filters:Varrock Teleport", open.getAction());
			assertTrue(Rs2SpellTeleportScene.dispatch(open));
			f.page(f.entry("Filters", -1), f.entry("Show Teleport spells", -1));
			RouteInteraction toggle = f.pending(open);
			assertTrue(toggle.getAction().contains("show-teleports:"));
			assertTrue(Rs2SpellTeleportScene.dispatch(toggle));
			when(f.client.getVarbitValue(6609)).thenReturn(0);
			RouteInteraction close = f.pending(toggle);
			assertTrue(close.getAction().contains("close-filters:"));
			assertTrue(Rs2SpellTeleportScene.dispatch(close));
			verify(f.mouse, times(3)).click(any(Rectangle.class));
			f.magic.verifyNoInteractions();
		}
	}

	@Test
	public void hiddenFilterButtonUsesTheObservedMagicTabRatherThanAWaitHelper()
	{
		try (Fixture f = new Fixture())
		{
			f.tab.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.MAGIC)).thenReturn(true);
			when(f.client.getVarbitValue(6609)).thenReturn(1);
			when(f.client.getVarbitValue(net.runelite.api.gameval.VarbitID.MAGIC_SPELLBOOK_HIDEFILTERBUTTON)).thenReturn(1);
			f.page();
			Widget tab = f.entry("", -1);
			when(f.client.getWidget(net.runelite.api.gameval.InterfaceID.Toplevel.STONE6)).thenReturn(tab);
			RouteInteraction enable = f.scan();
			assertEquals("spell-prepare:enable-filters:Varrock Teleport", enable.getAction());
			assertTrue(Rs2SpellTeleportScene.dispatch(enable));
			f.microbot.verify(() -> Microbot.doInvoke(any(), any(Rectangle.class)), times(1));
			f.magic.verifyNoInteractions();
		}
	}

	private static NavigationEngine engine()
	{
		NavigationEngine engine = new NavigationEngine();
		engine.start(new NavigationRequest(1, Set.of(TO), 0,
			new NavigationRouteOptions(true, true, false), "spell-test"));
		return engine;
	}

	private static RoutePlan plan()
	{
		return new RoutePlan(1, 1, FROM, Set.of(TO), List.of(FROM, TO), List.of(FROM, TO), true,
			List.of(new RouteEdge(0, FROM, TO, RouteEdge.Kind.SIMPLE_TELEPORT)));
	}

	private static NavigationObservation observation(long time, RouteInteraction interaction, WorldPoint player)
	{
		return NavigationObservation.route(time, player, plan(), false, false, false, false,
			false, false, "spell-test").withRouteInteraction(interaction);
	}

	private static final class Fixture implements AutoCloseable
	{
		final Client client = mock(Client.class);
		final Mouse mouse = mock(Mouse.class);
		final Rs2SimpleTeleportScene scene = new Rs2SimpleTeleportScene();
		final SimpleTeleportRouteScanner scanner = new SimpleTeleportRouteScanner();
		final MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
		final MockedStatic<Rs2Tab> tab = mockStatic(Rs2Tab.class);
		final MockedStatic<Rs2PathApi> paths = mockStatic(Rs2PathApi.class);
		final MockedStatic<PohPanel> poh = mockStatic(PohPanel.class);
		final MockedStatic<Rs2Magic> magic = mockStatic(Rs2Magic.class);
		Fixture()
		{
			ClientThread thread = mock(ClientThread.class);
			when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
				Optional.ofNullable(((Callable<?>) call.getArgument(0)).call()));
			microbot.when(Microbot::getClientThread).thenReturn(thread);
			microbot.when(Microbot::getClient).thenReturn(client);
			microbot.when(Microbot::getMouse).thenReturn(mouse);
			Transport transport = new Transport(TO, "Varrock Teleport", TransportType.TELEPORTATION_SPELL,
				false, 19, Map.of(Skill.MAGIC, 1));
			paths.when(Rs2PathApi::getTransports).thenReturn(Map.of(FROM, Set.of(transport)));
		}
		Widget entry(String text, int sprite)
		{
			Widget widget = mock(Widget.class);
			when(widget.getText()).thenReturn(text);
			when(widget.getSpriteId()).thenReturn(sprite);
			when(widget.getBounds()).thenReturn(new Rectangle(10, 10, 25, 25));
			when(widget.getId()).thenReturn(218 << 16 | 99);
			when(widget.getIndex()).thenReturn(-1);
			return widget;
		}
		void page(Widget... entries)
		{
			Widget root = mock(Widget.class);
			when(root.getStaticChildren()).thenReturn(entries);
			when(client.getWidget(218, 0)).thenReturn(root);
		}
		void showSpell()
		{
			tab.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.MAGIC)).thenReturn(true);
			page(entry("Varrock Teleport", MagicAction.VARROCK_TELEPORT.getSprite()));
		}
		RouteInteraction scan() { return scanner.scan(plan(), 0, 1, FROM, scene); }
		RouteInteraction pending(RouteInteraction previous) { return scanner.observePending(previous, FROM, scene); }
		void issue(NavigationEngine engine, RouteInteraction interaction, long time)
		{
			NavigationDecision decision = engine.observe(observation(time, interaction, FROM));
			assertEquals(NavigationDecision.Type.INTERACT, decision.getType());
			assertTrue(Rs2SpellTeleportScene.dispatch(decision.getInteraction()));
			engine.recordCommandResult(decision, true, time);
		}
		@Override public void close()
		{
			magic.close(); poh.close(); paths.close(); tab.close(); microbot.close();
		}
	}
}
