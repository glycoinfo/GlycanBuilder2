package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.util.WURCSDataConverter;
import org.glycoinfo.WURCSFramework.wurcs.array.GLIP;
import org.glycoinfo.WURCSFramework.wurcs.array.GLIPs;
import org.glycoinfo.WURCSFramework.wurcs.array.LIN;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GLIN;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;

import java.util.ArrayList;
import java.util.LinkedList;

public class GLINToLinkage {

	private LinkedList<GLIN> acceptorGLINs;
	private LinkedList<GLIN> donorGLINs;
	private ArrayList<GRES> acceptorGRESs;
	private GRES donorGRES;
	private GRES startGRES;

	private Residue acceptorRES;
	private Residue donorRES;

	private LinkedList<Linkage> donorLinkages;
	private LinkedList<Linkage> acceptorLinkages;
	private Linkage endSideRepLinkage;
	private Linkage startSideRepLinkage;
	private Linkage a_oCyclicChildLinkage;
	private Linkage a_oCyclicParentLinkage;

	private Linkage a_oBridgeLinkage;

	private LinkedList<LIN> linkages;

	private int min = 0;
	private int max = 0;

	private boolean isReverse;

	public GLINToLinkage() {}

	public GLINToLinkage(Residue _a_oRES) {
		this.acceptorRES = _a_oRES;
		this.donorRES = _a_oRES;
	}

	public GLINToLinkage(Residue _a_oRES, LinkedList<LIN> _a_aLINs) {
		this.acceptorRES = _a_oRES;
		this.donorRES = _a_oRES;
		this.linkages = _a_aLINs;
	}

	public LinkedList<Linkage> getParentLinkage() {
		return this.acceptorLinkages;
	}

	public Linkage getStartSideRepLinkage() {
		return this.startSideRepLinkage;
	}

	public Linkage getEndSideRepLinkage() {
		return this.endSideRepLinkage;
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

	public Linkage getBridgeLinkage() {
		return this.a_oBridgeLinkage;
	}

	public boolean isRepeating() {
		return (endSideRepLinkage != null || startSideRepLinkage != null);
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

	public GLIN getDonorSideGLIN () {
		if (this.donorGRES == null || this.acceptorGRESs.isEmpty()) return null;
		for (GLIN donorGLIN : this.donorGLINs) {
			if (donorGLIN.getDonor().contains(this.donorGRES) && donorGLIN.getAcceptor().contains(this.acceptorGRESs.get(0))) {
				return donorGLIN;
			}
		}
		return null;
	}

	public GLIN getAcceptorSideGLIN () {
		if (this.donorGRES == null || this.acceptorGRESs.isEmpty()) return null;
		for (GLIN acceptorGLIN : this.acceptorGLINs) {
			if (acceptorGLIN.getAcceptor().contains(this.acceptorGRESs.get(0)) && acceptorGLIN.getDonor().contains(this.donorGRES)) {
				return acceptorGLIN;
			}
		}
		return null;
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
		for(GLIN acceptorGLIN : _gres.getAcceptorGLINs()) {
			//if(acceptorGLIN.isRepeat()) this.analyzeStartSideRep(acceptorGLIN);
			if(acceptorGLIN.isRepeat()) this.analyzeAcceptorSideRep(acceptorGLIN);
			else if(acceptorGLIN.getDonor().size() > 0) {
				//if(a_oAGLIN.getAcceptor().get(0).getID() > a_oAGLIN.getDonor().get(0).getID()) {
				if(this.glinIsCyclic(acceptorGLIN)) {
					if(this.isFacingBetweenAnomer(acceptorGLIN) || !acceptorGLIN.getMAP().equals("") || this.isLinkageWithUnknown(acceptorGLIN))
						this.analyzeGLINforChild(acceptorGLIN);
					else if(this.isReverse)
						this.analyzeGLINforParent(acceptorGLIN);
					else
						this.analyzeEndSideCyclic(acceptorGLIN);
				}
			}
			else this.analyzeGLINforChild(acceptorGLIN);
		}
	}

	private void analyzeDonorGLIN(GRES _gres) {
		for(GLIN donorGLIN : _gres.getDonorGLINs()) {
			if(donorGLIN.getDonor().size() > 1) continue;
			//if(donorGLIN.isRepeat()) this.analyzeEndSideRep(donorGLIN);
			if(donorGLIN.isRepeat()) this.analyzeDonorSideRep(donorGLIN);
			else if (this.glinIsCyclic(donorGLIN)) {
				//else if(a_oDGLIN.getAcceptor().get(0).getID() > a_oDGLIN.getDonor().get(0).getID()) {
				if(this.isFacingBetweenAnomer(donorGLIN) || !donorGLIN.getMAP().equals("") || this.isLinkageWithUnknown(donorGLIN)) {
					this.analyzeGLINforParent(donorGLIN);
				}else
					this.analyzeStartSideCyclic(donorGLIN);
			}else this.analyzeGLINforParent(donorGLIN);
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
				//linkage.setSubstituent(a_oSUB);

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

	/*
	private void analyzeStartSideRep(GLIN _acceptorGLIN) {
		this.max = _acceptorGLIN.getRepeatCountMax();
		this.min = _acceptorGLIN.getRepeatCountMin();

		char[] a_caPositions = this.makeLinkagePosiiton(_acceptorGLIN.getAcceptorPositions());
		this.startSideRepLinkage = new Linkage(null, this.donorRES, a_caPositions);
		this.startSideRepLinkage.setAnomericCarbon(this.makeLinkagePosiiton(_acceptorGLIN.getDonorPositions())[0]);

		// 20210831 S.TSUCHIYA changed
		this.startGRES = _acceptorGLIN.getAcceptor().get(0);
	}
	 */

	private void analyzeAcceptorSideRep (GLIN _acceptorGLIN) {
		this.max = _acceptorGLIN.getRepeatCountMax();
		this.min = _acceptorGLIN.getRepeatCountMin();
		char[] a_caPositions = this.makeLinkagePosiiton(_acceptorGLIN.getAcceptorPositions());
		char[] a_cdPositions = this.makeLinkagePosiiton(_acceptorGLIN.getDonorPositions());

		GRES acceptorGRES = _acceptorGLIN.getAcceptor().getFirst();
		GRES donorGRES = _acceptorGLIN.getDonor().getFirst();

		// for bridge
		Residue bridge = null;
		if(!_acceptorGLIN.getMAP().equals("")) {
			try {
				SUBSTAnalyzer substAnalyzer = new SUBSTAnalyzer();
				bridge = substAnalyzer.MAPToBridge(_acceptorGLIN);

				// sugar->sub
				Linkage linkage = new Linkage(this.acceptorRES, bridge, a_caPositions);
				this.acceptorLinkages.add(linkage);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// for end-side rep
		if (acceptorGRES.getID() > donorGRES.getID()) {
			this.startGRES = donorGRES;

			// sub->EndRep or sugar->EndRep
			this.endSideRepLinkage = new Linkage(null, bridge, a_caPositions);
			this.endSideRepLinkage.setAnomericCarbon(a_cdPositions[0]);
		} else { // for start-side rep
			this.startGRES = acceptorGRES;

			// StartRep->sugar
			this.startSideRepLinkage = new Linkage(null, this.acceptorRES, a_caPositions);
			this.startSideRepLinkage.setAnomericCarbon(a_cdPositions[0]);
		}
	}

	private void analyzeDonorSideRep (GLIN _donorGLIN) {
		this.max = _donorGLIN.getRepeatCountMax();
		this.min = _donorGLIN.getRepeatCountMin();
		char[] a_caPositions; //= this.makeLinkagePosiiton(_donorGLIN.getAcceptorPositions());
		char[] a_cdPositions; //= this.makeLinkagePosiiton(_donorGLIN.getDonorPositions());

		GRES acceptorGRES = _donorGLIN.getAcceptor().getFirst();
		GRES donorGRES = _donorGLIN.getDonor().getFirst();

		// for bridge
		Residue bridge = null;
		if(!_donorGLIN.getMAP().equals("")) {
			a_caPositions = this.makeLinkagePosiiton(_donorGLIN.getAcceptorPositions());

			try {
				SUBSTAnalyzer substAnalyzer = new SUBSTAnalyzer();
				bridge = substAnalyzer.MAPToBridge(_donorGLIN);

				// sugar->sub
				Linkage linkage = new Linkage(this.acceptorRES, bridge, a_caPositions);
				this.acceptorLinkages.add(linkage);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// for start-side rep
		if (acceptorGRES.getID() > donorGRES.getID()) {
			// for StartRep<-sugar
			if (!this.acceptorGRESs.isEmpty()) {
				GLIN donorGLIN = this.getDonorSideGLIN();
				a_caPositions = this.makeLinkagePosiiton(donorGLIN.getAcceptorPositions());
				a_cdPositions = this.makeLinkagePosiiton(donorGLIN.getDonorPositions());
			} else {
				// StartRep is not attach any monosaccharide to acceptor side.
				a_caPositions = this.makeLinkagePosiiton(_donorGLIN.getAcceptorPositions());
				a_cdPositions = this.makeLinkagePosiiton(_donorGLIN.getDonorPositions());
			}

			this.startGRES = donorGRES;

			// StartRep->sugar
			this.startSideRepLinkage = new Linkage(null, this.donorRES, a_caPositions);
			this.startSideRepLinkage.setAnomericCarbon(a_cdPositions[0]);
		} else { // for end-side rep
			a_caPositions = this.makeLinkagePosiiton(_donorGLIN.getAcceptorPositions());
			a_cdPositions = this.makeLinkagePosiiton(_donorGLIN.getDonorPositions());

			this.startGRES = acceptorGRES;

			// sub->EndRep or sugar->EndRep
			this.endSideRepLinkage = new Linkage(null, bridge, a_cdPositions);
			this.endSideRepLinkage.setAnomericCarbon(a_cdPositions[0]);
		}
	}

	/*
	private void analyzeEndSideRep(GLIN _donorGLIN) {
		this.max = _donorGLIN.getRepeatCountMax();
		this.min = _donorGLIN.getRepeatCountMin();
		char[] a_caPositions = this.makeLinkagePosiiton(_donorGLIN.getAcceptorPositions());
		char[] a_cdPositions = this.makeLinkagePosiiton(_donorGLIN.getDonorPositions());

		if(_donorGLIN.getMAP().equals("")) {
			// 20220831 S.TSUCHIYA changed, make start-side rep
			this.endSideRepLinkage = new Linkage(null, null, a_caPositions);
			this.endSideRepLinkage.setAnomericCarbon(a_cdPositions[0]);
		} else {
			SUBSTAnalyzer a_oSUBSTAnalyzer = new SUBSTAnalyzer();
			Linkage a_oLIN;
			try {
				Residue a_oSUB = a_oSUBSTAnalyzer.MAPToBridge(_donorGLIN);

				// sugar->sub
				a_oLIN = new Linkage(this.acceptorRES, a_oSUB, a_caPositions);
				this.acceptorLinkages.add(a_oLIN);

				// sub->EndRep
				this.endSideRepLinkage = new Linkage(null, a_oSUB, a_cdPositions);
				this.endSideRepLinkage.setAnomericCarbon(a_cdPositions[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (_donorGLIN.getAcceptor().getFirst().getID() > _donorGLIN.getDonor().getFirst().getID()) {
			this.startGRES = _donorGLIN.getDonor().getFirst();
		} else {
			this.startGRES = _donorGLIN.getAcceptor().getFirst();
		}
	}
	 */

	private void analyzeStartSideCyclic(GLIN _donorGLIN) {
		char[] a_caPositions = this.makeLinkagePosiiton(_donorGLIN.getAcceptorPositions());
		char[] a_cdPositions = this.makeLinkagePosiiton(_donorGLIN.getDonorPositions());

		this.a_oCyclicParentLinkage = new Linkage(null, null, a_caPositions);
		this.a_oCyclicParentLinkage.getBonds().get(0).setChildPosition(a_cdPositions[0]);
	}

	private void analyzeEndSideCyclic(GLIN _acceptorGLIN) {
		char[] a_caPositions = this.makeLinkagePosiiton(_acceptorGLIN.getAcceptorPositions());
		char[] a_cdPositions = this.makeLinkagePosiiton(_acceptorGLIN.getDonorPositions());
		this.a_oCyclicChildLinkage = new Linkage(null, null, a_caPositions);
		this.a_oCyclicChildLinkage.getBonds().get(0).setChildPosition(a_cdPositions[0]);
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

	private boolean glinIsCyclic (GLIN _glin) {
		if (_glin.getAcceptor().isEmpty() || _glin.getDonor().isEmpty()) return false;
		return (_glin.getAcceptor().getFirst().getID() > _glin.getDonor().getFirst().getID());
	}
}