package com.pokemmo.launcher;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.ProgressTracker;
import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.enums.Arch;
import com.pokemmo.launcher.enums.OS;
import com.pokemmo.launcher.enums.PokeMMOLocale;
import com.pokemmo.launcher.enums.SandboxType;
import com.pokemmo.launcher.enums.UpdateChannel;
import com.pokemmo.launcher.ui.LauncherUI;
import com.pokemmo.launcher.updater.FeedManager;
import com.pokemmo.launcher.updater.UpdateFile;
import com.pokemmo.launcher.util.JREUtil;
import com.pokemmo.launcher.util.Util;

/**
 * PokeMMO Launcher / Updater / Installer
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * <p>
 * This program is created as a pairing to the PokeMMO Game Client. PokeMMO's
 * Game Client software is provided with the PokeMMO License. To view this license, visit
 * https://pokemmo.com/tos/
 * <p>
 * This program manages:
 * - Downloads of the PokeMMO game client
 * - Signature verification for the downloaded files
 * - Cache management / storage of the program
 * - Execution of the program
 *
 * @author Kyu <kyu@pokemmo.com>
 * @author Desu <desu@pokemmo.com>
 */
public class Launcher
{
	public static final int INSTALLER_VERSION = 60;
	public static final String INSTALLER_VERSION_CODE = "4.0a-beta2";

	public static final int EXIT_CODE_SUCCESS = 0;
	public static final int EXIT_CODE_NETWORK_FAILURE = 1;
	public static final int EXIT_CODE_IO_FAILURE = 2;
	public static final int EXIT_CODE_UNK_FAILURE = 127;

	/**
	 * Whether to silently start the game client (without bringing this UI to the front)
	 */
	public static boolean ENABLE_HEADLESS_LAUNCH = true;
	public static boolean ENABLE_CONFIG = false;

	private LauncherUI launcherUI;

	/**
	 * User home file. Obtained user.home
	 */
	private File userHome;
	/**
	 * Platform specific baseDirectory
	 */
	private File baseDir;
	/**
	 * The default location of PokeMMO.exe and other files
	 */
	private File pokemmoDir;

	/**
	 * The list of mirrors which have returned invalid results and must be skipped
	 */
	private final Set<Integer> disabledMirrors = new HashSet<>();
	/**
	 * If our PokeMMO client folder was missing
	 */
	private boolean firstRun = false;

	private boolean isLaunching = false;
	private boolean isUpdating = false;
	private ExecutorService networkExecutorService;

	StringWriter stackTraceStringWriter = new StringWriter();
	PrintWriter stackTracePrintWriter = new PrintWriter(stackTraceStringWriter);


	public static Methanol httpClient;
	public static final String httpClientUserAgent = "Mozilla/5.0 (PokeMMO; Launcher v"+ Launcher.INSTALLER_VERSION_CODE+")";

	private void run(boolean repair)
	{
		userHome = new File(System.getProperty("user.home"));
		//Use current working directory if not overridden
		pokemmoDir = new File(".").toPath().toAbsolutePath().normalize().toFile();

		baseDir = pokemmoDir;

		if(SandboxType.get() != null)
		{
			if(SandboxType.get() == SandboxType.MACOS_APP)
			{
				baseDir = new File(userHome, "/Library/Application Support/com.pokeemu.macos");
				pokemmoDir = new File(baseDir, "pokemmo-client-" + Config.UPDATE_CHANNEL.name());
			}

			if(SandboxType.get() == SandboxType.FLATPAK || SandboxType.get() == SandboxType.SNAPCRAFT)
			{
				if(System.getenv("SNAP_USER_COMMON") != null)
					baseDir = new File(System.getenv("SNAP_USER_COMMON"));
				else if(System.getenv("XDG_DATA_HOME") != null)
					baseDir = new File(System.getenv("XDG_DATA_HOME"));
				else
					baseDir = new File(userHome, ".local" + File.separator + "share");

				pokemmoDir = new File(baseDir, "pokemmo-client-" + Config.UPDATE_CHANNEL.name());
			}

			for(UpdateChannel channel : UpdateChannel.values())
			{
				if(new File(baseDir, "pokemmo-client-" + channel.name()).exists())
					channel.setSelectable(true);
			}
		}

		launcherUI = createLauncherUI();
		if(!Launcher.ENABLE_HEADLESS_LAUNCH)
			displayLauncherUI();

		System.out.println("=================================================");
		System.out.println("Running Launcher");
		System.out.println("Channel: " + Config.UPDATE_CHANNEL);
		System.out.println("OS: " + OS.get());
		System.out.println("Arch: " + Arch.get());
		System.out.println("Sandbox Type: " + SandboxType.get());
		System.out.println("=================================================");
		System.out.println("Working Dir: " + System.getProperty("user.dir"));
		System.out.println("Base Dir: " + baseDir.getAbsolutePath());
		System.out.println("User Home Dir: " + userHome.getAbsolutePath());
		System.out.println("PokeMMO Dir: " + pokemmoDir.getAbsolutePath());
		System.out.println("=================================================");

		checkForRunning();
		if(!downloadFeeds())
		{
			launcherUI.enterEventLoop();
			return;
		}

		File revisionFile = new File(pokemmoDir, "revision.txt");
		if(!pokemmoDir.exists() || !revisionFile.exists())
		{
			createPokemmoDir();
			firstRun = true;
		}

		if(!pokemmoDir.isDirectory())
		{
			launcherUI.showError(Config.getString("error.dir_not_dir", pokemmoDir, "DIR_5"), "", () -> System.exit(EXIT_CODE_IO_FAILURE));
			launcherUI.enterEventLoop();
			return;
		}

		if(OS.get() != OS.WINDOWS)
		{
			if(!pokemmoDir.setReadable(true) || !pokemmoDir.setWritable(true) || !pokemmoDir.setExecutable(true))
			{
				launcherUI.showError(Config.getString("error.dir_not_accessible", pokemmoDir, "DIR_2"), "", () -> System.exit(EXIT_CODE_IO_FAILURE));
				launcherUI.enterEventLoop();
				return;
			}
		}

		if(firstRun)
		{
			displayLauncherUI();

			createSymlinkedDirectories();
			launcherUI.createUpdaterWorker(repair);
		}
		else if(!isPokemmoValid())
		{
			displayLauncherUI();

			int revision = -1;
			if(revisionFile.exists() && revisionFile.isFile())
			{
				try
				{
					revision = Integer.parseInt(new String(Files.readAllBytes(revisionFile.toPath())));
				}
				catch(IOException | NumberFormatException e)
				{
					// Don't care
				}
			}

			// If our declared revision is invalid, repair
			launcherUI.createUpdaterWorker(repair || (revision <= 0 || (FeedManager.MIN_REVISION > 0 && revision >= FeedManager.MIN_REVISION)));
		}
		else
		{
			launcherUI.showInfo("status.check_success");
			launcherUI.setStatus("status.ready", 100);
			launcherUI.setCanStart();
		}

		launcherUI.enterEventLoop();
	}

	private LauncherUI createLauncherUI()
	{
		Class<?> clazz = null;
		try
		{
			clazz = Class.forName("com.pokemmo.launcher.ui.swt.MainShell");
		}
		catch(ClassNotFoundException e)
		{

		}
		if(clazz == null)
		{
			try
			{
				clazz = Class.forName("com.pokemmo.launcher.ui.awt.MainFrame");
			}
			catch(ClassNotFoundException e)
			{

			}
		}

		if(clazz == null)
			throw new RuntimeException("Could not find a LauncherUI impl");

		try
		{
			var constructor = clazz.getConstructor(Launcher.class);
			if(constructor == null)
				throw new RuntimeException("Could not find a LauncherUI impl constructor");
			return (LauncherUI) constructor.newInstance(this);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private void displayLauncherUI()
	{
		ENABLE_HEADLESS_LAUNCH = false;
		launcherUI.open();
	}

	public void launchGame()
	{
		if(isLaunching || isUpdating)
			return;

		isLaunching = true;
		try
		{
			start();
		}
		catch(InterruptedException e)
		{
			System.exit(Launcher.EXIT_CODE_UNK_FAILURE);
		}
	}

	private void start() throws InterruptedException
	{
		List<String> args = new ArrayList<>();

		File pokemmoExecutable;
		if(OS.get() == OS.WINDOWS)
		{
			if(Arch.get() == Arch.X64)
				pokemmoExecutable = new File(pokemmoDir, "PokeMMO.exe");
			else
				pokemmoExecutable = new File(pokemmoDir, "bin" + File.separator + OS.get().getName() + File.separator + Arch.get().getName() + File.separator + "PokeMMO.exe");
		}
		else
		{
			pokemmoExecutable = new File(pokemmoDir, "bin" + File.separator + OS.get().getName() + File.separator + Arch.get().getName() + File.separator + "PokeMMO");
		}

		boolean isJava = false;

		//If our native executable doesn't exist or Winx64 check if PokeMMO.exe exists and is a jar
		if(!pokemmoExecutable.exists() || (OS.get() == OS.WINDOWS && Arch.get() == Arch.X64))
			isJava = JREUtil.isPokeMMOJar(new File(pokemmoDir, "PokeMMO.exe"));

		if(isJava)
		{
			System.out.println("Launching legacy java...");
			File java = JREUtil.findJava(pokemmoDir);
			if(java == null || !java.exists() || !java.canExecute())
			{
				launcherUI.showError(Config.getString("error.incompatible_jvm", Config.getString("status.title.failed_startup")), "", () -> System.exit(EXIT_CODE_IO_FAILURE));
				return;
			}

			args.add(java.getAbsolutePath());
			args.add("-XX:+IgnoreUnrecognizedVMOptions");
			args.add("-Dfile.encoding=UTF-8");
			args.add("-XX:+UseZGC");
			args.add("-XX:+UnlockDiagnosticVMOptions");
			args.add("-XX:-UseAESCTRIntrinsics");
			args.add("-XX:-UseAESIntrinsics");
			args.add("-Dfile.encoding=UTF-8");

			if(OS.get() == OS.MAC)
			{
				args.add("-XstartOnFirstThread");
				args.add("-Dorg.lwjgl.system.allocator=system");
				args.add("-Xdock:name=PokeMMO");
			}

			args.addAll(Arrays.asList("-cp", "PokeMMO.exe", "com.pokeemu.client.Client"));
		}
		else
		{
			System.out.println("Launching native PokeMMO...");
			if(!pokemmoExecutable.exists())
			{
				launcherUI.showError(Config.getString("status.failed_startup"), Config.getString("status.title.failed_startup"), () -> System.exit(EXIT_CODE_IO_FAILURE));
				return;
			}
			args.add(pokemmoExecutable.getAbsolutePath());
			args.add("-Xms192M");
			args.add("-Xmx" + Config.HARD_MAX_MEMORY_MB + "M");
		}

		ProcessBuilder pb = new ProcessBuilder(args);
		pb.directory(pokemmoDir);
		pb.inheritIO();

		// Used by KDE to xdg-portal file dialogues
		pb.environment().put("GTK_USE_PORTALS", "1");

		pb.environment().put("POKEMMO_LAUNCHER_VER", INSTALLER_VERSION_CODE);

		if(SandboxType.get() == SandboxType.MACOS_APP)
		{
			pb.environment().put("POKEMMO_MACOS_LAUNCHER_VER", INSTALLER_VERSION_CODE);
			pb.environment().put("POKEMMO_IS_MACOS_APP", "1");
		}
		else if(SandboxType.get() == SandboxType.SNAPCRAFT)
		{
			pb.environment().put("POKEMMO_IS_SNAPPED", "1");
		}
		else if(SandboxType.get() == SandboxType.FLATPAK)
		{
			pb.environment().put("POKEMMO_IS_FLATPAKED", "1");
		}

		System.out.println("Starting with params " + Arrays.toString(args.toArray(new String[0])));

		try
		{
			pb.start();
		}
		catch(IOException e)
		{
			launcherUI.showErrorWithStacktrace(Config.getString("status.failed_startup"), Config.getString("status.title.failed_startup"), getStacktraceString(e), () -> System.exit(EXIT_CODE_IO_FAILURE));
			return;
		}

		launcherUI.dispose();
		System.exit(0);
	}

	private void checkForRunning()
	{
		/*
		 * It's safe to assume that only one process may use this processes's JRE, and it should be sufficient to query if any other processes are running from the current directory
		 * This is not usable on Windows due to the potential for shared JREs/JDKs, but the approach works on macOS due to the app format and Linux due to Snapcraft / Flatpak isolation
		 */
		ProcessHandle processHandle = ProcessHandle.current();
		ProcessHandle.Info processInfo = processHandle.info();

		if(processInfo.command().isEmpty())
		{
			// Something really bad happened. Our j11 process API doesn't work. Bail out to prevent other issues.
			launcherUI.showErrorWithStacktrace(Config.getString("status.failed_startup"), Config.getString("status.title.failed_startup"), "JPROC_FAIL", () -> System.exit(EXIT_CODE_IO_FAILURE));
			return;
		}

		String launcherPath = processInfo.command().get();

		List<ProcessHandle> destroyables = new ArrayList<>();
		ProcessHandle.allProcesses().filter(ProcessHandle::isAlive).forEach(f ->
		{
			try
			{
				if(f.info().command().isPresent() && f.pid() != processHandle.pid() && f.info().user().isPresent() && f.info().user().equals(processInfo.user()))
				{
					String path = f.info().command().get();

					//If sandboxed, only one process should be using this launcher (legacy java stuff)
					//TODO: Remove after full native-image
					if(SandboxType.get() != SandboxType.NONE && path.equals(launcherPath))
					{
						System.out.println("Found destroyable " + path);
						destroyables.add(f);
						return;
					}

					if(path.startsWith(pokemmoDir.getAbsolutePath()))
					{
						System.out.println("Found destroyable " + path);
						destroyables.add(f);
					}
				}
			}
			catch(Exception e)
			{
				// Skip!
			}
		});

		if(!destroyables.isEmpty())
		{
			if(launcherUI.showYesNoDialogue(Config.getString("status.game_already_running"), ""))
			{
				for(ProcessHandle p : destroyables)
				{
					p.destroyForcibly();
				}
			}
			else
			{
				launcherUI.dispose();
				System.exit(EXIT_CODE_SUCCESS);
			}
		}
	}

	private boolean isPokemmoValid()
	{
		if(System.getenv("POKEMMO_NOVERIFY") != null)
		{
			return true;
		}

		/*
		 * The list of files which MUST be updated before continuing to launch
		 */
		Set<UpdateFile> invalidFiles = new HashSet<>();

		launcherUI.setStatus("status.game_verification", 20);

		for(UpdateFile file : FeedManager.getFiles())
		{
			File f = getFile(file.name);
			if(!file.shouldDownload(f))
			{
				continue;
			}

			String checksum_sha256 = file.sha256;
			String actual_sha256 = Util.calculateHash("SHA-256", f);

			if(!checksum_sha256.equalsIgnoreCase(actual_sha256))
			{
				invalidFiles.add(file);
			}
		}

		return invalidFiles.isEmpty();
	}

	public void doUpdate(boolean repair)
	{
		if(isUpdating)
			return;

		isUpdating = true;

		if(repair)
		{
			launcherUI.setStatus("status.game_repair", 30);
		}
		else
		{
			launcherUI.addDetail("status.title.update_available", 30);
			launcherUI.setStatus("status.game_download", 30);
		}

		networkExecutorService = Executors.newFixedThreadPool(Config.NETWORK_THREADS);

		int total_files = FeedManager.getFiles().size();
		if(total_files < 1)
		{
			total_files = 1;
		}

		int counter = 0;

		List<UpdateFile> to_download = new ArrayList<>();

		for(UpdateFile file : FeedManager.getFiles())
		{
			File f = getFile(file.name);
			if(!file.shouldDownload(f))
			{
				continue;
			}

			if(!f.getParentFile().mkdirs() && !f.getParentFile().exists())
			{
				launcherUI.showError(Config.getString("error.dir_not_accessible", f.getParentFile(), "DIR_8"), "", () -> System.exit(EXIT_CODE_IO_FAILURE));
				return;
			}

			String checksum_sha256 = file.sha256;
			String hash_sha256 = Util.calculateHash("SHA-256", f);

			if(!checksum_sha256.equalsIgnoreCase(hash_sha256))
			{
				if(repair)
				{
					launcherUI.addDetail("status.files.repairing", ((counter * 100) / total_files), file.name);
					System.out.println("Checksum mismatch for " + file.name);
					System.out.println("Wanted SHA256: " + checksum_sha256 + " | Actual: " + hash_sha256);
				}

				to_download.add(file);
			}

			counter++;
		}

		if(to_download.isEmpty())
		{
			launcherUI.setStatus("status.game_verified", 90);
			launcherUI.setStatus("status.ready", 100);
			isUpdating = false;
			return;
		}

		launcherUI.setStatus("status.downloading", 0);

		Phaser phaser = new Phaser(to_download.size() + 1);

		lastSpeedTime = System.currentTimeMillis();
		for(UpdateFile file : to_download)
		{
			networkExecutorService.submit(() ->
			{
				launcherUI.addDetail("status.files.downloading", getProgress(to_download), file.name);

				if(downloadFile(file, (progress -> {
					file.downloadedBytes = progress.totalBytesTransferred();

					launcherUI.updateProgress(getProgress(to_download));
				})))
				{
					phaser.arrive();
				}
				else
				{
					launcherUI.showError(Config.getString("error.download_error"), "", () -> System.exit(EXIT_CODE_NETWORK_FAILURE));
				}
			});
		}

		phaser.arriveAndAwaitAdvance();

		if(repair)
			clearCache();

		networkExecutorService.shutdown();
		isUpdating = false;

		launcherUI.updateDLSpeed(-1);
		launcherUI.showInfo("status.check_success");
		launcherUI.setStatus("status.ready", 100);
	}

	private long lastSpeedTime = 0;
	private long lastSpeedBytes = 0;

	private int getProgress(List<UpdateFile> files)
	{
		long totalBytes = 0;
		long downloadedBytes = 0;

		for(UpdateFile file : files)
		{
			totalBytes += file.size;
			downloadedBytes += file.downloadedBytes;
		}

		int progress = totalBytes < 1 ? 100 : Math.round((downloadedBytes * 100f) / totalBytes);
		if(downloadedBytes >= totalBytes)
			progress = 100;

		long now = System.currentTimeMillis();
		if(now - lastSpeedTime >= 500)
		{
			long bytesThisFrame = downloadedBytes - lastSpeedBytes;
			long bytesPerSecond = (bytesThisFrame * 1000) / (now - lastSpeedTime);

			lastSpeedTime = now;
			lastSpeedBytes = downloadedBytes;

			launcherUI.updateDLSpeed(bytesPerSecond);
		}

		return progress;
	}

	public boolean isUpdating()
	{
		return isUpdating;
	}

	/**
	 * Cancels any running update/download threads and resets the updating state.
	 * Called when the UI window is closed.
	 */
	public void cancelUpdater()
	{
		isUpdating = false;
		if (networkExecutorService != null && !networkExecutorService.isShutdown())
		{
			networkExecutorService.shutdownNow();
		}
	}

	public LauncherUI getLauncherUI()
	{
		return launcherUI;
	}

	private boolean downloadFile(UpdateFile file, ProgressTracker.Listener progressListener)
	{
		String checksum_sha256 = file.sha256;
		String[] mirrors = Config.UPDATE_CHANNEL.getMirrors();
		for(int mirror_index = 0; mirror_index < mirrors.length; mirror_index++)
		{
			if(disabledMirrors.contains(mirror_index))
			{
				continue;
			}

			if(!Util.downloadUrlToFile(httpClient, mirrors[mirror_index] + "/" + Config.UPDATE_CHANNEL.urlComponent() + "/current/client/" + file.name + "?v=" + file.getCacheBuster(), getFile(file.name + ".TEMPORARY"), progressListener))
			{
				launcherUI.showInfo("status.files.failed_download", file.name, mirror_index);
				disabledMirrors.add(mirror_index);
				continue;
			}

			System.out.println("Downloaded new " + file.name + ".TEMPORARY");

			File temporary_file = getFile(file.name + ".TEMPORARY");

			System.out.println("Requested file download to " + temporary_file.getAbsolutePath());

			String new_sha256_hash = Util.calculateHash("SHA-256", temporary_file);
			if(!checksum_sha256.equalsIgnoreCase(new_sha256_hash))
			{
				launcherUI.showInfo(Config.getString("status.files.failed_checksum", file.name, checksum_sha256, new_sha256_hash, mirror_index));

				if(temporary_file.isFile() && temporary_file.exists())
				{
					temporary_file.delete();
				}

				disabledMirrors.add(mirror_index);
				continue;
			}

			if(file.executable)
				temporary_file.setExecutable(true, false);

			File old_file = getFile(file.name);
			if(old_file.isFile() && old_file.exists() && !old_file.delete())
			{
				//This is a fail case, changing mirror will not help.
				launcherUI.showError(Config.getString("status.title.fatal_error", old_file.getPath()), Config.getString("status.title.fatal_error"));
				return false;
			}

			if(!temporary_file.renameTo(old_file))
			{
				//This is a fail case, changing mirror will not help.
				launcherUI.showError(Config.getString("status.title.fatal_error", temporary_file.getPath(), old_file.getPath()), Config.getString("status.title.fatal_error"));
				return false;
			}
			return true;
		}

		return false;
	}

	private void clearCache()
	{
		launcherUI.showInfo("status.delete_caches");
		if(!getFile("PokeMMO.exe").exists())
		{
			return;
		}

		File cache_folder = getFile("cache");
		if(!cache_folder.exists() || (!Files.isSymbolicLink(cache_folder.toPath()) && !cache_folder.isDirectory()))
		{
			return;
		}

		File[] cache_files = cache_folder.listFiles(f ->
		{
			if(f.isDirectory() || !f.isFile())
			{
				return false;
			}

			return f.getName().toLowerCase(Locale.ENGLISH).endsWith(".bin");
		});

		if(cache_files == null)
		{
			return;
		}

		for(File cache_file : cache_files)
		{
			if(cache_file.delete())
			{
				launcherUI.showInfo("status.delete_cache_file", cache_file.getPath());
			}
		}
	}

	private boolean downloadFeeds()
	{
		launcherUI.setStatus("status.networking.load", 0);
		FeedManager.load(launcherUI);

		if(!FeedManager.SUCCESSFUL)
		{
			launcherUI.showErrorWithStacktrace(Config.getString("status.networking.feed_load_failed"), Config.getString("status.title.network_failure"), "UPDATE_FEED_FAILURE_1", () -> System.exit(EXIT_CODE_NETWORK_FAILURE));
			return false;
		}

		if(FeedManager.MIN_LAUNCHER_VERSION > INSTALLER_VERSION)
		{
			launcherUI.showMessage(Config.getString(OS.get() == OS.MAC ? "status.update_available_mac" : "status.update_available"), Config.getString("status.title.update_available"), () ->
			{
				launcherUI.openURL("https://pokemmo.com/downloads/mac/");
				System.exit(EXIT_CODE_UNK_FAILURE);
			});
			return false;
		}
		return true;
	}

	private File getFile(String path)
	{
		return new File(pokemmoDir, path);
	}

	public File getPokemmoDir()
	{
		return pokemmoDir;
	}

	public void createPokemmoDir()
	{
		File f = getPokemmoDir();
		if(!f.mkdirs() && !f.exists())
		{
			launcherUI.showError(Config.getString("error.dir_not_accessible", pokemmoDir, "DIR_1"), "", () -> System.exit(EXIT_CODE_IO_FAILURE));
		}
	}

	public void createSymlinkedDirectories()
	{
		if(OS.get() == OS.MAC)
		{
			File caches = new File(userHome, "Library/Caches/com.pokeemu.macos/pokemmo-client-caches/");

			if(!caches.exists() && !caches.mkdirs())
			{
				launcherUI.showError(Config.getString("error.dir_not_accessible", caches.getPath(), "DIR_3"), "", () -> System.exit(EXIT_CODE_IO_FAILURE));
				return;
			}
			else if(!caches.isDirectory())
			{
				launcherUI.showError(Config.getString("error.dir_not_dir", caches.getPath(), "DIR_6"), "", () -> System.exit(EXIT_CODE_IO_FAILURE));
				return;
			}

			File screenshots = new File(userHome, "Pictures/PokeMMO Screenshots/");

			if(!screenshots.exists() && !screenshots.mkdirs())
			{
				launcherUI.showError(Config.getString("error.dir_not_accessible", screenshots.getPath(), "DIR_4"), "", () -> System.exit(EXIT_CODE_IO_FAILURE));
				return;
			}
			else if(!screenshots.isDirectory())
			{
				launcherUI.showError(Config.getString("error.dir_not_dir", screenshots.getPath(), "DIR_7"), "", () -> System.exit(EXIT_CODE_IO_FAILURE));
				return;
			}

			try
			{
				Files.createSymbolicLink(new File(pokemmoDir, "cache").toPath(), caches.toPath());
				Files.createSymbolicLink(new File(pokemmoDir, "screenshots").toPath(), screenshots.toPath());
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return;
		}

		if(OS.get() == OS.LINUX)
		{
			// Screenshots symlink is only created if XDG_PICTURES_DIR is set. There is no way to predict what the pictures directory is otherwise set to, due to each DE implementing its own (and potentially different languages)
			String xdg_pictures_dir = System.getenv("XDG_PICTURES_DIR");
			if(xdg_pictures_dir != null)
			{
				File xdgPicturesDir = new File(xdg_pictures_dir);
				File screenshotsDir = new File(xdgPicturesDir, "PokeMMO Screenshots");

				if(!screenshotsDir.exists() && screenshotsDir.mkdir())
				{
					try
					{
						Files.createSymbolicLink(new File(pokemmoDir, "screenshots").toPath(), screenshotsDir.toPath());
					}
					catch(IOException e)
					{
						// Something has already set these up
						e.printStackTrace();
					}
				}
			}
			return;
		}
	}

	public String getStacktraceString(Throwable[] t)
	{
		StringBuilder sb = new StringBuilder();
		for(Throwable x : t)
		{
			if(!sb.isEmpty())
				sb.append("\n");

			sb.append(getStacktraceString(x));
		}

		return sb.toString();
	}

	public String getStacktraceString(Throwable t)
	{
		stackTracePrintWriter.flush();
		stackTraceStringWriter.flush();
		t.printStackTrace(stackTracePrintWriter);
		return stackTraceStringWriter.toString();
	}

	public static void main(String[] args)
	{
		var argsList = Arrays.asList(args);
		if(Arrays.asList(args).contains("--force-ui") || Arrays.asList(args).contains("--launch") || SandboxType.get() != SandboxType.NONE)
		{
			ENABLE_CONFIG = true;
			System.out.println("Enabling configuration...");
			Config.load();
			Runtime.getRuntime().addShutdownHook(new Thread(Config::save));
		}

		String httpAuthPassword = "";
		boolean repair = false;

		ArrayDeque<String> queue = new ArrayDeque<>(argsList);
		while(!queue.isEmpty())
		{
			String arg = queue.poll();
			if(arg.equals("--force-ui") || arg.equals("--update"))
			{
				ENABLE_HEADLESS_LAUNCH = false;
				continue;
			}

			if(arg.equals("--auth_password"))
			{
				if(!queue.isEmpty())
				{
					httpAuthPassword = queue.poll();
				}
				continue;
			}

			//Legacy
			if(arg.startsWith("-auth_password:"))
			{
				httpAuthPassword = arg.split(":", 2)[1];
			}

			//Legacy
			if(arg.startsWith("-updater_feeds"))
			{
				try
				{
					String temp = arg.split(":", 2)[1];
					if(!temp.trim().isEmpty())
					{
						String[] updater_feeds = temp.split(",");
						for(String s :  updater_feeds)
						{
							if(s.contains("testserver2"))
								Config.UPDATE_CHANNEL = UpdateChannel.testserver2;
							else if(s.contains("testserver"))
								Config.UPDATE_CHANNEL = UpdateChannel.testserver;
						}
					}
				}
				catch(Exception e)
				{
				}
			}

			if(arg.equals("--channel"))
			{
				if(!queue.isEmpty())
				{
					UpdateChannel channel = UpdateChannel.valueOf(queue.poll());
					Config.UPDATE_CHANNEL = channel;
				}
				continue;
			}

			if(arg.equals("--local") || arg.equals("--locale"))
			{
				if(!queue.isEmpty())
				{
					PokeMMOLocale locale = PokeMMOLocale.getFromString(queue.poll());
					Config.changeLocale(locale);
				}
				continue;
			}

			if(arg.equals("--os"))
			{
				if(!queue.isEmpty())
					OS.CURRENT = OS.getByName(queue.poll());
				continue;
			}

			if(arg.equals("--arch"))
			{
				if(!queue.isEmpty())
					Arch.CURRENT = Arch.getByName(queue.poll());
				continue;
			}

			//Legacy
			if(arg.equals("-pts"))
			{
				Config.UPDATE_CHANNEL = UpdateChannel.pts;
				continue;
			}

			if(arg.equals("-repair:true") || arg.equals("--repair"))
			{
				ENABLE_HEADLESS_LAUNCH = false;
				repair = true;
				continue;
			}
		}

		Methanol.Builder builder = Methanol.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(20));

		if(!httpAuthPassword.isEmpty())
		{
			char[] password = httpAuthPassword.toCharArray();
			builder.authenticator(new Authenticator()
			{
				@Override
				protected PasswordAuthentication getPasswordAuthentication()
				{
					return new PasswordAuthentication("authuser", password);
				}
			});
		}

		httpClient = builder.build();

		new Launcher().run(repair);
	}
}
