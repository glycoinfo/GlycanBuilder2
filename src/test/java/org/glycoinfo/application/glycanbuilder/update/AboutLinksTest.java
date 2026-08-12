package org.glycoinfo.application.glycanbuilder.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import org.junit.Test;

/**
 * The About window: its icon, and its links.
 *
 * <p>Both are things a rendered HTML pane resolves for itself, and both had been wrong without
 * anything failing. The icon pointed at a file that had stopped being the application's icon; the
 * links reported clicks that nobody was listening for, so they did nothing at all.
 *
 * <p>What is held here is what the pane can resolve from the document. Whether the listener is then
 * attached is {@code GlycanBuilder.onAbout}'s business, and whether a browser opens is the desktop's
 * - neither is reachable from a test without a display.
 */
public class AboutLinksTest {

	/** Every link in the About window resolves to an address a browser could be sent to. */
	@Test
	public void everyLinkResolvesToAnAbsoluteAddress() throws Exception {
		List<URL> links = linksIn(aboutWindow());

		assertEquals("the About window should carry three links", 3, links.size());
		for (URL link : links) {
			assertTrue(link + " is not a web address a browser can open",
					"https".equals(link.getProtocol()) || "http".equals(link.getProtocol()));
		}
	}

	/** And they are the three the window is for: the notation, the format, and the paper. */
	@Test
	public void theyAreTheNotationTheFormatAndThePaper() throws Exception {
		List<String> links = new ArrayList<>();
		for (URL link : linksIn(aboutWindow())) links.add(link.toString());

		assertTrue(links.toString(), links.stream().anyMatch(l -> l.contains("snfg")));
		assertTrue(links.toString(), links.stream().anyMatch(l -> l.contains("wurcs")));
		// Cited by DOI, not by publisher URL: the DOI is what the article keeps wherever it is
		// hosted. Verified against CrossRef that it names this article - the PII it resolves to,
		// S0008621516305316, is what the direct link used to point at.
		assertTrue(links.toString(),
				links.stream().anyMatch(l -> l.equals("https://doi.org/10.1016/j.carres.2017.04.015")));
	}

	/**
	 * The icon is the application's own, reached from where the document sits.
	 *
	 * <p>It is referenced rather than copied - {@code ../icons/icon_large.png}, the file jpackage
	 * builds the installers' icons from - so the two cannot drift again as they had. A relative
	 * reference across directories is exactly what resolves from a class directory and not from a
	 * jar, so what is checked is that it can actually be read, not that the text is right.
	 */
	@Test
	public void theIconIsTheApplicationsOwnAndCanBeRead() throws Exception {
		JEditorPane about = aboutWindow();
		HTMLDocument document = (HTMLDocument) about.getDocument();

		HTMLDocument.Iterator images = document.getIterator(HTML.Tag.IMG);
		assertTrue("the About window has no image", images.isValid());
		Object source = images.getAttributes().getAttribute(HTML.Attribute.SRC);
		assertNotNull("the image has no src", source);

		URL icon = new URL(document.getBase(), source.toString());
		assertTrue("the icon should be the application's own, not a copy beside the page: " + source,
				source.toString().contains("icons/icon_large.png"));
		icon.openStream().close();
	}

	private static JEditorPane aboutWindow() throws Exception {
		JEditorPane about = new JEditorPane(
				AboutLinksTest.class.getResource("/html/about_builder.html"));
		about.setEditable(false);
		// The pane loads asynchronously by default, as it does in the application.
		Thread.sleep(900);

		return about;
	}

	/**
	 * The links a click would produce, gathered the way the pane produces them - resolved against
	 * the document's own base, which is what a {@link HyperlinkEvent} carries.
	 */
	private static List<URL> linksIn(JEditorPane pane) throws Exception {
		HTMLDocument document = (HTMLDocument) pane.getDocument();
		List<URL> links = new ArrayList<>();

		for (HTMLDocument.Iterator anchors = document.getIterator(HTML.Tag.A);
				anchors.isValid(); anchors.next()) {
			Object href = anchors.getAttributes().getAttribute(HTML.Attribute.HREF);
			if (href != null) links.add(new URL(document.getBase(), href.toString()));
		}

		return links;
	}
}
