package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.CrossRingFragmentDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.CompositionOptions;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The 4-deoxy pentose is a pyranose (#386).
 *
 * <p>The dictionary gave it the ring f, which it cannot close: a furanose closes from C1 to O4, and a
 * 4-deoxy pentose has no O4. Its only ring is the pyranose, C1 to O5, as glycompconverter's 4dPen and
 * GlyTouCan's one entry for it, axxdh-1x_1-5, have it. Its eighteen cross-ring fragment rows were
 * for the furanose; a pentopyranose has none in the dictionary, Xyl and Ara included, so they are gone
 * rather than rewritten.
 */
public class FourDeoxyPentoseIsAPyranoseTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	/** A new one is a pyranose, and weighs what it did. */
	@Test
	public void itIsAPyranose() throws Exception {
		Residue residue = ResidueDictionary.newResidue("4dPen");
		assertEquals('p', residue.getRingSize());
		assertEquals('p', residue.getType().getRingSize());

		Glycan structure = Glycan.fromString("freeEnd--?b1D-4dPen,p$MONO,Und,0,0,freeEnd");
		MassOptions none = new MassOptions();
		none.setDerivatization(MassOptions.NO_DERIVATIZATION);
		structure.setMassOptions(none);
		assertEquals(134.0579, structure.computeMass(), 1e-4);
		assertEquals(structure.toString(), Glycan.fromString(structure.toString()).toString());
	}

	/** No cross-ring fragments for a ring it cannot close. */
	@Test
	public void noFragmentsOfAFuranose() throws Exception {
		Residue furanose = ResidueDictionary.newResidue("4dPen");
		furanose.setRingSize('f');
		assertTrue(CrossRingFragmentDictionary.getCrossRingFragmentTypes(furanose).isEmpty());
	}

	/** Its composition is written as before, the pyranose glycompconverter writes (#220). */
	@Test
	public void itsCompositionIsUnchanged() throws Exception {
		CompositionOptions counts = new CompositionOptions();
		counts.DPEN = 1;
		MassOptions neutral = new MassOptions();
		neutral.DERIVATIZATION = MassOptions.NO_DERIVATIZATION;
		neutral.ION_CLOUD = new IonCloud();
		assertEquals("WURCS=2.0/1,1,0/[axxdh-1x_1-5]/1/",
				new WURCS2Parser().toWURCS(counts.getCompositionAsGlycan(neutral)));
	}
}
