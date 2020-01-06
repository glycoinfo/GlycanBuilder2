package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import java.util.ArrayList;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.util.WURCSDataConverter;
import org.glycoinfo.WURCSFramework.util.array.WURCSFormatException;
import org.glycoinfo.WURCSFramework.wurcs.array.GLIP;
import org.glycoinfo.WURCSFramework.wurcs.array.GLIPs;
import org.glycoinfo.WURCSFramework.wurcs.array.LIN;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GLIN;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;

public class GLINToLinkage {

	private LinkedList<GLIN> a_aAcceptorGLINs;
	private LinkedList<GLIN> a_aDonorGLINs;
	private ArrayList<GRES> a_aAcceptorGRESs;
	private GRES a_oDonorGRES;
	
	private GRES a_oStartGRES;
	private GRES a_oStartGRESInCyclic;
	private GRES a_oEndGRESinCyclic;
	
	private Residue a_oParentRES;
	private Residue a_oChildRES;
	
	private LinkedList<Linkage> a_aChildLinkages;
	private LinkedList<Linkage> a_aParentLinkages;
	private Linkage a_oRepeatingChildLinkage;
	private Linkage a_oRepeatingParentLinkage;
	private Linkage a_oCyclicChildLinkage;
	private Linkage a_oCyclicParentLinkage;
	
	private Linkage a_oBridgeLinkage;
	
	private LinkedList<LIN> a_aLINs;
	
	private int a_iMin = 0;
	private int a_iMax = 0;
	
	private boolean a_bIsReverse;
	
	public GLINToLinkage() {
		
	}
	
	public GLINToLinkage(Residue _a_oRES) {
		this.a_oParentRES = _a_oRES;
		this.a_oChildRES = _a_oRES;
	}
	
	public GLINToLinkage(Residue _a_oRES, LinkedList<LIN> _a_aLINs) {
		this.a_oParentRES = _a_oRES;
		this.a_oChildRES = _a_oRES;
		this.a_aLINs = _a_aLINs;
	}
		
	public LinkedList<Linkage> getChildLinkages() {
		return this.a_aChildLinkages;
	}
	
	public LinkedList<Linkage> getParentLinkage() {
		return this.a_aParentLinkages;
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
		return this.a_oDonorGRES;
	}
	
	public GRES getStartRepeatingGRES() {
		return this.a_oStartGRES;
	}
	
	public ArrayList<GRES> getParents() {
		return this.a_aAcceptorGRESs;
	}
	
	public LinkedList<GLIN> getDonorGLINs() {
		return this.a_aDonorGLINs;
	}
	
	public LinkedList<GLIN> getAcceptorGLINs() {
		return this.a_aAcceptorGLINs;
	}
	
	public GRES getStartGRESInCyclic() {
		return this.a_oStartGRESInCyclic;
	}
	
	public GRES getEndGRESInCyclic() {
		return this.a_oEndGRESinCyclic;
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
		return String.valueOf(this.a_iMax);
	}
	
	public String getMinRepeatingCount() {
		return String.valueOf(this.a_iMin);
	}

	public void start(GRES a_oGRES) throws WURCSFormatException {
		this.init();
		this.setGRESs(a_oGRES);
		this.setLinkage(a_oGRES.getAcceptorGLINs(), a_oGRES.getDonorGLINs());
	
		/** extract acceptor GLIN*/
		this.analyzeAcceptorGLIN(a_oGRES);
		this.analyzeDonorGLIN(a_oGRES);
		
		return;
	}
	
	private void analyzeAcceptorGLIN(GRES a_oGRES) {
		for(GLIN a_oAGLIN : a_oGRES.getAcceptorGLINs()) {
			if(a_oAGLIN.isRepeat()) this.analyzeRepeatingGLINforChild(a_oAGLIN);
			else if(a_oAGLIN.getDonor().size() > 0) {
				if(a_oAGLIN.getAcceptor().get(0).getID() > a_oAGLIN.getDonor().get(0).getID()) {
					if(this.isFacingBetweenAnomer(a_oAGLIN) || !a_oAGLIN.getMAP().equals("") || this.isLinkageWithUnknown(a_oAGLIN))
						this.analyzeGLINforChild(a_oAGLIN);
					else if(this.a_bIsReverse)
						this.analyzeGLINforParent(a_oAGLIN);
					else
						this.analyzeCyclicGLINforEnd(a_oAGLIN);
				}
			}
			else this.analyzeGLINforChild(a_oAGLIN);
		}
	}
	
	private void analyzeDonorGLIN(GRES a_oGRES) {
		for(GLIN a_oDGLIN : a_oGRES.getDonorGLINs()) {
			if(a_oDGLIN.getDonor().size() > 1) continue;
			
			if(a_oDGLIN.isRepeat()) this.analyzeRepeatingGLINforParent(a_oDGLIN);
			else if(a_oDGLIN.getAcceptor().get(0).getID() > a_oDGLIN.getDonor().get(0).getID()) {
				if(this.isFacingBetweenAnomer(a_oDGLIN) || !a_oDGLIN.getMAP().equals("") || this.isLinkageWithUnknown(a_oDGLIN)) {
					this.analyzeGLINforParent(a_oDGLIN);
				}else
					this.analyzeCyclicGLINforStart(a_oDGLIN);
			}else this.analyzeGLINforParent(a_oDGLIN);
		}
		
		/** check dual linkages */
		if(a_aParentLinkages.size() == 2) checkDualLinkage(getParentLinkage(), a_oGRES);
	}
	
	protected void analyzeGLINforChild(GLIN a_oAGLIN) {	
		char[] a_caPositions = this.makeLinkagePosiiton(a_oAGLIN.getAcceptorPositions());
		this.a_aChildLinkages.add(new Linkage(this.a_oParentRES, null, a_caPositions));
	
		return;
	}
	
	protected void analyzeGLINforParent(GLIN a_oDGLIN) {
		char[] a_caPositions = this.makeLinkagePosiiton(a_oDGLIN.getAcceptorPositions());
		char[] a_cdPositions = this.makeLinkagePosiiton(a_oDGLIN.getDonorPositions());
		Linkage a_oLIN = null;
		
		if(a_oDGLIN.getMAP().equals("")) {
			a_oLIN = new Linkage(null, this.a_oParentRES, a_caPositions);
			a_oLIN.setAnomericCarbon(a_cdPositions[0]);
			this.a_aParentLinkages.add(a_oLIN);
		}else {
			SUBSTAnalyzer a_oSUBSTAnalyzer = new SUBSTAnalyzer();
			try {
				Residue a_oSUB = a_oSUBSTAnalyzer.MAPToBridge(a_oDGLIN);
				a_oLIN = new Linkage(a_oSUB, this.a_oParentRES, a_cdPositions);
				a_oLIN.setAnomericCarbon(a_cdPositions[0]);
				a_oLIN.setSubstituent(a_oSUB);

				this.a_oBridgeLinkage = a_oLIN;

				a_oLIN = new Linkage(null, a_oSUB, a_caPositions);
				this.a_aParentLinkages.add(a_oLIN);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		/** probability annotation */
		this.extractProbabilityAnnotation(a_oDGLIN, a_oLIN);
		
		return;
	}

	/**
	 * Set end repeating linkage
	 * @param a_oAGLIN
	 */
	private void analyzeRepeatingGLINforChild(GLIN a_oAGLIN) {
		this.a_iMax = a_oAGLIN.getRepeatCountMax();
		this.a_iMin = a_oAGLIN.getRepeatCountMin();

		char[] a_caPositions = this.makeLinkagePosiiton(a_oAGLIN.getAcceptorPositions());
		char[] a_cdPositions = this.makeLinkagePosiiton(a_oAGLIN.getDonorPositions());
		
		if(a_oAGLIN.getMAP().equals("")) {
			this.a_oRepeatingChildLinkage = new Linkage(this.a_oParentRES, null, a_caPositions);
			this.a_oRepeatingChildLinkage.setAnomericCarbon(a_cdPositions[0]);
		} else {
			SUBSTAnalyzer a_oSUBSTAnalyzer = new SUBSTAnalyzer();
			Linkage a_oLIN = null;
			try {
				Residue a_oSUB = a_oSUBSTAnalyzer.MAPToBridge(a_oAGLIN);
			
				/** sugar<->sub */
				a_oLIN = new Linkage(this.a_oParentRES, a_oSUB, a_caPositions);				
				this.a_aParentLinkages.add(a_oLIN);
				
				/** sub<->] */
				this.a_oRepeatingChildLinkage = new Linkage(a_oSUB, null, a_cdPositions);
				this.a_oRepeatingChildLinkage.setSubstituent(a_oSUB);
				this.a_oRepeatingChildLinkage.setAnomericCarbon(a_cdPositions[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
					
		this.a_oStartGRES = a_oAGLIN.getDonor().get(0);
		
		return;
	}
	
	/**
	 * Set start repeating linkage
	 * @param a_oDGLIN
	 */
	private void analyzeRepeatingGLINforParent(GLIN a_oDGLIN) {
		char[] a_caPositions = this.makeLinkagePosiiton(a_oDGLIN.getAcceptorPositions());
		this.a_oRepeatingParentLinkage = new Linkage(null, this.a_oChildRES, a_caPositions);		
		this.a_oRepeatingParentLinkage.setAnomericCarbon(this.makeLinkagePosiiton(a_oDGLIN.getDonorPositions())[0]);
		
		return;
	}
	
	private void analyzeCyclicGLINforStart(GLIN a_oDGLIN) {
		char[] a_caPositions = this.makeLinkagePosiiton(a_oDGLIN.getAcceptorPositions());
		char[] a_cdPositions = this.makeLinkagePosiiton(a_oDGLIN.getDonorPositions());
		
		this.a_oCyclicParentLinkage = new Linkage(null, null, a_caPositions);
		this.a_oCyclicParentLinkage.getBonds().get(0).setChildPosition(a_cdPositions[0]);
		
		this.a_oEndGRESinCyclic = a_oDGLIN.getAcceptor().get(0);
		
		return;
	}
	
	private void analyzeCyclicGLINforEnd(GLIN a_oAGLIN) {
		char[] a_caPositions = this.makeLinkagePosiiton(a_oAGLIN.getAcceptorPositions());
		char[] a_cdPositions = this.makeLinkagePosiiton(a_oAGLIN.getDonorPositions());
		this.a_oCyclicChildLinkage = new Linkage(null, null, a_caPositions);
		this.a_oCyclicChildLinkage.getBonds().get(0).setChildPosition(a_cdPositions[0]);
	
		this.a_oStartGRESInCyclic = a_oAGLIN.getDonor().get(0);
		
		return;
	}
	
	protected char[] makeLinkagePosiiton (LinkedList<Integer> a_aPositions) {
		if(a_aPositions.isEmpty()) return new char[] {'?'};

		char[] a_cPositions = new char[a_aPositions.size()];
		
		for(int i = 0; i < a_aPositions.size(); i++) {
			String a_sPosition = String.valueOf(a_aPositions.get(i));
			a_cPositions[i] = a_sPosition.equals("-1") ? '?' : a_sPosition.charAt(0);
		}
	
		return a_cPositions;
	}
	
	public void setLinkage(LinkedList<GLIN> a_oAcceptorGLINs, LinkedList<GLIN> a_oDonorGLINs) {
		this.a_aAcceptorGLINs = a_oAcceptorGLINs;
		this.a_aDonorGLINs = a_oDonorGLINs;
	}
	
	public void setChildLinkage(LinkedList<Linkage> a_aLinkages) {
		this.a_aChildLinkages = a_aLinkages;
	}
	
	public void setParentLinkage(LinkedList<Linkage> a_aLinkages) {
		this.a_aParentLinkages = a_aLinkages;
	}
	
	public void setGRESs(GRES a_oGRES) {
		this.a_oDonorGRES = a_oGRES;
		
		/** for reverse antenna */
		if(!a_oGRES.getAcceptorGLINs().isEmpty()) {
			LinkedList<GLIN> a_aGLINs = a_oGRES.getAcceptorGLINs();
			if(a_aGLINs.getFirst().getDonor().contains(a_oGRES)) {
				this.a_aAcceptorGRESs.addAll(a_aGLINs.getFirst().getDonor());
			}else {
				for(GRES a_oDGRES : a_aGLINs.getFirst().getDonor()) { 
				if(a_oDGRES.getID() - a_oGRES.getID() == 1) continue;
					this.a_aAcceptorGRESs.add(a_oDGRES);
				}
				this.a_bIsReverse = (this.a_aAcceptorGRESs.size() > 1);
				if(!this.a_bIsReverse) this.a_aAcceptorGRESs.clear();
			}
		}
		
		for(GLIN a_oDGLIN : a_oGRES.getDonorGLINs()) {
			if(a_oDGLIN.isRepeat()) continue;
			for(GRES a_oAGRES : a_oDGLIN.getAcceptor()) {
				if(this.a_aAcceptorGRESs.contains(a_oAGRES)) continue;
				if(a_oGRES.getID() - a_oAGRES.getID() > 0) this.a_aAcceptorGRESs.add(a_oAGRES);
				/** for facing fructose*/
				if(a_oAGRES.getID() - a_oGRES.getID() == 1) this.a_aAcceptorGRESs.add(a_oAGRES);
			}
		}	
	}
	
	private void extractProbabilityAnnotation (GLIN a_oDGLIN, Linkage a_oLIN) {
		if(a_oDGLIN.getAcceptor().size() > 1) return;

		for(LIN a_oWLIN : this.a_aLINs) {
			for(GLIPs a_aGLIPs : a_oWLIN.getListOfGLIPs()) {
				for(GLIP a_oGLIP : a_aGLIPs.getGLIPs()) {
					if(a_oGLIP.getModificationProbabilityLower() == 1.0 && a_oGLIP.getModificationProbabilityUpper() == 1.0) continue;
					if((WURCSDataConverter.convertRESIDToIndex(a_oDGLIN.getAcceptor().getFirst().getID()).equals(a_oGLIP.getRESIndex())) && 
							(a_oDGLIN.getAcceptorPositions().contains(a_oGLIP.getBackbonePosition()))) {
						a_oLIN.getBonds().get(0).setProbabilityLow(a_oGLIP.getModificationProbabilityLower());
						a_oLIN.getBonds().get(0).setProbabilityHigh(a_oGLIP.getModificationProbabilityUpper());
					}
				}
			}
		}
		
		return;
	}
	
	public boolean isFacingBetweenAnomer(GLIN a_oGLIN) {
		boolean a_bIsFacing = false;
		int a_iParentPos = a_oGLIN.getAcceptor().getFirst().getMS().getCoreStructure().getAnomericPosition();
		int a_iChildPos = a_oGLIN.getDonor().getFirst().getMS().getCoreStructure().getAnomericPosition();
		
		//a_iParentPos = a_oGLIN.getAcceptorPositions().getFirst();
		//a_iChildPos = a_oGLIN.getDonorPositions().getFirst();
		
		//if(a_oGLIN.getAcceptor().getFirst().getMS().getCoreStructure().getAnomericSymbol() == 'o')
		//	a_iParentPos = a_oGLIN.getAcceptorPositions().getFirst();
		//if(a_oGLIN.getDonor().getFirst().getMS().getCoreStructure().getAnomericSymbol() == 'o')
		//	a_iChildPos = a_oGLIN.getDonorPositions().getFirst();
		
		if((a_iParentPos == a_oGLIN.getAcceptorPositions().getFirst()) &&
				a_iChildPos == a_oGLIN.getDonorPositions().getFirst())
			a_bIsFacing = true;
	
		return a_bIsFacing;
	}
	
	public boolean isLinkageWithUnknown(GLIN a_oGLIN) {
		if(a_oGLIN.getAcceptor().isEmpty() || a_oGLIN.getDonor().isEmpty()) return false;
		boolean a_bIsUnknown = false;
		String a_sParent = a_oGLIN.getDonor().getFirst().getMS().getCoreStructure().getSkeletonCode();
		String a_sChild = a_oGLIN.getAcceptor().getFirst().getMS().getCoreStructure().getSkeletonCode();
		
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
		Linkage temp = new Linkage(null, a_oParentRES);
		temp.setLinkagePositions(new char[] {pos[0]}, new char[] {pos[1]}, second.getChildPositionsSingle());
		temp.setAnomericCarbon(_parents.getFirst().getAnomericCarbon());
	
		a_aParentLinkages.clear();
		a_aParentLinkages.addLast(temp);
		
		return;
	}
	
	private void init() {
		this.a_aAcceptorGRESs = new ArrayList<GRES>();
		this.a_aAcceptorGLINs = new LinkedList<GLIN>();
		this.a_aDonorGLINs = new LinkedList<GLIN>();
		this.a_aChildLinkages = new LinkedList<Linkage>();
		this.a_aParentLinkages = new LinkedList<Linkage>();
		
		this.a_bIsReverse = false;
	}
}
