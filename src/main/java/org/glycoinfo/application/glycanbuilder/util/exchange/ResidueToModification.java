package org.glycoinfo.application.glycanbuilder.util.exchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.util.exchange.SubstituentTemplate;
import org.glycoinfo.WURCSFramework.util.exchange.SubstituentTypeToMAP;

public class ResidueToModification {

	private Residue a_oSubstituent;
	private Linkage a_oChildLIN = null;
	private Linkage a_oParentLIN = null;

	private int a_iMAPPositionForParent = 0;
	private int a_iMAPPositionForChild = 0;
	private String a_sHeadAtom = "";
	private String a_sTailAtom = "";
	private String a_sMAP = "";
	
	private SubstituentTypeToMAP a_enumSubTypeToMAP;
	private SubstituentTemplate a_enumST;
	
	private LinkageType a_oParentType;
	private LinkageType a_oChildType;
	
	private String a_sCTNotation = "";
	
	public String getMAPCode() {
		return this.a_sMAP;
	}
	
	public int getMAPPositionForParentSide() {
		return this.a_iMAPPositionForParent;
	}
	
	public int getMAPPositionForChildSide() {
		return this.a_iMAPPositionForChild;
	}
	
	public String getHeadAtom() {
		return this.a_sHeadAtom;
	}
	
	public String getTailAtom() {
		return this.a_sTailAtom;
	}
	
	public SubstituentTemplate getSubstituentTemplate() {
		return this.a_enumST;
	}
	
	public void setParentLinkage(Linkage a_oLIN) {
		this.a_oParentLIN = a_oLIN;
	}
	
	public void setChildLinkage(Linkage a_oLIN) {
		this.a_oChildLIN = a_oLIN;
	}
	
	public void setSubstituentTemplate(Residue a_oSUB) throws Exception {
		this.a_enumST =	SubstituentTemplate.forIUPACNotation(a_oSUB.getTypeName());		
		this.a_sCTNotation = this.a_enumST.getGlycoCTnotation();
		
		return;
	}
	
	public void start(Residue a_oSub) throws Exception {
		this.a_oSubstituent = a_oSub;
		this.a_enumSubTypeToMAP = SubstituentTypeToMAP.forName(this.a_sCTNotation);
		
		this.a_sHeadAtom = this.a_enumSubTypeToMAP.getHeadAtom();
		this.a_sTailAtom = this.a_enumSubTypeToMAP.getTailAtom();
		
		if(this.a_oSubstituent.getParentLinkage() != null)
			this.a_oParentLIN = this.a_oSubstituent.getParentLinkage();
		if(this.a_oParentLIN == null)
			throw new Exception("Substituent should have parent linkage");
		if(!this.a_oSubstituent.getChildrenLinkages().isEmpty())
			this.a_oChildLIN = this.a_oSubstituent.getChildrenLinkages().get(0);
		
		this.a_oParentType = this.a_oParentLIN.getParentLinkageType();
		this.a_oChildType = this.a_oParentLIN.getChildLinkageType();
		
		if(this.a_oParentType == LinkageType.UNKNOWN)
			this.a_oParentType = LinkageType.H_AT_OH;
		
		String a_sMAPDouble = this.a_enumSubTypeToMAP.getMAPDouble();
		if(a_sMAPDouble != null && a_sMAPDouble.equals("") && 
				a_oSub.getParentLinkage().getBonds().size() > 1) return;
		
		this.a_sMAP = (a_oSub.getParentLinkage().getBonds().size() == 1 && 
				!a_oSub.getType().getSuperclass().equals("Bridge")) ? 
				this.getMAPCodeSingle() : this.getMAPCodeDouble();
	}
	
	public String getMAPCodeSingle() {
		String a_sMAP = this.a_enumSubTypeToMAP.getMAPSingle();
		boolean a_bIsOBond = (a_sMAP.startsWith("C") && !a_sMAP.equals("CO") && !a_sMAP.equals("Cl"))  || 
				(a_sMAP.startsWith("S") && !a_sMAP.equals("S") ||
				(a_sMAP.startsWith("P"))) ? true : false;
		
		if(this.a_oParentType.equals(LinkageType.H_AT_OH)) a_bIsOBond = true;
		else a_bIsOBond = false;
		
		if(a_bIsOBond) {
			this.a_sHeadAtom = "O";
			a_sMAP = this.addOxygenToHead(a_sMAP);
		}
		
		return "*" + a_sMAP;
	}
	
	public String getMAPCodeDouble() {
		String a_sDoubleMAP = this.a_enumSubTypeToMAP.getMAPDouble();
		Boolean a_bIsSwap = this.a_enumSubTypeToMAP.isSwapCarbonPositions();
		boolean a_bHasOrder = false;
		
		if(a_bIsSwap == null && this.a_oParentType != this.a_oChildType) {
			if(this.a_oParentType == LinkageType.H_AT_OH)
				a_bIsSwap = false;
			else if (this.a_oChildType == LinkageType.H_AT_OH)
				a_bIsSwap = true;
		}
		if(a_bIsSwap != null) {
			this.a_iMAPPositionForParent = 1;
			this.a_iMAPPositionForChild = 2;
			if(a_bIsSwap) {
				this.a_iMAPPositionForParent = 2;
				this.a_iMAPPositionForChild = 1;
			}
			a_bHasOrder = true;
		} else {
			a_bIsSwap = false;
		}

		/** add oxygen */
		if(this.a_oParentType == LinkageType.H_AT_OH) {
			this.a_sHeadAtom = "O";
			a_sDoubleMAP = (a_bIsSwap) ? 
					this.addOxygenToTail(a_sDoubleMAP) : this.addOxygenToHead(a_sDoubleMAP);
		}
		if(this.a_oChildType == LinkageType.H_AT_OH) {
			this.a_sTailAtom = "O";
			a_sDoubleMAP = (a_bIsSwap) ? 
					this.addOxygenToHead(a_sDoubleMAP) : this.addOxygenToTail(a_sDoubleMAP);
		}

		if(a_bHasOrder)
			a_sDoubleMAP = this.addMAPStarIndex(a_sDoubleMAP);
		
		a_sDoubleMAP = "*" + a_sDoubleMAP;
		a_sDoubleMAP = a_sDoubleMAP.replace("*OP^XO*", "*OPO*");
		a_sDoubleMAP = a_sDoubleMAP.replace("*P^X*", "*P*");
		
		return a_sDoubleMAP;
	}
	
	private String addOxygenToHead(String a_sMAP) {
		if(a_sMAP.startsWith("NCCOP")) return a_sMAP;
		
		ArrayList<Integer> a_aNums = new ArrayList<Integer>();
		String a_sNum = "";
		for(int i = 0; i < a_sMAP.length(); i++) {
			char a_cUnit = a_sMAP.charAt(i);
			if( Character.isDigit(a_cUnit)) {
				a_sNum += a_cUnit;
				continue;
			}
			if(a_sNum.equals("")) continue;
			if(a_aNums.contains(Integer.parseInt(a_sNum))) continue;
			a_aNums.add(Integer.parseInt(a_sNum));
			a_sNum = "";
		}
		Collections.sort(a_aNums);
		Collections.reverse(a_aNums);
		
		String a_sNewMAP = a_sMAP;
		for(Iterator<Integer> it = a_aNums.iterator(); it.hasNext();) {
			Integer a_iNum1 = it.next();
			Integer a_iNum2 = a_iNum1 + 1;
			a_sNewMAP = a_sNewMAP.replaceAll(a_iNum1.toString(), a_iNum2.toString());
		}
		return "O" + a_sNewMAP;
	}
	
	private String addOxygenToTail(String a_sMAP) {
		StringBuilder a_sbMAP = new StringBuilder(a_sMAP);
		int a_iInsertPos = a_sbMAP.lastIndexOf("*");
		a_sbMAP.insert(a_iInsertPos, 'O');
		a_sMAP = a_sbMAP.toString();
		
		int a_iPosO = 1;
		for(int i = 0; i < a_iInsertPos; i++) {
			char a_c = a_sMAP.charAt(i);
			if(a_c == '^' || a_c == '/') {
				i++;
				continue;
			} else if (a_c == '=' || a_c == '#') {
				continue;
			} else if (a_c == '*') {
				break;
			}
			a_iPosO++;
		}
		
		ArrayList<Integer> a_aNums = new ArrayList<Integer>();
		String a_sNum = "";
		for(int i = 0; i < a_sMAP.length(); i++) {
			char a_c = a_sMAP.charAt(i);
			if(Character.isDigit(a_c)) {
				a_sNum += a_c;
				continue;
			}
			if(a_sNum.equals("")) continue;
			if(a_aNums.contains(Integer.parseInt(a_sNum))) continue;
			a_aNums.add(Integer.parseInt(a_sNum));
			a_sNum = "";
		}
		Collections.sort(a_aNums);
		Collections.reverse(a_aNums);
		
		String a_sNewMAP = a_sMAP;
		
		for(Iterator<Integer> it = a_aNums.iterator(); it.hasNext();) {
			Integer a_iNum1 = it.next();
			if(a_iNum1 <= a_iPosO) continue;
			Integer a_iNum2 = a_iNum1 + 1;
			a_sNewMAP = a_sNewMAP.replaceAll(a_iNum1.toString(), a_iNum2.toString());
		}
		return a_sNewMAP;
	}
	
	private String addMAPStarIndex(String a_sMAP) {
		StringBuilder a_sbMAP = new StringBuilder(a_sMAP);
		int a_iInsertPos2 = a_sMAP.indexOf("*");
		a_sbMAP.insert(a_iInsertPos2 + 1,  '2');
		a_sbMAP.insert(0, '1');	
		return a_sbMAP.toString();
	}
}
