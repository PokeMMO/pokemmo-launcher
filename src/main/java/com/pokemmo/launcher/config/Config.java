package com.pokemmo.launcher.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import com.pokemmo.launcher.Launcher;
import com.pokemmo.launcher.enums.PokeMMOLocale;
import com.pokemmo.launcher.enums.SandboxType;
import com.pokemmo.launcher.enums.UpdateChannel;
import com.pokemmo.launcher.ui.shared.LocaleAwareElementManager;

/**
 * @author Kyu
 */
public class Config
{
	public static final short JOPTS_XMX_VAL_MIN = 384;
	public static final short JOPTS_XMX_VAL_MAX = 1024;
	/**
	 * The heap ceiling used when nothing sets one. Both the field initialiser below and
	 * {@link #load(File)}'s fallback use it, so the client gets the same limit whether the
	 * configuration file is missing, unreadable or malformed, or simply omits this key.
	 */
	public static final short JOPTS_XMX_VAL_DEFAULT = 512;
	public static final int NETWORK_THREADS_MAX = 4;

	public static int NETWORK_THREADS = 4;

	public static UpdateChannel UPDATE_CHANNEL = UpdateChannel.live;

	public static short HARD_MAX_MEMORY_MB = JOPTS_XMX_VAL_DEFAULT;

	public static PokeMMOLocale ACTIVE_LOCALE = PokeMMOLocale.getDefaultLocale();
	private static ResourceBundle STRINGS = ACTIVE_LOCALE.getStrings();

	private Config()
	{
	}

	public static void load()
	{
		load(getConfigFile());
	}

	/**
	 * Applies a configuration file if one exists, keeping the defaults if it does not.
	 * This is gated on {@link Launcher#ENABLE_CONFIG}. Updater mode should not load configuration.
	 */
	static void load(File configFile)
	{
		if(!Launcher.ENABLE_CONFIG)
			return;

		Properties props = new Properties();
		try
		{
			props.load(new FileReader(configFile));
		}
		catch(IOException | IllegalArgumentException e)
		{
			// Missing, unreadable, or malformed: use the default properties. Properties.load
			// throws IllegalArgumentException on a bad unicode escape.
			return;
		}

		try
		{
			NETWORK_THREADS = Integer.parseInt(props.getProperty("network_threads", "4"));
			if(NETWORK_THREADS < 1)
			{
				NETWORK_THREADS = 1;
			}
			else if(NETWORK_THREADS > NETWORK_THREADS_MAX)
			{
				NETWORK_THREADS = NETWORK_THREADS_MAX;
			}

			HARD_MAX_MEMORY_MB = Short.parseShort(props.getProperty("max_mem_hard", Short.toString(JOPTS_XMX_VAL_DEFAULT)));

			if(HARD_MAX_MEMORY_MB < JOPTS_XMX_VAL_MIN)
			{
				HARD_MAX_MEMORY_MB = JOPTS_XMX_VAL_MIN;
			}
			else if(HARD_MAX_MEMORY_MB > JOPTS_XMX_VAL_MAX)
			{
				HARD_MAX_MEMORY_MB = JOPTS_XMX_VAL_MAX;
			}

			ACTIVE_LOCALE = PokeMMOLocale.getFromString(props.getProperty("launcher_locale"));

			// The channel picks the public key the feeds are verified against, and this applies to
			// every launch, including the ones that never open the config dialog. So a file may
			// only name a channel the launcher has marked selectable, the same set the dialog
			// offers; a hand-written or inherited file is not otherwise bound by that.
			UpdateChannel channel = UpdateChannel.valueOf(props.getProperty("update_channel"));
			if(channel.isSelectable())
			{
				UPDATE_CHANNEL = channel;
			}
			else
			{
				System.out.println("Ignoring update_channel " + channel + ": not selectable");
			}
		}
		catch(Exception e)
		{
			System.out.println("Failed to load configuration file");
		}

		STRINGS = ACTIVE_LOCALE.getStrings();
	}

	public static void save()
	{
		if(!Launcher.ENABLE_CONFIG)
			return;

		File configDir = getConfigHome();
		if(configDir.exists() || configDir.mkdir())
		{
			save(getConfigFile());
		}
		else
		{
			System.out.println("Failed to save configuration for config_dir " + configDir);
		}
	}

	static void save(File configFile)
	{
		Properties props = new Properties();
		props.put("network_threads", Integer.toString(NETWORK_THREADS));
		props.put("update_channel", UPDATE_CHANNEL.toString());
		props.put("max_mem_hard", Short.toString(HARD_MAX_MEMORY_MB));
		props.put("launcher_locale", ACTIVE_LOCALE.getLangTag());

		try
		{
			props.store(new FileWriter(configFile, StandardCharsets.UTF_8), "PokeMMO Launcher v" + Launcher.INSTALLER_VERSION + " Properties");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void changeLocale(PokeMMOLocale target)
	{
		ACTIVE_LOCALE = target;
		STRINGS = target.getStrings();
		save();

		LocaleAwareElementManager.instance.updateElements();
	}

	private static File getConfigHome()
	{
		File userHome = new File(System.getProperty("user.home"));
		if(SandboxType.get() == SandboxType.MACOS_APP)
			return new File(userHome, "/Library/Application Support/com.pokeemu.macos");

		if(System.getenv("SNAP_USER_COMMON") != null)
			return new File(System.getenv("SNAP_USER_COMMON"));
		else if(System.getenv("XDG_CONFIG_HOME") != null)
			return new File(System.getenv("XDG_CONFIG_HOME"));
		return new File(userHome, ".config");
	}

	private static File getConfigFile()
	{
		if(SandboxType.get() == SandboxType.MACOS_APP)
			return new File(getConfigHome(), "installer.properties");

		return new File(getConfigHome(), "pokemmo-installer.properties");
	}

	public static String getString(String key)
	{
		try
		{
			return STRINGS.getString(key);
		}
		catch(MissingResourceException | NullPointerException e)
		{
			return "[" + key + "]";
		}
	}

	public static String getString(String key, Object... params)
	{
		try
		{
			return MessageFormat.format(STRINGS.getString(key), params);
		}
		catch(MissingResourceException | NullPointerException e)
		{
			return "[" + key + "]";
		}
	}

	public static boolean hasString(String key)
	{
		return STRINGS.containsKey(key);
	}
}