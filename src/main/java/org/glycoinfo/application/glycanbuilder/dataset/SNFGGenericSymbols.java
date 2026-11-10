package org.glycoinfo.application.glycanbuilder.dataset;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;

/**
 * The white SNFG symbol a residue with no SNFG symbol of its own is drawn in (#279).
 *
 * <p>SNFG draws a monosaccharide absent from its Table 1 as a white symbol with a letter: the white
 * symbol of its generic type where one fits, a circle for a hexose, a triangle for a deoxyhexose, and
 * otherwise the white pentagon. Which type fits is read from {@code conf/snfg_generic_symbols}, where
 * it was decided residue by residue.
 *
 * <p>A residue carrying deoxies its name does not imply (#258) is not its type, and moves along SNFG's
 * classes by them: a hexose with one is a deoxyhexose, with two a di-deoxyhexose; a HexNAc with one is
 * a deoxy-HexNAc; a deoxyhexose with one more is a di-deoxyhexose. Anything else has no class to move
 * to, and is the pentagon.
 */
public final class SNFGGenericSymbols {

	/** The row of {@code residue_styles_snfg} that is the white pentagon. */
	public static final String PENTAGON = "Assigned";

	private static final String FILE = "/conf/snfg_generic_symbols";
	private static final Map<String, String> SYMBOLS = load();

	private SNFGGenericSymbols() {}

	/** The table as read: residue name to the style row of its white symbol. */
	public static Map<String, String> table() {
		return SYMBOLS;
	}

	/**
	 * The style row of the white symbol this residue is drawn in, or null when the table has nothing
	 * to say and the residue is drawn as it was before.
	 */
	public static String symbolFor(Residue _residue) {
		ResidueType type = _residue.getType();
		if (!_residue.hasDeoxies()) return SYMBOLS.get(type.getName());

		String generic = genericOf(type);
		if (generic == null) return PENTAGON;
		return shifted(generic, _residue.getDeoxies().size());
	}

	/**
	 * The generic type of a residue type: from its row of SNFG's table where it has one (Glc's is Hex),
	 * from this table otherwise.
	 */
	static String genericOf(ResidueType _type) {
		for (SNFGTable.Cell cell : SNFGTable.getCells())
			if (_type.getName().equals(cell.getTypeName()))
				return cell.getShape().getCells().get(0).getTypeName();
		return SYMBOLS.get(_type.getName());
	}

	/** A generic type with so many more deoxies, along SNFG's classes, or the pentagon. */
	static String shifted(String _generic, int _deoxies) {
		if (_deoxies == 0) return _generic;
		switch (_generic) {
		case "Hex": return (_deoxies == 1) ? "dHex" : (_deoxies == 2) ? "ddHex" : PENTAGON;
		case "dHex": return (_deoxies == 1) ? "ddHex" : PENTAGON;
		case "HexNAc": return (_deoxies == 1) ? "dHexNAc" : PENTAGON;
		default: return PENTAGON;
		}
	}

	private static Map<String, String> load() {
		Map<String, String> symbols = new LinkedHashMap<String, String>();
		InputStream in = SNFGGenericSymbols.class.getResourceAsStream(FILE);
		if (in == null) return Collections.unmodifiableMap(symbols);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty() || line.startsWith("%")) continue;
				String[] fields = line.split("\t");
				if (fields.length >= 2) symbols.put(fields[0], fields[1]);
			}
		} catch (java.io.IOException unreadable) {
			symbols.clear();
		}
		return Collections.unmodifiableMap(symbols);
	}
}
