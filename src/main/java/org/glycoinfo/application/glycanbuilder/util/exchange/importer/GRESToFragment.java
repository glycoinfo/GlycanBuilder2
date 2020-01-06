package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GLIN;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;

public class GRESToFragment extends GLINToLinkage {

	private GRES a_oGRES;

	/** Monosaccharide */
	private ArrayList<GRES> a_aAcceptorGRESs = new ArrayList<GRES>();
	private ArrayList<GRES> a_aRootOfFragments = new ArrayList<GRES>();
	private ArrayList<GRES> a_aRootOfCompositions = new ArrayList<GRES>();
	
	/** Substituent */
	private LinkedList<GRES> a_aDonorGRESs = new LinkedList<GRES>();
	private ArrayList<GLIN> a_aSubFragments = new ArrayList<GLIN>();
	private HashMap<GLIN, Residue> a_mGRESToSubFrag = new HashMap<GLIN, Residue>();;
		
	public GRES getGRES() {
		return this.a_oGRES;
	}
	
	public LinkedList<GRES> getChildren() {
		return this.a_aDonorGRESs;
	}
	
	public ArrayList<GLIN> getSubstituentWithFragments() {
		return this.a_aSubFragments;
	}
	
	public ArrayList<GRES> getRootOfFragments() {
		return this.a_aRootOfFragments;
	}
	
	public ArrayList<GRES> getRootOfCompositions() {
		return this.a_aRootOfCompositions;
	}
	
	public Residue getSubStituentFragment(GLIN a_oGLIN) {
		return this.a_mGRESToSubFrag.get(a_oGLIN);
	}
	
	public ArrayList<GRES> getParents() {
		return this.a_aAcceptorGRESs;
	}
	
	public void start(GRES a_oGRES, Residue a_oRES) throws Exception {
		this.init();
		this.setGRESs(a_oGRES);
		
		int a_iAnomericPosition = a_oGRES.getMS().getCoreStructure().getAnomericPosition();
		char a_cAnomericState = a_oGRES.getMS().getCoreStructure().getAnomericSymbol();
		boolean a_bIsUnknownAnomer = (a_iAnomericPosition == 0 && a_cAnomericState == 'o') || 
				((a_iAnomericPosition == 1 || a_iAnomericPosition == 2) && a_cAnomericState == 'x');

		/***/
		for(GLIN a_oAGLIN : a_oGRES.getAcceptorGLINs()) {
			this.analyzeAcceptorGLIN(a_oAGLIN);
		}
		
		/** set root of fragments */
		if(this.getParents().size() > 1 && !getParents().contains(a_oGRES)) {
			this.a_aRootOfFragments.add(a_oGRES);
		}
		
		/** for ambiguous structure(Substituent)*/
		if(!this.a_aSubFragments.isEmpty()) {
			SUBSTAnalyzer a_oSUBSTToResidue = new SUBSTAnalyzer();
			for(GLIN a_oGLIN : this.a_aSubFragments) {
				Residue a_oSubRES = a_oSUBSTToResidue.MAPToFragment(a_oGLIN);
				if(this.a_mGRESToSubFrag.containsKey(a_oGLIN)) break;	
				Linkage a_oLIN = 
						new Linkage(null, a_oSubRES, this.makeLinkagePosiiton(a_oGLIN.getAcceptorPositions()));
				a_oSubRES.setParentLinkage(a_oLIN);
				this.a_mGRESToSubFrag.put(a_oGLIN, a_oSubRES);
			}	
		}
		
		/** for composition */
		if(a_oGRES.getAcceptorGLINs().isEmpty() && a_oGRES.getDonorGLINs().isEmpty() && a_bIsUnknownAnomer) {
			this.a_aRootOfCompositions.add(a_oGRES);
		}
		
		/** composition Lv2, 3, 4 */
		if(getParents().contains(a_oGRES)) this.a_aRootOfCompositions.add(a_oGRES);
		
		return;
	}
	
	private void analyzeAcceptorGLIN(GLIN a_oAGLIN) {
		if(!a_oAGLIN.getMAP().equals("") && a_oAGLIN.getAcceptor().size() > 1) {
			if(!this.a_aSubFragments.contains(a_oAGLIN))
				this.a_aSubFragments.add(a_oAGLIN);
			for(GRES a_oAGRES : a_oAGLIN.getAcceptor()) {
				if(!this.a_aDonorGRESs.contains(a_oAGRES))
					this.a_aDonorGRESs.add(a_oAGRES);
			}
		}
		
		return;
	}
	
	public void setGRESs(GRES a_oGRES) {
		this.a_oGRES = a_oGRES;
		
		if(!a_oGRES.getAcceptorGLINs().isEmpty()) {
			LinkedList<GLIN> a_aGLINs = a_oGRES.getAcceptorGLINs();
			if(a_aGLINs.getFirst().getDonor().contains(a_oGRES)) {
				this.a_aAcceptorGRESs.addAll(a_aGLINs.getFirst().getDonor());
			}else {
				for(GRES a_oDGRES : a_oGRES.getAcceptorGLINs().getFirst().getDonor()) {
					if(a_oDGRES.getID() - a_oGRES.getID() == 1) continue;
					this.a_aAcceptorGRESs.add(a_oDGRES);
				}				
			}
		}		
			
		for(GLIN a_oDGLIN : a_oGRES.getDonorGLINs()) {
			if(a_oDGLIN.isRepeat() || this.isFacingBetweenAnomer(a_oDGLIN)) continue;
			if(a_oDGLIN.getAcceptor().size() < 2) continue;
			else {
				for(GRES a_oAcceptor : a_oDGLIN.getAcceptor())
					if(!this.a_aAcceptorGRESs.contains(a_oAcceptor)) this.a_aAcceptorGRESs.add(a_oAcceptor);
			}
		}
		
	}
	
	private void init() {
		this.a_aAcceptorGRESs = new ArrayList<GRES>();
	}
}
