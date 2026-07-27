package com.pokemmo.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.config.ConfigTestAccess;
import com.pokemmo.launcher.enums.UpdateChannel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * main() scans baseDir for installed client directories and then reads the config file, and only
 * that order keeps a saved non-live channel. The first test measures both orders against the real
 * methods, the second checks main() still uses the one that works.
 */
class ChannelSelectionOrderTest
{
	private final UpdateChannel channel = Config.UPDATE_CHANNEL;

	@BeforeEach
	void enableConfig()
	{
		Launcher.ENABLE_CONFIG = true;
	}

	@AfterEach
	void restoreDefaults()
	{
		Config.UPDATE_CHANNEL = channel;
		for(UpdateChannel c : UpdateChannel.values())
		{
			c.setSelectable(c == UpdateChannel.live);
		}
	}

	/**
	 * Config.load() applies an update_channel only when that channel is already selectable, so the
	 * scan has to have run first. Marking the channel afterwards does not recover the value: the
	 * launcher is left on live and the Config::save shutdown hook writes live back over the file.
	 */
	@Test
	void onlyScanningBeforeTheLoadKeepsASavedChannel(@TempDir Path dir) throws IOException
	{
		File baseDir = Files.createDirectory(dir.resolve("base")).toFile();
		Files.createDirectory(baseDir.toPath().resolve("pokemmo-client-pts"));
		File configFile = dir.resolve("pokemmo-installer.properties").toFile();
		Files.writeString(configFile.toPath(), "launcher_locale=en\nupdate_channel=pts\n", StandardCharsets.UTF_8);

		Config.UPDATE_CHANNEL = UpdateChannel.live;
		Launcher.markSelectableChannels(baseDir);
		ConfigTestAccess.load(configFile);

		assertEquals(UpdateChannel.pts, Config.UPDATE_CHANNEL, "scan then load applies the saved channel");

		Config.UPDATE_CHANNEL = UpdateChannel.live;
		UpdateChannel.pts.setSelectable(false);
		ConfigTestAccess.load(configFile);

		assertEquals(UpdateChannel.live, Config.UPDATE_CHANNEL, "load then scan drops the saved channel");

		Launcher.markSelectableChannels(baseDir);

		assertEquals(UpdateChannel.live, Config.UPDATE_CHANNEL, "the scan does not re-apply a channel the load already dropped");
	}

	/**
	 * The order the test above measures, checked where it is decided. main() itself cannot be
	 * called from a test: it opens the UI and goes to the network.
	 */
	@Test
	void mainScansForInstalledChannelsBeforeItReadsTheConfig() throws IOException
	{
		Path source = launcherSource();
		assertNotNull(source, "Launcher.java was not found above the working directory");

		String body = Files.readString(source, StandardCharsets.UTF_8);
		int start = body.indexOf("public static void main(");
		assertTrue(start >= 0, "main() was not found in Launcher.java");
		body = body.substring(start);

		int scan = body.indexOf("launcher.resolveBaseDir();");
		int load = body.indexOf("Config.load();");
		assertTrue(scan >= 0, "main() no longer calls launcher.resolveBaseDir()");
		assertTrue(load >= 0, "main() no longer calls Config.load()");
		assertEquals(scan, body.lastIndexOf("launcher.resolveBaseDir();"), "main() calls resolveBaseDir() once");
		assertEquals(load, body.lastIndexOf("Config.load();"), "main() calls Config.load() once");

		assertTrue(scan < load, "main() must mark the installed channels before Config.load() reads the file, or a saved non-live channel is dropped and overwritten with live");
	}

	private static Path launcherSource()
	{
		for(Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent())
		{
			Path source = dir.resolve("src/main/java/com/pokemmo/launcher/Launcher.java");
			if(Files.exists(source))
				return source;
		}
		return null;
	}
}
