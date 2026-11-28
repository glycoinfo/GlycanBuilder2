package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A residue bonded at a carbon of its parent that it does not have itself is written (#249).
 *
 * <p>The writer looked up the bond to a residue's parent at the parent's position, as if it were one
 * of the residue's own carbons: a hexose on a heptose's C7, a pentose on a hexose's C6, was read past
 * the end of its skeleton, and the structure could not be written to WURCS at all - G63532WE, whose
 * GlcN hangs on C7 of an L-glycero-D-manno-heptose, and 52 GlyTouCan structures. G63532WE is the
 * issue's: its copy pasted into nothing, and then, pasted, could not be exported.
 */
public class AShortChildOnAHighCarbonTest {

	/** G63532WE, as GlyCosmos serves it. */
	private static final String G63532WE = "WURCS=2.0/14,16,15/[a2122h-1a_1-5_2*N][a2122h-1b_1-5_2*N_4*OPO/3O/3=O][Aad1122h-2a_2-6_4*OPO/3O/3=O][a11221h-1a_1-5][a2112h-1b_1-5][ha1222h-2b_2-6][a2122m-1b_1-5_2*N][a2112A-1b_1-5][Aad21122h-2a_2-6_5*N][a21EEA-1b_1-5][a2122h-1a_1-?_2*N][a2122h-1b_1-5][ha1222h-2b_2-5][a2122h-1a_1-5]/1-2-3-4-4-4-5-6-7-8-9-10-11-12-13-14/a6-b1_b6-c2_c5-d1_d3-e1_d4-n1_d6-p1_e2-f1_f3-g1_f7-m1_g3-h2_h4-i1_i3-j1_j3-k2_k4-l1_n6-o2";

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	private static String roundTrip(String _wurcs) throws Exception {
		return new WURCS2Parser().toWURCS(new WURCS2Parser().readGlycan(_wurcs, workspace.getDefaultMassOptions()));
	}

	/** Each is written back as it was read. */
	@Test
	public void eachIsWrittenBackAsRead() throws Exception {
		String[] sequences = {
				"WURCS=2.0/2,2,1/[a11221h-1a_1-5][a2122h-1a_1-5_2*N]/1-2/a7-b1",   // GlcN on C7 of a heptose
				"WURCS=2.0/2,2,1/[a2122h-1x_1-5][a212h-1x_1-5]/1-2/a6-b1",         // G02332YO, Xyl on C6 of Glc
				G63532WE,
		};
		for (String sequence : sequences)
			assertEquals(sequence, sequence, roundTrip(sequence));
	}

	/** G63532WE, copied and pasted, is pasted, and writes what it was. */
	@Test
	public void g63532weIsCopiedAndPasted() throws Exception {
		BuilderWorkspace own = new BuilderWorkspace(new GlycanRendererAWT());
		own.setAutoSave(false);
		GlycanDocument document = own.getStructures();
		document.setStructures(Collections.singletonList(new WURCS2Parser().readGlycan(G63532WE, own.getDefaultMassOptions())));
		Glycan original = document.getStructures().iterator().next();

		String clipboard = GlycanDocument.toString(document.extractView(new HashSet<Residue>(original.getAllResidues())));
		document.addStructures(null, document.parseString(clipboard));

		assertEquals(2, document.getStructures().size());
		Iterator<Glycan> structures = document.getStructures().iterator();
		structures.next();
		assertEquals(G63532WE, new WURCS2Parser().toWURCS(structures.next()));
	}
}
