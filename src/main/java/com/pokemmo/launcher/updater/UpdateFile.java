package com.pokemmo.launcher.updater;

import java.io.File;

import com.pokemmo.launcher.enums.Arch;
import com.pokemmo.launcher.enums.OS;

public class UpdateFile
{
	/**
	 * Filename (Including Path)
	 */
	public final String name;
	public final String sha256;
	public final boolean only_if_not_exists;
	public final int size;

	public String absoluteUrl = "";

	public String os = "";
	public String arch = "";
	public boolean executable = false;

	public long downloadedBytes = 0;

	public UpdateFile(String name, String sha256, int size, boolean only_if_not_exists)
	{
		this.name = name;
		this.sha256 = sha256;
		this.size = size;

		this.only_if_not_exists = only_if_not_exists;
	}

	public boolean shouldDownload(File target)
	{
		if(only_if_not_exists && target.exists())
		{
			System.out.println("File already exist for 'only_if_not_exists' file: " + name);
			return false;
		}

		//If the file already exists, we should always update it.
		//This prevents people from getting into inconsistent state if they change JRE/copy client between devices.
		if(target.exists())
			return true;
		if(!os.isEmpty() && !os.equals(OS.get().getName()))
			return false;
		if(!arch.isEmpty() && !arch.equals(Arch.get().getName()))
			return false;
		return true;
	}

	public String getCacheBuster()
	{
		if(sha256 != null && !sha256.isEmpty())
		{
			return sha256.substring(0, Math.min(8, sha256.length()));
		}
		return "";
	}
}
