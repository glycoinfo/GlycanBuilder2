package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.dataset.NativeMonosaccharideDictionary;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A residue named by a configurational prefix - araHex, lyxHex, eryPen - can be written to WURCS
 * (#261, #260).
 *
 * <p>The WURCS reader names a monosaccharide it has no trivial name for by the prefix of its
 * stereocentres and its length: a 2-deoxy arabino-hexose is araHex. The writer needs the stereo of
 * each free carbon, and no type of that kind had one recorded, so a structure read from WURCS could not
 * be written back: "araHex has no stereo recorded for it". The prefix is the stereo. Each one names a
 * fixed run of D-form digits, and the writer now takes them from the name.
 */
public class PrefixNamedResiduesAreWrittenTest {

	/** #260: a 3-O-methyl 2-deoxy arabino-hexose. */
	private static final String DEOXY_ARAHEX = "WURCS=2.0/1,1,0/[ad122h-1x_1-5_3*OC]/1/";

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	@Test
	public void theReportedStructureWritesBackUnchanged() throws Exception {
		assertRoundTrip(DEOXY_ARAHEX);
		assertRoundTrip("WURCS=2.0/2,2,1/[a2122h-1b_1-5][ad122h-1b_1-5_3*OC]/1-2/a4-b1");
	}

	/** Each prefix, in both configurations, read and written back as WURCSFramework normalizes it. */
	@Test
	public void everyPrefixWritesBackUnchanged() throws Exception {
		String[] skeletons = {
				// hexoses with a 2-deoxy: ara, lyx, xyl, rib
				"ad122h", "ad211h", "ad112h", "ad221h", "ad212h", "ad121h", "ad222h", "ad111h",
				// pentoses with a 2-deoxy: ery, thr
				"ad22h", "ad11h", "ad12h", "ad21h",
		};
		for (String skeleton : skeletons)
			for (String anomer : new String[] { "a", "b", "x" })
				assertRoundTrip("WURCS=2.0/1,1,0/[" + skeleton + "-1" + anomer + "_1-5]/1/");
	}

	/** The rule, on the dictionary: the prefix gives the D-form digits, and a named type is unchanged. */
	@Test
	public void thePrefixIsTheStereo() {
		assertEquals("122", NativeMonosaccharideDictionary.forResidueName("araHex").getStereo('D'));
		assertEquals("2112", NativeMonosaccharideDictionary.forResidueName("galHep").getStereo('D'));
		assertEquals("22", NativeMonosaccharideDictionary.forResidueName("eryPen").getStereo('D'));
		assertNotNull("a named type keeps its own entry", NativeMonosaccharideDictionary.forResidueName("Glc"));
		assertEquals(null, NativeMonosaccharideDictionary.forResidueName("fooHex"));
	}

	/**
	 * A skeleton with relative stereo only - 3 and 4, no 1 or 2 - has no D or L, and is not written
	 * back with absolute digits. Before this change it was not written at all; the first version of it
	 * wrote ad3dh as ad1dh, a different molecule.
	 *
	 * <p>It is not written back unchanged either: it comes back as the other relative form, ad4dh. That
	 * is #286.
	 */
	@Test
	public void relativeStereoIsNotMadeAbsolute() throws Exception {
		String written = new WURCS2Parser().writeGlycan(read("WURCS=2.0/1,1,0/[ad3dh-1b_1-5_3*OC]/1/"));
		String skeleton = written.substring(written.indexOf('[') + 1, written.indexOf('-'));
		assertFalse("written with absolute digits: " + written, skeleton.contains("1") || skeleton.contains("2"));
		assertTrue("not written as relative: " + written, skeleton.contains("3") || skeleton.contains("4"));
	}

	// ------------------------------------------------------------------------------------------------

	private static Glycan read(String sequence) throws Exception {
		return new WURCS2Parser().readGlycan(sequence, new MassOptions());
	}

	private static void assertRoundTrip(String sequence) throws Exception {
		String normal = new WURCSFactory(sequence).getWURCS();
		assertEquals(sequence, normal, new WURCS2Parser().writeGlycan(read(sequence)));
	}
}
