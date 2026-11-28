package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.property.chemical.WURCSGraphChemicalCompositionCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A carbonyl that is not the anomeric carbon of a ring is weighed and written as the skeleton has it
 * (#342).
 *
 * <p>An open-chain ketose, hO122h, was read as its alditol, two hydrogens heavy; a 1-deoxy one, mO122h,
 * as an aldose with its C1 a deoxy, a water light; a ketone inside a ring, aO122h, and an aldehyde at
 * the far end of the chain, a2122o, were built as children named "O" and "o" that weighed nothing.
 * All of the cases here are GlyTouCan's.
 */
public class FreeCarbonylsTest {

	private static final String[] CARBONYLS = {
			"WURCS=2.0/1,1,0/[hO122h_1*OPO/3O/3=O]/1/",       // G03873YC, fructose 1-phosphate, open
			"WURCS=2.0/1,1,0/[hO122m]/1/",                     // G05722PJ, 6-deoxy fructose, open
			"WURCS=2.0/1,1,0/[hO221h]/1/",                     // G36112OD
			"WURCS=2.0/1,1,0/[mO122h]/1/",                     // G06460IZ, 1-deoxy fructose, open
			"WURCS=2.0/1,1,0/[mO12h]/1/",                      // G16289GE, a 1-deoxy pentulose
			"WURCS=2.0/1,1,0/[aO122h-1b_1-5]/1/",              // G35382GP, D-arabino-hexos-2-ulose
			"WURCS=2.0/1,1,0/[a2O22h-1a_1-4]/1/",              // G32726ZM, a hexos-3-ulose
			"WURCS=2.0/2,2,1/[a2122h-1b_1-5][a21O2h-1b_1-5]/1-2/a4-b1",   // G35705GP, a 4-keto galactoside
			"WURCS=2.0/1,1,0/[a2122o-1a_1-5]/1/",              // G42102BK, gluco-hexodialdose
			"WURCS=2.0/1,1,0/[o2112o]/1/",                     // G20013XO, galacto-hexodialdose, open
			"WURCS=2.0/1,1,0/[oO211h]/1/",                     // G01798OD
			"WURCS=2.0/1,1,0/[o2O22h]/1/",                     // G32631BM
			"WURCS=2.0/1,1,0/[h1O21h]/1/",                     // G29036BC, a hex-3-ulose
			"WURCS=2.0/1,1,0/[hO1122h]/1/",                    // G35726XK, a hept-2-ulose
			"WURCS=2.0/1,1,0/[AOd22o]/1/",                     // G24840OJ
			"WURCS=2.0/1,1,0/[AO211h]/1/",                     // G10571IW, a 2-keto hexonic acid
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
		for (String sequence : CARBONYLS)
			assertEquals(sequence, wurcsFrameworkMass(sequence), wurcs(sequence).computeMass(), 0.0001);
		assertEquals("fructose, C6H12O6", 180.0634, wurcs("WURCS=2.0/1,1,0/[hO122h]/1/").computeMass(), 0.0001);
	}

	/** Each is written back as it was read. */
	@Test
	public void eachIsWrittenBackAsRead() throws Exception {
		for (String sequence : CARBONYLS)
			assertEquals(sequence, sequence, new WURCS2Parser().toWURCS(wurcs(sequence)));
	}

	/** An open-chain ketose is its ketose's open form, not its alditol. */
	@Test
	public void anOpenKetoseIsNoAlditol() throws Exception {
		Residue fructose = sugarOf(wurcs("WURCS=2.0/1,1,0/[hO122h]/1/"));
		assertEquals("Fru", fructose.getTypeName());
		assertFalse(fructose.isAlditol());
		assertTrue(fructose.isAldehyde());

		Residue deoxy = sugarOf(wurcs("WURCS=2.0/1,1,0/[mO122h]/1/"));
		assertEquals("Fru", deoxy.getTypeName());
		assertEquals(Arrays.asList("1*m"), deoxy.getDeoxies());
	}

	/** A ketone in a ring, and a dialdose's second aldehyde, stay on the residue - and the ring stays. */
	@Test
	public void theCarbonylIsKeptOnTheResidue() throws Exception {
		Residue ulose = sugarOf(wurcs("WURCS=2.0/1,1,0/[aO122h-1b_1-5]/1/"));
		assertEquals("named for six carbons, not as Ara", "araHex", ulose.getTypeName());
		assertEquals(Arrays.asList("2*O"), ulose.getOxidations());
		assertEquals('p', ulose.getRingSize());
		assertFalse("no child", ulose.hasChildren());

		Residue dialdose = sugarOf(wurcs("WURCS=2.0/1,1,0/[a2122o-1a_1-5]/1/"));
		assertEquals("Glc", dialdose.getTypeName());
		assertEquals(Arrays.asList("6*o"), dialdose.getOxidations());

		Residue hexulose = sugarOf(wurcs("WURCS=2.0/1,1,0/[h1O21h]/1/"));
		assertTrue("an alditol with its C3 a ketone", hexulose.isAlditol());
		assertEquals(Arrays.asList("3*O"), hexulose.getOxidations());
	}

	/**
	 * A ketone and an aldehyde have no hydroxyl. Glucose has five methylation and acetylation sites,
	 * and its dialdose four; open-chain fructose has five, as the furanose has.
	 */
	@Test
	public void sitesAreTheHydroxylsTheSkeletonHas() throws Exception {
		assertEquals(4, sites("WURCS=2.0/1,1,0/[a2122o-1a_1-5]/1/", MassOptions.PERMETHYLATED));
		assertEquals(4, sites("WURCS=2.0/1,1,0/[a2122o-1a_1-5]/1/", MassOptions.PERACETYLATED));
		assertEquals(4, sites("WURCS=2.0/1,1,0/[aO122h-1b_1-5]/1/", MassOptions.PERMETHYLATED));
		assertEquals(5, sites("WURCS=2.0/1,1,0/[hO122h]/1/", MassOptions.PERMETHYLATED));
		assertEquals(5, sites("WURCS=2.0/1,1,0/[hO122h]/1/", MassOptions.PERACETYLATED));
	}

	/** GWS carries the carbonyls as children, as it carries a deoxy, and reading takes them back. */
	@Test
	public void theySurviveGws() throws Exception {
		for (String sequence : CARBONYLS) {
			Glycan read = wurcs(sequence);
			String gws = read.toString();
			Glycan reopened = Glycan.fromString(gws);

			assertEquals(gws, sugarOf(read).getOxidations(), sugarOf(reopened).getOxidations());
			assertEquals(gws, read.computeMass(), reopened.computeMass(), 0.0001);
			assertEquals(gws, sequence, new WURCS2Parser().toWURCS(reopened));
		}
	}

	/** A copy is the same monosaccharide. */
	@Test
	public void aCopyIsTheSame() throws Exception {
		for (String sequence : CARBONYLS) {
			Glycan read = wurcs(sequence);
			assertEquals(sequence, read.computeMass(), read.clone().computeMass(), 0.0001);
			assertEquals(sequence, sequence, new WURCS2Parser().toWURCS(read.clone()));
		}
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
