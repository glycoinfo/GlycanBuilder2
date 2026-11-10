package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.glycoinfo.application.glycanbuilder.dataset.NativeMonosaccharideDictionary;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;

/**
 * Names a monosaccharide whose skeleton has stereocentres of unknown configuration, {@code x} (#265).
 *
 * <p>The naming converter refuses them outright - "This SkeletonCode partially contains the wild card"
 * for {@code a1122xh}, "could not handled" for {@code adxxxxh} - and the structure did not open. They
 * are named here instead, by what the known centres allow:
 *
 * <ul>
 * <li><b>None known</b>: the type of unknown stereo for the chain - {@code Pen}, {@code Hex},
 * {@code HexA}, {@code Hept} - with its deoxies.
 * <li><b>Known from C2 on, unknown beyond</b>: the configurational prefix of the known ones and the
 * chain - {@code a1122xh} is manno at C2 to C5 and nothing at C6, which is what {@code manHep} says, as
 * {@code D-glycero-D-manno-heptose} would say both. When no residue has that name, as below.
 * <li><b>Anything else</b> - an unknown centre between known ones: the type of unknown stereo, with
 * the skeleton kept on the residue to be written back as read.
 * </ul>
 *
 * <p>Aldoses only, open or closed, ending in a hydroxymethyl, a methyl or an acid, and glycerol. Anything else is
 * left to the converter, which refuses it as before.
 */
final class UnknownStereoNames {

	/** What a skeleton is read as. */
	static final class Naming {
		final String typeName;
		final char configuration;
		final List<String> deoxies;
		final boolean keepSkeleton;

		Naming(String _typeName, char _configuration, List<String> _deoxies, boolean _keepSkeleton) {
			typeName = _typeName;
			configuration = _configuration;
			deoxies = _deoxies;
			keepSkeleton = _keepSkeleton;
		}
	}

	private static final String[] LENGTHS = { null, null, null, "Tri", "Tet", "Pen", "Hex", "Hep", "Oct", "Non", "Dec" };

	private UnknownStereoNames() {}

	/** Whether a skeleton has a stereocentre of unknown configuration. */
	static boolean hasUnknownStereo(String _skeletonCode) {
		return _skeletonCode.indexOf('x') >= 0 || _skeletonCode.indexOf('X') >= 0;
	}

	/** The naming of a skeleton with unknown stereocentres, or null when it is not one named here. */
	static Naming name(String _skeletonCode) {
		String s = _skeletonCode;
		int n = s.length();
		// glycerol whose C2 is not known: the alditol of glyceraldehyde, Gro, of unknown configuration
		// (#282). Read as Tri until now, which no dictionary has.
		if (s.equals("hxh")) return new Naming("Gro", '?', new ArrayList<String>(), false);
		if (n < 5 || n > 10 || !hasUnknownStereo(s)) return null;
		char first = s.charAt(0), last = s.charAt(n - 1);
		if (first != 'a' && first != 'o') return null;
		if (last != 'h' && last != 'm' && last != 'A') return null;

		List<String> deoxies = new ArrayList<String>();
		StringBuilder known = new StringBuilder();
		boolean unknownSeen = false, knownAfterUnknown = false, relative = false;
		for (int i = 1; i < n - 1; i++) {
			char c = s.charAt(i);
			if (c == 'd') { deoxies.add((i + 1) + "*d"); continue; }
			if (c == 'x' || c == 'X') { unknownSeen = true; continue; }
			if ("1234".indexOf(c) < 0) return null;
			if (unknownSeen) knownAfterUnknown = true;
			if (c == '3' || c == '4') relative = true;
			known.append(c);
		}
		if (last == 'm') deoxies.add(n + "*m");

		String generic = genericFor(n, last);
		if (known.length() == 0)
			return (generic == null) ? null : new Naming(generic, '?', deoxies, false);

		// the known centres from C2 on, and only unknown ones past them: a prefix can say it
		if (!knownAfterUnknown && !relative && known.length() <= 4) {
			String[] prefix = NativeMonosaccharideDictionary.prefixFor(known.toString());
			String name = (prefix == null) ? null : prefix[0] + LENGTHS[n];
			if (name != null && last != 'A' && residueExists(name))
				return new Naming(name, prefix[1].charAt(0), deoxies, false);
		}

		return (generic == null) ? null : new Naming(generic, '?', deoxies, true);
	}

	/**
	 * Why a skeleton with unknown stereocentres is not one named here, in words a user can act on. The
	 * converter's own refusal - "partially contains the wild card", "could not handled" - named nothing.
	 * What is missing is a type of unknown stereo to read it as; which ones to add is #298's question.
	 */
	static String whyNot(String _skeletonCode) {
		String s = _skeletonCode;
		int n = s.length();
		String what;
		if (n == 0) what = "an empty skeleton";
		else if (s.charAt(0) != 'a' && s.charAt(0) != 'o') what = "a ketose or another monosaccharide that is not an aldose";
		else if (s.charAt(n - 1) == 'A' && n != 6) what = "a uronic acid of " + n + " carbons";
		else if (n < 5 || n > 7) what = "a chain of " + n + " carbons";
		else what = "a monosaccharide of this form";
		return s + " has stereocentres of unknown configuration (x in WURCS) in " + what
				+ ", for which GlycanBuilder2 has no type of unknown stereo to read it as";
	}

	/** The type of unknown stereo for a chain: Pen, Hex, HexA, Hept. */
	private static String genericFor(int _length, char _last) {
		if (_last == 'A') return (_length == 6) ? "HexA" : null;
		switch (_length) {
		case 5: return "Pen";
		case 6: return "Hex";
		case 7: return "Hept";
		default: return null;
		}
	}

	private static boolean residueExists(String _name) {
		return ResidueDictionary.hasResidueType(_name) || NonSymbolicResidueDictionary.hasResidueType(_name);
	}
}
