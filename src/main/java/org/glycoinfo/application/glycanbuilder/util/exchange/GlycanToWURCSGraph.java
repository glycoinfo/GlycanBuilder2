package org.glycoinfo.application.glycanbuilder.util.exchange;

import java.util.HashMap;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glycoinfo.WURCSFramework.util.exchange.WURCSExchangeException;
import org.glycoinfo.WURCSFramework.util.graph.WURCSGraphNormalizer;
import org.glycoinfo.WURCSFramework.wurcs.graph.Backbone;
import org.glycoinfo.WURCSFramework.wurcs.graph.BackboneUnknown;
import org.glycoinfo.WURCSFramework.wurcs.graph.DirectionDescriptor;
import org.glycoinfo.WURCSFramework.wurcs.graph.LinkagePosition;
import org.glycoinfo.WURCSFramework.wurcs.graph.Modification;
import org.glycoinfo.WURCSFramework.wurcs.graph.ModificationAlternative;
import org.glycoinfo.WURCSFramework.wurcs.graph.ModificationRepeat;
import org.glycoinfo.WURCSFramework.wurcs.graph.WURCSEdge;
import org.glycoinfo.WURCSFramework.wurcs.graph.WURCSGraph;
import org.glycoinfo.application.glycanbuilder.util.GlycanUtils;

public class GlycanToWURCSGraph {
	
	private WURCSGraph a_oGraph;
	private HashMap<Residue, Backbone> a_mResidueIndex = new HashMap<Residue, Backbone>();
	
	public WURCSGraph getGraph() {
		return this.a_oGraph;
	}
	
	public void start(Glycan a_oGlycan) throws Exception {
		GlycanVisitorAnalyzeForWURCSGraph a_uGVAWG = new GlycanVisitorAnalyzeForWURCSGraph();
		
		a_uGVAWG.start(a_oGlycan);
		this.a_oGraph = new WURCSGraph();
		
		/** Residue to */
		for(Residue a_oRES : a_uGVAWG.getResidues()) {
			if(GlycanUtils.isCollisionLinkagePosition(a_oRES))
				throw new Exception("This glycan have illegal linkage posiiton");
			boolean a_bIsRootOfFragment = (a_oRES.hasParent() && a_oRES.getParent().isBracket());
			this.analyzeResidue(a_oRES, a_bIsRootOfFragment);
		}
		
		/** Linkage to WURCSEdge*/
		for(Linkage a_oLIN : a_uGVAWG.getLinkages()) {	
			LinkageToWURCSEdge a_oL2WE = new LinkageToWURCSEdge();
			a_oL2WE.start(a_oLIN);

			if(a_oL2WE.getParent().isBracket()) continue;
						
			Modification a_oMOD = a_oL2WE.getModification();
			
			/** analyze for repeating */
			if(a_uGVAWG.getRepeatingResidueByLinkage(a_oLIN) != null) {
				ModificationRepeat a_oRepMOD = new ModificationRepeat(a_oMOD.getMAPCode());
				Residue a_oRepRES = a_uGVAWG.getRepeatingResidueByLinkage(a_oLIN);
				a_oRepMOD.setMaxRepeatCount(a_oRepRES.getMaxRepetitions());
				a_oRepMOD.setMinRepeatCount(a_oRepRES.getMinRepetitions());
				a_oMOD = a_oRepMOD;
			}
			
			/** parent side */
			if(!a_oL2WE.getParent().isReducingEnd() && !a_oL2WE.getParent().isRepetition() && !a_oL2WE.getParent().isStartCyclic()) {
				Backbone a_oParent = this.a_mResidueIndex.get(a_oL2WE.getParent());
				this.makeLinkage(a_oParent, a_oL2WE.getParentEdges(), a_oMOD);
			}
			
			if(a_oL2WE.getChild() == null || a_oL2WE.getChild().isRepetition()) continue;
			/** child side */
			Backbone a_oChild = this.a_mResidueIndex.get(a_oL2WE.getChild());
			this.makeLinkage(a_oChild, a_oL2WE.getChildEdges(), a_oMOD);
		}
		
		/** Ambiguous structure */
		for(Residue a_oRES : a_uGVAWG.getRootOfFragments()) {
			RootOfFragmentsToWURCSEdge a_oFragToEdge = new RootOfFragmentsToWURCSEdge();
			a_oFragToEdge.start(a_oRES);
			
			/** parent side */
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
					
					/** add bracket edge */
					a_oMODAlt.addLeadInEdge(a_aParentEdges.get(0));
				}
				a_oMOD = a_oMODAlt;
			} else {
				Backbone a_oBackbone = this.a_mResidueIndex.get(a_oFragToEdge.getParent());
				this.makeLinkage(a_oBackbone, a_oFragToEdge.getParentEdges(), a_oMOD);
			}
			
			/** child side */
			Backbone a_oChild = this.a_mResidueIndex.get(a_oFragToEdge.getChild());
			this.makeLinkage(a_oChild, a_oFragToEdge.getChildEdges(), a_oMOD);
		}
		
		WURCSGraphNormalizer a_oWGNorm = new WURCSGraphNormalizer();
		try {
			a_oWGNorm.start(this.a_oGraph);
		} catch (WURCSException e) {
			throw new WURCSExchangeException(e.getErrorMessage());
		}
	}
	
 	private void analyzeResidue (Residue a_oRES, boolean a_bIsRootOfFragment) throws Exception {
 		ResidueToBackbone a_oR2B = new ResidueToBackbone();
		
 		if(a_bIsRootOfFragment)
			a_oR2B.setRootOfFramgents();
		
		a_oR2B.start(a_oRES);
		Backbone a_oBackbone = a_oR2B.getBackbone();
		this.a_mResidueIndex.put(a_oRES, a_oBackbone);
		
		this.a_oGraph.addBackbone(a_oBackbone);

		/***/
		for(Modification a_oCoreMOD : a_oR2B.getCoreModifications()) {
			WURCSEdge a_oEdge = new WURCSEdge();
			a_oEdge.addLinkage(new LinkagePosition(-1, DirectionDescriptor._, 0));
			if( a_oCoreMOD.getMAPCode().lastIndexOf("*") > 0)
				a_oEdge.addLinkage(new LinkagePosition(-1, DirectionDescriptor._, 0));
			LinkedList<WURCSEdge> a_aCoreEdges = new LinkedList<WURCSEdge>();
			a_aCoreEdges.add(a_oEdge);
			this.makeLinkage(a_oBackbone, a_aCoreEdges, a_oCoreMOD);
		}
	
		if(a_oBackbone.getAnomericPosition() == 0) return;
		if(a_oBackbone instanceof BackboneUnknown) return;
		
		/** make ring position*/
		Modification a_oRing = new Modification("");

		WURCSEdge a_oStartEdge = new WURCSEdge();
		WURCSEdge a_oEndEdge = new WURCSEdge();
		if(a_oRES.getAnomericCarbon() != '?') {
			a_oStartEdge.addLinkage(new LinkagePosition(charToInt(a_oRES.getAnomericCarbon()), DirectionDescriptor._, 0));
			a_oEndEdge.addLinkage(new LinkagePosition(checkRingPos(a_oRES), DirectionDescriptor._, 0));
		}
		
		LinkedList<WURCSEdge> a_aEdges = new LinkedList<WURCSEdge>();
		a_aEdges.add(a_oStartEdge);
		a_aEdges.add(a_oEndEdge);
		
		makeLinkage(a_oBackbone, a_aEdges, a_oRing);		
	}
 	
 	private void makeLinkage(Backbone a_oBackbone, LinkedList<WURCSEdge> a_aEdges, Modification a_oMod) throws WURCSExchangeException {
 		try {
 			for(WURCSEdge a_oEdge : a_aEdges) {
 				this.a_oGraph.addResidues(a_oBackbone, a_oEdge, a_oMod);
 			}
 		} catch (WURCSException e) {
 			throw new WURCSExchangeException(e.getErrorMessage());
 		}
 	}
 	
 	private int charToInt(char a_cIndex) {
 		if(a_cIndex == '?') return -1;
 		return Integer.parseInt(String.valueOf(a_cIndex));
 	}
 	
 	private int checkRingPos (Residue a_oRES) {
 		char a_cRingSize = a_oRES.getRingSize();
 		char a_cAnomPos = a_oRES.getAnomericCarbon();
 		
 		if(a_cAnomPos == '1') {
 			if(a_cRingSize == 'f') return 4;
 			if(a_cRingSize == 'p') return 5;
 			if(a_cRingSize == '?') return -1;
 		}
 		if(a_cAnomPos == '2') {
 			if(a_cRingSize == 'f') return 5;
 			if(a_cRingSize == 'p') return 6;
 			if(a_cRingSize == '?') return -1;
 		}
 		if(a_cAnomPos == '3') {
 			if(a_cRingSize == '?') return -1;
 		}
 		
 		return -1;
 	}
}
