package org.eurocarbdb.application.glycanbuilder.renderutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueStyle;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.junit.Test;

/**
 * A residue's symbol, and the part of it that is filled, are the same whichever way a structure is
 * drawn and wherever the residue's parent is (#83).
 *
 * <p>#83 asked whether CFG's hat diamonds, 4uGlcA and the like and NeuAcLac, turned with their bond.
 * They do not, and have not: the angle they were handed was never used. What did turn was SNFG's
 * divided triangle, a deoxy-HexNAc such as FucNAc: its triangle points up whatever the orientation
 * (#82), but its coloured half followed the orientation - the right half from right to left, the
 * left half from left to right, a band across the top from top to bottom, and a misplaced band from
 * bottom to top. SNFG draws it white on the left and coloured on the right, and asks that a symbol not
 * be turned.
 */
public class SymbolsDoNotTurnTest {

	private static final int[] ORIENTATIONS = { GraphicOptions.RL, GraphicOptions.BT, GraphicOptions.LR, GraphicOptions.TB };

	/** The residue's own box, and a parent box to its right, below, to its left and above. */
	private static final Rectangle CURRENT = new Rectangle(100, 100, 22, 22);
	private static final Rectangle[] PARENTS = { new Rectangle(160, 100, 22, 22), new Rectangle(100, 160, 22, 22),
			new Rectangle(40, 100, 22, 22), new Rectangle(100, 40, 22, 22) };

	/** CFG's hat diamonds keep their outline and their fill. */
	@Test
	public void cfgHatDiamondsDoNotTurn() throws Exception {
		for (String name : new String[] { "4uGlcA", "4uGalA", "4uHexA", "NeuAcLac", "NeuGcLac" })
			assertTheSameEverywhere(GraphicOptions.NOTATION_CFG, name);
	}

	/** SNFG's divided triangles keep their outline, and the coloured half stays on the right. */
	@Test
	public void snfgDividedTrianglesDoNotTurn() throws Exception {
		for (String name : new String[] { "FucNAc", "QuiNAc", "RhaNAc", "6dTalNAc", "6dAltNAc" }) {
			assertTheSameEverywhere(GraphicOptions.NOTATION_SNFG, name);
			Shape fill = drawn(GraphicOptions.NOTATION_SNFG, name, GraphicOptions.RL, PARENTS[0])[1];
			assertEquals(name, new Rectangle(111, 100, 11, 22), fill.getBounds());
		}
	}

	private static void assertTheSameEverywhere(String notation, String name) throws Exception {
		Shape[] first = drawn(notation, name, ORIENTATIONS[0], PARENTS[0]);
		assertNotNull(name + " has no shape in " + notation, first[0]);
		for (int orientation : ORIENTATIONS)
			for (Rectangle parent : PARENTS) {
				Shape[] drawn = drawn(notation, name, orientation, parent);
				String where = name + " in " + notation + ", orientation " + orientation + ", parent at " + parent.x + "," + parent.y;
				assertEquals(where + ": outline", outline(first[0]), outline(drawn[0]));
				assertEquals(where + ": fill", outline(first[1]), outline(drawn[1]));
			}
	}

	/** The symbol's outline and its filled part, as the AWT renderer - and through it the SVG - builds them. */
	private static Shape[] drawn(String notation, String name, int orientation, Rectangle parent) throws Exception {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
		workspace.setNotation(notation);
		GlycanRendererAWT renderer = (GlycanRendererAWT) workspace.getGlycanRenderer();
		GraphicOptions options = renderer.getGraphicOptions();
		options.ORIENTATION = orientation;
		AbstractResidueRenderer residues = (AbstractResidueRenderer) renderer.getResidueRenderer();

		Residue residue = ResidueDictionary.newResidue(name);
		ResidueStyle style = renderer.getResidueStyleDictionary().getStyle(residue);
		ResAngle angle = options.getOrientationAngle();
		Shape outline = residues.createShape(residue, parent, CURRENT, parent, angle, style);
		Shape fill = residues.createFillShape(residue, CURRENT, style, angle, 0);
		return new Shape[] { outline, fill };
	}

	private static List<String> outline(Shape shape) {
		List<String> points = new ArrayList<String>();
		if (shape == null) return points;
		double[] at = new double[6];
		for (PathIterator path = shape.getPathIterator(null); !path.isDone(); path.next()) {
			int segment = path.currentSegment(at);
			points.add(segment + ":" + Math.round(at[0] * 10) + "," + Math.round(at[1] * 10));
		}
		return points;
	}
}
