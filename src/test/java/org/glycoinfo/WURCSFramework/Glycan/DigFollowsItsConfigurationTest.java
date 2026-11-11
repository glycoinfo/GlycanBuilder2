package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Dig follows the configuration it is drawn in (#313), and L-cymarose reads as D-cymarose does (#312).
 *
 * <p>An L-Dig was written to WURCS as D-digitoxose, silently the other enantiomer, and a WURCS of L-Dig
 * was not recognised as Dig: L-digitoxose read as an L-ribHex with two deoxies, and L-cymarose as Cym,
 * which could not be written. D-cymarose has always read as D-Dig with a 3-O-methyl; the maintainer's
 * choice (2026-11-10) was that L-cymarose read the same way, which SNFG draws as Dig's symbol and 3Me.
 */
public class DigFollowsItsConfigurationTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** Drawn as L, written as L; drawn as D, as before. */
	@Test
	public void anLDigIsWrittenAsL() throws Exception {
		assertEquals("WURCS=2.0/1,1,0/[ad222m-1b_1-5]/1/", write("freeEnd--1b1D-Dig,p$MONO,Und,0,0,freeEnd"));
		assertEquals("WURCS=2.0/1,1,0/[ad111m-1a_1-5]/1/", write("freeEnd--1a1L-Dig,p$MONO,Und,0,0,freeEnd"));
	}

	/** L-digitoxose and both cymaroses read as Dig in their configuration, and write back as read. */
	@Test
	public void bothEnantiomersReadAsDig() throws Exception {
		String[][] cases = {
				{ "WURCS=2.0/1,1,0/[ad111m-1a_1-5]/1/", "L", "" },
				{ "WURCS=2.0/1,1,0/[ad222m-1b_1-5_3*OC]/1/", "D", "Me" },
				{ "WURCS=2.0/1,1,0/[ad111m-1a_1-5_3*OC]/1/", "L", "Me" } };
		for (String[] one : cases) {
			Glycan read = new WURCS2Parser().readGlycan(one[0], workspace.getDefaultMassOptions());
			Residue dig = read.getRoot().getChildAt(0);
			assertEquals(one[0], "Dig", dig.getTypeName());
			assertEquals(one[0], one[1].charAt(0), dig.getChirality());
			assertEquals(one[0], one[2], dig.getNoChildren() == 0 ? "" : dig.getChildAt(0).getTypeName());
			assertEquals(one[0], one[0], new WURCS2Parser().toWURCS(read));
		}
	}

	/** L-cymarose weighs what it did, and as D-cymarose does. */
	@Test
	public void theCymarosesWeighTheSame() throws Exception {
		double d = new WURCS2Parser().readGlycan("WURCS=2.0/1,1,0/[ad222m-1b_1-5_3*OC]/1/", workspace.getDefaultMassOptions()).computeMass();
		double l = new WURCS2Parser().readGlycan("WURCS=2.0/1,1,0/[ad111m-1a_1-5_3*OC]/1/", workspace.getDefaultMassOptions()).computeMass();
		assertEquals(162.0892, d, 0.0001);
		assertEquals(d, l, 0.0001);
	}

	private static String write(String _gws) throws Exception {
		return new WURCS2Parser().toWURCS(Glycan.fromString(_gws));
	}
}
