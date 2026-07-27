package com.pokemmo.launcher.updater;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedManagerDeclaresTest
{
	@AfterEach
	void clearFeed()
	{
		FeedManager.getFiles().clear();
	}

	@Test
	void findsAnExactMatch()
	{
		FeedManager.getFiles().add(new UpdateFile("PokeMMO.exe", "aa", 10, false));
		FeedManager.getFiles().add(new UpdateFile("bin/linux/x64/PokeMMO", "bb", 10, false));

		assertTrue(FeedManager.declares("bin/linux/x64/PokeMMO"));
		assertTrue(FeedManager.declares("PokeMMO.exe"));
	}

	@Test
	void rejectsAPathTheFeedDoesNotList()
	{
		FeedManager.getFiles().add(new UpdateFile("PokeMMO.exe", "aa", 10, false));

		assertFalse(FeedManager.declares("bin/linux/x64/PokeMMO"),
				"a binary the feed does not list has no hash to check it against");
	}

	@Test
	void normalisesWindowsSeparatorsBeforeComparing()
	{
		FeedManager.getFiles().add(new UpdateFile("bin\\windows\\arm64\\PokeMMO.exe", "aa", 10, false));

		assertTrue(FeedManager.declares("bin\\windows\\arm64\\PokeMMO.exe"));
		assertTrue(FeedManager.declares("bin/windows/arm64/PokeMMO.exe"));
	}

	@Test
	void anEmptyFeedMatchesNothing()
	{
		assertFalse(FeedManager.declares("PokeMMO.exe"));
	}

	@Test
	void rejectsAnOnlyIfNotExistsEntryEvenWhenTheNameMatches()
	{
		FeedManager.getFiles().add(new UpdateFile("PokeMMO.sh", "aa", 10, true));

		assertFalse(FeedManager.declares("PokeMMO.sh"),
				"an only_if_not_exists entry is skipped once its target exists, so its bytes are never hashed");
	}
}
