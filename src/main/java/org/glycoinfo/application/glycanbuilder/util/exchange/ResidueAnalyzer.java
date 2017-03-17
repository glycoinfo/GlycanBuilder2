package org.glycoinfo.application.glycanbuilder.util.exchange;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;

import org.eurocarbdb.MolecularFramework.sugar.BaseType;
import org.eurocarbdb.MolecularFramework.sugar.GlycoconjugateException;
import org.eurocarbdb.MolecularFramework.sugar.ModificationType;
import org.eurocarbdb.MolecularFramework.sugar.Superclass;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.util.exchange.BaseTypeForRelativeConfiguration;
import org.glycoinfo.WURCSFramework.util.exchange.CompositSaccharideAnalyzer;
import org.glycoinfo.WURCSFramework.util.exchange.ConverterExchangeException;
import org.glycoinfo.WURCSFramework.util.exchange.TrivialNameDescriptor;
import org.glycoinfo.WURCSFramework.util.exchange.WURCSExchangeException;

public class ResidueAnalyzer {

	private String a_sSkeletonCode = "";
	
	private boolean a_bIsAldose = true;
	
	private int a_iAnomPos = 0;
	private char a_cAnomSymbol = 'x';
	private int a_iCAtom = 0;
	private char a_cDLconfiguration = '?';
	
	private LinkedList<Integer> a_aAnomPos = new LinkedList<Integer>();
	private TreeMap<Integer, Character> a_mPosToChar = new TreeMap<Integer, Character>(); 
	private LinkedList<String> a_aUnknownMAPs = new LinkedList<String>();
	
	private TrivialNameDescriptor a_enumTrivial;
	
	public int getAnomericPosition() {
		return this.a_iAnomPos;
	}
	
	public char getAnomericSymbol() {
		return this.a_cAnomSymbol;
	}
	
	public char getConfiguration() {
		return this.a_cDLconfiguration;
	}
	
	public int getNumberOfCarbons() {
		return this.a_iCAtom;
	}
	
	public String getSkeletonCode() {
		return this.a_sSkeletonCode;
	}
	
	public LinkedList<String> getUnknownMAPs() {
		return this.a_aUnknownMAPs;
	}
	
	public boolean isAldose() {
		return this.a_bIsAldose;
	}
	
	public void ResidueToSkeletonCode(Residue a_oRES) throws GlycoconjugateException, WURCSExchangeException, ConverterExchangeException {
		this.clear();

		this.a_iAnomPos = this.checkAnomerPosition(a_oRES.getAnomericCarbon());
		this.a_cAnomSymbol = this.checkAnomerSymbol(a_oRES.getAnomericState());
		this.a_cDLconfiguration = a_oRES.getChirality();

		Superclass a_enumClass = Superclass.forName(a_oRES.getType().getCompositionClass().toLowerCase());
		
		this.a_iCAtom = a_enumClass.getCAtomCount();

		this.a_mPosToChar.put(1, 'h');
		this.a_mPosToChar.put(a_iCAtom, 'h');
		
		this.a_enumTrivial = TrivialNameDescriptor.forTrivialName(a_oRES.getTypeName());
		if(this.a_enumTrivial != null && !this.a_enumTrivial.getConfiguration().isEmpty()) {
			if(a_oRES.getChirality() != this.a_enumTrivial.getConfiguration().getLast().charAt(0)) {
				this.a_enumTrivial = TrivialNameDescriptor.forTrivialName(a_oRES.getTypeName(), String.valueOf(a_oRES.getChirality()));
			}
		}
		
		/** extract native modification */	
		//TODO : 修飾の上書きをする場合, enxの解析と組み込み
		for(String a_sModification : this.extractNativeModificaiton(a_oRES)) {
			if(a_sModification.equals("")) continue;
			this.convertSingleModificationToCarbonDescriptor(a_sModification);			
		}
		
		if(!this.a_aAnomPos.isEmpty() && this.a_aAnomPos.get(0) != 1)
			this.a_bIsAldose = false;

		if(this.a_bIsAldose) {
			this.a_mPosToChar.put(1, 'o');
			this.a_aAnomPos.addFirst(1);
		}
		
		if(this.a_aAnomPos.isEmpty()) {
			this.a_iAnomPos = 0;
			this.a_cAnomSymbol = 'o';
		}
	
		if( this.a_iAnomPos != 0) {
			int a_iAnomPos = this.a_iAnomPos;
			char a_cCD = this.a_mPosToChar.get(a_iAnomPos);
			if(a_cCD == 'o' || a_cCD == 'O')
				this.a_mPosToChar.put(a_iAnomPos, 'a');
		}
		
		StringBuilder a_sStereo = new StringBuilder(this.convertBasetypesToStereoCode(defineBaseTypeFromResidue(a_oRES)));
		int j = 0;
		for(int i=2; i < this.a_iCAtom; i++) {
			if(this.a_mPosToChar.containsKey(i)) continue;			
			char a_cCD = (a_sStereo.length() == 0) ? 'x' : a_sStereo.charAt(j);
			this.a_mPosToChar.put(i, a_cCD);
			j++;
		}

		for(int i=0; i < this.a_iCAtom; i++) {
			this.a_sSkeletonCode += this.a_mPosToChar.get(i + 1);
		}		
		
		if(a_sStereo.length() != 0 && a_mPosToChar.size() != a_iCAtom)
			throw new WURCSExchangeException("error");
	}
	
	private ArrayList<String> extractNativeModificaiton(Residue a_oRES) {
		ArrayList<String> a_aModifications = new ArrayList<String>();
	
		/** for alditol */
		if(a_oRES.isAlditol())
			a_aModifications.add("1*aldi");
			
		for(Linkage a_oLIN : a_oRES.getChildrenLinkages()) {
			if(!a_oLIN.getChildResidue().isModificaiton()) continue;
			a_aModifications.add(a_oLIN.getParentPositionsString() + "*" + a_oLIN.getChildResidue().getTypeName());
		}
		
		for(String a_sMOD : a_oRES.getModifications()) {
			a_aModifications.add(a_sMOD);
		}
		
		if(this.a_enumTrivial != null) {
			a_aModifications.addAll(this.a_enumTrivial.getModifications());
		}
		
		/** for native modifications for nonulosonate */
		if(this.a_enumTrivial == null && a_oRES.getType().getSuperclass().equals("Nonulosonate")) {
			for(String a_sMod : TrivialNameDescriptor.NONULOSONATE.getModifications()) {
				if(a_oRES.getModifications().contains(a_sMod)) a_aModifications.add(a_sMod);
			}
		}
		
		/** for acidic sugar */
		if(a_oRES.getType().getCompositionClass().equals("uHexA")) 
			a_aModifications.add("6*a");
		if(a_oRES.getType().getSuperclass().equals("Hexuronic acid") && !a_aModifications.contains("6*a"))
			a_aModifications.add("6*a");
		
		return a_aModifications;
	}

	private ArrayList<BaseType> defineBaseTypeFromResidue(Residue a_oRES) throws GlycoconjugateException {
		ArrayList<BaseType> a_aBasetypes = new ArrayList<BaseType>();
		String a_sSugarName = this.checkMonosaccharideName(a_oRES);
		Superclass a_enumSuperclass = Superclass.forName(a_sSugarName);
		char a_cConfiguration = this.a_cDLconfiguration == '?' ? 'x' : this.a_cDLconfiguration;

		if(a_enumSuperclass != null) return a_aBasetypes;
		
		CompositSaccharideAnalyzer a_oCOM = new CompositSaccharideAnalyzer();
		a_oCOM.start(a_sSugarName);	
				
		if(!a_oCOM.isCompositSugar()) {
			if(a_cConfiguration == '?') a_sSugarName = 'x' + a_sSugarName;
			else a_sSugarName = a_cConfiguration + a_sSugarName;
			
			if(this.a_enumTrivial == null && a_enumSuperclass == null) {
				BaseType a_enumBase = BaseType.forName(a_sSugarName.toLowerCase());
				a_aBasetypes.add(a_enumBase);
				return a_aBasetypes;
			}
			
			for(String a_sBasetype : this.a_enumTrivial.getBasetypeWithConfiguration()) {
				if(a_sBasetype.length() < 4) {
					a_sSugarName = a_cConfiguration + a_sBasetype;
					a_aBasetypes.add(BaseType.forName(a_sSugarName.toLowerCase()));
				}else
					a_aBasetypes.add(BaseType.forName(a_sBasetype.toLowerCase()));
			}
		} else {
			for(String a_sConstitution : a_oCOM.getConstitutions()) {
				a_sSugarName = a_oCOM.getConfigurations().get(a_oCOM.getConstitutions().indexOf(a_sConstitution)) + a_sConstitution;
				a_aBasetypes.add(BaseType.forName(a_sSugarName.toLowerCase()));
			}
		}
		
		return a_aBasetypes;
	}
	
	private String checkMonosaccharideName(Residue a_oRES) throws GlycoconjugateException {
		String a_sSaccharideName = a_oRES.getTypeName().toLowerCase();
		
		/** for hexnac */
		if(this.a_enumTrivial != null) {
			if(Superclass.forName(this.a_enumTrivial.getBasetype().get(0)) != null) {
				return this.a_enumTrivial.getBasetype().get(0).toLowerCase();
			}
		}
		
		String a_sSize = a_oRES.getType().getCompositionClass();
		Superclass a_enumSuper = Superclass.forName(a_sSize);
		if(!a_sSaccharideName.equals(a_enumSuper.getName())) {
			if(a_sSaccharideName.contains(a_enumSuper.getName() + "a"))
				a_sSaccharideName = a_sSaccharideName.replace(a_sSize.toLowerCase() + "a", "");
			if(a_sSaccharideName.contains(a_enumSuper.getName()))
				a_sSaccharideName = a_sSaccharideName.replace(a_sSize.toLowerCase(), "");
		}
		
		return a_sSaccharideName;
	}
	
	private String convertBasetypesToStereoCode(ArrayList<BaseType> a_aBaseTypes) throws WURCSExchangeException {
		String a_sStereoCode = "";
		LinkedList<String> a_lDL = new LinkedList<String>();
		
		for(BaseType bs : a_aBaseTypes) {
			String code = bs.getStereoCode();
			if(bs.absoluteConfigurationUnknown()) {
				code = BaseTypeForRelativeConfiguration.forName(bs.getName()).getStereoCode();
			}
			if ( code.endsWith("1") ) a_lDL.add("L");
			if ( code.endsWith("2") ) a_lDL.add("D");
			a_sStereoCode = code + a_sStereoCode;
		}
		
		String dl = "X";
		if(a_lDL.size() > 0) dl = a_lDL.getLast();
		this.a_cDLconfiguration = dl.charAt(0);
		return a_sStereoCode;
	}
	
	private void convertSingleModificationToCarbonDescriptor(String a_sModification) throws GlycoconjugateException, ConverterExchangeException {
		if(a_sModification.contains(",")) return;

		String[] a_aMod = a_sModification.split("\\*");
		int a_iModPos = Integer.parseInt(a_aMod[0]);
		boolean a_bIsTerminal = (a_iModPos == 1 || a_iModPos == this.a_iCAtom);
		char a_cCD = this.convertModificationNameToCarbonDescriptor(a_aMod[1]);		
		
		if(a_cCD == 'd' && a_bIsTerminal)
			a_cCD = 'm';
		
		if(a_cCD == 'O') {
			if(this.a_iAnomPos != 1) this.a_aAnomPos.add(a_iModPos);
			if(a_bIsTerminal) a_cCD = 'o';
		}
		
		if(a_iModPos == 1)
			this.a_bIsAldose = false;
		
		if(a_iModPos == 0 || a_iModPos == -1) {
			this.a_aUnknownMAPs.add("*");
		}
		
		this.a_mPosToChar.put(a_iModPos, a_cCD);
	}
	
	private char convertModificationNameToCarbonDescriptor(String a_sMOD) throws GlycoconjugateException, ConverterExchangeException {				
		try {
			ModificationType a_enumModType = ModificationType.forName(a_sMOD.equals("ulo") ? "keto" : a_sMOD);		
			if(a_enumModType == ModificationType.DEOXY) return 'd';
			if(a_enumModType == ModificationType.ALDI) return 'h';
			if(a_enumModType == ModificationType.KETO) return 'O';
			if(a_enumModType == ModificationType.ACID) return 'A';
		}catch (GlycoconjugateException e){
			if(a_sMOD.equals("m")) return 'd';
		}

		return ' ';
	}
	
	private int checkAnomerPosition (char a_cAnomPos) {
		if(a_cAnomPos == '?') return 0;
		return Integer.parseInt(String.valueOf(a_cAnomPos));
	}
	
	private char checkAnomerSymbol (char a_cAnomSymbol) {
		if(this.a_iAnomPos != 0 && a_cAnomSymbol == '?')
			return 'x';
		return a_cAnomSymbol;
	}
	
	private void clear() {
		this.a_bIsAldose = true;
		
		this.a_aAnomPos = new LinkedList<Integer>();
		this.a_mPosToChar = new TreeMap<Integer, Character>();
	}
}
