package com.pokemmo.launcher.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class UtilSanitizeTest
{
	@TempDir
	Path dir;

	private String sanitize(String entry)
	{
		return Util.sanitize(dir.toFile(), entry);
	}

	@Test
	void acceptsThePlainRelativePathsTheFeedActuallyUses()
	{
		assertEquals(join("data", "sprites", "atlas", "main.png"), sanitize("data/sprites/atlas/main.png"));
		assertEquals("PokeMMO.exe", sanitize("PokeMMO.exe"));
		assertEquals(join("data", "themes", "default", "res", "fonts", "NotoSans-Bold.ttf"),
				sanitize("data/themes/default/res/fonts/NotoSans-Bold.ttf"));
	}

	@Test
	void keepsSingleDotsInsideAFileName()
	{
		assertEquals(join("data", "maps", "0_3.png"), sanitize("data/maps/0_3.png"));
	}

	@Test
	void rejectsAnEmptyName()
	{
		assertNull(sanitize(""));
	}

	@Test
	void rejectsAnAbsolutePath()
	{
		assertNull(sanitize("/etc/passwd"));
	}

	@Test
	void rejectsParentTraversal()
	{
		assertNull(sanitize("../evil.sh"));
		assertNull(sanitize("data/../../evil.sh"));
	}

	@Test
	void rejectsTraversalThatWouldResolveBackInside()
	{
		assertNull(sanitize("data/../data/resources.zip"),
				"a name containing .. is not something the feed should be sending");
	}

	@Test
	void rejectsABackslashTraversal()
	{
		assertNull(sanitize("..\\evil.sh"));
		assertNull(sanitize("data\\..\\..\\evil.sh"));
	}

	@Test
	void rejectsAUncPath()
	{
		assertNull(sanitize("\\\\server\\share\\evil.exe"));
	}

	@Test
	void rejectsAWindowsDriveRelativePath()
	{
		assertNull(sanitize("C:evil.exe"));
	}

	@Test
	void rejectsANulByte()
	{
		assertNull(sanitize("data/resources.zip\0.sh"));
	}

	@Test
	void rejectsADotSegment()
	{
		assertNull(sanitize("./data/resources.zip"));
		assertNull(sanitize("data/./resources.zip"));
	}

	/**
	 * Windows strips trailing dots and spaces when it opens a file, so ".. " and "..." name the
	 * parent directory there however they are spelled in the feed.
	 */
	@Test
	void rejectsASegmentWindowsWouldStripBackToATraversal()
	{
		assertNull(sanitize(".. \\evil.exe"));
		assertNull(sanitize(".. /evil.exe"));
		assertNull(sanitize("data/.. /../evil"));
		assertNull(sanitize("...\\evil"));
		assertNull(sanitize("..."));
		assertNull(sanitize("data/.../evil.zip"));
	}

	/**
	 * A dropped entry is only logged, and enough of them leave the feed looking empty and cost the
	 * mirror, so moving data/ to another disk must not change what is accepted.
	 */
	@Test
	void acceptsAnEntryUnderASymlinkedSubdirectory(@TempDir Path elsewhere) throws IOException
	{
		try
		{
			Files.createSymbolicLink(dir.resolve("data"), elsewhere);
		}
		catch(IOException | UnsupportedOperationException e)
		{
			assumeTrue(false, "this filesystem does not allow the test to create a symlink");
		}
		Files.createFile(elsewhere.resolve("resources.zip"));

		assertEquals(join("data", "resources.zip"), sanitize("data/resources.zip"));
	}

	@Test
	void collapsesRedundantSeparators()
	{
		assertEquals(join("a", "b"), sanitize("a//b"));
		assertEquals("data", sanitize("data/"));
	}

	private static String join(String... parts)
	{
		return String.join(File.separator, parts);
	}
}
