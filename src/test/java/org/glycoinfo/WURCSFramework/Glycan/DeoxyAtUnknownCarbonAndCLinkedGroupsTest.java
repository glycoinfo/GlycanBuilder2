package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.AbstractResidueRenderer;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.array.mass.WURCSMassCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Three MAPs that were refused as unsupported (#265's targets in GlyGen's dump): a deoxy at a carbon
 * not known, {@code ?*}; a C-methyl, {@code *C}; and a C-hydroxymethyl, {@code *CO}.
 *
 * <p>The deoxy is the residue's, as #258's are. The C-linked groups replace the hydroxyl, as N-linked
 * groups do, and are drawn "CMe" and "C(CH2OH)" - "C" for C-linked, so that neither reads as the
 * O-linked "Me". On a carbon whose descriptor has no hydrogen the group takes the hydrogen's place and
 * the hydroxyl stays, a branch this builder cannot hold; that is refused, saying so.
 */
public class DeoxyAtUnknownCarbonAndCLinkedGroupsTest {

	private static final double CH2 = 14.01565;
	private static final double C2H2O = 42.01056;

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** Each reads, weighs what WURCSFramework says, and goes back to WURCS and through GWS unchanged. */
	@Test
	public void eachReadsWeighsAndRoundTrips() throws Exception {
		String[][] cases = {
				{ "WURCS=2.0/1,1,0/[axxxxh-1x_1-5_?*]/1/", "Hex[?*d]" },
				{ "WURCS=2.0/1,1,0/[a2122h-1b_1-5_3*C]/1/", "Glc CMe" },
				{ "WURCS=2.0/1,1,0/[a2122h-1b_1-5_3*CO]/1/", "Glc CCH2OH" },
				{ "WURCS=2.0/2,2,1/[a2122h-1b_1-5][a1122h-1a_1-5_2*C]/1-2/a4-b1", "Glc Man CMe" },
		};
		for (String[] one : cases) {
			Glycan read = read(one[0]);
			assertEquals(one[0], one[1], names(read));
			assertEquals(one[0], wurcsMass(one[0]), underivatized(read).computeMass(), 1e-3);
			String written = new WURCS2Parser().writeGlycan(read);
			assertEquals(one[0], new WURCSFactory(one[0]).getWURCS(), written);
			assertEquals(one[0], written, new WURCS2Parser().writeGlycan(Glycan.fromString(read.toString())));
		}
	}

	/** Permethylated, a C-methyl has no hydroxyl and a C-hydroxymethyl one: four and five on a glucose. */
	@Test
	public void derivatizedTheyCountTheirHydroxyls() throws Exception {
		String cMe = "WURCS=2.0/1,1,0/[a2122h-1b_1-5_3*C]/1/";
		String cHm = "WURCS=2.0/1,1,0/[a2122h-1b_1-5_3*CO]/1/";
		assertEquals(wurcsMass(cMe) + 4 * CH2, derivatized(read(cMe), MassOptions.PERMETHYLATED).computeMass(), 1e-3);
		assertEquals(wurcsMass(cHm) + 5 * CH2, derivatized(read(cHm), MassOptions.PERMETHYLATED).computeMass(), 1e-3);
		assertEquals(wurcsMass(cMe) + 4 * C2H2O, derivatized(read(cMe), MassOptions.PERACETYLATED).computeMass(), 1e-3);
		assertEquals(wurcsMass(cHm) + 5 * C2H2O, derivatized(read(cHm), MassOptions.PERACETYLATED).computeMass(), 1e-3);
	}

	/** Drawn "CMe" and "C(CH2OH)", in every notation. */
	@Test
	public void theyAreDrawnAsDecided() throws Exception {
		AbstractResidueRenderer renderer = (AbstractResidueRenderer) new GlycanRendererAWT().getResidueRenderer();
		assertEquals("CMe", renderer.getText(child(read("WURCS=2.0/1,1,0/[a2122h-1b_1-5_3*C]/1/"))));
		assertEquals("C(CH2OH)", renderer.getText(child(read("WURCS=2.0/1,1,0/[a2122h-1b_1-5_3*CO]/1/"))));
	}

	/**
	 * On a carbon that keeps its hydroxyl, as the registered ones are, the group is refused by name.
	 * {@code AXh} is refused before its branch is looked at: its {@code X} is a stereocentre of unknown
	 * configuration in a chain of three carbons, which has no type to be read as (#298).
	 */
	@Test
	public void aBranchThatKeepsTheHydroxylIsRefused() throws Exception {
		for (String[] one : new String[][] {
				{ "WURCS=2.0/1,1,0/[a48h-1x_1-4_3*CO]/1/", "a48h has a C-linked C-Hydroxymethyl on carbon 3, which keeps its hydroxyl (8 in WURCS)" },
				{ "WURCS=2.0/1,1,0/[AXh_2*C]/1/", "AXh has stereocentres of unknown configuration (x in WURCS)" } }) {
			try {
				read(one[0]);
				fail("it was read: " + one[0]);
			} catch (WURCSToGlycanException refused) {
				assertTrue(refused.getMessage(), refused.getMessage().contains(one[1]));
			} catch (Exception refused) {
				assertTrue(refused.getMessage(), refused.getMessage().contains(one[1]));
			}
		}
	}

	// ------------------------------------------------------------------------------------------------

	private static Glycan read(String wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(wurcs, new MassOptions());
	}

	private static String names(Glycan glycan) {
		StringBuilder names = new StringBuilder();
		for (Residue r : glycan.getAllResidues())
			if (!r.isReducingEnd())
				names.append(names.length() == 0 ? "" : " ").append(r.getTypeName())
						.append(r.getDeoxies().isEmpty() ? "" : r.getDeoxies().toString().replace(" ", ""));
		return names.toString();
	}

	private static Residue child(Glycan glycan) {
		for (Residue r : glycan.getAllResidues())
			if (!r.isReducingEnd() && !r.isSaccharide()) return r;
		throw new AssertionError("no substituent in " + glycan);
	}

	private static Glycan underivatized(Glycan glycan) {
		MassOptions none = new MassOptions();
		none.setDerivatization(MassOptions.NO_DERIVATIZATION);
		glycan.setMassOptions(none);
		return glycan;
	}

	private static Glycan derivatized(Glycan glycan, String derivatization) {
		MassOptions options = new MassOptions();
		options.setDerivatization(derivatization);
		options.ION_CLOUD.set("Na", 0);
		glycan.setMassOptions(options);
		return glycan;
	}

	private static double wurcsMass(String wurcs) throws Exception {
		return WURCSMassCalculator.calcMassWURCS(new WURCSFactory(wurcs).getArray()).doubleValue();
	}
}
