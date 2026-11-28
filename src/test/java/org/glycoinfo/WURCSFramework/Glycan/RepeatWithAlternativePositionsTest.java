package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A repeat with alternative linkage positions is read, and written back as read (#270).
 *
 * <p>Whether a residue bonded to a repeat continues the chain after it was decided by reading its
 * position, and the repeat's, as numbers: an alternative, 2|3, could not be read as one, and the
 * WURCS could not be read at all - For input string: "2/3". Both are GlyTouCan's, as GlyCosmos serves
 * them.
 */
public class RepeatWithAlternativePositionsTest {

	/** A repeat closing on 3|6, and the chain after it at 3|6. */
	private static final String G73472DM =
			"WURCS=2.0/2,3,3/[a2122h-1x_1-?][a2122h-1b_1-5]/1-2-2/b1-a3|a6_c1-b3|b6_b1-b3|b6~n";
	/** A repeat of three Xyl closing on 4, with a Gal at 2|3 of its first and an Ara at 2|3 of its last. */
	private static final String G79171HQ = "WURCS=2.0/3,5,5/[a212h-1b_1-5][a2112h-1x_1-5][a211h-1x_1-?]/1-2-1-1-3/"
			+ "a4-c1_c4-d1_b1-a2|a3_e1-d2|d3_a1-d4~n";

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	private static Glycan read(String _wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(_wurcs, workspace.getDefaultMassOptions());
	}

	/** Each is read, and written back as read. */
	@Test
	public void eachIsWrittenBackAsRead() throws Exception {
		for (String sequence : new String[] { G73472DM, G79171HQ })
			assertEquals(sequence, sequence, new WURCS2Parser().toWURCS(read(sequence)));
	}

	/** G79171HQ's Gal and Ara, at alternatives, are residues of the unit, not the chain after it. */
	@Test
	public void sideChainsAtAlternativesAreInTheUnit() throws Exception {
		for (Residue residue : read(G79171HQ).getAllResidues())
			if (residue.getTypeName().equals("Gal") || residue.getTypeName().equals("Ara")) {
				assertEquals("2/3", residue.getParentLinkage().getParentPositionsString());
				assertFalse(residue.getTypeName() + " after the repeat", residue.getParent().isEndRepetition());
			}
	}
}
