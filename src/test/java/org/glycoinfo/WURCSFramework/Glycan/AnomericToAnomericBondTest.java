package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.converterGlycoCT.GlycoCTCondensedParser;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Sucrose, and anything joined anomeric carbon to anomeric carbon, opens (#357).
 *
 * <p>The WURCS reader chose one end of such a bond as the root and dropped the other, which goes on
 * the root's anomeric carbon: sucrose came in as its fructose alone, and since #295 was refused as
 * read in part. The GlycoCT reader dropped it with no word, and sucrose was read as glucose. All 23 of
 * the 19,602 GlyTouCan WURCS of the release check with such a bond were refused.
 *
 * <p>The residue the WURCS lists first is drawn at the reducing-end position (the maintainer,
 * 2026-11-28), and the other hangs on its anomeric carbon - the form GWS has always saved for such a
 * sugar, and the WURCS writer writes.
 */
public class AnomericToAnomericBondTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	private static final String SUCROSE = "WURCS=2.0/2,2,1/[a2122h-1a_1-5][ha122h-2b_2-5]/1-2/a1-b2";  // G05551OP
	private static final String TREHALOSE = "WURCS=2.0/1,2,1/[a2122h-1a_1-5]/1-1/a1-b1";
	private static final String RAFFINOSE = "WURCS=2.0/3,3,2/[ha122h-2b_2-5][a2122h-1a_1-5][a2112h-1a_1-5]/1-2-3/a2-b1_b6-c1";

	/** Each is read whole, with the first residue at the reducing end, and written back as read. */
	@Test
	public void eachIsReadWholeAndWrittenBackAsRead() throws Exception {
		String[][] cases = {
				// WURCS, the residue at the reducing end, the one on its anomeric carbon, C12H22O11 or C18H32O16
				{ SUCROSE, "Glc", "Fru", "342.1162" },
				{ TREHALOSE, "Glc", "Glc", "342.1162" },
				{ "WURCS=2.0/2,2,1/[a2122h-1b_1-5_2*N][a2122h-1a_1-5]/1-2/a1-b1", "GlcN", "Glc", "341.1322" }, // G43703KI
				{ RAFFINOSE, "Fru", "Glc", "504.1690" },
		};
		for (String[] one : cases) {
			Glycan read = new WURCS2Parser().readGlycan(one[0], new MassOptions());
			Residue root = read.getRoot().firstChild();
			assertEquals(one[0], one[1], root.getTypeName());
			Residue other = root.firstChild();
			assertEquals(one[0], one[2], other.getTypeName());
			assertEquals(one[0], root.getAnomericCarbon(), other.getParentLinkage().getParentPositionsSingle());
			assertEquals(one[0], other.getAnomericCarbon(), other.getParentLinkage().getChildPositionsSingle());

			assertEquals(one[0], new WURCS2Parser().toWURCS(read));
			read.getMassOptions().setDerivatization(MassOptions.NO_DERIVATIZATION);
			assertEquals(one[0], Double.parseDouble(one[3]), read.computeMass(), 1e-3);
		}
	}

	/**
	 * The fructan InvalidWurcsIsRefusedTest used for a short read until now: sucrose with four more
	 * fructoses, five of six read and 828.27 Da. Read whole, it weighs what WURCSFramework says.
	 */
	@Test
	public void aFructanOnSucroseIsReadWhole() throws Exception {
		String fructan = "WURCS=2.0/2,6,5/[a2122h-1a_1-5][ha122h-2b_2-5]/1-2-2-2-2-2/a1-b2_b6-c2_c1-d2_c6-e2_e6-f2";
		Glycan read = new WURCS2Parser().readGlycan(fructan, new MassOptions());
		assertEquals(fructan, new WURCS2Parser().toWURCS(read));
		read.getMassOptions().setDerivatization(MassOptions.NO_DERIVATIZATION);
		assertEquals(990.3275, read.computeMass(), 1e-3);  // C36H62O31
	}

	/** Sucrose has eight free hydroxyls: 2, 3, 4 and 6 of the glucose, 1, 3, 4 and 6 of the fructose. */
	@Test
	public void sucroseIsDerivatizedAtItsEightHydroxyls() throws Exception {
		Glycan sucrose = new WURCS2Parser().readGlycan(SUCROSE, new MassOptions());
		assertEquals(8, sucrose.computeNoMethylPositions());
		assertEquals(8, sucrose.computeNoAcetylPositions());
		sucrose.getMassOptions().setDerivatization(MassOptions.PERMETHYLATED);
		assertEquals(342.1162 + 8 * 14.0157, sucrose.computeMass(), 1e-3);
		sucrose.getMassOptions().setDerivatization(MassOptions.PERACETYLATED);
		assertEquals(342.1162 + 8 * 42.0106, sucrose.computeMass(), 1e-3);
	}

	/** And it is kept through a copy and through GWS, saved and opened again. */
	@Test
	public void itIsKeptThroughACopyAndAFile() throws Exception {
		for (String wurcs : new String[] { SUCROSE, TREHALOSE, RAFFINOSE }) {
			Glycan read = new WURCS2Parser().readGlycan(wurcs, new MassOptions());
			assertEquals(wurcs, new WURCS2Parser().toWURCS(read.clone()));
			assertEquals(wurcs, new WURCS2Parser().toWURCS(Glycan.fromString(read.toString())));
		}
	}

	/** GlycoCT: sucrose was read as its glucose alone, with no word. */
	@Test
	public void glycoCtReadsSucroseWhole() throws Exception {
		Glycan sucrose = new GlycoCTCondensedParser(false).readGlycan(
				"RES\n1b:a-dglc-HEX-1:5\n2b:b-dara-HEX-2:5|2:keto\nLIN\n1:1o(1+2)2d\n", new MassOptions());
		assertEquals(SUCROSE, new WURCS2Parser().toWURCS(sucrose));
		Glycan trehalose = new GlycoCTCondensedParser(false).readGlycan(
				"RES\n1b:a-dglc-HEX-1:5\n2b:a-dglc-HEX-1:5\nLIN\n1:1o(1+1)2d\n", new MassOptions());
		assertEquals(TREHALOSE, new WURCS2Parser().toWURCS(trehalose));
	}

	/** An ordinary disaccharide is as it was: lactose's Gal on C4 of the glucose at the reducing end. */
	@Test
	public void anOrdinaryBondIsAsItWas() throws Exception {
		String lactose = "WURCS=2.0/2,2,1/[a2122h-1x_1-5][a2112h-1b_1-5]/1-2/a4-b1";
		Glycan read = new WURCS2Parser().readGlycan(lactose, new MassOptions());
		assertEquals("Glc", read.getRoot().firstChild().getTypeName());
		assertTrue(read.getRoot().firstChild().firstChild().getParentLinkage().getParentPositionsSingle() == '4');
		assertEquals(lactose, new WURCS2Parser().toWURCS(read));
	}
}
