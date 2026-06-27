package com.pokemmo.launcher.enums;

import com.pokemmo.launcher.util.Util;

/**
 * @author Desu
 */
public enum SandboxType
{
	SNAPCRAFT,
	FLATPAK,
	MACOS_APP,
	NONE;
	
	public static SandboxType CURRENT = null;

	public static SandboxType get()
	{
		if(CURRENT != null)
			return CURRENT;

		CURRENT = computeCurrent();
		return CURRENT;
	}

	private static SandboxType computeCurrent()
	{
		if(Util.isEnv("POKEMMO_IS_SNAPPED"))
			return SNAPCRAFT;
		if(Util.isEnv("POKEMMO_IS_FLATPAKED"))
			return FLATPAK;
		if(Util.isEnv("POKEMMO_IS_MACOS_APP"))
			return MACOS_APP;
		return NONE;
	}
}
