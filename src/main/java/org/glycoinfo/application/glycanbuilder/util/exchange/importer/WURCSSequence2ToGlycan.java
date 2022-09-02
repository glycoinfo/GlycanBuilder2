package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.wurcs.array.LIN;
import org.glycoinfo.WURCSFramework.wurcs.array.WURCSArray;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GLIN;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.WURCSSequence2;

import java.util.HashMap;
import java.util.LinkedList;

public class WURCSSequence2ToGlycan {

	private Glycan glycan;
	private final HashMap<GRES, Residue> gres2residue;

	private final GRESToFragment gres2frag;

	public WURCSSequence2ToGlycan () {
		this.gres2residue = new HashMap<>();
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
				gres2frag.start(gres);
		}

		if (gres2frag.getRootOfCompositions().isEmpty()) {
			for (GRES gres : wSeq2.getGRESs()) {
				this.analyzeGLIN(gres, wArray.getLINs());
			}
		} else {
			for (GRES gres : wSeq2.getGRESs()) {
				this.analyzeCompositionGLIN(gres);
			}
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
			// substituent composition
			for (GLIN glin : gres2frag.getSubstituentWithFragments()) {
				Residue fragRoot = gres2frag.getSubStituentFragment(glin);
				for(GRES gres : gres2frag.getChildren()) {
					fragRoot.addParentOfFragment(this.gres2residue.get(gres));
				}
				this.glycan.addAntenna(fragRoot, fragRoot.getParentLinkage().getBonds());
			}

			// without linkage label
			if (this.isCompositionWithoutLinkage(this.glycan)) {
				Residue withoutLinkage = new Residue(ResidueType.createAssigned("no glycosidic linkages"));
				this.glycan.addAntenna(withoutLinkage);
			}
		}
	}

	private void analyzeGRES(GRES _gres) throws Exception {
		GRESToResidue gres2residue = new GRESToResidue();

		gres2residue.start(_gres);
		Residue residue = gres2residue.getResidue();
		this.gres2residue.put(_gres, residue);

		// add substituent as child residue
		SUBSTAnalyzer substAnalyzer = new SUBSTAnalyzer(gres2residue.getModifications());
		substAnalyzer.start(_gres, residue);
	}

	private void analyzeCompositionGLIN (GRES _gres) {
		for (GLIN acceptorGLIN : _gres.getAcceptorGLINs()) {
			for (GRES acceptor : acceptorGLIN.getAcceptor()) {
				/*
				Bond bond = new Bond();
				Residue acceptorRES = this.gres2residue.get(acceptor);
				Residue donorRES = this.gres2residue.get(_gres);
				Linkage lin = new Linkage(acceptorRES, donorRES, new char['?']);
				 */
				this.gres2residue.get(_gres).addParentOfFragment(this.gres2residue.get(acceptor));
			}
		}
	}

	private void analyzeGLIN(GRES _gres, LinkedList<LIN> _lins) throws Exception {
		GLINToLinkage glin2linkage = new GLINToLinkage(this.gres2residue.get(_gres), _lins);
		glin2linkage.start(_gres);

		// define glycosidic bond
		Residue donor = this.gres2residue.get(_gres);
		Residue acceptor = glin2linkage.getParents().size() > 0 ?
				this.gres2residue.get(glin2linkage.getParents().get(0)) : null;
		Residue start = this.gres2residue.get(glin2linkage.getStartRepeatingGRES());

		LinkageConnector linkageConnector = new LinkageConnector(donor, acceptor, start);
		linkageConnector.start(glin2linkage);

		// set parents for fragments
		if(glin2linkage.getParents().size() > 1) {
			for(GRES gres : glin2linkage.getParents()) {
				this.gres2residue.get(_gres).addParentOfFragment(this.gres2residue.get(gres));
			}
		}
	}

	private Residue makeRoot(LinkedList<GRES> _gress) throws Exception {
		// check type of reducing end
		Residue root = this.gres2residue.get(getRootResidue(_gress));
		Residue redEnd = null;

		if(root == null) return redEnd;


		redEnd = root.isAlditol() && root.getStartRepetitionResidue() == null ?
				ResidueDictionary.newResidue("redEnd") : ResidueDictionary.newResidue("freeEnd");

		// start-cyclic
		if (root.getStartCyclicResidue() != null) {
			return root.getStartCyclicResidue();
		}

		// start-rep is root
		if (root.getStartRepetitionResidue() != null && root.getStartCyclicResidue() == null) {
			redEnd.addChild(root.getStartRepetitionResidue(), root.getStartRepetitionResidue().getParentLinkage().getBonds());
			return redEnd;
		}

		Linkage linkage = new Linkage(redEnd, root);
		linkage.setLinkagePositions(new char[] {root.getAnomericCarbon()});
		root.setParentLinkage(linkage);
		redEnd.addChild(root, root.getParentLinkage().getBonds());

		/*
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
		 */

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

	private boolean isCompositionWithoutLinkage (Glycan _glycan) {
		if (!_glycan.isComposition()) return false;
		boolean ret = false;
		Residue bracket = _glycan.getBracket();
		for (Linkage fragEdge : bracket.getChildrenLinkages()) {
			Residue fragRoot = fragEdge.getChildResidue();
			if (fragRoot.getParentsOfFragment().isEmpty()) {
				ret = true;
				break;
			}
		}
		return ret;
	}
}