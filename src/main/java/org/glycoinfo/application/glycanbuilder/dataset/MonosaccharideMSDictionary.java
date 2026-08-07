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

		List<String> parts = new ArrayList<String>(Arrays.asList(_ms.split("_")));
		String skeleton = parts.get(0).split("-")[0];
		List<String> groups = (parts.size() > 2) ?
				new ArrayList<String>(parts.subList(2, parts.size())) : new ArrayList<String>();

		for (int owned = groups.size(); owned >= 0; owned--) {
			ResidueType residueType = findResidueType(describe(skeleton, groups.subList(0, owned)));
			if (residueType == null) continue;

			return new Match(residueType, new LinkedList<String>(groups.subList(owned, groups.size())));
		}

		return null;
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

			String ms = writeAlone(type);
			if (ms == null) continue;

			List<String> parts = new ArrayList<String>(Arrays.asList(ms.split("_")));
			String skeleton = parts.get(0).split("-")[0];
			List<String> own = (parts.size() > 2) ? parts.subList(2, parts.size()) : new ArrayList<String>();

			index.put(describe(skeleton, own), type.getName());
		}

		descriptionToResidue = index;
	}

	/** How this residue is written when it stands on its own, or null when it cannot be written. */
	private static String writeAlone(ResidueType _type) {
		try {
			Residue root = ResidueDictionary.createReducingEnd("freeEnd");
			root.addChild(ResidueDictionary.newResidue(_type.getName()));

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
