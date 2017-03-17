package org.glycoinfo.application.glycanbuilder.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;

public class GlycanUtils {
	
	LinkedList<Residue> a_aResidues = new LinkedList<Residue>();
	
	public LinkedList<Residue> getCoreResidues() {
		return this.a_aResidues;
	}
	
	public void getCoreResidue(Collection<Glycan> a_aGlycans) {
		Residue a_oRootRES = a_aGlycans.iterator().next().getRoot();
		
		this.getCoreResidue(a_oRootRES);
		return;
	}
	
	public static boolean isCollisionLinkagePosition (Residue a_oResidue) {
		if(a_oResidue.getChildrenLinkages().size() < 1) return false;
		boolean a_bIsCollision = false;
		HashMap<String, Integer> a_mapCount = new HashMap<String, Integer>();
		
		for(Linkage a_oLinkage : a_oResidue.getChildrenLinkages()) {
			String a_sPosition = a_oLinkage.getParentPositionsString();
			if(/*!a_oLinkage.getChildResidue().isSaccharide() || */a_sPosition.equals("?")) continue;
			
			if(!a_mapCount.containsKey(a_sPosition))
				a_mapCount.put(a_sPosition, 1);
			else {
				a_bIsCollision = true;
			}
		}
		
		return a_bIsCollision;
	}
	
	public static boolean isFacingAnom (Residue a_oRES) {
		boolean ret = false;
		
		if(a_oRES.hasParent() && !a_oRES.getParent().isReducingEnd()) return ret;
		
		for(Linkage a_oLIN : a_oRES.getChildrenLinkages()) {
			if(!a_oLIN.getChildResidue().isSaccharide() && 
					!a_oLIN.getChildResidue().getType().getSuperclass().equals("Bridge")) continue;
			if(a_oLIN.getChildPositionsSingle() == a_oLIN.getChildResidue().getAnomericCarbon() && 
					a_oLIN.getParentPositionsSingle() == a_oRES.getAnomericCarbon())
				ret = true;
		}
		
		return ret;
	}
	
	public static boolean isShowRedEnd (Glycan a_oGlycan, GraphicOptions theGraphicOptions, boolean show_redend) {
		Residue a_oRoot = a_oGlycan.getRoot().firstChild();
		boolean ret = show_redend;
		
		if(a_oRoot.isSaccharide() && isFacingAnom(a_oRoot)) ret = false;
		if(a_oRoot.isStartCyclic()) ret = false;
		if(theGraphicOptions.NOTATION.equals(GraphicOptions.NOTATION_SNFG)) {
			if(a_oGlycan.getRoot().firstChild().isAlditol()) ret = false;
		}
		
		return ret;
	}
	
	/**local utility*/
	private void getCoreResidue(Residue a_oResidue) {	
		if(a_oResidue != null) this.a_aResidues.addLast(a_oResidue);
		
		for(Linkage a_objLinkage : a_oResidue.getChildrenLinkages()) {
			if(!a_objLinkage.getChildResidue().isSaccharide()) continue;
			this.getCoreResidue(a_objLinkage.getChildResidue());
		}
	}
}
