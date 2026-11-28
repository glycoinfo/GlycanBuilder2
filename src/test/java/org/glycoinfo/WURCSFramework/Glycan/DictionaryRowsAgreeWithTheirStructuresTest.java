package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

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
 * The dictionary rows #350 found contradicting the structure they write, recounted from it.
 *
 * <p>The conventions are the ones the large majority of rows follow. A methylation site is each free
 * OH, each N-H and each carboxyl; an acetylation site is each free OH and each free amine (#283). The
 * positions a residue offers are its carbons, less the anomeric one, its deoxy carbons and the carbons
 * carrying its own groups; the ring carbon is offered, and {@code N} only for a free amine.
 *
 * <p>Each expected mass is the formula's, plus CH2 for each methylation site or C2H2O for each
 * acetylation site, computed by hand.
 */
public class DictionaryRowsAgreeWithTheirStructuresTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	// ---- derivatization sites of residues

	/** A 6-deoxy-HexNAc has a hydroxyl fewer than a HexNAc: 4 and 3, not HexNAc's 5 and 4. */
	@Test
	public void aDeoxyHexNAcHasOneSiteFewerThanAHexNAc() throws Exception {
		for (String name : new String[] { "dHexNAc", "FucNAc", "QuiNAc", "RhaNAc", "6dAltNAc", "6dTalNAc" })
			assertSites(name, 4, 3, "3,4,5");
		assertMasses("FucNAc", 205.0950, 261.1576, 331.1267);
	}

	/**
	 * The 5,7-diamino-3,5,7,9-tetradeoxy-non-2-ulosonic acids: three hydroxyls, a carboxyl and two
	 * amines - 8 and 5, where the rows had 1 and 1.
	 */
	@Test
	public void aDiaminoNonulosonicAcidHasEightAndFiveSites() throws Exception {
		for (String name : new String[] { "Pse", "Leg", "4eLeg", "Aci", "ddNon" })
			assertSites(name, 8, 5, "1,4,6,8,N");
		assertMasses("Pse", 250.1165, 362.2417, 460.1693);
	}

	/** Mur's amine is free, so it counts as an amine, not an amide; MurNGc's glycolyl has an OH. */
	@Test
	public void muramicAcids() throws Exception {
		assertSites("Mur", 6, 4, "4,5,6,N");
		assertSites("MurNAc", 5, 3, "4,5,6");
		assertSites("MurNGc", 6, 4, "4,5,6");
		assertMasses("Mur", 251.1005, 335.1944, 419.1428);
		assertMasses("MurNGc", 309.1060, 393.1999, 477.1482);
	}

	/** A uronic acid's carboxyl is a methylation site, and its carbon a position, as GlcA's are. */
	@Test
	public void theUronicAcidsNamedByAPrefix() throws Exception {
		for (String name : new String[] { "araHexA", "ribHexA", "eryHexA", "thrHexA" })
			assertSites(name, 5, 4, "2,3,4,5,6");
		assertMasses("araHexA", 194.0427, 264.1209, 362.0849);
	}

	/**
	 * Apiofuranose has four free hydroxyls: at 1, at 2, the tertiary one at 3, and its CH2OH. It
	 * offers 2, 3 and 4 - 4 the ring carbon - and no longer 5 and 6, which a tetrose does not have.
	 */
	@Test
	public void apiose() throws Exception {
		assertSites("Api", 4, 4, "2,3,4");
		assertMasses("Api", 150.0528, 206.1154, 318.0951);
	}

	/**
	 * The tertiary hydroxyl at 3 takes a group: a methyl there was refused as if C3 had no hydroxyl,
	 * though a glucose there was taken. It is written beside the branch, and read back.
	 */
	@Test
	public void apioseTakesAMethylAtItsTertiaryHydroxyl() throws Exception {
		Residue api = residue("Api");
		assertTrue(api.addChild(ResidueDictionary.newResidue("Me"), '3'));
		String wurcs = new WURCS2Parser().toWURCS(new Glycan(api, true, new MassOptions()));
		assertTrue(wurcs, wurcs.contains("3*CO") && wurcs.contains("3*OC"));

		Glycan read = new WURCS2Parser().readGlycan(wurcs, new MassOptions());
		assertEquals(wurcs, new WURCS2Parser().toWURCS(read));
	}

	/**
	 * galNon is Kdn's skeleton, 3-deoxy-D-galacto-non-2-ulosonic acid, with C8 unknown: what it writes,
	 * Aad4334xh, and what its superclass and anomeric carbon say. Its composition was a nonose's.
	 */
	@Test
	public void galNonIsANonulosonicAcid() throws Exception {
		ResidueType galNon = ResidueDictionary.findResidueType("galNon");
		assertEquals("C9H16O9", galNon.getComposition());
		assertEquals(ResidueDictionary.findResidueType("Kdn").getComposition(), galNon.getComposition());
		assertSites("galNon", 7, 6, "1,4,5,6,7,8,9");
		assertMasses("galNon", 268.0794, 366.1890, 520.1428);
	}

	/**
	 * Read as galNon, an 8-deoxy Neu5Ac keeps its carboxyl and its 3-deoxy as the name's, and only its
	 * 8-deoxy as its own. By its prefix alone galNon was a nonose: the carboxyl became a child "A" no
	 * writer knows, and the weight came out right only because the nonose's two hydrogens too many and
	 * that child's missing ones cancelled. G06588NS: Glc + Gal + 8-deoxy-Neu5Ac, C11H19NO8.
	 */
	@Test
	public void anEightDeoxyNeu5AcReadsAsGalNonWithOneDeoxy() throws Exception {
		Glycan read = new WURCS2Parser().readGlycan("WURCS=2.0/3,3,2/[a2122h-1x_1-5][a2112h-1x_1-5]"
				+ "[Aad2112dh-2x_2-6_5*NCC/3=O]/1-2-3/a?-b1_b?-c2", new MassOptions());
		Residue galNon = null;
		for (Residue r : read.getAllResidues())
			if ("galNon".equals(r.getTypeName())) galNon = r;
		assertTrue("read as galNon", galNon != null);
		assertEquals("[8*d]", galNon.getDeoxies().toString());
		for (Residue r : read.getAllResidues())
			assertFalse("a child A", "A".equals(r.getTypeName()));

		read.getMassOptions().setDerivatization(MassOptions.NO_DERIVATIZATION);
		// 180.0634 + 180.0634 + 293.1111 - 2 x 18.0106
		assertEquals(617.2167, read.computeMass(), 1e-3);
	}

	/** The heptoses list six positions, and now say six. */
	@Test
	public void theLinkageCountIsTheNumberOfPositions() throws Exception {
		for (String name : new String[] { "L-gro-D-manHep", "D-gro-D-manHep" })
			assertEquals(name, 6, ResidueDictionary.findResidueType(name).getMaxLinkages());
	}

	// ---- positions

	/** A carbon carrying the residue's amine is not offered; the amine is, as N, only when it is free. */
	@Test
	public void theAmineIsOfferedAsNOnlyWhenFree() throws Exception {
		assertSites("Neu", 8, 6, "1,4,6,7,8,9,N");
		assertSites("NeuAc", 7, 5, "1,4,6,7,8,9");
		assertSites("NeuGc", 8, 6, "1,4,6,7,8,9");
	}

	/** An aldose named by its prefix does not offer its anomeric carbon. */
	@Test
	public void thePrefixNamedAldosesDoNotOfferC1() throws Exception {
		List<String> names = Arrays.asList("eryTet", "thrTet", "groPen", "eryPen", "thrPen", "lyxPen", "groHex",
				"eryHex", "thrHex", "araHex", "ribHex", "xylHex", "lyxHex", "gulHex", "araHep", "talHep", "gulHep",
				"manHep", "lyxHep");
		for (String name : names) {
			ResidueType type = ResidueDictionary.findResidueType(name);
			String positions = new String(type.getLinkagePositions());
			assertFalse(name + " offers " + positions, positions.contains("1"));
			assertEquals(name, positions.length(), type.getMaxLinkages());
		}
	}

	/** Sed is a ketose: C2 is its anomeric carbon, and C6 its ring carbon, as Fru's 5 is. */
	@Test
	public void sedoheptuloseIsListedAsAKetose() throws Exception {
		assertEquals("1,3,4,5,6,7", positions(ResidueDictionary.findResidueType("Sed")));
	}

	// ---- substituents: the same group weighs the same whether it is the residue's own or a child

	/** GlcN drawn as Glc with an N at 2 is permethylated as GlcN is: the amine takes two methyls. */
	@Test
	public void anAmineSubstituentIsPermethylatedAsGlcNIs() throws Exception {
		assertEquals(derivatized(residue("GlcN"), MassOptions.PERMETHYLATED),
				derivatized(glcWith("N", '2'), MassOptions.PERMETHYLATED), 1e-4);
		assertEquals(derivatized(residue("GlcN"), MassOptions.PERACETYLATED),
				derivatized(glcWith("N", '2'), MassOptions.PERACETYLATED), 1e-4);
		// 4-amino-4-deoxyglucose: hydroxyls at 1, 2, 3 and 6, and an amine
		assertMasses(glcWith("N", '4'), 179.0794, 263.1733, 389.1322);
	}

	/** NMe has an N-H; NGc an amide N-H and a hydroxyl; NFo an amide N-H, as NAc has. NDiMe has none. */
	@Test
	public void theNitrogenSubstituentsCountTheirHydrogens() throws Exception {
		assertMasses(glcWith("NMe", '2'), 193.0950, 263.1733, 403.1478);
		assertMasses(glcWith("NGc", '2'), 237.0849, 321.1788, 447.1377);
		assertMasses(glcWith("NFo", '2'), 207.0743, 277.1525, 375.1165);
		assertMasses(glcWith("NDiMe", '2'), 207.1107, 263.1733, 375.1529);
	}

	/** NDiMe is (CH3)2NH as a free compound, as NMe is CH3NH2 and N is NH3. */
	@Test
	public void nDiMeIsDimethylamine() throws Exception {
		assertEquals("C2H7N1", ResidueDictionary.findResidueType("NDiMe").getComposition());
	}

	// ---- the counters themselves

	/**
	 * Peracetylation counts the children with acetylation's counts, not methylation's: a GlcNAc below
	 * the root was counted with its amide N-H. Chitobiose, C16H28N2O11, has six free hydroxyls - 1, 3
	 * and 6 of the reducing GlcNAc, 3, 4 and 6 of the other - and two amide N-H.
	 */
	@Test
	public void theAcetylationCounterCountsChildrenForAcetylation() throws Exception {
		Glycan chitobiose = new WURCS2Parser().readGlycan(
				"WURCS=2.0/2,2,1/[a2122h-1x_1-5_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O]/1-2/a4-b1", new MassOptions());
		assertEquals(6, chitobiose.computeNoAcetylPositions());
		assertEquals(8, chitobiose.computeNoMethylPositions());

		chitobiose.getMassOptions().setDerivatization(MassOptions.PERACETYLATED);
		assertEquals(424.1693 + 6 * 42.0106, chitobiose.computeMass(), 1e-3);
	}

	// ----

	private static void assertSites(String _name, int _methyls, int _acetyls, String _positions) throws Exception {
		ResidueType type = ResidueDictionary.findResidueType(_name);
		assertEquals(_name + " methylation sites", _methyls, type.getNoMethyls());
		assertEquals(_name + " acetylation sites", _acetyls, type.getNoAcetyls());
		assertEquals(_name + " positions", _positions, positions(type));
		assertEquals(_name + " linkage count", type.getLinkagePositions().length, type.getMaxLinkages());
	}

	private static String positions(ResidueType _type) {
		StringBuilder out = new StringBuilder();
		for (char c : _type.getLinkagePositions())
			out.append(out.length() == 0 ? "" : ",").append(c);
		return out.toString();
	}

	private static void assertMasses(String _name, double _free, double _methylated, double _acetylated)
			throws Exception {
		assertMasses(residue(_name), _free, _methylated, _acetylated);
	}

	private static void assertMasses(Residue _root, double _free, double _methylated, double _acetylated)
			throws Exception {
		String what = _root.getTypeName();
		assertEquals(what + " underivatized", _free, derivatized(_root, MassOptions.NO_DERIVATIZATION), 1e-3);
		assertEquals(what + " permethylated", _methylated, derivatized(_root, MassOptions.PERMETHYLATED), 1e-3);
		assertEquals(what + " peracetylated", _acetylated, derivatized(_root, MassOptions.PERACETYLATED), 1e-3);
	}

	private static Residue residue(String _name) throws Exception {
		return ResidueDictionary.newResidue(_name);
	}

	private static Residue glcWith(String _substituent, char _position) throws Exception {
		Residue glc = ResidueDictionary.newResidue("Glc");
		assertTrue(_substituent + " at " + _position, glc.addChild(ResidueDictionary.newResidue(_substituent), _position));
		return glc;
	}

	private static double derivatized(Residue _root, String _derivatization) throws Exception {
		MassOptions options = new MassOptions();
		options.setDerivatization(_derivatization);
		Glycan glycan = new Glycan(_root.cloneSubtree(), true, options);
		glycan.getMassOptions().setDerivatization(_derivatization);
		return glycan.computeMass();
	}
}
