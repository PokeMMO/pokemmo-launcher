package com.pokemmo.launcher.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Windows has no execute bit: setExecutable returns its own argument without touching the file and
 * canExecute is true for every file that exists. The cases that clear the bit are POSIX only.
 */
class UtilExecutableTest
{
	@TempDir
	Path dir;

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void setsTheBitWhenItIsMissing() throws IOException
	{
		File f = Files.createFile(dir.resolve("PokeMMO")).toFile();
		assertTrue(f.setExecutable(false, false));

		assertTrue(Util.ensureExecutable(f, true, false));
		assertTrue(f.canExecute());
	}

	@Test
	void reportsNoChangeWhenTheBitIsAlreadySet() throws IOException
	{
		File f = Files.createFile(dir.resolve("PokeMMO")).toFile();
		assertTrue(f.setExecutable(true, false));

		assertFalse(Util.ensureExecutable(f, true, false));
		assertTrue(f.canExecute());
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void leavesFilesTheFeedDoesNotMarkExecutable() throws IOException
	{
		File f = Files.createFile(dir.resolve("resources.zip")).toFile();
		assertTrue(f.setExecutable(false, false));

		assertFalse(Util.ensureExecutable(f, false, false));
		assertFalse(f.canExecute());
	}

	@Test
	void isANoOpOnWindows() throws IOException
	{
		File f = Files.createFile(dir.resolve("PokeMMO.exe")).toFile();

		assertFalse(Util.ensureExecutable(f, true, true),
				"Windows has no POSIX execute bit to repair");
	}

	@Test
	void isANoOpForAMissingFile()
	{
		assertFalse(Util.ensureExecutable(dir.resolve("absent").toFile(), true, false));
	}

	@Test
	void isANoOpForADirectory()
	{
		assertFalse(Util.ensureExecutable(dir.toFile(), true, false));
	}
}
