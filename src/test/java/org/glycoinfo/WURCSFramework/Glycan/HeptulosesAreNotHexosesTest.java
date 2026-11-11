package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A hept-2-ulose is not read as an aldohexose (#310).
 *
 * <p>The naming converter named one by the aldohexose whose four stereocentres match its C3 to C6:
 * ha1122h, D-manno-hept-2-ulose, was read as Man - the wrong monosaccharide, CH2O light, and not
 * writable, with nothing to say so. Fourteen of the sixteen were. GlycanBuilder2 has a type for
 * sedoheptulose only, so the rest are refused with the reason; Sed, which read right and could not be
 * written, is written back.
 */
public class HeptulosesAreNotHexosesTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** Every hept-2-ulose but sedoheptulose is refused, and says why, rather than read as a hexose. */
	@Test
	public void aHeptuloseWithNoTypeIsRefused() {
		int refused = 0;
		for (String skeleton : heptuloses()) {
			if (skeleton.equals("ha1222h") || skeleton.equals("ha2111h")) continue;
			String wurcs = "WURCS=2.0/1,1,0/[" + skeleton + "-2b_2-6]/1/";
			try {
				Glycan read = new WURCS2Parser().readGlycan(wurcs, workspace.getDefaultMassOptions());
				fail(wurcs + " was read, as " + read.getRoot().getChildAt(0).getTypeName());
			} catch (Exception refusedAsItShouldBe) {
				assertTrue(wurcs + ": " + refusedAsItShouldBe.getMessage(),
						String.valueOf(refusedAsItShouldBe.getMessage()).contains(skeleton + " has 7 carbons"));
				refused++;
			}
		}
		assertEquals(14, refused);
	}

	/** Sedoheptulose, D and L, pyranose and furanose, weighs a heptulose and is written back as read. */
	@Test
	public void sedoheptuloseIsReadAndWrittenBack() throws Exception {
		for (String wurcs : new String[] { "WURCS=2.0/1,1,0/[ha1222h-2b_2-6]/1/", "WURCS=2.0/1,1,0/[ha1222h-2a_2-5]/1/",
				"WURCS=2.0/1,1,0/[ha2111h-2b_2-6]/1/" }) {
			Glycan read = new WURCS2Parser().readGlycan(wurcs, workspace.getDefaultMassOptions());
			assertEquals(wurcs, "Sed", read.getRoot().getChildAt(0).getTypeName());
			assertEquals(wurcs, 210.0740, read.computeMass(), 0.0001);
			assertEquals(wurcs, wurcs, new WURCS2Parser().toWURCS(read));
		}
	}

	/** The hex-2-uloses, which always read right, still do. */
	@Test
	public void theHexulosesAreUnchanged() throws Exception {
		String[][] hexuloses = { { "ha122h", "Fru" }, { "ha222h", "Psi" }, { "ha212h", "Sor" }, { "ha112h", "Tag" } };
		for (String[] one : hexuloses) {
			String wurcs = "WURCS=2.0/1,1,0/[" + one[0] + "-2b_2-6]/1/";
			Glycan read = new WURCS2Parser().readGlycan(wurcs, workspace.getDefaultMassOptions());
			assertEquals(wurcs, one[1], read.getRoot().getChildAt(0).getTypeName());
			assertEquals(wurcs, wurcs, new WURCS2Parser().toWURCS(read));
		}
	}

	private static String[] heptuloses() {
		String[] all = new String[16];
		for (int i = 0; i < 16; i++) {
			StringBuilder digits = new StringBuilder();
			for (int bit = 3; bit >= 0; bit--) digits.append(((i >> bit) & 1) == 0 ? '1' : '2');
			all[i] = "ha" + digits + "h";
		}
		return all;
	}
}
