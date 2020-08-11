package org.glycoinfo.application.glycanbuilder.util.exchange.importer.glycontainer2glycan;

import org.eurocarbdb.application.glycanbuilder.linkage.Bond;
import org.glycoinfo.GlycanFormatconverter.Glycan.Edge;
import org.glycoinfo.GlycanFormatconverter.Glycan.Linkage;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Edge2Bond {

    public Edge2Bond () {

    }


    public ArrayList<Bond> startForNormalLinkage (Edge _edge) {
        // for normal linkage
        ArrayList<Bond> bonds = new ArrayList<>();
        for (org.glycoinfo.GlycanFormatconverter.Glycan.Linkage gfcLin : _edge.getGlycosidicLinkages()) {
            Bond bond = new Bond();
            bond.setProbabilityHigh(gfcLin.getParentProbabilityUpper());
            bond.setProbabilityLow(gfcLin.getParentProbabilityLower());
            bond.setChildPosition(this.int2char(gfcLin.getChildLinkages().get(0)));
            bond.setParentPositions(this.convertAcceptroPositin(gfcLin.getParentLinkages()));

            bonds.add(bond);
        }
        return bonds;
    }

    public ArrayList<ArrayList<Bond>> startForBridgeLinkage (Edge _edge) {
        ArrayList<ArrayList<Bond>> ret = new ArrayList<>();
        Linkage lin = _edge.getGlycosidicLinkages().get(0);

        /*
         * 0 : donor <- bridge
         * 1 : bridge <- acceptor
         * donor-childLinkages-firstPosition-bridge-secondPosition-parentLinkages-acceptor
         *       childPosition parentPosition       childPosition  parentPosition
         */
        ArrayList<Bond> b2d = new ArrayList<>();
        Bond bridgeToDonor = new Bond();
        bridgeToDonor.setParentPositions(this.convertAcceptroPositin(new ArrayList<Integer>(1)));
        bridgeToDonor.setChildPosition(this.int2char(lin.getChildLinkages().get(0)));
        b2d.add(bridgeToDonor);

        ArrayList<Bond> a2b = new ArrayList<>();
        Bond acceptorToBridge = new Bond();
        acceptorToBridge.setParentPositions(this.convertAcceptroPositin(lin.getParentLinkages()));
        acceptorToBridge.setChildPosition(this.int2char(-1));
        a2b.add(acceptorToBridge);

        ret.add(b2d);
        ret.add(a2b);

        return ret;
    }

    public ArrayList<char[]> startForCyclicSubLinkage (Edge _edge) {
        /*
         * 0 : char[] link_poss (1st acceptor side)
         * 1 : char[] second_p_poss (2nd acceptor side)
         * 2 : char second_c_pos (2nd donor side)
         */
        ArrayList<char[]> bonds = new ArrayList<>();
        for (Linkage lin : _edge.getGlycosidicLinkages()) {
            if (_edge.getGlycosidicLinkages().indexOf(lin) == 0) {
                bonds.add(this.int2charArray(lin.getParentLinkages().get(0)));
            }
            if (_edge.getGlycosidicLinkages().indexOf(lin) == 1) {
                bonds.add(this.int2charArray(lin.getParentLinkages().get(0)));
                if (lin.getChildLinkages().get(0) == 0) {
                    bonds.add(this.int2charArray(-1));
                } else {
                    bonds.add(this.int2charArray(lin.getChildLinkages().get(0)));
                }
            }
        }

        return bonds;
    }

    public char startForModification (Integer _position) {
        return this.int2char(_position);
    }

    private char int2char (Integer _int) {
        char ret = '?';

        if (_int != -1) {
            ret = String.valueOf(_int).charAt(0);
        }
        return ret;
    }

    private char[] int2charArray (Integer _int) {
        char[] ret = new char[1];
        ret[0] = this.int2char(_int);
        return ret;
    }

    private char[] convertAcceptroPositin (ArrayList<Integer> _positions) {
        char[] ret = new char[_positions.size()];
        for (Integer pos : _positions) {
            if (pos == -1) {
                ret[_positions.indexOf(pos)] = '?';
            } else {
                ret[_positions.indexOf(pos)] = int2char(pos);
            }
        }
        return ret;
    }
}
