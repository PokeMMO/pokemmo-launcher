package com.pokeemu.updater.enums;

import java.util.Locale;

public enum OS
{
	WINDOWS("windows"),
	LINUX("linux"),
	MAC("macos"),
	ANDROID("android"),
	UNKNOWN("-");

	private final String name;
	public static OS CURRENT = null;

	OS(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public static OS getByName(String name)
	{
		for(OS os : OS.values())
		{
			if(os.name.equals(name))
				return os;
		}
		return UNKNOWN;
	}

	public static OS get()
	{
		if(CURRENT != null)
			return CURRENT;

		CURRENT = computeCurrent();
		return CURRENT;
	}

	private static OS computeCurrent()
	{
		String vm = System.getProperty("java.runtime.name");
		if(vm != null && vm.contains("Android Runtime"))
		{
			return ANDROID;
		}

		String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
		if(os.indexOf("win") >= 0)
		{
			return WINDOWS;
		}
		if(os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0)
		{
			return LINUX;
		}
		if(os.indexOf("mac") >= 0)
		{
			return MAC;
		}
		return UNKNOWN;
	}
}
