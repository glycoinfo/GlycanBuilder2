package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;

public class LinkageConnector {

	Residue a_oCurrentRES;
	Residue a_oParentRES;
	Residue a_oStartRES;
	
	/**
	 * 
	 * @param _a_oCurrent
	 * @param _a_oParent
	 * @param _a_oStart
	 */
	public LinkageConnector(Residue _a_oCurrent, Residue _a_oParent, Residue _a_oStart) {
		this.a_oCurrentRES = _a_oCurrent;
		this.a_oParentRES = _a_oParent;
		this.a_oStartRES = _a_oStart;
	}
	
	/**
	 * 
	 * @param a_oG2L
	 */
	public void start(GLINToLinkage a_oG2L) {
		Residue a_oRES = this.a_oCurrentRES;
		Residue a_oSUB = null;
		
		/** set parent linkage for current residue*/
		if(!a_oG2L.getParentLinkage().isEmpty()) {
			if(a_oG2L.getBridgeLinkage() != null) {
				a_oRES.setParentLinkage(a_oG2L.getBridgeLinkage());
				a_oSUB = a_oG2L.getBridgeLinkage().getSubstituent();
				a_oSUB.setParentLinkage(a_oG2L.getParentLinkage().get(0));
			}else {		
				a_oRES.setParentLinkage(a_oG2L.getParentLinkage().getLast());
			}
		}else if(a_oG2L.getStartCyclicLinkage() != null) {
			a_oRES.setParentLinkage(a_oG2L.getStartCyclicLinkage());
		}else if(a_oG2L.getParentRepeatingLinkage() != null) {
			a_oRES.setParentLinkage(a_oG2L.getParentRepeatingLinkage());
		}
		
		if(a_oG2L.getDonorGLINs().isEmpty()) return;
		if(a_oG2L.getParents().size() > 1) return;
		
		if(a_oG2L.isCyclic() == false && a_oG2L.isRepeating() == false) {
			/** add child edge between child and parent residues */				
			Residue a_oParent = this.a_oParentRES;
			if(this.isOutRepeating(a_oRES, a_oParent)) {
				/** make glycosidic bond out of repating : o-]<->o*/
				/*if(a_oSUB != null) {
					a_oParent.getEndRepitionResidue().addChild(a_oSUB, a_oSUB.getParentLinkage().getBonds());
					a_oSUB.addChild(a_oRES, a_oRES.getParentLinkage().getBonds());
				} else*/ 
					a_oParent.getEndRepitionResidue().addChild(a_oRES, a_oRES.getParentLinkage().getBonds());
			} else {
				if(a_oSUB != null) {
					/** make glycosidic bond with substituent : s<->o*/
					a_oParent.addChild(a_oSUB, a_oSUB.getParentLinkage().getBonds());
					
					/** make glicosidic bond with monosaccharide : o<->s*/
					a_oSUB.addChild(a_oRES, a_oRES.getParentLinkage().getBonds());	
				}else {
					/** make glycosidic bond : o<->o*/
					a_oParent.addChild(a_oRES, a_oRES.getParentLinkage().getBonds());
				}
			}
		}else {
			this.analyzeBracketNotation(a_oRES, a_oSUB, a_oG2L);
		}
		
		return;
	}
	
	private void analyzeBracketNotation (Residue a_oRES, Residue a_oBridge, GLINToLinkage a_oG2L) {
		/** define end repeating bracket */
		if(a_oG2L.getChildRepeatingLinkage() != null) {
			this.makeEdgeWithEndBracket(a_oG2L, a_oRES);
		}

		/** define end cyclic bracket */
		if(a_oG2L.getEndCyclicLinkage() != null) {
			this.makeEdgeWithEndCyclic(a_oG2L, a_oRES);
		}

		/** define start repeating bracket */
		if(a_oG2L.getParentRepeatingLinkage() != null) {
			this.makeEdgeWithStartBracket(a_oG2L, a_oRES);
		}
		
		/** define start cyclic bracket */ 
		if(a_oG2L.getStartCyclicLinkage() != null) {
			this.makeEdgeWithStartCyclic(a_oG2L, a_oRES);
		}
		
		/** define an edge for glycan structure */
		if(a_oRES.getStartRepetitionResidue() != null && a_oRES.getStartCyclicResidue() == null && a_oG2L.getDonorGLINs().size() > 1) {
			Residue a_oParent = this.a_oParentRES;
			Residue a_oStartRep = a_oRES.getStartRepetitionResidue();
			if(this.isOutRepeating(a_oStartRep, a_oParent)) {
				/** end repeat to start repeat : o-]<->[-o */
				a_oParent.getEndRepitionResidue().addChild(a_oStartRep, a_oStartRep.getParentLinkage().getBonds());
			} else {
				/** start repeat to monosaccharide : o-]<->o */
				a_oParent.addChild(a_oStartRep, a_oStartRep.getParentLinkage().getBonds());
			}
		}else if(a_oRES.getStartCyclicResidue() == null && !a_oG2L.getParents().isEmpty()){
			Residue a_oParent = this.a_oParentRES;
			if(this.isOutRepeating(a_oRES, a_oParent)) {
				/** end repeating bracket to monosaccharide : o<->[-o */
				a_oParent.getEndRepitionResidue().addChild(a_oRES, a_oRES.getParentLinkage().getBonds());
			} else {
				if(a_oBridge == null) {
					/** monosaccharide(in rep) to monosacchadie(in rep) : 
					 * [
					 *   \
					 *    >o-
					 *   /
					 *  o 
					 *  
					 *  some monosaccharide have end repeating bracket
					 *  [-o<->o-
					 * */
					if(!a_oRES.equals(a_oParent)) a_oParent.addChild(a_oRES, a_oRES.getParentLinkage().getBonds());
				} else {
					/**  
					 * end repeating node make bridge for other monosaccharide in repeating
					 * [-o<->bridge<->o-
					 * */
					a_oParent.addChild(a_oBridge, a_oBridge.getParentLinkage().getBonds());
					a_oBridge.addChild(a_oRES, a_oRES.getParentLinkage().getBonds());
				}
			}
		}	
		
		return;
	}
		
	/**
	 * 
	 * @param a_oG2L
	 * @param a_oRES
	 */
	private void makeEdgeWithStartBracket(GLINToLinkage a_oG2L, Residue a_oRES) {
		Residue a_oStartRep = ResidueDictionary.createStartRepetition();
		a_oStartRep.setParentLinkage(a_oG2L.getParentRepeatingLinkage());
		a_oRES.setStartRepetiionResidue(a_oStartRep);
		/** start repeating bracket to residue : o<->]*/
		a_oStartRep.addChild(a_oRES/*, a_oRES.getParentLinkage().getBonds()*/);
		a_oStartRep.getParentLinkage().setLinkagePositions(a_oStartRep.getParentLinkage().getBonds());
	
		return;
	}
	
	/**
	 * 
	 * @param a_oG2L
	 * @param a_oRES
	 */
	private void makeEdgeWithEndBracket(GLINToLinkage a_oG2L, Residue a_oRES) {
		Residue a_oEndRep = ResidueDictionary.createEndRepetition(
				a_oG2L.getMinRepeatingCount(), a_oG2L.getMaxRepeatingCount());
		Residue a_oSUB = null;
		
		/** set parameter */
		a_oEndRep.setParentLinkage(a_oG2L.getChildRepeatingLinkage());
		a_oEndRep.setStartResidue(this.a_oStartRES);
		a_oRES.setEndRepitionResidue(a_oEndRep);
		
		/** [<->s<->o */
		if(a_oG2L.getChildRepeatingLinkage().getSubstituent() != null) {
			a_oSUB = a_oG2L.getChildRepeatingLinkage().getSubstituent();
			
			/** sugar to substituent : s<->o*/
			a_oSUB.setParentLinkage(a_oG2L.getParentLinkage().get(0));
			a_oRES.addChild(a_oSUB, a_oSUB.getParentLinkage().getBonds());
			
			/** substituent to end repeating : [<->s*/		
			a_oSUB.addChild(a_oEndRep, a_oEndRep.getParentLinkage().getBonds());
		} else {
			/** end repeating to residue : [<->o */
			a_oRES.addChild(a_oEndRep, a_oEndRep.getParentLinkage().getBonds());
		}			
	
		return;
	}
	
	/**
	 * make edge between start cyclic and residue or start repeating bracket 
	 * start cyclic to residue : o<->(
	 * start cyclic to start repeating bracket : o-]<->(
	 * @param a_oG2L
	 * @param a_oRES
	 */
	private void makeEdgeWithStartCyclic(GLINToLinkage a_oG2L, Residue a_oRES) {
		Residue a_oStartCyclic = ResidueDictionary.createStartCyclic();
		a_oRES.setStartCyclicResidue(a_oStartCyclic);
		
		if(a_oRES.getStartRepetitionResidue() != null) {
			a_oStartCyclic.addChild(a_oRES.getStartRepetitionResidue(), a_oRES.getStartRepetitionResidue().getParentLinkage().getBonds());
		}else {
			a_oStartCyclic.addChild(a_oRES, a_oRES.getParentLinkage().getBonds());
		}	
		
		return;
	}
	
	/**
	 * make edge to end cyclic bracket
	 * residue to end cyclic bracket : )<->o
	 * end repeating to end cyclic bracket : )<->[-o
	 * @param a_oG2L
	 * @param a_oRES
	 */
	private void makeEdgeWithEndCyclic(GLINToLinkage a_oG2L, Residue a_oRES) {
		Residue a_oEndCyclic = ResidueDictionary.createEndCyclic();
		a_oEndCyclic.setParentLinkage(a_oG2L.getEndCyclicLinkage());
		a_oRES.setEndCyclicResidue(a_oEndCyclic);
	
		if(a_oRES.getEndRepitionResidue() != null) {
			a_oRES.getEndRepitionResidue().addChild(a_oEndCyclic, a_oEndCyclic.getParentLinkage().getBonds());
		}else {	
			a_oRES.addChild(a_oEndCyclic, a_oEndCyclic.getParentLinkage().getBonds());
		}	
		
		return;
	}
	
	/**
	 * 
	 * @param a_oChild
	 * @param a_oParent
	 * @return
	 */
	private boolean isOutRepeating(Residue a_oChild, Residue a_oParent) {
		if(a_oParent.getEndRepitionResidue() == null) return false;
				
		String a_sChildPos = a_oChild.getParentLinkage().getParentPositionsString();
		String a_sEndRepPos = a_oParent.getEndRepitionResidue().getParentLinkage().getParentPositionsString();
		
		if(a_sChildPos.contains("/") && a_sEndRepPos.contains("/")) {
			if(a_sChildPos.equals(a_sEndRepPos)) return true;
			a_sChildPos = a_sChildPos.substring(0, 1);
			a_sEndRepPos = a_sEndRepPos.substring(0, 1);
		}
		
		int a_iChildDonorPos = a_sChildPos.equals("?") ? -1 : Integer.parseInt(a_sChildPos);
		int a_iEndRepDonorPos = a_sEndRepPos.equals("?") ? -1 : Integer.parseInt(a_sEndRepPos);

		if(a_iChildDonorPos == a_iEndRepDonorPos) return true;
		
		return false;
	}
}
