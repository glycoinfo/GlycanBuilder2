package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.massutil.CompositionOptions;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.array.mass.WURCSMassCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.util.exchange.GlycanToWURCSException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The composition counts #220 asked about, written as glycompconverter writes them - which is what
 * GlyTouCan holds - or refused with the reason.
 *
 * <p>The maintainer's decisions (2026-11-28): sulfate, phosphate and O-acetyl counted on their own, on
 * a residue and at a position not known; the methyl hexose with its methyl on oxygen 4, as
 * glycompconverter has it and as 47 GlyTouCan entries do, not on 3 as GlycanBuilder2's MeH row says;
 * the 4-deoxy pentose as a pyranose. Pyruvate, phosphocholine and dHexA stay refused.
 */
public class CompositionWritesWhatGlyTouCanHoldsTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** A sulfated composition is written exactly as GlyTouCan holds it: G13535XD, HexNAc2Hex2S1. */
	@Test
	public void aSulfatedCompositionIsG13535XD() throws Exception {
		assertWrites("WURCS=2.0/2,4,4/[axxxxh-1x_1-5_2*NCC/3=O][axxxxh-1x_1-5]/1-1-2-2/"
				+ "a?|b?|c?|d?}*OSO/3=O/3=O_a?|b?|c?|d?}-{a?|b?|c?|d?_a?|b?|c?|d?}-{a?|b?|c?|d?_a?|b?|c?|d?}-{a?|b?|c?|d?",
				counts("HEXNAC", 2, "HEX", 2, "S", 1));
	}

	/** And with a sialic acid: G14385VC, Neu5Ac1Hex2S1. */
	@Test
	public void aSulfatedSialylatedCompositionIsG14385VC() throws Exception {
		assertWrites("WURCS=2.0/2,3,3/[Aad21122h-2x_2-6_5*NCC/3=O][axxxxh-1x_1-5]/1-2-2/"
				+ "a?|b?|c?}*OSO/3=O/3=O_a?|b?|c?}-{a?|b?|c?_a?|b?|c?}-{a?|b?|c?",
				counts("NEU5AC", 1, "HEX", 2, "S", 1));
	}

	/** Phosphate and O-acetyl go the same way, each on its oxygen. */
	@Test
	public void phosphateAndAcetylAreCountedTheSameWay() throws Exception {
		assertWrites("WURCS=2.0/1,1,0/[axxxxh-1x_1-5_?*OPO/3O/3=O]/1/", counts("HEX", 1, "P", 1));
		assertWrites("WURCS=2.0/1,1,0/[axxxxh-1x_1-5_?*OCC/3=O]/1/", counts("HEX", 1, "AC", 1));
	}

	/** The methyl hexose has its methyl on oxygen 4, and the 4-deoxy pentose is a pyranose. */
	@Test
	public void theMethylHexoseAndTheDeoxyPentose() throws Exception {
		assertWrites("WURCS=2.0/1,1,0/[axxxxh-1x_1-5_4*OC]/1/", counts("MEHEX", 1));
		assertWrites("WURCS=2.0/1,1,0/[axxdh-1x_1-5]/1/", counts("DPEN", 1));
	}

	/** A substituent with no monosaccharide to sit on is no composition, and says so. */
	@Test
	public void aSubstituentAloneIsRefused() throws Exception {
		assertRefused(counts("S", 2), "no monosaccharide to sit on");
	}

	/** The ones decided against are refused, naming the residue. */
	@Test
	public void theOnesDecidedAgainstAreRefused() throws Exception {
		assertRefused(counts("HEX", 1, "PYR", 1), "'Pyr'");
		assertRefused(counts("HEX", 1, "PC", 1), "'PC'");
		assertRefused(counts("HEX", 1, "DHEXA", 1), "'dHexA'");
	}

	/** Writes the expected WURCS, weighs what WURCSFramework says, and reads back to itself. */
	private static void assertWrites(String expected, Glycan composition) throws Exception {
		String written = new WURCS2Parser().toWURCS(composition);
		assertEquals(expected, written);
		assertEquals(expected, WURCSMassCalculator.calcMassWURCS(new WURCSFactory(written).getArray()).doubleValue(),
				composition.computeMass(), 1e-4);

		GlycanDocument document = workspace.getStructures();
		document.setStructures(new ArrayList<Glycan>());
		document.importFromString(written, "wurcs2");
		List<Glycan> read = document.getStructures();
		assertEquals(expected, 1, read.size());
		assertEquals(expected, new WURCS2Parser().toWURCS(read.get(0)));
	}

	private static void assertRefused(Glycan composition, String saying) {
		try {
			String written = new WURCS2Parser().toWURCS(composition);
			fail("written, and should not have been: " + written);
		} catch (GlycanToWURCSException refused) {
			assertTrue(refused.getMessage(), String.valueOf(refused.getMessage()).contains(saying));
		}
	}

	/** A composition built the way the dialog builds it, from field names and counts. */
	private static Glycan counts(Object... fieldsAndCounts) throws Exception {
		CompositionOptions counts = new CompositionOptions();
		for (int i = 0; i < fieldsAndCounts.length; i += 2)
			CompositionOptions.class.getField((String) fieldsAndCounts[i]).setInt(counts, (Integer) fieldsAndCounts[i + 1]);

		MassOptions neutral = new MassOptions();
		neutral.DERIVATIZATION = MassOptions.NO_DERIVATIZATION;
		neutral.ION_CLOUD = new IonCloud();
		return counts.getCompositionAsGlycan(neutral);
	}
}
