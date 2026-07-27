package com.pokemmo.launcher;

import java.io.File;
import java.nio.file.Path;

import com.pokemmo.launcher.updater.FeedManager;
import com.pokemmo.launcher.updater.UpdateFile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The feed gate in start() has to name the file that is actually executed, which differs between
 * the legacy jar branch and the native branch.
 */
class LaunchTargetTest
{
	@TempDir
	Path dir;

	@AfterEach
	void clearFeed()
	{
		FeedManager.getFiles().clear();
	}

	@Test
	void theLegacyBranchNamesTheJar()
	{
		File nativeBinary = new File(dir.toFile(), path("bin", "linux", "x64", "PokeMMO"));

		assertEquals("PokeMMO.exe", Launcher.launchTargetName(dir.toFile(), nativeBinary, true));
	}

	@Test
	void theNativeBranchNamesThePlatformBinary()
	{
		File nativeBinary = new File(dir.toFile(), path("bin", "linux", "x64", "PokeMMO"));

		assertEquals(path("bin", "linux", "x64", "PokeMMO"),
				Launcher.launchTargetName(dir.toFile(), nativeBinary, false));
	}

	@Test
	void windowsX64NamesTheSameFileOnEitherBranch()
	{
		File rootExe = new File(dir.toFile(), "PokeMMO.exe");

		assertEquals("PokeMMO.exe", Launcher.launchTargetName(dir.toFile(), rootExe, true));
		assertEquals("PokeMMO.exe", Launcher.launchTargetName(dir.toFile(), rootExe, false));
	}

	/**
	 * No live feed entry ships a native client binary, so this is the branch every user takes and
	 * the one the gate has to accept.
	 */
	@Test
	void aFeedDeclaringOnlyTheJarAcceptsTheLegacyBranch()
	{
		FeedManager.getFiles().add(new UpdateFile("PokeMMO.exe", "aa", 10, false));
		File nativeBinary = new File(dir.toFile(), path("bin", "linux", "x64", "PokeMMO"));

		assertTrue(FeedManager.declares(Launcher.launchTargetName(dir.toFile(), nativeBinary, true)));
		assertFalse(FeedManager.declares(Launcher.launchTargetName(dir.toFile(), nativeBinary, false)),
				"a binary the feed does not list has no hash to check it against");
	}

	@Test
	void aFeedDeclaringTheBinaryAcceptsTheNativeBranch()
	{
		FeedManager.getFiles().add(new UpdateFile("bin/linux/x64/PokeMMO", "aa", 10, false));
		File nativeBinary = new File(dir.toFile(), path("bin", "linux", "x64", "PokeMMO"));

		assertTrue(FeedManager.declares(Launcher.launchTargetName(dir.toFile(), nativeBinary, false)));
	}

	private static String path(String... parts)
	{
		return String.join(File.separator, parts);
	}
}
