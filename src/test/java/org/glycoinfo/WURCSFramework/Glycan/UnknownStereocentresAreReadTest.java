package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.array.mass.WURCSMassCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.SkeletonInGWS;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A monosaccharide with stereocentres of unknown configuration, {@code x}, is read (#265).
 *
 * <p>The naming converter refused them, and the structures did not open: "This SkeletonCode partially
 * contains the wild card" for {@code a1122xh}, "could not handled" for {@code adxxxxh}. They are named
 * by what the known centres allow: none known, the type of unknown stereo for the chain; anything
 * else, the type of unknown stereo with the skeleton kept on the residue and written back as read -
 * except known from C2 on and unknown beyond, which a prefix name used to misname and is refused
 * since #327.
 */
public class UnknownStereocentresAreReadTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** Each is read as the residue named, weighs what WURCSFramework says, and is written back unchanged. */
	@Test
	public void eachIsReadWeighedAndWrittenBack() throws Exception {
		String[][] cases = {
				// none known: the type of unknown stereo for the chain, with its deoxies
				{ "WURCS=2.0/1,1,0/[adxxxxh-1x_1-5]/1/", "?-Hept[2*d]" },
				{ "WURCS=2.0/1,1,0/[axxxxxh-1x_1-5]/1/", "?-Hept[]" },
				{ "WURCS=2.0/1,1,0/[axxxxh-1x_1-5]/1/", "?-Hex[]" },
				{ "WURCS=2.0/1,1,0/[axxxxA-1b_1-5]/1/", "?-HexA[]" },
				{ "WURCS=2.0/1,1,0/[axxxh-1x_1-4]/1/", "?-Pen[]" },
				// an unknown centre between known ones: the type of unknown stereo, the skeleton kept
				{ "WURCS=2.0/1,1,0/[a1x22h-1b_1-5]/1/", "?-Hex[]{a1x22h}" },
		};
		for (String[] one : cases) {
			Glycan read = read(one[0]);
			Residue sugar = onlySugar(read);
			String named = sugar.getChirality() + "-" + sugar.getTypeName() + sugar.getDeoxies()
					+ (sugar.getReadSkeleton() == null ? "" : "{" + sugar.getReadSkeleton() + "}");
			assertEquals(one[0], one[1], named);
			assertEquals(one[0], wurcsMass(one[0]), underivatized(read).computeMass(), 1e-3);
			assertEquals(one[0], new WURCSFactory(one[0]).getWURCS(), new WURCS2Parser().writeGlycan(read));
		}
	}

	/**
	 * Known from C2 and unknown beyond is refused, with the reason (#327). It was read under a prefix
	 * name - a1122xh as D-manHep, a2211xh as L-manHep, a2xxxh as D-groHex - and the name was wrong: a
	 * prefix names a whole block of four centres from C2, so a2xxxh is no glycero-hexose and has no D
	 * or L, and a heptose's C6 block, unknown, is still part of its name (?-gro-D-manHep). The
	 * maintainer's choice (2026-11-11): refused in GB2, and named in GB3.
	 */
	@Test
	public void aBlockKnownOnlyInPartIsRefused() throws Exception {
		for (String skeleton : new String[] { "a1122xh-1a_1-5", "a2211xh-1a_1-5", "a2xxxh-1b_1-5", "a21xxh-1a_1-5",
				"a122xh-1a_1-5", "a2xxh-1a_1-5" }) {
			try {
				read("WURCS=2.0/1,1,0/[" + skeleton + "]/1/");
				org.junit.Assert.fail(skeleton + " was read");
			} catch (Exception refused) {
				assertTrue(refused.getMessage(), String.valueOf(refused.getMessage()).contains("known only in part"));
			}
		}
	}

	/**
	 * The reported structures open, every residue a monosaccharide - bridges aside - and are written
	 * back unchanged, weighing what WURCSFramework says. Their Kdo has an unknown ring,
	 * {@code Aad1122h-2x_2-?}, which needed #259's fix as well.
	 */
	@Test
	public void theReportedStructuresOpen() throws Exception {
		String[][] reported = {
				// G55200VD, reported with these, has an a1122xh, which is refused since #327
				{ "G15560EB", "WURCS=2.0/4,6,5/[Aad1122h-2x_2-?][a11221h-1a_1-5][a2122h-1b_1-5][adxxxxh-1x_1-5]/1-2-2-2-3-4/b3-c1_c3-d1_c7-f1_d3-e1_b1-a4|a5", "6" },
				{ "G07957FT (#58)", "WURCS=2.0/7,9,8/[a2122h-1x_1-5_2*N][Aad1122h-2x_2-6][axxxxxh-1x_1-5][a2122h-1x_1-5][a2112h-1x_1-5][axxxxxh-1x_1-5_6*OP^XOCCN/3O/3=O][a2122h-1x_1-5_2*NCC/3=O]/1-2-3-4-5-5-6-7-2/a?-b2_b?-c1_b?-i2_c?-d1_c?-g1_d?-e1_e?-f1_g?-h1", "9" },
		};
		for (String[] one : reported) {
			Glycan read = read(one[1]);
			List<String> notSugars = new ArrayList<String>();
			int sugars = 0;
			for (Residue r : read.getAllResidues()) {
				if (r.isSaccharide() && !r.isBridge()) sugars++;
				else if (!r.isReducingEnd() && !r.isBridge() && !"Substituent".equals(r.getType().getSuperclass())) notSugars.add(r.getTypeName());
			}
			assertEquals(one[0] + " " + notSugars, Integer.parseInt(one[2]), sugars);
			assertEquals(one[0], new WURCSFactory(one[1]).getWURCS(), new WURCS2Parser().writeGlycan(read));
			assertEquals(one[0], wurcsMass(one[1]), underivatized(read).computeMass(), 1e-3);
		}
	}

	/**
	 * And what was written before still is. An open-chain residue of relative or unknown stereo has no
	 * configuration and no ring of its own; its type has a ring, and that is enough for the writer.
	 * Asking the residue alone wrote these empty - 117 registered structures in GlyGen's dump, three
	 * of them here.
	 */
	@Test
	public void openChainsAreStillWritten() throws Exception {
		for (String registered : new String[] {
				"WURCS=2.0/2,4,3/[u4444h][a2122h-1x_1-5]/1-2-2-2/a?-b1_b?-c1_b?-d1",                       // G00479NZ
				"WURCS=2.0/3,3,2/[uxxxxh][a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O]/1-2-3/b3-c2_a?-b1",  // G02123VE
				"WURCS=2.0/2,2,1/[u4334h][a4334h-1b_1-4]/1-2/a6-b1" })                                  // G02350ZD
			assertEquals(registered, new WURCSFactory(registered).getWURCS(), new WURCS2Parser().writeGlycan(read(registered)));
	}

	/** What a save to GWS cannot hold is said, with what it will become. */
	@Test
	public void aSaveSaysWhatItCannotHold() throws Exception {
		assertEquals(Arrays.asList("Hex: a1x22h -> axxxxh"),
				SkeletonInGWS.lostOnSave(Collections.singletonList(read("WURCS=2.0/1,1,0/[a1x22h-1b_1-5]/1/"))));
		// a skeleton the type says in full loses nothing
		assertTrue(SkeletonInGWS.lostOnSave(Collections.singletonList(read("WURCS=2.0/1,1,0/[axxxxh-1a_1-5]/1/"))).isEmpty());
	}

	// ------------------------------------------------------------------------------------------------

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

	private static Residue onlySugar(Glycan glycan) {
		Residue found = null;
		for (Residue r : glycan.getAllResidues())
			if (r.isSaccharide()) found = r;
		if (found == null) throw new AssertionError("no monosaccharide in " + glycan);
		return found;
	}
}
