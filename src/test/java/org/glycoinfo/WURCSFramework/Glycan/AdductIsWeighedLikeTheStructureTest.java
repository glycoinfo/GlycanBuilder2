package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That an m/z weighs its adduct the same way it weighed the structure (#203).
 *
 * <p>#127 brought the neutral mass under {@code MassOptions.ISOTOPE}. It did not reach the adducts:
 * {@code IonCloud} captured an ion's mass when the ion was <em>set</em>, which is always the
 * monoisotopic figure, so an average neutral mass arrived carrying a monoisotopic adduct and the m/z
 * was neither.
 *
 * <p><b>The test is written on potassium and chloride on purpose.</b> Sodium has one stable isotope,
 * so its average and monoisotopic masses are the same number and a sodiated m/z was right by accident
 * throughout - a test written on the default adduct would have passed before the fix. Sodium appears
 * here as the control that says so.
 *
 * <p>The expected differences are the atomic ones, from the periodic table rather than from this
 * code: K 39.0983 against 38.96371, Cl 35.4525 against 34.96885. The electron taken off for the
 * charge is a constant and cancels.
 */
public class AdductIsWeighedLikeTheStructureTest {

	/** Man₃GlcNAc₂, the N-glycan core: 910.3278 monoisotopic, 910.82 average. */
	private static final String CORE =
			"freeEnd--?b1D-GlcNAc,p--4b1D-GlcNAc,p--4b1D-Man,p--3b1D-Man,p--?b1D-Man,p";

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/**
	 * A potassiated m/z gains the adduct's average-minus-monoisotopic difference on top of the
	 * structure's own.
	 *
	 * <p>This is the assertion that fails without the fix, by 0.1346 - small, never zero, and
	 * plausible at every digit somebody would think to check.
	 */
	@Test
	public void potassiumIsAveragedToo() {
		assertEquals("the potassium adduct is still monoisotopic in an average m/z",
				0.134594, adductContributionToTheAverage(MassOptions.ION_K), 0.001);
	}

	/** Chloride is the same fault at four times the size. */
	@Test
	public void chlorideIsAveragedToo() {
		assertEquals("the chloride adduct is still monoisotopic in an average m/z",
				0.483685, adductContributionToTheAverage(MassOptions.ION_CL), 0.001);
	}

	/**
	 * Sodium contributes nothing, having one stable isotope — the control.
	 *
	 * <p>If this ever fails, the adduct is being averaged against something that is not the atom.
	 */
	@Test
	public void sodiumHasNothingToAverage() {
		assertEquals("sodium has one stable isotope and cannot differ",
				0., adductContributionToTheAverage(MassOptions.ION_NA), 0.0001);
	}

	/**
	 * And the monoisotopic m/z is unchanged, which is the half that matters more.
	 *
	 * <p>Man₃GlcNAc₂ 910.3278 plus a sodium ion, 22.98977 less the electron.
	 */
	@Test
	public void theMonoisotopicMZIsUnchanged() {
		assertEquals(933.3170, mz(MassOptions.ISOTOPE_MONO, MassOptions.ION_NA), 0.001);
		assertEquals(949.2911, mz(MassOptions.ISOTOPE_MONO, MassOptions.ION_K), 0.001);
	}

	/**
	 * How much of the m/z's average-minus-monoisotopic gap the adduct is responsible for.
	 *
	 * <p>The structure's own gap is subtracted, so what is left is the adduct's alone and can be
	 * compared against the periodic table.
	 */
	private static double adductContributionToTheAverage(String ion) {
		double mzGap = mz(MassOptions.ISOTOPE_AVG, ion) - mz(MassOptions.ISOTOPE_MONO, ion);
		double massGap = mass(MassOptions.ISOTOPE_AVG) - mass(MassOptions.ISOTOPE_MONO);
		return mzGap - massGap;
	}

	private static double mz(String isotope, String ion) {
		return weigh(isotope, ion).computeMZ();
	}

	private static double mass(String isotope) {
		return weigh(isotope, null).computeMass();
	}

	private static Glycan weigh(String isotope, String ion) {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		try {
			document.importFromString(CORE, "gws");
		} catch (Exception cannotRead) {
			throw new IllegalStateException(CORE, cannotRead);
		}

		Glycan glycan = document.getStructures().get(0);
		MassOptions options = glycan.getMassOptions();
		options.DERIVATIZATION = MassOptions.NO_DERIVATIZATION;
		options.ISOTOPE = isotope;
		options.ION_CLOUD = new IonCloud();
		if (ion != null)
			options.ION_CLOUD.set(ion, 1);
		glycan.setMassOptions(options);

		return glycan;
	}
}
