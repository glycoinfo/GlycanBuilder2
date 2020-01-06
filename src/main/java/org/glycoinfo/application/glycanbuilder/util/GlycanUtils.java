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
		if(a_oResidue.getChildrenLinkages().size() != 1) return false;
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

			char childPos = a_oLIN.getChildPositionsSingle();
			char childAnom = a_oLIN.getChildResidue().getAnomericCarbon();
			char parentPos = a_oLIN.getParentPositionsSingle();
			char parentAnom = a_oRES.getAnomericCarbon();
			boolean childSide = ((childPos != '?' && childAnom != '?') && (childPos == childAnom));
			boolean parentSide = ((parentPos != '?' && parentAnom != '?') && (parentPos == parentAnom));
			
			if(childSide && parentSide) ret = true;
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
	private void getCoreResidue(Residue _residue) {
		if(_residue != null && !_residue.isReducingEnd()) this.a_aResidues.addLast(_residue);
		
		for(Linkage linkage : _residue.getChildrenLinkages()) {
			if(!linkage.getChildResidue().isSaccharide()) continue;
			//if(_residue.getParent() != null && _residue.getParent().isBracket()) continue;
			this.getCoreResidue(linkage.getChildResidue());
		}
	}
}
