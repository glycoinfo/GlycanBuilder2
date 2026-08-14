package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That asking for the average mass gives the average mass (#127).
 *
 * <p>{@code MassOptions.ISOTOPE} could be set to {@code AVG}, and the Mass options dialog offered
 * it, and nothing read it: every mass came from the monoisotopic figure, so choosing AVG changed
 * the dialog and not the answer. Each residue had carried its average mass alongside its
 * monoisotopic one all along, and nothing had ever asked for it.
 *
 * <p>A setting that appears to work is worse than one that is missing. Someone weighing a glycan
 * for a low-resolution instrument, where the average is the right figure, was handed the
 * monoisotopic one and told it was what they asked for.
 *
 * <p>The expected figures are literature values rather than this code's own output, since a test
 * that pins what the code happens to produce would have passed just as well before the fix.
 */
public class AverageMassIsAverageTest {

	/** Free glucose: 180.0634 monoisotopic, 180.156 average. */
	private static final String GLC = "freeEnd--?b1D-Glc,p";

	/** Man₃GlcNAc₂, the N-glycan core: 910.3278 monoisotopic, 910.82 average. */
	private static final String CORE =
			"freeEnd--?b1D-GlcNAc,p--4b1D-GlcNAc,p--4b1D-Man,p--3b1D-Man,p--?b1D-Man,p";

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** The average is the average, to the precision the literature quotes it at. */
	@Test
	public void theAverageMassIsTheAverageMass() {
		assertEquals(180.156, weigh(GLC, MassOptions.ISOTOPE_AVG), 0.001);
		assertEquals(910.82, weigh(CORE, MassOptions.ISOTOPE_AVG), 0.01);
	}

	/**
	 * And the monoisotopic figure is untouched, which is the more important half.
	 *
	 * <p>Everything that weighs anything goes through the same path, so the way to get this wrong is
	 * to change what MONO answers while making AVG work.
	 */
	@Test
	public void theMonoisotopicMassIsUnchanged() {
		assertEquals(180.0634, weigh(GLC, MassOptions.ISOTOPE_MONO), 0.0001);
		assertEquals(910.3278, weigh(CORE, MassOptions.ISOTOPE_MONO), 0.0001);
	}

	/** They are not the same number, which is what the report was about. */
	@Test
	public void theTwoAnswersDiffer() {
		assertNotEquals("AVG still answers the monoisotopic mass",
				weigh(CORE, MassOptions.ISOTOPE_MONO), weigh(CORE, MassOptions.ISOTOPE_AVG), 0.01);
	}

	/**
	 * The derivatization follows the same choice.
	 *
	 * <p>A permethylated glycan's added methyls have an average mass too, and taking them from the
	 * monoisotopic table while taking the residues from the average one would give a figure that is
	 * neither - the kind of wrong answer that looks plausible at every digit that matters.
	 */
	@Test
	public void aDerivatizationIsWeighedTheSameWay() {
		double mono = weigh(CORE, MassOptions.ISOTOPE_MONO, MassOptions.PERMETHYLATED);
		double average = weigh(CORE, MassOptions.ISOTOPE_AVG, MassOptions.PERMETHYLATED);

		assertEquals("permethylated Man3GlcNAc2, monoisotopic", 1148.5938, mono, 0.001);
		assertTrue("the permethylated average should sit above the monoisotopic figure by about the "
				+ "same margin the underivatized pair does: " + mono + " against " + average,
				average - mono > 0.4);
	}

	private static double weigh(String structure, String isotope) {
		return weigh(structure, isotope, MassOptions.NO_DERIVATIZATION);
	}

	private static double weigh(String structure, String isotope, String derivatization) {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		try {
			document.importFromString(structure, "gws");
		} catch (Exception cannotRead) {
			throw new IllegalStateException(structure, cannotRead);
		}

		Glycan glycan = document.getStructures().get(0);
		MassOptions options = glycan.getMassOptions();
		options.DERIVATIZATION = derivatization;
		options.ION_CLOUD = new IonCloud();
		options.ISOTOPE = isotope;
		glycan.setMassOptions(options);

		return glycan.computeMass();
	}
}
