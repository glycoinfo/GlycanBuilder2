package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A carbon-carbon double bond of unknown geometry, WURCS's F and f, is refused saying what and where
 * (#266).
 *
 * <p>GlycanBuilder2 has no way to hold a double bond on a residue, so these cannot be read yet. They
 * were refused with "hxFFxh could not handled", which named nothing a user could act on.
 */
public class UnknownDoubleBondSaysSoTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** G78126EI and G37831OG, as reported. */
	@Test
	public void theReportedSequencesSaySo() throws Exception {
		assertRefusedNaming("WURCS=2.0/4,4,3/[hxFFxh][a2122h-1b_1-5_2*NCC/3=O][a2112h-1b_1-5][a1221m-1a_1-5]/1-2-3-4/a6-b1_b4-c1_c2-d1",
				"hxFFxh has a carbon-carbon double bond of unknown geometry at C3 and C4");
		assertRefusedNaming("WURCS=2.0/4,4,3/[hxFfxh][a2122h-1b_1-5_2*NCC/3=O][a1221m-1a_1-5][a2112h-1b_1-5]/1-2-3-4/a1-b1_b3-c1_b4-d1",
				"hxFfxh has a carbon-carbon double bond of unknown geometry at C3 and C4");
	}

	private static void assertRefusedNaming(String wurcs, String expected) throws Exception {
		try {
			new WURCS2Parser().readGlycan(wurcs, new MassOptions());
			fail("it was read: " + wurcs);
		} catch (WURCSToGlycanException refused) {
			assertTrue(refused.getMessage(), refused.getMessage().contains(expected));
			assertTrue(refused.getMessage(), refused.getMessage().contains("cannot represent"));
		}
	}
}
