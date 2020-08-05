package org.glycoinfo.application.glycanbuilder.util.exchange.exporter;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glycoinfo.WURCSFramework.util.exchange.WURCSExchangeException;
import org.glycoinfo.WURCSFramework.util.graph.WURCSGraphNormalizer;
import org.glycoinfo.WURCSFramework.wurcs.graph.*;
import org.glycoinfo.application.glycanbuilder.util.GlycanUtils;

import java.util.HashMap;
import java.util.LinkedList;

public class GlycanToWURCSGraph {
	
	private WURCSGraph graph;
	private HashMap<Residue, Backbone> a_mResidueIndex = new HashMap<Residue, Backbone>();
	
	public WURCSGraph getGraph() {
		return this.graph;
	}
	
	public void start(Glycan a_oGlycan) throws Exception {
		GlycanVisitorAnalyzeForWURCSGraph a_uGVAWG = new GlycanVisitorAnalyzeForWURCSGraph();
		
		a_uGVAWG.start(a_oGlycan);
		this.graph = new WURCSGraph();
		
		// Residue to
		for(Residue a_oRES : a_uGVAWG.getResidues()) {
			if(GlycanUtils.isCollisionLinkagePosition(a_oRES))
				throw new Exception("This glycan have illegal linkage posiiton");
			boolean a_bIsRootOfFragment = (a_oRES.hasParent() && a_oRES.getParent().isBracket());
			this.analyzeResidue(a_oRES, a_bIsRootOfFragment);
		}
		
		// Linkage to WURCSEdge
		for(Linkage a_oLIN : a_uGVAWG.getLinkages()) {	
			LinkageToWURCSEdge a_oL2WE = new LinkageToWURCSEdge();
			a_oL2WE.start(a_oLIN);

			if(a_oL2WE.getParent().isBracket()) continue;
						
			Modification a_oMOD = a_oL2WE.getModification();
			
			// analyze for repeating
			if(a_uGVAWG.getRepeatingResidueByLinkage(a_oLIN) != null) {
				ModificationRepeat a_oRepMOD = new ModificationRepeat(a_oMOD.getMAPCode());
				Residue a_oRepRES = a_uGVAWG.getRepeatingResidueByLinkage(a_oLIN);
				a_oRepMOD.setMaxRepeatCount(a_oRepRES.getMaxRepetitions());
				a_oRepMOD.setMinRepeatCount(a_oRepRES.getMinRepetitions());
				a_oMOD = a_oRepMOD;
			}
			
			// parent side
			if(!a_oL2WE.getParent().isReducingEnd() && !a_oL2WE.getParent().isRepetition() && !a_oL2WE.getParent().isStartCyclic()) {
				Backbone a_oParent = this.a_mResidueIndex.get(a_oL2WE.getParent());
				this.makeLinkage(a_oParent, a_oL2WE.getParentEdges(), a_oMOD);
			}
			
			if(a_oL2WE.getChild() == null || a_oL2WE.getChild().isRepetition()) continue;
			// child side
			Backbone a_oChild = this.a_mResidueIndex.get(a_oL2WE.getChild());
			this.makeLinkage(a_oChild, a_oL2WE.getChildEdges(), a_oMOD);
		}
		
		// Ambiguous structure
		for(Residue a_oRES : a_uGVAWG.getRootOfFragments()) {
			RootOfFragmentsToWURCSEdge a_oFragToEdge = new RootOfFragmentsToWURCSEdge();
			a_oFragToEdge.start(a_oRES);
			
			// parent side
			Modification a_oMOD = a_oFragToEdge.getModification();
			if(a_oFragToEdge.isAlternative()) {
				if(a_oFragToEdge.getParentEdges().size() > 1)
					throw new WURCSExchangeException("UnderdeterminedSubTree must have only one linkage to parents.");

				ModificationAlternative a_oMODAlt = new ModificationAlternative(a_oMOD.getMAPCode());
				
				for(Residue a_oParent : a_oFragToEdge.getParents()) {
					LinkedList<WURCSEdge> a_aParentEdges = new LinkedList<WURCSEdge>();
					a_aParentEdges.add(a_oFragToEdge.getParentEdges().get(0).copy());
				
					Backbone a_oBackbone = this.a_mResidueIndex.get(a_oParent);
					this.makeLinkage(a_oBackbone, a_aParentEdges, a_oMODAlt);
					
					// add bracket edge
					a_oMODAlt.addLeadInEdge(a_aParentEdges.get(0));
				}
				a_oMOD = a_oMODAlt;
			} else {
				Backbone a_oBackbone = this.a_mResidueIndex.get(a_oFragToEdge.getParent());
				this.makeLinkage(a_oBackbone, a_oFragToEdge.getParentEdges(), a_oMOD);
			}
			
			// child side
			Backbone a_oChild = this.a_mResidueIndex.get(a_oFragToEdge.getChild());
			this.makeLinkage(a_oChild, a_oFragToEdge.getChildEdges(), a_oMOD);
		}
		
		WURCSGraphNormalizer a_oWGNorm = new WURCSGraphNormalizer();
		try {
			a_oWGNorm.start(this.graph);
		} catch (WURCSException e) {
			throw new WURCSExchangeException(e.getErrorMessage());
		}
	}
	
 	private void analyzeResidue (Residue _residue, boolean _isRootFrag) throws Exception {
 		ResidueToBackbone res2back = new ResidueToBackbone();
		
 		if(_isRootFrag)
			res2back.setRootOfFramgents();
		
		res2back.start(_residue);
		Backbone backbone = res2back.getBackbone();
		this.a_mResidueIndex.put(_residue, backbone);
		
		this.graph.addBackbone(backbone);

		//
		for(Modification coreMOD : res2back.getCoreModifications()) {
			WURCSEdge wurcsEdge = new WURCSEdge();
			wurcsEdge.addLinkage(new LinkagePosition(-1, DirectionDescriptor.L, 0));
			if( coreMOD.getMAPCode().lastIndexOf("*") > 0)
				wurcsEdge.addLinkage(new LinkagePosition(-1, DirectionDescriptor.L, 0));
			LinkedList<WURCSEdge> wurcsEdges = new LinkedList<WURCSEdge>();
			wurcsEdges.add(wurcsEdge);
			this.makeLinkage(backbone, wurcsEdges, coreMOD);
		}
	
		if(backbone.getAnomericPosition() == 0) return;
		if (backbone.hasUnknownLength()) return;

		// make ring position
		Modification ring = new Modification("");
		WURCSEdge ringStart = new WURCSEdge();
		WURCSEdge ringEdge = new WURCSEdge();
		if(_residue.getAnomericCarbon() != '?') {
			ringStart.addLinkage(new LinkagePosition(charToInt(_residue.getAnomericCarbon()), DirectionDescriptor.L, 0));
			ringEdge.addLinkage(new LinkagePosition(checkRingPos(_residue), DirectionDescriptor.L, 0));
		}
		
		LinkedList<WURCSEdge> wurcsEdges = new LinkedList<WURCSEdge>();
		wurcsEdges.add(ringStart);
		wurcsEdges.add(ringEdge);
		
		makeLinkage(backbone, wurcsEdges, ring);
	}
 	
 	private void makeLinkage(Backbone a_oBackbone, LinkedList<WURCSEdge> a_aEdges, Modification a_oMod) throws WURCSExchangeException {
 		try {
 			for(WURCSEdge a_oEdge : a_aEdges) {
 				this.graph.addResidues(a_oBackbone, a_oEdge, a_oMod);
 			}
 		} catch (WURCSException e) {
 			throw new WURCSExchangeException(e.getErrorMessage());
 		}
 	}
 	
 	private int charToInt(char _target) {
 		if(_target == '?') return -1;
 		return Integer.parseInt(String.valueOf(_target));
 	}
 	
 	private int checkRingPos (Residue _residue) {
 		char ringSize = _residue.getRingSize();
 		char anomPos = _residue.getAnomericCarbon();
 		
 		if(anomPos == '1') {
 			if(ringSize == 'f') return 4;
 			if(ringSize == 'p') return 5;
 			if(ringSize == '?') return -1;
 		}
 		if(anomPos == '2') {
 			if(ringSize == 'f') return 5;
 			if(ringSize == 'p') return 6;
 			if(ringSize == '?') return -1;
 		}
 		if(anomPos == '3') {
 			if(ringSize == '?') return -1;
 		}
 		
 		return -1;
 	}
}
