package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.SVGUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That a glycan is drawn the same whether it was read from WURCS or from GlycoCT (#62).
 *
 * <p>An antenna - a subtree whose attachment point is undetermined - names the residues it may hang
 * from. WURCS carries them, GlycoCT states them in its UND section's ParentIDs, and reading GlycoCT
 * dropped them: the antenna arrived knowing of no parents at all. The renderer asks exactly that
 * ({@code getParentsOfFragment().isEmpty()}) when it decides whether to draw a link towards the
 * bracket, so the same glycan came out with the link from one sequence and without it from the
 * other.</p>
 *
 * <p>The structure here is G00955WX, the one in the report, reduced to what shows the fault: a
 * bracketed antenna over a small core.</p>
 */
public class GlycoCtKeepsAntennaParentsTest {

	/**
	 * G00955WX itself: a biantennary N-glycan with a sialic acid whose attachment is undetermined,
	 * so it is drawn from the bracket and may hang from any of the eleven residues before it.
	 *
	 * <p>A smaller structure was tried first and would not do: with one Fuc over a short core the
	 * WURCS side names no parents either, so the two routes agree by both being empty and the test
	 * proves nothing. The assertion that the WURCS side has parents is there to keep it honest.</p>
	 */
	private static final String WITH_AN_ANTENNA =
			"WURCS=2.0/5,12,11/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5]"
			+ "[a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O]/1-1-2-3-1-4-1-4-3-1-4-5"
			+ "/a4-b1_b4-c1_c3-d1_c6-i1_d2-e1_d4-g1_e4-f1_g4-h1_i2-j1_j4-k1"
			+ "_l2-a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?}";

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** The antenna knows what it may hang from after a trip through GlycoCT. */
	@Test
	public void theAntennaKeepsItsParentsThroughGlycoCt() throws Exception {
		Glycan fromWurcs = read(WITH_AN_ANTENNA, "wurcs2");

		Glycan fromGlycoCt = read(writeAs(fromWurcs, "glycoct_condensed"), "glycoct_condensed");

		assertEquals("the antenna lost its parents on the way through GlycoCT",
				parentsOfFragmentCount(fromWurcs), parentsOfFragmentCount(fromGlycoCt));
		assertTrue("the WURCS side has no antenna parents, so this proves nothing",
				parentsOfFragmentCount(fromWurcs) > 0);
	}

	/** And so it is drawn the same, which is what the report is about. */
	@Test
	public void bothRoutesDrawTheSamePicture() throws Exception {
		Glycan fromWurcs = read(WITH_AN_ANTENNA, "wurcs2");
		Glycan fromGlycoCt = read(writeAs(fromWurcs, "glycoct_condensed"), "glycoct_condensed");

		assertEquals("the two routes draw different pictures", draw(fromWurcs), draw(fromGlycoCt));
	}

	/** How many residues in the structure name a parent they may hang from. */
	private static int parentsOfFragmentCount(Glycan structure) {
		int named = 0;
		for (Residue residue : structure.getAllResidues()) {
			if (!residue.getParentsOfFragment().isEmpty()) named++;
		}

		return named;
	}

	private static String draw(Glycan structure) throws Exception {
		List<Glycan> one = Collections.singletonList(structure);

		return SVGUtils.getVectorGraphics(new GlycanRendererAWT(), one, false, false);
	}

	private static Glycan read(String sequence, String format) throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		assertTrue(format + " would not import", document.importFromString(sequence, format));

		return document.getStructures().get(0);
	}

	private static String writeAs(Glycan structure, String format) {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		document.addStructure(structure);
		document.exportFromStructure(document.getStructures(), format);
		assertTrue("nothing came out as " + format, !document.getString().isEmpty());

		return document.getString().get(0);
	}
}
