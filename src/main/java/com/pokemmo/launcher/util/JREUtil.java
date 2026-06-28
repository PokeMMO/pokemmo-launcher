package com.pokemmo.launcher.util;


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipFile;

import com.pokemmo.launcher.enums.OS;

/**
 * Some utility functions for legacy jre launching
 * @author Desu
 */
public class JREUtil
{
	public static File findJava(File pokemmoDir)
	{
		File java;
		if(System.getProperty("java.home") != null)
		{
			java = new File(System.getProperty("java.home"), "bin" + File.separator + "java" + (OS.get() == OS.WINDOWS ? ".exe" : ""));
			if(java.exists() && java.isFile() && java.canExecute())
			{
				System.out.println("Found java from java.home: " + java.getAbsolutePath());
				return java;
			}
		}

		//Flatpak
		java = new File("/app/jre", "bin" + File.separator + "java" + (OS.get() == OS.WINDOWS ? ".exe" : ""));
		if(java.exists() && java.isFile() && java.canExecute())
		{
			System.out.println("Found java from hardcoded /app/jre: " + java.getAbsolutePath());
			return java;
		}

		//Macos
		java = new File("../runtime/Contents/Home", "bin" + File.separator + "java" + (OS.get() == OS.WINDOWS ? ".exe" : ""));
		if(java.exists() && java.isFile() && java.canExecute())
		{
			System.out.println("Found java from macos runtime: " + java.getAbsolutePath());
			return java;
		}

		//Bundled
		java = new File(pokemmoDir, "bin" + File.separator + "java" + (OS.get() == OS.WINDOWS ? ".exe" : ""));
		if(java.exists() && java.isFile() && java.canExecute())
		{
			System.out.println("Found java from bundled jre: " + java.getAbsolutePath());
			return java;
		}

		if(System.getenv("JAVA_HOME") != null)
		{
			java = new File(System.getenv("JAVA_HOME"), "bin" + File.separator + "java" + (OS.get() == OS.WINDOWS ? ".exe" : ""));
			if(java.exists() && java.isFile() && java.canExecute())
			{
				System.out.println("Found java from JAVA_HOME env: " + java.getAbsolutePath());
				return java;
			}
		}

		//try to brute force from path
		java = findInPath("java");
		if(java != null && java.exists() && java.isFile() && java.canExecute())
		{
			System.out.println("Found java in PATH: " + java.getAbsolutePath());
			return java;
		}

		System.out.println("Could not find java...");
		return null;
	}

	public static File findInPath(String executableName)
	{
		String pathEnv = System.getenv("PATH");
		if(pathEnv == null || pathEnv.isEmpty())
			return null;

		String[] dirs = pathEnv.split(File.pathSeparator);
		for(String dir : dirs)
		{
			if(dir == null || dir.trim().isEmpty())
				continue;

			// Resolve relative paths (e.g., ".") against current working directory
			Path dirPath = Paths.get(dir).normalize();
			Path filePath = dirPath.resolve(executableName);

			File file = filePath.toFile();
			if(file.exists() && file.isFile() && file.canExecute())
				return file;

			if(OS.get() == OS.WINDOWS)
			{
				File extendedFile = new File(dir, executableName + ".exe");
				if(extendedFile.exists() && extendedFile.isFile() && extendedFile.canExecute())
					return extendedFile;
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
