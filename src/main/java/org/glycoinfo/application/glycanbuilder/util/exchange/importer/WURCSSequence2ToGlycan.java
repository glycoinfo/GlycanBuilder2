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

import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;
import org.glycoinfo.WURCSFramework.util.oldUtil.ConverterExchangeException;

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

		// A bond from anomeric carbon to anomeric carbon - sucrose, the trehaloses - is taken out of the
		// sequence before the bonds are read, and made last (#357). Left in, the residue chosen as the
		// root was one end of it and the other, which goes on the root's anomeric carbon, was dropped:
		// sucrose came in as its fructose alone, and was refused as read in part. Not before the
		// fragments are found: a residue of unknown anomer with no bond at all is a composition's.
		GLIN anomeric = gres2frag.getRootOfCompositions().isEmpty() ? takeOutTheBondBetweenAnomericCarbons(wSeq2) : null;

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
		if(anomeric != null) joinTheAnomericCarbons(anomeric);
		refuseARepeatLeftOpen(this.glycan);

		// append ambiguous root
		for(GRES gres : gres2frag.getRootOfFragments()) {
			Residue fragRoot = this.gres2residue.get(gres);
			this.detachFromNamedParent(fragRoot);
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

			// Monosaccharides with no linkage at all between them are separate molecules, not one glycan,
			// and GlycanBuilder2 does not read them (the maintainer, 2026-11-29). They were read as a
			// composition with a "no glycosidic linkages" label hung off it, which nothing wrote back: the
			// export refused the label as a residue. A composition is written with its residues joined by
			// linkages of unknown position, a?|b?}-{a?|b?, as GlyTouCan holds every composition.
			if (this.isCompositionWithoutLinkage(this.glycan)) {
				int monosaccharides = this.glycan.getBracket().getChildrenLinkages().size();
				throw new WURCSToGlycanException("this WURCS has " + monosaccharides + " monosaccharides and no linkage"
						+ " between them, which makes them separate molecules rather than one glycan. A composition is"
						+ " written with its residues joined by linkages of unknown position (a?|b?}-{a?|b?); separate"
						+ " monosaccharides are not read");
			}

			this.glycan.normalizeCompositionFlags();
		}
	}

	/**
	 * Refuse a structure with a repeat whose end could not be placed, saying so (#264).
	 *
	 * <p>A repeat ends with a marker on its last residue, at the carbon the next unit is bonded to. Two
	 * repeats closing on the same carbon - G31898IW's b1-b4~5-6 inside a1-b4~n, both on b4 - leave the
	 * outer one no carbon for its end, and it was read with a start and no end: drawn with the residues
	 * around it misplaced, weighed by a count it does not have, and written back with the repeat gone.
	 */
	static void refuseARepeatLeftOpen(Glycan _glycan) throws WURCSToGlycanException {
		// Read from the tree, a start marker not always being told of its end while reading; and walked
		// here, each residue once, since a sequence that loops back on itself is refused only later and
		// getAllResidues would not come back from it
		java.util.Set<Residue> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Residue, Boolean>());
		java.util.Deque<Residue> toVisit = new java.util.ArrayDeque<Residue>();
		if (_glycan.getRoot() != null) toVisit.push(_glycan.getRoot());
		int starts = 0, ends = 0;
		Residue open = null;
		while (!toVisit.isEmpty()) {
			Residue residue = toVisit.pop();
			if (!seen.add(residue)) continue;
			if (residue.isEndRepetition()) ends++;
			if (residue.isStartRepetition()) {
				starts++;
				if (open == null && !endsBelow(residue)) open = residue;
			}
			for (org.eurocarbdb.application.glycanbuilder.linkage.Linkage child : residue.getChildrenLinkages())
				toVisit.push(child.getChildResidue());
		}
		if (open == null && starts == ends) return;
		Residue first = (open != null) ? open.firstChild() : null;
		throw new WURCSToGlycanException("a repeat" + (first == null ? "" : " starting at " + first.getTypeName())
				+ " could not be closed: its last residue's carbon is taken by another repeat's end, and GlycanBuilder2"
				+ " cannot hold two repeats closing on one carbon, so it is not read");
	}

	/** Whether an end marker is below this start marker, each residue looked at once. */
	private static boolean endsBelow(Residue _start) {
		java.util.Set<Residue> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Residue, Boolean>());
		java.util.Deque<Residue> toVisit = new java.util.ArrayDeque<Residue>();
		toVisit.push(_start);
		while (!toVisit.isEmpty()) {
			Residue residue = toVisit.pop();
			if (!seen.add(residue)) continue;
			for (org.eurocarbdb.application.glycanbuilder.linkage.Linkage child : residue.getChildrenLinkages()) {
				if (child.getChildResidue().isEndRepetition()) return true;
				toVisit.push(child.getChildResidue());
			}
		}
		return false;
	}

	/**
	 * Take an antenna out of the tree it was provisionally linked into, so that the bracket
	 * is its only parent.
	 *
	 * <p>A GRES whose attachment point is undetermined names several possible acceptors -
	 * {@code m1-f?|i?|k?} in G42735RP. {@link #analyzeGLIN} has no way to choose between
	 * them and links the residue to the first, then records every candidate with
	 * {@link Residue#addParentOfFragment}; the same residue then becomes an antenna, a
	 * child of the bracket. It ends up with two parents at once - a shape no tree can hold
	 * - and everything that walks the structure meets it twice: the renderer lays it out
	 * under the bracket and then translates it a second time along with the other parent's
	 * subtree, which carries it clean out of the picture the renderer has sized, and the
	 * writers put it in the sequence twice over (#71).</p>
	 *
	 * <p>The candidates are already recorded, so the provisional link has nothing left to
	 * say. The linkage itself is put back on the residue after the parent lets go of it:
	 * the bracket reads its positions and its linkage types when it adopts the residue.</p>
	 */
	private void detachFromNamedParent(Residue _fragRoot) {
		if (_fragRoot == null) return;
		Residue parent = _fragRoot.getParent();
		if (parent == null) return;

		Linkage provisional = _fragRoot.getParentLinkage();
		parent.removeChild(_fragRoot);
		_fragRoot.setParentLinkage(provisional);
	}

	private void analyzeGRES(GRES _gres) throws Exception {
		GRESToResidue gres2residue;
		try {
			gres2residue = new GRESToResidue();
			gres2residue.start(_gres);
		} catch (WURCSToGlycanException ex) {
			throw ex;
		} catch (ConverterExchangeException ex) {
			throw new WURCSToGlycanException(ex.getMessage(),ex);
		} catch (Exception ex) {
			// This is helpful when a burried class throws a bare Exception with useful information
			// about the WURCS conversion - this block should catch unexpected issues (NPE etc.) not known
			// glycan conversion issues.
			// ex.printStackTrace(); 
			throw new WURCSToGlycanException(WURCSToGlycanException.badResidueMessage,_gres.getMS().getString(),ex);
		}
		Residue residue = gres2residue.getResidue();
		this.gres2residue.put(_gres, residue);

		// add substituent as child residue
		SUBSTAnalyzer substAnalyzer = new SUBSTAnalyzer(gres2residue.getModifications());
		try {
			substAnalyzer.start(_gres, residue);
		} catch (WURCSToGlycanException ex) {
			throw ex;
		} catch (ConverterExchangeException ex) {
			throw new WURCSToGlycanException(ex.getMessage(),ex);
		} catch (Exception ex) {
			// This is helpful when a burried class throws a bare Exception with useful information
			// about the WURCS conversion - this block should catch unexpected issues (NPE etc.) not known
			// glycan conversion issues.
			// ex.printStackTrace(); 
			throw new WURCSToGlycanException(WURCSToGlycanException.badResidueMessage,_gres.getMS().getString(),ex);
		}
		
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
		GLINToLinkage glin2linkage;
		try {
			glin2linkage = new GLINToLinkage(this.gres2residue.get(_gres), _lins);
			glin2linkage.start(_gres);
		} catch (WURCSToGlycanException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new WURCSToGlycanException("Failed to convert linkage",ex);
		}
		

		// define glycosidic bond
		Residue donor = this.gres2residue.get(_gres);
		Residue acceptor = glin2linkage.getParents().size() > 0 ?
				this.gres2residue.get(glin2linkage.getParents().get(0)) : null;
		Residue start = this.gres2residue.get(glin2linkage.getStartRepeatingGRES());

		LinkageConnector linkageConnector;
		try {
			linkageConnector = new LinkageConnector(donor, acceptor, start);
			linkageConnector.start(glin2linkage);
		} catch (Exception ex) {
			throw new WURCSToGlycanException("Failed to convert linkage",ex);
		}

		// set parents for fragments
		if(glin2linkage.getParents().size() > 1) {
			for(GRES gres : glin2linkage.getParents()) {
				this.gres2residue.get(_gres).addParentOfFragment(this.gres2residue.get(gres));
			}
		}
	}

	/**
	 * The one bond in this sequence between two anomeric carbons, taken out of both residues' lists, or
	 * null when there is none or more than one. A tree has room for one: it uses up every anomeric
	 * carbon, the reducing end's included. With it gone both of its residues have their anomeric carbon
	 * free, and {@link #getRootResidue} takes the one the WURCS lists first - the maintainer's choice,
	 * 2026-11-28: glucose for sucrose as G44928ZS writes it, fructose for raffinose as its WURCS does.
	 */
	private static GLIN takeOutTheBondBetweenAnomericCarbons(WURCSSequence2 _seq) {
		GLIN found = null;
		for(GRES gres : _seq.getGRESs()) {
			for(GLIN glin : gres.getDonorGLINs()) {
				if(glin.isRepeat() || !glin.getMAP().equals("")) continue;
				if(glin.getDonor().size() != 1 || glin.getAcceptor().size() != 1) continue;
				if(glin.getDonorPositions().size() != 1 || glin.getAcceptorPositions().size() != 1) continue;
				if(glin.getDonor().getFirst() == glin.getAcceptor().getFirst()) continue;
				if(!new GLINToLinkage().isFacingBetweenAnomer(glin)) continue;
				if(found != null && found != glin) return null;
				found = glin;
			}
		}
		if(found == null) return null;
		found.getDonor().getFirst().getDonorGLINs().remove(found);
		found.getAcceptor().getFirst().getAcceptorGLINs().remove(found);
		return found;
	}

	/** The bond taken out, made: the residue the WURCS lists second on the first one's anomeric carbon. */
	private void joinTheAnomericCarbons(GLIN _glin) throws WURCSToGlycanException {
		GRES donor = _glin.getDonor().getFirst();
		GRES acceptor = _glin.getAcceptor().getFirst();
		GRES first = (donor.getID() < acceptor.getID()) ? donor : acceptor;
		GRES second = (first == donor) ? acceptor : donor;
		Residue root = this.gres2residue.get(first);
		Residue other = this.gres2residue.get(second);
		if(root == null || other == null || !root.joinAtAnomericCarbons(other))
			throw new WURCSToGlycanException("the bond between the anomeric carbons of " + first.getMS().getString()
					+ " and " + second.getMS().getString() + " could not be made, so this structure is not read");
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