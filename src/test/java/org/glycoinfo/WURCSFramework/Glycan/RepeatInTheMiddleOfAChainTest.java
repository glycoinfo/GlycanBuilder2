package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.Before;
import org.junit.Test;

/**
 * A repeat made of residues in the middle of a chain keeps the chain after it (#271).
 *
 * <p>On the hyaluronic acid template, a repeat of the 2nd and 3rd residues put its end marker on the
 * GlcNAc's 3, where the next GlcA already was: refused by #34's rule, without the refusal being looked
 * at, and the GlcA was then moved onto a marker hanging on nothing. The GlcA vanished, the repeat had no
 * end, and the canvas could freeze drawing it. And the end marker never had the unit's first residue's
 * carbon, so every repeat made on the canvas was written with it as ?.
 */
public class RepeatInTheMiddleOfAChainTest {

	/** The Hyaluronic Acid template. */
	private static final String HYALURONIC_ACID =
			"freeEnd--?b1D-GlcNAc,p--3b1D-GlcA,p--4b1D-GlcNAc,p--3b1D-GlcA,p$MONO,Und,0,0,freeEnd";

	private GlycanDocument document;
	private Residue first, second, third, fourth;

	@Before
	public void open() throws Exception {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
		document = workspace.getStructures();
		document.setStructures(new ArrayList<Glycan>(document.parseString(HYALURONIC_ACID)));
		Glycan template = structure();
		first = template.getRoot().firstChild();
		second = first.firstChild();
		third = second.firstChild();
		fourth = third.firstChild();
	}

	private Glycan structure() {
		return document.getStructures().iterator().next();
	}

	/** The 2nd and 3rd residues repeated: the 4th, a GlcA, follows the closing bracket. */
	@Test
	public void theChainAfterTheRepeatIsKept() throws Exception {
		assertTrue(document.createRepetition(null, Arrays.asList(second, third)));

		assertEquals("freeEnd--?b1D-GlcNAc,p--3[--3b1D-GlcA,p--4b1D-GlcNAc,p--3]--3b1D-GlcA,p$MONO,Und,0,0,freeEnd",
				structure().toString());
		String written = new WURCS2Parser().toWURCS(structure());
		assertEquals("WURCS=2.0/2,4,4/[a2122h-1b_1-5_2*NCC/3=O][a2122A-1b_1-5]/1-2-1-2/a3-b1_b4-c1_c3-d1_b1-c3~n", written);
		// and read back as drawn
		assertEquals(written, new WURCS2Parser().toWURCS(new WURCS2Parser().readGlycan(written,
				structure().getMassOptions())));
	}

	/** A repeat at the reducing end is written with the unit's first carbon, 1, not ?. */
	@Test
	public void theRepeatKeepsItsFirstResiduesCarbon() throws Exception {
		assertTrue(document.createRepetition(null, Arrays.asList(first, second)));
		assertTrue(new WURCS2Parser().toWURCS(structure()).endsWith("_a1-b?~n"));
	}

	/**
	 * A unit whose last residue already bonds a residue of the unit where the repeat would close is
	 * refused, saying so, and nothing is changed.
	 */
	@Test
	public void aUnitWithNoRoomToCloseIsRefused() throws Exception {
		try {
			document.createRepetition(third, Arrays.asList(second, third, fourth));
			fail("made a repeat with no room for its end");
		} catch (Exception refused) {
			assertTrue(refused.getMessage(), refused.getMessage().contains("closes on 3 of its last residue, GlcNAc"));
		}
		assertEquals(HYALURONIC_ACID, structure().toString());
	}
}
