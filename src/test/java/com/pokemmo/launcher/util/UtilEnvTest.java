package com.pokemmo.launcher.util;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UtilEnvTest
{
	@Test
	void returnsTheValueWhenSet()
	{
		assertEquals("/home/u/.local/share",
				Util.nonEmptyEnv(Map.of("XDG_DATA_HOME", "/home/u/.local/share"), "XDG_DATA_HOME"));
	}

	@Test
	void returnsNullWhenTheVariableIsAbsent()
	{
		assertNull(Util.nonEmptyEnv(Map.of(), "XDG_DATA_HOME"));
	}

	@Test
	void treatsAnEmptyValueAsUnset()
	{
		assertNull(Util.nonEmptyEnv(Map.of("XDG_DATA_HOME", ""), "XDG_DATA_HOME"));
	}

	@Test
	void treatsAnEmptySnapUserCommonAsUnset()
	{
		Map<String, String> env = Map.of("SNAP_USER_COMMON", "", "XDG_DATA_HOME", "/home/u/.local/share");

		assertNull(Util.nonEmptyEnv(env, "SNAP_USER_COMMON"));
		assertEquals("/home/u/.local/share", Util.nonEmptyEnv(env, "XDG_DATA_HOME"));
	}
}
