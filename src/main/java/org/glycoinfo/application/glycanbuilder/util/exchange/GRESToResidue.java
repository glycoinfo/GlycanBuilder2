package org.glycoinfo.application.glycanbuilder.util.exchange;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.glycoinfo.WURCSFramework.util.exchange.GRESToTrivialName;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.BRIDGE;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.MSCORE;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;

public class GRESToResidue {

	private Residue a_oResidue;
	
	private int a_iAnomerPosition = 0;
	private char a_cAnomerSymbol = '?';
	private char a_cRingSize = '?';	
	
	private GRES a_oGRES;

	ArrayList<String> a_aModifications = new ArrayList<String>();
	
	public GRES getGRES() {
		return this.a_oGRES;
	}
	
	public Residue getResidue() {
		return this.a_oResidue;
	}
	
	public char getRingSize() {
		return this.a_cRingSize;
	}
	
	public ArrayList<String> getModifications() {
		return this.a_aModifications;
	}
	
	public void start(GRES a_oGRES) throws Exception {
		MSCORE a_oMSCORE = a_oGRES.getMS().getCoreStructure();
		this.a_oGRES = a_oGRES;

		this.a_iAnomerPosition = a_oMSCORE.getAnomericPosition();
		this.a_cAnomerSymbol = a_oMSCORE.getAnomericSymbol();
		this.a_cRingSize = makeRingSize(a_oMSCORE);
		
		GRESToTrivialName a_oGRESToTrivialName = new GRESToTrivialName();
		a_oGRESToTrivialName.start(getGRES());
		
		String a_sTrivialName = a_oGRESToTrivialName.getTrivialName().replace("5", "");
		Residue a_oRES = ResidueDictionary.newResidue(a_sTrivialName);
		
		if(NonSymbolicResidueDictionary.hasResidueType(a_sTrivialName)) {
			a_oRES.getType().changeDescription(a_oGRESToTrivialName.getCarbBankNotation());
		}
		
		if(!a_oGRES.getMS().getString().contains("<Q>")  && a_oRES.getTypeName().equals("Sugar"))
			throw new WURCSToGlycanException(a_oGRES.getMS().getString() + " is not handled in GlycanBuilder");
		
		a_oRES.setWasSticky(isSticky(a_sTrivialName));
		a_oRES.setAlditol(this.isAlditol());
		a_oRES.setAldehyde(this.isAldehyde());
	
		a_oRES.setAnomericCarbon(this.checkAnomerPosition());
		a_oRES.setAnomericState(this.checkAnomerSymbol());
		a_oRES.setChirality(a_oGRESToTrivialName.getConfiguration().charAt(0));
		a_oRES.setRingSize(a_oRES.isAlditol() ? 'o' : this.a_cRingSize);
		
		/** add modificaiton */
		this.a_aModifications = a_oGRESToTrivialName.getModifications();
		this.checkUnsaturation(a_oGRES);
		this.a_oResidue = a_oRES;
	}
	
	private boolean isSticky(String a_sSugarname) {
		if(a_sSugarname.contains("Fuc") || a_sSugarname.contains("Xyl"))
			return true;
		
		return false;
	}
	
	private char makeRingSize(MSCORE a_oMSCORE) {
		int a_iAnomerPos = a_oMSCORE.getAnomericPosition();
		LinkedList<BRIDGE> a_aBRIDGE = a_oMSCORE.getDivalentSubstituents();
		
		if(a_iAnomerPos == 0 || a_iAnomerPos == -1 || a_iAnomerPos == 3) return '?';
		if(a_aBRIDGE.isEmpty()) return '?';
		
		int a_iEndPos = -1;
		
		for(BRIDGE a : a_aBRIDGE) {
			if(a.getMAP().equals("") && a.getStartPositions().contains(a_iAnomerPos)) {
				a_iEndPos = a.getEndPositions().get(0);
			}
		}
		
		//1-4, 2-5 is franose
		//1-5, 2-6 is pyranose
		// 3 : anomeric position -> WURCS=2.0/1,1,0/[h2a1221h-3x_3-8]/1/
		if(this.a_iAnomerPosition > 1) {
			a_iEndPos = a_iEndPos - this.a_iAnomerPosition + 1;
		}
		
		if(a_iEndPos == 4) return 'f';
		if(a_iEndPos == 5) return 'p';
		return '?';
	}
	
	private boolean isAlditol() {
		String a_sSkeletonCode = this.a_oGRES.getMS().getString();	
	
		if(a_sSkeletonCode.indexOf("u") == 0 || a_sSkeletonCode.indexOf("U") == 1)
			return false;
		
		int a_iAlcoholIndex = a_sSkeletonCode.indexOf("h");
		if(a_iAlcoholIndex == -1 || a_iAlcoholIndex > 3) return false;
		if(a_iAlcoholIndex == (this.a_iAnomerPosition)) return true;
		
		return false;
	}
	
	private boolean isAldehyde() {
		String a_sSkeletonCode = this.a_oGRES.getMS().getCoreStructure().getString();

		if(this.a_cAnomerSymbol == 'o' && a_sSkeletonCode.equals("<Q>"))
			return true;
		if(a_sSkeletonCode.indexOf("o") == -1 && a_sSkeletonCode.indexOf("O") == -1)
			return false;
		if(a_sSkeletonCode.indexOf("o") == this.a_iAnomerPosition)
			return true;
		if(a_sSkeletonCode.indexOf("O") == this.a_iAnomerPosition)
			return true;
		
		return false;
	}
	
	private char checkAnomerSymbol() {
		if(this.a_cAnomerSymbol == 'x') return '?';
		if(this.a_cAnomerSymbol == 'o') return '?';
		return this.a_cAnomerSymbol;
	}
	
	private char checkAnomerPosition() {
		String a_sSkeletonCode = this.a_oGRES.getMS().getCoreStructure().getSkeletonCode();
		
		if(this.a_iAnomerPosition == -1) return '?';
		if(this.a_iAnomerPosition == 0) {
			if(a_sSkeletonCode.contains("o") || a_sSkeletonCode.contains("O"))
				return '?';
			if(a_sSkeletonCode.contains("u") || a_sSkeletonCode.contains("U"))
				return '?';
			if(a_sSkeletonCode.equals("<Q>"))
				return '1';
			
			return '?';
		}
		
		return String.valueOf(this.a_iAnomerPosition).charAt(0);
	}
	
	private void checkUnsaturation (GRES a_oGRES) {
		String a_sSC = a_oGRES.getMS().getCoreStructure().getSkeletonCode();
		String a_sEnx = "";
		ArrayList<Integer> a_aPoss = new ArrayList<Integer>();
		
		for(int i = 0; i < a_sSC.length(); i++) {
			if(a_sSC.charAt(i) == 'e' || 
				a_sSC.charAt(i) == 'E' || 
				a_sSC.charAt(i) == 'f' || 
				a_sSC.charAt(i) == 'F' || 
				a_sSC.charAt(i) == 'z' || 
				a_sSC.charAt(i) == 'Z') a_aPoss.add(i + 1);
		}
		
		for(Iterator<Integer> i = a_aPoss.iterator(); i.hasNext();) {
			a_sEnx += i.next();
			if(i.hasNext()) a_sEnx +=",";
		}
		
		if(a_sEnx.length() != 0) {
			a_sEnx += "*en";
			this.a_aModifications.add(a_sEnx);
		}		
	}
}
