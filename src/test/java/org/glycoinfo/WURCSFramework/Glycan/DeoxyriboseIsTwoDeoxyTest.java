package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.converterGlycoCT.GlycoCTCondensedParser;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Deoxyribose is 2-deoxyribose, in its IUPAC name as in the rest of its row (#402).
 *
 * <p>Its description, its link positions (no 2, the deoxy carbon), its furanose ring and the
 * cross-ring fragment rows it shares with 2dPen all describe 2-deoxyribose, the deoxyribose of DNA.
 * Its IUPAC name said 4-deoxy-Rib, which GlycoCT is written from: the export came back empty.
 */
public class DeoxyriboseIsTwoDeoxyTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	@Test
	public void itsIupacNameIsTwoDeoxy() {
		assertEquals("2-deoxy-Rib$", NonSymbolicResidueDictionary.findResidueType("dRib").getIupacName());
	}

	/** Written to GlycoCT as a 2-deoxy pentofuranose; it was not written at all. */
	@Test
	public void itIsWrittenToGlycoCtWithItsDeoxyAtTwo() throws Exception {
		Glycan structure = Glycan.fromString("freeEnd--?b1D-dRib,f--3b1D-Glc,p$MONO,Und,0,0,freeEnd");
		String glycoct = new GlycoCTCondensedParser(false).writeGlycan(structure);
		assertTrue(glycoct, glycoct.contains("-PEN-1:4|2:d"));
	}

	/** Nothing else about it changes: its mass and where it can be linked. */
	@Test
	public void itsMassAndPositionsAreAsBefore() throws Exception {
		Glycan structure = Glycan.fromString("freeEnd--?b1D-dRib,f$MONO,Und,0,0,freeEnd");
		MassOptions none = new MassOptions();
		none.setDerivatization(MassOptions.NO_DERIVATIZATION);
		structure.setMassOptions(none);
		assertEquals(134.0579, structure.computeMass(), 1e-4);

		Residue residue = ResidueDictionary.newResidue("dRib");
		residue.setRingSize('f');
		assertEquals("135", new String(residue.availableLinkagePositions()));
	}
}
