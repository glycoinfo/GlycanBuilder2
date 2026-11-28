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
 * The WURCS of #268 is read, and what was drawn to make it is not written as it (#268).
 *
 * <p>Drawn: Glc 4 - [Glc β]n closing on 6, and after it a Glc β1-3. Exported as
 * {@code a4-b1_b3-c1_b1-b6~n}, which in WURCS is a repeat of Glc with a Glc at 3 of every unit, the
 * WURCS could not be read back at all: a repeat of one residue lost its end marker, placed at the
 * residue's own anomeric carbon. Since #269 it is read - as the repeat with a side chain WURCS says it
 * is - and the drawing, a chain after the repeat at a carbon the repeat does not close on, is refused
 * rather than written as that.
 */
public class ARepeatWithASideChainTest {

	private static final String EXPORTED = "WURCS=2.0/2,3,3/[a2122h-1x_1-5][a2122h-1b_1-5]/1-2-2/a4-b1_b3-c1_b1-b6~n";

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** It is read, as a repeat of Glc with a Glc at 3 of each unit, and written back as read. */
	@Test
	public void itIsReadAsTheRepeatItSays() throws Exception {
		Glycan read = new WURCS2Parser().readGlycan(EXPORTED, workspace.getDefaultMassOptions());

		Residue unit = null, end = null;
		for (Residue residue : read.getAllResidues()) {
			if (residue.isEndRepetition()) end = residue;
			if (residue.getParent() != null && residue.getParent().isStartRepetition()) unit = residue;
		}
		assertTrue("the repeat has no end", end != null);
		assertEquals(unit, end.getParent());
		boolean sideChainAt3 = false;
		for (org.eurocarbdb.application.glycanbuilder.linkage.Linkage child : unit.getChildrenLinkages())
			if (child.getChildResidue().getTypeName().equals("Glc") && "3".equals(child.getParentPositionsString()))
				sideChainAt3 = true;
		assertTrue("the Glc at 3 is not on the unit", sideChainAt3);
		assertEquals(EXPORTED, new WURCS2Parser().toWURCS(read));
	}

	/** What was drawn, the Glc after the repeat at 3 where it closes on 6, is not written as that. */
	@Test
	public void theDrawingIsNotWrittenAsThat() throws Exception {
		Glycan drawn = Glycan.fromString("freeEnd--1?1D-Glc,p--4[--4b1D-Glc,p--6]--3b1D-Glc,p$MONO,Und,0,0,freeEnd");
		try {
			new WURCS2Parser().toWURCS(drawn);
			fail("written as a repeat with a side chain");
		} catch (Exception refused) {
			assertTrue(refused.getMessage(), refused.getMessage().contains("where the repeat closes on 6"));
		}
	}
}
