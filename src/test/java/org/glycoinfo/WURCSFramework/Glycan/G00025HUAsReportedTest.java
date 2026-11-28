package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The structures reported in #239, read and written back.
 *
 * <p>A reducing-end residue whose ring closes on a carbon not known, {@code _1-?}, was read with a
 * 1,? anhydro under it, and written back with a second {@code _1-?}. Reading that again added
 * nothing new, but the structure was no longer the one asked for. The cause was fixed for #259:
 * these keep it fixed for the structures #239 reported.
 */
public class G00025HUAsReportedTest {

	private static final String G00025HU = "WURCS=2.0/7,11,10/[a2122h-1x_1-?_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O]"
			+ "[a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O][a1221m-1a_1-5]"
			+ "/1-2-3-4-2-5-4-2-5-6-7/a4-b1_a6-k1_b4-c1_c3-d1_c6-g1_d2-e1_e4-f1_g2-h1_h4-i1_i?-j2";

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** G00025HU, and the GalNAc and Glc of the issue's comment: no anhydro, and the same WURCS back. */
	@Test
	public void anUnknownRingIsNotAnAnhydro() throws Exception {
		for (String wurcs : new String[] { G00025HU,
				"WURCS=2.0/1,1,0/[a2112h-1x_1-?_2*NCC/3=O]/1/",
				"WURCS=2.0/1,1,0/[a2122h-1x_1-?]/1/" }) {
			Glycan read = read(wurcs);
			for (Residue r : read.getAllResidues())
				assertFalse(wurcs + " has an anhydro", "Anhydro".equals(r.getTypeName()));
			assertEquals(wurcs, new WURCSFactory(wurcs).getWURCS(), new WURCS2Parser().writeGlycan(read));
		}
	}

	/** A ring not known and, besides it, a 1,? anhydro, as the wrong export wrote it: one anhydro, kept. */
	@Test
	public void anAnhydroBesideAnUnknownRingIsOneAnhydro() throws Exception {
		String wurcs = "WURCS=2.0/1,1,0/[a2122h-1x_1-?_1-?]/1/";
		int anhydros = 0;
		for (Residue r : read(wurcs).getAllResidues())
			if ("Anhydro".equals(r.getTypeName())) anhydros++;
		assertEquals(1, anhydros);
		assertEquals(new WURCSFactory(wurcs).getWURCS(), new WURCS2Parser().writeGlycan(read(wurcs)));
	}

	private static Glycan read(String wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(wurcs, new MassOptions());
	}
}
