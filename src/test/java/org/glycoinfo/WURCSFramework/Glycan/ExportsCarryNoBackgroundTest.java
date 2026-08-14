package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.PositionManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.SVGUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That an exported picture brings no background with it (#186, #188).
 *
 * <p>An exported glycan goes into a figure or a slide, where a white rectangle behind it covers
 * whatever it was placed on. PNG carries an alpha channel and the renderer could always paint
 * without a background; the export simply never asked, passing opaque for every format alike.
 *
 * <p>SVG was worse than one rectangle. The renderers clear behind text so it stays legible over a
 * bond line, and {@code clearRect} on an SVG surface paints the background colour rather than
 * removing anything — so every piece of text carried its own white patch, and removing them by hand
 * in Illustrator was what the report described doing.
 */
public class ExportsCarryNoBackgroundTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void newWorkspace() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** A PNG is transparent where nothing was drawn. */
	@Test
	public void aPngHasNoBackground() throws Exception {
		ByteArrayOutputStream written = new ByteArrayOutputStream();
		SVGUtils.export(written, (GlycanRendererAWT) workspace.getGlycanRenderer(), structures(),
				false, true, 1.0, "png", new PositionManager(), new BBoxManager());

		BufferedImage image = ImageIO.read(new ByteArrayInputStream(written.toByteArray()));
		int topLeft = image.getRGB(0, 0);

		assertEquals("the corner of the image is painted, so the background is still there",
				0, topLeft >>> 24);
	}

	/**
	 * Formats with no alpha keep theirs.
	 *
	 * <p>BMP and JPEG have nowhere to write transparency. Painting them onto nothing gives a black
	 * background or an undefined one, which is worse than the white it would replace.
	 */
	@Test
	public void formatsWithoutAlphaKeepTheirBackground() throws Exception {
		for (String format : new String[] { "bmp", "jpg" }) {
			ByteArrayOutputStream written = new ByteArrayOutputStream();
			SVGUtils.export(written, (GlycanRendererAWT) workspace.getGlycanRenderer(), structures(),
					false, true, 1.0, format, new PositionManager(), new BBoxManager());

			BufferedImage image = ImageIO.read(new ByteArrayInputStream(written.toByteArray()));
			int topLeft = image.getRGB(0, 0);

			assertEquals(format + " should still be painted onto white", 255, topLeft >>> 24);
		}
	}

	/** No white rectangle anywhere in the SVG — neither the page's nor one behind each character. */
	@Test
	public void anSvgHasNoWhiteRectangles() {
		String svg = SVGUtils.getVectorGraphics((GlycanRendererAWT) workspace.getGlycanRenderer(),
				structures(), false, true);

		int white = 0;
		Matcher rectangles = Pattern.compile("<rect\\b[^>]*/?>").matcher(svg);
		while (rectangles.find()) {
			String rectangle = rectangles.group();
			if (rectangle.contains("fill:white") || rectangle.contains("fill:#ffffff")
					|| rectangle.contains("fill:rgb(255,255,255)")) {
				white++;
			}
		}

		assertEquals("the SVG still paints white: " + white + " rectangles", 0, white);
	}

	/**
	 * And the glycan is still drawn.
	 *
	 * <p>The way to pass the assertion above is to stop painting altogether, so this holds the other
	 * side: the residues keep their colours.
	 */
	@Test
	public void theGlycanItselfIsStillPainted() {
		String svg = SVGUtils.getVectorGraphics((GlycanRendererAWT) workspace.getGlycanRenderer(),
				structures(), false, true);

		assertTrue("GlcNAc's blue is gone", svg.contains("rgb(0,114,188)"));
		assertTrue("Man's green is gone", svg.contains("rgb(0,166,81)"));
	}

	private static Collection<Glycan> structures() {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		try {
			document.importFromString(
					"freeEnd--?b1D-GlcNAc,p--4b1D-GlcNAc,p--4b1D-Man,p--3a1D-Man,p", "gws");
		} catch (Exception cannotRead) {
			throw new IllegalStateException(cannotRead);
		}

		return document.getStructures();
	}
}
