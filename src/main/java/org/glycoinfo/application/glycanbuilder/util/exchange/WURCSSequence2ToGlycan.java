package org.glycoinfo.application.glycanbuilder.util.exchange;

import java.util.HashMap;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.wurcs.array.LIN;
import org.glycoinfo.WURCSFramework.wurcs.array.WURCSArray;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GLIN;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.WURCSSequence2;

public class WURCSSequence2ToGlycan {

	private Glycan a_oGlycan;
	private HashMap<GRES, Residue> a_mGRESToResidue;
	
	private GRESToFragment a_oGRESToFragment;
	
	public Glycan getGlycan() {
		return this.a_oGlycan;
	}
	
	public void start(WURCSFactory a_oWF, MassOptions a_oMO) throws Exception {
		this.init();
		
		WURCSSequence2 a_oWS2 = a_oWF.getSequence();
		WURCSArray a_oWA = a_oWF.getArray();
		
		/** convert GRES to Residue*/
		for(GRES a_oGRES : a_oWS2.getGRESs()) {
			this.analyzeGRES(a_oGRES);
			if(a_oWS2.getGRESs().size() > 1) 
				a_oGRESToFragment.start(a_oGRES, this.a_mGRESToResidue.get(a_oGRES));
		}
				
		for(GRES a_oGRES : a_oWS2.getGRESs()) {
			this.analyzeGLIN(a_oGRES, a_oWA.getLINs());			
		}
		
		this.a_oGlycan = new Glycan(makeRoot(a_oWS2.getGRESs()), false, a_oMO);
		
		/** append ambiguous root*/
		for(GRES a_oGRES : a_oGRESToFragment.getRootOfFragments()) {
			Residue a_oRootOfFragment = this.a_mGRESToResidue.get(a_oGRES);
			this.a_oGlycan.addAntenna(a_oRootOfFragment, a_oRootOfFragment.getParentLinkage().getBonds());
		}
		
		/** append ambiguous substituent */
		for(GLIN a_oGLIN : a_oGRESToFragment.getSubstituentWithFragments()) {
			Residue a_oRootOfFragment = a_oGRESToFragment.getSubStituentFragment(a_oGLIN);
			for(GRES a_oGRES : a_oGRESToFragment.getChildren()) {
				a_oRootOfFragment.addParentOfFragment(this.a_mGRESToResidue.get(a_oGRES));
			}
			this.a_oGlycan.addAntenna(a_oRootOfFragment, a_oRootOfFragment.getParentLinkage().getBonds());
		}
		
		/** append composition */
		if(!a_oGRESToFragment.getRootOfCompositions().isEmpty()) {
			this.a_oGlycan = Glycan.createComposition(a_oMO);
			for(GRES a_oGRES : a_oGRESToFragment.getRootOfCompositions()) {
				Residue a_oRootOfComposition = this.a_mGRESToResidue.get(a_oGRES);
				a_oRootOfComposition.isComposition(true);
				this.a_oGlycan.addAntenna(a_oRootOfComposition);
			}			
		}
		
		return;
	}

	private void analyzeGRES(GRES a_oGRES) throws Exception {
		GRESToResidue a_G2R = new GRESToResidue();
		
		a_G2R.start(a_oGRES);	
		Residue a_oRES = a_G2R.getResidue();
		this.a_mGRESToResidue.put(a_oGRES, a_oRES);
		
		/** add substituent as child residue */
		SUBSTAnalyzer a_oSUBSTToResidue = new SUBSTAnalyzer(a_G2R.getModifications());
		a_oSUBSTToResidue.start(a_oGRES, a_oRES);
		
		return;
	}
	
	private void analyzeGLIN(GRES a_oGRES, LinkedList<LIN> a_aWLINs) throws Exception {
		GLINToLinkage a_oG2L = new GLINToLinkage(this.a_mGRESToResidue.get(a_oGRES), a_aWLINs);
		a_oG2L.start(a_oGRES);
		
		/** define glycosidic bond*/
		Residue a_oCurrent = this.a_mGRESToResidue.get(a_oGRES);
		Residue a_oParent = a_oG2L.getParents().size() > 0 ? 
				this.a_mGRESToResidue.get(a_oG2L.getParents().get(0)) : null;
		Residue a_oStart = this.a_mGRESToResidue.get(a_oG2L.getStartRepeatingGRES());

		LinkageConnector a_oLinkageConnector = 
				new LinkageConnector(a_oCurrent, a_oParent, a_oStart);
		
		a_oLinkageConnector.start(a_oG2L);

		/** set parents for fragments */
		if(a_oG2L.getParents().size() > 1) {
			for(GRES a_oParentG : a_oG2L.getParents()) {
				this.a_mGRESToResidue.get(a_oGRES).addParentOfFragment(this.a_mGRESToResidue.get(a_oParentG));
			}
		}
		
		return;
	}
	
	private Residue makeRoot(LinkedList<GRES> a_oGRESs) throws Exception {
		/** check type of reducing end */
		Residue a_oRoot = this.a_mGRESToResidue.get(getRootResidue(a_oGRESs));
		Residue a_oRedEnd = null;
		
		if(a_oRoot == null) return a_oRedEnd;
	
		
		a_oRedEnd = a_oRoot.isAlditol() && a_oRoot.getStartRepetitionResidue() == null ?
				ResidueDictionary.newResidue("redEnd") : ResidueDictionary.newResidue("freeEnd");
		
		if(a_oRoot.getStartRepetitionResidue() != null) {			
			a_oRedEnd.addChild(a_oRoot.getStartRepetitionResidue(), a_oRoot.getStartRepetitionResidue().getParentLinkage().getBonds());
		}else if (a_oRoot.getStartCyclicResidue() != null){
			a_oRedEnd.addChild(a_oRoot.getStartCyclicResidue(), '?');
		}else {
			Linkage a_oLIN = new Linkage(a_oRedEnd, a_oRoot);
			a_oLIN.setLinkagePositions(new char[] {a_oRoot.getAnomericCarbon()});
			a_oRoot.setParentLinkage(a_oLIN);
			a_oRedEnd.addChild(a_oRoot, a_oRoot.getParentLinkage().getBonds());			
		}
		
		return a_oRedEnd;
	}
	
	private GRES getRootResidue(LinkedList<GRES> a_oGRESs) {
		for(GRES a_oGRES : a_oGRESs) {
			if(this.a_oGRESToFragment.getRootOfCompositions().contains(a_oGRES)) continue;
			
			if(a_oGRES.getDonorGLINs().isEmpty() && a_oGRES.getAcceptorGLINs().isEmpty())
				return a_oGRES;
			if(a_oGRES.getDonorGLINs().isEmpty())
				return a_oGRES;
			if(a_oGRES.getAcceptorGLINs().isEmpty()) continue;

			for(GLIN a_Donor : a_oGRES.getDonorGLINs()) {
				if(a_oGRES.getDonorGLINs().size() == 1 && a_Donor.isRepeat()) return a_oGRES;
				if(a_Donor.isRepeat()) continue;
				GLIN a_oAcceptor = a_oGRES.getAcceptorGLINs().get(0);

				if(a_oAcceptor.getDonor().isEmpty()) continue;
				if(!new GLINToLinkage().isFacingBetweenAnomer(a_Donor)) {
					if(a_oAcceptor.getAcceptor().get(0).getID() == 1 && a_Donor.getDonor().get(0).getID() == 1)
						return a_oGRES;
					if(a_oGRES.getID() > a_Donor.getAcceptor().get(0).getID()) {
						return a_oGRES;
					}
				}
			}			
		}
		
		return null;
	}	
	
	private void init() {
		this.a_mGRESToResidue = new HashMap<GRES, Residue>();
		this.a_oGlycan = null;
		this.a_oGRESToFragment = new GRESToFragment();
	}
}
