package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A deoxy a residue's name does not imply stays on the residue (#258, #263).
 *
 * <p>The WURCS reader built it as a child residue named "m" or "d". No dictionary knows either name,
 * so the child was a non-saccharide of no composition: drawn beside the symbol as "6m" or "2d",
 * weighed as nothing while the bond to it still took away a water - 2.0157 Da light, two hydrogens -
 * and refused by the WURCS writer. The residue it hung off kept its type's symbol, so a 6-deoxy allose
 * was drawn as the violet allose circle.
 *
 * <p>Now the deoxy is recorded on the residue. Its composition is the type's less what WURCSFramework
 * says a deoxy removes; its derivatizable hydroxyls are one fewer per deoxy; it has no symbol of its
 * own, so SNFG draws it white with a letter, and its footnote names it. GWS has no way to say it except
 * as the child it used to be, so that is how GWS writes it, and the reader takes it back - the file is
 * the same text whichever version wrote it.
 */
public class DeoxyStaysOnTheResidueTest {

	/** #258: a 3-O-methyl 6-deoxy allose. 6dAll is not in SNFG's table. */
	private static final String DEOXY_ALLOSE = "WURCS=2.0/1,1,0/[a2222m-1b_1-5_3*OC]/1/";
	/** #263, from #260: a 3-O-methyl 2-deoxy arabino-hexose. */
	private static final String DEOXY_ARAHEX = "WURCS=2.0/1,1,0/[ad122h-1x_1-5_3*OC]/1/";
	/** A 3-O-methyl fucose: the same composition and the same hydroxyls, with the deoxy in the name. */
	private static final String METHYL_FUCOSE = "WURCS=2.0/1,1,0/[a1221m-1a_1-5_3*OC]/1/";

	/** What 1.40.0 wrote to GWS for these two, and what is written now. */
	private static final String DEOXY_ALLOSE_GWS = "freeEnd--1b1D-All,p(--3?1Me)--6m$MONO,Und,0,0,freeEnd";
	private static final String DEOXY_ARAHEX_GWS = "freeEnd--1?1D-araHex,p(--3?1Me)--2d$MONO,Und,0,0,freeEnd";

	/** C7H14O5, worked by hand: C 12, H 1.00782503207, O 15.99491461956. */
	private static final double C7H14O5 = 178.0841;

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	@Test
	public void theDeoxyIsOnTheResidueNotAChild() throws Exception {
		Residue allose = sugarOf(wurcs(DEOXY_ALLOSE));
		assertEquals("All", allose.getTypeName());
		assertEquals(Arrays.asList("6*m"), allose.getDeoxies());
		assertEquals("only the methyl is a child: " + childNames(allose), Arrays.asList("Me"), childNames(allose));

		Residue araHex = sugarOf(wurcs(DEOXY_ARAHEX));
		assertEquals(Arrays.asList("2*d"), araHex.getDeoxies());
		assertEquals(Arrays.asList("Me"), childNames(araHex));
	}

	/** It was 176.0685 for both: the deoxy child weighed nothing and its bond took a water. */
	@Test
	public void theMassIsRight() throws Exception {
		assertEquals(C7H14O5, wurcs(DEOXY_ALLOSE).computeMass(), 1e-4);
		assertEquals(C7H14O5, wurcs(DEOXY_ARAHEX).computeMass(), 1e-4);
	}

	/**
	 * And derivatized, the deoxy is a hydroxyl that is not there.
	 *
	 * <p>Checked against the dictionary rather than against arithmetic: 3-O-methyl fucose has the same
	 * composition and the same free hydroxyls as 3-O-methyl 6-deoxy allose, with its deoxy in its name,
	 * so the two must weigh the same however they are derivatized.
	 */
	@Test
	public void derivatizedItWeighsTheSameAsAFucoseOfTheSameShape() throws Exception {
		for (String derivatization : new String[] { MassOptions.NO_DERIVATIZATION, MassOptions.PERMETHYLATED,
				MassOptions.PERACETYLATED }) {
			Glycan allose = wurcs(DEOXY_ALLOSE);
			Glycan fucose = wurcs(METHYL_FUCOSE);
			allose.getMassOptions().setDerivatization(derivatization);
			fucose.getMassOptions().setDerivatization(derivatization);

			assertEquals(derivatization, fucose.computeMass(), allose.computeMass(), 1e-4);
		}
	}

	/** #258: a 6-deoxy allose is not an allose, so it does not take the allose's symbol. */
	@Test
	public void itHasNoSymbolOfItsOwn() throws Exception {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setNotation(GraphicOptions.NOTATION_SNFG);
		Residue allose = sugarOf(wurcs(DEOXY_ALLOSE));

		assertFalse(workspace.getGlycanRenderer().getResidueStyleDictionary().containsResidue(allose));
		// and a plain allose still has it - the guard that this did not take symbols away
		assertTrue(workspace.getGlycanRenderer().getResidueStyleDictionary().containsResidue(
				sugarOf(wurcs("WURCS=2.0/1,1,0/[a2222h-1b_1-5]/1/"))));
	}

	/**
	 * #263: the footnote names the monosaccharide, with its deoxy, and not the substituent.
	 *
	 * <p>The methyl is drawn beside the symbol as "3Me". Named in the footnote too it was said twice.
	 */
	@Test
	public void theFootnoteNamesTheMonosaccharideOnly() throws Exception {
		assertEquals("β-6-deoxy-D-Allp", sugarOf(wurcs(DEOXY_ALLOSE)).getLegend());
		assertEquals("?-2-deoxy-D-araHexp", sugarOf(wurcs(DEOXY_ARAHEX)).getLegend());
	}

	/** #258 can be written to WURCS again, as the same string. It was refused: "m has neither ...". */
	@Test
	public void itWritesBackToWurcs() throws Exception {
		assertEquals(DEOXY_ALLOSE, new WURCS2Parser().writeGlycan(wurcs(DEOXY_ALLOSE)));
		assertEquals("WURCS=2.0/1,1,0/[a2222m-1b_1-5]/1/",
				new WURCS2Parser().writeGlycan(wurcs("WURCS=2.0/1,1,0/[a2222m-1b_1-5]/1/")));
	}

	/** GWS is written exactly as it was before the change. */
	@Test
	public void gwsIsTheSameTextAsBefore() throws Exception {
		assertEquals(DEOXY_ALLOSE_GWS, wurcs(DEOXY_ALLOSE).toString());
		assertEquals(DEOXY_ARAHEX_GWS, wurcs(DEOXY_ARAHEX).toString());
	}

	/**
	 * And a GWS file written before it reads correctly: the "--6m" child goes back onto the residue,
	 * the mass is right, and the file writes back as the same text.
	 */
	@Test
	public void gwsWrittenBeforeReadsCorrectly() throws Exception {
		for (String gws : new String[] { DEOXY_ALLOSE_GWS, DEOXY_ARAHEX_GWS }) {
			Glycan read = Glycan.fromString(gws);
			assertNotNull(gws, read);

			Residue sugar = sugarOf(read);
			assertEquals(gws, 1, sugar.getDeoxies().size());
			assertEquals(gws, Arrays.asList("Me"), childNames(sugar));
			assertEquals(gws, C7H14O5, read.computeMass(), 1e-4);
			assertEquals(gws, read.toString());
		}
	}

	/**
	 * Reopened from GWS, the footnote has no skeleton to name it from - the legend is not saved - so
	 * the deoxy is put in front of the type's name. "Allose" alone would name a 6-deoxy allose as an
	 * allose.
	 */
	@Test
	public void reopenedTheFootnoteStillSaysDeoxy() throws Exception {
		assertEquals("6-deoxy-Allose", sugarOf(Glycan.fromString(DEOXY_ALLOSE_GWS)).getLegend());
	}

	/**
	 * A copy keeps the deoxy. {@code cloneResidue} did not copy what the WURCS reader records about
	 * the skeleton, so a copy of a 6-deoxy allose would have been an allose.
	 */
	@Test
	public void aCopyKeepsTheDeoxy() throws Exception {
		Residue allose = sugarOf(wurcs(DEOXY_ALLOSE));
		Residue copy = allose.cloneResidue();

		assertEquals(allose.getDeoxies(), copy.getDeoxies());
		assertEquals(allose.getModifications(), copy.getModifications());
		// Not asserted here: that Glycan.clone() keeps the mass. It does not, for any structure with an
		// O-methyl, deoxy or not - the clone's linkages come back UNVALIDATED and the methyl is weighed
		// an oxygen light. Measured on the unmodified 1.40.0 jar with a 3-O-methyl glucose (194.0790,
		// cloned 178.0841); a defect of its own, recorded in TRIAGE.md.
	}

	/** The guard: a deoxy the name implies - fucose's 6-deoxy - is not recorded as an extra one. */
	@Test
	public void aDeoxyTheNameImpliesIsNotAnExtraOne() throws Exception {
		Residue fucose = sugarOf(wurcs(METHYL_FUCOSE));
		assertEquals("Fuc", fucose.getTypeName());
		assertTrue("an implied deoxy was recorded as extra: " + fucose.getDeoxies(), fucose.getDeoxies().isEmpty());
		assertEquals(METHYL_FUCOSE, new WURCS2Parser().writeGlycan(wurcs(METHYL_FUCOSE)));
	}

	// ------------------------------------------------------------------------------------------------

	private static Glycan wurcs(String sequence) throws Exception {
		return new WURCS2Parser().readGlycan(sequence, new MassOptions());
	}

	private static Residue sugarOf(Glycan glycan) {
		for (Residue r : glycan.getAllResidues())
			if (r.isSaccharide()) return r;
		throw new AssertionError("no monosaccharide in " + glycan);
	}

	private static List<String> childNames(Residue r) {
		List<String> names = new ArrayList<String>();
		for (int i = 0; i < r.getNoChildren(); i++) names.add(r.getChildAt(i).getTypeName());
		return names;
	}
}
