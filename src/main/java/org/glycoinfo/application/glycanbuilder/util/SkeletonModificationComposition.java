package org.glycoinfo.application.glycanbuilder.util;

import java.util.HashMap;
import java.util.Map;

import org.eurocarbdb.application.glycanbuilder.massutil.Molecule;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.property.chemical.WURCSGraphChemicalCompositionCalculator;

/**
 * What a skeleton modification does to a monosaccharide's composition, as WURCSFramework computes it.
 *
 * <p>A residue read from WURCS can carry a deoxy its type's name does not imply - a 6-deoxy allose, a
 * 2-deoxy arabino-hexose - and this application has no residue type for most of them. The residue
 * keeps its type and records the deoxy (#258, #263). Its composition is then the type's, changed by
 * what the deoxy changes, and that change is asked of WURCSFramework rather than written down here:
 * the composition of a reference skeleton with the modification, less the same skeleton without it.
 *
 * <p>Asked of a reference skeleton and not of the residue's own, because after a save to GWS and a
 * reopen there is no WURCS left to ask - the structure is this application's model, and the model knows
 * its type and its deoxies but not a skeleton code. A deoxy's effect does not depend on where it is or
 * on the stereo around it, so a stereo-free hexose is enough to measure it on.
 *
 * <p>The two deoxies, and the carbons of a double bond (#292): e and z, a CH, and E and Z, a C that keeps
 * its oxygen - e and E one geometry, z and Z the other. A skeleton modification also changes the
 * hydroxyls a derivatization reaches. For a deoxy that is checked against this application's own
 * dictionary, where fucose, the 6-deoxy galactose, has one fewer methylation and acetylation site than
 * galactose. For a double bond the maintainer's rule is to count the hydroxyls the skeleton has
 * (2026-11-10): e and z have lost theirs, E and Z keep it - see {@link #removesAHydroxyl}.
 */
public final class SkeletonModificationComposition {

	/** A reference pyranose with no stereo, and the same with each modification put in. */
	private static final String REFERENCE = "axxxxh-1x_1-5";
	private static final Map<Character, String> MODIFIED = new HashMap<Character, String>();
	static {
		MODIFIED.put('d', "adxxxh-1x_1-5");   // an internal deoxy, here at C2
		MODIFIED.put('m', "axxxxm-1x_1-5");   // a terminal deoxy, C6
	}

	private static final Map<Character, Molecule> DELTAS = new HashMap<Character, Molecule>();

	private SkeletonModificationComposition() {
	}

	/** Whether this is a modification code whose effect is known here: {@code d} or {@code m}. */
	public static boolean isDeoxy(char code) {
		return MODIFIED.containsKey(code);
	}

	/** Whether this is the code of a carbon in a double bond of known geometry: e, E, z or Z. */
	public static boolean isDoubleBond(char code) {
		return code == 'e' || code == 'E' || code == 'z' || code == 'Z';
	}

	/** Whether a modification written as WURCS writes it - {@code 4*e} - is a carbon of a double bond. */
	public static boolean isDoubleBond(String modification) {
		Character code = codeOf(modification);
		return code != null && isDoubleBond(code.charValue());
	}

	/**
	 * Whether this skeleton modification takes a hydroxyl away, and so a derivatization site: a deoxy,
	 * and a double-bond carbon that is a CH (e, z). One that keeps its oxygen (E, Z) keeps the site.
	 */
	public static boolean removesAHydroxyl(String modification) {
		Character code = codeOf(modification);
		if (code == null) return false;
		return isDeoxy(code.charValue()) || code.charValue() == 'e' || code.charValue() == 'z';
	}

	/** Whether a modification written as WURCS writes it - {@code 6*m}, {@code 2*d} - is a deoxy. */
	public static boolean isDeoxy(String modification) {
		Character code = codeOf(modification);
		return code != null && isDeoxy(code.charValue());
	}

	/** The code of a {@code position*code} modification, or null when it has some other shape. */
	public static Character codeOf(String modification) {
		if (modification == null) return null;
		int star = modification.indexOf('*');
		if (star < 1 || star != modification.length() - 2) return null;
		return modification.charAt(star + 1);
	}

	/** The position of a {@code position*code} modification, or null when it is not a single carbon. */
	public static Character positionOf(String modification) {
		if (codeOf(modification) == null) return null;
		String position = modification.substring(0, modification.indexOf('*'));
		return (position.length() == 1) ? position.charAt(0) : null;
	}

	/**
	 * The change this modification makes to a composition, as a molecule whose counts may be negative -
	 * one oxygen fewer, for either deoxy.
	 *
	 * @throws Exception if it is not a deoxy, or WURCSFramework cannot compute it
	 */
	public static synchronized Molecule deltaOf(String modification) throws Exception {
		Character code = codeOf(modification);
		if (code == null || !(isDeoxy(code.charValue()) || isDoubleBond(code.charValue())))
			throw new Exception("no composition is known here for the skeleton modification " + modification);

		Molecule delta = DELTAS.get(code);
		if (delta == null) {
			delta = isDeoxy(code.charValue()) ? change(MODIFIED.get(code)) : doubleBondCarbon(code.charValue());
			DELTAS.put(code, delta);
		}
		return delta;
	}

	/** What a skeleton changes, against the reference. */
	private static Molecule change(String _modified) throws Exception {
		Molecule delta = new Molecule();
		delta.add(composition(_modified));
		delta.remove(composition(REFERENCE));
		return delta;
	}

	/**
	 * What one carbon of a double bond changes. A double bond is two carbons, so it is measured as a
	 * pair: C3=C4 both keeping their oxygen (EE, ZZ) is twice the change of one of them, and a CH beside
	 * one (eE, zZ) is that one's change and its own.
	 */
	private static Molecule doubleBondCarbon(char _code) throws Exception {
		char upper = Character.toUpperCase(_code);
		Molecule keepsItsOxygen = half(change("ax" + upper + upper + "xh-1x_1-5"));
		if (_code == upper) return keepsItsOxygen;

		Molecule mixed = change("ax" + _code + upper + "xh-1x_1-5");
		mixed.remove(keepsItsOxygen);
		return mixed;
	}

	private static Molecule half(Molecule _twice) throws Exception {
		Molecule ret = new Molecule();
		for (java.util.Map.Entry<org.eurocarbdb.application.glycanbuilder.massutil.Atom, Integer> atom : _twice.getAtoms()) {
			int count = atom.getValue().intValue();
			if (count % 2 != 0) throw new Exception("a pair of double-bond carbons changed an odd number of " + atom.getKey());
			if (count > 0) ret.add(atom.getKey(), count / 2);
			else if (count < 0) ret.remove(atom.getKey(), -count / 2);
		}
		return ret;
	}

	/** The composition WURCSFramework gives a lone monosaccharide. */
	static Molecule composition(String core) throws Exception {
		WURCSGraphChemicalCompositionCalculator calculator = new WURCSGraphChemicalCompositionCalculator(
				new WURCSFactory("WURCS=2.0/1,1,0/[" + core + "]/1/").getGraph());
		calculator.start();
		return new Molecule(calculator.getComposition());
	}
}
