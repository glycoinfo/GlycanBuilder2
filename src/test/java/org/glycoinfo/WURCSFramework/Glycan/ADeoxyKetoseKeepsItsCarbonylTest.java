package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A ketose whose C1 is a deoxy is read as that ketose, with the deoxy, and written back as read (#354).
 *
 * <p>WURCSFramework's naming converter names 1-deoxy-D-fructose, ma122h, as an aldose read from the
 * other end: araHex with a deoxy at 1. Weighed, that came out right; written, it was hx112m, a 6-deoxy
 * aldohexose of unknown C2 - another monosaccharide, with nothing to say so.
 */
public class ADeoxyKetoseKeepsItsCarbonylTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	/** Each, its name, its deoxies, and its mass: C6H12O5, or C6H12O4 for the 1,6-dideoxy. */
	@Test
	public void eachIsReadAsItsKetoseAndWrittenBackAsRead() throws Exception {
		String[][] cases = {
				{ "WURCS=2.0/1,1,0/[ma122h-2x_2-5]/1/", "Fru", "[1*m]", "164.0685" },  // #354's own
				{ "WURCS=2.0/1,1,0/[ma122h-2b_2-5]/1/", "Fru", "[1*m]", "164.0685" },
				{ "WURCS=2.0/1,1,0/[ma122h-2a_2-6]/1/", "Fru", "[1*m]", "164.0685" },
				{ "WURCS=2.0/1,1,0/[mU122h]/1/", "Fru", "[1*m]", "164.0685" },
				{ "WURCS=2.0/1,1,0/[ma212h-2b_2-5]/1/", "Sor", "[1*m]", "164.0685" },
				{ "WURCS=2.0/1,1,0/[ma112h-2b_2-5]/1/", "Tag", "[1*m]", "164.0685" },
				{ "WURCS=2.0/1,1,0/[ma122m-2b_2-5]/1/", "Fru", "[1*m, 6*m]", "148.0736" },
				{ "WURCS=2.0/1,1,0/[ma122h-2b_2-5_3*OC]/1/", "Fru", "[1*m]", "178.0841" },
		};
		for (String[] one : cases) {
			Glycan read = new WURCS2Parser().readGlycan(one[0], new MassOptions());
			Residue ketose = read.getRoot().firstChild();
			assertEquals(one[0], one[1], ketose.getTypeName());
			assertEquals(one[0], one[2], ketose.getDeoxies().toString());
			assertEquals(one[0], one[0], new WURCS2Parser().toWURCS(read));
			read.getMassOptions().setDerivatization(MassOptions.NO_DERIVATIZATION);
			assertEquals(one[0], Double.parseDouble(one[3]), read.computeMass(), 1e-3);
		}
	}

	/**
	 * The open chain, once #342 gave a ketose its open form: mO122h is Fru with its C1 a deoxy, open, and
	 * is written back as read rather than as mU122h. It was left as an araHex until then.
	 */
	@Test
	public void theOpenChainIsItsKetoseOpen() throws Exception {
		String wurcs = "WURCS=2.0/1,1,0/[mO122h]/1/";
		Glycan read = new WURCS2Parser().readGlycan(wurcs, new MassOptions());
		assertEquals("Fru", read.getRoot().firstChild().getTypeName());
		assertEquals(wurcs, new WURCS2Parser().toWURCS(read));
	}

	/** A 6-deoxy fructose was read so already, and still is. */
	@Test
	public void aSixDeoxyFructoseIsAsItWas() throws Exception {
		String wurcs = "WURCS=2.0/1,1,0/[ha122m-2b_2-5]/1/";
		Glycan read = new WURCS2Parser().readGlycan(wurcs, new MassOptions());
		assertEquals("Fru", read.getRoot().firstChild().getTypeName());
		assertEquals("[6*m]", read.getRoot().firstChild().getDeoxies().toString());
		assertEquals(wurcs, new WURCS2Parser().toWURCS(read));
	}
}
