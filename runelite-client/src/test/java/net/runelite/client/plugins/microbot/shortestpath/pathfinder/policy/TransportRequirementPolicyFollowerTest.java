package net.runelite.client.plugins.microbot.shortestpath.pathfinder.policy;

import net.runelite.api.NPC;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class TransportRequirementPolicyFollowerTest
{
	@Test
	public void whirlpoolFollowerGateFailsClosedWithoutMutatingFollower()
	{
		assertTrue(TransportRequirementPolicy.noFollower(null));
		assertFalse(TransportRequirementPolicy.noFollower(mock(NPC.class)));
	}
}
