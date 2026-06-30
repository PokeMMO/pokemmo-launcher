package com.pokemmo.launcher.util;

import java.io.File;
import java.io.IOException;

import com.pokemmo.launcher.enums.Arch;
import com.pokemmo.launcher.updater.UpdateFile;

/**
 * @author Desu
 */
public class WindowsUtil
{
	public static boolean isVC14RedistInstalled()
	{
		return checkValueExists("HKLM\\SOFTWARE\\Microsoft\\VisualStudio\\14.0\\VC\\Runtimes\\" + Arch.get().name(), "Version");
	}

	private static boolean checkValueExists(String keyPath, String valueName) {
		try {
			Process process = new ProcessBuilder("reg", "query", keyPath, "/v", valueName).start();
			process.waitFor();
			return process.exitValue() == 0;
		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	public static boolean installVC(File pokemmoDir, UpdateFile vcRedist)
	{
		try
		{
			File targetExe = new File(pokemmoDir, vcRedist.name);
			String command = "powershell -Command \"Start-Process -FilePath '" + targetExe.getAbsolutePath() + "' -ArgumentList '/quiet', '/install', '/norestart' -Verb RunAs\"";
			ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
			Process process = pb.start();
			process.waitFor();
			if(process.exitValue() == 0)
			{
				targetExe.delete();
				return true;
			}
		} catch (IOException | InterruptedException e) {
		}
		return false;
	}
}
