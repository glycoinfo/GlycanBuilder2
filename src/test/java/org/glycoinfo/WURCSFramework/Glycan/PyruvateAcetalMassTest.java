package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.property.chemical.WURCSGraphChemicalCompositionCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.dataset.CrossLinkedSubstituentDictionary;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A cyclic pyruvate acetal, 4,6-O-(1-carboxyethylidene), weighs what it is (#343).
 *
 * <p>A bridge's composition is the molecule that condenses with the two hydroxyls it joins, each bond
 * taking a water away: phosphate is H3PO4, sulfate H2SO4, an anhydro H2O. A pyruvate acetal is formed
 * from pyruvic acid, C3H4O3, and two hydroxyls with one water lost, so its composition is C3H6O4 - the
 * hydrate, CH3C(OH)2COOH. It was C3H5O4, and every residue with one weighed a hydrogen light.
 */
public class PyruvateAcetalMassTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** Each configuration, and two on one structure, weigh what WURCSFramework says they weigh. */
	@Test
	public void eachWeighsWhatWurcsFrameworkSays() throws Exception {
		String[] sequences = {
				"WURCS=2.0/1,1,0/[a2112h-1b_1-5_4-6*OC^XO*/3CO/6=O/3C]/1/",    // G36522MA
				"WURCS=2.0/1,1,0/[a2112h-1b_1-5_4-6*OC^SO*/3CO/6=O/3C]/1/",
				"WURCS=2.0/1,1,0/[a2112h-1b_1-5_4-6*OC^RO*/3CO/6=O/3C]/1/",
				// G12121XY: two of them, in the 4n2-6n1 form
				"WURCS=2.0/3,3,2/[a2122h-1x_1-5][a2122h-1x_1-5_4n2-6n1*1OC^RO*2/3CO/6=O/3C]"
						+ "[a2122h-1x_1-5_4n2-6n1*1OC^RO*2/3CO/6=O/3C_3*OC]/1-2-3/a?-b1_b?-c1",
		};
		for (String sequence : sequences)
			assertEquals(sequence, wurcsFrameworkMass(sequence), wurcs(sequence).computeMass(), 0.0001);
		assertEquals("4,6-O-(1-carboxyethylidene)-galactose, C9H14O8", 250.0689,
				wurcs(sequences[0]).computeMass(), 0.0001);
	}

	/** The three rows say the same composition. */
	@Test
	public void theRowsAreTheHydrate() throws Exception {
		for (String name : new String[] { "Py", "(S)Py", "(R)Py" })
			assertEquals(name, "C3H6O4", CrossLinkedSubstituentDictionary.getCrossLinkedSubstituent(name).getComposition());
	}

	private static Glycan wurcs(String _wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(_wurcs, workspace.getDefaultMassOptions());
	}

	private static double wurcsFrameworkMass(String _wurcs) throws Exception {
		WURCSGraphChemicalCompositionCalculator calculator =
				new WURCSGraphChemicalCompositionCalculator(new WURCSFactory(_wurcs).getGraph());
		calculator.start();
		return new org.eurocarbdb.application.glycanbuilder.massutil.Molecule(calculator.getComposition()).getMainMass();
	}
}
