package net.runelite.client.plugins.microbot.util.grandexchange;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Rs2GrandExchangeQuantityTest {
    @Test
    public void missingButtonCannotRetryForever() {
        AtomicInteger attempts = new AtomicInteger();
        assertFalse(Rs2GrandExchange.retryQuantity(10, () -> 1, attempts::incrementAndGet));
        assertEquals(3, attempts.get());
    }

    @Test
    public void acceptsSuccessOnLastAttempt() {
        AtomicInteger quantity = new AtomicInteger();
        assertTrue(Rs2GrandExchange.retryQuantity(3, quantity::get, quantity::incrementAndGet));
        assertEquals(3, quantity.get());
    }

    @Test
    public void stopsAsSoonAsQuantityMatches() {
        AtomicInteger attempts = new AtomicInteger();
        assertTrue(Rs2GrandExchange.retryQuantity(1, attempts::get, attempts::incrementAndGet));
        assertEquals(1, attempts.get());
    }

    @Test
    public void matchingQuantityNeedsNoInteraction() {
        AtomicInteger attempts = new AtomicInteger();
        assertTrue(Rs2GrandExchange.retryQuantity(10, () -> 10, attempts::incrementAndGet));
        assertEquals(0, attempts.get());
    }
}
