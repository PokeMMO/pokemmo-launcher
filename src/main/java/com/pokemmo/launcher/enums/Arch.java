package com.pokemmo.launcher.enums;

public enum Arch
{
	X86("x86"),
	X64("x64"),
	ARM32("arm32"),
	ARM64("arm64"),
	UNKNOWN("-");

	private final String name;
	public static Arch CURRENT = null;

	Arch(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public static Arch getByName(String name)
	{
		for(Arch arch : Arch.values())
		{
			if(arch.name.equals(name))
				return arch;
		}
		return UNKNOWN;
	}

	public static Arch get()
	{
		if(CURRENT != null)
			return CURRENT;

		CURRENT = computeCurrent();
		return CURRENT;
	}

	private static Arch computeCurrent()
	{
		boolean isARM = System.getProperty("os.arch").startsWith("arm") || System.getProperty("os.arch").startsWith("aarch64");
		boolean is64Bit = System.getProperty("os.arch").contains("64") || System.getProperty("os.arch").startsWith("armv8");

		if(isARM)
			return is64Bit ? ARM64 : ARM32;
		return is64Bit ? X64 : X86;
	}
}
