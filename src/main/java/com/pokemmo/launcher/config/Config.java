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
	public static final int NETWORK_THREADS_MAX = 4;

	public static int NETWORK_THREADS = 4;

	public static UpdateChannel UPDATE_CHANNEL = UpdateChannel.live;

	public static short HARD_MAX_MEMORY_MB = JOPTS_XMX_VAL_MIN;

	public static PokeMMOLocale ACTIVE_LOCALE = PokeMMOLocale.getDefaultLocale();
	private static ResourceBundle STRINGS = ACTIVE_LOCALE.getStrings();

	private Config()
	{
	}

	public static void load()
	{
		Properties props = new Properties();
		try
		{
			props.load(new FileReader(getConfigFile()));
		}
		catch(IOException e)
		{
			return; // Use default properties
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

			HARD_MAX_MEMORY_MB = Short.parseShort(props.getProperty("max_mem_hard", "512"));

			if(HARD_MAX_MEMORY_MB < JOPTS_XMX_VAL_MIN)
			{
				HARD_MAX_MEMORY_MB = JOPTS_XMX_VAL_MIN;
			}
			else if(HARD_MAX_MEMORY_MB > JOPTS_XMX_VAL_MAX)
			{
				HARD_MAX_MEMORY_MB = JOPTS_XMX_VAL_MAX;
			}

			ACTIVE_LOCALE = PokeMMOLocale.getFromString(props.getProperty("launcher_locale"));

			UPDATE_CHANNEL = UpdateChannel.valueOf(props.getProperty("update_channel"));
		}
		catch(Exception e)
		{
			System.out.println("Failed to load configuration file");
		}

		STRINGS = ACTIVE_LOCALE.getStrings();
	}

	public static void save()
	{
		Properties props = new Properties();
		props.put("network_threads", Integer.toString(NETWORK_THREADS));
		props.put("update_channel", UPDATE_CHANNEL.toString());
		props.put("max_mem_hard", Short.toString(HARD_MAX_MEMORY_MB));
		props.put("launcher_locale", ACTIVE_LOCALE.getLangTag());

		File configDir = getConfigHome();
		if(configDir.exists() || configDir.mkdir())
		{
			try
			{
				props.store(new FileWriter(getConfigFile(), StandardCharsets.UTF_8), "PokeMMO Launcher v" + Launcher.INSTALLER_VERSION + " Properties");
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("Failed to save configuration for config_dir " + configDir);
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