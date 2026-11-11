package org.eurocarbdb.application.glycanbuilder.renderutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The SNFG footnote names a residue from the residue, and so names it the same whichever format the
 * structure was read from (#307).
 *
 * <p>A sedoheptulose read from WURCS was "β-D-Altp", which reads as an altrose, and read from GWS
 * "Sedoheptulose"; its pyranose and furanose had two letters one way and shared one the other. The
 * maintainer's rules (2026-11-10): a ketose by its 2-Carb trivial name where it has one and
 * systematically where it has none; chemically different residues apart, the same together; and one
 * name whatever the format.
 */
public class FootnoteNameTest {

	private static BuilderWorkspace workspace;

	private static final String SED_P = "WURCS=2.0/1,1,0/[ha1222h-2b_2-6]/1/";
	private static final String SED_F = "WURCS=2.0/1,1,0/[ha1222h-2b_2-5]/1/";

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
		workspace.setNotation(GraphicOptions.NOTATION_SNFG);
	}

	/** A ketose with an accepted trivial name: that name, with its ring as the ending (2-Carb-10.2, 5). */
	@Test
	public void aKetoseIsNamedByItsTrivialName() throws Exception {
		assertEquals("β-D-Sedoheptulopyranose", nameOf(wurcs(SED_P)));
		assertEquals("β-D-Sedoheptulofuranose", nameOf(wurcs(SED_F)));
		assertEquals("?-D-Sedoheptulopyranose", nameOf(wurcs("WURCS=2.0/1,1,0/[ha1222h-2x_2-6]/1/")));
		assertEquals("α-D-Ribulofuranose", nameOf(wurcs("WURCS=2.0/1,1,0/[ha22h-2a_2-5]/1/")));
	}

	/** Read from GWS, which keeps no name, the residue is named the same as read from WURCS. */
	@Test
	public void theNameDoesNotDependOnTheFormat() throws Exception {
		for (String wurcs : new String[] { SED_P, SED_F, "WURCS=2.0/1,1,0/[ha22h-2a_2-5]/1/",
				"WURCS=2.0/1,1,0/[a2222m-1b_1-5_3*OC]/1/", "WURCS=2.0/1,1,0/[ad122h-1x_1-5_3*OC]/1/" }) {
			Glycan read = wurcs(wurcs);
			Glycan reopened = Glycan.fromString(read.toString());
			assertEquals(wurcs, nameOf(read), nameOf(reopened));
		}
	}

	/**
	 * A pyranose and a furanose are different residues and get different letters; two of the same
	 * share one - from WURCS and from GWS alike.
	 */
	@Test
	public void lettersFollowTheResidue() throws Exception {
		// Sedp-(2-3)-Sedp, with a Sedf on the first: two pyranoses, one furanose
		Glycan two = wurcs("WURCS=2.0/2,3,2/[ha1222h-2b_2-6][ha1222h-2b_2-5]/1-1-2/a3-b2_a4-c2");
		java.util.List<String> lines = Arrays.asList("A=β-D-Sedoheptulopyranose", "B=β-D-Sedoheptulofuranose");

		assertEquals(lines, footnote(two).lines());
		assertEquals(lines, footnote(Glycan.fromString(two.toString())).lines());
	}

	/** A ketose with no trivial name is named systematically, 2-Carb-10.3 and 10.4. */
	@Test
	public void aKetoseWithNoTrivialNameIsNamedSystematically() throws Exception {
		Residue sed = sugarOf(wurcs(SED_P));
		assertEquals("β-D-altro-Hept-2-ulopyranose", FootnoteName.ketoseSystematically(sed, "ha1222h"));
	}

	/** 2-Carb-8: groups of four from C-1, each D or L by its last centre, the furthest cited first. */
	@Test
	public void configurationalPrefixes() {
		assertEquals("D-gluco", FootnoteName.configurationalPrefixes("a2122h", 1));
		assertEquals("D-galacto", FootnoteName.configurationalPrefixes("a2112h", 1));
		assertEquals("D-manno", FootnoteName.configurationalPrefixes("a1122h", 1));
		assertEquals("L-gluco", FootnoteName.configurationalPrefixes("a1211h", 1));
		assertEquals("D-arabino", FootnoteName.configurationalPrefixes("ha122h", 2));
		assertEquals("D-altro", FootnoteName.configurationalPrefixes("ha1222h", 2));
		assertEquals("L-glycero-D-manno", FootnoteName.configurationalPrefixes("a11221h", 1));
		assertEquals("D-erythro", FootnoteName.configurationalPrefixes("a22h", 1));
		assertEquals(null, FootnoteName.configurationalPrefixes("a2x22h", 1));
	}

	/** Two names that differ only in the anomer are two residues. */
	@Test
	public void anAlphaAndABetaAreTwoNames() throws Exception {
		assertNotEquals(nameOf(wurcs(SED_P)), nameOf(wurcs("WURCS=2.0/1,1,0/[ha1222h-2a_2-6]/1/")));
	}

	private static Glycan wurcs(String _wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(_wurcs, workspace.getDefaultMassOptions());
	}

	private static Residue sugarOf(Glycan _structure) {
		return _structure.getRoot().getChildAt(0);
	}

	private static String nameOf(Glycan _structure) {
		return FootnoteName.of(sugarOf(_structure));
	}

	private static SNFGFootnote footnote(Glycan _structure) {
		return SNFGFootnote.of(_structure, workspace.getGlycanRenderer().getResidueStyleDictionary(),
				workspace.getGraphicOptions());
	}
}
