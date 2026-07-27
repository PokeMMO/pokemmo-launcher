package com.pokemmo.launcher.updater;

import java.io.File;
import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedManagerParseTest
{
	private static final File DIR = new File(".");

	private static Element parse(String xml) throws Exception
	{
		Document doc = FeedManager.secureDocumentBuilderFactory()
				.newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		return (Element) doc.getElementsByTagName("update_feed").item(0);
	}

	@Test
	void skipsAnEntryWithAnUnparseableSizeInsteadOfAbandoningTheMirror() throws Exception
	{
		Element feed = parse("""
				<update_feed>
				  <file name="good.pak" sha256="aa" size="10"/>
				  <file name="bad.pak" sha256="bb" size="not-a-number"/>
				  <file name="also_good.pak" sha256="cc" size="20"/>
				</update_feed>
				""");

		List<UpdateFile> files = FeedManager.parseUpdateFiles(feed, DIR);

		assertEquals(2, files.size());
		assertEquals("good.pak", files.get(0).name);
		assertEquals("also_good.pak", files.get(1).name);
	}

	@Test
	void skipsAnEntryWithANonPositiveSize() throws Exception
	{
		Element feed = parse("""
				<update_feed>
				  <file name="empty.pak" sha256="aa" size="0"/>
				  <file name="real.pak" sha256="bb" size="10"/>
				</update_feed>
				""");

		List<UpdateFile> files = FeedManager.parseUpdateFiles(feed, DIR);

		assertEquals(1, files.size());
		assertEquals("real.pak", files.get(0).name);
	}

	@Test
	void skipsAnEntryWithNoSha256() throws Exception
	{
		Element feed = parse("""
				<update_feed>
				  <file name="unhashed.pak" size="10"/>
				  <file name="blank.pak" sha256="" size="10"/>
				</update_feed>
				""");

		assertTrue(FeedManager.parseUpdateFiles(feed, DIR).isEmpty(),
				"a file with no hash cannot be verified and must not be accepted");
	}

	@Test
	void skipsLegacyOptionEntriesBeforeParsingTheirSize() throws Exception
	{
		Element feed = parse("""
				<update_feed>
				  <file name="opt.pak" sha256="aa" size="" option_name="extras"/>
				  <file name="real.pak" sha256="bb" size="10"/>
				</update_feed>
				""");

		List<UpdateFile> files = FeedManager.parseUpdateFiles(feed, DIR);

		assertEquals(1, files.size());
		assertEquals("real.pak", files.get(0).name);
	}

	@Test
	void carriesOsArchAndExecutableThrough() throws Exception
	{
		Element feed = parse("""
				<update_feed>
				  <file name="PokeMMO.sh" sha256="aa" size="10" os="linux" arch="x86_64" executable="true" only_if_not_exists="true"/>
				</update_feed>
				""");

		List<UpdateFile> files = FeedManager.parseUpdateFiles(feed, DIR);

		assertEquals(1, files.size());
		UpdateFile f = files.get(0);
		assertEquals("linux", f.os);
		assertEquals("x86_64", f.arch);
		assertTrue(f.executable);
		assertTrue(f.only_if_not_exists);
	}

	/**
	 * Nothing in this payload is ill-formed, so only disallow-doctype-decl can reject it.
	 */
	@Test
	void refusesADoctypeDeclaration()
	{
		assertThrows(Exception.class, () -> parse("""
				<!DOCTYPE update_feed>
				<update_feed><file name="real.pak" sha256="aa" size="10"/></update_feed>
				"""), "DTDs must be refused outright");
	}

	/**
	 * An entity referenced in element content, which a parser that accepts the doctype resolves.
	 */
	@Test
	void refusesAnExternalEntityInElementContent()
	{
		assertThrows(Exception.class, () -> parse("""
				<!DOCTYPE update_feed [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
				<update_feed><file name="real.pak" sha256="aa" size="10"/>&xxe;</update_feed>
				"""), "an external entity must never be resolved");
	}
}
