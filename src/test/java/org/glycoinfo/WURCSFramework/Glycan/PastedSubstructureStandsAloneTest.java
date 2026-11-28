package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.Before;
import org.junit.Test;

/**
 * A substructure copied from inside a structure and pasted on its own is a structure of its own (#241).
 *
 * <p>The copy of an N-glycan's GlcNAc-Gal-NeuAc arm hangs on an attach point that keeps where it was
 * bonded, 2 of the Man, so that pasted onto another residue it is bonded there again. Pasted with no
 * residue selected, the attach point stayed the new structure's root: an empty box, weighed a water
 * light for the bond to it, and a structure that could not be copied again.
 */
public class PastedSubstructureStandsAloneTest {

	/** A biantennary N-glycan with one arm extended to NeuAc α2-6. */
	private static final String N_GLYCAN = "WURCS=2.0/5,10,9/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5]"
			+ "[a1122h-1a_1-5][a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O]/1-1-2-3-1-4-5-3-1-4/"
			+ "a4-b1_b4-c1_c3-d1_c6-h1_d2-e1_e4-f1_f6-g2_h2-i1_i4-j1";
	/** The arm on its own: β-GlcNAc-(1-4)... with a free reducing end. */
	private static final String ARM =
			"WURCS=2.0/3,3,2/[a2122h-1b_1-5_2*NCC/3=O][a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O]/1-2-3/a4-b1_b6-c2";

	private BuilderWorkspace workspace;
	private GlycanDocument document;

	@Before
	public void open() throws Exception {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
		document = workspace.getStructures();
		document.setStructures(Collections.singletonList(read(N_GLYCAN)));
	}

	private Glycan read(String _wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(_wurcs, workspace.getDefaultMassOptions());
	}

	/** The GlcNAc, Gal and NeuAc of the extended arm. */
	private static HashSet<Residue> theArm(Glycan _structure) {
		Residue neuAc = null;
		for (Residue residue : _structure.getAllResidues())
			if (residue.getTypeName().equals("NeuAc")) neuAc = residue;
		return new HashSet<Residue>(Arrays.asList(neuAc, neuAc.getParent(), neuAc.getParent().getParent()));
	}

	/** What GlycanCanvas.copy and paste do, without the clipboard, pasting onto {@code _onto}. */
	private Glycan copyAndPaste(HashSet<Residue> _selection, Residue _onto) throws Exception {
		int before = document.getStructures().size();
		String clipboard = GlycanDocument.toString(document.extractView(_selection));
		document.addStructures(_onto, document.parseString(clipboard));
		if (document.getStructures().size() == before) return null;
		Iterator<Glycan> structures = document.getStructures().iterator();
		Glycan last = null;
		while (structures.hasNext()) last = structures.next();
		return last;
	}

	/** Pasted on its own, it has a reducing end, weighs the trisaccharide, and writes it. */
	@Test
	public void pastedOnItsOwnItIsTheTrisaccharide() throws Exception {
		Glycan original = document.getStructures().iterator().next();
		Glycan pasted = copyAndPaste(theArm(original), null);

		assertTrue("its root is " + pasted.getRoot().getTypeName(), pasted.getRoot().isReducingEnd());
		assertFalse(pasted.getRoot().isAttachPoint());
		assertEquals("GlcNAc", pasted.getRoot().firstChild().getTypeName());
		assertEquals(read(ARM).computeMass(), pasted.computeMass(), 1e-4);
		assertEquals(ARM, new WURCS2Parser().toWURCS(pasted));
	}

	/** And what was pasted can be copied and pasted again. */
	@Test
	public void whatWasPastedCanBeCopiedAgain() throws Exception {
		Glycan original = document.getStructures().iterator().next();
		Glycan pasted = copyAndPaste(theArm(original), null);
		Glycan again = copyAndPaste(new HashSet<Residue>(pasted.getAllResidues()), null);

		assertEquals(ARM, new WURCS2Parser().toWURCS(again));
		assertEquals(pasted.computeMass(), again.computeMass(), 1e-4);
	}

	/** Pasted onto a residue, it is bonded where it was, as before. */
	@Test
	public void pastedOntoAResidueItKeepsItsBond() throws Exception {
		document.setStructures(Collections.singletonList(read(N_GLYCAN)));
		Glycan original = document.getStructures().iterator().next();
		HashSet<Residue> arm = theArm(original);
		document.addStructure(read("WURCS=2.0/1,1,0/[a1122h-1a_1-5]/1/"));
		Glycan mannose = null;
		for (Glycan structure : document.getStructures()) mannose = structure;
		Residue man = mannose.getRoot().firstChild();

		String clipboard = GlycanDocument.toString(document.extractView(arm));
		document.addStructures(man, document.parseString(clipboard));

		assertEquals(1, man.getNoChildren());
		assertEquals("GlcNAc", man.firstChild().getTypeName());
		assertEquals("2", man.getChildrenLinkages().getFirst().getParentPositionsString());
	}
}
