package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Structures registered in GlyTouCan, read in and written back out through the WURCS2 parser.
 *
 * <p>These strings come from real accessions rather than from anything this project generates, so
 * they are the outside check on the conversion: whatever the exporter decides to emit has to agree
 * with what the registry already holds. They were previously exercised against the GlyContainer
 * conversion path, which the parser no longer uses; they now run against the live path.
 *
 * <p>Only structures that survive the round trip today are asserted here. Ambiguous and repeating
 * structures (undetermined linkages, {@code ~n} repeats, {@code u}/{@code h} skeletons) do not, and
 * are left out rather than pinned to their current broken behaviour.
 */
public class RegisteredGlycanWURCSRoundTripTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** G00027JG */
	@Test
	public void biantennaryWithFucose() throws Exception {
		assertRoundTrip("WURCS=2.0/4,6,5/[a2112h-1b_1-5_2*NCC/3=O][a2112h-1b_1-5][a1221m-1a_1-5]"
				+ "[a2122h-1b_1-5_2*NCC/3=O]/1-2-3-4-2-3/a3-b1_a6-d1_b2-c1_d4-e1_e2-f1");
	}

	/** G00020MO - N-acetyl and sulfate on the same residue. */
	@Test
	public void sulfatedKeratanLikeChain() throws Exception {
		assertRoundTrip("WURCS=2.0/3,4,3/[u2122h_2*NCC/3=O_6*OSO/3=O/3=O][a2112h-1b_1-5]"
				+ "[a2122h-1b_1-5_2*NCC/3=O_6*OSO/3=O/3=O]/1-2-3-2/a4-b1_b3-c1_c4-d1");
	}

	/** G13561HU - a 4,6-pyruvate bridge next to a 3-O-methyl. */
	@Test
	public void pyruvateBridgeWithMethyl() throws Exception {
		assertRoundTrip("WURCS=2.0/3,3,2/[a2221m-1x_1-5][a2211m-1a_1-5]"
				+ "[a2122h-1b_1-5_4-6*OC^XO*/3CO/6=O/3C_3*OC]/1-2-3/a2-b1_b3-c1");
	}

	/**
	 * G07716WO - an ether bridge across positions 2 and 3, written as the positions alone. This is
	 * the registry's own evidence that such a bridge carries no MAP of its own.
	 */
	@Test
	public void etherBridgeIsWrittenAsBarePositions() throws Exception {
		assertRoundTrip("WURCS=2.0/3,3,2/[u2122h][a2222h-1a_1-5_2-3][a2122h-1a_1-5]/1-2-3/a4-b1_b4-c1");
	}

	/** G00048ZA - a cyclic chain carrying two methyls and an amino. */
	@Test
	public void cyclicChainWithSeveralSubstituents() throws Exception {
		assertRoundTrip("WURCS=2.0/1,7,7/[a2122h-1a_1-5_2*OC_3*OC_6*N]/1-1-1-1-1-1-1"
				+ "/a1-g4_a4-b1_b4-c1_c4-d1_d4-e1_e4-f1_f4-g1");
	}

	/** G00050XR - undetermined linkages and floating acetyls alongside a sialic acid. */
	@Test
	public void structureWithUndeterminedLinkages() throws Exception {
		assertRoundTrip("WURCS=2.0/7,12,14/[a2122h-1x_1-5_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O]"
				+ "[a1122h-1b_1-5][a1122h-1a_1-5][axxxxh-1x_1-5_2*NCC/3=O][a2112h-1b_1-5]"
				+ "[Aad21122h-2a_2-6_5*NCC/3=O]/1-2-3-4-5-6-7-7-4-5-6-7"
				+ "/a4-b1_b4-c1_e4-f1_f3-g2_g8-h2_j4-k1_k3-l2_c?-d1_c?-i1_d?-e1_i?-j1"
				+ "_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?}*OCC/3=O"
				+ "_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?}*OCC/3=O"
				+ "_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?}*OCC/3=O");
	}

	/**
	 * A deoxy marker on the terminal carbon is part of the residue's name. Read as a modification
	 * instead, it became a residue of its own, and the structure could not be written back out.
	 */
	@Test
	public void deoxyResiduesOfUnknownStereochemistrySurvive() throws Exception {
		assertRoundTrip("WURCS=2.0/1,1,0/[axxxxm-1x_1-5]/1/");
		assertRoundTrip("WURCS=2.0/1,1,0/[axxxxm-1x_1-5_2*NCC/3=O]/1/");
		assertRoundTrip("WURCS=2.0/1,1,0/[Aadxxxxxm-2x_2-6_5*N_7*N]/1/");
	}

	/**
	 * A nonulosonate's acid on carbon 1 is part of what the residue is, and so is a deoxy marker on
	 * its last carbon. Read as modifications they were built as residues of their own, and importing
	 * one of these threw before it got that far, for want of an IUPAC name.
	 */
	@Test
	public void dideoxynonulosonatesSurvive() throws Exception {
		assertRoundTrip("WURCS=2.0/1,1,0/[Aad21122m-2x_2-6_5*N_7*N]/1/");   // Leg
		assertRoundTrip("WURCS=2.0/1,1,0/[Aad22111m-2x_2-6_5*N_7*N]/1/");   // Pse
		assertRoundTrip("WURCS=2.0/1,1,0/[Aad11122m-2x_2-6_5*N_7*N]/1/");   // 4eLeg
	}

	/**
	 * The three that the old naming could not read: a substituent with no template of its own, a
	 * skeleton the converter could not resolve, and a bridge across the anomeric carbon, which
	 * makes a second ring it refuses to model. All three are answered from our own dictionaries.
	 */
	@Test
	public void readsWhatTheNamingConverterCouldNot() throws Exception {
		assertRoundTrip("WURCS=2.0/1,1,0/[a2122h-1x_1-5_3*OCC]/1/");   // 3-O-ethyl
		assertRoundTrip("WURCS=2.0/1,1,0/[adxxxm-1x_1-5]/1/");         // ddHex
		assertRoundTrip("WURCS=2.0/1,1,0/[a2122h-1x_1-5_1-6]/1/");     // 1,6-anhydro
	}

	/**
	 * Residues grouped under "Unknown" used to be refused as a class on the way out, though several
	 * describe themselves well enough to write. These four are checked against PubChem and the SNFG
	 * table: Kdo carries the same 1122 stereo as D-mannose, as 3-deoxy-D-manno-oct-2-ulosonic acid
	 * should; the muramic acids carry an (R) carboxyethyl at position 3; and Bac reads as
	 * 2,4-diamino-2,4,6-trideoxy-D-glucose.
	 */
	@Test
	public void residuesGroupedUnderUnknownStillConvert() throws Exception {
		assertRoundTrip("WURCS=2.0/1,1,0/[Aad1122h-2x_2-6]/1/");                                  // Kdo
		assertRoundTrip("WURCS=2.0/1,1,0/[a2122h-1x_1-5_2*NCC/3=O_3*OCC^RC/4O/3=O]/1/");          // MurNAc
		assertRoundTrip("WURCS=2.0/1,1,0/[a2122h-1x_1-5_2*NCCO/3=O_3*OCC^RC/4O/3=O]/1/");         // MurNGc
		assertRoundTrip("WURCS=2.0/1,1,0/[a2122m-1x_1-5_2*N_4*N]/1/");                            // Bac
	}

	/**
	 * Dha is 3-deoxy-D-lyxo-hept-2-ulosaric acid, and residue_types described it with no anomeric
	 * carbon, configuration or ring, so the skeleton came out indeterminate and did not read back as
	 * itself. All three are stated by the sources: SNFG Note 4 lists Dha among the residues whose D
	 * is implicit in the name and makes pyranose the default, and PubChem CID 15608515 names it
	 * (4R,5R,6S)-2,4,5-trihydroxyoxane-2,6-dicarboxylic acid - an oxane, so a six-membered ring
	 * spanning C2 to C6, with the anomeric carbon at C2 carrying both OH and COOH.
	 *
	 * <p>The 112 in the middle is D-lyxo, the same trio Tag carries as D-lyxo-hex-2-ulose (ha112h),
	 * and the carboxyl at either end is what makes it a ulosaric rather than a ulosonic acid.
	 */
	@Test
	public void dhaConvertsOnceItHasAnAnomericCarbonAndRing() throws Exception {
		assertRoundTrip("WURCS=2.0/1,1,0/[Aad112A-2x_2-6]/1/");
	}

	private void assertRoundTrip(String _wurcs) throws Exception {
		WURCS2Parser parser = new WURCS2Parser();
		Glycan glycan = parser.readGlycan(_wurcs, new MassOptions());
		assertEquals(_wurcs, parser.writeGlycan(glycan));
	}
}
