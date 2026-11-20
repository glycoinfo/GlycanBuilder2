package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.SNFGFootnote;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.array.mass.WURCSMassCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.dataset.NativeMonosaccharideDictionary;
import org.glycoinfo.application.glycanbuilder.dialog.ResidueDesignDialog;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Glycerol, and alditols that are not at a reducing end (#282).
 *
 * <p>Glycerol is Gro, glyceraldehyde, as its alditol, as a ribitol is Rib as its alditol. {@code h2h}
 * was read so and could not be written, Gro having no stereo recorded; {@code hxh}, a glycerol whose
 * C2 is not known, was read as "Tri", which no dictionary has.
 *
 * <p>An alditol inside a chain - the ribitols and glycerols of teichoic acids, between phosphates - was
 * weighed as the sugar it was reduced from: 2.016 light, and one derivatization site short, since only
 * a reducing end carried the reduction. And the carbon it hangs from was lost, {@code c1} written back
 * as {@code c?}.
 */
public class GlycerolAndAlditolsInChainsTest {

	private static final double CH2 = 14.01565;

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/**
	 * Each is read as glycerol, weighs what WURCSFramework says, and goes back to WURCS, and through
	 * GWS, as WURCSFramework's normal form of it.
	 */
	@Test
	public void glycerolReadsWeighsAndRoundTrips() throws Exception {
		for (String wurcs : new String[] {
				"WURCS=2.0/1,1,0/[h2h]/1/",
				"WURCS=2.0/1,1,0/[h1h]/1/",
				"WURCS=2.0/1,1,0/[hxh]/1/",
				"WURCS=2.0/2,2,1/[h2h][a2122A-1a_1-5]/1-2/a3-b1",
				"WURCS=2.0/2,2,1/[hxh][a2122A-1a_1-5]/1-2/a3-b1" }) {
			Glycan read = read(wurcs);
			assertTrue(wurcs, glycerols(read) == 1);
			assertEquals(wurcs, wurcsMass(wurcs), underivatized(read).computeMass(), 1e-3);
			String normal = new WURCSFactory(wurcs).getWURCS();
			assertEquals(wurcs, normal, write(read));
			assertEquals(wurcs, normal, write(Glycan.fromString(read.toString())));
		}
	}

	/**
	 * D is h2h. A glycerol of unknown configuration is written hxh, as the registry writes it, and not
	 * in the relative form h4h that other residues of unknown configuration are written in.
	 */
	@Test
	public void configurationIsWrittenAtC2() throws Exception {
		assertEquals("2", NativeMonosaccharideDictionary.forResidueName("Gro").getStereo('D'));
		// glycerol on its own is not chiral, so WURCSFramework writes h1h as h2h; on a glucuronide its
		// C2 is a centre, and what the residue says is written
		Glycan read = read("WURCS=2.0/2,2,1/[h2h][a2122A-1a_1-5]/1-2/a3-b1");
		for (Residue residue : read.getAllResidues())
			if (residue.isGlycerol()) residue.setChirality('?');
		assertEquals("WURCS=2.0/2,2,1/[hxh][a2122A-1a_1-5]/1-2/a3-b1", write(read));
	}

	/**
	 * A ribitol and a glycerol inside a chain, each hanging from a phosphate: they weigh what
	 * WURCSFramework says, keep the carbon they hang from, and round-trip.
	 */
	@Test
	public void alditolsInAChainWeighAndKeepTheirCarbon() throws Exception {
		for (String wurcs : new String[] {
				"WURCS=2.0/2,3,2/[h222h][a2122h-1b_1-5]/1-2-1/a5-b1_b6-c1*OPO*/3O/3=O",
				"WURCS=2.0/2,3,2/[h2h][a2122h-1b_1-5]/1-2-1/a3-b1_b6-c3*OPO*/3O/3=O",
				"WURCS=2.0/2,3,2/[hxh][a2122h-1b_1-5]/1-2-1/a3-b1_b6-c1*OPO*/3O/3=O" }) {
			Glycan read = read(wurcs);
			assertEquals(wurcs, wurcsMass(wurcs), underivatized(read).computeMass(), 1e-3);
			String normal = new WURCSFactory(wurcs).getWURCS();
			assertEquals(wurcs, normal, write(read));
			assertEquals(wurcs, normal, write(Glycan.fromString(read.toString())));
		}
	}

	/**
	 * Permethylated, the alditol in the chain has the hydroxyl its reduction made. Ribitol-5-Glc-6-P-1-
	 * ribitol: four on the reducing ribitol, three on the glucose, one on the phosphodiester and four on
	 * the ribitol in the chain - twelve. Before, eleven, and 2.016 light.
	 */
	@Test
	public void anAlditolInAChainIsDerivatizedAtItsReducedCarbon() throws Exception {
		String wurcs = "WURCS=2.0/2,3,2/[h222h][a2122h-1b_1-5]/1-2-1/a5-b1_b6-c1*OPO*/3O/3=O";
		Glycan read = read(wurcs);
		MassOptions permethylated = read.getMassOptions().clone();
		permethylated.setDerivatization(MassOptions.PERMETHYLATED);
		read.setMassOptions(permethylated);
		assertEquals(wurcsMass(wurcs) + 12 * CH2, read.computeMass(), 1e-3);
		assertEquals(12, read.computeNoMethylPositions());
	}

	/** At the reducing end the reducing end carries the reduction, and it is not counted twice. */
	@Test
	public void atTheReducingEndTheReductionIsCountedOnce() throws Exception {
		String wurcs = "WURCS=2.0/2,2,1/[h2h][a2122A-1a_1-5]/1-2/a3-b1";
		Glycan read = read(wurcs);
		assertEquals("redEnd", read.getRoot().getTypeName());
		MassOptions permethylated = read.getMassOptions().clone();
		permethylated.setDerivatization(MassOptions.PERMETHYLATED);
		read.setMassOptions(permethylated);
		// two on the glycerol, four on the glucuronic acid
		assertEquals(wurcsMass(wurcs) + 6 * CH2, read.computeMass(), 1e-3);
	}

	/** Drawn in SNFG as a white pentagon with a letter, the footnote saying "Glycerol". */
	@Test
	public void theFootnoteSaysGlycerol() throws Exception {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
		workspace.setNotation(GraphicOptions.NOTATION_SNFG);
		GlycanRendererAWT renderer = (GlycanRendererAWT) workspace.getGlycanRenderer();

		Glycan read = read("WURCS=2.0/2,3,2/[hxh][a2122h-1b_1-5]/1-2-1/a3-b1_b6-c1*OPO*/3O/3=O");
		SNFGFootnote footnote = SNFGFootnote.of(read, renderer.getResidueStyleDictionary(), renderer.getGraphicOptions());
		List<String> expected = new ArrayList<String>();
		expected.add("A=Glycerol");
		assertEquals(expected, footnote.lines());
		for (Residue residue : read.getAllResidues())
			if (residue.isGlycerol()) assertEquals(1, footnote.ordinalOf(residue));
	}

	/**
	 * Offered by the chooser among the trioses with no SNFG symbol, as Gro to be made an alditol. A
	 * glycerol made so weighs as glycerol and is written as one.
	 */
	@Test
	public void theChooserOffersGlycerol() throws Exception {
		List<ResidueDesignDialog.Choice> found = ResidueDesignDialog.search("glycerol");
		assertTrue(!found.isEmpty());
		ResidueDesignDialog.Choice glycerol = found.get(0);
		assertEquals(ResidueDesignDialog.GLYCEROL, glycerol.getLabel());
		assertEquals("Gro", glycerol.getType().getName());
		assertTrue(glycerol.isAlditol());
		assertTrue(glycerol.getWhere(), glycerol.getWhere().startsWith(ResidueDesignDialog.NO_SYMBOL));

		// as GlycanCanvas makes it from that choice
		Residue made = new Residue(glycerol.getType());
		made.setAnomericState('?');
		made.setRingSize('o');
		Residue root = ResidueDictionary.createReducingEnd("freeEnd");
		root.addChild(made);
		Glycan drawn = new Glycan(root, false, new MassOptions());
		assertTrue(made.isGlycerol());
		assertEquals(wurcsMass("WURCS=2.0/1,1,0/[hxh]/1/"), underivatized(drawn).computeMass(), 1e-3);
		assertEquals("WURCS=2.0/1,1,0/[hxh]/1/", write(drawn));
	}

	// ------------------------------------------------------------------------------------------------

	private static Glycan read(String wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(wurcs, new MassOptions());
	}

	private static String write(Glycan glycan) throws Exception {
		return new WURCS2Parser().writeGlycan(glycan);
	}

	private static int glycerols(Glycan glycan) {
		int n = 0;
		for (Residue residue : glycan.getAllResidues())
			if (residue.isGlycerol()) n++;
		return n;
	}

	/** The structure weighed underivatized, keeping its own reducing end. */
	private static Glycan underivatized(Glycan glycan) {
		MassOptions none = glycan.getMassOptions().clone();
		none.setDerivatization(MassOptions.NO_DERIVATIZATION);
		glycan.setMassOptions(none);
		return glycan;
	}

	private static double wurcsMass(String wurcs) throws Exception {
		return WURCSMassCalculator.calcMassWURCS(new WURCSFactory(wurcs).getArray()).doubleValue();
	}
}
