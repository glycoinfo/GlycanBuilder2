package org.glycoinfo.application.glycanbuilder.convertutil;

import java.util.ArrayList;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.util.WURCSDataConverter;
import org.glycoinfo.WURCSFramework.util.residuecontainer.LinkageBlock;
import org.glycoinfo.WURCSFramework.util.residuecontainer.RepeatingBlock;
import org.glycoinfo.WURCSFramework.util.residuecontainer.ResidueContainer;
import org.glycoinfo.WURCSFramework.util.residuecontainer.RootStatusDescriptor;
import org.glycoinfo.WURCSFramework.util.residuecontainer.SuperClass;

public class GlycanToResidueContainer {

	private LinkedList<ResidueContainer> lst_RC = new LinkedList<ResidueContainer>();
	
	public LinkedList<ResidueContainer> getResidueContainerList(Glycan a_objGlycan) {
		LinkedList<Residue> lst_cpResidue = this.copy(a_objGlycan);
		this.convertGlycan(a_objGlycan, lst_cpResidue);
		
		return this.lst_RC;
	}

	private void convertGlycan(Glycan a_objGlycan, LinkedList<Residue> lst_Residue) {
		for(Residue a_objResidue : lst_Residue) {
			ResidueContainer a_objRC = new ResidueContainer();
			
			a_objRC.setMS("");			
			a_objRC.setNodeID(this.lst_RC.size() + 1);
			a_objRC.setNodeIndex(WURCSDataConverter.convertRESIDToIndex(this.lst_RC.size() + 1));
			a_objRC.setSugarName(/*a_objResidue.getMotifName() + */a_objResidue.getResidueName());
			a_objRC.setIUPACExtednedNotation(
					/*a_objResidue.getMotifName() + */a_objResidue.getType().getIupacName().replace("$", ""));
			a_objRC.setAnomerPosition(this.extractAnomerPos(a_objResidue.getAnomericCarbon()));
			a_objRC.setAnomerSymbol(a_objResidue.getAnomericState());
			a_objRC.addDLconfiguration(String.valueOf(a_objResidue.getChirality()));
			a_objRC.setRingSize(a_objResidue.getRingSize());
			a_objRC.setRootStatus(RootStatusDescriptor.forRootStatus(this.checkReducingEnd(a_objResidue)));
			a_objRC.setBackBoneSize(
					SuperClass.getSuperClass(this.trim(a_objResidue.getType().getCompositionClass())).getSize());
			
			/** define status*/
			checkStatus(a_objResidue, a_objRC);
			
			/** add substituent */
			for(String s : this.extractmodification(a_objResidue)) {
				a_objRC.addSubstituent(s);
			}
			a_objRC.addNativeSubstituent(checkNativeSubstituent(a_objResidue));
			
			/** define linkage block */
			a_objRC.addLinkage(this.extractLinkage(a_objResidue, a_objRC.getSubstituent(), lst_Residue));
			
			this.lst_RC.addLast(a_objRC);
		}
		return;
	}
	
	private String trim(String str_size) {
		if(str_size.contains("Hex")) return "Hex";
		if(str_size.contains("Pen")) return "Pen";
		if(str_size.contains("Non")) return "Non";
		return str_size;
	}
	
	private int extractAnomerPos(char char_anomerPos) {
		if(char_anomerPos == '?') return -1;
		return Integer.parseInt(String.valueOf(char_anomerPos));
	}
	
	private LinkageBlock 
	extractLinkage(Residue a_objResidue, ArrayList<String> arr_modPos, LinkedList<Residue> lst_Residue) {
		LinkageBlock a_objLB = new LinkageBlock();
		
		/**set linkage ID*/
		a_objLB.setAcceptorID(!a_objResidue.hasChildren() ? 
				-1 : lst_Residue.indexOf(a_objResidue));
		a_objLB.setDonorID(!a_objResidue.getParent().isReducingEnd() ? 
				lst_Residue.indexOf(a_objResidue) : -1);
		
		/**set parent*/
		if(a_objResidue.hasSaccharideParent()) {
			Residue parent = a_objResidue.getSaccharideParent();
			if(!parent.equals(a_objResidue))
				a_objLB.addParent(WURCSDataConverter.convertRESIDToIndex(lst_Residue.indexOf(parent) + 1));
			
			if(a_objResidue.getParentsOfFragment().size() > 0) {	
				for(Residue s : a_objResidue.getParentsOfFragment()) {
					if(!a_objLB.getAntenna().contains(s)) a_objLB.addParent("1");
				}
			}
			if(a_objResidue.hasParent() && a_objResidue.getParent().isBracket() 
					&& a_objResidue.getParentsOfFragment().size() == 0) {
				a_objLB.addParent("?");
			}
		}
		
		/**set parent linkage*/
		Linkage a_objCLIN = a_objResidue.getParentLinkage();
		a_objLB.addChildDonor(this.checkAmbiguousLinkage(a_objCLIN.getChildPositionsString()));
		a_objLB.addChildAcceptor(this.checkAmbiguousLinkage(a_objCLIN.getParentPositionsString()));
		
		/**set child linkage*/
		for(Linkage a_objPLIN : this.checkChildLinkage(a_objResidue)) {
			Residue ind_red = this.getSaccharideResidue(a_objPLIN.getChildResidue());
			if(ind_red.isAntenna() && ind_red.getParent().isBracket()) continue;

			String childIndex = 
					WURCSDataConverter.convertRESIDToIndex(lst_Residue.indexOf(ind_red) + 1);
			if(!childIndex.equals("")) a_objLB.addChild(childIndex);
			
			for(Residue s : a_objResidue.getParentsOfFragment()) {
				this.lst_RC.get(WURCSDataConverter.convertRESIndexToID("1") - 1)
					.getLinkage().addAntennaRoot("1");
			}
		
			/**check duplicate a linkage position for modification/substituent*/
//			for(String mod : arr_modPos) {
//				String[] str_mod = mod.split("\\*");
//				for(LinkedList<Integer> lst_pos : a_objLB.) {
//					if(str_mod[0].contains("/") || str_mod[0].contains(",")) continue;
//					if(lst_pos.contains(Integer.parseInt(str_mod[0]))) {
//						a_objLB.getParentDonor().remove(a_objLB.getParentAcceptor().indexOf(lst_pos));
//						a_objLB.getParentAcceptor().remove(lst_pos);
//					}
//				}
//			}
		}
		
		/**define repeating unit*/
		if(a_objResidue.getStartRepetitionResidue() != null) {
			a_objLB.addRepeatingBlock(
					"start", this.extractRepetation(a_objResidue.getParent(), lst_Residue));
		}
		if(a_objResidue.getEndRepitionResidue() != null) {
			a_objLB.addRepeatingBlock(
					"end", this.extractRepetation(a_objResidue.getEndRepitionResidue(), lst_Residue));
		}
		
		/** define cyclic unit */
		if(a_objResidue.getStartCyclicResidue() != null) {
			a_objLB.addRepeatingBlock("cyclic_start", extractCyclic(a_objResidue, lst_Residue));
		}
		if(a_objResidue.getEndCyclicResidue() != null) {
			a_objLB.addRepeatingBlock("cyclic_end", extractCyclic(a_objResidue, lst_Residue));
		}
		
/*		System.out.println(a_objResidue.getStartCyclicResidue());
		System.out.println(a_objResidue.getStartRepetitionResidue());
		System.out.println(a_objResidue.getTypeName());
		System.out.println(a_objResidue.getEndRepitionResidue());
		System.out.println(a_objResidue.getEndCyclicResidue());
		System.out.println(a_objLB.getRepeatingBlock());
		System.out.println("");
		*/
		return a_objLB;
	}
	
	private LinkedList<Integer> checkAmbiguousLinkage(String str_LINpos) {
		LinkedList<Integer> ret = new LinkedList<Integer>();
		if(str_LINpos.equals("?")) ret.addLast(-1);
		if(str_LINpos.contains("/")) {
			for(String s : str_LINpos.split("/")) {
				ret.addLast(Integer.parseInt(s));
			}
		}
		if(ret.size() == 0) ret.addLast(Integer.parseInt(str_LINpos));
		
		return ret;
	}
	
	private ArrayList<String> extractmodification(Residue a_objResidue) {
		ArrayList<String> a_lstMod = new ArrayList<String>();

		for(Linkage a_objLIN : a_objResidue.getChildrenLinkages()) {
			Residue a_objSub = a_objLIN.getChildResidue();
			if(a_objSub.isSubstituent() || a_objSub.isModificaiton()) {
				StringBuilder str_mod = new StringBuilder();
				str_mod.append(a_objLIN.getParentPositionsString());
				str_mod.append("*" + a_objSub.getTypeName());
				a_lstMod.add(str_mod.toString());
			}
		}
		
		return a_lstMod;
	}
	
	private String checkNativeSubstituent(Residue a_objRES) {
		String ret = "";
		
		if(a_objRES.getType().getSuperclass().equals("N-acetylhexosamine"))
			ret = "2*NAc";
		if(a_objRES.getType().getSuperclass().equals("Hexosamine"))
			ret = "2*N";
		if(a_objRES.getTypeName().contains("Neu")) {
			String a_strMod = "5*N";
			if(a_objRES.getType().getIupacName().contains("Gc")) a_strMod = a_strMod + "Gc";
			if(a_objRES.getType().getIupacName().contains("Ac")) a_strMod = a_strMod + "Ac";
			
			ret = a_strMod;
		}
		
		return ret;
	}
	
	private RepeatingBlock extractRepetation(Residue a_objResidue, LinkedList<Residue> lst_Residue) {
		RepeatingBlock a_objRB = new RepeatingBlock();
		if(a_objResidue.isEndRepetition()) {
			a_objRB.setMax(a_objResidue.getMaxRepetitions());
			a_objRB.setMin(a_objResidue.getMinRepetitions());
			a_objRB.setOppositdeNode(
				WURCSDataConverter.convertRESIDToIndex(
					lst_Residue.indexOf(a_objResidue.getStartRepetitionResidue()) + 1));
		}else {
			a_objRB.setOppositdeNode(
				WURCSDataConverter.convertRESIDToIndex(
					lst_Residue.indexOf(a_objResidue.getEndRepitionResidue().getParent()) + 1));
		}
		
		/**set parent linkage in repeating block*/
		Linkage obj_parent = a_objResidue.getParentLinkage();
		a_objRB.setChildAcceptor(this.checkAmbiguousLinkage(obj_parent.getParentPositionsString()));
		a_objRB.setChildDonor(this.checkAmbiguousLinkage(obj_parent.getChildPositionsString()));
		
		/**set child linkage in repeating block*/
		for(Linkage unit : a_objResidue.getChildrenLinkages()) {
			a_objRB.addParentAcceptor(this.checkAmbiguousLinkage(unit.getParentPositionsString()));
			a_objRB.addParentDonor(this.checkAmbiguousLinkage(unit.getChildPositionsString()));
		}
		
		return a_objRB;
	}
	
	private RepeatingBlock extractCyclic(Residue a_objRES, LinkedList<Residue> lst_Residue) {
		RepeatingBlock a_objCB = new RepeatingBlock();
		if(a_objRES.isEndCyclic()) {
			a_objCB.setOppositdeNode(
					WURCSDataConverter.convertRESIDToIndex(
							lst_Residue.indexOf(a_objRES.getStartCyclicResidue()) - 1));
			
			for(Linkage a_cLink : a_objRES.getChildrenLinkages()) {
				a_objCB.addParentAcceptor(checkAmbiguousLinkage(a_cLink.getParentPositionsString()));
				a_objCB.addParentDonor(checkAmbiguousLinkage(a_cLink.getChildPositionsString()));
			}
		}else {
			a_objCB.setOppositdeNode(
					WURCSDataConverter.convertRESIDToIndex(
							lst_Residue.indexOf(a_objRES.getEndCyclicResidue().getParent()) - 1));
			
			Linkage a_pLink = a_objRES.getParentLinkage();
			a_objCB.setChildAcceptor(checkAmbiguousLinkage(a_pLink.getParentPositionsString()));
			a_objCB.setChildDonor(checkAmbiguousLinkage(a_pLink.getChildPositionsString()));
		}
		
		return a_objCB;
	}
	
	private String checkReducingEnd(Residue a_objResidue) {
		if(!a_objResidue.getParent().isReducingEnd() && !a_objResidue.getParent().isBracket()) return "";
		
		if(a_objResidue.isAlditol()) return "O";
		if(a_objResidue.getParent().isBracket()) return "frgRoot";
		if(a_objResidue.getParent().isReducingEnd()) return a_objResidue.getParent().getResidueName();
		return "";
	}
	
	private Residue getSaccharideResidue(Residue a_objResidue) {
		if(a_objResidue.isSubstituent() || a_objResidue.isModificaiton()) return a_objResidue.getParent();
		if(a_objResidue.isEndRepetition() && !a_objResidue.hasChildren()) return a_objResidue.getParent();
		if(a_objResidue.getType().getSuperclass().equals("unknown")) return a_objResidue.getParent();
		if(a_objResidue.isEndCyclic()) return a_objResidue.getParent();
		
		while(!a_objResidue.isSaccharide()) {
			if(a_objResidue.getChildAt(0) == null) break;
			a_objResidue = a_objResidue.getChildAt(0);
		}
		
		return a_objResidue;
	}
	
	private LinkedList<Linkage> checkChildLinkage(Residue a_objResidue) {
		LinkedList<Linkage> ret = new LinkedList<Linkage>();
		if(!a_objResidue.hasChildren()) return ret;
		if(a_objResidue.getNoChildren() == 1) {
			if(a_objResidue.getChildAt(0).isSubstituent()) return ret;
			if(a_objResidue.getChildAt(0).isEndRepetition() && !a_objResidue.getChildAt(0).hasChildren()) return ret;
		}
		
		if(a_objResidue.getNoChildren() > 1) {
			for(Linkage a_objLIN : a_objResidue.getChildrenLinkages()) {
				if(!a_objLIN.getChildResidue().isSaccharide()) continue;
				ret.addLast(a_objLIN);
			}
			return ret;
		}
		
		if(a_objResidue.getChildAt(0).isSaccharide()) {
			return a_objResidue.getChildrenLinkages();
		}else {
			ret.addLast(this.getSaccharideResidue(a_objResidue.getChildAt(0)).getParentLinkage());
			return ret;
		}
	}

	private void checkStatus(Residue a_objResidue, ResidueContainer a_objRC) {
		if(a_objResidue.getType().getSuperclass().equals("Hexuronate")) {
			a_objRC.setAcidicSugar(true);
		}
		if(a_objResidue.getType().getSuperclass().contains("Kedo")) {
			a_objRC.setONIC(true);
		}
	}
	
	private LinkedList<Residue> copy(Glycan obj_Glycan) {
		LinkedList<Residue> lst_cpResidue = new LinkedList<Residue>();
		lst_cpResidue = obj_Glycan.getAllSaccharide();
		return lst_cpResidue;
	}
	
}