package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A heptopyranose has a methylation and an acetylation site at each free hydroxyl: six, at 1, 2, 3,
 * 4, 6 and 7 (#283).
 *
 * <p>Thirteen heptose rows recorded 5 and 3, and Hept 6 and 5, so a permethylated or peracetylated
 * structure with one of them was weighed light, with no warning - L-gro-D-manHep, the heptose of
 * bacterial lipopolysaccharide cores, among them. The rows are recounted from the structure: every
 * position with a free OH is a site (the maintainer, 2026-11-28), as a hexopyranose's five are.
 */
public class HeptoseDerivatizationSitesTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	/** Every heptopyranose row with no deoxy and no substituent of its own records six and six. */
	@Test
	public void everyHeptopyranoseRowRecordsSixSites() {
		List<ResidueType> rows = new ArrayList<ResidueType>(ResidueDictionary.allResidues());
		rows.addAll(NonSymbolicResidueDictionary.getResidues());

		List<String> wrong = new ArrayList<String>();
		int checked = 0;
		for (ResidueType row : rows) {
			if (!row.isSaccharide() || !"C7H14O7".equals(row.getComposition()) || row.getRingSize() != 'p') continue;
			// a heptulose's anomeric carbon is C2; its free hydroxyls are 1, 2, 3, 4, 5 and 7, six as well
			checked++;
			if (row.getNoMethyls() != 6 || row.getNoAcetyls() != 6)
				wrong.add(row.getName() + " " + row.getNoMethyls() + "/" + row.getNoAcetyls());
		}

		// 13 stereo-named heptoses, 5 prefix-named ones and Sed
		assertTrue("too few heptose rows found: " + checked, checked >= 19);
		assertEquals(wrong.toString(), 0, wrong.size());
	}

	/** And Hept, the heptose of unknown stereo, whose ring is not known: 6 and 5 until now. */
	@Test
	public void heptRecordsSixSites() {
		ResidueType hept = ResidueDictionary.findResidueType("Hept");

		assertEquals(6, hept.getNoMethyls());
		assertEquals(6, hept.getNoAcetyls());
	}

	/** The masses of the report: L-gro-D-manHep alone, permethylated and peracetylated. */
	@Test
	public void lGroDManHepWeighsWhatItsSixHydroxylsMake() throws Exception {
		assertEquals(294.1679, derivatized("WURCS=2.0/1,1,0/[a11221h-1a_1-5]/1/", MassOptions.PERMETHYLATED), 1e-3);
		assertEquals(462.1373, derivatized("WURCS=2.0/1,1,0/[a11221h-1a_1-5]/1/", MassOptions.PERACETYLATED), 1e-3);
	}

	private static double derivatized(String _wurcs, String _derivatization) throws Exception {
		Glycan read = new WURCS2Parser().readGlycan(_wurcs, new MassOptions());
		read.getMassOptions().setDerivatization(_derivatization);
		return read.computeMass();
	}
}
