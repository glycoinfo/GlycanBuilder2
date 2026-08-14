package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import org.eurocarbdb.application.glycanbuilder.renderutil.ResidueRendererAWT;
import org.junit.Test;

/**
 * That a label is cleared with the same room on every side (#88).
 *
 * <p>The cleared area used to be the glyphs' bounds cast to int, which truncates the origin one way
 * and the width the other — so a label came out with about a pixel of room on its left and none on
 * its right. On a phosphate bridge the P then touched the edge of the linkage and read as part of
 * it.
 */
public class ABridgeLabelHasRoomTest {

	/** Room on all four sides, whatever fraction of a pixel the glyphs happen to start on. */
	@Test
	public void thereIsRoomOnEverySide() {
		for (double offset : new double[] { 0.0, 0.1, 0.5, 0.9 }) {
			Rectangle2D.Double glyphs = new Rectangle2D.Double(10 + offset, 20 + offset, 7.4, 9.6);
			Rectangle cleared = ResidueRendererAWT.clearanceAround(glyphs);

			assertTrue("nothing cleared to the left at offset " + offset,
					cleared.getX() < glyphs.getX());
			assertTrue("nothing cleared above at offset " + offset,
					cleared.getY() < glyphs.getY());
			assertTrue("nothing cleared to the right at offset " + offset,
					cleared.getMaxX() > glyphs.getMaxX());
			assertTrue("nothing cleared below at offset " + offset,
					cleared.getMaxY() > glyphs.getMaxY());
		}
	}

	/**
	 * And the room is even, which is what the report was about.
	 *
	 * <p>It is the asymmetry that made the P look attached: one side had a pixel and the other had
	 * nothing, so the label sat against the linkage on the right.
	 */
	@Test
	public void theRoomIsTheSameOnBothSides() {
		Rectangle2D.Double glyphs = new Rectangle2D.Double(10.6, 20.6, 7.4, 9.6);
		Rectangle cleared = ResidueRendererAWT.clearanceAround(glyphs);

		double left = glyphs.getX() - cleared.getX();
		double right = cleared.getMaxX() - glyphs.getMaxX();

		assertTrue("left " + left + " against right " + right, Math.abs(left - right) <= 1.0);
	}
}
