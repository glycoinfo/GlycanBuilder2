package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueStyle;
import org.eurocarbdb.application.glycanbuilder.ResidueStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;
import org.glycoinfo.application.glycanbuilder.dataset.SNFGGenericSymbols;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A residue with no SNFG symbol of its own is drawn in the white symbol of its generic type where one
 * fits, and otherwise in the white pentagon (#279).
 *
 * <p>It was the white pentagon for nearly all of them: the style lookup fell back to the residue's
 * superclass, and the style file had rows for two superclasses only. The type that fits is decided
 * residue by residue in {@code conf/snfg_generic_symbols}, as the superclasses do not say it reliably.
 */
public class SnfgGenericWhiteSymbolsTest {

	private static ResidueStyleDictionary snfg;
	private static ResidueStyleDictionary cfg;

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
		snfg = new GlycanRendererAWT().getResidueStyleDictionary();
		cfg = new ResidueStyleDictionary();
		cfg.loadStyles("/conf/residue_styles_cfg");
	}

	/**
	 * Every saccharide residue with no SNFG row of its own is in the table, once, so that each is a
	 * decision and not a default; and the table names nothing else.
	 */
	@Test
	public void everyResidueWithoutASymbolIsDecided() {
		TreeSet<String> without = new TreeSet<String>();
		List<ResidueType> types = new ArrayList<ResidueType>(ResidueDictionary.allResidues());
		types.addAll(NonSymbolicResidueDictionary.getResidues());
		for (ResidueType type : types)
			if (type.isSaccharide() && !type.getName().startsWith("#") && !snfg.containsResidue(new Residue(type)))
				without.add(type.getName());

		assertEquals(without, new TreeSet<String>(SNFGGenericSymbols.table().keySet()));
	}

	/** And each is drawn in a white symbol, from a row the style file has. */
	@Test
	public void eachIsAWhiteSymbol() {
		for (Map.Entry<String, String> one : SNFGGenericSymbols.table().entrySet()) {
			ResidueStyle style = styleOf(one.getKey());
			assertNotNull(one.getKey() + " has no shape", style.getShape());
			assertEquals(one.getKey(), Color.WHITE, style.getFillColor());
		}
	}

	/** The ones #279 named, each in its generic type's shape. */
	@Test
	public void theGenericShapes() {
		String[][] expected = {
				{ "araHex", "circle" }, { "MeH", "circle" },        // hexose
				{ "araHexA", "diamond" },                            // hexuronate
				{ "The", "triangle" },                               // deoxyhexose
				{ "Ole", "flatsquare" }, { "Cym", "flatsquare" }, { "Boi", "flatsquare" }, // di-deoxyhexose
				{ "lyxPen", "star" },                                // pentose
				{ "L-gro-D-manNon", "flatdiamond" }, { "8eLeg", "flatdiamond" }, // 3,9-dideoxy-nonulosonic
				{ "Sugar", "flathexagon" },                          // nothing known
				// and the pentagon where nothing fits
				{ "Sed", "pentagon" }, { "D-gro-D-galHep", "pentagon" }, { "Ko", "pentagon" }, { "Rho", "pentagon" },
				{ "4uGlcA", "pentagon" }, { "thrHexA", "pentagon" }, { "GlcNA", "pentagon" }, { "NeuAcLac", "pentagon" },
		};
		for (String[] one : expected)
			assertEquals(one[0], one[1], styleOf(one[0]).getShape());
	}

	/**
	 * A residue with deoxies its name does not imply moves along SNFG's classes (#258): a 6-deoxy
	 * allose is a deoxyhexose, the white triangle; the 2-deoxy arabino-hexose of #260 the same.
	 */
	@Test
	public void aDeoxyMovesTheClass() throws Exception {
		assertEquals("triangle", styleOfRead("WURCS=2.0/1,1,0/[a2222m-1b_1-5_3*OC]/1/").getShape());
		assertEquals("triangle", styleOfRead("WURCS=2.0/1,1,0/[ad122h-1x_1-5_3*OC]/1/").getShape());
		assertEquals(Color.WHITE, styleOfRead("WURCS=2.0/1,1,0/[a2222m-1b_1-5_3*OC]/1/").getFillColor());
	}

	/** Each still carries its letter, as SNFG asks: the footnote names it. */
	@Test
	public void theLetterIsKept() throws Exception {
		Residue ole = new Residue(NonSymbolicResidueDictionary.getResidueType("Ole"));
		ole.setID(1);
		assertEquals("A", snfg.getStyle(ole).getText());
	}

	/**
	 * Only SNFG: in CFG they are drawn as before, as text, the CFG style file having no rows for the
	 * generic types to fall back to.
	 */
	@Test
	public void otherNotationsAreUnchanged() {
		for (String name : new String[] { "Ole", "araHex", "The", "lyxPen", "L-gro-D-manNon", "Sugar", "Sed" })
			assertEquals(name, null, cfg.getStyle(new Residue(type(name))).getShape());
	}

	// ------------------------------------------------------------------------------------------------

	private static ResidueType type(String name) {
		ResidueType type = ResidueDictionary.hasResidueType(name) ? ResidueDictionary.findResidueType(name)
				: NonSymbolicResidueDictionary.findResidueType(name);
		assertNotNull(name, type);
		return type;
	}

	private static ResidueStyle styleOf(String name) {
		return snfg.getStyle(new Residue(type(name)));
	}

	private static ResidueStyle styleOfRead(String wurcs) throws Exception {
		for (Residue r : new WURCS2Parser().readGlycan(wurcs, new MassOptions()).getAllResidues())
			if (r.isSaccharide()) return snfg.getStyle(r);
		throw new AssertionError("no monosaccharide in " + wurcs);
	}
}
