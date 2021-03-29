package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.glycoinfo.GlycanFormatconverter.Glycan.Monosaccharide;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.BRIDGE;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.MSCORE;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;

import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;

import java.util.ArrayList;
import java.util.LinkedList;

public class GRESToResidue {

	private Residue residue;
	
	private int anomPosition = 0;
	private char anomSymbol = '?';
	private char ringSize = '?';
	
	private GRES gres;

	private ArrayList<String> modifications = new ArrayList<>();
	
	public GRES getGRES() {
		return this.gres;
	}
	
	public Residue getResidue() {
		return this.residue;
	}
	
	public char getRingSize() {
		return this.ringSize;
	}
	
	public ArrayList<String> getModifications() {
		return this.modifications;
	}
	
	public void start(GRES _gres) throws Exception {
		MSCORE mscore = _gres.getMS().getCoreStructure();
		this.gres = _gres;

		this.anomPosition = mscore.getAnomericPosition();
		this.anomSymbol = mscore.getAnomericSymbol();
		this.ringSize = makeRingSize(mscore);

		TrivialNameConverter trinConv = new TrivialNameConverter();
		trinConv.start(_gres);
		String trivialName = trinConv.getTrivialName().replace("5", "");

		ResidueType newType = ResidueDictionary.findResidueType(trivialName);
		Residue residue = new Residue(newType);

		// generate monosaccharide legend
		if(NonSymbolicResidueDictionary.hasResidueType(trivialName)) {
			residue.getType().changeDescription(trinConv.getIUPACNotation());
		}

		if(!_gres.getMS().getString().contains("<Q>")  && residue.getTypeName().equals("Sugar"))
			throw new WURCSToGlycanException(_gres.getMS().getString() + " is not handled in GlycanBuilder");
		
		residue.setWasSticky(isSticky(trivialName));
		residue.setAlditol(this.isAlditol());
		residue.setAldehyde(this.isAldehyde());
	
		residue.setAnomericCarbon(this.checkAnomerPosition());
		residue.setAnomericState(this.checkAnomerSymbol());
		residue.setChirality(getConfiguration(((Monosaccharide) trinConv.getNode()).getStereos()));
		residue.setRingSize(residue.isAlditol() ? 'o' : this.ringSize);
		
		// add modificaiton
		this.modifications = trinConv.getModifications();
		this.residue = residue;
	}
	
	private boolean isSticky(String _trivialName) {
		return _trivialName.contains("Fuc") || _trivialName.contains("Xyl");
	}
	
	private char makeRingSize(MSCORE _mscore) {
		int anomPos = _mscore.getAnomericPosition();
		LinkedList<BRIDGE> bridge = _mscore.getDivalentSubstituents();
		
		if(anomPos == 0 || anomPos == -1 || anomPos == 3) return '?';
		if(bridge.isEmpty()) return '?';
		
		int endPos = -1;
		
		for(BRIDGE a : bridge) {
			if(a.getMAP().equals("") && a.getStartPositions().contains(anomPos)) {
				endPos = a.getEndPositions().get(0);
			}
		}
		
		//1-4, 2-5 is franose
		//1-5, 2-6 is pyranose
		// 3 : anomeric position -> WURCS=2.0/1,1,0/[h2a1221h-3x_3-8]/1/
		if(this.anomPosition > 1) {
			endPos = endPos - this.anomPosition + 1;
		}
		
		if(endPos == 4) return 'f';
		if(endPos == 5) return 'p';
		return '?';
	}
	
	private boolean isAlditol() {
		String skeletonCode = this.gres.getMS().getString();
	
		if(skeletonCode.indexOf("u") == 0 || skeletonCode.indexOf("U") == 1)
			return false;
		
		int alcohol = skeletonCode.indexOf("h");
		if(alcohol == -1 || alcohol > 3) return false;
		if(alcohol == (this.anomPosition)) return true;
		
		return false;
	}
	
	private boolean isAldehyde() {
		String skeletonCode = this.gres.getMS().getCoreStructure().getString();

		if(this.anomSymbol == 'o' && skeletonCode.equals("<Q>"))
			return true;
		if(!skeletonCode.contains("o") && !skeletonCode.contains("O"))
			return false;
		if(skeletonCode.indexOf("o") == this.anomPosition)
			return true;
		if(skeletonCode.indexOf("O") == this.anomPosition)
			return true;
		
		return false;
	}
	
	private char checkAnomerSymbol() {
		if(this.anomSymbol == 'x') return '?';
		if(this.anomSymbol == 'o') return '?';
		return this.anomSymbol;
	}
	
	private char checkAnomerPosition() {
		String skeletonCode = this.gres.getMS().getCoreStructure().getSkeletonCode();
		
		if(this.anomPosition == -1) return '?';
		if(this.anomPosition == 0) {
			if(skeletonCode.contains("o") || skeletonCode.contains("O"))
				return '?';
			if(skeletonCode.contains("u") || skeletonCode.contains("U"))
				return '?';
			if(skeletonCode.equals("<Q>"))
				return '1';
			
			return '?';
		}
		
		return String.valueOf(this.anomPosition).charAt(0);
	}

	private char getConfiguration (LinkedList<String> _stereos) {
		if (_stereos.isEmpty()) return '?';
		String ret = _stereos.getFirst();
		if (ret.length() == 3) return '?';
		return ret.substring(0, 1).toUpperCase().charAt(0);
	}
}
