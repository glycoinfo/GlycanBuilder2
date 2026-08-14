package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That two monosaccharides which CFG tells apart by their interior pattern are not drawn as the same
 * picture (#132).
 *
 * <p>CFG gives Tal vertical stripes and All horizontal ones, and likewise Tag/Psi and
 * TalNAc/AllNAc. The style files say so - {@code v_stripes} and {@code h_stripes} appear in six
 * places - but the renderer had a case for neither, so the fill fell through every branch and
 * nothing was painted inside the outline. Each pair came out as one identical symbol.
 *
 * <p>That is the kind of wrong answer nobody can see: a reader of the figure is not told that two
 * different sugars were drawn the same, and the person who drew it has no reason to look.
 *
 * <p>What is asserted is the drawn image rather than the shape returned, because the shape is only
 * half of it - the renderer clips the fill to the symbol's outline, and a fill that is right but
 * clipped away is still a blank symbol.
 */
public class StripedResiduesAreDrawnApartTest {

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** The three pairs CFG distinguishes only by which way the stripes run. */
	@Test
	public void eachStripedPairIsDrawnDifferently() {
		assertDrawnApart("Tal", "All");
		assertDrawnApart("Tag", "Psi");
		assertDrawnApart("TalNAc", "AllNAc");
	}

	/**
	 * And the stripes are actually painted, rather than the two differing by colour alone.
	 *
	 * <p>Tal and All are green and blue, so an image comparison passes as soon as the colours
	 * differ - even with nothing painted inside either outline. This holds the pattern itself:
	 * drawn in the same colour, a striped symbol still differs from a solid one.
	 */
	@Test
	public void theStripesThemselvesArePainted() {
		BufferedImage striped = drawn("Tal");
		BufferedImage solid = drawn("Glc");

		assertTrue("the striped symbol has no interior pattern",
				distinctColoursIn(striped) > 2);
		assertTrue("a striped symbol should carry more distinct colours than a solid one: "
				+ distinctColoursIn(striped) + " against " + distinctColoursIn(solid),
				distinctColoursIn(striped) >= distinctColoursIn(solid));
	}

	private static void assertDrawnApart(String one, String other) {
		assertNotEquals(one + " and " + other + " are drawn as the same picture",
				fingerprint(drawn(one)), fingerprint(drawn(other)));
	}

	/** The residue on its own, drawn in CFG, where the interior pattern is the distinction. */
	private static BufferedImage drawn(String residue) {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setNotation("cfg");

		GlycanDocument document = workspace.getStructures();
		document.importFromString("freeEnd--?b1D-" + residue + ",p", "gws");
		Glycan structure = document.getStructures().get(0);

		return workspace.getGlycanRenderer().getImage(structure, false, false, true, 4.0);
	}

	/** Every pixel, so a difference anywhere counts. */
	private static String fingerprint(BufferedImage image) {
		StringBuilder pixels = new StringBuilder();
		for (int y = 0; y < image.getHeight(); y++)
			for (int x = 0; x < image.getWidth(); x++)
				pixels.append(image.getRGB(x, y)).append(',');

		return pixels.toString();
	}

	private static int distinctColoursIn(BufferedImage image) {
		java.util.Set<Integer> colours = new java.util.HashSet<>();
		for (int y = 0; y < image.getHeight(); y++)
			for (int x = 0; x < image.getWidth(); x++)
				colours.add(image.getRGB(x, y));

		return colours.size();
	}
}
