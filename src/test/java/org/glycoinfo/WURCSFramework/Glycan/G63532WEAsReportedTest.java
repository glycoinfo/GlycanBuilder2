package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.SVGUtils;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.Before;
import org.junit.Test;

/**
 * G63532WE, which #247 reported drawn and handled wrong in five ways, is read, drawn, written and
 * copied as it is.
 *
 * <p>The five were each fixed on their own: the symbol of an unknown residue (#248), text overlapping
 * uncommon residues (#250), a residue's index (#251), the spurious 1,? anhydro beside a ring of
 * unknown size (#239, by #259), and the copy that pasted into nothing and the structure that could not
 * be written (#249). This holds them together, on the structure they were found on.
 */
public class G63532WEAsReportedTest {

	/** G63532WE, as GlyCosmos serves it. */
	private static final String G63532WE = "WURCS=2.0/14,16,15/[a2122h-1a_1-5_2*N][a2122h-1b_1-5_2*N_4*OPO/3O/3=O][Aad1122h-2a_2-6_4*OPO/3O/3=O][a11221h-1a_1-5][a2112h-1b_1-5][ha1222h-2b_2-6][a2122m-1b_1-5_2*N][a2112A-1b_1-5][Aad21122h-2a_2-6_5*N][a21EEA-1b_1-5][a2122h-1a_1-?_2*N][a2122h-1b_1-5][ha1222h-2b_2-5][a2122h-1a_1-5]/1-2-3-4-4-4-5-6-7-8-9-10-11-12-13-14/a6-b1_b6-c2_c5-d1_d3-e1_d4-n1_d6-p1_e2-f1_f3-g1_f7-m1_g3-h2_h4-i1_i3-j1_j3-k2_k4-l1_n6-o2";

	private GlycanRendererAWT renderer;
	private BuilderWorkspace workspace;
	private Glycan read;

	@Before
	public void read() throws Exception {
		renderer = new GlycanRendererAWT();
		workspace = new BuilderWorkspace(renderer);
		workspace.setAutoSave(false);
		read = new WURCS2Parser().readGlycan(G63532WE, workspace.getDefaultMassOptions());
	}

	/** All sixteen monosaccharides, and no anhydro beside the GlcN whose ring is not known. */
	@Test
	public void everyResidueAndNothingElse() {
		int monosaccharides = 0;
		for (Residue residue : read.getAllResidues()) {
			if (residue.isSaccharide() && !residue.isBridge()) monosaccharides++;
			assertFalse("an anhydro nobody asked for (#239)", residue.getTypeName().contains("Anhydro"));
		}
		assertEquals(16, monosaccharides);
	}

	/** Its drawing has no anhydro either. */
	@Test
	public void theDrawingHasNoAnhydro() {
		String drawn = SVGUtils.getVectorGraphics(renderer, Collections.singletonList(read), false, true);
		assertFalse(drawn.contains("Anhydro"));
	}

	/** It is written back as it was read. */
	@Test
	public void itIsWrittenBackAsRead() throws Exception {
		assertEquals(G63532WE, new WURCS2Parser().toWURCS(read));
	}

	/** Copied and pasted, it is a second structure that writes what was copied (#249). */
	@Test
	public void itIsCopiedAndPasted() throws Exception {
		GlycanDocument document = workspace.getStructures();
		document.setStructures(Collections.singletonList(read));
		Glycan original = document.getStructures().iterator().next();

		String clipboard = GlycanDocument.toString(document.extractView(new HashSet<Residue>(original.getAllResidues())));
		document.addStructures(null, document.parseString(clipboard));

		assertEquals(2, document.getStructures().size());
		Iterator<Glycan> structures = document.getStructures().iterator();
		structures.next();
		assertEquals(G63532WE, new WURCS2Parser().toWURCS(structures.next()));
	}
}
