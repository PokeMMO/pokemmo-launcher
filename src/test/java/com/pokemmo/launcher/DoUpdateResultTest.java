package com.pokemmo.launcher;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.github.mizosoft.methanol.Methanol;
import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.ui.LauncherUI;
import com.pokemmo.launcher.updater.FeedManager;
import com.pokemmo.launcher.updater.UpdateFile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * doUpdate() has to report the outcome to its caller. The error dialogue is not enough on its own:
 * showError only queues one and returns, so the update thread runs on whatever the user clicks.
 */
class DoUpdateResultTest
{
	@TempDir
	Path dir;

	private final Methanol previousHttpClient = Launcher.httpClient;

	@AfterEach
	void restore()
	{
		FeedManager.getFiles().clear();
		Launcher.httpClient = previousHttpClient;
	}

	@Test
	void aFileThatCannotBeDownloadedFailsTheUpdate() throws Exception
	{
		Launcher.httpClient = Methanol.newBuilder().build();

		UpdateFile file = new UpdateFile("resources.zip", "00", 10, false);
		// Rejected by the URI parser, so the download fails without contacting a mirror
		file.absoluteUrl = "http://[";
		FeedManager.getFiles().add(file);

		RecordingUI ui = new RecordingUI();
		Launcher launcher = launcher(ui);

		assertFalse(launcher.doUpdate(false), "a file that never arrived cannot be reported as updated");
		assertTrue(ui.errors.contains(Config.getString("error.download_error")),
				"the download must have failed through downloadFile rather than by throwing");
		assertFalse(ui.info.contains("status.check_success"), "a failed update must not claim success");
		assertFalse(launcher.isUpdating());
	}

	@Test
	void anInstallThatAlreadyMatchesTheFeedPasses() throws Exception
	{
		Launcher launcher = launcher(new RecordingUI());

		assertTrue(launcher.doUpdate(false), "nothing left to download means the install matches the feed");
		assertFalse(launcher.isUpdating());
	}

	private Launcher launcher(LauncherUI ui) throws Exception
	{
		Launcher launcher = new Launcher();
		set(launcher, "launcherUI", ui);
		set(launcher, "pokemmoDir", dir.toFile());
		return launcher;
	}

	private static void set(Launcher launcher, String name, Object value) throws Exception
	{
		Field field = Launcher.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(launcher, value);
	}

	/**
	 * Records what the launcher told the user. The exit callbacks are deliberately not run, which
	 * is what a real dialogue does until the user dismisses it.
	 */
	private static final class RecordingUI implements LauncherUI
	{
		final List<String> errors = new ArrayList<>();
		final List<String> info = new ArrayList<>();

		@Override
		public void setStatus(String key, int progress, Object... params)
		{
		}

		@Override
		public void addDetail(String string, int progress, Object... params)
		{
		}

		@Override
		public void updateProgress(int progress)
		{
		}

		@Override
		public void updateDLSpeed(long bytes_per_second)
		{
		}

		@Override
		public void showMessage(String message, String windowTitle)
		{
		}

		@Override
		public void showMessage(String message, String windowTitle, Runnable runnable)
		{
		}

		@Override
		public void showError(String message, String windowTitle)
		{
			errors.add(message);
		}

		@Override
		public void showError(String message, String windowTitle, Runnable runnable)
		{
			errors.add(message);
		}

		@Override
		public void showErrorWithStacktrace(String message, String windowTitle, String stacktrace, Runnable runnable)
		{
			errors.add(message);
		}

		@Override
		public void showErrorWithStacktrace(String message, String windowTitle, Throwable throwable, Runnable runnable)
		{
			errors.add(message);
		}

		@Override
		public void showErrorWithStacktrace(String message, String windowTitle, Throwable[] throwables, Runnable runnable)
		{
			errors.add(message);
		}

		@Override
		public void showInfo(String message, Object... params)
		{
			info.add(message);
		}

		@Override
		public boolean showYesNoDialogue(String message, String windowTitle)
		{
			return false;
		}

		@Override
		public void setCanStart()
		{
		}

		@Override
		public void setVisible(boolean visible)
		{
		}

		@Override
		public void dispose()
		{
		}

		@Override
		public void createUpdaterWorker(boolean repair)
		{
		}

		@Override
		public void exec(Runnable runnable)
		{
			runnable.run();
		}

		@Override
		public void schedule(int delayMs, Runnable runnable)
		{
			runnable.run();
		}
	}
}
