package net.runelite.client.plugins.microbot.util.mouse;

import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.plugins.microbot.Microbot;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class VirtualMouseProjectedClickTest
{
	@Test
	public void projectionChangedBeforeDispatchCancelsClick()
	{
		Client client = mock(Client.class);
		try (MockedStatic<Microbot> api = mockStatic(Microbot.class))
		{
			api.when(Microbot::getClient).thenReturn(client);
			VirtualMouse mouse = new VirtualMouse();
			AtomicInteger checks = new AtomicInteger();
			assertFalse(mouse.tryClick(new Point(0, 0), () -> checks.incrementAndGet() == 1));
			assertEquals(2, checks.get());
			assertEquals(new Point(-1, -1), mouse.getLastClick());
		}
	}

	@Test
	public void clientThreadCannotRunProjectedClickMotion()
	{
		Client client = mock(Client.class);
		when(client.isClientThread()).thenReturn(true);
		try (MockedStatic<Microbot> api = mockStatic(Microbot.class))
		{
			api.when(Microbot::getClient).thenReturn(client);
			assertFalse(new VirtualMouse().tryClick(new Point(0, 0), () -> true));
		}
	}
}
