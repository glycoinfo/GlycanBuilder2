package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.property.chemical.WURCSGraphChemicalCompositionCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * An open-chain acid - an aldonic acid, an aldaric acid, a 2-keto acid, and their lactones - is read as
 * the alditol of its name with the oxidised carbons kept on the residue (#341).
 *
 * <p>The carboxyl at C1 was built as a child named "A" that weighed nothing, or was left out, and the
 * acid came back 16 to 64 Da light; a hexonic acid was read as the uronic acid whose reduction it is,
 * with that acid's aldehyde, two hydrogens light, and every position on it counted from the other end.
 * All of the cases here are GlyTouCan's.
 */
public class OpenChainAcidsTest {

	private static final String[] ACIDS = {
			"WURCS=2.0/1,1,0/[A2h]/1/",                 // G41185RT, glyceric acid
			"WURCS=2.0/1,1,0/[A21h]/1/",                // G04439TG, threonic acid
			"WURCS=2.0/1,1,0/[A22A]/1/",                // G12655FY, erythraric acid
			"WURCS=2.0/1,1,0/[A2122h]/1/",              // D-gluconic acid
			"WURCS=2.0/1,1,0/[A1222h]/1/",              // G03567FY
			"WURCS=2.0/1,1,0/[AOd2A]/1/",               // G29844PK, a 2-keto 3-deoxy pentaric acid
			"WURCS=2.0/1,1,0/[Ad12Oh]/1/",              // G05333AM, a 5-keto 2-deoxy hexonic acid
			"WURCS=2.0/1,1,0/[A22h_1-4]/1/",            // G37923KE, erythrono-1,4-lactone
			"WURCS=2.0/1,1,0/[A2112h_2-6]/1/",          // G05866BJ, 2,6-anhydro galactonic acid
			"WURCS=2.0/1,1,0/[AZZ21h_1-4]/1/",          // G06868CT, an ascorbic acid
			"WURCS=2.0/1,1,0/[A2h_2*OPO/3O/3=O]/1/",    // G21469RW, 2-phosphoglyceric acid
			"WURCS=2.0/2,2,1/[A2122h_1-5][a2122h-1b_1-5]/1-2/a4-b1",   // G19354UQ
			"WURCS=2.0/2,2,1/[A2h][a2122h-1a_1-5]/1-2/a2-b1",          // G11499DG, glucosylglycerate
			"WURCS=2.0/2,2,1/[A2122h_1-5_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O]/1-2/a4-b1",  // G08176ED
	};

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** Each weighs what WURCSFramework says it weighs. */
	@Test
	public void eachWeighsWhatItsSkeletonWeighs() throws Exception {
		for (String sequence : ACIDS)
			assertEquals(sequence, wurcsFrameworkMass(sequence), wurcs(sequence).computeMass(), 0.0001);
		assertEquals("C6H12O7", 196.0583, wurcs("WURCS=2.0/1,1,0/[A2122h]/1/").computeMass(), 0.0001);
	}

	/** Each is written back as it was read. */
	@Test
	public void eachIsWrittenBackAsRead() throws Exception {
		for (String sequence : ACIDS)
			assertEquals(sequence, sequence, new WURCS2Parser().toWURCS(wurcs(sequence)));
	}

	/** The acid is the alditol of its name, the oxidised carbons on the residue as WURCS writes them. */
	@Test
	public void theOxidisedCarbonsAreKeptOnTheAlditol() throws Exception {
		Residue gluconic = sugarOf(wurcs("WURCS=2.0/1,1,0/[A2122h]/1/"));
		assertEquals("named from the WURCS's own C1, not as an L-GulA alditol", "Glc", gluconic.getTypeName());
		assertTrue(gluconic.isAlditol());
		assertEquals(Arrays.asList("1*A"), gluconic.getOxidations());
		assertFalse("no child", gluconic.hasChildren());

		assertEquals(Arrays.asList("1*A", "4*A"), sugarOf(wurcs("WURCS=2.0/1,1,0/[A22A]/1/")).getOxidations());
		assertEquals(Arrays.asList("1*A", "2*O", "5*A"), sugarOf(wurcs("WURCS=2.0/1,1,0/[AOd2A]/1/")).getOxidations());
	}

	/**
	 * A ketone takes its hydroxyl away; a carboxyl keeps one, which a methylation reaches as an ester
	 * and an acetylation does not - as the dictionary has a uronic acid. Gluconic acid has five
	 * hydroxyls and its carboxyl, erythraric acid two and two carboxyls, and the 2-keto 3-deoxy
	 * pentaric acid one hydroxyl, at C4, and two carboxyls.
	 */
	@Test
	public void sitesAreTheHydroxylsAndCarboxylsTheChainHas() throws Exception {
		assertEquals(6, sites("WURCS=2.0/1,1,0/[A2122h]/1/", MassOptions.PERMETHYLATED));
		assertEquals(5, sites("WURCS=2.0/1,1,0/[A2122h]/1/", MassOptions.PERACETYLATED));
		assertEquals(4, sites("WURCS=2.0/1,1,0/[A22A]/1/", MassOptions.PERMETHYLATED));
		assertEquals(2, sites("WURCS=2.0/1,1,0/[A22A]/1/", MassOptions.PERACETYLATED));
		assertEquals(3, sites("WURCS=2.0/1,1,0/[AOd2A]/1/", MassOptions.PERMETHYLATED));
		assertEquals(1, sites("WURCS=2.0/1,1,0/[AOd2A]/1/", MassOptions.PERACETYLATED));
	}

	/**
	 * GWS carries the oxidised carbons as children, as it carries a deoxy, and reading takes them back.
	 * The lactones are left out: an Anhydro read back from GWS is not the bridge read from WURCS, and
	 * weighs a water light on any residue - a 1,6-anhydro glucose too - which is not this issue's.
	 */
	@Test
	public void theySurviveGws() throws Exception {
		for (String sequence : ACIDS) {
			if (sequence.matches(".*_[0-9]-[0-9].*")) continue;
			Glycan read = wurcs(sequence);
			String gws = read.toString();
			Glycan reopened = Glycan.fromString(gws);

			assertTrue(gws, gws.contains("--1A"));
			assertEquals(gws, sugarOf(read).getOxidations(), sugarOf(reopened).getOxidations());
			assertEquals(gws, read.computeMass(), reopened.computeMass(), 0.0001);
			assertEquals(gws, sequence, new WURCS2Parser().toWURCS(reopened));
		}
	}

	/** A copy is the same acid. */
	@Test
	public void aCopyIsTheSameAcid() throws Exception {
		for (String sequence : ACIDS) {
			Glycan read = wurcs(sequence);
			assertEquals(sequence, read.computeMass(), read.clone().computeMass(), 0.0001);
			assertEquals(sequence, sequence, new WURCS2Parser().toWURCS(read.clone()));
		}
	}

	/**
	 * A name of another number of carbons is refused, saying so: the converter names a 2-keto tetronic
	 * acid, AO1h, by its one stereocentre as Gro, which has three, and it was written back as AOh.
	 */
	@Test
	public void aNameOfAnotherLengthIsRefused() throws Exception {
		try {
			wurcs("WURCS=2.0/1,1,0/[AO1h]/1/");
			fail("read as Gro");
		} catch (WURCSToGlycanException refused) {
			assertTrue(refused.getMessage(), refused.getMessage().contains("AO1h has 4 carbons"));
		}
	}

	/** The open chain of an ulosonic acid is no alditol, and is read as it was. */
	@Test
	public void kdoIsNoAlditol() throws Exception {
		String kdo = "WURCS=2.0/1,1,0/[Aad1122h-2a_2-6]/1/";
		Residue read = sugarOf(wurcs(kdo));
		assertEquals("Kdo", read.getTypeName());
		assertFalse(read.isAlditol());
		assertTrue(read.getOxidations().isEmpty());
	}

	private static Glycan wurcs(String _wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(_wurcs, workspace.getDefaultMassOptions());
	}

	private static Residue sugarOf(Glycan _structure) {
		return _structure.getRoot().getChildAt(0);
	}

	/** How many groups a derivatization puts on the structure, from the mass it adds. */
	private static int sites(String _wurcs, String _derivatization) throws Exception {
		Glycan plain = wurcs(_wurcs);
		Glycan derivatized = plain.clone();
		MassOptions options = plain.getMassOptions().clone();
		options.setDerivatization(_derivatization);
		derivatized.setMassOptions(options);
		double group = MassOptions.PERMETHYLATED.equals(_derivatization) ? 14.01565 : 42.01057;
		return (int) Math.round((derivatized.computeMass() - plain.computeMass()) / group);
	}

	private static double wurcsFrameworkMass(String _wurcs) throws Exception {
		WURCSGraphChemicalCompositionCalculator calculator =
				new WURCSGraphChemicalCompositionCalculator(new WURCSFactory(_wurcs).getGraph());
		calculator.start();
		return new org.eurocarbdb.application.glycanbuilder.massutil.Molecule(calculator.getComposition()).getMainMass();
	}
}
