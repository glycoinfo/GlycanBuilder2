package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GLIN;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;

public class GRESToFragment extends GLINToLinkage {

	private GRES gres;

	// Monosaccharide
	private ArrayList<GRES> acceptorGRESs = new ArrayList<>();
	private final ArrayList<GRES> fragmentsRoot = new ArrayList<>();
	private final ArrayList<GRES> compositionsRoot = new ArrayList<>();
	
	// Substituent
	private final LinkedList<GRES> donorGRESs = new LinkedList<>();
	private final ArrayList<GLIN> fragmentsSubstituent = new ArrayList<>();
	private final HashMap<GLIN, Residue> gres2subfrag = new HashMap<>();

	public GRES getGRES() {
		return this.gres;
	}
	
	public LinkedList<GRES> getChildren() {
		return this.donorGRESs;
	}
	
	public ArrayList<GLIN> getSubstituentWithFragments() {
		return this.fragmentsSubstituent;
	}
	
	public ArrayList<GRES> getRootOfFragments() {
		return this.fragmentsRoot;
	}
	
	public ArrayList<GRES> getRootOfCompositions() {
		return this.compositionsRoot;
	}
	
	public Residue getSubStituentFragment(GLIN a_oGLIN) {
		return this.gres2subfrag.get(a_oGLIN);
	}
	
	public ArrayList<GRES> getParents() {
		return this.acceptorGRESs;
	}

	public void start(GRES _gres) throws Exception {
		this.init();
		this.setGRESs(_gres);
		
		int a_iAnomericPosition = _gres.getMS().getCoreStructure().getAnomericPosition();
		char a_cAnomericState = _gres.getMS().getCoreStructure().getAnomericSymbol();
		boolean a_bIsUnknownAnomer = (a_iAnomericPosition == 0 && a_cAnomericState == 'o') || 
				((a_iAnomericPosition == 1 || a_iAnomericPosition == 2) && a_cAnomericState == 'x');

		//
		for(GLIN a_oAGLIN : _gres.getAcceptorGLINs()) {
			this.analyzeAcceptorGLIN(a_oAGLIN);
		}
		
		// set root of fragments
		if(this.getParents().size() > 1 && !getParents().contains(_gres)) {
			this.fragmentsRoot.add(_gres);
		}
		
		// for ambiguous structure(Substituent)
		if(!this.fragmentsSubstituent.isEmpty()) {
			SUBSTAnalyzer a_oSUBSTToResidue = new SUBSTAnalyzer();
			for(GLIN a_oGLIN : this.fragmentsSubstituent) {
				Residue a_oSubRES = a_oSUBSTToResidue.MAPToFragment(a_oGLIN);
				if(this.gres2subfrag.containsKey(a_oGLIN)) break;
				Linkage a_oLIN = 
						new Linkage(null, a_oSubRES, this.makeLinkagePosiiton(a_oGLIN.getAcceptorPositions()));
				a_oSubRES.setParentLinkage(a_oLIN);
				this.gres2subfrag.put(a_oGLIN, a_oSubRES);
			}	
		}
		
		// for composition
		if(_gres.getAcceptorGLINs().isEmpty() && _gres.getDonorGLINs().isEmpty() && a_bIsUnknownAnomer) {
			this.compositionsRoot.add(_gres);
		}

		// composition Lv2, 3, 4
		if(getParents().contains(_gres)) this.compositionsRoot.add(_gres);
	}
	
	private void analyzeAcceptorGLIN(GLIN a_oAGLIN) {
		if(!a_oAGLIN.getMAP().equals("") && a_oAGLIN.getAcceptor().size() > 1) {
			if(!this.fragmentsSubstituent.contains(a_oAGLIN))
				this.fragmentsSubstituent.add(a_oAGLIN);
			for(GRES a_oAGRES : a_oAGLIN.getAcceptor()) {
				if(!this.donorGRESs.contains(a_oAGRES))
					this.donorGRESs.add(a_oAGRES);
			}
		}
	}
	
	public void setGRESs(GRES _gres) {
		this.gres = _gres;
		
		if(!_gres.getAcceptorGLINs().isEmpty()) {
			LinkedList<GLIN> a_aGLINs = _gres.getAcceptorGLINs();
			if(a_aGLINs.getFirst().getDonor().contains(_gres)) {
				this.acceptorGRESs.addAll(a_aGLINs.getFirst().getDonor());
			}else {
				for(GRES a_oDGRES : _gres.getAcceptorGLINs().getFirst().getDonor()) {
					if(a_oDGRES.getID() - _gres.getID() == 1) continue;
					this.acceptorGRESs.add(a_oDGRES);
				}				
			}
		}		
			
		for(GLIN a_oDGLIN : _gres.getDonorGLINs()) {
			if(a_oDGLIN.isRepeat() || this.isFacingBetweenAnomer(a_oDGLIN)) continue;
			if(a_oDGLIN.getAcceptor().size() < 2) continue;
			else {
				for(GRES a_oAcceptor : a_oDGLIN.getAcceptor())
					if(!this.acceptorGRESs.contains(a_oAcceptor)) this.acceptorGRESs.add(a_oAcceptor);
			}
		}
		
	}
	
	private void init() {
		this.acceptorGRESs = new ArrayList<>();
	}
}
