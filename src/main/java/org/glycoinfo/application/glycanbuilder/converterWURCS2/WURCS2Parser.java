package org.glycoinfo.application.glycanbuilder.converterWURCS2;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParser;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.wurcs.graph.Modification;
import org.glycoinfo.WURCSFramework.wurcs.graph.WURCSGraph;
import org.glycoinfo.application.glycanbuilder.dataset.SubstituentMAPDictionary;
import org.glycoinfo.application.glycanbuilder.util.exchange.exporter.GlycanToWURCSGraph;
import org.glycoinfo.application.glycanbuilder.util.exchange.importer.WURCSSequence2ToGlycan;

public class WURCS2Parser implements GlycanParser{
	
	public void setTolerateUnknown(boolean f) {}
	
	public String writeGlycan(Glycan structure) {
		if (structure.isFragment()) return "";
    if (structure.isComposition()) return "";

		try{
			LinkageTypeOptimizer linkOpt = new LinkageTypeOptimizer();
			linkOpt.start(structure);

			GlycanToWURCSGraph glycan2graph = new GlycanToWURCSGraph();
			glycan2graph.start(structure);
			// MAPs go out in their normalized form: that is what a validator asks for, and readGlycan
			// accepts either spelling, so the round trip still holds
			WURCSGraph graph = glycan2graph.getGraph();
			this.rewriteMAPs(graph, true);

			return new WURCSFactory(graph).getWURCS();
		}catch (Exception e) {
			LogUtils.report(e);
			return "";
		}
	}
	
	public Glycan readGlycan(String str, MassOptions mass_opt) throws Exception{
		if(str.equals("") || !str.contains("WURCS")) throw new Exception(str + " is wrong format");
		mass_opt.setDerivatization("Und");
		mass_opt.ION_CLOUD.set("Na", 0);
		
		str = str.trim();		
		if(str.contains("\t")) str = str.substring(str.indexOf("\t") + 1);
		
		WURCSGraph graph = new WURCSFactory(str).getGraph();
		// whatever spelling the MAPs arrived in, hand the conversion the one it recognises
		this.rewriteMAPs(graph, false);

		Glycan glycan;
		try {
			WURCSSequence2ToGlycan seq22glycan = new WURCSSequence2ToGlycan();
			seq22glycan.start(new WURCSFactory(graph), mass_opt);
			glycan = seq22glycan.getGlycan();
		} catch (RuntimeException undescribed) {
			// The conversion's own failures come out as raw NullPointerExceptions and
			// StringIndexOutOfBounds - "String index out of range: 8" for a substituent placed at
			// position 9 of a hexose, say - which name nothing and read as crashes rather than as
			// answers (#123). This is the one door every WURCS enters through, so the translation
			// happens here: what kind of failure, on which sequence, with the original underneath
			// for whoever needs the trace.
			throw new WURCSToGlycanException("could not convert this WURCS to a structure ("
					+ undescribed.getClass().getSimpleName()
					+ (undescribed.getMessage() != null ? ": " + undescribed.getMessage() : "")
					+ "): " + shortenedForError(str), undescribed);
		}

		// A WURCS with two connections between the same pair of residues - a bridge plus a direct
		// bond, as in G11127BT - comes out of the conversion as a genuine cycle in what every
		// walker downstream assumes is a tree. The first of them to touch it then descends
		// forever, and the report is a StackOverflowError far from the cause (#125). Refusing here
		// with a sentence keeps the document untouched and gives the caller something it can
		// actually catch; drawing such structures needs a representation for the second
		// connection, which is its own piece of work.
		refuseCycles(glycan.getRoot(), java.util.Collections.newSetFromMap(
				new java.util.IdentityHashMap<Residue, Boolean>()));

		// Say what each bond is made of here, where the structure is built, rather than leaving it
		// to whoever writes it out (#4). This pass used to run in writeGlycan alone, so a structure
		// read from WURCS carried UNVALIDATED bonds until the moment it was written back to WURCS -
		// and anything else that asked, a GlycoCT writer or a renderer, was asking a placeholder.
		new LinkageTypeOptimizer().start(glycan);

		return glycan;
	}

	/**
	 * The sequence, cut to fit an error message.
	 * @param sequence The WURCS being read.
	 * @return Returns at most eighty characters of it.
	 */
	private static String shortenedForError(String sequence) {
		String flat = sequence.strip();

		return flat.length() > 80 ? flat.substring(0, 80) + "..." : flat;
	}

	/**
	 * Walks the residue tree and throws where it finds itself again.
	 * @param residue Residue to walk from.
	 * @param visited Every residue already walked, by identity.
	 * @throws Exception If the tree has a cycle in it.
	 */
	private static void refuseCycles(Residue residue, java.util.Set<Residue> visited) throws Exception {
		if (residue == null) return;
		if (!visited.add(residue))
			throw new Exception("this WURCS makes two connections between the same residues"
					+ " (a ring through a bridge), which cannot be represented yet");

		for (org.eurocarbdb.application.glycanbuilder.linkage.Linkage linkage : residue.getChildrenLinkages())
			refuseCycles(linkage.getChildResidue(), visited);
	}

	/**
	 * Rewrites every MAP in the graph, either into its normalized form on the way out or into the
	 * spelling the conversion dictionaries recognise on the way in. Doing both through the same
	 * table is what keeps a structure identical across a round trip.
	 * @param graph Graph whose modifications are rewritten in place.
	 * @param toNormalized True to normalize, false to use the dictionary spelling.
	 */
	private void rewriteMAPs(WURCSGraph graph, boolean toNormalized) {
		for (Modification modification : graph.getModifications()) {
			String map = modification.getMAPCode();
			if (map == null || map.isEmpty()) continue;

			String rewritten = toNormalized ?
					SubstituentMAPDictionary.normalizeForOutput(map) : SubstituentMAPDictionary.toDictionaryForm(map);
			if (!rewritten.equals(map)) modification.setMAPCode(rewritten);
		}
	}

	@Override
	public String writeGlycan(Glycan structure, BBoxManager bboxManager) {
		throw new UnsupportedOperationException();
	}
}
