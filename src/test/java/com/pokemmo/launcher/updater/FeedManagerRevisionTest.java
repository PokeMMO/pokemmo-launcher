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
	void aRevisionBelowTheFloorIsAPlainUpdate()
	{
		assertFalse(FeedManager.needsRepair(31000, 31914),
				"an out of date install is what a plain update exists to fix, and must keep its caches");
	}

	@Test
	void aRevisionAtTheFloorNeedsRepair()
	{
		assertTrue(FeedManager.needsRepair(31914, 31914),
				"the install declares itself current yet failed verification, so its files disagree with revision.txt");
	}

	@Test
	void aRevisionAboveTheFloorNeedsRepair()
	{
		assertTrue(FeedManager.needsRepair(31915, 31914));
	}

	@Test
	void anUnsetFloorNeverForcesRepairForAReadableRevision()
	{
		assertFalse(FeedManager.needsRepair(31914, 0),
				"a feed with no min_revision element publishes no floor to compare against");
	}
}
