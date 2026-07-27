package com.pokemmo.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.pokemmo.launcher.enums.UpdateChannel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LauncherSelectableChannelsTest
{
	@AfterEach
	void restoreDefaults()
	{
		for(UpdateChannel channel : UpdateChannel.values())
		{
			channel.setSelectable(channel == UpdateChannel.live);
		}
	}

	/**
	 * The scan is what puts a channel in the config dialog's list, and it has to run before
	 * {@code Config.load()}, which drops an update_channel that is not selectable yet.
	 */
	@Test
	void anInstalledChannelBecomesSelectable(@TempDir Path baseDir) throws IOException
	{
		assertFalse(UpdateChannel.pts.isSelectable(), "only live is selectable before the scan");
		Files.createDirectory(baseDir.resolve("pokemmo-client-pts"));

		Launcher.markSelectableChannels(baseDir.toFile());

		assertTrue(UpdateChannel.pts.isSelectable());
		assertArrayEquals(new UpdateChannel[]{UpdateChannel.live, UpdateChannel.pts},
				UpdateChannel.ENABLED_UPDATE_CHANNELS);
	}

	@Test
	void aChannelWithNoClientDirectoryStaysUnselectable(@TempDir Path baseDir)
	{
		Launcher.markSelectableChannels(baseDir.toFile());

		assertFalse(UpdateChannel.pts.isSelectable());
		assertFalse(UpdateChannel.testserver.isSelectable());
		assertFalse(UpdateChannel.testserver2.isSelectable());
	}
}
