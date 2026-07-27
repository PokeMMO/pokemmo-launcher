package com.pokemmo.launcher;

import java.io.File;
import java.util.Map;

import com.pokemmo.launcher.enums.SandboxType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwtLibraryPathTest
{
	private static final String SNAP_COMMON = File.separator + "snapcommon";
	private static final String XDG_DATA = File.separator + "xdgdata";

	@Test
	void snapUsesSnapUserCommon()
	{
		Map<String, String> env = Map.of("SNAP_USER_COMMON", SNAP_COMMON);

		assertEquals(SNAP_COMMON + File.separator + "swt-60",
				Launcher.resolveSwtLibraryPath(SandboxType.SNAPCRAFT, env, 60));
	}

	@Test
	void flatpakUsesXdgDataHome()
	{
		Map<String, String> env = Map.of("XDG_DATA_HOME", XDG_DATA);

		assertEquals(XDG_DATA + File.separator + "swt-60",
				Launcher.resolveSwtLibraryPath(SandboxType.FLATPAK, env, 60));
	}

	/**
	 * Either variable can be inherited by a process outside the sandbox that set it, so an
	 * environment carrying one must not be enough on its own.
	 */
	@Test
	void anUnsandboxedRunKeepsTheSwtDefault()
	{
		Map<String, String> env = Map.of("SNAP_USER_COMMON", SNAP_COMMON, "XDG_DATA_HOME", XDG_DATA);

		assertNull(Launcher.resolveSwtLibraryPath(SandboxType.NONE, env, 60));
		assertNull(Launcher.resolveSwtLibraryPath(SandboxType.MACOS_APP, env, 60));
		assertNull(Launcher.resolveSwtLibraryPath(SandboxType.NONE, Map.of(), 60));
		assertNull(Launcher.resolveSwtLibraryPath(SandboxType.MACOS_APP, Map.of(), 60));
	}

	@Test
	void aFlatpakRunIgnoresSnapUserCommon()
	{
		Map<String, String> env = Map.of("SNAP_USER_COMMON", SNAP_COMMON);

		assertNull(Launcher.resolveSwtLibraryPath(SandboxType.FLATPAK, env, 60));
	}

	/**
	 * XDG_DATA_HOME is only private to the application under flatpak, so outside a sandbox it must
	 * not be used no matter what it points at.
	 */
	@Test
	void anUnsandboxedRunIgnoresXdgDataHome()
	{
		Map<String, String> env = Map.of("XDG_DATA_HOME", File.separator + "tmp");

		assertNull(Launcher.resolveSwtLibraryPath(SandboxType.NONE, env, 60));
	}

	@Test
	void anEmptyVariableCountsAsUnset()
	{
		assertNull(Launcher.resolveSwtLibraryPath(SandboxType.SNAPCRAFT, Map.of("SNAP_USER_COMMON", ""), 60));
		assertNull(Launcher.resolveSwtLibraryPath(SandboxType.FLATPAK, Map.of("XDG_DATA_HOME", ""), 60));
	}

	/**
	 * Snap sets both, and only SNAP_USER_COMMON is a directory the snap can map a library from.
	 */
	@Test
	void snapUserCommonWinsOverXdgDataHome()
	{
		Map<String, String> env = Map.of("SNAP_USER_COMMON", SNAP_COMMON, "XDG_DATA_HOME", XDG_DATA);

		assertEquals(SNAP_COMMON + File.separator + "swt-60",
				Launcher.resolveSwtLibraryPath(SandboxType.SNAPCRAFT, env, 60));
	}

	@Test
	void theVersionIsPartOfEveryPath()
	{
		Map<String, String> snap = Map.of("SNAP_USER_COMMON", SNAP_COMMON);
		Map<String, String> flatpak = Map.of("XDG_DATA_HOME", XDG_DATA);

		assertTrue(Launcher.resolveSwtLibraryPath(SandboxType.SNAPCRAFT, snap, 61).endsWith("61"));
		assertTrue(Launcher.resolveSwtLibraryPath(SandboxType.FLATPAK, flatpak, 61).endsWith("61"));

		assertNotEquals(Launcher.resolveSwtLibraryPath(SandboxType.SNAPCRAFT, snap, 60),
				Launcher.resolveSwtLibraryPath(SandboxType.SNAPCRAFT, snap, 61));
		assertNotEquals(Launcher.resolveSwtLibraryPath(SandboxType.FLATPAK, flatpak, 60),
				Launcher.resolveSwtLibraryPath(SandboxType.FLATPAK, flatpak, 61));
	}
}
