package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * That reading a sequence never fetches anything the sequence names (CVE-2021-33813).
 *
 * <p>Three of the formats the library accepts are XML - {@code glycoct_xml}, {@code cabosml} and
 * {@code glyde} - and they are read by JDOM's {@code SAXBuilder}, which resolves external entities
 * as it meets them. A document declaring {@code <!ENTITY xx SYSTEM "...">} therefore made the
 * parser open whatever that named: measured before the fix, a {@code file://} entity had the
 * process open that path mid-parse, so whoever supplied a sequence chose what was read and what was
 * connected to. On a server taking sequences from callers that is the whole of the vulnerability.
 *
 * <p>There is no fixed version to move to. The advisory names {@code org.jdom:jdom2:2.0.6.1}, and
 * these libraries are compiled against {@code org.jdom}, a different package - which is why the
 * dependency alert reports no patched version. So the document type declaration is refused before
 * the parser sees it, a DTD being the only way to declare an entity.
 *
 * <p>What is asserted is behaviour rather than the refusal: a local server is put where the
 * document points, and the test is that it is never called. An assertion that the import failed
 * would pass just as well if the parse failed for some unrelated reason while still fetching.
 */
public class XmlSequencesFetchNothingTest {

	/** Every XML format reachable through the importer factory. */
	private static final String[] XML_FORMATS = { "glycoct_xml", "cabosml", "glyde" };

	private static HttpServer server;

	/** Counts what the parser asked for, which should stay at zero. */
	private static final AtomicInteger requests = new AtomicInteger();

	@BeforeClass
	public static void startTheServerTheDocumentWillPointAt() throws IOException {
		// Loopback and an ephemeral port: nothing leaves the machine, and nothing is claimed.
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/", new HttpHandler() {
			public void handle(HttpExchange exchange) throws IOException {
				requests.incrementAndGet();
				byte[] body = "fetched".getBytes("UTF-8");
				exchange.sendResponseHeaders(200, body.length);
				OutputStream out = exchange.getResponseBody();
				out.write(body);
				out.close();
			}
		});
		server.start();
	}

	@AfterClass
	public static void stopTheServer() {
		if (server != null) server.stop(0);
	}

	/**
	 * A document type naming a URL does not make the parser go and get it.
	 *
	 * <p>Before the fix this test fails on the first format: the count reaches one as the parser
	 * resolves the entity, and the fetched text is spliced into the document it goes on to read.
	 */
	@Test
	public void whatADocumentTypeNamesIsNeverFetched() {
		String url = "http://" + server.getAddress().getHostString() + ":"
				+ server.getAddress().getPort() + "/fetched-by-the-parser";
		String document =
				"<?xml version=\"1.0\"?>\n"
				+ "<!DOCTYPE sugar [ <!ENTITY xx SYSTEM \"" + url + "\"> ]>\n"
				+ "<sugar version=\"1.0\"><residues><basetype id=\"1\">&xx;</basetype></residues>"
				+ "<linkages/></sugar>";

		for (String format : XML_FORMATS) {
			requests.set(0);

			boolean imported = importAndReport(document, format);

			assertEquals(format + ": the parser fetched what the document type named",
					0, requests.get());
			assertFalse(format + ": a document declaring a document type should not be read",
					imported);
		}
	}

	/**
	 * The formats that are not XML are untouched by the refusal.
	 *
	 * <p>The check is on the text handed to the parser rather than on the format, so this is what
	 * says it costs nothing: the sequences actually in use still read.
	 */
	@Test
	public void theSequencesInUseStillRead() {
		assertTrue("GWS should still read",
				importAndReport("freeEnd--?b1D-GlcNAc,p--4b1D-GlcNAc,p", "gws"));
		assertTrue("WURCS should still read", importAndReport(
				"WURCS=2.0/1,1,0/[a2122h-1b_1-5_2*NCC/3=O]/1/", "wurcs2"));
	}

	private static boolean importAndReport(String sequence, String format) {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		try {
			return document.importFromString(sequence, format);
		} catch (Exception refused) {
			return false;
		}
	}
}
