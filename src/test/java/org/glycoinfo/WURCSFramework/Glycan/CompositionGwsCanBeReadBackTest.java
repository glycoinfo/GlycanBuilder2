package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.Test;

/**
 * A composition read from WURCS can be written to GWS and read back.
 *
 * <p>It could not. The marker a composition carries to say that no linkages are known was written
 * out like a child - {@code --?no glycosidic linkages} - and the parser, reading everything after
 * {@code --?} as a linkage, met a sentence and stopped: <em>invalid format for linkage: " glycosidic
 * linkages"</em>. So a composition could not be moved between the two formats the application itself
 * uses.
 */
public class CompositionGwsCanBeReadBackTest {

	/**
	 * Hex3HexNAc2 - the N-glycan core's composition - with its residues joined by linkages of unknown
	 * position, as GlyTouCan holds compositions. It was the same residues with no linkage at all, which
	 * since 2026-11-29 is read as what it is, separate monosaccharides, and refused.
	 */
	private static final String AS_WURCS = "WURCS=2.0/2,5,4/[u2122h_2*NCC/3=O][u1122h]/1-1-2-2-2/a?|b?|c?|d?|e?}-{a?|b?|c?|d?|e?_a?|b?|c?|d?|e?}-{a?|b?|c?|d?|e?_a?|b?|c?|d?|e?}-{a?|b?|c?|d?|e?_a?|b?|c?|d?|e?}-{a?|b?|c?|d?|e?";

	@Test
	public void aCompositionSurvivesTheRoundTrip() throws Exception {
		Glycan composition = fromWurcs();
		String gws = composition.toString();

		assertFalse("the marker is written into the GWS: " + gws,
				gws.contains(Residue.NO_GLYCOSIDIC_LINKAGES));

		Glycan readBack = Glycan.fromString(gws);
		assertNotNull("the GWS a composition writes cannot be read back: " + gws, readBack);
	}

	/** And weighs the same on the way back, which is what the round trip is for. */
	@Test
	public void andWeighsTheSameOnTheWayBack() throws Exception {
		Glycan composition = fromWurcs();

		Glycan readBack = Glycan.fromString(composition.toString());

		assertEquals(composition.computeMass(), readBack.computeMass(), 1e-6);
		assertEquals(910.3278, readBack.computeMass(), 1e-3);
	}

	private static Glycan fromWurcs() throws Exception {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
		GlycanDocument document = workspace.getStructures();
		document.importFromString(AS_WURCS, "wurcs2");

		List<Glycan> structures = document.getStructures();
		return structures.get(0);
	}
}
