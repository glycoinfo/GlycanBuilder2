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
 * <p>Only the two deoxies are here. A skeleton modification also changes the hydroxyls a derivatization
 * reaches, and only for these two is that known and checked - against this application's own dictionary,
 * where fucose, the 6-deoxy galactose, has one fewer methylation and acetylation site than galactose.
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
		if (code == null || !isDeoxy(code.charValue()))
			throw new Exception("no composition is known here for the skeleton modification " + modification);

		Molecule delta = DELTAS.get(code);
		if (delta == null) {
			delta = new Molecule();
			delta.add(composition(MODIFIED.get(code)));
			delta.remove(composition(REFERENCE));
			DELTAS.put(code, delta);
		}
		return delta;
	}

	/** The composition WURCSFramework gives a lone monosaccharide. */
	static Molecule composition(String core) throws Exception {
		WURCSGraphChemicalCompositionCalculator calculator = new WURCSGraphChemicalCompositionCalculator(
				new WURCSFactory("WURCS=2.0/1,1,0/[" + core + "]/1/").getGraph());
		calculator.start();
		return new Molecule(calculator.getComposition());
	}
}
