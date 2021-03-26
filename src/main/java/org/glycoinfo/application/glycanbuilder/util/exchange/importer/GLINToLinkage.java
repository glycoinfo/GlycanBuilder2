package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import java.util.ArrayList;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.util.WURCSDataConverter;
import org.glycoinfo.WURCSFramework.wurcs.array.GLIP;
import org.glycoinfo.WURCSFramework.wurcs.array.GLIPs;
import org.glycoinfo.WURCSFramework.wurcs.array.LIN;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GLIN;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;

public class GLINToLinkage {

	private LinkedList<GLIN> acceptorGLINs;
	private LinkedList<GLIN> donorGLINs;
	private ArrayList<GRES> acceptorGRESs;
	private GRES donorGRES;
	
	private GRES startGRES;
	private GRES startGRESInCyclic;
	private GRES endGRESinCyclic;
	
	private Residue acceptorRES;
	private Residue donorRES;
	
	private LinkedList<Linkage> donorLinkages;
	private LinkedList<Linkage> acceptorLinkages;
	private Linkage a_oRepeatingChildLinkage;
	private Linkage a_oRepeatingParentLinkage;
	private Linkage a_oCyclicChildLinkage;
	private Linkage a_oCyclicParentLinkage;
	
	private Linkage a_oBridgeLinkage;
	
	private LinkedList<LIN> linkages;
	
	private int min = 0;
	private int max = 0;
	
	private boolean isReverse;
	
	public GLINToLinkage() {
		
	}
	
	public GLINToLinkage(Residue _a_oRES) {
		this.acceptorRES = _a_oRES;
		this.donorRES = _a_oRES;
	}
	
	public GLINToLinkage(Residue _a_oRES, LinkedList<LIN> _a_aLINs) {
		this.acceptorRES = _a_oRES;
		this.donorRES = _a_oRES;
		this.linkages = _a_aLINs;
	}
		
	public LinkedList<Linkage> getChildLinkages() {
		return this.donorLinkages;
	}
	
	public LinkedList<Linkage> getParentLinkage() {
		return this.acceptorLinkages;
	}
	
	public Linkage getParentRepeatingLinkage() {
		return this.a_oRepeatingParentLinkage;
	}
	
	public Linkage getChildRepeatingLinkage() {
		return this.a_oRepeatingChildLinkage;
	}
	
	public Linkage getStartCyclicLinkage() {
		return this.a_oCyclicParentLinkage;
	}
	
	public Linkage getEndCyclicLinkage() {
		return this.a_oCyclicChildLinkage;
	}
	
	public GRES getChild() {
		return this.donorGRES;
	}
	
	public GRES getStartRepeatingGRES() {
		return this.startGRES;
	}
	
	public ArrayList<GRES> getParents() { return this.acceptorGRESs; }
	
	public LinkedList<GLIN> getDonorGLINs() {
		return this.donorGLINs;
	}
	
	public LinkedList<GLIN> getAcceptorGLINs() {
		return this.acceptorGLINs;
	}
	
	public GRES getStartGRESInCyclic() {
		return this.startGRESInCyclic;
	}
	
	public GRES getEndGRESInCyclic() {
		return this.endGRESinCyclic;
	}
	
	public Linkage getBridgeLinkage() {
		return this.a_oBridgeLinkage;
	}
	
	public boolean isRepeating() {
		return (a_oRepeatingChildLinkage != null || a_oRepeatingParentLinkage != null);
	}
	
	public boolean isCyclic() {
		return (a_oCyclicChildLinkage != null || a_oCyclicParentLinkage != null);
	}

	public String getMaxRepeatingCount () {
		return String.valueOf(this.max);
	}
	
	public String getMinRepeatingCount() {
		return String.valueOf(this.min);
	}

	public void start(GRES _gres) throws Exception {
		this.init();
		this.setGRESs(_gres);
		this.setLinkage(_gres.getAcceptorGLINs(), _gres.getDonorGLINs());
	
		// extract acceptor GLIN
		this.analyzeAcceptorGLIN(_gres);
		this.analyzeDonorGLIN(_gres);
	}
	
	private void analyzeAcceptorGLIN(GRES _gres) {
		for(GLIN a_oAGLIN : _gres.getAcceptorGLINs()) {
			if(a_oAGLIN.isRepeat()) this.analyzeRepeatingGLINforChild(a_oAGLIN);
			else if(a_oAGLIN.getDonor().size() > 0) {
				if(a_oAGLIN.getAcceptor().get(0).getID() > a_oAGLIN.getDonor().get(0).getID()) {
					if(this.isFacingBetweenAnomer(a_oAGLIN) || !a_oAGLIN.getMAP().equals("") || this.isLinkageWithUnknown(a_oAGLIN))
						this.analyzeGLINforChild(a_oAGLIN);
					else if(this.isReverse)
						this.analyzeGLINforParent(a_oAGLIN);
					else
						this.analyzeCyclicGLINforEnd(a_oAGLIN);
				}
			}
			else this.analyzeGLINforChild(a_oAGLIN);
		}
	}
	
	private void analyzeDonorGLIN(GRES _gres) {
		for(GLIN a_oDGLIN : _gres.getDonorGLINs()) {
			if(a_oDGLIN.getDonor().size() > 1) continue;
			
			if(a_oDGLIN.isRepeat()) this.analyzeRepeatingGLINforParent(a_oDGLIN);
			else if(a_oDGLIN.getAcceptor().get(0).getID() > a_oDGLIN.getDonor().get(0).getID()) {
				if(this.isFacingBetweenAnomer(a_oDGLIN) || !a_oDGLIN.getMAP().equals("") || this.isLinkageWithUnknown(a_oDGLIN)) {
					this.analyzeGLINforParent(a_oDGLIN);
				}else
					this.analyzeCyclicGLINforStart(a_oDGLIN);
			}else this.analyzeGLINforParent(a_oDGLIN);
		}
		
		// check dual linkages
		if(acceptorLinkages.size() == 2) checkDualLinkage(getParentLinkage(), _gres);
	}
	
	protected void analyzeGLINforChild(GLIN _acceptorGLIN) {
		char[] a_caPositions = this.makeLinkagePosiiton(_acceptorGLIN.getAcceptorPositions());
		this.donorLinkages.add(new Linkage(this.acceptorRES, null, a_caPositions));
	}
	
	protected void analyzeGLINforParent(GLIN _donorGLIN) {
		char[] a_caPositions = this.makeLinkagePosiiton(_donorGLIN.getAcceptorPositions());
		char[] a_cdPositions = this.makeLinkagePosiiton(_donorGLIN.getDonorPositions());
		Linkage linkage = null;
		
		if(_donorGLIN.getMAP().equals("")) {
			linkage = new Linkage(null, this.acceptorRES, a_caPositions);
			linkage.setAnomericCarbon(a_cdPositions[0]);
			this.acceptorLinkages.add(linkage);
		}else {
			SUBSTAnalyzer a_oSUBSTAnalyzer = new SUBSTAnalyzer();
			try {
				Residue a_oSUB = a_oSUBSTAnalyzer.MAPToBridge(_donorGLIN);
				linkage = new Linkage(a_oSUB, this.acceptorRES, a_cdPositions);
				linkage.setAnomericCarbon(a_cdPositions[0]);
				linkage.setSubstituent(a_oSUB);

				this.a_oBridgeLinkage = linkage;

				linkage = new Linkage(null, a_oSUB, a_caPositions);
				this.acceptorLinkages.add(linkage);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// probability annotation
		this.extractProbabilityAnnotation(_donorGLIN, linkage);
	}

	/**
	 * Set end repeating linkage
	 * @param _acceptorGLIN
	 */
	private void analyzeRepeatingGLINforChild(GLIN _acceptorGLIN) {
		this.max = _acceptorGLIN.getRepeatCountMax();
		this.min = _acceptorGLIN.getRepeatCountMin();

		char[] a_caPositions = this.makeLinkagePosiiton(_acceptorGLIN.getAcceptorPositions());
		char[] a_cdPositions = this.makeLinkagePosiiton(_acceptorGLIN.getDonorPositions());
		
		if(_acceptorGLIN.getMAP().equals("")) {
			this.a_oRepeatingChildLinkage = new Linkage(this.acceptorRES, null, a_caPositions);
			this.a_oRepeatingChildLinkage.setAnomericCarbon(a_cdPositions[0]);
		} else {
			SUBSTAnalyzer a_oSUBSTAnalyzer = new SUBSTAnalyzer();
			Linkage a_oLIN = null;
			try {
				Residue a_oSUB = a_oSUBSTAnalyzer.MAPToBridge(_acceptorGLIN);
			
				// sugar<->sub
				a_oLIN = new Linkage(this.acceptorRES, a_oSUB, a_caPositions);
				this.acceptorLinkages.add(a_oLIN);
				
				// sub<->]
				this.a_oRepeatingChildLinkage = new Linkage(a_oSUB, null, a_cdPositions);
				this.a_oRepeatingChildLinkage.setSubstituent(a_oSUB);
				this.a_oRepeatingChildLinkage.setAnomericCarbon(a_cdPositions[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
					
		this.startGRES = _acceptorGLIN.getDonor().get(0);
	}
	
	/**
	 * Set start repeating linkage
	 * @param _donorGLIN
	 */
	private void analyzeRepeatingGLINforParent(GLIN _donorGLIN) {
		char[] a_caPositions = this.makeLinkagePosiiton(_donorGLIN.getAcceptorPositions());
		this.a_oRepeatingParentLinkage = new Linkage(null, this.donorRES, a_caPositions);
		this.a_oRepeatingParentLinkage.setAnomericCarbon(this.makeLinkagePosiiton(_donorGLIN.getDonorPositions())[0]);
	}
	
	private void analyzeCyclicGLINforStart(GLIN _donorGLIN) {
		char[] a_caPositions = this.makeLinkagePosiiton(_donorGLIN.getAcceptorPositions());
		char[] a_cdPositions = this.makeLinkagePosiiton(_donorGLIN.getDonorPositions());
		
		this.a_oCyclicParentLinkage = new Linkage(null, null, a_caPositions);
		this.a_oCyclicParentLinkage.getBonds().get(0).setChildPosition(a_cdPositions[0]);
		
		this.endGRESinCyclic = _donorGLIN.getAcceptor().get(0);
	}
	
	private void analyzeCyclicGLINforEnd(GLIN _acceptorGLIN) {
		char[] a_caPositions = this.makeLinkagePosiiton(_acceptorGLIN.getAcceptorPositions());
		char[] a_cdPositions = this.makeLinkagePosiiton(_acceptorGLIN.getDonorPositions());
		this.a_oCyclicChildLinkage = new Linkage(null, null, a_caPositions);
		this.a_oCyclicChildLinkage.getBonds().get(0).setChildPosition(a_cdPositions[0]);
	
		this.startGRESInCyclic = _acceptorGLIN.getDonor().get(0);
	}
	
	protected char[] makeLinkagePosiiton (LinkedList<Integer> _positions) {
		if(_positions.isEmpty()) return new char[] {'?'};

		char[] a_cPositions = new char[_positions.size()];
		
		for(int i = 0; i < _positions.size(); i++) {
			String a_sPosition = String.valueOf(_positions.get(i));
			a_cPositions[i] = a_sPosition.equals("-1") ? '?' : a_sPosition.charAt(0);
		}
	
		return a_cPositions;
	}
	
	public void setLinkage(LinkedList<GLIN> _acceptorGLINs, LinkedList<GLIN> _donorGLINs) {
		this.acceptorGLINs = _acceptorGLINs;
		this.donorGLINs = _donorGLINs;
	}
	
	public void setChildLinkage(LinkedList<Linkage> a_aLinkages) {
		this.donorLinkages = a_aLinkages;
	}
	
	public void setParentLinkage(LinkedList<Linkage> a_aLinkages) {
		this.acceptorLinkages = a_aLinkages;
	}
	
	public void setGRESs(GRES _gres) {
		this.donorGRES = _gres;
		
		// for reverse antenna
		if(!_gres.getAcceptorGLINs().isEmpty()) {
			LinkedList<GLIN> a_aGLINs = _gres.getAcceptorGLINs();
			if(a_aGLINs.getFirst().getDonor().contains(_gres)) {
				this.acceptorGRESs.addAll(a_aGLINs.getFirst().getDonor());
			}else {
				for(GRES a_oDGRES : a_aGLINs.getFirst().getDonor()) {
				if(a_oDGRES.getID() - _gres.getID() == 1) continue;
					this.acceptorGRESs.add(a_oDGRES);
				}
				this.isReverse = (this.acceptorGRESs.size() > 1);
				if(!this.isReverse) this.acceptorGRESs.clear();
			}
		}
		/*
		for (GLIN acceptorGLIN : _gres.getAcceptorGLINs()) {
			if (acceptorGLIN.isRepeat()) continue;
			for(GRES acceptor : acceptorGLIN.getAcceptor()) {
				if (this.acceptorGRESs.contains(acceptor)) continue;
				if (_gres.equals(acceptor)) continue;
				this.acceptorGRESs.add(acceptor);

			}
		}
		 */


		for(GLIN a_oDGLIN : _gres.getDonorGLINs()) {
			if(a_oDGLIN.isRepeat()) continue;
			for(GRES a_oAGRES : a_oDGLIN.getAcceptor()) {
				if(this.acceptorGRESs.contains(a_oAGRES)) continue;
				if(_gres.getID() - a_oAGRES.getID() > 0) this.acceptorGRESs.add(a_oAGRES);
				// for facing fructose
				if(a_oAGRES.getID() - _gres.getID() == 1) this.acceptorGRESs.add(a_oAGRES);
			}
		}	
	}
	
	private void extractProbabilityAnnotation (GLIN _glin, Linkage _linkage) {
		if(_glin.getAcceptor().size() > 1) return;

		for(LIN a_oWLIN : this.linkages) {
			for(GLIPs a_aGLIPs : a_oWLIN.getListOfGLIPs()) {
				for(GLIP a_oGLIP : a_aGLIPs.getGLIPs()) {
					if(a_oGLIP.getModificationProbabilityLower() == 1.0 && a_oGLIP.getModificationProbabilityUpper() == 1.0) continue;
					if((WURCSDataConverter.convertRESIDToIndex(_glin.getAcceptor().getFirst().getID()).equals(a_oGLIP.getRESIndex())) &&
							(_glin.getAcceptorPositions().contains(a_oGLIP.getBackbonePosition()))) {
						_linkage.getBonds().get(0).setProbabilityLow(a_oGLIP.getModificationProbabilityLower());
						_linkage.getBonds().get(0).setProbabilityHigh(a_oGLIP.getModificationProbabilityUpper());
					}
				}
			}
		}
	}
	
	public boolean isFacingBetweenAnomer(GLIN a_oGLIN) {
		boolean a_bIsFacing = false;
		int a_iParentPos = a_oGLIN.getAcceptor().getFirst().getMS().getCoreStructure().getAnomericPosition();
		int a_iChildPos = a_oGLIN.getDonor().getFirst().getMS().getCoreStructure().getAnomericPosition();
		
		if((a_iParentPos == a_oGLIN.getAcceptorPositions().getFirst()) &&
				a_iChildPos == a_oGLIN.getDonorPositions().getFirst())
			a_bIsFacing = true;
	
		return a_bIsFacing;
	}
	
	public boolean isLinkageWithUnknown(GLIN _glin) {
		if(_glin.getAcceptor().isEmpty() || _glin.getDonor().isEmpty()) return false;
		boolean a_bIsUnknown = false;
		String a_sParent = _glin.getDonor().getFirst().getMS().getCoreStructure().getSkeletonCode();
		String a_sChild = _glin.getAcceptor().getFirst().getMS().getCoreStructure().getSkeletonCode();
		
		if(a_sParent.equals("<Q>") && a_sChild.equals("<Q>")) a_bIsUnknown = true;
		
		return a_bIsUnknown;
	}
	
	private void checkDualLinkage(LinkedList<Linkage> _parents, GRES _gres) {
		LinkedList<GRES> firstParents = _gres.getDonorGLINs().getFirst().getDonor();
		LinkedList<GRES> secondParents = _gres.getDonorGLINs().getLast().getDonor();
		
		if(firstParents.size() != 1 || secondParents.size() != 1) return;
		if(!firstParents.contains(secondParents.get(0))) return;		
		if(!_parents.getFirst().getChildResidue().equals(_parents.getLast().getChildResidue())) return;
		
		char[] pos = new char[2];
		for(int i = 0; i < _parents.size(); i++) {
			pos[i] = _parents.get(i).getParentPositionsSingle();
		}
		
		Linkage second = _parents.getLast();
		Linkage temp = new Linkage(null, acceptorRES);
		temp.setLinkagePositions(new char[] {pos[0]}, new char[] {pos[1]}, second.getChildPositionsSingle());
		temp.setAnomericCarbon(_parents.getFirst().getAnomericCarbon());
	
		acceptorLinkages.clear();
		acceptorLinkages.addLast(temp);
	}
	
	private void init() {
		this.acceptorGRESs = new ArrayList<>();
		this.acceptorGLINs = new LinkedList<>();
		this.donorGLINs = new LinkedList<>();
		this.donorLinkages = new LinkedList<>();
		this.acceptorLinkages = new LinkedList<>();
		
		this.isReverse = false;
	}
}
