package org.glycoinfo.application.glycanbuilder.converterWURCS2;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
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

		WURCSSequence2ToGlycan seq22glycan = new WURCSSequence2ToGlycan();
		seq22glycan.start(new WURCSFactory(graph), mass_opt);
		Glycan glycan = seq22glycan.getGlycan();

		// A WURCS with two connections between the same pair of residues - a bridge plus a direct
		// bond, as in G11127BT - comes out of the conversion as a genuine cycle in what every
		// walker downstream assumes is a tree. The first of them to touch it then descends
		// forever, and the report is a StackOverflowError far from the cause (#125). Refusing here
		// with a sentence keeps the document untouched and gives the caller something it can
		// actually catch; drawing such structures needs a representation for the second
		// connection, which is its own piece of work.
		refuseCycles(glycan.getRoot(), java.util.Collections.newSetFromMap(
				new java.util.IdentityHashMap<Residue, Boolean>()));

		return glycan;
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
