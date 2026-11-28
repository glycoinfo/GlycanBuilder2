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
 * A residue whose ring is larger than a pyranose is refused, saying so (#344).
 *
 * <p>A septanose, a2221h-1a_1-6, closes its ring from C1 to C6. GlycanBuilder2 has furanoses,
 * pyranoses and rings of size not known, and no seven-membered ring. The 1-6 was taken for a
 * 1,6-anhydro beside a ring not known: the residue weighed a water light and was written back as
 * a2221h-1a_1-6_1-?. The maintainer's decision (2026-11-28) is to refuse it rather than read it as
 * something else. All four septanoses of GlyTouCan are here.
 */
public class SeptanoseIsRefusedTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	@Test
	public void aSeptanoseIsRefusedSayingWhy() throws Exception {
		String[] septanoses = {
				"WURCS=2.0/1,1,0/[a2221h-1a_1-6]/1/",   // G16188IP
				"WURCS=2.0/1,1,0/[a2211h-1b_1-6]/1/",   // G17625OB
				"WURCS=2.0/1,1,0/[a1122h-1b_1-6]/1/",   // G36756DP
				"WURCS=2.0/1,1,0/[a1112h-1b_1-6]/1/",   // G39701HX
		};
		for (String sequence : septanoses) {
			try {
				new WURCS2Parser().readGlycan(sequence, workspace.getDefaultMassOptions());
				fail(sequence + " was read");
			} catch (Exception refused) {
				assertTrue(sequence + ": " + refused.getMessage(),
						refused.getMessage().contains("closes its ring from C1 to C6, a ring of 7 members"));
			}
		}
	}

	/** A 1,6-anhydro beside its pyranose is a bridge, as it was, and is written back as read. */
	@Test
	public void aOneSixAnhydroPyranoseIsAsItWas() throws Exception {
		String anhydro = "WURCS=2.0/1,1,0/[a2122h-1b_1-5_1-6]/1/";
		Glycan read = new WURCS2Parser().readGlycan(anhydro, workspace.getDefaultMassOptions());
		assertEquals(anhydro, new WURCS2Parser().toWURCS(read));
		assertEquals("C6H10O5", 162.0528, read.computeMass(), 0.0001);
	}

	/** A ring of size not known, 1-?, is read as it was. */
	@Test
	public void aRingNotKnownIsAsItWas() throws Exception {
		String unknown = "WURCS=2.0/1,1,0/[a2122h-1x_1-?]/1/";
		Glycan read = new WURCS2Parser().readGlycan(unknown, workspace.getDefaultMassOptions());
		assertEquals(unknown, new WURCS2Parser().toWURCS(read));
	}
}
