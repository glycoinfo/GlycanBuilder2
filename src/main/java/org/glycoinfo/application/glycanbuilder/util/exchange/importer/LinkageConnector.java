package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;

public class LinkageConnector {

	private final Residue donor;
	private final Residue acceptor;
	private final Residue start;

	/**
	 *
	 * @param _donor
	 * @param _acceptor
	 * @param _start
	 */
	public LinkageConnector(Residue _donor, Residue _acceptor, Residue _start) {
		this.donor = _donor;
		this.acceptor = _acceptor;
		this.start = _start;
	}

	/**
	 *
	 * @param _glin2linkage
	 */
	public void start(GLINToLinkage _glin2linkage) {
		Residue donor = this.donor;
		Residue substituent = null;

		// set parent linkage for current residue
		if(!_glin2linkage.getParentLinkage().isEmpty()) {
			if(_glin2linkage.getBridgeLinkage() != null) {
				donor.setParentLinkage(_glin2linkage.getBridgeLinkage());
				//substituent = _glin2linkage.getBridgeLinkage().getSubstituent();
				substituent = _glin2linkage.getBridgeLinkage().getParentResidue();
				substituent.setParentLinkage(_glin2linkage.getParentLinkage().get(0));
			}else {
				donor.setParentLinkage(_glin2linkage.getParentLinkage().getLast());
			}
		}else if(_glin2linkage.getStartCyclicLinkage() != null) {
			donor.setParentLinkage(_glin2linkage.getStartCyclicLinkage());
		}else if(_glin2linkage.getStartSideRepLinkage() != null) {
			donor.setParentLinkage(_glin2linkage.getStartSideRepLinkage());
		}

		if(_glin2linkage.getDonorGLINs().isEmpty()) {
			//　start-rep is root node
			this.analyzeBracketNotation (donor, null, _glin2linkage);
			return;
		}
		if(_glin2linkage.getParents().size() > 1) return;

		if(!_glin2linkage.isCyclic() && !_glin2linkage.isRepeating()) {
			// add child edge between child and parent residues
			if(this.isOutRepeating(this.donor, this.acceptor)) {
				// make glycosidic bond out of repating : sugar<-EndRep
				this.acceptor.getEndRepitionResidue().addChild(donor, donor.getParentLinkage().getBonds());
			} else {
				if(substituent != null) {
					// make glycosidic bond with substituent : sub<-sugar
					this.acceptor.addChild(substituent, substituent.getParentLinkage().getBonds());

					// make glicosidic bond with monosaccharide : sugar<-sub
					substituent.addChild(donor, donor.getParentLinkage().getBonds());
				}else {
					// make glycosidic bond : sugar<-sugar
					this.acceptor.addChild(donor, donor.getParentLinkage().getBonds());
				}
			}
		}else {
			this.analyzeBracketNotation(donor, substituent, _glin2linkage);
		}
	}

	private void analyzeBracketNotation (Residue a_oRES, Residue a_oBridge, GLINToLinkage a_oG2L) {
		// define end repeating bracket
		if(a_oG2L.getEndSideRepLinkage() != null) {
			this.makeEdgeWithEndBracket(a_oG2L, a_oRES);
		}

		// define end cyclic bracket
		if(a_oG2L.getEndCyclicLinkage() != null) {
			this.makeEdgeWithEndCyclic(a_oG2L, a_oRES);
		}

		// define start repeating bracket
		if(a_oG2L.getStartSideRepLinkage() != null) {
			this.makeEdgeWithStartBracket(a_oG2L, a_oRES);
		}

		// define start cyclic bracket
		if(a_oG2L.getStartCyclicLinkage() != null) {
			this.makeEdgeWithStartCyclic(a_oRES);
		}

		// for repetition contains single monosaccharide
		if (this.donor.equals(this.acceptor)) return;

		// make glycosidic linkage for outside of repetition
		// undefined linkage positions -> acceptor, defined linkage positions -> donor
		// 20220901 S.TSUCHIYA changed
		if(a_oRES.getStartRepetitionResidue() != null && a_oRES.getStartCyclicResidue() == null && this.acceptor != null) {
			//if(a_oRES.getStartRepetitionResidue() != null && a_oRES.getStartCyclicResidue() == null && a_oG2L.getDonorGLINs().size() > 1) {
			//Residue acceptor = this.acceptor;
			Residue a_oStartRep = a_oRES.getStartRepetitionResidue();
			if(this.isOutRepeating(a_oStartRep, acceptor)) {
				// repeat connect to other repeat : sugar<-StartRep<-EndRep<-sugar
				acceptor.getEndRepitionResidue().addChild(a_oStartRep, a_oStartRep.getParentLinkage().getBonds());
			} else {
				// start repeat connect to monosaccharide : StartRep<-sugar
				acceptor.addChild(a_oStartRep, a_oStartRep.getParentLinkage().getBonds());
			}
		}else if(a_oRES.getStartCyclicResidue() == null && !a_oG2L.getParents().isEmpty()){
			//Residue acceptor = this.acceptor;
			if(this.isOutRepeating(a_oRES, acceptor)) {
				// end repeating bracket connect to monosaccharide : sugar<-EndRep
				acceptor.getEndRepitionResidue().addChild(a_oRES, a_oRES.getParentLinkage().getBonds());
			} else {
				if(a_oBridge == null) {
					/* monosaccharide(in rep) to monosacchadie(in rep) :
					 * EndRep
					 *   \
					 *    sugar<-
					 *   /
					 *  sugar (This one)
					 *
					 *  some monosaccharide have end repeating bracket
					 *  EndRep<-sugar<-sugar<-
					 */
					if(!a_oRES.equals(acceptor)) acceptor.addChild(a_oRES, a_oRES.getParentLinkage().getBonds());
				} else {
					/*
					 * end repeating node make bridge for other monosaccharide in repeating
					 * EndRep<-sugar<-bridge<-sugar<-
					 */
					acceptor.addChild(a_oBridge, a_oBridge.getParentLinkage().getBonds());
					a_oBridge.addChild(a_oRES, a_oRES.getParentLinkage().getBonds());
				}
			}
		}
	}

	/**
	 *
	 * @param a_oG2L
	 * @param a_oRES
	 */
	private void makeEdgeWithStartBracket(GLINToLinkage a_oG2L, Residue a_oRES) {
		Residue a_oStartRep = ResidueDictionary.createStartRepetition();
		a_oStartRep.setParentLinkage(a_oG2L.getStartSideRepLinkage());
		a_oRES.setStartRepetiionResidue(a_oStartRep);
		a_oStartRep.setAnomericCarbon(a_oRES.getAnomericCarbon());
		// start repeating bracket to residue : StartRep->sugar
		a_oStartRep.addChild(a_oRES, a_oRES.getParentLinkage().getBonds());
		//a_oStartRep.getParentLinkage().setLinkagePositions(a_oStartRep.getParentLinkage().getBonds());
	}

	/**
	 *
	 * @param a_oG2L
	 * @param a_oRES
	 */
	private void makeEdgeWithEndBracket(GLINToLinkage a_oG2L, Residue a_oRES) {
		Residue a_oEndRep = ResidueDictionary.createEndRepetition(
				a_oG2L.getMinRepeatingCount(), a_oG2L.getMaxRepeatingCount());
		Residue a_oSUB;

		// set parameter
		a_oEndRep.setParentLinkage(a_oG2L.getEndSideRepLinkage());
		a_oEndRep.setStartResidue(this.start);
		a_oRES.setEndRepitionResidue(a_oEndRep);

		// sugar->sub->EndRep
		if(a_oG2L.getEndSideRepLinkage().getChildResidue() != null) {
			a_oSUB = a_oG2L.getEndSideRepLinkage().getChildResidue();

			// sugar to substituent
			a_oSUB.setParentLinkage(a_oG2L.getParentLinkage().get(0));
			a_oRES.addChild(a_oSUB, a_oSUB.getParentLinkage().getBonds());

			// substituent to end repeating : sub->EndRep
			a_oSUB.addChild(a_oEndRep, a_oEndRep.getParentLinkage().getBonds());
		} else {
			// end repeating to residue : sugar->EndRep
			a_oRES.addChild(a_oEndRep, a_oEndRep.getParentLinkage().getBonds());
		}
	}

	/**
	 * make edge between start cyclic and residue or start repeating bracket
	 * start cyclic to residue : sugar<-StartCyclic
	 * start cyclic to start repeating bracket : sugar<-StartRep<-StartCyclic
	 * @param a_oRES
	 */
	private void makeEdgeWithStartCyclic(Residue a_oRES) {
		Residue a_oStartCyclic = ResidueDictionary.createStartCyclic();
		a_oRES.setStartCyclicResidue(a_oStartCyclic);

		if(a_oRES.getStartRepetitionResidue() != null) {
			a_oStartCyclic.addChild(a_oRES.getStartRepetitionResidue(), a_oRES.getStartRepetitionResidue().getParentLinkage().getBonds());
		}else {
			a_oStartCyclic.addChild(a_oRES, a_oRES.getParentLinkage().getBonds());
		}
	}

	/**
	 * make edge to end cyclic bracket
	 * residue to end cyclic bracket : EndCyclic<-sugar
	 * end repeating to end cyclic bracket : EndCyclic<-EndRep<-sugar
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
	}

	/**
	 *
	 * @param _donor
	 * @param _acceptor
	 * @return
	 */
	private boolean isOutRepeating(Residue _donor, Residue _acceptor) {
		if(_acceptor.getEndRepitionResidue() == null) return false;

		String donorPos = _donor.getParentLinkage().getParentPositionsString();
		String acceptorPos = _acceptor.getEndRepitionResidue().getParentLinkage().getParentPositionsString();

		if(donorPos.contains("/") && acceptorPos.contains("/")) {
			if(donorPos.equals(acceptorPos)) return true;
			donorPos = donorPos.substring(0, 1);
			acceptorPos = acceptorPos.substring(0, 1);
		}

		if (donorPos.equals("?") || acceptorPos.equals("?")) return false;
		return (Integer.parseInt(donorPos) == Integer.parseInt(acceptorPos));
	}
}