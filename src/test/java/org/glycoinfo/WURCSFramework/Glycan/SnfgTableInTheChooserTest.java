package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.Icon;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueStyle;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.dataset.OfferedTypes;
import org.glycoinfo.application.glycanbuilder.dataset.SNFGTable;
import org.glycoinfo.application.glycanbuilder.dialog.ResidueDesignDialog;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * SNFG's Table 1 in the monosaccharide chooser (#252), and how this application draws each symbol in it
 * against SNFG's own shapes and colours.
 *
 * <p>The chooser shows SNFG's table so that a placed "Assigned" placeholder can become any monosaccharide
 * SNFG has a symbol for, as well as one it has none for. Each symbol is drawn by this application's own
 * SNFG renderer, so the table shows what will be drawn - and this test checks that against SNFG.
 */
public class SnfgTableInTheChooserTest {

	private static final String GLUCOSE = "freeEnd--?b1D-Glc,p$MONO,Und,0,0,freeEnd";

	/** SNFG symbols this application has no residue for. */
	private static final TreeSet<String> NOT_HERE = new TreeSet<String>(Arrays.asList("Sia"));

	/**
	 * Symbols this application draws differently from SNFG, and how: none. Listed so that one appearing
	 * is noticed.
	 *
	 * <p>Unknown, SNFG's white flat hexagon, was drawn as the white pentagon: #280 had made the
	 * {@code Unknown} row the pentagon so that the residues filed under Unknown with no symbol would fall
	 * back to it. They now fall back to the {@code Assigned} row, and Unknown is its hexagon again.
	 */
	private static final TreeMap<String, String> DRAWN_DIFFERENTLY = new TreeMap<String, String>();

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** Twelve shapes by ten colours, 87 symbols, as SNFG's snfg-table1.tsv has them. */
	@Test
	public void theTableIsSnfgsTable() {
		assertEquals(12, SNFGTable.getShapes().size());
		for (SNFGTable.Shape shape : SNFGTable.getShapes())
			assertEquals(shape.getName(), 10, shape.getCells().size());
		assertEquals(87, SNFGTable.getCells().size());

		// SNFG's Table 2
		assertEquals(new Color(255, 212, 0), colour("Yellow"));
		assertEquals(new Color(237, 28, 36), colour("Red"));
	}

	/** Every symbol is a residue this application has, but those it has none for. */
	@Test
	public void everySymbolIsAResidueHere() {
		TreeSet<String> missing = new TreeSet<String>();
		for (SNFGTable.Cell cell : SNFGTable.getCells()) {
			ResidueType type = cell.getType();
			if (type == null) missing.add(cell.getSNFGName());
			else assertTrue(cell.getSNFGName() + " is not a saccharide here", type.isSaccharide());
		}
		assertEquals(NOT_HERE, missing);
	}

	/**
	 * Each symbol is drawn in SNFG's shape for its row and SNFG's colour for its column.
	 *
	 * <p>Tag was drawn in 255,172,0 where SNFG's yellow is 255,212,0, which Gal, Lyx, Kdo and Leg are
	 * drawn in; corrected here.
	 */
	@Test
	public void eachSymbolIsDrawnInSnfgsShapeAndColour() {
		GlycanRendererAWT snfg = new GlycanRendererAWT();
		TreeMap<String, String> different = new TreeMap<String, String>();

		for (SNFGTable.Cell cell : SNFGTable.getCells()) {
			ResidueType type = cell.getType();
			if (type == null) continue;
			ResidueStyle style = snfg.getResidueStyleDictionary().getStyle(new Residue(type));

			String drawn = style.getShape() + "/" + style.getFillStyle();
			List<String> expected = shapesOf(cell.getShape().getName());
			if (!expected.contains(drawn))
				different.put(cell.getSNFGName(), "shape " + drawn + ", SNFG " + String.join(" or ", expected));
			else if (!style.getFillColor().equals(cell.getColour().getRGB()))
				different.put(cell.getSNFGName(), "colour " + rgb(style.getFillColor()) + ", SNFG "
						+ cell.getColour().getName() + " " + rgb(cell.getColour().getRGB()));
		}
		assertEquals(DRAWN_DIFFERENTLY, different);
	}

	/** Every symbol in the chooser is drawn, by the renderer the chooser uses. */
	@Test
	public void everySymbolHasAnIcon() {
		for (SNFGTable.Cell cell : SNFGTable.getCells()) {
			if (cell.getType() == null) continue;
			Icon icon = ResidueDesignDialog.snfgIcon(cell.getType());
			assertNotNull(cell.getSNFGName(), icon);
			assertTrue(cell.getSNFGName(), icon.getIconWidth() > 0 && icon.getIconHeight() > 0);
		}
	}

	/** A placeholder can become any of them, and is saved to GWS and read back as it. */
	@Test
	public void aPlaceholderCanBecomeAnySymbol() throws Exception {
		List<String> failures = new ArrayList<String>();
		for (SNFGTable.Cell cell : SNFGTable.getCells()) {
			ResidueType type = cell.getType();
			if (type == null || "Assigned".equals(type.getName())) continue;

			GlycanDocument doc = new GlycanDocument(workspace);
			doc.addStructures(Arrays.asList(Glycan.fromString(GLUCOSE)));
			Residue placeholder = ResidueDictionary.newResidue("Assigned");
			doc.addResidue(sugarOf(doc), placeholder);

			if (!doc.assignPlaceholder(placeholder, null, type)) {
				failures.add(cell.getSNFGName() + ": not assigned");
				continue;
			}
			String gws = doc.getFirstStructure().toString();
			Glycan read = Glycan.fromString(gws);
			if (read == null || !gws.equals(read.toString()) || !hasType(read, type))
				failures.add(cell.getSNFGName() + ": " + gws + " -> " + (read == null ? null : read.toString()));
		}
		assertTrue(failures.size() + " failed:\n" + String.join("\n", failures), failures.isEmpty());
	}

	/** The tabs follow SNFG's table: every row in one tab, in SNFG's order. */
	@Test
	public void everyRowIsInOneTabInSnfgsOrder() {
		List<SNFGTable.Shape> inTabs = new ArrayList<SNFGTable.Shape>();
		for (List<SNFGTable.Shape> tab : ResidueDesignDialog.snfgTabs().values()) inTabs.addAll(tab);
		assertEquals(SNFGTable.getShapes(), inTabs);
		assertEquals(Arrays.asList("Hexoses", "Deoxyhexoses", "Pentoses", "Nonulosonic acids", "Other SNFG"),
				new ArrayList<String>(ResidueDesignDialog.snfgTabs().keySet()));
	}

	/** A tab's icon is the white symbol of its class, SNFG's symbol for a class rather than one residue. */
	@Test
	public void aTabsIconIsTheWhiteSymbolOfItsClass() {
		for (java.util.Map.Entry<String, List<SNFGTable.Shape>> tab : ResidueDesignDialog.snfgTabs().entrySet()) {
			SNFGTable.Cell white = tab.getValue().get(0).getCells().get(0);
			assertEquals(tab.getKey(), "White", white.getColour().getName());
			assertEquals(tab.getKey(), white.getTypeName(), ResidueDesignDialog.tabSymbol(tab.getKey()));
		}
		assertEquals("Assigned", ResidueDesignDialog.tabSymbol(ResidueDesignDialog.NO_SYMBOL));
	}

	/**
	 * Every residue on offer is in the dialog once: each SNFG symbol here, and each without one.
	 * Glycerol is offered besides, as Gro made an alditol (#282), and is counted apart.
	 */
	@Test
	public void everythingIsOnOfferOnce() {
		List<ResidueType> offered = new ArrayList<ResidueType>();
		int alditols = 0;
		for (ResidueDesignDialog.Choice choice : ResidueDesignDialog.allChoices()) {
			if (choice.isAlditol()) {
				alditols++;
				continue;
			}
			offered.add(choice.getType());
		}
		assertEquals(1, alditols);

		// what cannot be written once drawn is not offered (#315): of SNFG's symbols, Unknown
		List<ResidueType> expected = new ArrayList<ResidueType>();
		for (SNFGTable.Cell cell : SNFGTable.getCells())
			if (cell.getType() != null && OfferedTypes.residue(cell.getType())) expected.add(cell.getType());
		for (List<ResidueType> section : ResidueDesignDialog.sections().values()) expected.addAll(section);

		assertEquals(expected, offered);
		assertEquals(85 + 27, offered.size());
	}

	/** One search finds across every tab, the best match first. */
	@Test
	public void searchFindsAcrossTheTabsBestFirst() {
		assertTrue(ResidueDesignDialog.search("  ").isEmpty());
		assertEquals("Glc", ResidueDesignDialog.search("glc").get(0).getType().getName());
		assertEquals("Kdo", ResidueDesignDialog.search("KDO").get(0).getType().getName());
		// SNFG's name finds this application's: Neu5Ac is NeuAc here
		assertEquals("NeuAc", ResidueDesignDialog.search("neu5ac").get(0).getType().getName());

		List<String> heptoses = labels(ResidueDesignDialog.search("hep"));
		assertTrue(heptoses.toString(), heptoses.containsAll(Arrays.asList("LDmanHep", "manHep", "Sed")));
		// and not what is not offered (#315)
		assertFalse(heptoses.toString(), heptoses.contains("D-gro-D-galHep"));

		// and by shape or colour
		List<String> flatHexagons = labels(ResidueDesignDialog.search("flat hexagon"));
		assertTrue(flatHexagons.toString(), flatHexagons.containsAll(Arrays.asList("Kdo", "Mur")));
		assertFalse(flatHexagons.toString(), flatHexagons.contains("Unknown"));
	}

	/**
	 * The chooser's symbols are SNFG's whatever notation the canvas is in: its table is drawn by a
	 * renderer of its own, checked by drawing a symbol with the workspace in SNFG and again in UOXF,
	 * which has other shapes.
	 */
	@Test
	public void theChoosersSymbolsAreSnfgWhateverTheNotation() {
		ResidueType placeholder = ResidueDictionary.findResidueType("Assigned");
		String before = workspace.getGraphicOptions().NOTATION;
		int[] inSnfg;
		int[] inUoxf;
		try {
			workspace.setNotation(org.eurocarbdb.application.glycanbuilder.util.GraphicOptions.NOTATION_SNFG);
			inSnfg = pixels(ResidueDesignDialog.snfgImage(placeholder, 24));
			workspace.setNotation(org.eurocarbdb.application.glycanbuilder.util.GraphicOptions.NOTATION_UOXF);
			inUoxf = pixels(ResidueDesignDialog.snfgImage(placeholder, 24));
		} finally {
			workspace.setNotation(before);
		}
		assertTrue("nothing was drawn", inSnfg.length > 0);
		assertTrue("the symbol changed with the notation", Arrays.equals(inSnfg, inUoxf));
	}

	/**
	 * The chooser's icon is a small table of SNFG symbols - blue, yellow, red and the white pentagon - in
	 * SNFG's colours, with the workspace in another notation. A single symbol beside the toolbar's
	 * residue buttons read as one more of them.
	 */
	@Test
	public void theChoosersIconIsASmallSnfgTable() {
		String before = workspace.getGraphicOptions().NOTATION;
		java.awt.image.BufferedImage icon;
		try {
			workspace.setNotation(org.eurocarbdb.application.glycanbuilder.util.GraphicOptions.NOTATION_UOXF);
			icon = (java.awt.image.BufferedImage) ResidueDesignDialog.chooserImage(24);
		} finally {
			workspace.setNotation(before);
		}
		assertEquals(24, icon.getWidth());
		// the middle of each quarter, and for the triangle a little below it, where it is widest
		assertEquals(colour("Blue"), new Color(icon.getRGB(6, 6)));
		assertEquals(colour("Yellow"), new Color(icon.getRGB(18, 6)));
		assertEquals(colour("Red"), new Color(icon.getRGB(6, 19)));
		assertEquals(colour("White"), new Color(icon.getRGB(18, 18)));
	}

	/**
	 * When the canvas is not in SNFG the dialog says so, naming the notation as the View menu does; in
	 * SNFG it says nothing, as what it shows is what will be drawn.
	 */
	@Test
	public void theDialogSaysWhenTheCanvasIsNotSnfg() {
		assertEquals(null, ResidueDesignDialog.canvasNote("snfg"));
		assertEquals(null, ResidueDesignDialog.canvasNote(null));
		for (String[] notation : new String[][] { { "cfg", "CFG" }, { "cfgbw", "CFG black and white" },
				{ "cfglink", "CFG with linkage placement" }, { "uoxf", "UOXF" }, { "uoxfcol", "UOXFCOL" },
				{ "text", "Text only" } }) {
			String note = ResidueDesignDialog.canvasNote(notation[0]);
			assertNotNull(notation[0], note);
			assertTrue(note, note.contains("shown in SNFG") && note.contains("in " + notation[1] + " notation"));
		}
	}

	// ------------------------------------------------------------------------------------------------

	private static int[] pixels(java.awt.Image image) {
		javax.swing.ImageIcon loaded = new javax.swing.ImageIcon(image);  // waits for the image to load
		java.awt.image.BufferedImage b = new java.awt.image.BufferedImage(
				loaded.getIconWidth(), loaded.getIconHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = b.createGraphics();
		g.drawImage(loaded.getImage(), 0, 0, null);
		g.dispose();
		return b.getRGB(0, 0, b.getWidth(), b.getHeight(), null, 0, b.getWidth());
	}

	private static List<String> labels(List<ResidueDesignDialog.Choice> choices) {
		List<String> labels = new ArrayList<String>();
		for (ResidueDesignDialog.Choice choice : choices) labels.add(choice.getLabel());
		return labels;
	}

	/** SNFG's shapes as this application's styles name them: shape/fill. */
	private static List<String> shapesOf(String snfgShape) {
		switch (snfgShape) {
		case "Filled Circle": return Arrays.asList("circle/full");
		case "Filled Square": return Arrays.asList("square/full");
		case "Crossed Square": return Arrays.asList("square/topright");
		// the L-hexuronates, IdoA and AltA, are SNFG's divided diamond turned over
		case "Divided Diamond": return Arrays.asList("diamond/top", "diamond/bottom");
		case "Filled Triangle": return Arrays.asList("triangle/full");
		case "Divided Triangle": return Arrays.asList("triangle/right");
		case "Flat Rectangle": return Arrays.asList("flatsquare/full");
		case "Filled Star": return Arrays.asList("star/full");
		case "Filled Diamond": return Arrays.asList("diamond/full");
		case "Flat Diamond": return Arrays.asList("flatdiamond/full");
		case "Flat Hexagon": return Arrays.asList("flathexagon/full");
		case "Pentagon": return Arrays.asList("pentagon/full");
		default: throw new AssertionError("no shape " + snfgShape);
		}
	}

	private static Color colour(String name) {
		for (SNFGTable.Colour colour : SNFGTable.getColours())
			if (colour.getName().equals(name)) return colour.getRGB();
		throw new AssertionError(name);
	}

	private static String rgb(Color c) {
		return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
	}

	private static Residue sugarOf(GlycanDocument doc) {
		for (Residue r : doc.getFirstStructure().getAllResidues())
			if (r.isSaccharide()) return r;
		throw new AssertionError("no monosaccharide");
	}

	private static boolean hasType(Glycan glycan, ResidueType type) {
		for (Residue r : glycan.getAllResidues())
			if (r.getType() == type) return true;
		return false;
	}
}
