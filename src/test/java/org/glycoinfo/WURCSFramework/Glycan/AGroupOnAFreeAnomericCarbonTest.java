package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

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
 * A group on the anomeric carbon of a residue at the reducing end is read, as it was before 1.37.0
 * (#238).
 *
 * <p>G00012JB's GlcNAc carries an amine at C1, a glycosylamine. A residue's positions leave out its
 * anomeric carbon, as the carbon it is bonded to its parent by, and since the rule that a group goes
 * only where the residue takes one the amine had nowhere to go: the residue was read as a Glc with an
 * NAc, and later refused. At the reducing end that carbon is bonded to nothing, and the group takes it.
 */
public class AGroupOnAFreeAnomericCarbonTest {

	private static final String G00012JB =
			"WURCS=2.0/2,2,1/[a2122h-1a_1-5_1*N_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O]/1-2/a3-b1";

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	private static Glycan read(String _wurcs) throws Exception {
		MassOptions options = workspace.getDefaultMassOptions().clone();
		options.setDerivatization(MassOptions.NO_DERIVATIZATION);
		return new WURCS2Parser().readGlycan(_wurcs, options);
	}

	/** G00012JB's reducing-end GlcNAc keeps its NAc and has its amine at 1. */
	@Test
	public void theAmineIsAt1OfTheGlcNAc() throws Exception {
		Residue reducing = read(G00012JB).getRoot().firstChild();
		assertEquals("GlcNAc", reducing.getTypeName());
		boolean amineAt1 = false;
		for (org.eurocarbdb.application.glycanbuilder.linkage.Linkage child : reducing.getChildrenLinkages())
			if (child.getChildResidue().getTypeName().equals("N") && "1".equals(child.getParentPositionsString()))
				amineAt1 = true;
		assertEquals("no amine at 1", true, amineAt1);
	}

	/** Each weighs what WURCSFramework says, is written back as read, and survives GWS. */
	@Test
	public void eachIsWeighedAndWrittenAsRead() throws Exception {
		String[] sequences = {
				G00012JB,
				"WURCS=2.0/1,1,0/[a2122h-1b_1-5_1*N_2*NCC/3=O]/1/",   // β-GlcNAc-amine
				"WURCS=2.0/1,1,0/[a2122h-1b_1-5_1*OC]/1/",            // methyl β-glucoside
		};
		for (String sequence : sequences) {
			Glycan read = read(sequence);
			assertEquals(sequence, wurcsFrameworkMass(sequence), read.computeMass(), 1e-4);
			assertEquals(sequence, sequence, new WURCS2Parser().toWURCS(read));
			assertEquals(sequence, sequence, new WURCS2Parser().toWURCS(Glycan.fromString(read.toString())));
		}
	}

	private static double wurcsFrameworkMass(String _wurcs) throws Exception {
		WURCSGraphChemicalCompositionCalculator calculator =
				new WURCSGraphChemicalCompositionCalculator(new WURCSFactory(_wurcs).getGraph());
		calculator.start();
		return new org.eurocarbdb.application.glycanbuilder.massutil.Molecule(calculator.getComposition()).getMainMass();
	}
}
