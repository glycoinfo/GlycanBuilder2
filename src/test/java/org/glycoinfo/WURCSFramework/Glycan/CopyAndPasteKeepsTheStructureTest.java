package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.Test;

/**
 * What is pasted is what was copied (#242, #362).
 *
 * <p>Copy and paste inside the application is a GWS round trip: GlycanCanvas.copy puts
 * GlycanDocument.extractView's copy of the selection on the clipboard as GWS text, and paste parses it
 * back. Two things were lost on the way. A bridge within one residue - a 1,6-anhydro, a cyclic
 * phosphate, a pyruvate acetal, a 1,5-imino - came back from GWS as a residue of unknown type: drawn
 * in another font, a water light or more, and impossible to write to WURCS. And the partner of a
 * non-reducing sugar, which hangs on the other's anomeric carbon (#357), was turned away by the copy:
 * a copy of trehalose was one glucose.
 */
public class CopyAndPasteKeepsTheStructureTest {

	private static final String[] COPIED = {
			// the issue's structure, G00025HU: the spurious anhydro is gone since #297
			"WURCS=2.0/7,11,10/[a2122h-1x_1-?_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5]"
					+ "[a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O][a1221m-1a_1-5]/1-2-3-4-2-5-4-2-5-6-7/"
					+ "a4-b1_a6-k1_b4-c1_c3-d1_c6-g1_d2-e1_e4-f1_g2-h1_h4-i1_i?-j2",
			"WURCS=2.0/1,1,0/[a2122h-1b_1-5_1-6]/1/",                          // 1,6-anhydro-glucose
			"WURCS=2.0/1,1,0/[a2112h-1a_1-5_3-6]/1/",                          // 3,6-anhydro-galactose
			"WURCS=2.0/1,1,0/[a2122h-1b_1-5_4-6*OPO*/3O/3=O]/1/",              // a 4,6-cyclic phosphate
			"WURCS=2.0/1,1,0/[a2112h-1b_1-5_4-6*OC^XO*/3CO/6=O/3C]/1/",        // a pyruvate acetal
			"WURCS=2.0/1,1,0/[h2112h_1-5*N*]/1/",                              // an imino-alditol
			"WURCS=2.0/1,1,0/[A22h_1-4]/1/",                                   // a lactone
			"WURCS=2.0/1,2,1/[a2122h-1a_1-5]/1-1/a1-b1",                       // trehalose
			"WURCS=2.0/2,2,1/[a2122h-1a_1-5][ha122h-2b_2-5]/1-2/a1-b2",        // sucrose
	};

	/** Each, copied whole and pasted, weighs and writes what it did. */
	@Test
	public void whatIsPastedIsWhatWasCopied() throws Exception {
		for (String sequence : COPIED) {
			BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
			workspace.setAutoSave(false);
			GlycanDocument document = workspace.getStructures();
			document.setStructures(Collections.singletonList(
					new WURCS2Parser().readGlycan(sequence, workspace.getDefaultMassOptions())));
			Glycan original = document.getStructures().iterator().next();

			// what GlycanCanvas.copy and paste do, without the clipboard
			String clipboard = GlycanDocument.toString(document.extractView(new HashSet<Residue>(original.getAllResidues())));
			document.addStructures(null, document.parseString(clipboard));
			Iterator<Glycan> structures = document.getStructures().iterator();
			structures.next();
			Glycan pasted = structures.next();

			assertEquals(sequence, original.computeMass(), pasted.computeMass(), 1e-4);
			assertEquals(sequence, new WURCS2Parser().toWURCS(original), new WURCS2Parser().toWURCS(pasted));
		}
	}

	/** A bridge read from GWS is the bridge: drawn as one, weighed as one (#362). */
	@Test
	public void aBridgeReadFromGwsIsABridge() throws Exception {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
		Glycan anhydro = Glycan.fromString("freeEnd--1b1D-Glc,p--1=1,6Anhydro$MONO,Und,0,0,freeEnd");
		Residue bridge = anhydro.getRoot().firstChild().firstChild();
		assertEquals("Anhydro", bridge.getTypeName());
		assertTrue("read as a residue of unknown type", bridge.isBridge());
		anhydro.getMassOptions().setDerivatization(org.eurocarbdb.application.glycanbuilder.massutil.MassOptions.NO_DERIVATIZATION);
		assertEquals("C6H10O5", 162.0528, anhydro.computeMass(), 1e-4);
	}
}
