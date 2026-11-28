package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A nitrogen or a sulfur that takes the place of a hydroxyl is weighed without that hydroxyl's
 * oxygen, and one in place of the ring oxygen is refused, saying so (#345).
 *
 * <p>A bridge with no oxygen of its own, *N* or *S*, was bonded through the monosaccharide's hydroxyl
 * as a phosphate is, so 1,5-dideoxy-1,5-imino-galactitol was weighed with both of the oxygens the NH
 * replaces, 32 Da heavy. Each mass here is the molecular formula's, worked by hand: WURCSFramework
 * weighs a divalent sulfur four hydrogens heavy (wurcsframework#55).
 */
public class AnAtomInPlaceOfAHydroxylTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** Each weighs what its formula weighs, and is written back as read. */
	@Test
	public void eachWeighsItsFormula() throws Exception {
		String[][] cases = {
				// G33457CX, 1,5-dideoxy-1,5-imino-D-galactitol, C6H13NO4
				{ "WURCS=2.0/1,1,0/[h2112h_1-5*N*]/1/", "163.0845" },
				// G37613OF, a 1,2,5-trideoxy-1,5-imino-hexitol, C6H13NO3
				{ "WURCS=2.0/1,1,0/[hd222h_1-5*N*]/1/", "147.0895" },
				// G22895PY, a 1,5-thiolactone with a 2,3-double bond and a 3-O-methyl, C7H10O3S
				{ "WURCS=2.0/1,1,0/[AzZd1h_1-5*S*_3*OC]/1/", "174.0351" },
		};
		for (String[] one : cases) {
			Glycan read = wurcs(one[0]);
			assertEquals(one[0], Double.parseDouble(one[1]), read.computeMass(), 0.0001);
			assertEquals(one[0], one[0], new WURCS2Parser().toWURCS(read));
		}
	}

	/** A sulfur in place of the ring oxygen is refused, saying where and what. */
	@Test
	public void anotherAtomInTheRingIsRefused() throws Exception {
		String thioGlucose = "WURCS=2.0/1,1,0/[a2122h-1a_1-5*S*]/1/";  // G25472LV, 5-thio-D-glucopyranose
		try {
			wurcs(thioGlucose);
			fail("read as a thio bridge beside a ring not known");
		} catch (Exception refused) {
			assertTrue(refused.getMessage(),
					refused.getMessage().contains("closes its ring from C1 to C5 through *S* in place of the ring oxygen"));
		}
	}

	/** A bridge with an oxygen of its own is bonded through the hydroxyl, as before. */
	@Test
	public void aPhosphateBridgeIsAsItWas() throws Exception {
		// glucose 4,6-cyclic phosphate, C6H11O8P
		String cyclic = "WURCS=2.0/1,1,0/[a2122h-1b_1-5_4-6*OPO*/3O/3=O]/1/";
		Glycan read = wurcs(cyclic);
		assertEquals(242.0192, read.computeMass(), 0.0001);
		assertEquals(cyclic, new WURCS2Parser().toWURCS(read));
	}

	private static Glycan wurcs(String _wurcs) throws Exception {
		MassOptions options = workspace.getDefaultMassOptions().clone();
		options.setDerivatization(MassOptions.NO_DERIVATIZATION);
		return new WURCS2Parser().readGlycan(_wurcs, options);
	}
}
