package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That a reduced structure stays reduced across a save and a reload (#133).
 *
 * <p>The reducing end's type implies the sugar's form: redEnd and every reductive-amination label
 * reduce their sugar, leaving it acyclic. A file saved by 1.30.0 carries that as the ring letter
 * {@code ,o} and read back correctly; a file saved by anything earlier - or written by hand - says
 * {@code ,p}, and reading it back turned the alditol into a ring again: the WURCS reverted from
 * {@code [h2122h]} to a cyclic residue, two hydrogens lighter, with nothing said. Every .gws file
 * saved before 1.30.0 is in the second group.</p>
 */
public class GwsAlditolRestoreTest {

	private static final String ALDITOL_WURCS = "WURCS=2.0/1,1,0/[h2122h]/1/";

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void newWorkspace() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** A pre-1.30.0 file - redEnd with the ring letter still "p" - reads back reduced. */
	@Test
	public void aLegacyRedEndFileReadsBackReduced() throws Exception {
		Glycan reloaded = Glycan.fromString("redEnd--?b1D-Glc,p$MONO,Und,0,0,redEnd");

		Residue sugar = reloaded.getRoot().getChildAt(0);
		assertTrue("the sugar came back unreduced", sugar.isAlditol());
		assertEquals('o', sugar.getRingSize());
		assertEquals('?', sugar.getAnomericState());
		assertEquals(ALDITOL_WURCS, wurcsOf(reloaded));
	}

	/** A label reduces its sugar the same way, so a legacy labelled file reads back reduced too. */
	@Test
	public void aLegacyLabelledFileReadsBackReduced() throws Exception {
		Glycan reloaded = Glycan.fromString("2AB--?b1D-Glc,p$MONO,Und,0,0,2AB");

		assertTrue(reloaded.getRoot().getChildAt(0).isAlditol());
		assertEquals("the label is not carried by WURCS, but the reduction is",
				ALDITOL_WURCS, wurcsOf(reloaded));
	}

	/** A free reducing end stays a ring - the repair only says what the root already says. */
	@Test
	public void aFreeEndFileStaysARing() throws Exception {
		Glycan reloaded = Glycan.fromString("freeEnd--?b1D-Glc,p$MONO,Und,0,0,freeEnd");

		assertFalse("a free sugar was reduced by loading it",
				reloaded.getRoot().getChildAt(0).isAlditol());
		assertEquals("WURCS=2.0/1,1,0/[a2122h-1b_1-5]/1/", wurcsOf(reloaded));
	}

	/** What the UI sets, a save and a reload keep - the round trip that was broken. */
	@Test
	public void settingSavingAndReloadingAgree() throws Exception {
		Glycan edited = Glycan.fromString("freeEnd--?b1D-Glc,p$MONO,Und,0,0,freeEnd");
		edited.setReducingEndType(ResidueDictionary.getResidueType("redEnd"));
		String wurcsBefore = wurcsOf(edited);

		Glycan reloaded = Glycan.fromString(edited.toString());

		assertEquals(wurcsBefore, wurcsOf(reloaded));
		assertEquals(ALDITOL_WURCS, wurcsBefore);
	}

	/** And a 1.30.0-style file, whose ring letter already says "o", is left exactly as it is. */
	@Test
	public void aFileThatAlreadySaysOIsLeftAlone() throws Exception {
		Glycan reloaded = Glycan.fromString("redEnd--??1D-Glc,o$MONO,Und,0,0,redEnd");

		assertTrue(reloaded.getRoot().getChildAt(0).isAlditol());
		assertEquals(ALDITOL_WURCS, wurcsOf(reloaded));
	}

	/** A structure of nothing but the reducing end has no sugar to repair, and does not fall over. */
	@Test
	public void aBareReducingEndDoesNotFallOver() {
		assertTrue(Glycan.fromString("redEnd$MONO,Und,0,0,redEnd") != null
				|| Glycan.fromString("freeEnd$MONO,Und,0,0,freeEnd") != null);
	}

	private static String wurcsOf(Glycan structure) throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		document.addStructure(structure);

		return document.toString("wurcs2").strip();
	}
}
