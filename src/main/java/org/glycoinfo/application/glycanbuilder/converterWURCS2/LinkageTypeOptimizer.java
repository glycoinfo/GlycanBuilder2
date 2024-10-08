package org.glycoinfo.application.glycanbuilder.converterWURCS2;

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
                    e.printStackTrace();
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
                        e.printStackTrace();
                    }
                }

                // bridge-monosaccharide
                if (acceptorLinkage.getChildResidue().isSaccharide() && acceptorLinkage.getParentResidue().isBridge()) {
                    try {
                        acceptorLinkage.setParentLinkageType(LinkageType.H_AT_OH);
                        acceptorLinkage.setChildLinkageType(LinkageType.H_AT_OH);
                    } catch (Exception e) {
                        e.printStackTrace();
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

	private LinkageType getSubstituentLinkageType(Residue res) {
		switch(res.getType().getCompositionClass()) {
			case "O-type":
			case "Organic":
				return LinkageType.H_AT_OH;
		}
		switch(res.getType().getName()) {
			case "P":
			case "S":
				return LinkageType.H_AT_OH;
		}
		return LinkageType.DEOXY;
	}

}
