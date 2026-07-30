package net.runelite.client.plugins.microbot.api.playerstate;

import java.util.Optional;
import java.util.concurrent.Callable;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Rs2PlayerStateCacheTest
{
	private static final int VARP_ID = 100;
	private static final int VARBIT_ID = 200;

	private Client client;
	private EventBus eventBus;
	private Rs2PlayerStateCache cache;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		ClientThread clientThread = mock(ClientThread.class);
		eventBus = new EventBus();

		// Run client-thread work inline so cache reads behave synchronously in tests
		when(clientThread.runOnClientThreadOptional(any())).thenAnswer(invocation ->
		{
			Callable<?> callable = invocation.getArgument(0);
			return Optional.ofNullable(callable.call());
		});

		cache = new Rs2PlayerStateCache(eventBus, client, clientThread);
	}

	private void setGameState(GameState gameState)
	{
		when(client.getGameState()).thenReturn(gameState);
		GameStateChanged event = new GameStateChanged();
		event.setGameState(gameState);
		eventBus.post(event);
	}

	@Test
	public void varpValueOfZeroIsCachedWhileLoggedIn()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getVarpValue(VARP_ID)).thenReturn(0);

		assertEquals(0, cache.getVarpValue(VARP_ID));
		assertEquals(0, cache.getVarpValue(VARP_ID));

		verify(client, times(1)).getVarpValue(VARP_ID);
	}

	@Test
	public void varbitValueOfZeroIsCachedWhileLoggedIn()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getVarbitValue(VARBIT_ID)).thenReturn(0);

		assertEquals(0, cache.getVarbitValue(VARBIT_ID));
		assertEquals(0, cache.getVarbitValue(VARBIT_ID));

		verify(client, times(1)).getVarbitValue(VARBIT_ID);
	}

	@Test
	public void nonZeroVarpIsCachedWhileLoggedIn()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getVarpValue(VARP_ID)).thenReturn(7);

		assertEquals(7, cache.getVarpValue(VARP_ID));
		assertEquals(7, cache.getVarpValue(VARP_ID));

		verify(client, times(1)).getVarpValue(VARP_ID);
	}

	@Test
	public void nothingIsCachedBeforeLogin()
	{
		when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		when(client.getVarpValue(VARP_ID)).thenReturn(5);
		when(client.getVarbitValue(VARBIT_ID)).thenReturn(5);

		assertEquals(5, cache.getVarpValue(VARP_ID));
		assertEquals(5, cache.getVarbitValue(VARBIT_ID));

		// Values read pre-login must not poison the logged-in cache
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getVarpValue(VARP_ID)).thenReturn(0);
		when(client.getVarbitValue(VARBIT_ID)).thenReturn(0);

		assertEquals(0, cache.getVarpValue(VARP_ID));
		assertEquals(0, cache.getVarbitValue(VARBIT_ID));
	}

	@Test
	public void varbitChangedEventUpdatesCachedValues()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getVarpValue(VARP_ID)).thenReturn(0);
		assertEquals(0, cache.getVarpValue(VARP_ID));

		VarbitChanged event = new VarbitChanged();
		event.setVarpId(VARP_ID);
		event.setValue(3);
		cache.onVarbitChanged(event);

		assertEquals(3, cache.getVarpValue(VARP_ID));
		verify(client, times(1)).getVarpValue(VARP_ID);
	}

	@Test
	public void hoppingFlushesVarbitAndVarpCaches()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getVarpValue(VARP_ID)).thenReturn(4);
		assertEquals(4, cache.getVarpValue(VARP_ID));

		setGameState(GameState.HOPPING);

		// The varp dropped to 0 on the new world; the server does not re-send zero varps
		setGameState(GameState.LOGGED_IN);
		when(client.getVarpValue(VARP_ID)).thenReturn(0);

		assertEquals(0, cache.getVarpValue(VARP_ID));
	}

	@Test
	public void loginScreenFlushesVarbitAndVarpCaches()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getVarbitValue(VARBIT_ID)).thenReturn(9);
		assertEquals(9, cache.getVarbitValue(VARBIT_ID));

		setGameState(GameState.LOGIN_SCREEN);

		setGameState(GameState.LOGGED_IN);
		when(client.getVarbitValue(VARBIT_ID)).thenReturn(0);

		assertEquals(0, cache.getVarbitValue(VARBIT_ID));
	}
}
