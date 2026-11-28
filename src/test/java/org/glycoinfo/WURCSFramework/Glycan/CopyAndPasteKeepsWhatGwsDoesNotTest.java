package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.GlycanSelection;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.Before;
import org.junit.Test;

/**
 * Copied and pasted in the application, a structure keeps what GWS does not carry (#370).
 *
 * <p>Paste parsed the GWS text the copy had put on the clipboard. GWS does not carry stereo read as
 * partly or relatively known, an antenna's candidate parents, a probability, or a cycle, and a
 * composition's members were copied each as a structure of its own: of the structures in these tests,
 * 22 came back different, four could not be pasted at all, and Hex2 came back as two Hex. The
 * clipboard now holds the structures themselves as well, and a paste in the application takes copies
 * of them; the text and the pictures are still there for any other application.
 */
public class CopyAndPasteKeepsWhatGwsDoesNotTest {

	private BuilderWorkspace workspace;

	@Before
	public void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** One of each kind that came back different or not at all. */
	@Test
	public void eachComesBackAsCopied() throws Exception {
		for (String wurcs : new String[] {
				// stereo partly known, and relative stereo the way round it was read
				"WURCS=2.0/1,1,0/[a2122xh-1x_1-5]/1/",
				"WURCS=2.0/1,1,0/[a3433h-1b_1-5]/1/",
				"WURCS=2.0/1,1,0/[ad3dh-1b_1-5_3*OC]/1/",
				// cycles, which could not be pasted
				"WURCS=2.0/1,7,7/[a2122h-1a_1-5_2*OC_3*OC_6*N]/1-1-1-1-1-1-1/a1-g4_a4-b1_b4-c1_c4-d1_d4-e1_e4-f1_f4-g1",
				"WURCS=2.0/2,2,2/[h4344h_2*NCC/3=O][a2112h-1b_1-5]/1-2/a4-b1_a6-b2",
				// a repeat, and one closing on a carbon not known
				"WURCS=2.0/2,3,3/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5]/1-1-2/a4-b1_b4-c1_c4-c?~n",
				// an antenna with its candidate parents
				"WURCS=2.0/6,11,10/[a2122h-1x_1-5_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][a1221m-1a_1-5]/1-2-3-4-2-5-4-2-6-2-5/a4-b1_a6-i1_b4-c1_c3-d1_c6-g1_d2-e1_e4-f1_g2-h1_j4-k1_j1-d4|d6|g4|g6}",
				// a probability, and glycerol phosphate in a repeat
				"WURCS=2.0/2,4,4/[a1122m-1a_1-5][a2112m-1a_1-4]/1-1-1-2/a2-b1_b3-c1_d1-b4%.35%_a1-c3~n",
				"WURCS=2.0/2,4,4/[hxh][a2112h-1x_1-5_2*NCC/3=O]/1-1-1-2/a1-b1*OPO*/3O/3=O_b3-c1*OPO*/3O/3=O_c2-d1_a3-c3*OPO*/3O/3=O~n",
				// a composition
				"WURCS=2.0/1,2,1/[axxxxh-1x_1-5]/1-1/a?|b?}-{a?|b?" }) {
			Glycan original = read(wurcs);
			String written = new WURCS2Parser().toWURCS(original);
			List<Glycan> pasted = copyAndPaste(original);
			assertEquals(wurcs, 1, pasted.size());
			assertEquals(wurcs, written, new WURCS2Parser().toWURCS(pasted.get(0)));
			assertEquals(wurcs, original.computeMass(), pasted.get(0).computeMass(), 1e-6);
		}
	}

	/** A repeat around a chain of repeats nested in it is copied with them. Taken for one with no end, it was dropped. */
	@Test
	public void anOuterRepeatIsKept() throws Exception {
		String wurcs = "WURCS=2.0/4,13,17/[a122h-1x_1-4][a2112h-1x_1-4][a2112h-1x_1-5][a1122h-1x_1-5]/1-1-2-3-1-1-1-1-1-1-1-4-4/"
				+ "a5-b1_c5-d1_c6-l1_d4-e1_e5-f1_f5-j1_g2-h1_h5-i1_j5-k1_l2-m1_g1-f2|f3_b?-c1_a1-k5~n_e1-e5~n_h1-h5~n_j1-j5~n_b1-b?~n";
		Glycan original = read(wurcs);
		String written = new WURCS2Parser().toWURCS(original);
		assertTrue(written, written.contains("a1-k5~n"));

		GlycanDocument document = holding(original);
		Collection<Glycan> view = document.extractView(new HashSet<Residue>(document.getStructures().get(0).getAllResidues()));
		assertEquals(written, new WURCS2Parser().toWURCS(view.iterator().next()));
	}

	/** Antennae of uncertain parent are copied in the order the structure has them, each time. */
	@Test
	public void antennaeKeepTheirOrder() throws Exception {
		String wurcs = "WURCS=2.0/6,15,14/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][a2112m-1a_1-5]"
				+ "[Aad21122h-2a_2-6_5*NCC/3=O]/1-1-2-3-1-4-1-4-3-1-4-5-6-6-6/a4-b1_a6-l1_b4-c1_c3-d1_c6-i1_d2-e1_d4-g1_e4-f1_g4-h1"
				+ "_i2-j1_j4-k1_m2-f3|h3|k3}_n2-f3|h3|k3}_o2-f6|h6|k6}";
		Glycan original = read(wurcs);
		String written = new WURCS2Parser().toWURCS(original);
		for (int copy = 0; copy < 20; copy++)
			assertEquals(written, new WURCS2Parser().toWURCS(copyAndPaste(original).get(0)));
	}

	/** Each paste is a copy of its own, and of what was copied, not of what the document became since. */
	@Test
	public void eachPasteIsACopyOfWhatWasCopied() throws Exception {
		Glycan original = read("WURCS=2.0/2,2,1/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5]/1-2/a4-b1");
		String written = new WURCS2Parser().toWURCS(original);
		GlycanDocument document = holding(original);
		Glycan held = document.getStructures().get(0);
		GlycanSelection clipboard = new GlycanSelection(workspace.getGlycanRenderer(), Collections.singletonList(held));

		// the document changes after the copy
		Residue man = null;
		for (Residue residue : held.getAllResidues())
			if ("Man".equals(residue.getTypeName())) man = residue;
		document.removeResidues(Collections.singletonList(man));

		List<Glycan> first = GlycanSelection.structuresIn(clipboard);
		List<Glycan> second = GlycanSelection.structuresIn(clipboard);
		assertNotNull(first);
		assertNotSame(first.get(0), second.get(0));
		assertEquals(written, new WURCS2Parser().toWURCS(first.get(0)));
		assertEquals(written, new WURCS2Parser().toWURCS(second.get(0)));
	}

	/** Copied as the canvas copies, and pasted as it pastes, from the clipboard's structures. */
	private List<Glycan> copyAndPaste(Glycan original) throws Exception {
		GlycanDocument document = holding(original);
		Glycan held = document.getStructures().get(0);
		GlycanSelection clipboard = new GlycanSelection(workspace.getGlycanRenderer(),
				document.extractView(new HashSet<Residue>(held.getAllResidues())));
		return GlycanSelection.structuresIn(clipboard);
	}

	private GlycanDocument holding(Glycan structure) {
		GlycanDocument document = workspace.getStructures();
		document.setStructures(new ArrayList<Glycan>(Collections.singletonList(structure)));
		return document;
	}

	private Glycan read(String wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(wurcs, workspace.getDefaultMassOptions());
	}
}
