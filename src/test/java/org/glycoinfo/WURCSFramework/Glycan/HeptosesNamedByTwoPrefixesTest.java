package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The heptoses and octuloses named by two configurational prefixes are written as their names say
 * (#275). Eleven of them had no stereo recorded and could not be written at all.
 *
 * <p>By 2-Carb-8 the prefix of four - gluco, manno - covers C2 to C5 of an aldoheptose, C3 to C6 of an
 * oct-2-ulose, and "glycero" the next carbon, whose D or L is the name's. Each skeleton below was
 * derived so, by hand, and is the one WURCSFramework's trivial-name converter gives that name. Each
 * heptose is registered in GlyTouCan; an accession is given for each. The octuloses are not.
 */
public class HeptosesNamedByTwoPrefixesTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	private static final String[][] NAMED = {
			// name, skeleton, its D or L, a GlyTouCan accession with it
			{ "D-gro-D-glcHep", "a21222h", "D", "G44930BS" },
			{ "L-gro-D-glcHep", "a21221h", "L", "G91414TV" },
			{ "D-gro-L-glcHep", "a12112h", "D", "G43517DR" },
			{ "L-gro-D-idoHep", "a12121h", "L", "G36968FL" },
			{ "D-gro-D-altHep", "a12222h", "D", "G21108CB" },
			{ "D-gro-D-galHep", "a21122h", "D", "G00633CX" },
			{ "L-gro-D-galHep", "a21121h", "L", "G46400YK" },
			{ "D-gro-D-manHep", "a11222h", "D", "G00006SB" },
			{ "L-gro-D-manHep", "a11221h", "L", "G00006SB" },
			{ "L-gro-L-manHep", "a22111h", "L", "G25857TR" },
			{ "D-gro-L-manHep", "a22112h", "D", "G55393DZ" },
	};

	/** Each, placed as the chooser places it, in its own D or L, is written as its name says. */
	@Test
	public void eachIsWrittenAsItsNameSays() throws Exception {
		for (String[] one : NAMED) {
			Residue placed = ResidueDictionary.newResidue(one[0]);
			assertEquals(one[0], one[2].charAt(0), placed.getChirality());
			assertEquals(one[0], "WURCS=2.0/1,1,0/[" + one[1] + "-1x_1-5]/1/", written(placed));
		}
	}

	/**
	 * L-gro-D-manHep, the heptose of lipopolysaccharide cores, a11221h - 7,894 GlyTouCan entries.
	 * Recorded the other way round since #313, it was written as a22112h, its enantiomer D-gro-L-manHep,
	 * when placed from the chooser.
	 */
	@Test
	public void lGroDManHepIsA11221h() throws Exception {
		assertEquals("WURCS=2.0/1,1,0/[a11221h-1x_1-5]/1/", written(ResidueDictionary.newResidue("L-gro-D-manHep")));
		Glycan read = new WURCS2Parser().readGlycan("WURCS=2.0/1,1,0/[a11221h-1a_1-5]/1/", new MassOptions());
		Residue back = read.getRoot().firstChild();
		assertEquals("L-gro-D-manHep", back.getTypeName());
		assertEquals('L', back.getChirality());
	}

	/**
	 * Each reads back as the skeleton it was written as. Most by their own name; L-gro-L-manHep and
	 * D-gro-L-manHep as the enantiomer of a manno-heptose of the main dictionary, which is the same
	 * molecule.
	 */
	@Test
	public void eachReadsBackAsItsSkeleton() throws Exception {
		for (String[] one : NAMED) {
			String wurcs = "WURCS=2.0/1,1,0/[" + one[1] + "-1a_1-5]/1/";
			Glycan read = new WURCS2Parser().readGlycan(wurcs, new MassOptions());
			assertEquals(one[0], wurcs, new WURCS2Parser().toWURCS(read));
			read.getMassOptions().setDerivatization(MassOptions.NO_DERIVATIZATION);
			assertEquals(one[0], 210.0740, read.computeMass(), 1e-3);  // C7H14O7
		}
	}

	/** The two oct-2-uloses, as #349 corrected their rows: carbon 2 is the carbonyl. */
	@Test
	public void theOctulosesAreWritten() throws Exception {
		assertEquals("WURCS=2.0/1,1,0/[ha21122h-2x_2-6]/1/", written(ResidueDictionary.newResidue("D-gro-D-galOct")));
		assertEquals("WURCS=2.0/1,1,0/[ha11122h-2x_2-6]/1/", written(ResidueDictionary.newResidue("D-gro-D-talOct")));
	}

	/**
	 * An octonic acid with an octulose's stereo is not that octulose. The converter names by stereo, and
	 * G27265UQ, Ad21122h, came back as D-gro-D-galOct: once the octuloses were written as ketoses, its
	 * carboxyl was a child nothing could write and it weighed 34 Da light. Refused, saying why. A deoxy
	 * the name does not imply is no contradiction: a 1-deoxy ketohexose still reads.
	 */
	@Test
	public void anOctonicAcidIsNotReadAsAnOctulose() throws Exception {
		for (String wurcs : new String[] { "WURCS=2.0/1,1,0/[Ad21122h_3-7]/1/", "WURCS=2.0/1,1,0/[Ad11122h_3-7]/1/" }) {
			try {
				new WURCS2Parser().readGlycan(wurcs, new MassOptions());
				org.junit.Assert.fail(wurcs + " was read");
			} catch (org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException refused) {
				org.junit.Assert.assertTrue(refused.getMessage(), refused.getMessage().contains("C1 A"));
			}
		}
		// read, as before this check; what it is written back as is another matter, and wrong on develop
		// as well (read as araHex with a 1-deoxy, it is written as hx112m)
		Glycan deoxy = new WURCS2Parser().readGlycan("WURCS=2.0/1,1,0/[ma122h-2x_2-5]/1/", new MassOptions());
		assertEquals(1, deoxy.getRoot().getNoChildren());
	}

	/** The two of unknown C6 have no D or L of their own, and are written as named. */
	@Test
	public void theTwoOfUnknownC6() throws Exception {
		assertEquals("WURCS=2.0/1,1,0/[a2122xh-1x_1-5]/1/", written(ResidueDictionary.newResidue("gro-D-glcHep")));
		assertEquals("WURCS=2.0/1,1,0/[a1122xh-1x_1-5]/1/", written(ResidueDictionary.newResidue("gro-D-manHep")));
		assertEquals('?', ResidueDictionary.newResidue("gro-D-manHep").getChirality());
	}

	/** And none offers C1, its anomeric carbon. */
	@Test
	public void noneOffersItsAnomericCarbon() throws Exception {
		for (String[] one : NAMED) {
			ResidueType type = ResidueDictionary.findResidueType(one[0]);
			String positions = new String(type.getLinkagePositions());
			assertEquals(one[0], "234567", positions);
			assertEquals(one[0], 6, type.getMaxLinkages());
		}
		for (String name : new String[] { "gro-D-glcHep", "gro-D-manHep" })
			assertFalse(name, new String(ResidueDictionary.findResidueType(name).getLinkagePositions()).contains("1"));
	}

	private static String written(Residue _residue) throws Exception {
		return new WURCS2Parser().toWURCS(new Glycan(_residue, true, new MassOptions()));
	}
}
