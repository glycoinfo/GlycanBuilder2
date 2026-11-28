package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A structure with a repeat of one residue at the end of a chain is read whole (#264).
 *
 * <p>A repeat of one residue, b1-b4~n, has the residue as its own donor. The reader took that for the
 * mark of a composition's member, a?|b?}-{a?|b?, and for a second parent of the residue: G67119YN, an
 * N-glycan with polysialic acids and a polyfucose, was read as a composition of its two NeuAc and its
 * Fuc - three of its thirteen monosaccharides - and drawn as that. Both are GlyTouCan's, as GlyCosmos
 * serves them.
 */
public class RepeatsOfOneResidueAreReadWholeTest {

	private static final String G67119YN = "WURCS=2.0/5,13,15/[a2122h-1x_1-5_2*NCC/3=O][a1122h-1x_1-5]"
			+ "[a2112h-1x_1-5][Aad21122h-2x_2-6_5*NCC/3=O][a1221m-1x_1-5]/1-1-1-2-2-1-3-2-1-3-4-4-5/"
			+ "a?-b1_b?-c1_c?-d1_d?-e1_d?-h1_e?-f1_f?-g1_h?-i1_i?-j1_a?-m?_g?-k?_j?-l?_k2-k?~n_l2-l?~n_m1-m?~n";
	/** Two repeats closing on one carbon: b1-b4~5-6 inside a1-b4~n. */
	private static final String G31898IW =
			"WURCS=2.0/2,3,4/[a2122h-1a_1-5][a2122h-1x_1-5]/1-2-1/a4-b1_a6-c1_b1-b4~5-6_a1-b4~n";

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	private static Glycan read(String _wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(_wurcs, workspace.getDefaultMassOptions());
	}

	/** A Gal repeated on a Glc is read as that, and written back as read. */
	@Test
	public void aRepeatOfOneOnAChainIsReadAsThat() throws Exception {
		String sequence = "WURCS=2.0/2,2,2/[a2122h-1b_1-5][a2112h-1b_1-5]/1-2/a4-b1_b1-b4~n";
		Glycan read = read(sequence);
		assertFalse("read as a composition", read.isComposition());
		assertEquals(sequence, new WURCS2Parser().toWURCS(read));
	}

	/** G67119YN has its thirteen monosaccharides and its three repeats, each closed, and no composition. */
	@Test
	public void g67119ynIsReadWhole() throws Exception {
		Glycan read = read(G67119YN);
		assertFalse("read as a composition", read.isComposition());
		int monosaccharides = 0, starts = 0, ends = 0;
		for (Residue residue : read.getAllResidues()) {
			if (residue.isSaccharide() && !residue.isRepetition()) monosaccharides++;
			if (residue.isStartRepetition()) starts++;
			if (residue.isEndRepetition()) ends++;
		}
		assertEquals(13, monosaccharides);
		assertEquals(3, starts);
		assertEquals(3, ends);

		// written, and read back to the same
		String written = new WURCS2Parser().toWURCS(read);
		assertEquals(written, new WURCS2Parser().toWURCS(read(written)));
	}

	/** Two repeats closing on one carbon cannot be held, and are refused rather than read with one lost. */
	@Test
	public void twoRepeatsClosingOnOneCarbonAreRefused() throws Exception {
		try {
			read(G31898IW);
			fail("read with a repeat left open");
		} catch (Exception refused) {
			assertTrue(refused.getMessage(), refused.getMessage().contains("cannot hold two repeats closing on one carbon"));
		}
	}
}
