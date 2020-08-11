package org.glycoinfo.application.glycanbuilder.util.exchange.importer.glycontainer2glycan;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Bond;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.glycoinfo.GlycanFormatconverter.Glycan.*;
import org.glycoinfo.GlycanFormatconverter.io.IUPAC.IUPACNotationConverter;

import java.util.ArrayList;
import java.util.HashMap;

public class GlyContainer2Glycan {

    private final HashMap<Node, Residue> residue2node;

    public GlyContainer2Glycan () {
        this.residue2node = new HashMap<>();
    }

    public Glycan start(GlyContainer _gc, MassOptions _massOpt) throws Exception {

        // make residue
        for (Node node : _gc.getAllNodes()) {
            this.residue2node.put(node, this.makeResidue(node));
            this.makeModification(node);
        }

        // make edge
        EdgeAnalyzer edgeAnalyzer = new EdgeAnalyzer(this.residue2node);
        for (Node node : _gc.getAllNodes()) {
            edgeAnalyzer.start(node);
        }

        Glycan ret;
        Residue root = this.residue2node.get(_gc.getRootNodes().get(0));

        if (root.getStartRepetitionResidue() != null) {
            // append start repetition to reducing end

            //TODO : cyclic-startrep-monosaccharideの扱い
            //TODO : cyclicが最優先になるようにする
            ret = new Glycan(root.getStartRepetitionResidue(), false, _massOpt);
        } else {
            Residue redEnd = new Residue(ResidueType.createFreeReducingEnd());
            redEnd.addChild(root);
            ret = new Glycan(redEnd, false, _massOpt);
        }

        // define fragments
        if (!_gc.getUndefinedUnit().isEmpty()) {
            ret.addBracket();
            Residue bracket = ret.getBracket();
            this.makeFragments(bracket, _gc.getUndefinedUnit());
        }

        System.out.println(ret);
        return ret;
    }

    private Residue makeResidue (Node _node) throws Exception {
        IUPACNotationConverter inConv = new IUPACNotationConverter();
        inConv.makeTrivialName(_node);
        String trivialName = inConv.getCoreCode();

        Residue residue = ResidueDictionary.newResidue(trivialName);
        residue.setWasSticky(isSticky(trivialName));
        residue.setAlditol(this.isAlditol(_node));
        residue.setAldehyde(this.isAldehyde(_node));
        residue.setAnomericCarbon(makeAnomericPosition(_node));
        residue.setAnomericState(this.makeAnomericSymbol(_node));
        residue.setChirality(this.makeChirality(_node));
        residue.setRingSize(residue.isAlditol() ? 'o' : this.makeRingSize(_node));

        return residue;
    }

    private void makeModification (Node _node) throws Exception {
        Residue residue = residue2node.get(_node);
        Edge2Bond edge2Bond = new Edge2Bond();

        // extract modification
        Monosaccharide mono = (Monosaccharide) _node;
        for (GlyCoModification gMod : mono.getModifications()) {
            if (gMod.getModificationTemplate().equals(ModificationTemplate.HYDROXYL)) continue;
            Residue mod = ResidueDictionary.newResidue(gMod.getModificationTemplate().getGlycoCTnotation());
            residue.addChild(mod, edge2Bond.startForModification(gMod.getPositionOne()));
        }
    }

    private boolean isSticky(String _trivialName) {
        return (_trivialName.equals("Fuc") || _trivialName.equals("Xyl"));
    }

    private char makeRingSize(Node _node) throws Exception {
        Monosaccharide mono = (Monosaccharide) _node;
        int ringStart = mono.getRingStart();
        int ringEnd = mono.getRingEnd();
        int anomericPosition = mono.getAnomericPosition();

        //1-4, 2-5 is franose
        //1-5, 2-6 is pyranose
        // 3 : anomeric position -> WURCS=2.0/1,1,0/[h2a1221h-3x_3-8]/1/
        if (anomericPosition == 3) {
            throw new Exception(anomericPosition + " is not support.");
        }
        if (anomericPosition != ringStart) {
            throw new Exception(anomericPosition + " and " + ringStart + " are incorrect.");
        }

        if (anomericPosition == 1) {
            if (ringEnd == 4) return 'f';
            if (ringEnd == 5) return 'p';
        }
        if (anomericPosition == 2) {
            if (ringEnd == 5) return 'f';
            if (ringEnd == 6) return 'p';
        }
        if (anomericPosition == 0) {
            return 'o';
        }

        return '?';
    }

    private boolean isAlditol(Node _node) {
        Monosaccharide mono = (Monosaccharide) _node;
        if (mono.getAnomericPosition() != 1) return false;

        for (GlyCoModification gMod : mono.getModifications()) {
            if (gMod.getPositionOne() == mono.getAnomericPosition() &&
                    gMod.getModificationTemplate().equals(ModificationTemplate.HYDROXYL)) return true;
        }

        return false;
    }

    private boolean isAldehyde(Node _node) {
        Monosaccharide mono = (Monosaccharide) _node;

        if (mono.getAnomericPosition() == -1) return false;

        for (GlyCoModification gMod : mono.getModifications()) {
            if (gMod.getPositionOne() == mono.getAnomericPosition() &&
                    gMod.getModificationTemplate().equals(ModificationTemplate.ALDEHYDE)) return true;
        }

        return false;
    }

    private char makeAnomericSymbol(Node _node) {
        Monosaccharide mono = (Monosaccharide) _node;
        AnomericStateDescriptor anomDesc = mono.getAnomer();
        if (anomDesc.equals(AnomericStateDescriptor.UNKNOWN) ||
                anomDesc.equals(AnomericStateDescriptor.UNKNOWN_STATE)) return '?';
        if (anomDesc.equals(AnomericStateDescriptor.ALPHA)) return 'a';
        if (anomDesc.equals(AnomericStateDescriptor.BETA)) return 'b';
        if (anomDesc.equals(AnomericStateDescriptor.OPEN)) return 'o';

        return '?';
    }

    private char makeAnomericPosition(Node _node) {
        Monosaccharide mono = (Monosaccharide) _node;
        int anomPosition = mono.getAnomericPosition();

        if (anomPosition == -1) {
            return '?';
        } else {
            return Integer.valueOf(anomPosition).toString().charAt(0);
        }
    }

    private char makeChirality (Node _node) {
        Monosaccharide mono = (Monosaccharide) _node;

        if (mono.getStereos().isEmpty()) return '?';

        String stereo = mono.getStereos().getFirst();

        if (stereo.length() == 4) {
            return stereo.substring(0, 1).toUpperCase().charAt(0);
        } else {
            return '?';
        }
    }

    private void makeFragments (Residue _bracket, ArrayList<GlycanUndefinedUnit> _und) throws Exception {
        Edge2Bond e2b = new Edge2Bond();

        for (GlycanUndefinedUnit und : _und) {
            Residue root = null;
            if (und.getRootNodes().get(0) instanceof Substituent) {
                Substituent rootSub = (Substituent) und.getRootNodes().get(0);
                root = ResidueDictionary.newResidue(rootSub.getSubstituent().getIUPACnotation());
            } else {
                root = residue2node.get(und.getRootNodes().get(0));
            }

            ArrayList<Bond> bond = e2b.startForNormalLinkage(und.getConnection());
            Linkage coreSide = new Linkage(_bracket, root, bond);

            //_bracket.addChild(root);
            root.setParentLinkage(coreSide);
            //TODO : core側の情報を定義する必要がある
        }
    }

    private void checkResidue (Residue _residue) {
        System.out.println(_residue.getType());
        System.out.println(_residue.isAlditol());
        System.out.println(_residue.isAldehyde());
        System.out.println(_residue.getAnomericCarbon());
        System.out.println(_residue.getAnomericState());
        System.out.println(_residue.getChirality());
        System.out.println(_residue.getRingSize());
        System.out.println("");
    }
}