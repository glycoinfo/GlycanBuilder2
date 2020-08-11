package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.glycoinfo.WURCSFramework.util.oldUtil.GRESToTrivialName;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.BRIDGE;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.MSCORE;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;

import org.glycoinfo.GlycanFormatconverter.Glycan.Monosaccharide;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class GRESToResidue {

	private Residue residue;
	
	private int anomPosition = 0;
	private char anomSymbol = '?';
	private char ringSize = '?';
	
	private GRES gres;

	private ArrayList<String> modifications = new ArrayList<String>();
	
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
		
		GRESToTrivialName gres2trivialName = new GRESToTrivialName();
		gres2trivialName.start(getGRES());
		String trivialName = gres2trivialName.getTrivialName().replace("5", "");

		//TODO: GRESの変換処理が古すぎるので対策を考える必要がある
		//TODO: WURCSSequence2ではなくWURCSGraphが望ましい
		//今のままでは新たに追加した単糖を生成することができない, trivial nameを持つ単糖の修飾や置換基を十分にパースできていない
		//例えばPseのSkeletonCodeをパースした際にコア部分のL-gro-L-manNonを変換できているが細かな修飾までは対応できていない
		//Trivial nameの生成はIUPAC notationの生成処理に任せたほうが良い
		Monosaccharide mono = new Monosaccharide();

		//String trivialName = "";
		if (mscore.getSkeletonCode().indexOf("m") == 5) {
			if (trivialName.contains("Tal") || trivialName.contains("Alt") || trivialName.contains("Gul")) {
				trivialName = (mscore.getSkeletonCode().indexOf("m") + 1) + "d" + trivialName;
			}
			if (trivialName.contains("Hex")) {
				trivialName = "d" + trivialName;
			}
		}
		Residue residue = ResidueDictionary.newResidue(trivialName);
		
		if(NonSymbolicResidueDictionary.hasResidueType(trivialName)) {
			residue.getType().changeDescription(gres2trivialName.getCarbBankNotation());
		}
		
		if(!_gres.getMS().getString().contains("<Q>")  && residue.getTypeName().equals("Sugar"))
			throw new WURCSToGlycanException(_gres.getMS().getString() + " is not handled in GlycanBuilder");
		
		residue.setWasSticky(isSticky(trivialName));
		residue.setAlditol(this.isAlditol());
		residue.setAldehyde(this.isAldehyde());
	
		residue.setAnomericCarbon(this.checkAnomerPosition());
		residue.setAnomericState(this.checkAnomerSymbol());
		residue.setChirality(gres2trivialName.getConfiguration().charAt(0));
		residue.setRingSize(residue.isAlditol() ? 'o' : this.ringSize);
		
		// add modificaiton
		this.modifications = gres2trivialName.getModifications();
		this.checkUnsaturation(_gres);
		this.residue = residue;
	}
	
	private boolean isSticky(String _trivialName) {
		if(_trivialName.contains("Fuc") || _trivialName.contains("Xyl"))
			return true;
		
		return false;
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
		if(skeletonCode.indexOf("o") == -1 && skeletonCode.indexOf("O") == -1)
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
	
	private void checkUnsaturation (GRES _gres) {
		String skeletonCode = _gres.getMS().getCoreStructure().getSkeletonCode();
		String unsaturation = "";
		ArrayList<Integer> a_aPoss = new ArrayList<Integer>();
		
		for(int i = 0; i < skeletonCode.length(); i++) {
			if(skeletonCode.charAt(i) == 'e' ||
				skeletonCode.charAt(i) == 'E' ||
				skeletonCode.charAt(i) == 'f' ||
				skeletonCode.charAt(i) == 'F' ||
				skeletonCode.charAt(i) == 'z' ||
				skeletonCode.charAt(i) == 'Z') a_aPoss.add(i + 1);
		}
		
		for(Iterator<Integer> i = a_aPoss.iterator(); i.hasNext();) {
			unsaturation += i.next();
			if(i.hasNext()) unsaturation +=",";
		}
		
		if(unsaturation.length() != 0) {
			unsaturation += "*en";
			this.modifications.add(unsaturation);
		}		
	}
}
