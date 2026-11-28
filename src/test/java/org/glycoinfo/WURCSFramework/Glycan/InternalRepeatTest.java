package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
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
 * An internal repeat - a repeat with the chain continuing after it - is read and written as it is, and
 * one WURCS cannot write is refused, saying so (#269).
 *
 * <p>WURCS writes the chain after an internal repeat as bonded to the repeat's last residue at the
 * carbon the repeat closes on, which in the last unit is free: G43728TR's a4-b1_b4-c1_b1-b4~n. Any other
 * bond to a residue of the repeat is part of every unit. The issue's structure, [Glc(b1-6)Glc]n with a
 * Glc β1-3 on the last unit's Glc, was written c3-d1 beside b1-c6~n and read back as a Glc on every
 * unit. Read, a repeat of one residue lost its end marker - placed at the residue's own anomeric
 * carbon - and was written back with no repeat at all; and a chain after a repeat, read before the
 * repeat's end, took the carbon the end marker needed.
 */
public class InternalRepeatTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	private static Glycan read(String _wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(_wurcs, workspace.getDefaultMassOptions());
	}

	/** Each is written back as it was read. */
	@Test
	public void eachIsWrittenBackAsRead() throws Exception {
		String[] sequences = {
				"WURCS=2.0/1,1,1/[a2122h-1b_1-5]/1/a1-a4~n",                                        // [Glc]n
				"WURCS=2.0/2,3,3/[a2122h-1b_1-5][a2112h-1b_1-5]/1-2-2/a4-b1_b4-c1_b1-b4~n",           // G43728TR
				"WURCS=2.0/2,4,4/[a2122h-1x_1-5][a2122h-1b_1-5]/1-2-2-2/a4-b1_b6-c1_c6-d1_b1-c6~n",   // the issue's, at 6
				// G42488IE: a repeat of one Rha after a repeat of one Man
				"WURCS=2.0/3,4,5/[a2122h-1b_1-5][a1122h-1a_1-5][a2211m-1a_1-5]/1-2-3-3/a6-b1_b6-c1_c3-d1_b1-b6~n_c1-c3~n",
		};
		for (String sequence : sequences)
			assertEquals(sequence, sequence, new WURCS2Parser().toWURCS(read(sequence)));
	}

	/** The chain after a repeat is after its end marker, not a residue of the repeat. */
	@Test
	public void theChainAfterARepeatIsAfterItsEnd() throws Exception {
		Glycan g43728tr = read("WURCS=2.0/2,3,3/[a2122h-1b_1-5][a2112h-1b_1-5]/1-2-2/a4-b1_b4-c1_b1-b4~n");
		Residue end = null;
		for (Residue residue : g43728tr.getAllResidues())
			if (residue.isEndRepetition()) end = residue;
		assertTrue("the repeat has no end", end != null);
		assertEquals(1, end.getNoChildren());
		assertEquals("Gal", end.firstChild().getTypeName());
	}

	/**
	 * The issue's structure, a Glc on the last unit at a carbon the repeat does not close on, is refused:
	 * written as WURCS writes it, it is read back with a Glc on every unit.
	 */
	@Test
	public void aChainAfterARepeatElsewhereIsRefused() throws Exception {
		Glycan drawn = Glycan.fromString(
				"freeEnd--1?1D-Glc,p--4[--4b1D-Glc,p--6b1D-Glc,p--6]--3b1D-Glc,p$MONO,Und,0,0,freeEnd");
		try {
			new WURCS2Parser().toWURCS(drawn);
			fail("written as a Glc on every unit");
		} catch (Exception refused) {
			assertTrue(refused.getMessage(),
					refused.getMessage().contains("continues the chain after a repeat at 3 of its last Glc, where the repeat closes on 6"));
		}
	}
}
