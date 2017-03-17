package org.glycoinfo.application.glycanbuilder.util.exchange;

import java.util.Collection;
import java.util.LinkedList;

import org.eurocarbdb.MolecularFramework.sugar.*;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.wurcs.graph.Backbone;
import org.glycoinfo.WURCSFramework.util.property.AtomicProperties;
import org.glycoinfo.WURCSFramework.wurcs.graph.BackboneCarbon;
import org.glycoinfo.WURCSFramework.wurcs.graph.BackboneUnknown_TBD;
import org.glycoinfo.WURCSFramework.wurcs.graph.Backbone_TBD;
import org.glycoinfo.WURCSFramework.wurcs.graph.CarbonDescriptor_TBD;
import org.glycoinfo.WURCSFramework.wurcs.graph.Modification;

public class ResidueToBackbone {

	private Backbone a_oBackbone;
	
	private char a_cAnomSymbol = '?';
	private int a_iAnomPosition = 0;
	private char a_cConfiguration = '?';
	
	private Residue a_oResidue;
	private boolean a_bIsRootOfFragment = false;

	private LinkedList<Modification> a_aUnknownPosCoreMOD = new LinkedList<Modification>();
	
	public Residue getResidue() {
		return this.a_oResidue;
	}
	
	public Backbone getBackbone() {
		return this.a_oBackbone;
	}
	
	public LinkedList<Modification> getCoreModifications() {
		return this.a_aUnknownPosCoreMOD;
	}
	
	public void setRootOfFramgents() {
		this.a_bIsRootOfFragment = true;
	}
	
	public void start(Residue a_oRES) throws Exception {
		this.a_oResidue = a_oRES;
		this.a_iAnomPosition = checkAnomericSymbolCharactor(a_oRES.getAnomericCarbon());
		this.a_cAnomSymbol = 
				(a_oRES.isAldehyde() || a_oRES.isAlditol()) ? 'o' : a_oRES.getAnomericState();
		this.a_cConfiguration = a_oRES.getChirality();
			
		String a_sSuperClass = (a_oRES.getType().getCompositionClass().equals("Sugar")) ? "sug" : a_oRES.getType().getCompositionClass();
		Superclass a_enumClass = Superclass.forName(a_sSuperClass.toLowerCase());
		
		int a_iCAtom = a_enumClass.getCAtomCount();
		
		if(a_iCAtom == 0) {
			this.a_oBackbone = new BackboneUnknown_TBD(this.a_cAnomSymbol);
			return;
		}
		
		ResidueAnalyzer a_oRAnalyzer = new ResidueAnalyzer();
		a_oRAnalyzer.ResidueToSkeletonCode(a_oRES);
	
		this.a_iAnomPosition = a_oRAnalyzer.getAnomericPosition();
		this.a_cAnomSymbol = a_oRAnalyzer.getAnomericSymbol();
		this.a_cConfiguration = a_oRAnalyzer.getConfiguration();
		String a_sSkeletonCode = a_oRAnalyzer.getSkeletonCode();
		
		for(String a_sMAP : a_oRAnalyzer.getUnknownMAPs()) {
			this.a_aUnknownPosCoreMOD.add( new Modification(a_sMAP));
		}
		
		/** check unknown anomeric position*/
		if(this.a_iAnomPosition == 0 && this.a_cAnomSymbol == '?') {
			if(!hasParent()) {
				if(!a_oRES.isAldehyde() && !a_oRES.isAlditol()) {
					a_sSkeletonCode = a_sSkeletonCode.replaceAll("o", "u");
					a_sSkeletonCode = a_sSkeletonCode.replaceAll("O", "U");
				}
			}else if (this.hasParent() && 
					(a_oRES.getParent().getType().getSuperclass().equals("Bridge"))) {
			} else {
				if(!a_oRES.isAldehyde()) {
					if(a_sSkeletonCode.contains("o")) {
						this.a_iAnomPosition = a_sSkeletonCode.indexOf("o") + 1;
						a_sSkeletonCode = a_sSkeletonCode.replaceFirst("o", "a");
					}else if(a_sSkeletonCode.contains("O")) {
						this.a_iAnomPosition = a_sSkeletonCode.indexOf("O") + 1;
						a_sSkeletonCode = a_sSkeletonCode.replaceFirst("O", "a");
					}
				}
			}
		}
		
		StringBuilder a_sbSkeletonCode = new StringBuilder(a_sSkeletonCode);
		/** extract substituent*/
		if(a_oRES.getParentLinkage() != null) {
			this.replaceCarbonDescriptorByLinkage(a_sbSkeletonCode, a_oRES.getParentLinkage(), true);
		}
		for(Linkage a_oLIN : a_oRES.getChildrenLinkages()) {
			this.replaceCarbonDescriptorByLinkage(a_sbSkeletonCode, a_oLIN, false);
		}
		
		/** make Backbone*/
		Backbone_TBD a_oBackbone = new Backbone_TBD();
		a_oBackbone.setAnomericPosition(this.a_iAnomPosition);
		a_oBackbone.setAnomericSymbol(this.a_cAnomSymbol);
		for(int i = 0; i < a_iCAtom; i ++) {
			char a_cCD = a_sbSkeletonCode.charAt(i);
			CarbonDescriptor_TBD a_enumCD = CarbonDescriptor_TBD.forCharacter(a_cCD, ( i == 0 || i == a_iCAtom - 1));
			BackboneCarbon a_oBC = new BackboneCarbon(a_oBackbone, a_enumCD);
			a_oBackbone.addBackboneCarbon(a_oBC);
		}
	
		this.a_oBackbone = a_oBackbone;
	}
	
	private void replaceCarbonDescriptorByLinkage(StringBuilder a_sbSkeletonCode, Linkage a_oLIN, boolean a_bIsParentSideLinkage) throws Exception {
		Residue a_oRES = (a_bIsParentSideLinkage) ? 
				a_oLIN.getParentResidue() : a_oLIN.getChildResidue();
		Residue a_oSUB = (a_oRES.isSubstituent()) ? a_oRES : null;
		boolean a_bIsSwapChirality = false;
		
		Collection<Character> a_aPositions = a_oLIN.getParentPositions();
		if(a_aPositions.size() > 1) return;
		
		a_bIsSwapChirality = (this.compareConnectAtom(a_oLIN, a_oSUB, a_bIsParentSideLinkage) < 0);
		Linkage a_oParentLink = a_oLIN;
		
		if(a_oLIN.getParentResidue().isRepetition())
			a_oParentLink = a_oLIN.getParentResidue().getParentLinkage();
		
		int a_iPos = a_aPositions.iterator().next() == '?' ? -1 : Integer.parseInt(String.valueOf(a_aPositions.iterator().next()));
		
		if(a_iPos == -1) return;
		char a_cCD = a_sbSkeletonCode.charAt(a_iPos - 1);
		char a_cNewCD = a_cCD;
		
		LinkageType a_oType0 = (a_bIsParentSideLinkage) ? a_oLIN.getChildLinkageType() : a_oParentLink.getParentLinkageType();
		LinkageType a_oType1 = (a_bIsParentSideLinkage) ? a_oParentLink.getParentLinkageType() : a_oLIN.getChildLinkageType();
		
		if(a_oType0 == LinkageType.H_LOSE) {
			a_cNewCD = this.replaceCarbonDescriptorByHydrogenLose(a_cCD, a_bIsSwapChirality);
		} else if(a_oType0 == LinkageType.DEOXY && a_oType1 != LinkageType.H_AT_OH) {
			a_cNewCD = (a_cCD == 'c') ? 'x' :
					   (a_cCD == 'C') ? 'X' : a_cNewCD;
		}
		
		a_sbSkeletonCode.replace(a_iPos - 1, a_iPos, a_cNewCD+"");
		
		if(!a_bIsParentSideLinkage) return;
		if(this.a_iAnomPosition == 0 || this.a_iAnomPosition == -1) return;
		if(this.a_cAnomSymbol != 'a' || this.a_cAnomSymbol != 'b') return;
		
		char a_cAnomCD = a_sbSkeletonCode.charAt(this.a_iAnomPosition - 1);
		if(a_cAnomCD == 'x' || a_cAnomCD == 'X') return;
		
		char a_cAnomStereo = (this.a_cAnomSymbol == 'a') ? '1' : '2';
		if(this.a_cConfiguration == 'L')
			a_cAnomStereo = (a_cAnomStereo == '1') ? '2' : '1';
		if(a_bIsSwapChirality)
			a_cAnomStereo = (a_cAnomStereo == '1') ? '2' : '1';
		if(a_cAnomCD == 'X')
			a_cAnomStereo = (a_cAnomStereo == '1') ? '5' : '6';
		
		a_sbSkeletonCode.replace(this.a_iAnomPosition - 1, this.a_iAnomPosition, a_cAnomStereo+"");
	}
	
	private char replaceCarbonDescriptorByHydrogenLose(char a_cCD, boolean a_bIsSwapChirality) {
		char a_cNewCD = (a_cCD == '1') ? '5' :
						(a_cCD == '2') ? '6' : 
						(a_cCD == '3') ? '7' :
						(a_cCD == '4') ? '8' :
						(a_cCD == 'x') ? 'X' :
						(a_cCD == 'C') ? 'C' :
						(a_cCD == 'm') ? 'h' :
						(a_cCD == 'h') ? 'c' : a_cCD;
		if(a_bIsSwapChirality) {
			a_cNewCD = (a_cNewCD == '5') ? '6' :
					   (a_cNewCD == '6') ? '5' :
					   (a_cNewCD == '7') ? '8' :
					   (a_cNewCD == '8') ? '7' : a_cNewCD;
		}
		
		return a_cNewCD;
	}
	
	private int compareConnectAtom(Linkage a_oLIN, Residue a_oSUB, boolean a_bIsParentSide) throws Exception {
		LinkageType a_oParentType = (a_bIsParentSide) ? a_oLIN.getChildLinkageType() : a_oLIN.getParentLinkageType();
		LinkageType a_oChildType = (a_bIsParentSide) ? a_oLIN.getParentLinkageType() : a_oLIN.getChildLinkageType();
		if(a_oParentType == LinkageType.H_AT_OH || a_oChildType == LinkageType.H_AT_OH) return 0;
		
		if(a_oSUB == null) return 0;
		return this.compareConnectAtomOfSubstituent(a_oSUB, a_bIsParentSide);
	}
	
	private int compareConnectAtomOfSubstituent(Residue a_oSUB, boolean a_bSubstituentIsParent) throws Exception {
		ResidueToModification a_oResToMod = new ResidueToModification();
		a_oResToMod.setSubstituentTemplate(a_oSUB);
		
		a_oResToMod.start(a_oSUB);
		String a_sConnAtom = (a_bSubstituentIsParent) ? a_oResToMod.getTailAtom() : a_oResToMod.getHeadAtom();
		
		int a_iNumberOfFirstAtom = AtomicProperties.forSymbol(a_sConnAtom).getAtomicNumber();
		if(a_iNumberOfFirstAtom > 16) return 1;
		return -1;
	}
	
	private int checkAnomericSymbolCharactor(char a_cAnomPosition) {
		if(a_cAnomPosition == '?') return 0;
		return Integer.parseInt(String.valueOf(a_cAnomPosition));
	}
	
	private boolean hasParent() {
		if(this.a_oResidue.getParent() == null || this.a_oResidue.getParent().isReducingEnd() || a_oResidue.isComposition())
			return false;
		return true;
	}
}
