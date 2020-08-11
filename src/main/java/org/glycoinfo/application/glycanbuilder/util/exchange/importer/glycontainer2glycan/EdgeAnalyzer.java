package org.glycoinfo.application.glycanbuilder.util.exchange.importer.glycontainer2glycan;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Bond;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.GlycanFormatconverter.Glycan.*;
import org.glycoinfo.GlycanFormatconverter.util.TrivialName.ModifiedMonosaccharideDescriptor;
import org.glycoinfo.GlycanFormatconverter.util.TrivialName.ThreeLetterCodeConverter;
import org.glycoinfo.GlycanFormatconverter.util.TrivialName.TrivialNameDictionary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class EdgeAnalyzer {

    private final HashMap<Node, Residue> residue2node;

    public EdgeAnalyzer (HashMap<Node, Residue> _residue2node) {
        residue2node = _residue2node;
    }

    public void start (Node _acceptor) throws Exception {
        Residue acceptor = residue2node.get(_acceptor);
        Edge2Bond edge2Bond = new Edge2Bond();

        // remove core substituent
        //TODO : native substituentを捕捉しない条件を追加する
        NodeAnalyzer nodeAnalyzer = new NodeAnalyzer();
        _acceptor = nodeAnalyzer.start(_acceptor, acceptor);

        for (Edge edge : _acceptor.getChildEdges()) {
            Residue donor = residue2node.get(edge.getChild());
            Substituent sub = null;

            if (edge.getSubstituent() != null) {
                sub = (Substituent) edge.getSubstituent();
            }

            if (sub != null) {

                // extract simple substituent
                if (sub.getSubstituent() instanceof BaseSubstituentTemplate) {
                    Residue donorSub = this.makeSubstituent(sub);

                    ArrayList<Bond> bonds = edge2Bond.startForNormalLinkage(edge);

                    //TODO : LinkageTypeの設定
                    acceptor.addChild(donorSub, bonds);
                    donorSub.getParentLinkage().setChildLinkageType(org.eurocarbdb.MolecularFramework.sugar.LinkageType.UNVALIDATED);
                    donorSub.getParentLinkage().setParentLinkageType(org.eurocarbdb.MolecularFramework.sugar.LinkageType.UNVALIDATED);
                }

                // extract repeating unit
                if (sub instanceof GlycanRepeatModification) {
                    this.makeRepeatingUnit(edge, sub, acceptor, donor);
                    continue;
                }

                // extract cross-linked substituent
                if (sub.getSubstituent() instanceof BaseCrossLinkedTemplate) {
                    // donor : monosaccharide
                    // acceptor : monosaccharide
                    if (edge.getChild() != null) {
                        donor = residue2node.get(edge.getChild());
                        acceptor = residue2node.get(edge.getParent());
                        this.makeCrossLinkedSubstituent(donor, this.makeSubstituent(sub), acceptor, edge2Bond.startForBridgeLinkage(edge));
                    } else {
                        this.makeCyclicSubstituent(acceptor, this.makeSubstituent(sub), edge2Bond.startForCyclicSubLinkage(edge));
                    }
                }
                continue;
            }

            // extract monosaccharide
            if (edge.getChild() != null) {
                Collection bonds = edge2Bond.startForNormalLinkage(edge);

                //TODO : LinkageTypeの設定
                acceptor.addChild(donor, bonds);
                donor.getParentLinkage().setChildLinkageType(org.eurocarbdb.MolecularFramework.sugar.LinkageType.UNVALIDATED);
                donor.getParentLinkage().setParentLinkageType(org.eurocarbdb.MolecularFramework.sugar.LinkageType.UNVALIDATED);
            }
        }
    }

    private void makeRepeatingUnit (Edge _edge, Substituent _sub, Residue _acceptor, Residue _donor) throws Exception {
        Edge2Bond edge2Bond = new Edge2Bond();
        GlycanRepeatModification repMod = (GlycanRepeatModification) _edge.getSubstituent();
        ArrayList<Bond> bonds = edge2Bond.startForNormalLinkage(_edge);
        Residue startRep = null;
        Residue endRep = null;

        if (_edge.isRepeat()) {
            startRep = new Residue(ResidueType.createStartRepetition());
            endRep = new Residue(ResidueType.createEndRepetition());
        }
        if (_edge.isCyclic()) {
            startRep = new Residue(ResidueType.createStartCyclic());
            endRep = new Residue(ResidueType.createEndCyclic());
        }

        endRep.setMaxRepetitions(String.valueOf(repMod.getMaxRepeatCount()));
        endRep.setMinRepetitions(String.valueOf(repMod.getMinRepeatCount()));

        if (_sub.getSubstituent() != null) {
            // with cross-linked substituent
            // donor : start rep node
            // acceptor : start monosaccharide
            Residue bridge = this.makeSubstituent(_sub);
            this.makeCrossLinkedSubstituent(_acceptor, bridge, startRep, edge2Bond.startForBridgeLinkage(_edge));
        }
        _acceptor.setEndRepitionResidue(endRep);
        _acceptor.addChild(endRep, bonds);
        _donor.setStartRepetiionResidue(startRep);
        startRep.addChild(_donor, bonds);
    }

    private Residue makeSubstituent (Node _node) throws Exception {
        Substituent sub = (Substituent) _node;
        Residue residue = ResidueDictionary.newResidue(sub.getSubstituent().getIUPACnotation());
        return residue;
    }

    private void makeCyclicSubstituent (Residue _acceptor, Residue _sub, ArrayList<char[]> _bonds) {
        //TODO : LinkageTypeの設定
        _acceptor.addChild(_sub);

        org.eurocarbdb.application.glycanbuilder.linkage.Linkage toCyclic = new org.eurocarbdb.application.glycanbuilder.linkage.Linkage(_acceptor, _sub, _bonds.get(2), _bonds.get(1), _bonds.get(0)[0]);
        _sub.setParentLinkage(toCyclic);
    }

    private void makeCrossLinkedSubstituent (Residue _donor, Residue _bridge, Residue _acceptor, ArrayList<ArrayList<Bond>> _bonds) {
        _acceptor.addChild(_bridge);
        _bridge.addChild(_donor);

        org.eurocarbdb.application.glycanbuilder.linkage.Linkage toBridge = new org.eurocarbdb.application.glycanbuilder.linkage.Linkage(_acceptor, _bridge, _bonds.get(0));
        org.eurocarbdb.application.glycanbuilder.linkage.Linkage toMonosaccharide = new Linkage(_bridge, _donor, _bonds.get(1));

        _donor.setParentLinkage(toMonosaccharide);
        _bridge.setParentLinkage(toBridge);
    }
}
