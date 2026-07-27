package com.pokemmo.launcher.config;

import java.io.File;

/**
 * Calls {@link Config}'s package-private load(File) on behalf of tests in other packages, which
 * would otherwise have to read the real configuration file through the public load().
 */
public final class ConfigTestAccess
{
	private ConfigTestAccess()
	{
	}

	public static void load(File configFile)
	{
		Config.load(configFile);
	}
}
