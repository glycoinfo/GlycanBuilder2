package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.PositionManager;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.junit.Test;

/**
 * SNFG symbols for monosaccharides that have none of their own (#248, #254, #255).
 *
 * <p>SNFG's notes: a monosaccharide absent from its Table 1 is drawn as a white symbol carrying a
 * letter, "fitting the generic type if possible ... otherwise, the white pentagon". The white flat
 * hexagon is for an unknown or partially defined monosaccharide, and in colour for the eight named
 * residues of that row.
 *
 * <p>Twenty residue types here have no symbol and the superclass "Unknown" - sixteen heptoses, Sed,
 * Ole, Cym and The - and fell back to the white flat hexagon. None of the heptoses or Sed has a
 * generic shape, so the white pentagon. (Ole, Cym and The do have one, a white triangle or rectangle,
 * and are a separate issue; they are a pentagon now rather than a flat hexagon, and wrong either way.)
 *
 * <p>And GlycoWorkbench's {@code Hept} was in no dictionary at all, so it was not a saccharide: drawn
 * on its parent's border as text, given no letter, and weighed nothing.
 */
public class SnfgAbsentResiduesTest {

	/** The structure in #253, #254 and #255, as GlycoWorkbench wrote it. */
	private static final String HEPTOSES = "freeEnd--5?2KDO--7?1D-Hept,p(((--?Hept--??1D-GlcNAc,p)--??1D-Glc,p"
			+ "--??1D-Glc,p--??1D-Gal,p--??1D-GlcNAc,p)--??1D-Glc,p)--??1PEtn$MONO,Und,H,Na-H,freeEnd";

	/** #248: sedoheptulose, fully defined and absent from SNFG's table, is the white pentagon. */
	@Test
	public void sedIsTheWhitePentagon() throws Exception {
		assertEquals("pentagon", snfgShapeOf("Sed"));
	}

	/** #254: so is every heptose without a symbol of its own. */
	@Test
	public void heptosesWithoutASymbolAreTheWhitePentagon() throws Exception {
		for (String heptose : new String[] { "D-gro-D-galHep", "L-gro-D-galHep", "D-gro-D-glcHep",
				"L-gro-D-idoHep", "manHep" })
			assertEquals(heptose, "pentagon", snfgShapeOf(heptose));
	}

	/**
	 * #254 as reported, on what is drawn: both of GlycoWorkbench's Hept residues are pentagons.
	 *
	 * <p>Counted from the contours the renderer draws, because for Hept the pentagon is not in the style
	 * file - its superclass, Heptose, has no row - but is the renderer's own white fallback for a
	 * saccharide with no shape. Before, there were none: one Hept was text on KDO's border and the
	 * other a flat hexagon.
	 */
	@Test
	public void bothHeptsAreDrawnAsPentagons() throws Exception {
		assertEquals(2, pentagonsDrawn(HEPTOSES, "gws"));
	}

	/** #248 on what is drawn: G63532WE's two Sed and its thrHexA. It drew one, the thrHexA. */
	@Test
	public void g63532weDrawsThreePentagons() throws Exception {
		assertEquals(3, pentagonsDrawn(G63532WE, "wurcs2"));
	}

	/**
	 * And the eight residues SNFG names in its flat-hexagon row keep their own coloured flat hexagons.
	 *
	 * <p>The guard on the change: it altered the fallback for residues with no row of their own, and
	 * these have one each.
	 */
	/**
	 * The Unknown monosaccharide itself is SNFG's white flat hexagon. #280 had made it the pentagon, so
	 * that what is filed under Unknown would fall back to one; that falls back to the Assigned row now
	 * (#288), and the residues above are still pentagons.
	 */
	@Test
	public void unknownItselfIsTheWhiteFlatHexagon() throws Exception {
		assertEquals("flathexagon", snfgShapeOf("Unknown"));
		assertEquals("pentagon", snfgShapeOf("D-gro-D-galHep"));
	}

	@Test
	public void theNamedFlatHexagonsAreUnchanged() throws Exception {
		for (String named : new String[] { "Bac", "L-gro-D-manHep", "Kdo", "Dha", "D-gro-D-manHep",
				"MurNAc", "MurNGc", "Mur" }) {
			assertTrue(named + " has lost its own style row", styles().containsResidue(alone(named)));
			assertEquals(named, "flathexagon", snfgShapeOf(named));
		}
	}

	/** The style file defines Unknown once. It defined it twice, and only the later row was ever read. */
	@Test
	public void unknownIsDefinedOnce() throws Exception {
		int rows = 0;
		java.io.InputStream in = getClass().getResourceAsStream("/conf/residue_styles_snfg");
		assertNotNull(in);
		try (java.io.BufferedReader reader = new java.io.BufferedReader(
				new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
			for (String line; (line = reader.readLine()) != null; )
				if (line.startsWith("Unknown\t")) rows++;
		}
		assertEquals(1, rows);
	}

	/** #254/#255: Hept is a saccharide, and so it is laid out in the chain, not on its parent's border. */
	@Test
	public void heptIsASaccharideInTheChain() throws Exception {
		BuilderWorkspace workspace = snfgWorkspace();
		Glycan structure = read(workspace, HEPTOSES);

		List<Residue> hepts = named(structure, "Hept");
		assertEquals("both heptoses", 2, hepts.size());
		for (Residue hept : hepts)
			assertTrue("Hept is not a saccharide", hept.isSaccharide());

		PositionManager positions = new PositionManager();
		workspace.getGlycanRenderer().computeBoundingBoxes(java.util.Collections.singletonList(structure),
				true, true, positions, new BBoxManager());

		// the one attached to KDO is the one that was drawn as "7Hept" on KDO's border
		Residue onKdo = hepts.get(0).getParent().getTypeName().equals("Kdo") ? hepts.get(0) : hepts.get(1);
		assertFalse("the heptose on KDO is placed on KDO's border, like a substituent", positions.isOnBorder(onKdo));

		// and PEtn, which is a substituent, still is - the guard that this did not move everything
		List<Residue> petn = named(structure, "PEtn");
		assertEquals(1, petn.size());
		assertTrue("PEtn is no longer placed on its parent's border", positions.isOnBorder(petn.get(0)));
	}

	/**
	 * And it weighs something.
	 *
	 * <p>Worked by hand, residue masses: Kdo C8H12O7 220.0583, two heptoses C7H12O6 2 x 192.0634, four
	 * hexoses C6H10O5 4 x 162.0528, two GlcNAc C8H13NO5 2 x 203.0794, PEtn 123.0085, water 18.0106 -
	 * 1799.5743 neutral. The ions are H with one Na-for-H exchange: + 1.0073 + 21.9819 = 1822.5635.
	 *
	 * <p>It read 1402.4155: 420.15 Da light, two C7H14O7, the two heptoses counted as nothing.
	 */
	@Test
	public void theHeptosesHaveMass() throws Exception {
		Glycan structure = read(snfgWorkspace(), HEPTOSES);

		assertEquals(1822.5635, structure.computeMZ(), 1e-3);
	}

	/** And the name survives a save: a GlycoWorkbench file still says Hept after GlycanBuilder2 writes it. */
	@Test
	public void heptIsWrittenBackAsHept() throws Exception {
		String written = read(snfgWorkspace(), HEPTOSES).toString();

		assertTrue("Hept was respelled on writing: " + written, written.contains("7?1D-Hept,p"));
		assertTrue("Hept was respelled on writing: " + written, written.contains("--?Hept--"));
	}

	/** G63532WE, as GlyCosmos serves it - the structure #248 was reported against. */
	private static final String G63532WE = "WURCS=2.0/14,16,15/[a2122h-1a_1-5_2*N]"
			+ "[a2122h-1b_1-5_2*N_4*OPO/3O/3=O][Aad1122h-2a_2-6_4*OPO/3O/3=O][a11221h-1a_1-5][a2112h-1b_1-5]"
			+ "[ha1222h-2b_2-6][a2122m-1b_1-5_2*N][a2112A-1b_1-5][Aad21122h-2a_2-6_5*N][a21EEA-1b_1-5]"
			+ "[a2122h-1a_1-?_2*N][a2122h-1b_1-5][ha1222h-2b_2-5][a2122h-1a_1-5]"
			+ "/1-2-3-4-4-4-5-6-7-8-9-10-11-12-13-14/a6-b1_b6-c2_c5-d1_d3-e1_d4-n1_d6-p1_e2-f1_f3-g1_f7-m1"
			+ "_g3-h2_h4-i1_i3-j1_j3-k2_k4-l1_n6-o2";

	// ------------------------------------------------------------------------------------------------

	/**
	 * How many distinct pentagons the renderer draws for this structure in SNFG.
	 *
	 * <p>Painted the way every export paints - lay out, then paint - into a Batik graphics context that
	 * records each contour drawn. A symbol's outline can be drawn more than once, so pentagons are
	 * counted by where they are, not by how often.
	 */
	private static int pentagonsDrawn(String sequence, String format) throws Exception {
		BuilderWorkspace w = snfgWorkspace();
		GlycanDocument document = w.getStructures();
		assertTrue(document.importFromString(sequence, format));
		List<Glycan> structures = document.getStructures();

		GlycanRendererAWT renderer = (GlycanRendererAWT) w.getGlycanRenderer();
		PositionManager positions = new PositionManager();
		BBoxManager boxes = new BBoxManager();
		renderer.computeBoundingBoxes(structures, false, true, positions, boxes);

		Outlines g2d = new Outlines();
		for (Glycan structure : structures)
			renderer.paint(new org.eurocarbdb.application.glycanbuilder.DefaultPaintable(g2d), structure,
					null, null, false, true, positions, boxes);
		return g2d.pentagons.size();
	}

	/** Records the bounds of every closed five-cornered outline drawn. */
	private static final class Outlines extends org.apache.batik.svggen.SVGGraphics2D {
		final java.util.Set<java.awt.Rectangle> pentagons = new java.util.HashSet<java.awt.Rectangle>();

		Outlines() {
			super(org.apache.batik.dom.GenericDOMImplementation.getDOMImplementation()
					.createDocument(null, "svg", null));
		}

		@Override
		public void draw(java.awt.Shape shape) {
			if (corners(shape) == 5) this.pentagons.add(shape.getBounds());
			super.draw(shape);
		}

		/**
		 * The corners of a closed outline made of straight lines, or -1 for anything else.
		 *
		 * <p>Counted as distinct points: a polygon may return to its first point explicitly before closing,
		 * which would make a square look five-cornered.
		 */
		private static int corners(java.awt.Shape shape) {
			java.util.Set<java.awt.Point> points = new java.util.HashSet<java.awt.Point>();
			boolean closed = false;
			double[] c = new double[6];
			for (java.awt.geom.PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
				switch (it.currentSegment(c)) {
				case java.awt.geom.PathIterator.SEG_MOVETO:
				case java.awt.geom.PathIterator.SEG_LINETO:
					points.add(new java.awt.Point((int) Math.round(c[0]), (int) Math.round(c[1])));
					break;
				case java.awt.geom.PathIterator.SEG_CLOSE:
					closed = true;
					break;
				default:
					return -1;
				}
			}
			return closed ? points.size() : -1;
		}
	}

	private static BuilderWorkspace workspace;

	private static BuilderWorkspace snfgWorkspace() {
		BuilderWorkspace w = new BuilderWorkspace(new GlycanRendererAWT());
		w.setAutoSave(false);
		w.setNotation(GraphicOptions.NOTATION_SNFG);
		workspace = w;
		return w;
	}

	private static ResidueStyleDictionary styles() {
		if (workspace == null) snfgWorkspace();
		return workspace.getGlycanRenderer().getResidueStyleDictionary();
	}

	/** The shape SNFG draws for this residue: its own row's, or the fallback's. */
	private static String snfgShapeOf(String typeName) throws Exception {
		snfgWorkspace();
		return styles().getStyle(alone(typeName)).getShape();
	}

	private static Residue alone(String typeName) throws Exception {
		return ResidueDictionary.newResidue(typeName);
	}

	private static Glycan read(BuilderWorkspace w, String gws) throws Exception {
		GlycanDocument document = w.getStructures();
		assertTrue("could not read: " + gws, document.importFromString(gws, "gws"));
		return document.getStructures().get(0);
	}

	private static List<Residue> named(Glycan structure, String typeName) {
		List<Residue> found = new ArrayList<Residue>();
		for (Residue r : structure.getAllResidues())
			if (r.getTypeName().equals(typeName)) found.add(r);
		return found;
	}

}
