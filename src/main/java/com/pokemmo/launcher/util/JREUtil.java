package com.pokemmo.launcher.util;


import java.io.File;
import java.util.zip.ZipFile;

import com.pokemmo.launcher.enums.OS;

/**
 * Some utility functions for legacy jre launching
 * @author Desu
 */
public class JREUtil
{
	public static File findJava()
	{
		File java;
		if(System.getProperty("java.home") != null)
		{
			java = new File(System.getProperty("java.home"), "bin" + File.separator + "java" + (OS.get() == OS.WINDOWS ? ".exe" : ""));
			if(java.exists())
			{
				System.out.println("Found java from java.home");
				return java;
			}
		}

		//Flatpak
		java = new File("/app/jre", "bin" + File.separator + "java" + (OS.get() == OS.WINDOWS ? ".exe" : ""));
		if(java.exists())
		{
			System.out.println("Found java from hardcoded /app/jre");
			return java;
		}

		//Bundled
		java = new File("bin" + File.separator + "java" + (OS.get() == OS.WINDOWS ? ".exe" : ""));
		if(java.exists())
		{
			System.out.println("Found java from bundled jre");
			return java;
		}

		if(System.getenv("JAVA_HOME") != null)
		{
			java = new File(System.getenv("JAVA_HOME"), "bin" + File.separator + "java" + (OS.get() == OS.WINDOWS ? ".exe" : ""));
			if(java.exists())
			{
				System.out.println("Found java from JAVA_HOME env");
				return java;
			}
		}

		return null;
	}

	public static boolean isPokeMMOJar(File file) {
		try (ZipFile zipFile = new ZipFile(file)) {
			return zipFile.getEntry("com/pokeemu/client/Client.class") != null;
		} catch (Exception e) {
			return false;
		}
	}
}
