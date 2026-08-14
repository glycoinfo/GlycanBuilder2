package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

import org.eurocarbdb.application.glycanbuilder.util.SAXUtils;
import org.eurocarbdb.application.glycanbuilder.util.XMLUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * That neither XML reader will fetch what a document points at.
 *
 * <p>A default JAXP parser resolves external entities, so a document can name a file and have its
 * contents substituted into the parse. Measured before the fix, with an entity pointing at a temporary
 * file: both {@link XMLUtils#read(String)} and {@link SAXUtils#read} expanded it and handed back the
 * file's contents. The same configuration will issue network requests from a document, so the reach was
 * never limited to the local disk.
 *
 * <p>The sentinel is written to a temporary file and looked for by name, so the test proves the
 * substitution did not happen rather than that some particular error was produced. No network and no
 * listening socket: an entity with a {@code file:} URI is the whole of it.
 */
public class XmlReadersRefuseExternalEntitiesTest {

	private static final String SENTINEL = "glycanbuilder-xxe-sentinel-4f2a";

	/** The DOM reader refuses it, by its existing contract of reporting and returning null. */
	@Test
	public void theDomReaderDoesNotFetchTheFile() throws Exception {
		Document parsed = XMLUtils.read(xmlNaming(fileHoldingTheSentinel()));

		assertNull("the DOM reader accepted a document with an external entity", parsed);
	}

	/** The SAX reader throws, and the handler is given nothing first. */
	@Test
	public void theSaxReaderDoesNotFetchTheFile() throws Exception {
		final StringBuilder seen = new StringBuilder();
		String xml = xmlNaming(fileHoldingTheSentinel());

		boolean threw = false;
		try {
			SAXUtils.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
					new DefaultHandler() {
						@Override
						public void characters(char[] ch, int start, int length) {
							seen.append(ch, start, length);
						}
					});
		} catch (Exception refused) {
			threw = true;
		}

		assertTrue("the SAX reader accepted a document with an external entity", threw);
		assertFalse("the handler was given the file's contents before the parse failed: " + seen,
				seen.toString().contains(SENTINEL));
	}

	/**
	 * And ordinary XML still reads, which is the half that says the hardening did not cost anything.
	 *
	 * <p>Nothing this application reads as XML declares a {@code DOCTYPE} — workspaces, configuration,
	 * fragment definitions — which is why refusing one outright was available as the strongest setting
	 * and the compatible one at the same time.
	 */
	@Test
	public void ordinaryXmlStillReads() throws Exception {
		Document parsed = XMLUtils.read(
				"<?xml version=\"1.0\"?><Workspace><Glycan>ordinary</Glycan></Workspace>");

		assertNotNull("an ordinary document should still parse", parsed);
		assertEquals("Workspace", parsed.getDocumentElement().getNodeName());
		assertEquals("ordinary", parsed.getDocumentElement().getTextContent());
	}

	/** And through SAX as well. */
	@Test
	public void ordinaryXmlStillReadsThroughSax() throws Exception {
		final StringBuilder seen = new StringBuilder();

		SAXUtils.read(new ByteArrayInputStream(
						"<?xml version=\"1.0\"?><Workspace><Glycan>ordinary</Glycan></Workspace>"
								.getBytes(StandardCharsets.UTF_8)),
				new DefaultHandler() {
					@Override
					public void characters(char[] ch, int start, int length) {
						seen.append(ch, start, length);
					}

					@Override
					public void startElement(String uri, String local, String name, Attributes a) {
					}
				});

		assertEquals("ordinary", seen.toString().trim());
	}

	/** A file the parse must not be able to reach. */
	private static File fileHoldingTheSentinel() throws Exception {
		File secret = File.createTempFile("glycanbuilder-xxe-", ".txt");
		secret.deleteOnExit();

		FileWriter writer = new FileWriter(secret);
		try {
			writer.write(SENTINEL);
		} finally {
			writer.close();
		}

		return secret;
	}

	private static String xmlNaming(File secret) {
		return "<?xml version=\"1.0\"?>"
				+ "<!DOCTYPE root [<!ENTITY leak SYSTEM \"" + secret.toURI() + "\">]>"
				+ "<root><taken>&leak;</taken></root>";
	}
}
