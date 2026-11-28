package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.CoreType;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.CoreDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The GAG repeat templates are the repeats they are named for (#330).
 *
 * <p>Eight sulfated a carbon with no free hydroxyl and could not be written: the dermatan GalNAc at C5,
 * the carbon of its ring, and the heparan GlcN at C4, where the next GlcA is bonded. Dermatan sulfate
 * is sulfated at GalNAc C4 and C6, heparan sulfate at GlcN N, C3 and C6. The maintainer's decision,
 * 2026-11-28: C5 becomes C6, and GlcN C4 becomes C3. The templates are renamed for the positions they
 * carry.
 *
 * <p>And the iduronic acid of dermatan sulfate and heparin is α-L-IdoA, a2121A - 2,775 GlyTouCan
 * entries, against 161 for a1212A. The templates drew D-IdoA, and since #313 that is what was written.
 * L by the maintainer's decision of the same day.
 */
public class GagTemplatesTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	/** Every GAG template is written, and read back as written. */
	@Test
	public void everyGagTemplateIsWrittenAndReadBack() throws Exception {
		int checked = 0;
		for (CoreType core : CoreDictionary.getCores()) {
			if (!core.getName().startsWith("gag")) continue;
			String wurcs = written(core.getName());
			Glycan read = new WURCS2Parser().readGlycan(wurcs, new MassOptions());
			assertEquals(core.getName(), wurcs, new WURCS2Parser().toWURCS(read));
			checked++;
		}
		// hyaluronan, five chondroitins, keratan, eight dermatans, heparin and seven heparans
		assertEquals(23, checked);
	}

	/** The dermatan GalNAc is sulfated at 4 and 6, never 5; the heparan GlcN at N, 3 and 6, never 4. */
	@Test
	public void theSulfatesAreWhereTheNamesSay() throws Exception {
		String[][] cases = {
				{ "gagdermatan6", "6*OSO" }, { "gagdermatan46", "4*OSO" }, { "gagdermatan46", "6*OSO" },
				{ "gagdermatan26", "6*OSO" }, { "gagdermatan246", "6*OSO" },
				{ "gagheparan23", "3*OSO" }, { "gagheparan223", "3*OSO" }, { "gagheparan236", "3*OSO" },
				{ "gagheparan2236", "3*OSO" }, { "gagheparan2236", "2*NSO" },
		};
		for (String[] one : cases)
			assertTrue(one[0] + " " + one[1], written(one[0]).contains(one[1]));
		for (CoreType core : CoreDictionary.getCores()) {
			if (core.getName().startsWith("gagdermatan"))
				assertFalse(core.getName(), written(core.getName()).contains("5*OSO"));
			if (core.getName().startsWith("gagheparan"))
				assertFalse(core.getName(), written(core.getName()).contains("4*OSO"));
		}
	}

	/** The old names are gone, so that none names a position it does not carry. */
	@Test
	public void theOldNamesAreGone() {
		for (String old : new String[] { "gagdermatan5", "gagdermatan45", "gagdermatan25", "gagdermatan245",
				"gagheparan24", "gagheparan224", "gagheparan246", "gagheparan2246" })
			for (CoreType core : CoreDictionary.getCores())
				assertFalse(old, core.getName().equals(old));
	}

	/** The iduronic acid of dermatan sulfate and heparin is L: a2121A, not a1212A. */
	@Test
	public void theIduronicAcidIsL() throws Exception {
		for (CoreType core : CoreDictionary.getCores()) {
			if (!core.getName().startsWith("gagdermatan") && !core.getName().equals("gagheparin")) continue;
			String wurcs = written(core.getName());
			assertTrue(core.getName(), wurcs.contains("[a2121A-1a_1-5"));
			assertFalse(core.getName(), wurcs.contains("a1212A"));
		}
	}

	private static String written(String _core) throws Exception {
		Residue root = CoreDictionary.newCore(_core);
		Glycan glycan = root.isReducingEnd() ? new Glycan(root, false, new MassOptions())
				: new Glycan(root, true, new MassOptions());
		return new WURCS2Parser().toWURCS(glycan);
	}
}
