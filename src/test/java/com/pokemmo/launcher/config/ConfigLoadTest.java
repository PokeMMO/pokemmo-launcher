package com.pokemmo.launcher.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.pokemmo.launcher.Launcher;
import com.pokemmo.launcher.enums.PokeMMOLocale;
import com.pokemmo.launcher.enums.UpdateChannel;

class ConfigLoadTest
{
	private final int networkThreads = Config.NETWORK_THREADS;
	private final short maxMemory = Config.HARD_MAX_MEMORY_MB;
	private final PokeMMOLocale locale = Config.ACTIVE_LOCALE;
	private final UpdateChannel channel = Config.UPDATE_CHANNEL;

	@BeforeEach
	void enableConfig()
	{
		Launcher.ENABLE_CONFIG = true;
	}

	@AfterEach
	void restoreDefaults()
	{
		Config.NETWORK_THREADS = networkThreads;
		Config.HARD_MAX_MEMORY_MB = maxMemory;
		Config.ACTIVE_LOCALE = locale;
		Config.UPDATE_CHANNEL = channel;
	}

	private static File write(Path dir, String contents) throws IOException
	{
		Path file = dir.resolve("pokemmo-installer.properties");
		Files.writeString(file, contents, StandardCharsets.UTF_8);
		return file.toFile();
	}

	/**
	 * A user who never opens the config dialog has no file to load, so the field initialiser is
	 * the heap ceiling the client is launched with. Read in its own classloader because the other
	 * tests here assign to the field.
	 */
	@Test
	void aRunWithNoConfigFileDefaultsTo512() throws Exception
	{
		String[] classpath = System.getProperty("java.class.path").split(File.pathSeparator);
		URL[] urls = new URL[classpath.length];
		for(int i = 0; i < classpath.length; i++)
		{
			urls[i] = new File(classpath[i]).toURI().toURL();
		}

		// The platform loader as parent keeps java.net.http reachable while still leaving every
		// launcher class to be defined afresh here
		try(URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader()))
		{
			Class<?> config = loader.loadClass("com.pokemmo.launcher.config.Config");

			assertEquals(512, config.getField("HARD_MAX_MEMORY_MB").getShort(null));
		}
	}

	/**
	 * The ceiling must not depend on whether a file happens to exist, so a file that omits the key
	 * has to produce the same value as no file at all.
	 */
	@Test
	void aFileWithoutTheMemoryKeyUsesTheSameDefault(@TempDir Path dir) throws IOException
	{
		Config.HARD_MAX_MEMORY_MB = 800;

		Config.load(write(dir, "network_threads=2\n"));

		assertEquals(512, Config.JOPTS_XMX_VAL_DEFAULT);
		assertEquals(Config.JOPTS_XMX_VAL_DEFAULT, Config.HARD_MAX_MEMORY_MB);
	}

	@Test
	void aMissingFileLeavesTheDefaults(@TempDir Path dir)
	{
		Config.HARD_MAX_MEMORY_MB = 800;

		Config.load(new File(dir.toFile(), "absent.properties"));

		assertEquals(800, Config.HARD_MAX_MEMORY_MB);
	}

	@Test
	void aMalformedFileLeavesTheDefaults(@TempDir Path dir) throws IOException
	{
		Config.HARD_MAX_MEMORY_MB = 800;

		Config.load(write(dir, "max_mem_hard=\\u00zz\n"));

		assertEquals(800, Config.HARD_MAX_MEMORY_MB);
	}

	@Test
	void aMemoryValueOutsideTheAllowedRangeIsClamped(@TempDir Path dir) throws IOException
	{
		Config.load(write(dir, "max_mem_hard=64\n"));
		assertEquals(Config.JOPTS_XMX_VAL_MIN, Config.HARD_MAX_MEMORY_MB);

		Config.load(write(dir, "max_mem_hard=4096\n"));
		assertEquals(Config.JOPTS_XMX_VAL_MAX, Config.HARD_MAX_MEMORY_MB);
	}

	@Test
	void aFileMissingTheOtherKeysStillSetsTheMemory(@TempDir Path dir) throws IOException
	{
		Config.HARD_MAX_MEMORY_MB = Config.JOPTS_XMX_VAL_MIN;

		Config.load(write(dir, "max_mem_hard=768\n"));

		assertEquals(768, Config.HARD_MAX_MEMORY_MB);
	}

	/**
	 * testserver moves the trust root to the test signing key, so a file must not be able to pick
	 * it. Only live is selectable until a launch marks otherwise.
	 */
	@Test
	void aFileCannotSelectAChannelTheLauncherHasNotEnabled(@TempDir Path dir) throws IOException
	{
		Config.UPDATE_CHANNEL = UpdateChannel.live;

		Config.load(write(dir, "launcher_locale=en\nupdate_channel=testserver\n"));

		assertEquals(UpdateChannel.live, Config.UPDATE_CHANNEL);
	}

	@Test
	void aRejectedChannelStillLeavesTheOtherSettingsApplied(@TempDir Path dir) throws IOException
	{
		Config.HARD_MAX_MEMORY_MB = Config.JOPTS_XMX_VAL_MIN;
		Config.NETWORK_THREADS = 4;

		Config.load(write(dir, "launcher_locale=en\nupdate_channel=testserver2\nmax_mem_hard=768\nnetwork_threads=2\n"));

		assertEquals(UpdateChannel.live, Config.UPDATE_CHANNEL);
		assertEquals(768, Config.HARD_MAX_MEMORY_MB);
		assertEquals(2, Config.NETWORK_THREADS);
	}

	@Test
	void aFileCanSelectAChannelTheLauncherHasEnabled(@TempDir Path dir) throws IOException
	{
		Config.UPDATE_CHANNEL = UpdateChannel.live;
		UpdateChannel.pts.setSelectable(true);
		try
		{
			Config.load(write(dir, "launcher_locale=en\nupdate_channel=pts\n"));

			assertEquals(UpdateChannel.pts, Config.UPDATE_CHANNEL);
		}
		finally
		{
			UpdateChannel.pts.setSelectable(false);
		}
	}

	/**
	 * A channel the config dialog offered has to come back on the next launch. The launcher marks
	 * the installed channels selectable before it loads, so the write and the read see the same
	 * set and the shutdown hook cannot overwrite the file with live.
	 */
	@Test
	void aSavedNonLiveChannelSurvivesALoadSaveRoundTrip(@TempDir Path dir)
	{
		File configFile = dir.resolve("pokemmo-installer.properties").toFile();
		UpdateChannel.pts.setSelectable(true);
		try
		{
			Config.UPDATE_CHANNEL = UpdateChannel.pts;
			Config.save(configFile);

			Config.UPDATE_CHANNEL = UpdateChannel.live;
			Config.load(configFile);

			assertEquals(UpdateChannel.pts, Config.UPDATE_CHANNEL);
		}
		finally
		{
			UpdateChannel.pts.setSelectable(false);
		}
	}
}
