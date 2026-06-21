package com.pokemmo.launcher.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.concurrent.CompletableFuture;

import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.ProgressTracker;
import com.pokemmo.launcher.Launcher;
import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.enums.OS;
import com.pokemmo.launcher.ui.awt.MainFrame;

/**
 * @author Desu
 */
public class Util
{
	public static void open(File file)
	{
		ProcessBuilder pb = new ProcessBuilder();
		pb.inheritIO();

		if(OS.get() == OS.MAC)
		{
			pb.command("open", "--", file.getAbsolutePath());
		}
		else if(OS.get() == OS.LINUX)
		{
			pb.command("xdg-open", file.getAbsolutePath());
		}
		else if(OS.get() == OS.WINDOWS)
		{
			pb.command("cmd", "/c", "start", "", file.getAbsolutePath());
		}
		else
		{
			throw new IllegalStateException();
		}

		new Thread(() -> {
			try
			{
				pb.start();
			}
			catch(IOException e)
			{
				System.out.println("Failed to start open");
				e.printStackTrace();
				MainFrame.getInstance().showError(Config.getString("error.cant_open_client_folder"), Config.getString("error.io_exception"));
			}
		}).start();
	}

	public static String calculateHash(String digest_type, File file)
	{
		if(digest_type.equalsIgnoreCase("sha256"))
		{
			digest_type = "SHA-256";
		}

		if(!file.exists() || !file.isFile())
		{
			return "FILE_DOESNT_EXIST";
		}

		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(file);
			return calculateHash(digest_type, fis);
		}
		catch(Exception e)
		{
			return "ERROR CALCULATING";
		}
		finally
		{
			try
			{
				if(fis != null)
				{
					fis.close();
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static String calculateHash(String digest_type, InputStream input)
	{
		try
		{
			MessageDigest algorithm = MessageDigest.getInstance(digest_type);
			BufferedInputStream bis = new BufferedInputStream(input);
			DigestInputStream dis = new DigestInputStream(bis, algorithm);

			// read the file and update the hash calculation
			byte[] buffer = new byte[4096];
			while(dis.read(buffer) != -1) ;

			// get the hash value as byte array
			byte[] hash = algorithm.digest();

			return byteArray2Hex(hash);
		}
		catch(NoSuchAlgorithmException e)
		{
			return "Invalid Hash Algo";
		}
		catch(Exception e)
		{
			return "ERROR CALCULATING";
		}
	}

	public static String byteArray2Hex(byte[] hash)
	{
		Formatter formatter = new Formatter();
		for(byte b : hash)
		{
			formatter.format("%02x", b);
		}
		String result = formatter.toString().toLowerCase();
		formatter.close();
		return result;
	}

	/**
	 * Checks a Directory/Filepath combination to make sure it is safe.
	 *
	 * @param dir   current directory we are checking from
	 * @param entry filepath we are checking
	 * @return null if unsafe, otherwise relative path of file
	 */
	public static String sanitize(final File dir, final String entry)
	{
		if(entry.isEmpty())
		{
			return null;
		}

		if(new File(entry).isAbsolute())
		{
			return null;
		}

		try
		{
			final String DirPath = dir.getPath() + File.separator;
			final String EntryPath = new File(dir, entry).getPath();

			if(!EntryPath.startsWith(DirPath))
			{
				return null;
			}

			return EntryPath.substring(DirPath.length());
		}
		catch(Exception e)
		{
			// Ignored
		}

		return null;
	}

	/**
	 * Sends a synchronized HTTP GET request using the requested Methanol, returning the value as an InputStream
	 */
	public static HttpResponse<InputStream> getUrl(Methanol httpClient, String raw_url) throws URISyntaxException, IOException, InterruptedException
	{
		HttpRequest httpRequest = HttpRequest.newBuilder(new URI(raw_url))
				.setHeader("User-Agent", Launcher.httpClientUserAgent)
				.GET()
				.build();

		return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
	}

	/**
	 * Sends an asynchronous HTTP GET request using the requested Methanol, returning the value as an InputStream
	 */
	public static CompletableFuture<HttpResponse<byte[]>> getUrlAsync(Methanol httpClient, String raw_url) throws URISyntaxException
	{
		HttpRequest httpRequest = HttpRequest.newBuilder(new URI(raw_url))
				.setHeader("User-Agent", Launcher.httpClientUserAgent)
				.GET()
				.build();

		return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
	}

	static final ProgressTracker progressTracker = ProgressTracker.newBuilder()
			.bytesTransferredThreshold(60 * 1024) // 60 kB
			.build();

	/**
	 * Sends a synchronized HTTP GET request using the requested Methanol, returning the value as an InputStream.
	 * This method accepts content compression where available
	 */
	public static HttpResponse<Path> downloadFile(Methanol httpClient, String raw_url, File file, ProgressTracker.Listener progressListener) throws URISyntaxException, IOException, InterruptedException
	{
		HttpRequest httpRequest = HttpRequest.newBuilder(new URI(raw_url))
				.setHeader("User-Agent", Launcher.httpClientUserAgent)
				.GET()
				.build();

		var bodyHandler = HttpResponse.BodyHandlers.ofFile(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		if(progressListener != null)
			bodyHandler = progressTracker.tracking(bodyHandler, progressListener);

		return httpClient.send(httpRequest, bodyHandler);
	}

	/**
	 * Downloads and saves the requested URL to the requested filename
	 * @return true, if the http server successfully responded to the request. false if an exception occurred.
	 * A true response does not necessarily mean the server responded with what you want (e.g. a 404 request will return an http snippet.)
	 */
	public static boolean downloadUrlToFile(Methanol httpClient, String raw_url, File file, ProgressTracker.Listener progressListener)
	{
		try
		{
			raw_url = raw_url.replace("\\", "/");

			file.getParentFile().mkdirs();

			downloadFile(httpClient, raw_url, file, progressListener);
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static boolean startsWithIgnoreCase(String src, String what)
	{
		return src.regionMatches(true, 0, what, 0, what.length());
	}
}
