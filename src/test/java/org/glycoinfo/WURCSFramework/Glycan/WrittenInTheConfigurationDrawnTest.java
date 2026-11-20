package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A residue is written in the configuration it is drawn in (#313).
 *
 * <p>Sixteen residues whose names carry a configuration - the dideoxyhexoses, the nonulosonic acids,
 * Kdo, Dha and the two manno-heptoses - were written in their name's configuration whatever they were
 * drawn in: an L-NeuAc came back as D-NeuAc, silently the other enantiomer. Apiose as well. The maintainer's rule
 * (2026-11-10) is to write the configuration drawn. Each now writes a different WURCS in D and in L,
 * and reads back in the configuration drawn - as the same residue, or as the one that is that
 * enantiomer's name: an L-Abe is colitose, a D-Col abequose.
 */
public class WrittenInTheConfigurationDrawnTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	@Test
	public void eachIsWrittenAsDrawn() throws Exception {
		String[][] residues = {
				{ "Abe", "1", "Col" }, { "Col", "1", "Abe" }, { "Par", "1", null }, { "Tyv", "1", null },
				{ "Neu", "2", null }, { "NeuAc", "2", null }, { "NeuGc", "2", null }, { "Kdn", "2", null },
				{ "Leg", "2", null }, { "4eLeg", "2", null }, { "Aci", "2", null }, { "Pse", "2", null },
				{ "Kdo", "2", null }, { "Dha", "2", null },
				{ "D-gro-D-manHep", "1", null }, { "L-gro-D-manHep", "1", null } };
		// Api is a furanose; written below with its own ring
		for (String[] one : residues) {
			String inD = write(one[0], one[1], 'D');
			String inL = write(one[0], one[1], 'L');
			assertNotEquals(one[0] + " writes the same for D and L", inD, inL);

			for (char configuration : new char[] { 'D', 'L' }) {
				String wurcs = (configuration == 'D') ? inD : inL;
				Residue back = new WURCS2Parser().readGlycan(wurcs, workspace.getDefaultMassOptions()).getRoot().getChildAt(0);
				assertEquals(one[0] + " " + configuration, configuration, back.getChirality());
			}
		}
	}

	/**
	 * Apiose's C3 is a branched carbon, written 5 or 6 - WURCSFramework's S and R for one - and an
	 * enantiomer swaps those as it swaps 1 and 2. L is its default, as SNFG's note 1 says.
	 */
	@Test
	public void apioseIsWrittenAsDrawn() throws Exception {
		String l = new WURCS2Parser().toWURCS(Glycan.fromString("freeEnd--1a1L-Api,f$MONO,Und,0,0,freeEnd"));
		String d = new WURCS2Parser().toWURCS(Glycan.fromString("freeEnd--1a1D-Api,f$MONO,Und,0,0,freeEnd"));
		assertEquals("WURCS=2.0/1,1,0/[a26h-1a_1-4_3*CO]/1/", l);
		assertEquals("WURCS=2.0/1,1,0/[a15h-1a_1-4_3*CO]/1/", d);
		assertEquals('D', new WURCS2Parser().readGlycan(d, workspace.getDefaultMassOptions()).getRoot().getChildAt(0).getChirality());
	}

	/** Each one's own configuration is written exactly as it always was. */
	@Test
	public void theNamesOwnConfigurationIsUnchanged() throws Exception {
		assertEquals("WURCS=2.0/1,1,0/[a1d21m-1a_1-5]/1/", write("Col", "1", 'L'));
		assertEquals("WURCS=2.0/1,1,0/[Aad21122h-2a_2-6_5*NCC/3=O]/1/", write("NeuAc", "2", 'D'));
		assertEquals("WURCS=2.0/1,1,0/[Aad22111m-2a_2-6_5*N_7*N]/1/", write("Pse", "2", 'L'));
		assertEquals("WURCS=2.0/1,1,0/[a11221h-1a_1-5]/1/", write("L-gro-D-manHep", "1", 'D'));
	}

	private static String write(String _name, String _anomeric, char _configuration) throws Exception {
		return new WURCS2Parser().toWURCS(Glycan.fromString("freeEnd--" + _anomeric + "a" + _anomeric
				+ _configuration + "-" + _name + ",p$MONO,Und,0,0,freeEnd"));
	}
}
