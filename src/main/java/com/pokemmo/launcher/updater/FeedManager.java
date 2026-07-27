package com.pokemmo.launcher.updater;

import java.io.File;
import java.io.StringReader;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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

	/**
	 * Backstop so a feed request cannot hang the mirror loop forever. Kept above the client's
	 * connectTimeout plus headersTimeout, 20 seconds each, so those decide when a slow connect or
	 * header wait is given up on; the client's separate readTimeout, not this constant, is what
	 * bounds a stalled body.
	 */
	private static final int FEED_DEADLINE_SECONDS = 45;

	public static void load(LauncherUI launcherUI)
	{
		UpdateChannel channel = Config.UPDATE_CHANNEL;

		String sig_format = "SHA256withRSA";
		PublicKey pub_key = channel.getPublicKey();

		List<Throwable> failures = new ArrayList<>();
		loop:for(String mirror : channel.getMirrors())
		{
			// Every attempt starts from clean state, so a mirror abandoned part-way through
			// cannot leave entries or version floors behind for whichever mirror succeeds.
			files.clear();
			MIN_REVISION = 0;
			MIN_LAUNCHER_VERSION = 0;

			try
			{
				System.out.println("Loading feed from " + mirror);
				CompletableFuture<HttpResponse<byte[]>> mainFeedResponse = Util.getUrlAsync(Launcher.httpClient, mirror + "/" + channel.urlComponent() + "/current/feeds/main_feed.txt").orTimeout(FEED_DEADLINE_SECONDS, TimeUnit.SECONDS);
				CompletableFuture<HttpResponse<byte[]>> mainFeedSigResponse = Util.getUrlAsync(Launcher.httpClient, mirror + "/" + channel.urlComponent() + "/current/feeds/main_feed.sig256").orTimeout(FEED_DEADLINE_SECONDS, TimeUnit.SECONDS);
				CompletableFuture<HttpResponse<byte[]>> updateFeedResponse = Util.getUrlAsync(Launcher.httpClient, mirror + "/" + channel.urlComponent() + "/current/feeds/update_feed.txt").orTimeout(FEED_DEADLINE_SECONDS, TimeUnit.SECONDS);
				CompletableFuture<HttpResponse<byte[]>> updateFeedSigResponse = Util.getUrlAsync(Launcher.httpClient, mirror + "/" + channel.urlComponent() + "/current/feeds/update_feed.sig256").orTimeout(FEED_DEADLINE_SECONDS, TimeUnit.SECONDS);

				// allOf only completes once every input has settled, so each future carries its
				// own deadline; one unbounded future would hold this join open indefinitely.
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

				DocumentBuilderFactory dbf = secureDocumentBuilderFactory();
				DocumentBuilder db = dbf.newDocumentBuilder();
				InputSource is = new InputSource(new StringReader(new String(mainFeedRaw, StandardCharsets.UTF_8)));
				Document doc = db.parse(is);

				Element main_feed = (Element) doc.getElementsByTagName("main_feed").item(0);

				if(main_feed.getElementsByTagName("min_revision").getLength() > 0)
				{
					MIN_REVISION = Integer.parseInt(main_feed.getElementsByTagName("min_revision").item(0).getTextContent());
				}

				File current_directory = new File(".");
				dbf = secureDocumentBuilderFactory();
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

				List<UpdateFile> parsed = parseUpdateFiles(update_feed, current_directory);

				//Make sure we have at least 1 normal file
				if(parsed.isEmpty())
				{
					SUCCESSFUL = false;
					launcherUI.showInfo(Config.getString("status.networking.feed_load_failed_alt", mirror));
					continue loop;
				}

				files.addAll(parsed);
				SUCCESSFUL = true;
				return;
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

	/**
	 * A {@link DocumentBuilderFactory} with DTDs, external entities and XInclude switched off.
	 */
	static DocumentBuilderFactory secureDocumentBuilderFactory() throws ParserConfigurationException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
		dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		dbf.setXIncludeAware(false);
		dbf.setExpandEntityReferences(false);
		return dbf;
	}

	/**
	 * Reads the {@code file} entries of an update feed.
	 * <p>
	 * An entry the launcher cannot act on is skipped on its own rather than abandoning the
	 * whole feed, so a single malformed entry does not cost us an otherwise healthy mirror.
	 *
	 * @param update_feed       the update_feed element to read
	 * @param current_directory the directory entry names must resolve inside
	 * @return the entries that were usable, in feed order
	 */
	static List<UpdateFile> parseUpdateFiles(Element update_feed, File current_directory)
	{
		List<UpdateFile> parsed = new ArrayList<>();
		NodeList filesNodeList = update_feed.getElementsByTagName("file");

		for(int x = 0; x < filesNodeList.getLength(); x++)
		{
			Node fileT = filesNodeList.item(x);
			if(fileT.getNodeType() != Node.ELEMENT_NODE)
				continue;

			Element file = (Element) fileT;

			//Legacy options
			if(!file.getAttribute("option_name").isEmpty())
				continue;

			String sanitized = Util.sanitize(current_directory, file.getAttribute("name"));
			if(sanitized == null)
			{
				System.out.println("Skipping feed entry with an unsafe name: " + file.getAttribute("name"));
				continue;
			}

			//Without a hash the download cannot be verified
			String sha256 = file.getAttribute("sha256");
			if(sha256.isEmpty())
			{
				System.out.println("Skipping feed entry with no sha256: " + sanitized);
				continue;
			}

			int size;
			try
			{
				size = Integer.parseInt(file.getAttribute("size"));
			}
			catch(NumberFormatException e)
			{
				System.out.println("Skipping feed entry with an unparseable size: " + sanitized);
				continue;
			}

			if(size <= 0)
			{
				System.out.println("Skipping feed entry with a non-positive size: " + sanitized);
				continue;
			}

			UpdateFile f = new UpdateFile(sanitized, sha256, size, Boolean.parseBoolean(file.getAttribute("only_if_not_exists")));
			if(file.hasAttribute("os"))
				f.os = file.getAttribute("os");
			if(file.hasAttribute("arch"))
				f.arch = file.getAttribute("arch");
			f.executable = Boolean.parseBoolean(file.getAttribute("executable"));
			parsed.add(f);
		}

		return parsed;
	}
}
