package org.glycoinfo.application.glycanbuilder.util.exchange;

import java.util.ArrayList;
import java.util.LinkedList;

import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.util.array.WURCSImporter;
import org.glycoinfo.WURCSFramework.util.exchange.SubstituentTemplate;
import org.glycoinfo.WURCSFramework.util.exchange.TrivialNameDescriptor;
import org.glycoinfo.WURCSFramework.wurcs.array.LIP;
import org.glycoinfo.WURCSFramework.wurcs.array.LIPs;
import org.glycoinfo.WURCSFramework.wurcs.array.MOD;
import org.glycoinfo.WURCSFramework.wurcs.array.MS;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.BRIDGE;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GLIN;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.SUBST;
import org.glycoinfo.application.glycanbuilder.dataset.CrossLinkedSubstituentDictionary;

public class SUBSTAnalyzer {

	TrivialNameDescriptor a_enumTrivialDescriptor;
	ArrayList<String> a_aModifications = new ArrayList<String>();

	public SUBSTAnalyzer(ArrayList<String> _a_aModifications) {
		this.a_aModifications = _a_aModifications;
	}
	
	public SUBSTAnalyzer() {
		// TODO Auto-generated constructor stub
	}

	public void start(GRES a_oGRES, Residue a_oRES) throws Exception {
		this.a_enumTrivialDescriptor = 
				TrivialNameDescriptor.forTrivialName(a_oRES.getTypeName());
		
		if(a_enumTrivialDescriptor == null && a_oRES.getType().getSuperclass().equals("Nonulosonate")) {
			this.a_enumTrivialDescriptor = TrivialNameDescriptor.NONULOSONATE;
		}
		
		MS a_oMS = new WURCSImporter().extractMS(a_oGRES.getMS().getString());
		
		for(SUBST a_oSUBST : a_oGRES.getMS().getCoreStructure().getSubstituents()) {
			if(a_oSUBST.getMAP().equals("")) continue;			
			this.analyzeSUBST(a_oSUBST, a_oRES, a_oMS);
		}		
		
		for(SUBST a_oSUBST : a_oGRES.getMS().getSubstituents()) {
			if(a_oSUBST.getMAP().equals("")) continue;	
			this.analyzeSUBST(a_oSUBST, a_oRES, a_oMS);
		}
		
		for(BRIDGE a_oBRIDGE : a_oGRES.getMS().getCoreStructure().getDivalentSubstituents()) {
			if(a_oBRIDGE.getStartPositions().contains(a_oGRES.getMS().getCoreStructure().getAnomericPosition()))
				continue;
			this.analyzeBRIDGE(a_oBRIDGE, a_oRES);
		}
		
		for(BRIDGE a_oBRIDGE : a_oGRES.getMS().getDivalentSubstituents()) {
			if(a_oBRIDGE.getMAP().equals("")) continue;
			this.analyzeBRIDGE(a_oBRIDGE, a_oRES);
		}
		
		this.analyzeModificaitons(a_oRES);
		
		return;
	}
	
	/** 
	 * create bridge substituent 
	 * @param a_oGLIN
	 * @return
	 * @throws Exception
	 */
	public Residue MAPToBridge(GLIN a_oGLIN) throws Exception {
		if(a_oGLIN.getMAP().equals("")) return null;
		
		SubstituentTemplate a_enumST = SubstituentTemplate.forMAP(a_oGLIN.getMAP());
		return new Residue(CrossLinkedSubstituentDictionary.getCrossLinkedSubstituent(a_enumST.getIUPACnotation()));
	}
	
	/**
	 * create substituent for fragment
	 * @param a_oGLIN
	 * @return
	 * @throws Exception
	 */
	public Residue MAPToFragment(GLIN a_oGLIN) throws Exception {
		if(a_oGLIN.getMAP().equals("")) return null;
		
		SubstituentTemplate a_enumST = SubstituentTemplate.forMAP(a_oGLIN.getMAP());
		Residue a_oFragment = ResidueDictionary.newResidue(a_enumST.getIUPACnotation());

		return a_oFragment;
	}
	
	private void analyzeSUBST(SUBST a_oSUBST, Residue a_oRES, MS a_oMS) throws Exception {
		Linkage a_oLIN = new Linkage();
		SubstituentTemplate a_enumST = SubstituentTemplate.forMAP(a_oSUBST.getMAP());
		char[] a_cPositions = this.makePosition(a_oSUBST.getPositions());
		String a_sMAPWithPOS = a_cPositions[0] + "*" + a_enumST.getIUPACnotation();

		if(a_enumST.getIUPACnotation().equals(""))
			throw new Exception(a_oSUBST.getMAP() + " can not handled in GlycanBuilder");
		
		/** check native substituent */
		if(a_enumTrivialDescriptor != null) {
			if(a_enumTrivialDescriptor.getSubstituent().contains(a_sMAPWithPOS)) return;
		}	

		/** change n_sulfate with hexosamine */
		if(isNSubstituent(a_oRES, a_enumST, a_oSUBST.getPositions().get(0))) {
			a_enumST = SubstituentTemplate.forMAP(a_oSUBST.getMAP().replaceFirst("N", "O"));
			a_sMAPWithPOS = a_oSUBST.getPositions().get(0) + "*" + a_enumST.getIUPACnotation();
		}
		
		a_oLIN.setLinkagePositions(a_cPositions);
		
		/** set LinkageType */
		a_oLIN.setParentLinkageType(checkLinkageTypeOfMAP(a_oSUBST, a_oMS));
		a_oLIN.setChildLinkageType(LinkageType.NONMONOSACCHARID);

		/** set probability annotation */
		this.extractProbabilityAnnotation(a_oSUBST, a_oMS, a_oLIN);

		Residue a_oSUB = ResidueDictionary.newResidue(a_enumST.getIUPACnotation());
		this.checkNode(a_oSUB, a_enumST.getIUPACnotation());
		a_oSUB.setParentLinkage(a_oLIN);
		a_oRES.addChild(a_oSUB, a_oSUB.getParentLinkage().getBonds());
		
		return;
	}
	
	private void analyzeBRIDGE(BRIDGE a_oBRIDGE, Residue a_oRES) throws Exception {
		Linkage a_oLIN = new Linkage();
		String a_sMAP = a_oBRIDGE.getMAP().equals("") ? "*o" : a_oBRIDGE.getMAP();
		SubstituentTemplate a_enumST = SubstituentTemplate.forMAP(a_sMAP);
		
		char[] a_cStartPos = this.makePosition(a_oBRIDGE.getStartPositions());
		char[] a_cEndPos = this.makePosition(a_oBRIDGE.getEndPositions());
		
		a_oLIN.setLinkagePositions(a_cEndPos, a_cStartPos, '1');
		
		Residue a_oSUB = ResidueDictionary.newResidue(a_enumST.getIUPACnotation());
		this.checkNode(a_oSUB, a_enumST.getIUPACnotation());
		a_oRES.addChild(a_oSUB, a_oLIN.getBonds());
		
		return;
	}
	
	private char[] makePosition(LinkedList<Integer> a_aPositions) {
		char[] a_aPosition = new char[a_aPositions.size()];
		
		for(int i = 0; i < a_aPositions.size(); i++) {
			String a_sPos = String.valueOf(a_aPositions.get(i));
			if(a_sPos.equals("-1")) a_aPosition[i] = '?';
			else a_aPosition[i] = a_sPos.charAt(0);
		}
		
		return a_aPosition;
	}
		
	private void analyzeModificaitons(Residue a_oRES) throws Exception {
		for(String a_sMOD : this.a_aModifications) {
			Linkage a_oLIN = new Linkage();
			String[] a_aSUBs = a_sMOD.split("\\*");
			SubstituentTemplate a_enumST = SubstituentTemplate.forMAP("*" + a_aSUBs[1]);
			
			if(a_enumTrivialDescriptor != null) {
				a_oRES.addModification(a_sMOD);
				if(a_enumTrivialDescriptor.getModifications().contains(a_sMOD)) continue;
			}
			
			if(a_aSUBs[0].contains(",")) {
				char[] a_aPos = new char[a_aSUBs[0].length()];
				for(int i = 0; i < a_aSUBs[0].length(); i++) {
					if(a_aSUBs[0].charAt(i) != ',') a_aPos[i] = a_aSUBs[0].charAt(i);
				}				
				
				a_oLIN.setLinkagePositions(new char[]{a_aPos[0]}, new char[]{a_aPos[2]}, '1');
			}else 
				a_oLIN.setLinkagePositions(new char[] {a_aSUBs[0].charAt(0)});
			
			Residue a_oSUB = ResidueDictionary.newResidue(a_enumST.getIUPACnotation());
			this.checkNode(a_oSUB, a_enumST.getIUPACnotation());
			a_oRES.addChild(a_oSUB, a_oLIN.getBonds());
		}
		
		return;
	}
	
	private void extractProbabilityAnnotation (SUBST a_oSUBST, MS a_oMS, Linkage a_oLIN) {
		for(MOD a_oMOD : a_oMS.getMODs()) {
			if(a_oMOD.getMAPCode().equals("")) continue;
			
			for(LIPs a_oLIPS : a_oMOD.getListOfLIPs()) {
				for(LIP a_oLIP : a_oLIPS.getLIPs()) {
					if((a_oSUBST.getMAP().equals(a_oMOD.getMAPCode())) && (a_oSUBST.getPositions().contains(a_oLIP.getBackbonePosition()))) {
						a_oLIN.getBonds().get(0).setProbabilityHigh(a_oLIP.getModificationProbabilityUpper());
						a_oLIN.getBonds().get(0).setProbabilityLow(a_oLIP.getModificationProbabilityLower());
					}
				}
			}
		}
		return;
	}
	
	private void checkNode(Residue a_oRES, String a_sMAP) throws WURCSToGlycanException {
		if(a_oRES.getTypeName().equals("Sugar"))
			throw new WURCSToGlycanException(a_sMAP + " is not handled in GlycanBuilder");
	}
	
	private boolean isNSubstituent(Residue a_oRES, SubstituentTemplate a_enumST, int a_iPos) {
		boolean a_bIsNtype = false;
		boolean a_bIsNSub = this.isNTypes(a_enumST);
		String a_sClass = a_oRES.getType().getSuperclass();
		
		if(a_sClass.equals("Hexuronic acid")) return false;
		if(a_sClass.equals("N-Acetylhexosamine")) return false;
		
		if(a_iPos == 2 || a_iPos == 4) {
			if(a_oRES.getTypeName().equals("Bac") && a_bIsNSub) a_bIsNtype = true;
			if(a_sClass.equals("Hexosamine") && a_bIsNSub) a_bIsNtype = true;
			if(a_oRES.getTypeName().equals("Mur") && a_bIsNSub) a_bIsNtype = true;
		}
		if(a_iPos == 5) {
			if(a_oRES.getTypeName().contains("Leg") && a_bIsNSub) a_bIsNtype = true;
			else if(a_oRES.getTypeName().equals("Neu") && a_bIsNSub) a_bIsNtype = true;
			else if(a_oRES.getType().getSuperclass().equals("Nonulosonate") && a_bIsNSub) a_bIsNtype = true; 
			else a_bIsNtype = false;
		}
		if(a_iPos == 7) {
			if(a_oRES.getTypeName().contains("Leg") && a_bIsNSub) a_bIsNtype = true;
			else if(a_oRES.getType().getSuperclass().equals("Nonulosonate") && a_bIsNSub) a_bIsNtype = true;
			else a_bIsNtype = false;
		}
		
		return a_bIsNtype;
	}
	
	private LinkageType checkLinkageTypeOfMAP (SUBST a_oSUBST, MS a_oMS) {
		String a_sSC = a_oMS.getSkeletonCode();
		int a_iPos = a_oSUBST.getPositions().getFirst();
		
		if(a_iPos != -1) {
			try {
				if(Integer.parseInt(String.valueOf(a_sSC.charAt(a_iPos - 1))) > 4) return LinkageType.H_LOSE;
			} catch (NumberFormatException e) {}
		}
		
		if(a_oSUBST.getMAP().startsWith("*O")) return LinkageType.H_AT_OH;
		
		return LinkageType.DEOXY;
	}
	
	private boolean isNTypes(SubstituentTemplate a_enumST) {
		if(a_enumST.equals(SubstituentTemplate.N_SULFATE)) return true;
		if(a_enumST.equals(SubstituentTemplate.N_AMIDINO)) return true;
		if(a_enumST.equals(SubstituentTemplate.N_ACETYL)) return true;
		if(a_enumST.equals(SubstituentTemplate.N_DIMETHYL)) return true;
		if(a_enumST.equals(SubstituentTemplate.N_FORMYL)) return true;
		if(a_enumST.equals(SubstituentTemplate.N_GLYCOLYL)) return true;
		if(a_enumST.equals(SubstituentTemplate.N_METHYL)) return true;
		if(a_enumST.equals(SubstituentTemplate.N_SUCCINATE)) return true;
		if(a_enumST.equals(SubstituentTemplate.ETHANOLAMINE)) return true;
		
		return false;
	}
}
