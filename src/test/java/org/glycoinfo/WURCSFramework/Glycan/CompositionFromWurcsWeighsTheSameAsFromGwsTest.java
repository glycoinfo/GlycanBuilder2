package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.massutil.CompositionOptions;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.Test;

/**
 * One composition weighs one thing, however it was written.
 *
 * <p>Hex3HexNAc2 built through {@link CompositionOptions} - the route the composition dialog takes -
 * and the same composition imported from WURCS must agree. They did not: the WURCS one came out
 * <b>18.0106 light</b> underivatized, and a further methyl or acetyl group light on top of that once
 * derivatized.
 *
 * <p>The cause was a residue that is not a residue. {@code WURCSSequence2ToGlycan} hangs a synthetic
 * <em>"no glycosidic linkages"</em> marker off a composition's bracket to record that no linkages
 * are known. The renderers have always skipped it; the mass calculation had not been told about it,
 * so it was counted twice over - once among the members, where N members imply N-1 glycosidic bonds
 * and so subtracted one water too many, and once as an ordinary residue, where it collected a
 * derivatization adjustment for its single bond to the bracket.
 *
 * <p>The numbers below are the composition dialog's, which were already right and are what a user
 * sees on the canvas.
 */
public class CompositionFromWurcsWeighsTheSameAsFromGwsTest {

	/** Hex3HexNAc2 - the N-glycan core's composition. */
	private static final String AS_WURCS = "WURCS=2.0/2,5,0/[u2122h_2*NCC/3=O][u1122h]/1-1-2-2-2/";

	/** Underivatized, this is the Man3GlcNAc2 figure. */
	@Test
	public void underivatized() throws Exception {
		assertEquals(910.3278, massFromWurcs(MassOptions.NO_DERIVATIZATION), 1e-3);
		assertEquals(massFromTheCompositionDialog(MassOptions.NO_DERIVATIZATION),
				massFromWurcs(MassOptions.NO_DERIVATIZATION), 1e-6);
	}

	/** And derivatized, where the marker was costing a group of its own. */
	@Test
	public void derivatized() throws Exception {
		for (String derivatization : new String[] { MassOptions.PERMETHYLATED,
				MassOptions.PERACETYLATED, MassOptions.PERDMETHYLATED }) {
			assertEquals(derivatization,
					massFromTheCompositionDialog(derivatization), massFromWurcs(derivatization), 1e-6);
		}
	}

	private static double massFromWurcs(String derivatization) throws Exception {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
		GlycanDocument document = workspace.getStructures();
		document.importFromString(AS_WURCS, "wurcs2");

		List<Glycan> structures = document.getStructures();
		Glycan composition = structures.get(0);
		composition.setMassOptions(massOptions(derivatization));

		return composition.computeMass();
	}

	private static double massFromTheCompositionDialog(String derivatization) throws Exception {
		CompositionOptions counted = new CompositionOptions();
		counted.HEX = 3;
		counted.HEXNAC = 2;

		return counted.getCompositionAsGlycan(massOptions(derivatization)).computeMass();
	}

	private static MassOptions massOptions(String derivatization) {
		MassOptions options = new MassOptions();
		options.DERIVATIZATION = derivatization;
		options.ION_CLOUD = new IonCloud();

		return options;
	}
}
