package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Monosaccharides with no linkage between them are not read, and the reason says why (the
 * maintainer's decision, 2026-11-29).
 *
 * <p>A WURCS of several monosaccharides and no linkage, {@code WURCS=2.0/2,6,0/...}, is that many
 * separate molecules, not one glycan. It was read as a composition with a "no glycosidic linkages"
 * label on it, which could then not be written back: the export took the label for a residue and
 * refused it. A composition is written with its residues joined by linkages of unknown position,
 * {@code a?|b?}-{a?|b?}, which is how GlyTouCan holds every composition - none of the 19,602 GlyTouCan
 * structures in the corpus has several monosaccharides and no linkage.
 */
public class UnlinkedMonosaccharidesAreRefusedTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	/** Each of the three forms #117 compares, unlinked, is refused, counting what it holds. */
	@Test
	public void unlinkedMonosaccharidesAreRefused() throws Exception {
		for (String wurcs : new String[] {
				"WURCS=2.0/2,6,0/[uxxxxh_2*NCC/3=O][uxxxxh]/1-1-2-2-2-2/",
				"WURCS=2.0/2,6,0/[a2122h-1x_1-?_2*NCC/3=O][a1122h-1x_1-?]/1-1-2-2-2-2/",
				"WURCS=2.0/2,6,0/[axxxxh-1x_1-5_2*NCC/3=O][axxxxh-1x_1-5]/1-1-2-2-2-2/" }) {
			try {
				new WURCS2Parser().readGlycan(wurcs, new org.eurocarbdb.application.glycanbuilder.massutil.MassOptions());
				fail(wurcs + " was read");
			} catch (WURCSToGlycanException refused) {
				assertTrue(refused.getMessage(), refused.getMessage().contains("6 monosaccharides and no linkage"));
			}
		}
	}

	/** A composition joined as GlyTouCan joins it is read, and written back as read. */
	@Test
	public void aLinkedCompositionIsRead() throws Exception {
		String linked = "WURCS=2.0/2,6,5/[axxxxh-1x_1-5_2*NCC/3=O][axxxxh-1x_1-5]/1-1-2-2-2-2/"
				+ "a?|b?|c?|d?|e?|f?}-{a?|b?|c?|d?|e?|f?_a?|b?|c?|d?|e?|f?}-{a?|b?|c?|d?|e?|f?"
				+ "_a?|b?|c?|d?|e?|f?}-{a?|b?|c?|d?|e?|f?_a?|b?|c?|d?|e?|f?}-{a?|b?|c?|d?|e?|f?"
				+ "_a?|b?|c?|d?|e?|f?}-{a?|b?|c?|d?|e?|f?";
		Glycan read = new WURCS2Parser().readGlycan(linked, new org.eurocarbdb.application.glycanbuilder.massutil.MassOptions());
		assertTrue(read.isComposition());
		assertEquals(linked, new WURCS2Parser().toWURCS(read));
	}

	/** One monosaccharide, which has nothing to be linked to, is read as before. */
	@Test
	public void oneMonosaccharideIsRead() throws Exception {
		String one = "WURCS=2.0/1,1,0/[a2122h-1b_1-5_2*NCC/3=O]/1/";
		assertEquals(one, new WURCS2Parser().toWURCS(
				new WURCS2Parser().readGlycan(one, new org.eurocarbdb.application.glycanbuilder.massutil.MassOptions())));
	}
}
