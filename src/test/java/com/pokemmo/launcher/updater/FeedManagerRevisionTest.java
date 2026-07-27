package com.pokemmo.launcher.updater;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedManagerRevisionTest
{
	@Test
	void anUnreadableRevisionNeedsRepair()
	{
		assertTrue(FeedManager.needsRepair(-1, 31914));
		assertTrue(FeedManager.needsRepair(0, 31914));
	}

	@Test
	void aRevisionAtTheFloorIsHealthy()
	{
		assertFalse(FeedManager.needsRepair(31914, 31914),
				"a client that is exactly current must not be forced through repair");
	}

	@Test
	void aRevisionAboveTheFloorIsHealthy()
	{
		assertFalse(FeedManager.needsRepair(31915, 31914));
	}

	@Test
	void aRevisionBelowTheFloorNeedsRepair()
	{
		assertTrue(FeedManager.needsRepair(31000, 31914));
	}

	@Test
	void anUnsetFloorNeverForcesRepairForAReadableRevision()
	{
		assertFalse(FeedManager.needsRepair(31914, 0),
				"a feed with no min_revision element publishes no floor to fall below");
	}
}
