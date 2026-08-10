package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collections;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.PositionManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.SVGUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That a composition is drawn whether or not the reducing-end marker is shown (#153).
 *
 * <p>A composition has no residue privileged as the reducing end: its members all hang from the
 * bracket and the free end is only a marker. The renderer laid it out from the residue past that
 * marker, which for a composition is nothing at all, so asking for one without the marker gave an
 * empty picture - a blank 1x1 SVG, or a NullPointerException out of the middle of painting when
 * the legend went to measure the box that had never been computed.</p>
 *
 * <p>Whether the marker is <em>drawn</em> is a display preference. Whether the composition is drawn
 * should not follow it.</p>
 */
public class CompositionIsDrawnWithoutTheReducingEndTest {

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** The layout has a size, which is what the picture is cut to. */
	@Test
	public void aCompositionIsLaidOutWithoutTheReducingEnd() throws Exception {
		Rectangle box = layoutOf(composition(), false);

		assertTrue("a composition laid out to nothing: " + box, box.width > 1 && box.height > 1);
	}

	/** And it draws, where it used to throw. */
	@Test
	public void aCompositionIsDrawnWithoutTheReducingEnd() throws Exception {
		BufferedImage image = new GlycanRendererAWT().getImage(composition(), false, false, false, 1.0);

		assertNotNull("nothing was drawn", image);
		assertTrue("the image is empty: " + image.getWidth() + "x" + image.getHeight(),
				image.getWidth() > 1 && image.getHeight() > 1);
	}

	/**
	 * The same picture either way, because there is no reducing end for the marker to mark.
	 *
	 * <p>The composition's free end is never painted - {@code paint} draws the bracket and leaves
	 * the core alone - so the flag has nothing to act on here.</p>
	 */
	@Test
	public void aCompositionIsDrawnTheSameEitherWay() throws Exception {
		assertEquals("a composition is drawn differently depending on the reducing-end marker",
				draw(composition(), true), draw(composition(), false));
	}

	/** An ordinary glycan still minds the marker, which is the point of the preference. */
	@Test
	public void anOrdinaryGlycanStillMindsTheReducingEnd() throws Exception {
		Glycan chain = Glycan.fromString("freeEnd--?b1D-GlcNAc,p--4b1D-Gal,p$MONO,Und,0,0,freeEnd");

		assertNotEquals("the reducing-end marker stopped making any difference",
				draw(chain, true), draw(chain, false));
	}

	/** Two hexoses, unlinked and unordered - the smallest thing that is a composition. */
	private static Glycan composition() throws Exception {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.getCompositionOptions().HEX = 2;
		Glycan composition =
				workspace.getCompositionOptions().getCompositionAsGlycan(workspace.getDefaultMassOptions());
		assertTrue("this was meant to be a composition", composition.isComposition());

		return composition;
	}

	private static Rectangle layoutOf(Glycan structure, boolean showReducingEnd) throws Exception {
		return new GlycanRendererAWT().computeBoundingBoxes(Collections.singletonList(structure),
				false, showReducingEnd, new PositionManager(), new BBoxManager());
	}

	private static String draw(Glycan structure, boolean showReducingEnd) throws Exception {
		return SVGUtils.getVectorGraphics(new GlycanRendererAWT(),
				Collections.singletonList(structure), false, showReducingEnd);
	}
}
