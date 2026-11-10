package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.AbstractGlycanRenderer;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The tetrose rows, and the mass of a structure holding a placeholder (#285).
 *
 * <p>thrTet and eryTet had the composition "C4H804" - a zero for the O, four carbons and 804
 * hydrogens - and a pyranose ring, which a four-carbon sugar cannot close. Thr and Ery, the tetroses
 * the WURCS reader names, had no configuration, ring or stereo, so they could not be written. And an
 * Assigned placeholder was weighed as one hydrogen, which put a figure under the structure that
 * looked like a measurement.
 */
public class TetrosesAndPlaceholderMassTest {

	/** C4H8O4, worked by hand: C 12, H 1.00782503207, O 15.99491461956. */
	private static final double C4H8O4 = 120.0423;

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** Each weighs C4H8O4 on a free reducing end. thrTet and eryTet weighed 858.2913. */
	@Test
	public void theTetrosesWeighC4H8O4() throws Exception {
		for (String name : new String[] { "thrTet", "eryTet", "Thr", "Ery" })
			assertEquals(name, C4H8O4, alone(name).computeMass(), 1e-4);
	}

	/** A furanose: the largest ring a tetrose can close. */
	@Test
	public void theTetrosesAreFuranoses() throws Exception {
		for (String name : new String[] { "thrTet", "eryTet", "Thr", "Ery" })
			assertEquals(name, 'f', NonSymbolicResidueDictionary.getResidueType(name).getRingSize());
	}

	/**
	 * And they are written to WURCS, closing on carbon 4, and read back as the same string. Thr and Ery
	 * could not be written at all; thrTet and eryTet were written closing on carbon 5 of four.
	 */
	@Test
	public void theTetrosesAreWrittenAndReadBack() throws Exception {
		String[][] expected = {
				{ "Thr", "WURCS=2.0/1,1,0/[a12h-1x_1-4]/1/" },
				{ "Ery", "WURCS=2.0/1,1,0/[a22h-1x_1-4]/1/" },
				{ "thrTet", "WURCS=2.0/1,1,0/[a34h-1x_1-4]/1/" },
				{ "eryTet", "WURCS=2.0/1,1,0/[a44h-1x_1-4]/1/" },
		};
		for (String[] one : expected) {
			String written = new WURCS2Parser().writeGlycan(alone(one[0]));
			assertEquals(one[0], one[1], written);
			assertEquals(one[0], new WURCSFactory(written).getWURCS(), written);
			assertEquals(one[0], written,
					new WURCS2Parser().writeGlycan(new WURCS2Parser().readGlycan(written, new MassOptions())));
		}
	}

	/** While a placeholder is in the structure, no m/z is given; it has no mass to give. */
	@Test
	public void aStructureWithAPlaceholderHasNoMassShown() throws Exception {
		Glycan glucose = Glycan.fromString("freeEnd--?b1D-Glc,p$MONO,perMe,Na,0,freeEnd");
		String withoutOne = new Label().text(glucose);
		assertTrue(withoutOne, withoutOne.matches("m/z: [0-9]+\\.[0-9]{4} \\[.*\\]"));
		assertFalse(withoutOne, withoutOne.contains(AbstractGlycanRenderer.UNWEIGHED));

		Residue sugar = glucose.getRoot().getChildAt(0);
		sugar.addChild(ResidueDictionary.newResidue("Assigned"));
		String withOne = new Label().text(glucose);
		assertEquals("m/z: " + AbstractGlycanRenderer.UNWEIGHED + " [" + glucose.getMassOptions() + "]", withOne);
	}

	// ------------------------------------------------------------------------------------------------

	private static Glycan alone(String name) throws Exception {
		Residue root = ResidueDictionary.createReducingEnd("freeEnd");
		root.addChild(new Residue(NonSymbolicResidueDictionary.getResidueType(name)));
		Glycan glycan = new Glycan(root, false, new MassOptions());
		MassOptions underivatized = new MassOptions();
		underivatized.setDerivatization(MassOptions.NO_DERIVATIZATION);
		glycan.setMassOptions(underivatized);
		return glycan;
	}

	/** The renderer's own label, as it is drawn beneath a structure. */
	private static final class Label extends GlycanRendererAWT {
		String text(Glycan structure) {
			return getMassText(structure);
		}
	}
}
