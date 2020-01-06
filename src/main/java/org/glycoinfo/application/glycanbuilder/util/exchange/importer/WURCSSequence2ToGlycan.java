package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

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

	private Glycan glycan;
	private HashMap<GRES, Residue> gres2residue;
	
	private GRESToFragment gres2frag;

	public WURCSSequence2ToGlycan () {
		this.gres2residue = new HashMap<GRES, Residue>();
		this.glycan = null;
		this.gres2frag = new GRESToFragment();
	}

	public Glycan getGlycan() {
		return this.glycan;
	}
	
	public void start(WURCSFactory _wf, MassOptions _massOpt) throws Exception {
		WURCSSequence2 wSeq2 = _wf.getSequence();
		WURCSArray wArray = _wf.getArray();
		
		// convert GRES to Residue
		for(GRES gres : wSeq2.getGRESs()) {
			this.analyzeGRES(gres);
			if(wSeq2.getGRESs().size() > 1)
				gres2frag.start(gres, this.gres2residue.get(gres));
		}

		for(GRES gres : wSeq2.getGRESs()) {
			this.analyzeGLIN(gres, wArray.getLINs());
		}
		
		this.glycan = new Glycan(makeRoot(wSeq2.getGRESs()), false, _massOpt);
		
		// append ambiguous root
		for(GRES gres : gres2frag.getRootOfFragments()) {
			Residue fragRoot = this.gres2residue.get(gres);
			this.glycan.addAntenna(fragRoot, fragRoot.getParentLinkage().getBonds());
		}
		
		// append ambiguous substituent
		for(GLIN glin : gres2frag.getSubstituentWithFragments()) {
			Residue fragRoot = gres2frag.getSubStituentFragment(glin);
			for(GRES gres : gres2frag.getChildren()) {
				fragRoot.addParentOfFragment(this.gres2residue.get(gres));
			}
			this.glycan.addAntenna(fragRoot, fragRoot.getParentLinkage().getBonds());
		}
		
		// append composition
		if(!gres2frag.getRootOfCompositions().isEmpty()) {
			this.glycan = Glycan.createComposition(_massOpt);
			for(GRES gres : gres2frag.getRootOfCompositions()) {
				Residue compoRoot = this.gres2residue.get(gres);
				compoRoot.isComposition(true);
				this.glycan.addAntenna(compoRoot);
			}			
		}
		
		return;
	}

	private void analyzeGRES(GRES _gres) throws Exception {
		GRESToResidue gres2residue = new GRESToResidue();
		
		gres2residue.start(_gres);
		Residue residue = gres2residue.getResidue();
		this.gres2residue.put(_gres, residue);
		
		// add substituent as child residue
		SUBSTAnalyzer substAnalyzer = new SUBSTAnalyzer(gres2residue.getModifications());
		substAnalyzer.start(_gres, residue);
		
		return;
	}
	
	private void analyzeGLIN(GRES _gres, LinkedList<LIN> _lins) throws Exception {
		GLINToLinkage glin2linkage = new GLINToLinkage(this.gres2residue.get(_gres), _lins);
		glin2linkage.start(_gres);
		
		// define glycosidic bond
		Residue donor = this.gres2residue.get(_gres);
		Residue acceptor = glin2linkage.getParents().size() > 0 ?
				this.gres2residue.get(glin2linkage.getParents().get(0)) : null;
		Residue start = this.gres2residue.get(glin2linkage.getStartRepeatingGRES());

		LinkageConnector linkageConnector =
				new LinkageConnector(donor, acceptor, start);
		
		linkageConnector.start(glin2linkage);

		// set parents for fragments
		if(glin2linkage.getParents().size() > 1) {
			for(GRES gres : glin2linkage.getParents()) {
				this.gres2residue.get(_gres).addParentOfFragment(this.gres2residue.get(gres));
			}
		}
		
		return;
	}
	
	private Residue makeRoot(LinkedList<GRES> _gress) throws Exception {
		// check type of reducing end
		Residue root = this.gres2residue.get(getRootResidue(_gress));
		Residue redEnd = null;
		
		if(root == null) return redEnd;
	
		
		redEnd = root.isAlditol() && root.getStartRepetitionResidue() == null ?
				ResidueDictionary.newResidue("redEnd") : ResidueDictionary.newResidue("freeEnd");
		
		if(root.getStartRepetitionResidue() != null) {
			redEnd.addChild(root.getStartRepetitionResidue(), root.getStartRepetitionResidue().getParentLinkage().getBonds());
		}else if (root.getStartCyclicResidue() != null){
			redEnd.addChild(root.getStartCyclicResidue(), '?');
		}else {
			Linkage linkage = new Linkage(redEnd, root);
			linkage.setLinkagePositions(new char[] {root.getAnomericCarbon()});
			root.setParentLinkage(linkage);
			redEnd.addChild(root, root.getParentLinkage().getBonds());
		}
		
		return redEnd;
	}
	
	private GRES getRootResidue(LinkedList<GRES> _gress) {
		for(GRES gres : _gress) {
			if(this.gres2frag.getRootOfCompositions().contains(gres)) continue;
			
			if(gres.getDonorGLINs().isEmpty() && gres.getAcceptorGLINs().isEmpty())
				return gres;
			if(gres.getDonorGLINs().isEmpty())
				return gres;
			if(gres.getAcceptorGLINs().isEmpty()) continue;

			for(GLIN donorGLIN : gres.getDonorGLINs()) {
				if(gres.getDonorGLINs().size() == 1 && donorGLIN.isRepeat()) return gres;
				if(donorGLIN.isRepeat()) continue;
				GLIN acceptorGLIN = gres.getAcceptorGLINs().get(0);

				if(acceptorGLIN.getDonor().isEmpty()) continue;
				if(!new GLINToLinkage().isFacingBetweenAnomer(donorGLIN)) {
					if(acceptorGLIN.getAcceptor().get(0).getID() == 1 && donorGLIN.getDonor().get(0).getID() == 1)
						return gres;
					if(gres.getID() > donorGLIN.getAcceptor().get(0).getID()) {
						return gres;
					}
				}
			}			
		}
		
		return null;
	}
}
