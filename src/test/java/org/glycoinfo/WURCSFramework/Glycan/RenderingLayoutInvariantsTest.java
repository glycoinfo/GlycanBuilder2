package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.PositionManager;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * What has to be true of any layout, whatever it looks like.
 *
 * <p>The drawing issues left open - #58, #71, #29, #88 - all want changes in the middle of the
 * renderer, where a change meant to help one structure can quietly spoil another, and there was no
 * way to tell. Freezing the SVG of a set of structures would tell, but it would also fail on every
 * intentional improvement and say nothing about what went wrong: twelve kilobytes of path data
 * differing in the fourth decimal.</p>
 *
 * <p>These are the properties instead. They hold for a well-formed layout regardless of where it
 * puts things, so an improvement passes them and a mistake does not:</p>
 *
 * <ul>
 *   <li>every residue lies within the bounding box the renderer reports - what is drawn outside it
 *       is clipped out of the image, which is how #71 shows itself;</li>
 *   <li>no two residues share a top-left corner - two symbols on the same spot are unreadable, and
 *       are a sign that one of them was never placed.</li>
 * </ul>
 */
public class RenderingLayoutInvariantsTest {

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** A plain chain. */
	@Test
	public void aChainIsLaidOutWithin() throws Exception {
		assertLaidOutWithin(gws("freeEnd--?b1D-GlcNAc,p--4b1D-Gal,p$MONO,Und,0,0,freeEnd"));
	}

	/** A branch. */
	@Test
	public void anNGlycanCoreIsLaidOutWithin() throws Exception {
		assertLaidOutWithin(gws("freeEnd--?b1D-GlcNAc,p--4b1D-GlcNAc,p--4b1D-Man,p"
				+ "(--3a1D-Man,p)--6a1D-Man,p$MONO,Und,0,0,freeEnd"));
	}

	/** A substituent, which is drawn beside its sugar rather than as a symbol of its own. */
	@Test
	public void aSubstituentIsLaidOutWithin() throws Exception {
		assertLaidOutWithin(gws("freeEnd--?b1D-Glc,p--2?1N--??1Ac$MONO,Und,0,0,freeEnd"));
	}

	/** A bridge, which sits in the middle of a linkage. */
	@Test
	public void aBridgeIsLaidOutWithin() throws Exception {
		assertLaidOutWithin(gws("freeEnd--?b1D-Glc,p--6?1P--1b1D-Gal,p$MONO,Und,0,0,freeEnd"));
	}

	/** A reduced structure. */
	@Test
	public void anAlditolIsLaidOutWithin() throws Exception {
		assertLaidOutWithin(gws("redEnd--?b1D-Glc,o$MONO,Und,0,0,redEnd"));
	}

	/** A bracket with antennae - the shape #71 is about, in a structure that gets it right. */
	@Test
	public void aBracketWithAntennaeIsLaidOutWithin() throws Exception {
		assertLaidOutWithin(wurcs(
				"WURCS=2.0/5,12,11/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5]"
				+ "[a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O]/1-1-2-3-1-4-1-4-3-1-4-5"
				+ "/a4-b1_b4-c1_c3-d1_c6-i1_d2-e1_d4-g1_e4-f1_g4-h1_i2-j1_j4-k1"
				+ "_l2-a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?}"));
	}

	/**
	 * A repeating unit - G03246MZ, which #57 was about.
	 *
	 * <p>A repeat was tried by hand first and would not do: the sequence read without complaint and
	 * laid nothing out at all, so the test passed on an empty picture. This is a structure from the
	 * tracker, which is what makes it worth having.</p>
	 */
	@Test
	public void aRepeatIsLaidOutWithin() throws Exception {
		assertLaidOutWithin(wurcs(
				"WURCS=2.0/7,15,15/[h2122h_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5]"
				+ "[a1122h-1a_1-5][a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O][a1221m-1a_1-5]"
				+ "/1-2-3-4-2-5-6-2-5-6-4-2-5-6-7"
				+ "/a4-b1_a6-o1_b4-c1_c3-d1_c6-k1_d2-e1_d4-h1_e4-f1_f3-g2_h4-i1_i3-j2"
				+ "_k2-l1_l4-m1_m3-n2_l1-m3~n"));
	}

	/**
	 * G07957FT, which #58 says is drawn with badly positioned links.
	 *
	 * <p>Its links do cross, and these properties do not speak to that - crossing lines are a
	 * matter of where things are put, not of whether they are inside the picture. It is here so
	 * that a change made for #58 has to keep the rest true.</p>
	 */
	@Test
	public void theStructureOfIssue58IsAtLeastLaidOutWithin() throws Exception {
		assertLaidOutWithin(wurcs(
				"WURCS=2.0/7,9,8/[a2122h-1x_1-5_2*N][Aad1122h-2x_2-6][axxxxxh-1x_1-5]"
				+ "[a2122h-1x_1-5][a2112h-1x_1-5][axxxxxh-1x_1-5_6*OP^XOCCN/3O/3=O]"
				+ "[a2122h-1x_1-5_2*NCC/3=O]/1-2-3-4-5-5-6-7-2"
				+ "/a?-b2_b?-c1_b?-i2_c?-d1_c?-g1_d?-e1_e?-f1_g?-h1"));
	}

	/**
	 * G42735RP, the structure of #71, which breaks both properties today.
	 *
	 * <p>Two residues - an antenna's sialic acid, at linkage position 1 - are placed on the same
	 * spot, outside the bounding box the renderer reports, and so are cut off by the edge of the
	 * image. They never reach the list the bracket layout aligns and measures, so nothing sizes the
	 * picture to include them.</p>
	 *
	 * <p>This test states the fault rather than the fix, so that the fault cannot quietly get
	 * worse. <b>When #71 is fixed this test will fail</b> - that is what it is for. Move the
	 * structure to a plain {@code assertLaidOutWithin} then, and delete this one.</p>
	 */
	@Test
	public void theStructureOfIssue71IsStillDrawnOutsideItsOwnBox() throws Exception {
		Glycan structure = wurcs(
				"WURCS=2.0/7,13,12/[a2122h-1x_1-5_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O]"
				+ "[a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][a1221m-1a_1-5]"
				+ "[Aad21122h-2a_2-6_5*NCC/3=O]/1-2-3-4-2-5-4-2-5-2-5-6-7"
				+ "/a4-b1_a6-l1_b4-c1_c3-d1_c6-g1_d2-e1_e4-f1_g2-h1_g6-j1_h4-i1_j4-k1"
				+ "_m1-f?|i?|k?}");

		assertEquals("#71 looks fixed: no residue falls outside the box any more", 2, outside(structure));
		assertEquals("#71 looks fixed: no two residues share a spot any more", 1, sharedSpots(structure));
	}

	/** Everything drawn is inside the box, and no two symbols sit on the same spot. */
	private static void assertLaidOutWithin(Glycan structure) throws Exception {
		assertEquals("residues fall outside the bounding box, and are clipped from the image",
				0, outside(structure));
		assertEquals("two residues are drawn on the same spot", 0, sharedSpots(structure));
	}

	/** How many residues lie outside the bounding box the renderer reports for the structure. */
	private static int outside(Glycan structure) throws Exception {
		Layout layout = layoutOf(structure);

		int escaped = 0;
		for (Rectangle each : layout.rectangles.values()) {
			if (!layout.all.contains(each)) escaped++;
		}

		return escaped;
	}

	/** How many spots carry more than one residue. */
	private static int sharedSpots(Glycan structure) throws Exception {
		Layout layout = layoutOf(structure);

		Map<String, Integer> perSpot = new HashMap<String, Integer>();
		int shared = 0;
		for (Rectangle each : layout.rectangles.values()) {
			String spot = each.x + "," + each.y;
			Integer now = perSpot.merge(spot, 1, Integer::sum);
			if (now == 2) shared++;
		}

		return shared;
	}

	/** Where the renderer puts each residue, and the box it says it used. */
	private static Layout layoutOf(Glycan structure) throws Exception {
		GlycanRendererAWT renderer = new GlycanRendererAWT();
		PositionManager positions = new PositionManager();
		BBoxManager boxes = new BBoxManager();
		Rectangle all = renderer.computeBoundingBoxes(
				Collections.singletonList(structure), true, false, positions, boxes);

		Layout layout = new Layout();
		layout.all = all;
		int at = 0;
		for (Residue residue : structure.getAllResidues()) {
			Rectangle rectangle = boxes.getCurrent(residue);
			// Keyed by order rather than by residue: the same residue can appear twice in the walk,
			// and both appearances are drawn.
			if (rectangle != null) layout.rectangles.put(at++, rectangle);
		}
		assertTrue("nothing was laid out at all", !layout.rectangles.isEmpty());

		return layout;
	}

	private static final class Layout {
		private Rectangle all;
		private final Map<Integer, Rectangle> rectangles = new HashMap<Integer, Rectangle>();
	}

	private static Glycan gws(String sequence) throws Exception {
		return Glycan.fromString(sequence);
	}

	private static Glycan wurcs(String sequence) throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		assertTrue("the WURCS would not import", document.importFromString(sequence, "wurcs2"));

		return document.getStructures().get(0);
	}
}
