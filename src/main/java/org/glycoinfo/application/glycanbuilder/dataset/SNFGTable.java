package org.glycoinfo.application.glycanbuilder.dataset;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;

/**
 * Table 1 of the Symbol Nomenclature for Glycans, the monosaccharide symbols: twelve shapes, each a
 * row, by ten colours, each a column, as https://www.ncbi.nlm.nih.gov/glycans/snfg.html gives them
 * (snfg-table1.tsv; last updated February 26, 2024). The colours are its Table 2.
 *
 * <p>Each cell holds SNFG's name and the name of the residue type this application draws for it.
 * They differ where this application has its own name for the residue - Neu5Ac is NeuAc here, LDmanHep
 * is L-gro-D-manHep, and the generic white symbols are named Hex, HexNAc and so on. A cell whose
 * residue this application does not have has no type name.
 */
public final class SNFGTable {

	/** A column of the table. */
	public static final class Colour {
		private final String name;
		private final Color rgb;

		Colour(String _name, int _r, int _g, int _b) {
			name = _name;
			rgb = new Color(_r, _g, _b);
		}

		public String getName() { return name; }
		/** SNFG's RGB, from its Table 2. */
		public Color getRGB() { return rgb; }
	}

	/** A row of the table: a shape, and the class of monosaccharide its white symbol stands for. */
	public static final class Shape {
		private final String name;
		private final String className;
		private final List<Cell> cells = new ArrayList<Cell>();

		Shape(String _name, String _className) {
			name = _name;
			className = _className;
		}

		public String getName() { return name; }
		public String getClassName() { return className; }
		/** One for each colour, in order; a colour SNFG leaves empty in this row is {@code null}. */
		public List<Cell> getCells() { return Collections.unmodifiableList(cells); }
	}

	/** A symbol. */
	public static final class Cell {
		private final Shape shape;
		private final Colour colour;
		private final String snfgName;
		private final String typeName;

		Cell(Shape _shape, Colour _colour, String _snfgName, String _typeName) {
			shape = _shape;
			colour = _colour;
			snfgName = _snfgName;
			typeName = _typeName;
		}

		public Shape getShape() { return shape; }
		public Colour getColour() { return colour; }
		/** The name SNFG gives the symbol. */
		public String getSNFGName() { return snfgName; }
		/** The residue type this application draws for it, or null when it has none. */
		public String getTypeName() { return typeName; }

		/** The residue type, or null when this application has none. */
		public ResidueType getType() {
			return (typeName != null && ResidueDictionary.hasResidueType(typeName))
					? ResidueDictionary.findResidueType(typeName) : null;
		}
	}

	private static final List<Colour> COLOURS = new ArrayList<Colour>();
	private static final List<Shape> SHAPES = new ArrayList<Shape>();

	static {
		colour("White", 255, 255, 255);
		colour("Blue", 0, 114, 188);
		colour("Green", 0, 166, 81);
		colour("Yellow", 255, 212, 0);
		colour("Orange", 244, 121, 32);
		colour("Pink", 246, 158, 161);
		colour("Purple", 165, 67, 153);
		colour("Light Blue", 143, 204, 233);
		colour("Brown", 161, 122, 77);
		colour("Red", 237, 28, 36);

		// Ten cells to a row, in the order of the colours above; "" is a colour SNFG leaves empty, and
		// "SNFG=type" a symbol this application names differently.
		shape("Filled Circle", "Hexose",
				"Hexose=Hex", "Glc", "Man", "Gal", "Gul", "Alt", "All", "Tal", "Ido", "");
		shape("Filled Square", "HexNAc",
				"HexNAc", "GlcNAc", "ManNAc", "GalNAc", "GulNAc", "AltNAc", "AllNAc", "TalNAc", "IdoNAc", "");
		shape("Crossed Square", "Hexosamine",
				"Hexosamine=HexN", "GlcN", "ManN", "GalN", "GulN", "AltN", "AllN", "TalN", "IdoN", "");
		shape("Divided Diamond", "Hexuronate",
				"Hexuronate=HexA", "GlcA", "ManA", "GalA", "GulA", "AltA", "AllA", "TalA", "IdoA", "");
		shape("Filled Triangle", "Deoxyhexose",
				"Deoxyhexose=dHex", "Qui", "Rha", "", "6dGul", "6dAlt", "", "6dTal", "", "Fuc");
		shape("Divided Triangle", "DeoxyhexNAc",
				"DeoxyhexNAc=dHexNAc", "QuiNAc", "RhaNAc", "", "", "6dAltNAc", "", "6dTalNAc", "", "FucNAc");
		shape("Flat Rectangle", "Di-deoxyhexose",
				"Di-deoxyhexose=ddHex", "Oli", "Tyv", "", "Abe", "Par", "Dig", "Col", "", "");
		shape("Filled Star", "Pentose",
				"Pentose=Pen", "", "Ara", "Lyx", "Xyl", "Rib", "", "", "", "");
		shape("Filled Diamond", "3-deoxy-nonulosonic acids",
				"3-deoxy-nonulosonic acids=Non", "", "Kdn", "", "", "", "Neu5Ac=NeuAc", "Neu5Gc=NeuGc", "Neu", "Sia=");
		shape("Flat Diamond", "3,9-dideoxy-nonulosonic acids",
				"3,9-dideoxy-nonulosonic acids=ddNon", "", "Pse", "Leg", "", "Aci", "", "4eLeg", "", "");
		shape("Flat Hexagon", "Unknown",
				"Unknown", "Bac", "LDmanHep=L-gro-D-manHep", "Kdo", "Dha", "DDmanHep=D-gro-D-manHep",
				"MurNAc", "MurNGc", "Mur", "");
		shape("Pentagon", "Assigned",
				"Assigned", "Api", "Fru", "Tag", "Sor", "Psi", "", "", "", "");
	}

	private SNFGTable() {}

	public static List<Colour> getColours() {
		return Collections.unmodifiableList(COLOURS);
	}

	public static List<Shape> getShapes() {
		return Collections.unmodifiableList(SHAPES);
	}

	/** Every symbol in the table, row by row. */
	public static List<Cell> getCells() {
		List<Cell> cells = new ArrayList<Cell>();
		for (Shape shape : SHAPES)
			for (Cell cell : shape.cells)
				if (cell != null) cells.add(cell);
		return cells;
	}

	private static void colour(String _name, int _r, int _g, int _b) {
		COLOURS.add(new Colour(_name, _r, _g, _b));
	}

	private static void shape(String _name, String _className, String... _cells) {
		if (_cells.length != COLOURS.size())
			throw new IllegalStateException(_name + " has " + _cells.length + " cells");
		Shape shape = new Shape(_name, _className);
		for (int i = 0; i < _cells.length; i++) {
			String cell = _cells[i];
			if (cell.isEmpty()) {
				shape.cells.add(null);
				continue;
			}
			int equals = cell.indexOf('=');
			String snfgName = (equals < 0) ? cell : cell.substring(0, equals);
			String typeName = (equals < 0) ? cell : cell.substring(equals + 1);
			shape.cells.add(new Cell(shape, COLOURS.get(i), snfgName, typeName.isEmpty() ? null : typeName));
		}
		SHAPES.add(shape);
	}
}
