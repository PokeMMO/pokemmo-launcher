package com.pokemmo.launcher.updater;

import java.io.File;
import java.io.StringReader;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.pokemmo.launcher.Launcher;
import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.enums.UpdateChannel;
import com.pokemmo.launcher.ui.LauncherUI;
import com.pokemmo.launcher.util.CryptoUtil;
import com.pokemmo.launcher.util.Util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Manager class for loading Main / Update feeds
 *
 * @author Desu
 */
public class FeedManager
{
	/**
	 * Min client revision allowed. If lower, will force update.
	 */
	public static int MIN_REVISION = 0;
	public static int MIN_LAUNCHER_VERSION = 0;
	public static boolean SUCCESSFUL = false;
	private static final List<UpdateFile> files = new ArrayList<>();

	public static void load(LauncherUI launcherUI)
	{
		files.clear();

		UpdateChannel channel = Config.UPDATE_CHANNEL;

		String sig_format = "SHA256withRSA";
		PublicKey pub_key = channel.getPublicKey();

		List<Throwable> failures = new ArrayList<>();
		loop:for(String mirror : channel.getMirrors())
		{
			try
			{
				System.out.println("Loading feed from " + mirror);
				CompletableFuture<HttpResponse<byte[]>> mainFeedResponse = Util.getUrlAsync(Launcher.httpClient, mirror + "/" + channel.urlComponent() + "/current/feeds/main_feed.txt");
				CompletableFuture<HttpResponse<byte[]>> mainFeedSigResponse = Util.getUrlAsync(Launcher.httpClient, mirror + "/" + channel.urlComponent() + "/current/feeds/main_feed.sig256");
				CompletableFuture<HttpResponse<byte[]>> updateFeedResponse = Util.getUrlAsync(Launcher.httpClient, mirror + "/" + channel.urlComponent() + "/current/feeds/update_feed.txt");
				CompletableFuture<HttpResponse<byte[]>> updateFeedSigResponse = Util.getUrlAsync(Launcher.httpClient, mirror + "/" + channel.urlComponent() + "/current/feeds/update_feed.sig256");

				// Using CompleteableFuture#allOf#join will eagerly terminate this mirror's processing if one of the URLs throws some kind of exception
				CompletableFuture.allOf(mainFeedResponse, mainFeedSigResponse, updateFeedResponse, updateFeedSigResponse)
						.exceptionally(error ->
						{
							launcherUI.showInfo("status.networking.feed_load_failed_validation", mirror, "INVALID_001");
							return null;
						}).join();

				byte[] mainFeedRaw = mainFeedResponse.get().body();
				byte[] mainFeedSigRaw = mainFeedSigResponse.get().body();
				byte[] updateFeedRaw = updateFeedResponse.get().body();
				byte[] updateFeedSigRaw = updateFeedSigResponse.get().body();

				if(!CryptoUtil.verifySignature(mainFeedRaw, mainFeedSigRaw, pub_key, sig_format))
				{
					System.out.println("Main feed failed verification");
					launcherUI.showInfo(Config.getString("status.networking.feed_load_failed_alt", mirror));
					continue;
				}

				if(!CryptoUtil.verifySignature(updateFeedRaw, updateFeedSigRaw, pub_key, sig_format))
				{
					System.out.println("Update feed failed verification");
					launcherUI.showInfo(Config.getString("status.networking.feed_load_failed_alt", mirror));
					continue;
				}

				// If sig validity passes, move on to xml parsing / updates / min_revision checks

				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				InputSource is = new InputSource(new StringReader(new String(mainFeedRaw, StandardCharsets.UTF_8)));
				Document doc = db.parse(is);

				Element main_feed = (Element) doc.getElementsByTagName("main_feed").item(0);

				if(main_feed.getElementsByTagName("min_revision").getLength() > 0)
				{
					MIN_REVISION = Integer.parseInt(main_feed.getElementsByTagName("min_revision").item(0).getTextContent());
				}

				File current_directory = new File(".");
				dbf = DocumentBuilderFactory.newInstance();
				db = dbf.newDocumentBuilder();
				is = new InputSource(new StringReader(new String(updateFeedRaw, StandardCharsets.UTF_8)));
				doc = db.parse(is);

				Element update_feed = (Element) doc.getElementsByTagName("update_feed").item(0);

				if(update_feed.hasAttribute("min_launcher_version"))
				{
					try
					{
						MIN_LAUNCHER_VERSION = Integer.parseInt(update_feed.getAttribute("min_launcher_version"));
					}
					catch(Exception e)
					{
						// Don't care
					}
				}

				//Only use if min_launcher_version above failed
				if(MIN_LAUNCHER_VERSION < 1 && update_feed.hasAttribute("min_osx_installer_version"))
				{
					try
					{
						MIN_LAUNCHER_VERSION = Integer.parseInt(update_feed.getAttribute("min_osx_installer_version"));
					}
					catch(Exception e)
					{
						// Don't care
					}
				}

				NodeList filesNodeList = update_feed.getElementsByTagName("file");
				for(int x = 0; x < filesNodeList.getLength(); x++)
				{
					Node fileT = filesNodeList.item(x);
					if(fileT.getNodeType() == Node.ELEMENT_NODE)
					{
						Element file = (Element) fileT;
						String sanitized = Util.sanitize(current_directory, file.getAttribute("name"));
						int size = Integer.parseInt(file.getAttribute("size"));

						//Legacy options
						if(!file.getAttribute("option_name").isEmpty())
							continue;

						if(sanitized != null && size > 0)
						{
							UpdateFile f = new UpdateFile(sanitized, file.getAttribute("sha256"), size, Boolean.parseBoolean(file.getAttribute("only_if_not_exists")));
							if(file.hasAttribute("os"))
								f.os = file.getAttribute("os");
							if(file.hasAttribute("arch"))
								f.arch = file.getAttribute("arch");
							f.executable = Boolean.parseBoolean(file.getAttribute("executable"));
							files.add(f);
						}
						else
						{
							SUCCESSFUL = false;
							continue loop;
						}
					}
				}

				//Make sure we have at least 1 normal file
				if(!files.isEmpty())
				{
					SUCCESSFUL = true;
					return;
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				launcherUI.showInfo(Config.getString("status.networking.feed_load_failed_alt", mirror));
				failures.add(e);
			}
		}
	}

	/**
	 * List files and their checksums.
	 *
	 * @return files
	 */
	public static List<UpdateFile> getFiles()
	{
		return files;
	}
}
