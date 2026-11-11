package org.eurocarbdb.application.glycanbuilder.renderutil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.util.exchange.importer.GRESToResidue;

/**
 * The name a residue drawn as a white symbol with a letter is given in the SNFG footnote (#307).
 *
 * <p>Built from the residue - its type, anomer, configuration, ring and deoxies - and from nothing
 * else, so that it does not depend on where the structure came from. It used to: read from WURCS, a
 * residue was named from the skeleton it was read from; read from GWS, which keeps no such name, by
 * its type's description. A sedoheptulose was "β-D-Altp" one way and "Sedoheptulose" the other, and the
 * letter, which follows the name, followed the format as well.
 *
 * <p>Three rules, the maintainer's (2026-11-10, #307):
 *
 * <ul>
 * <li><b>A ketose</b> is named as IUPAC 2-Carb names it: by its accepted trivial name where it has
 * one (2-Carb-10.2, "Sedoheptulose is the accepted trivial name for D-altro-hept-2-ulose"), with the
 * ring as the ending (2-Carb-5, as in β-D-fructofuranose) - "β-D-Sedoheptulopyranose"; and
 * systematically where it has none (2-Carb-10.3, 10.4) - "β-D-altro-Hept-2-ulopyranose". It was
 * named by an aldose's abbreviation, "β-D-Altp", which reads as an altrose.
 * <li><b>Any other residue</b> is named from its own skeleton, as the WURCS reader names it - the
 * skeleton the residue writes, read back - so that a structure opened from GWS names it the same.
 * <li><b>Chemically different residues get different names</b>, and so different letters: a
 * pyranose and a furanose differ, and so do an α and a β. The same residue gets the same name, so
 * the same letter, wherever it came from.
 * </ul>
 *
 * <p>A residue that cannot be written to WURCS keeps the name it had.
 */
public final class FootnoteName {

	private FootnoteName() {}

	/** Names worked out from a residue's skeleton, by everything that decides them. */
	private static final Map<String, String> FROM_SKELETON = new ConcurrentHashMap<String, String>();

	/** The residue's footnote name. */
	public static String of(Residue _residue) {
		if (_residue == null || _residue.getType() == null) return "";

		// An alditol keeps the residue's own name. The WURCS naming has none for an open chain that
		// reads - a glycerol came back "?-Gro" one way and "?-?-Gro?" another, two letters for one
		// residue - and the residue's name, "Glycerol", is the one #282 settled on.
		if (_residue.isAlditol()) return _residue.getLegend();

		String trivial = ketoseByTrivialName(_residue);
		if (trivial != null) return trivial;

		// Kept whether or not it could be named, so a residue that cannot be written is tried once
		// rather than at every paint. "" is the answer that there is none.
		String key = signature(_residue);
		String named = FROM_SKELETON.get(key);
		if (named == null) {
			named = fromSkeleton(_residue);
			if (named == null) named = "";
			FROM_SKELETON.put(key, named);
		}
		return named.isEmpty() ? _residue.getLegend() : named;
	}

	/** What decides the name: everything the residue holds about its monosaccharide, and nothing else. */
	private static String signature(Residue _residue) {
		return _residue.getTypeName() + "|" + _residue.getAnomericState() + _residue.getAnomericCarbon()
				+ _residue.getChirality() + _residue.getRingSize() + "|" + _residue.getDeoxies() + "|"
				+ _residue.getReadSkeleton();
	}

	/**
	 * A ketose whose type is named by an accepted trivial name, "Sedoheptulose", "Ribulose",
	 * "Xylulose": that name with the ring as its ending. Null for anything else.
	 */
	static String ketoseByTrivialName(Residue _residue) {
		if (!_residue.isSaccharide() || _residue.getType().getAnomericCarbon() != '2') return null;
		String description = _residue.getType().getDescription();
		if (description == null || !description.toLowerCase().endsWith("ulose") || description.contains(" ")) return null;

		String ring = ringEnding(_residue.getRingSize());
		if (ring == null) return null;
		// "Sedoheptulose" less its "se" takes the ring: "Sedoheptulopyranose"
		String stem = description.substring(0, description.length() - "se".length());
		String name = ring.isEmpty() ? description : stem + ring;
		return deoxies(_residue) + anomer(_residue) + "-" + configuration(_residue) + "-" + capitalised(name);
	}

	/**
	 * The name from the residue's own skeleton: the residue written to WURCS alone and read back,
	 * named as the WURCS reader names one - or, for a ketose, systematically. Null when the residue
	 * cannot be written.
	 */
	private static String fromSkeleton(Residue _residue) {
		try {
			Residue alone = _residue.cloneResidue();
			alone.setLegend(null);
			// toWURCS, not writeGlycan: a residue that cannot be written is an answer here, and
			// writeGlycan reports it, which the desktop application shows as a dialog - at every paint
			String wurcs = new WURCS2Parser().toWURCS(new Glycan(alone, true, new MassOptions()));
			if (wurcs == null || wurcs.isEmpty()) return null;
			GRES gres = new WURCSFactory(wurcs).getSequence().getGRESs().getFirst();

			if (_residue.getType().getAnomericCarbon() == '2') {
				String systematic = ketoseSystematically(_residue,
						gres.getMS().getCoreStructure().getSkeletonCode());
				if (systematic != null) return systematic;
			}
			return GRESToResidue.nameOf(gres);
		} catch (Exception | Error cannotBeWritten) {
			return null;
		}
	}

	/**
	 * A neutral 2-ulose named systematically, 2-Carb-10.3 and 10.4: "β-D-altro-Hept-2-ulopyranose".
	 * Null for a skeleton this does not cover - an acid, a double bond, a stereocentre not known.
	 */
	static String ketoseSystematically(Residue _residue, String _skeleton) {
		String ring = ringEnding(_residue.getRingSize());
		if (ring == null || ring.isEmpty()) return null;
		String prefixes = configurationalPrefixes(_skeleton, 2);
		String chain = chainName(_skeleton.length());
		if (prefixes == null || chain == null) return null;

		return deoxies(_residue) + anomer(_residue) + "-" + prefixes + "-" + chain + "-2-ulo" + ring;
	}

	/**
	 * The configurational prefixes of a skeleton, 2-Carb-8: its stereocentres in groups of four from
	 * the one nearest C-1, each group named by the aldose of that many centres and given D or L by its
	 * highest-numbered centre, and the groups cited furthest from C-1 first - "L-glycero-D-manno".
	 *
	 * <p>A WURCS digit says which side of the Fischer projection the hydroxyl is on: 2 to the right,
	 * 1 to the left (a2122h is D-gluco: right, left, right, right).
	 *
	 * @param _skeleton A WURCS skeleton code, "ha1222h".
	 * @param _carbonyl The position of the carbonyl, which is not a stereocentre: 1 for an aldose, 2 for
	 *        a 2-ulose.
	 * @return Returns the prefixes, or null when a character is one this does not name: a relative
	 *         or unknown centre, an acid, a double bond.
	 */
	static String configurationalPrefixes(String _skeleton, int _carbonyl) {
		List<Character> sides = new ArrayList<Character>();
		for (int i = 0; i < _skeleton.length(); i++) {
			int position = i + 1;
			char c = _skeleton.charAt(i);
			if (position == _carbonyl) {
				if (c != 'a' && c != 'o') return null;
				continue;
			}
			if (c == '1') sides.add('L');
			else if (c == '2') sides.add('R');
			// a deoxy carbon is named by a prefix this does not build, and is left to the WURCS naming
			else if (c != 'h') return null;
		}
		if (sides.isEmpty()) return null;

		List<String> groups = new ArrayList<String>();
		for (int start = 0; start < sides.size(); start += 4) {
			List<Character> group = sides.subList(start, Math.min(start + 4, sides.size()));
			String prefix = groupPrefix(group);
			if (prefix == null) return null;
			groups.add(0, prefix);
		}
		return String.join("-", groups);
	}

	/** One group of up to four centres: "D-altro". */
	private static String groupPrefix(List<Character> _sides) {
		boolean isD = _sides.get(_sides.size() - 1) == 'R';
		StringBuilder asD = new StringBuilder();
		for (char side : _sides) asD.append(isD ? side : (side == 'R' ? 'L' : 'R'));

		String name;
		switch (asD.toString()) {
			case "R": name = "glycero"; break;
			case "RR": name = "erythro"; break;
			case "LR": name = "threo"; break;
			case "RRR": name = "ribo"; break;
			case "LRR": name = "arabino"; break;
			case "RLR": name = "xylo"; break;
			case "LLR": name = "lyxo"; break;
			case "RRRR": name = "allo"; break;
			case "LRRR": name = "altro"; break;
			case "RLRR": name = "gluco"; break;
			case "LLRR": name = "manno"; break;
			case "RRLR": name = "gulo"; break;
			case "LRLR": name = "ido"; break;
			case "RLLR": name = "galacto"; break;
			case "LLLR": name = "talo"; break;
			default: return null;
		}
		return (isD ? "D-" : "L-") + name;
	}

	/** "Hept" for seven carbons, 2-Carb-10.3's stem; null where none is given here. */
	private static String chainName(int _carbons) {
		switch (_carbons) {
			case 4: return "Tetr";
			case 5: return "Pent";
			case 6: return "Hex";
			case 7: return "Hept";
			case 8: return "Oct";
			case 9: return "Non";
			case 10: return "Dec";
			default: return null;
		}
	}

	/** The ring as a name's ending: "pyranose", "furanose"; "" when it is not known; null otherwise. */
	private static String ringEnding(char _ring) {
		switch (_ring) {
			case 'p': return "pyranose";
			case 'f': return "furanose";
			case '?': return "";
			default: return null;
		}
	}

	private static String anomer(Residue _residue) {
		switch (_residue.getAnomericState()) {
			case 'a': return "α";
			case 'b': return "β";
			default: return "?";
		}
	}

	private static String configuration(Residue _residue) {
		char c = _residue.getChirality();
		return (c == 'D' || c == 'L') ? String.valueOf(c) : "?";
	}

	/** "6-deoxy-", as the residue records its deoxies; "" for none. */
	private static String deoxies(Residue _residue) {
		List<String> positions = new ArrayList<String>();
		for (String deoxy : _residue.getDeoxies()) {
			int star = deoxy.indexOf('*');
			if (star > 0) positions.add(deoxy.substring(0, star));
		}
		if (positions.isEmpty()) return "";
		java.util.Collections.sort(positions);
		String[] multiplier = { "", "", "di", "tri", "tetra" };
		String count = (positions.size() < multiplier.length) ? multiplier[positions.size()] : "poly";
		return String.join(",", positions) + "-" + count + "deoxy-";
	}

	private static String capitalised(String _name) {
		return _name.isEmpty() ? _name : Character.toUpperCase(_name.charAt(0)) + _name.substring(1);
	}
}
