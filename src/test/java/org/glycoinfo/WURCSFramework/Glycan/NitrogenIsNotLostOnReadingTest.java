package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.array.mass.WURCSMassCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Reading a WURCS does not lose a nitrogen (#318, #319).
 *
 * <p>Two faults, each turning an N-acyl into an O-acyl - 0.9840 heavy, written back as another
 * molecule, with nothing to say so:
 *
 * <ul>
 * <li>#318 - a sialic acid with a substituent at C1 or C4 was read as Kdn. The residue's own groups
 * were looked for only as a prefix of the MS's, and NeuAc's N-acetyl, after a 4-O-acetyl, was never
 * tried alone. 107 GlyGen sequences.
 * <li>#319 - an N-linked group at another carbon of an amino sugar, a GlcN's 4 or a NeuAc's 7, was
 * taken to hang off the residue's own amine, and read as an O-acyl. Whether a carbon has an amine is
 * now what the residue's name says, not a class and a list of positions.
 * </ul>
 */
public class NitrogenIsNotLostOnReadingTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** Each is read as what it is, weighs what WURCSFramework says, and is written back unchanged. */
	@Test
	public void eachReadsAsWritten() throws Exception {
		String[][] cases = {
				// #318
				{ "WURCS=2.0/1,1,0/[Aad21122h-2a_2-6_4*OCC/3=O_5*NCC/3=O]/1/", "NeuAc" },     // Neu4,5Ac2
				{ "WURCS=2.0/1,1,0/[Aad21122h-2a_2-6_1*OC_5*NCC/3=O]/1/", "NeuAc" },          // methyl ester
				{ "WURCS=2.0/1,1,0/[Aad21122h-2a_2-6_4*OCC/3=O_5*NCCO/3=O]/1/", "NeuGc" },
				{ "WURCS=2.0/2,2,1/[a2112h-1b_1-5][Aad21122h-2a_2-6_4*OCC/3=O_5*NCC/3=O]/1-2/a3-b2", "Gal" },
				// #319
				{ "WURCS=2.0/1,1,0/[a2122h-1a_1-5_2*N_4*NCC/3=O]/1/", "GlcN" },
				{ "WURCS=2.0/1,1,0/[Aad21122h-2a_2-6_5*NCC/3=O_7*NCC/3=O]/1/", "NeuAc" },
				// and what was right stays right
				{ "WURCS=2.0/1,1,0/[Aad21122h-2a_2-6_5*NCC/3=O_9*OCC/3=O]/1/", "NeuAc" },
				{ "WURCS=2.0/1,1,0/[a2122h-1a_1-5_2*NCC/3=O_4*NCC/3=O]/1/", "GlcNAc" },
				{ "WURCS=2.0/1,1,0/[a2122h-1a_1-5_4*NCC/3=O]/1/", "Glc" },
				{ "WURCS=2.0/1,1,0/[a2122h-1a_1-5_2*NSO/3=O/3=O]/1/", null },
				{ "WURCS=2.0/1,1,0/[a2122m-1a_1-5_2*NCC/3=O_4*NCC/3=O]/1/", null } };
		for (String[] one : cases) {
			Glycan read = new WURCS2Parser().readGlycan(one[0], workspace.getDefaultMassOptions());
			if (one[1] != null)
				assertEquals(one[0], one[1], read.getRoot().getChildAt(0).getTypeName());
			double theirs = WURCSMassCalculator.calcMassWURCS(new WURCSFactory(one[0]).getArray()).doubleValue();
			assertEquals(one[0], theirs, read.computeMass(), 0.0001);
			assertEquals(one[0], one[0], new WURCS2Parser().toWURCS(read));
		}
	}
}
