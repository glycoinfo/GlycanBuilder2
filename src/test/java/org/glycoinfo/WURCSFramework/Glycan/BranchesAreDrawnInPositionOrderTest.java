package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.PositionManager;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That a branch goes where its linkage position says, not where the drawing order left it (#29).
 *
 * <p>Every ordinary monosaccharide is placed straight out — the placement rules that pick a side from
 * the linkage position apply to substituents, and a saccharide child falls through to the catch-all.
 * So all of a residue's branches arrive in one region and are stacked in the order the children are
 * stored, which was the order the sequence listed them or the order somebody drew them in.
 *
 * <p>Attaching a GlcNAc to the β-Man's 4 of a finished biantennary core therefore appended it, and it
 * was stacked last: at the far edge, above the 6-antenna, rather than between the two antennae. The
 * convention is that branches are drawn in the numeric order of their positions, so a bisecting
 * GlcNAc at 4 belongs between the 3- and 6-antennae (I. Yamada, 2026-08-14), which is also what
 * GlycoCraft's layout document states.
 *
 * <p>These assertions are about order along the axis the branches stack on, not about coordinates. A
 * test pinned to y=30 and y=82 would fail on any change to spacing and would say nothing about what
 * had gone wrong.
 */
public class BranchesAreDrawnInPositionOrderTest {

	private static final String RESIDUES =
			"[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5]";

	/** A biantennary N-glycan core: antennae at the β-Man's 3 and 6. */
	private static final String CORE =
			"WURCS=2.0/3,5,4/" + RESIDUES + "/1-1-2-3-3/a4-b1_b4-c1_c3-d1_c6-e1";

	/** The same with a bisecting GlcNAc at 4, stated in the sequence. */
	private static final String BISECTED =
			"WURCS=2.0/3,6,5/" + RESIDUES + "/1-1-2-3-1-3/a4-b1_b4-c1_c3-d1_c4-e1_c6-f1";

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/**
	 * The bisecting GlcNAc sits between the two antennae, in the default orientation.
	 *
	 * <p>Without the fix it is above both of them: measured on 1.37.0, the 6-antenna fell from y=30 to
	 * y=82 and the new GlcNAc took y=30.
	 */
	@Test
	public void aBisectingGlcNAcSitsBetweenTheAntennae() throws Exception {
		Glycan structure = coreWithGlcNAcAddedAfterwards();

		int sixAntenna = topOf(structure, branchAt(structure, '6'));
		int bisecting = topOf(structure, branchAt(structure, '4'));
		int threeAntenna = topOf(structure, branchAt(structure, '3'));

		assertTrue("the 6-antenna should be above the bisecting GlcNAc, and is at " + sixAntenna
				+ " against " + bisecting, sixAntenna < bisecting);
		assertTrue("the bisecting GlcNAc should be above the 3-antenna, and is at " + bisecting
				+ " against " + threeAntenna, bisecting < threeAntenna);
	}

	/**
	 * It is drawn level with the residue it hangs off, which is what "straight out" means.
	 *
	 * <p>GlycoCraft states this as the rule for position 4 and gives the coordinate: the bisecting
	 * GlcNAc on the parent's own line, an antenna above and an antenna below.
	 */
	@Test
	public void aBisectingGlcNAcIsLevelWithItsParent() throws Exception {
		Glycan structure = coreWithGlcNAcAddedAfterwards();

		assertEquals("the bisecting GlcNAc should be on the β-Man's own line",
				topOf(structure, betaMan(structure)), topOf(structure, branchAt(structure, '4')));
	}

	/**
	 * Adding it afterwards gives the same picture as stating it in the sequence.
	 *
	 * <p>This is what the issue reports — that it depends on when you attach it — and it is the whole
	 * of the rule: the picture follows the molecule, not the order it was assembled in.
	 */
	@Test
	public void whenItWasAddedMakesNoDifference() throws Exception {
		Glycan added = coreWithGlcNAcAddedAfterwards();
		Glycan stated = wurcs(BISECTED);

		assertEquals("stating the GlcNAc in the sequence and adding it afterwards draw differently",
				rowsOf(stated), rowsOf(added));
	}

	/**
	 * And a plain biantennary core is where it always was: 6-antenna above, 3-antenna below.
	 *
	 * <p>The way to make the assertions above pass wrongly is to reverse the stacking, which would move
	 * every branch in every structure. This is the half that says nothing else moved.
	 */
	@Test
	public void aPlainCoreIsUnchanged() throws Exception {
		Glycan structure = wurcs(CORE);

		assertTrue("the 6-antenna should be above the 3-antenna",
				topOf(structure, branchAt(structure, '6')) < topOf(structure, branchAt(structure, '3')));
	}

	/** The core as imported, with a GlcNAc then attached to the β-Man's 4 — the path #29 describes. */
	private static Glycan coreWithGlcNAcAddedAfterwards() throws Exception {
		Glycan structure = wurcs(CORE);

		assertTrue("the GlcNAc was refused at the β-Man's 4",
				betaMan(structure).addChild(ResidueDictionary.newResidue("GlcNAc"), '4'));

		return structure;
	}

	/**
	 * Which residue is drawn on which row, so two layouts can be compared as a whole.
	 *
	 * <p>Sorted, because the two structures are walked in different orders — the same picture described
	 * from two directions is the same picture, and comparing the walk would be comparing the wrong
	 * thing.
	 */
	private static List<String> rowsOf(Glycan structure) throws Exception {
		BBoxManager boxes = boxes(structure);

		List<String> rows = new ArrayList<String>();
		for (Residue residue : structure.getAllResidues()) {
			Rectangle box = boxes.getCurrent(residue);
			if (box == null || residue.getParentLinkage() == null) continue;
			rows.add(residue.getTypeName() + "@" + positionOf(residue) + ":" + box.y);
		}
		Collections.sort(rows);

		return rows;
	}

	/** Where the top edge of a residue ends up. */
	private static int topOf(Glycan structure, Residue residue) throws Exception {
		Rectangle box = boxes(structure).getCurrent(residue);
		assertTrue(residue.getTypeName() + " was not laid out", box != null);
		return box.y;
	}

	/**
	 * The branch point: the Man carrying more than one child.
	 *
	 * <p>Found by its children rather than by name and position. The chitobiose core has a GlcNAc at
	 * position 4 as well, and an earlier version of this test picked that one up and compared the wrong
	 * pair of residues — it failed without the change, but for the wrong reason.
	 */
	private static Residue betaMan(Glycan structure) {
		for (Residue residue : structure.getAllResidues())
			if ("Man".equals(residue.getTypeName()) && residue.getNoChildren() >= 2)
				return residue;
		throw new IllegalStateException("no branch point");
	}

	/** The β-Man's child at one of its positions — an antenna, or the bisecting GlcNAc. */
	private static Residue branchAt(Glycan structure, char position) {
		for (Linkage link : betaMan(structure).getChildrenLinkages())
			if (positionOf(link.getChildResidue()) == position)
				return link.getChildResidue();
		throw new IllegalStateException("the branch point has nothing at " + position);
	}

	private static char positionOf(Residue residue) {
		Linkage link = residue.getParentLinkage();
		if (link == null || link.getParentPositions().size() != 1) return '?';
		return link.getParentPositions().iterator().next();
	}

	private static BBoxManager boxes(Glycan structure) throws Exception {
		GlycanRendererAWT renderer = new GlycanRendererAWT();
		BBoxManager boxes = new BBoxManager();
		renderer.computeBoundingBoxes(Collections.singletonList(structure), false, false,
				new PositionManager(), boxes);
		return boxes;
	}

	private static Glycan wurcs(String sequence) throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		assertTrue("the WURCS would not import", document.importFromString(sequence, "wurcs2"));

		return document.getStructures().get(0);
	}
}
