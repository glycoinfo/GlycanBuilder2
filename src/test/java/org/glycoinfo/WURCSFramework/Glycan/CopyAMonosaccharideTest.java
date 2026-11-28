package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
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
 * A residue selected without the reducing end it stands on can be copied and pasted (#240).
 *
 * <p>A lone GlcNAc, selected and copied, was copied alone: the GWS on the clipboard began with the
 * residue and no reducing end, which no reader takes as a structure, and paste did nothing - on its own
 * or onto a residue. The same went for the first residue of any structure selected without its
 * reducing end. It is now copied with the reducing end it stands on, as when that is selected too.
 */
public class CopyAMonosaccharideTest {

	private static final String GLCNAC = "WURCS=2.0/1,1,0/[a2122h-1b_1-5_2*NCC/3=O]/1/";
	private static final String CHITOBIOSE = "WURCS=2.0/1,2,1/[a2122h-1b_1-5_2*NCC/3=O]/1-1/a4-b1";

	private BuilderWorkspace workspace;
	private GlycanDocument document;

	@Before
	public void open() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
		document = workspace.getStructures();
	}

	private Glycan show(String _wurcs) throws Exception {
		document.setStructures(Collections.singletonList(
				new WURCS2Parser().readGlycan(_wurcs, workspace.getDefaultMassOptions())));
		return document.getStructures().iterator().next();
	}

	/** Its monosaccharides, without the reducing end: what clicking them selects. */
	private static HashSet<Residue> monosaccharidesOf(Glycan _structure) {
		HashSet<Residue> selection = new HashSet<Residue>();
		for (Residue residue : _structure.getAllResidues())
			if (residue.isSaccharide()) selection.add(residue);
		return selection;
	}

	/** What GlycanCanvas.copy puts on the clipboard, parsed back as paste parses it. */
	private Collection<Glycan> copied(HashSet<Residue> _selection) throws Exception {
		return document.parseString(GlycanDocument.toString(document.extractView(_selection)));
	}

	/** Pasted with nothing selected, it is a second structure, the same as the first. */
	@Test
	public void aLoneMonosaccharideIsPastedAsAStructure() throws Exception {
		for (String sequence : new String[] { GLCNAC, CHITOBIOSE }) {
			Glycan original = show(sequence);
			Collection<Glycan> clipboard = copied(monosaccharidesOf(original));
			assertNotNull(sequence + ": the clipboard is not read", clipboard);

			document.addStructures(null, clipboard);

			assertEquals(sequence, 2, document.getStructures().size());
			Iterator<Glycan> structures = document.getStructures().iterator();
			structures.next();
			Glycan pasted = structures.next();
			assertTrue(pasted.getRoot().isReducingEnd());
			assertEquals(sequence, new WURCS2Parser().toWURCS(original), new WURCS2Parser().toWURCS(pasted));
			assertEquals(original.computeMass(), pasted.computeMass(), 1e-4);
		}
	}

	/** Pasted onto a residue, it is added to that residue. */
	@Test
	public void aLoneMonosaccharideIsPastedOntoAResidue() throws Exception {
		Glycan original = show(GLCNAC);
		Collection<Glycan> clipboard = copied(monosaccharidesOf(original));
		Residue glcNAc = original.getRoot().firstChild();

		document.addStructures(glcNAc, clipboard);

		assertEquals(1, document.getStructures().size());
		assertEquals(1, glcNAc.getNoChildren());
		assertEquals("GlcNAc", glcNAc.firstChild().getTypeName());
	}

	/** The reducing end comes as it was: a GlcNAc on an alditol marker is copied as an alditol. */
	@Test
	public void theReducingEndComesAsItWas() throws Exception {
		Glycan original = show(GLCNAC);
		original.setReducingEndType(org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary
				.findResidueType("redEnd"));
		Collection<Glycan> clipboard = copied(monosaccharidesOf(original));

		Glycan pasted = clipboard.iterator().next();
		assertEquals("redEnd", pasted.getRoot().getTypeName());
		assertTrue(pasted.getRoot().firstChild().isAlditol());
	}
}
