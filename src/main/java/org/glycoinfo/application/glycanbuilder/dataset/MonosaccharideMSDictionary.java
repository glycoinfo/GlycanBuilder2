package org.glycoinfo.application.glycanbuilder.dataset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.LinkageTypeOptimizer;
import org.glycoinfo.application.glycanbuilder.util.exchange.exporter.GlycanToWURCSGraph;

/**
 * Which residue a WURCS monosaccharide description stands for, answered from this builder's own
 * dictionary rather than from a name lookup elsewhere.
 *
 * <p>A WURCS MS reads {@code a2122h-1x_1-5_2*NCC/3=O}: a skeleton code, the anomeric position and
 * symbol, the ring, and then whatever hangs off the residue. The skeleton alone does not say which
 * residue it is - Glc, GlcN and GlcNAc all carry {@code a2122h} - but the skeleton together with the
 * substituents that belong to the residue itself does, and uniquely so for every residue this
 * builder can write.
 *
 * <p>The index is built by writing each residue out and reading its description back, so it agrees
 * with the exporter by construction: a residue is recognised on the way in exactly as it is written
 * on the way out. Anything the exporter cannot write is simply absent, and the caller falls back.</p>
 */
public class MonosaccharideMSDictionary {

	/** skeleton code and own substituents -> residue name */
	private static Map<String, String> descriptionToResidue = null;

	/**
	 * The residue a monosaccharide description names, ignoring anything attached to it beyond the
	 * substituents that are part of the residue itself.
	 * @param _description MS description as it appears in a WURCS string.
	 * @return Returns the residue type, or null when this builder writes no such residue.
	 */
	public static ResidueType findResidueType(String _description) {
		if (_description == null || _description.isEmpty()) return null;

		String name = getIndex().get(_description);
		return (name == null) ? null : ResidueDictionary.findResidueType(name);
	}

	/**
	 * Splits a monosaccharide description into the residue it names and the groups left over, which
	 * are the substituents and bridges attached to it. The longest description that names a residue
	 * wins, so a GlcNAc is read as one residue rather than as a Glc carrying an N-acetyl.
	 * @param _ms Full MS string of one GRES.
	 * @return Returns the split, or null when no residue could be recognised.
	 */
	public static Match match(String _ms) {
		if (_ms == null || _ms.isEmpty()) return null;

		String skeleton = skeletonOf(_ms);
		List<String> groups = groupsOf(_ms);

		for (int owned = groups.size(); owned >= 0; owned--) {
			ResidueType residueType = findResidueType(describe(skeleton, groups.subList(0, owned)));
			if (residueType == null) continue;

			return new Match(residueType, new LinkedList<String>(groups.subList(owned, groups.size())));
		}

		return null;
	}

	/** The skeleton code of an MS, which is whatever precedes the anomeric information. */
	private static String skeletonOf(String _ms) {
		return _ms.split("_")[0].split("-")[0];
	}

	/**
	 * The groups written after the residue itself.
	 *
	 * <p>An MS with a ring reads {@code a2122h-1x_1-5_2*NCC/3=O}, and the ring - {@code 1-5} - is the
	 * one segment that is not a group. A residue with no ring has no such segment at all, so the
	 * groups begin one place earlier: {@code u2122h_2*NCC/3=O}. Assuming the ring was always there
	 * lost the substituents of every residue written without one.
	 */
	private static List<String> groupsOf(String _ms) {
		List<String> parts = new ArrayList<String>(Arrays.asList(_ms.split("_")));
		int first = (parts.size() > 1 && parts.get(1).matches("[0-9?]+-[0-9?]+")) ? 2 : 1;

		return (parts.size() > first) ?
				new ArrayList<String>(parts.subList(first, parts.size())) : new ArrayList<String>();
	}

	/** What a residue and the groups it owns are written as. */
	private static String describe(String _skeleton, List<String> _ownGroups) {
		StringBuilder description = new StringBuilder(_skeleton);
		for (String group : _ownGroups) description.append('|').append(group);

		return description.toString();
	}

	/** Clears the index so it is rebuilt against a newly loaded dictionary. */
	public static synchronized void clear() {
		descriptionToResidue = null;
	}

	private static synchronized Map<String, String> getIndex() {
		if (descriptionToResidue == null) build();
		return descriptionToResidue;
	}

	private static void build() {
		Map<String, String> index = new HashMap<String, String>();

		for (ResidueType type : ResidueDictionary.allResidues()) {
			if (!type.isSaccharide() || type.isBridge()) continue;

			for (Form form : Form.values()) {
				String ms = writeAlone(type, form);
				if (ms == null) continue;

				// the ring form is written first and keeps the key if two forms ever collide
				String description = describe(skeletonOf(ms), groupsOf(ms));
				if (!index.containsKey(description)) index.put(description, type.getName());
			}
		}

		descriptionToResidue = index;
	}

	/**
	 * The forms one residue can be written in. Indexing only the first left the rest to a name lookup
	 * elsewhere, and they are not rare: in a corpus of two hundred structures one residue in five
	 * arrived without a determined anomeric carbon, as an alditol, or as an open chain.
	 */
	private enum Form {
		/** with a ring and a determined anomeric carbon - {@code a2122h-1x_1-5} */
		RING,
		/** ring and anomeric carbon both undetermined - {@code u2122h} */
		UNDETERMINED,
		/** reduced, so no anomeric carbon to determine - {@code h2122h} */
		ALDITOL,
		/** open chain - {@code o2122h} */
		ALDEHYDE
	}

	/** How this residue is written standing on its own in that form, or null when it cannot be. */
	private static String writeAlone(ResidueType _type, Form _form) {
		try {
			Residue root = ResidueDictionary.createReducingEnd("freeEnd");
			Residue residue = ResidueDictionary.newResidue(_type.getName());
			if (_form == Form.UNDETERMINED) {
				residue.setAnomericCarbon('?');
				residue.setRingSize('?');
			}
			residue.setAlditol(_form == Form.ALDITOL);
			residue.setAldehyde(_form == Form.ALDEHYDE);
			root.addChild(residue);

			Glycan glycan = new Glycan(root, false, new MassOptions());
			new LinkageTypeOptimizer().start(glycan);

			GlycanToWURCSGraph toGraph = new GlycanToWURCSGraph();
			toGraph.start(glycan);
			String wurcs = new WURCSFactory(toGraph.getGraph()).getWURCS();

			return wurcs.substring(wurcs.indexOf('[') + 1, wurcs.indexOf(']'));
		} catch (Throwable cannotBeWritten) {
			return null;
		}
	}

	/** A monosaccharide description split into the residue it names and what is attached to it. */
	public static class Match {
		private final ResidueType residueType;
		private final LinkedList<String> attachedGroups;

		Match(ResidueType _residueType, LinkedList<String> _attachedGroups) {
			this.residueType = _residueType;
			this.attachedGroups = _attachedGroups;
		}

		public ResidueType getResidueType() {
			return this.residueType;
		}

		/** The groups left over, each written as WURCS writes it: "6*OSO/3=O/3=O", "4-6", ... */
		public LinkedList<String> getAttachedGroups() {
			return this.attachedGroups;
		}
	}
}
