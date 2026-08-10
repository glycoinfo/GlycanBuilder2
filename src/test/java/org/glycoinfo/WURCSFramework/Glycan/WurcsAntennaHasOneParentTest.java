package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That an antenna read from WURCS hangs from the bracket and from nothing else (#71).
 *
 * <p>An antenna is a subtree whose attachment point is undetermined: it names the residues it may
 * hang from, and is drawn from the bracket rather than from any one of them. Reading WURCS linked
 * it to the first of the named residues as well - there is no way to choose between them, and the
 * reader chose - so the residue had two parents at once, a shape no tree can hold.</p>
 *
 * <p>Everything that walks the structure then met it twice. The renderer laid it out under the
 * bracket and translated it a second time along with the subtree it was also linked into, which put
 * it outside the picture the renderer had sized; the writers emitted it twice, so G42735RP came
 * back out of WURCS with fourteen residues where it went in with thirteen.</p>
 *
 * <p>The structure here is G42735RP, the one in the report.</p>
 */
public class WurcsAntennaHasOneParentTest {

	/**
	 * G42735RP: a fucosylated N-glycan whose sialic acid may hang from any of three galactoses.
	 *
	 * <p>{@code m1-f?|i?|k?} is the part that matters - residue m, the NeuAc, named against f, i
	 * and k with the position on each of them unknown.</p>
	 */
	private static final String WITH_AN_AMBIGUOUS_ANTENNA =
			"WURCS=2.0/7,13,12/[a2122h-1x_1-5_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O]"
			+ "[a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][a1221m-1a_1-5]"
			+ "[Aad21122h-2a_2-6_5*NCC/3=O]/1-2-3-4-2-5-4-2-5-2-5-6-7"
			+ "/a4-b1_a6-l1_b4-c1_c3-d1_c6-g1_d2-e1_e4-f1_g2-h1_g6-j1_h4-i1_j4-k1"
			+ "_m1-f?|i?|k?}";

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** The antenna is the bracket's child, and nobody else's. */
	@Test
	public void theAntennaHangsFromTheBracketAlone() throws Exception {
		Glycan structure = read(WITH_AN_AMBIGUOUS_ANTENNA);
		Residue bracket = structure.getBracket();

		assertEquals("the bracket should carry the one antenna", 1, bracket.getNoChildren());
		Residue antenna = bracket.getChildAt(0);
		assertSame("the antenna is linked to a residue as well as to the bracket",
				bracket, antenna.getParent());
		assertEquals("the antenna is still a child of a residue in the tree",
				0, timesReachedFrom(structure.getRoot(), antenna));
	}

	/** And it still knows which residues it may hang from, which is what says where to draw it. */
	@Test
	public void theAntennaKeepsTheParentsItMayHangFrom() throws Exception {
		Glycan structure = read(WITH_AN_AMBIGUOUS_ANTENNA);

		Residue antenna = structure.getBracket().getChildAt(0);
		assertEquals("the antenna should name the three residues f, i and k",
				3, antenna.getParentsOfFragment().size());
	}

	/** So the walk meets each residue once, and the writers say the structure once. */
	@Test
	public void theStructureIsWrittenOutAsOneOfEachResidue() throws Exception {
		Glycan structure = read(WITH_AN_AMBIGUOUS_ANTENNA);

		// the thirteen residues of the sequence, plus the free end and the bracket
		assertEquals("a residue is reachable twice, so it will be written twice",
				15, structure.getAllResidues().size());

		String written = writeAs(structure, "wurcs2");
		assertTrue("the antenna came out twice: " + written, written.startsWith("WURCS=2.0/7,13,12/"));
	}

	/** How many times the antenna can be reached by walking down from a residue. */
	private static int timesReachedFrom(Residue from, Residue antenna) {
		if (from == null) return 0;

		int found = 0;
		for (int i = 0; i < from.getNoChildren(); i++) {
			Residue child = from.getChildAt(i);
			if (child == antenna) found++;
			found += timesReachedFrom(child, antenna);
		}

		return found;
	}

	private static Glycan read(String sequence) throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		assertTrue("the WURCS would not import", document.importFromString(sequence, "wurcs2"));

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
