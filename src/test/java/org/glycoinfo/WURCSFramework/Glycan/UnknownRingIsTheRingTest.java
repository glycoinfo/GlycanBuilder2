package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.array.mass.WURCSMassCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A ring closing on a carbon not known, {@code _1-?}, is the ring, of a size not known (#259).
 *
 * <p>It was read as a ring of unknown size and, besides it, a 1,? anhydro: the closure's end was not
 * a carbon, so it did not look like a ring, and was built as a bridge. The structure was drawn with
 * an anhydro nobody asked for, weighed a water light, and was written back with a second {@code _1-?}.
 */
public class UnknownRingIsTheRingTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** #259's three, and the Kdo of #265's reported structures: no anhydro, the right mass, the same WURCS. */
	@Test
	public void theUnknownRingIsReadAsTheRing() throws Exception {
		for (String wurcs : new String[] {
				"WURCS=2.0/1,1,0/[ad122m-1x_1-?_3*OC]/1/",
				"WURCS=2.0/1,1,0/[ad122h-1a_1-?_3*OC]/1/",
				"WURCS=2.0/1,1,0/[a2122h-1x_1-?_2*NCC/3=O]/1/",
				"WURCS=2.0/1,1,0/[axxxxh-1x_1-?]/1/",
				"WURCS=2.0/1,1,0/[Aad1122h-2x_2-?]/1/" }) {
			Glycan read = read(wurcs);
			for (Residue r : read.getAllResidues())
				assertFalse(wurcs + " has an anhydro", "Anhydro".equals(r.getTypeName()));
			assertEquals(wurcs, wurcsMass(wurcs), underivatized(read).computeMass(), 1e-3);
			assertEquals(wurcs, new WURCSFactory(wurcs).getWURCS(), new WURCS2Parser().writeGlycan(read));
		}
	}

	/** A real anhydro beside a known ring is still an anhydro. */
	@Test
	public void aRealAnhydroIsKept() throws Exception {
		String wurcs = "WURCS=2.0/1,1,0/[a2122h-1x_1-5_1-6]/1/";
		boolean anhydro = false;
		for (Residue r : read(wurcs).getAllResidues())
			if ("Anhydro".equals(r.getTypeName())) anhydro = true;
		assertTrue("the 1,6-anhydro was lost", anhydro);
		assertEquals(new WURCSFactory(wurcs).getWURCS(), new WURCS2Parser().writeGlycan(read(wurcs)));
	}

	private static Glycan read(String wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(wurcs, new MassOptions());
	}

	private static Glycan underivatized(Glycan glycan) {
		MassOptions none = new MassOptions();
		none.setDerivatization(MassOptions.NO_DERIVATIZATION);
		glycan.setMassOptions(none);
		return glycan;
	}

	private static double wurcsMass(String wurcs) throws Exception {
		return WURCSMassCalculator.calcMassWURCS(new WURCSFactory(wurcs).getArray()).doubleValue();
	}
}
