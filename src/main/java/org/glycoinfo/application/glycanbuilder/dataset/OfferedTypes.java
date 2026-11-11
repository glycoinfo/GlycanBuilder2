package org.glycoinfo.application.glycanbuilder.dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eurocarbdb.application.glycanbuilder.CoreType;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;

/**
 * Which residues and structure templates the menus, galleries and dialogs offer: only those that
 * can be written to WURCS once drawn (#315).
 *
 * <p>A residue that can be drawn but not written leaves the person with a structure they cannot
 * save. dHexA was one: offered in the residue menu, refused by the writer in every configuration.
 * The structure it stands for is not settled, so it cannot be made to write in GB2, and until GB3
 * settles it the menus leave it out (the maintainer's decision, 2026-11-11). The same holds for every
 * other type that cannot be written, so the question is asked of the writer rather than listed here:
 * a type GB3 makes writable comes back without anything here changing.
 *
 * <p>Left out, measured on 1.7.1's develop:
 *
 * <ul>
 * <li>from the residue menus and galleries: dHexA; Unknown, the white flat hexagon; Hept, which the
 * writer refuses while neither its configuration nor its ring is known; and Ino, a substituent
 * WURCS has no MAP for;
 * <li>from the structure templates: eight of the GAG repeats, whose sulfates sit where no hydroxyl
 * is free;
 * <li>from the monosaccharide chooser: Unknown, and 60 of the residues with no symbol of their own,
 * for which the writer has no skeleton.
 * </ul>
 *
 * <p>What is only left out of the offer: a file holding one of these still opens, and its residue
 * stays as it was read. The types themselves stay in their dictionaries.
 *
 * <p>Kept although it cannot be written: the "Assigned" placeholder, the white pentagon put down
 * first and said to be one monosaccharide or another afterwards (#252). It is a step on the way to a
 * residue, not one, and the maintainer kept it (2026-11-11).
 */
public final class OfferedTypes {

	private OfferedTypes() {}

	/** Offered although they cannot be written, each for a reason of its own; see the class. */
	private static final Set<String> KEPT = Collections.unmodifiableSet(new HashSet<String>(
			Collections.singletonList("Assigned")));

	/** Each type and template asked about, with the answer: writing one takes a moment, menus ask often. */
	private static final Map<ResidueType, Boolean> RESIDUES = new ConcurrentHashMap<ResidueType, Boolean>();
	private static final Map<String, Boolean> CORES = new ConcurrentHashMap<String, Boolean>();

	/**
	 * Whether a residue type is offered: whether a residue of it, added as the menus add one, can be
	 * written. A monosaccharide is tried on its own, as the first residue of a structure; anything
	 * else on a glucose, since it cannot stand alone.
	 */
	public static boolean residue(ResidueType _type) {
		if (_type == null) return false;
		if (KEPT.contains(_type.getName())) return true;
		// a reducing end is not drawn as a residue but set on a structure, and all of them write so
		if ("Reducing end".equals(_type.getSuperclass())) return true;
		Boolean known = RESIDUES.get(_type);
		if (known == null) {
			known = writes(_type);
			RESIDUES.put(_type, known);
		}
		return known;
	}

	/** The types of a collection that are offered, in its order. */
	public static List<ResidueType> residues(Collection<ResidueType> _types) {
		List<ResidueType> offered = new ArrayList<ResidueType>();
		if (_types == null) return offered;
		for (ResidueType type : _types)
			if (residue(type)) offered.add(type);
		return offered;
	}

	/** Whether a structure template is offered: whether the structure it makes can be written. */
	public static boolean core(CoreType _core) {
		if (_core == null) return false;
		Boolean known = CORES.get(_core.getName());
		if (known == null) {
			known = writes(_core);
			CORES.put(_core.getName(), known);
		}
		return known;
	}

	/** The templates of a collection that are offered, in its order. */
	public static List<CoreType> cores(Collection<CoreType> _cores) {
		List<CoreType> offered = new ArrayList<CoreType>();
		if (_cores == null) return offered;
		for (CoreType core : _cores)
			if (core(core)) offered.add(core);
		return offered;
	}

	private static boolean writes(ResidueType _type) {
		try {
			Residue residue = new Residue(_type);
			Glycan structure;
			if (_type.isSaccharide()) {
				structure = new Glycan(residue, true, new MassOptions());
			} else {
				structure = Glycan.fromString("freeEnd--?b1D-Glc,p");
				if (!structure.getRoot().getChildAt(0).addChild(residue)) return false;
			}
			return writes(structure);
		} catch (Exception | Error cannotBeMade) {
			return false;
		}
	}

	private static boolean writes(CoreType _core) {
		try {
			return writes(new Glycan(_core.newCore(), true, new MassOptions()));
		} catch (Exception | Error cannotBeMade) {
			return false;
		}
	}

	/**
	 * toWURCS rather than writeGlycan: a structure that cannot be written is an answer here, and
	 * writeGlycan reports it as an error, which the desktop application shows as a dialog.
	 */
	private static boolean writes(Glycan _structure) {
		try {
			String wurcs = new WURCS2Parser().toWURCS(_structure);
			return wurcs != null && !wurcs.isEmpty();
		} catch (Exception | Error cannotBeWritten) {
			return false;
		}
	}
}
