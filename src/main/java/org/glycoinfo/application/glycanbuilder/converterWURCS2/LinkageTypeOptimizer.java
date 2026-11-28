package org.glycoinfo.application.glycanbuilder.converterWURCS2;

import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;

public class LinkageTypeOptimizer {

    public Glycan start (Glycan _glycan) {
        for (Residue res : _glycan.getAllResidues()) {
            // optimize substituent at monosaccharides
            if (res.isSubstituent()) {
                Linkage acceptorLinkage = res.getParentLinkage();
                LinkageType donorType = acceptorLinkage.getChildLinkageType();
                LinkageType acceptorType = acceptorLinkage.getParentLinkageType();
                if (!donorType.equals(LinkageType.UNVALIDATED) || !acceptorType.equals(LinkageType.UNVALIDATED)) continue;

                try {
                	LinkageType lTypeOnChild = getSubstituentLinkageType(res);
                    acceptorLinkage.setParentLinkageType(lTypeOnChild);
                    acceptorLinkage.setChildLinkageType(LinkageType.NONMONOSACCHARID);
                } catch (Exception e) {
                    LogUtils.report(e);
                }
            }

            // optimize substituent in linkages
            if (res.isSaccharide()) {
                Linkage acceptorLinkage = res.getParentLinkage();
                if (acceptorLinkage.getChildResidue() == null || acceptorLinkage.getParentResidue() == null) continue;

                LinkageType donorType = acceptorLinkage.getChildLinkageType();
                LinkageType acceptorType = acceptorLinkage.getParentLinkageType();
                if (!donorType.equals(LinkageType.UNVALIDATED) || !acceptorType.equals(LinkageType.UNVALIDATED)) continue;

                // monosaccharide-bridge
                if (acceptorLinkage.getChildResidue().isBridge() && acceptorLinkage.getParentResidue().isSaccharide()) {
                    try {
                        acceptorLinkage.setParentLinkageType(LinkageType.H_AT_OH);
                        acceptorLinkage.setChildLinkageType(LinkageType.H_AT_OH);
                    } catch (Exception e) {
                        LogUtils.report(e);
                    }
                }

                // bridge-monosaccharide
                if (acceptorLinkage.getChildResidue().isSaccharide() && acceptorLinkage.getParentResidue().isBridge()) {
                    try {
                        acceptorLinkage.setParentLinkageType(LinkageType.H_AT_OH);
                        acceptorLinkage.setChildLinkageType(LinkageType.H_AT_OH);
                    } catch (Exception e) {
                        LogUtils.report(e);
                    }
                }

                // monosaccharide-monosaccharide: an ordinary glycosidic bond, which had no branch
                // here at all and so stayed UNVALIDATED however the structure was read (#4). The
                // donor gives up the OH at its anomeric centre (DEOXY) and the acceptor keeps the
                // oxygen the bond is made through (H_AT_OH) - which is what the GlycoCT writer had
                // been assuming for an unvalidated bond all along, writing "1o(4+1)2d". Saying it
                // in the model rather than at one exporter is the point: every other reader of a
                // linkage type was reading a placeholder.
                if (acceptorLinkage.getSubstituent() == null
                        && acceptorLinkage.getChildResidue().isSaccharide()
                        && acceptorLinkage.getParentResidue().isSaccharide()) {
                    try {
                        // A bridge is a saccharide here too, so this is also the bond from a
                        // monosaccharide to a bridge it carries: made through the hydroxyl, unless
                        // the bridge takes the hydroxyl's place (#345)
                        acceptorLinkage.setParentLinkageType(acceptorLinkage.getChildResidue().isBridge()
                                ? sugarSideOf(acceptorLinkage.getChildResidue()) : LinkageType.H_AT_OH);
                        acceptorLinkage.setChildLinkageType(LinkageType.DEOXY);
                    } catch (Exception e) {
                        LogUtils.report(e);
                    }
                }

                if (acceptorLinkage.getSubstituent() == null) continue;

                // monosaccharide-bridge-monosaccharide
                if (acceptorLinkage.getChildResidue().isSaccharide() && acceptorLinkage.getParentResidue().isSaccharide()) {
                    //acceptorLinkage.setParentLinkageType();
                    //acceptorLinkage.setChildLinkageType();
                }
            }
        }

        return _glycan;
    }

	/**
	 * How a monosaccharide is bonded to a bridge it carries, on the monosaccharide's side. A bridge with
	 * an oxygen in its composition - a phosphate, a sulfate, an anhydro - is made through the
	 * monosaccharide's hydroxyl, which keeps its oxygen (H_AT_OH). A bridge with none - an imino, *N*,
	 * a thio, *S* - takes the hydroxyl's place (DEOXY): 1,5-dideoxy-1,5-imino-galactitol is galactitol
	 * with its 1- and 5-hydroxyls replaced by one NH. Made through the hydroxyls, it was weighed with
	 * both of their oxygens still there, 32 Da heavy (#345).
	 */
	static LinkageType sugarSideOf(Residue _bridge) {
		String composition = (_bridge == null || _bridge.getType() == null) ? null : _bridge.getType().getComposition();
		if (composition != null && !composition.isEmpty() && !composition.contains("O")) return LinkageType.DEOXY;
		return LinkageType.H_AT_OH;
	}

	private LinkageType getSubstituentLinkageType(Residue res) {
		// O-type and P/S-type substituents both attach through an oxygen (the
		// old "Organic" category, and "P"/"S" matched by name, have been folded
		// into these two respectively - see residue_types)
		switch(res.getType().getCompositionClass()) {
			case "O-type":
			case "P/S-type":
				return LinkageType.H_AT_OH;
		}
		return LinkageType.DEOXY;
	}

}
