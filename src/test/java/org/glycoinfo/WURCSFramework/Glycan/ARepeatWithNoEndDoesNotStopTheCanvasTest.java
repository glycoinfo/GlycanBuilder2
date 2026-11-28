package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanCanvas;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ThemeManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.Before;
import org.junit.Test;

/**
 * A structure that cannot be weighed or drawn does not stop the canvas (#267).
 *
 * <p>Before #269, #267's WURCS was read into a repeat with a start and no end. The canvas writes the mass
 * under every structure, the mass threw for a repeat with no end, and the exception left paintComponent
 * halfway, at every repaint: the window stopped refreshing, as #96 had. The WURCS is read whole now; a
 * repeat with no end - however one comes about - weighs ??? like a repeat of unknown count, and a
 * structure that cannot be drawn is said on the canvas rather than stopping it.
 */
public class ARepeatWithNoEndDoesNotStopTheCanvasTest {

	private BuilderWorkspace workspace;
	private GlycanRendererAWT renderer;

	@Before
	public void open() {
		renderer = new GlycanRendererAWT();
		workspace = new BuilderWorkspace(renderer);
		workspace.setAutoSave(false);
	}

	/** What #267's WURCS was read into before #269: a Glc, and a repeat of Glc with a start and no end. */
	private static Glycan aRepeatWithNoEnd() throws Exception {
		return Glycan.fromString("freeEnd--1?1D-Glc,p--6[--4b1D-Glc,p--3b1D-Glc,p$MONO,Und,0,0,freeEnd");
	}

	/** #267's own WURCS is read whole: its repeat has an end. */
	@Test
	public void theReportedWurcsIsReadWhole() throws Exception {
		GlycanDocument document = workspace.getStructures();
		assertTrue(document.importFromString(
				"WURCS=2.0/2,3,3/[a2122h-1x_1-?][a2122h-1b_1-5]/1-2-2/b1-a4_c1-b3_b1-b6~n", "wurcs2"));
		boolean ends = false;
		for (Residue residue : document.getStructures().iterator().next().getAllResidues())
			if (residue.isEndRepetition()) ends = true;
		assertTrue("the repeat has no end", ends);
	}

	/** A repeat with no end has no mass, as one of unknown count has none, and is drawn with ??? under it. */
	@Test
	public void aRepeatWithNoEndWeighsNothingAndIsDrawn() throws Exception {
		Glycan repeat = aRepeatWithNoEnd();
		assertEquals(-1.0, repeat.computeMass(), 0.0);
		BufferedImage drawn = renderer.getImage(new LinkedList<Glycan>(Collections.singletonList(repeat)), true, true, true);
		assertTrue(drawn.getWidth() > 0);
	}

	/**
	 * The canvas paints with structures it cannot draw, and says so on itself: thrown out of
	 * paintComponent, the failure left the repaint halfway, and came again at every one.
	 */
	@Test
	public void theCanvasGoesOnPainting() throws Exception {
		// a canvas needs a display; on a headless build machine there is none to paint on
		org.junit.Assume.assumeFalse("no display", java.awt.GraphicsEnvironment.isHeadless());
		GlycanRendererAWT failing = new GlycanRendererAWT() {
			@Override
			public java.awt.Rectangle computeBoundingBoxes(java.util.Collection<Glycan> _structures, boolean _masses,
					boolean _redend, org.eurocarbdb.application.glycanbuilder.renderutil.PositionManager _positions,
					org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager _boxes) {
				throw new IllegalStateException("cannot be drawn");
			}
		};
		BuilderWorkspace own = new BuilderWorkspace(failing);
		own.setAutoSave(false);
		own.getStructures().setStructures(Collections.singletonList(aRepeatWithNoEnd()));
		GlycanCanvas canvas = new GlycanCanvas(null, own,
				new ThemeManager(null, ARepeatWithNoEndDoesNotStopTheCanvasTest.class), false);
		canvas.setSize(400, 300);

		BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D graphics = image.createGraphics();
		canvas.paint(graphics);   // threw
		canvas.paint(graphics);   // and at every repaint
		graphics.dispose();

		boolean said = false;
		for (int x = 0; x < image.getWidth() && !said; x++)
			for (int y = 0; y < 40 && !said; y++)
				if ((image.getRGB(x, y) & 0xFFFFFF) == 0xFF0000) said = true;
		assertTrue("nothing said on the canvas", said);
	}
}
