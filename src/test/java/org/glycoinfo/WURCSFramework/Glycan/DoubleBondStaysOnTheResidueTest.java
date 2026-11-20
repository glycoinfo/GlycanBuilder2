package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.property.chemical.WURCSGraphChemicalCompositionCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A double bond in a residue's skeleton stays on the residue (#292).
 *
 * <p>A 4,5-unsaturated uronic acid read from WURCS was read as the saturated acid: the double bond was
 * dropped, the mass was two hydrogens heavy, and the residue could not be written back. Now each carbon
 * of the double bond is kept as the skeleton writes it - "4*e", "5*E" - and the residue is weighed,
 * derivatized and written from it, and saved to GWS and read back with it.
 *
 * <p>The maintainer's rules (2026-11-10): count the hydroxyls the skeleton has, so a CH (e, z) takes a
 * derivatization site away and a carbon keeping its oxygen (E, Z) does not; carry it in GWS as a child,
 * as a deoxy is carried; give thrHexA and eryHexA the stereo their names say; and correct their rows.
 */
public class DoubleBondStaysOnTheResidueTest {

	/** G63532WE's: C4 keeps its oxygen, an enol. */
	private static final String ENOL = "WURCS=2.0/1,1,0/[a21EEA-1a_1-5]/1/";
	/** The ΔUA of heparin and chondroitin lyases: 4-deoxy-L-threo-hex-4-enopyranuronic acid. */
	private static final String DELTA_UA = "WURCS=2.0/1,1,0/[a21eEA-1a_1-5]/1/";
	/** The issue's second case, on a GlcNAc. */
	private static final String ON_GLCNAC = "WURCS=2.0/2,2,1/[a21EEA-1a_1-5][a2122h-1a_1-5_2*NCC/3=O]/1-2/a4-b1";

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** The double bond is on the residue, carbon by carbon as the skeleton writes it. */
	@Test
	public void theDoubleBondIsKept() throws Exception {
		assertEquals(Arrays.asList("4*E", "5*E"), sugarOf(wurcs(ENOL)).getDoubleBonds());
		assertEquals(Arrays.asList("4*e", "5*E"), sugarOf(wurcs(DELTA_UA)).getDoubleBonds());
	}

	/** It weighs what WURCSFramework says the skeleton weighs. */
	@Test
	public void itWeighsWhatTheSkeletonWeighs() throws Exception {
		for (String sequence : new String[] { ENOL, DELTA_UA, ON_GLCNAC })
			assertEquals(sequence, wurcsFrameworkMass(sequence), wurcs(sequence).computeMass(), 0.0001);
		assertEquals("C6H8O6, as the dictionary's 4uHexA", 176.0321, wurcs(DELTA_UA).computeMass(), 0.0001);
	}

	/**
	 * A CH takes a derivatization site away and a carbon keeping its oxygen does not. A hexuronic acid
	 * has five methylation sites and four acetylation sites - four hydroxyls and the carboxyl, which is
	 * methylated and not acetylated - so ΔUA has four and three, as 4uHexA has, and the enol keeps five
	 * and four.
	 */
	@Test
	public void sitesAreTheHydroxylsTheSkeletonHas() throws Exception {
		assertEquals(5, sites(ENOL, MassOptions.PERMETHYLATED));
		assertEquals(4, sites(ENOL, MassOptions.PERACETYLATED));
		assertEquals(4, sites(DELTA_UA, MassOptions.PERMETHYLATED));
		assertEquals(3, sites(DELTA_UA, MassOptions.PERACETYLATED));
	}

	/** Written back to WURCS as it was read: it could not be written at all. */
	@Test
	public void itIsWrittenBackAsRead() throws Exception {
		for (String sequence : new String[] { ENOL, DELTA_UA, ON_GLCNAC })
			assertEquals(sequence, new WURCS2Parser().toWURCS(wurcs(sequence)));
	}

	/** GWS carries it as children, as it carries a deoxy, and reading takes them back onto the residue. */
	@Test
	public void itSurvivesGws() throws Exception {
		for (String sequence : new String[] { ENOL, DELTA_UA, ON_GLCNAC }) {
			Glycan read = wurcs(sequence);
			String gws = read.toString();
			Glycan reopened = Glycan.fromString(gws);

			assertEquals(gws, gws, reopened.toString());
			assertEquals(gws, read.computeMass(), reopened.computeMass(), 0.0001);
			assertEquals(gws, sequence, new WURCS2Parser().toWURCS(reopened));
		}
		assertTrue(wurcs(DELTA_UA).toString(), wurcs(DELTA_UA).toString().contains("--4e)--5E"));
	}

	/** thrHexA and eryHexA have the stereo their names say, so the saturated acids can be written too. */
	@Test
	public void theSaturatedAcidsCanBeWritten() throws Exception {
		assertEquals("WURCS=2.0/1,1,0/[a21xxA-1a_1-5]/1/",
				new WURCS2Parser().toWURCS(Glycan.fromString("freeEnd--1a1L-thrHexA,p$MONO,Und,0,0,freeEnd")));
		assertEquals("WURCS=2.0/1,1,0/[a22xxA-1b_1-5]/1/",
				new WURCS2Parser().toWURCS(Glycan.fromString("freeEnd--1b1D-eryHexA,p$MONO,Und,0,0,freeEnd")));
	}

	/** Their rows: five and four sites, as every hexuronic acid of the dictionary has, and their names. */
	@Test
	public void theirRowsAreCorrected() {
		for (String name : new String[] { "thrHexA", "eryHexA" }) {
			ResidueType type = ResidueDictionary.findResidueType(name);
			assertEquals(name, 5, type.getNoMethyls());
			assertEquals(name, 4, type.getNoAcetyls());
		}
		assertEquals("threo-Hexuronic acid", ResidueDictionary.findResidueType("thrHexA").getDescription());
		assertEquals("erythro-Hexuronic acid", ResidueDictionary.findResidueType("eryHexA").getDescription());
	}

	private static Glycan wurcs(String _wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(_wurcs, workspace.getDefaultMassOptions());
	}

	private static Residue sugarOf(Glycan _structure) {
		return _structure.getRoot().getChildAt(0);
	}

	/** How many groups a derivatization puts on the lone residue, from the mass it adds. */
	private static int sites(String _wurcs, String _derivatization) throws Exception {
		Glycan plain = wurcs(_wurcs);
		Glycan derivatized = plain.clone();
		MassOptions options = plain.getMassOptions().clone();
		options.setDerivatization(_derivatization);
		derivatized.setMassOptions(options);
		double group = MassOptions.PERMETHYLATED.equals(_derivatization) ? 14.01565 : 42.01057;
		// one site is the reducing end's own hydroxyl, as for every free residue
		return (int) Math.round((derivatized.computeMass() - plain.computeMass()) / group);
	}

	private static double wurcsFrameworkMass(String _wurcs) throws Exception {
		WURCSGraphChemicalCompositionCalculator calculator =
				new WURCSGraphChemicalCompositionCalculator(new WURCSFactory(_wurcs).getGraph());
		calculator.start();
		return new org.eurocarbdb.application.glycanbuilder.massutil.Molecule(calculator.getComposition()).getMainMass();
	}
}
