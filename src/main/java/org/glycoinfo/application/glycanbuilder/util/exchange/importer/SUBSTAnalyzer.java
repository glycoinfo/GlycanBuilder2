package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.util.array.WURCSImporter;
import org.glycoinfo.WURCSFramework.util.oldUtil.SubstituentTemplate;
import org.glycoinfo.WURCSFramework.wurcs.array.LIP;
import org.glycoinfo.WURCSFramework.wurcs.array.LIPs;
import org.glycoinfo.WURCSFramework.wurcs.array.MOD;
import org.glycoinfo.WURCSFramework.wurcs.array.MS;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.BRIDGE;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GLIN;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.SUBST;
import org.glycoinfo.application.glycanbuilder.dataset.CrossLinkedSubstituentDictionary;
import org.glycoinfo.GlycanFormatconverter.util.TrivialName.TrivialNameDictionary;
import org.glycoinfo.GlycanFormatconverter.util.TrivialName.ModifiedMonosaccharideDescriptor;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;

import java.util.ArrayList;
import java.util.LinkedList;

public class SUBSTAnalyzer {

	private ArrayList<String> modifications;

	public SUBSTAnalyzer(ArrayList<String> _a_aModifications) {
		this.modifications = _a_aModifications;
	}
	
	public SUBSTAnalyzer() {
		 this.modifications = new ArrayList<String>();
	}

	public void start(GRES _gres, Residue _residue) throws Exception {
		MS ms = new WURCSImporter().extractMS(_gres.getMS().getString());

		//TODO: TrivialNameDictionaryとModifiedMonosaccharideDescriptorを見る
		for(SUBST subst : _gres.getMS().getCoreStructure().getSubstituents()) {
			if(subst.getMAP().equals("")) continue;
			this.analyzeSUBST(subst, _residue, ms);
		}		
		
		for(SUBST subst : _gres.getMS().getSubstituents()) {
			if(subst.getMAP().equals("")) continue;
			this.analyzeSUBST(subst, _residue, ms);
		}
		//End of TODO
		
		for(BRIDGE bridge : _gres.getMS().getCoreStructure().getDivalentSubstituents()) {
			if(bridge.getStartPositions().contains(_gres.getMS().getCoreStructure().getAnomericPosition()))
				continue;
			this.analyzeBRIDGE(bridge, _residue);
		}
		
		for(BRIDGE bridge : _gres.getMS().getDivalentSubstituents()) {
			if(bridge.getMAP().equals("")) continue;
			this.analyzeBRIDGE(bridge, _residue);
		}

		//TODO: TrivialNameDictionaryとModifiedMonosaccharideDescriptorを見る
		this.analyzeModificaitons(_residue);
		//End of TODO
		
		return;
	}
	
	/** 
	 * create bridge substituent 
	 * @param _glin
	 * @return
	 * @throws Exception
	 */
	public Residue MAPToBridge(GLIN _glin) throws Exception {
		if(_glin.getMAP().equals("")) return null;
		
		SubstituentTemplate subTemp = SubstituentTemplate.forMAP(_glin.getMAP());
		return new Residue(CrossLinkedSubstituentDictionary.getCrossLinkedSubstituent(subTemp.getIUPACnotation()));
	}
	
	/**
	 * create substituent for fragment
	 * @param _glin
	 * @return
	 * @throws Exception
	 */
	public Residue MAPToFragment(GLIN _glin) throws Exception {
		if(_glin.getMAP().equals("")) return null;
		
		SubstituentTemplate subTemp = SubstituentTemplate.forMAP(_glin.getMAP());
		Residue fragment = ResidueDictionary.newResidue(subTemp.getIUPACnotation());

		return fragment;
	}
	
	private void analyzeSUBST(SUBST _subst, Residue _residue, MS _ms) throws Exception {
		Linkage linkage = new Linkage();
		SubstituentTemplate subTemp = SubstituentTemplate.forMAP(_subst.getMAP());
		char[] positions = this.makePosition(_subst.getPositions());
		String subNotation = positions[0] + "*" + subTemp.getIUPACnotation();

		if(subTemp.getIUPACnotation().equals(""))
			throw new Exception(_subst.getMAP() + " can not handled in GlycanBuilder");
		
		// check native substituent
		TrivialNameDictionary trivDict = TrivialNameDictionary.forThreeLetterCode(_residue.getTypeName());
		ModifiedMonosaccharideDescriptor modDesc = ModifiedMonosaccharideDescriptor.forTrivialName(_residue.getTypeName());
		if(trivDict != null) {
			if(trivDict.getSubstituents().contains(subNotation)) return;
		}
		if (modDesc != null) {
			if (modDesc.getSubstituents().contains(subNotation)) return;
		}

		// change n_sulfate with hexosamine
		if(isNSubstituent(_residue, subTemp, _subst.getPositions().get(0))) {
			subTemp = SubstituentTemplate.forMAP(_subst.getMAP().replaceFirst("N", "O"));
			subNotation = _subst.getPositions().get(0) + "*" + subTemp.getIUPACnotation();
		}
		
		linkage.setLinkagePositions(positions);
		
		// set LinkageType
		linkage.setParentLinkageType(checkLinkageTypeOfMAP(_subst, _ms));
		linkage.setChildLinkageType(LinkageType.NONMONOSACCHARID);

		// set probability annotation
		this.extractProbabilityAnnotation(_subst, _ms, linkage);

		Residue substituent = ResidueDictionary.newResidue(subTemp.getIUPACnotation());
		this.checkNode(substituent, subTemp.getIUPACnotation());
		substituent.setParentLinkage(linkage);
		_residue.addChild(substituent, substituent.getParentLinkage().getBonds());
		
		return;
	}
	
	private void analyzeBRIDGE(BRIDGE _bridge, Residue _residue) throws Exception {
		Linkage linkage = new Linkage();
		String map = _bridge.getMAP().equals("") ? "*o" : _bridge.getMAP();
		SubstituentTemplate subTemp = SubstituentTemplate.forMAP(map);
		
		char[] startPos = this.makePosition(_bridge.getStartPositions());
		char[] endPos = this.makePosition(_bridge.getEndPositions());
		
		linkage.setLinkagePositions(endPos, startPos, '1');
		
		Residue bridge = ResidueDictionary.newResidue(subTemp.getIUPACnotation());
		this.checkNode(bridge, subTemp.getIUPACnotation());
		_residue.addChild(bridge, linkage.getBonds());
		
		return;
	}
	
	private char[] makePosition(LinkedList<Integer> _positions) {
		char[] position = new char[_positions.size()];
		
		for(int i = 0; i < _positions.size(); i++) {
			String unit = String.valueOf(_positions.get(i));
			if(unit.equals("-1")) position[i] = '?';
			else position[i] = unit.charAt(0);
		}
		
		return position;
	}
		
	private void analyzeModificaitons(Residue _residue) throws Exception {
		TrivialNameDictionary trivDict = TrivialNameDictionary.forThreeLetterCode(_residue.getTypeName());
		ModifiedMonosaccharideDescriptor modDesc = ModifiedMonosaccharideDescriptor.forTrivialName(_residue.getTypeName());

		for(String mod : this.modifications) {
			Linkage linkage = new Linkage();
			String[] subNotations = mod.split("\\*");
			SubstituentTemplate subTemp = SubstituentTemplate.forMAP("*" + subNotations[1]);

			if(trivDict != null) {
				_residue.addModification(mod);
				if(trivDict.getModifications().contains(mod)) continue;
			}
			if (modDesc != null) {
				_residue.addModification(mod);
				if(modDesc.getModifications().contains(mod)) continue;
			}
			
			if(subNotations[0].contains(",")) {
				char[] positions = new char[subNotations[0].length()];
				for(int i = 0; i < subNotations[0].length(); i++) {
					if(subNotations[0].charAt(i) != ',') positions[i] = subNotations[0].charAt(i);
				}				
				
				linkage.setLinkagePositions(new char[]{positions[0]}, new char[]{positions[2]}, '1');
			}else 
				linkage.setLinkagePositions(new char[] {subNotations[0].charAt(0)});
			
			Residue substituent = ResidueDictionary.newResidue(subTemp.getIUPACnotation());
			this.checkNode(substituent, subTemp.getIUPACnotation());
			_residue.addChild(substituent, linkage.getBonds());
		}
		
		return;
	}
	
	private void extractProbabilityAnnotation (SUBST _subst, MS _ms, Linkage _linkage) {
		for(MOD mod : _ms.getMODs()) {
			if(mod.getMAPCode().equals("")) continue;
			
			for(LIPs lips : mod.getListOfLIPs()) {
				for(LIP lip : lips.getLIPs()) {
					if((_subst.getMAP().equals(mod.getMAPCode())) && (_subst.getPositions().contains(lip.getBackbonePosition()))) {
						_linkage.getBonds().get(0).setProbabilityHigh(lip.getModificationProbabilityUpper());
						_linkage.getBonds().get(0).setProbabilityLow(lip.getModificationProbabilityLower());
					}
				}
			}
		}
		return;
	}
	
	private void checkNode(Residue _residue, String _map) throws WURCSToGlycanException {
		if(_residue.getTypeName().equals("Sugar"))
			throw new WURCSToGlycanException(_map + " is not handled in GlycanBuilder");
	}
	
	private boolean isNSubstituent(Residue _residue, SubstituentTemplate _subTemp, int _pos) {
		boolean isNType = false;
		boolean isNSub = this.isNTypes(_subTemp);
		String superclass = _residue.getType().getSuperclass();
		
		if(superclass.equals("Hexuronic acid")) return false;
		if(superclass.equals("N-Acetylhexosamine")) return false;
		
		if(_pos == 2 || _pos == 4) {
			if(_residue.getTypeName().equals("Bac") && isNSub) isNType = true;
			if(superclass.equals("Hexosamine") && isNSub) isNType = true;
			if(_residue.getTypeName().equals("Mur") && isNSub) isNType = true;
		}
		if(_pos == 5) {
			if(_residue.getTypeName().contains("Leg") && isNSub) isNType = true;
			else if(_residue.getTypeName().equals("Neu") && isNSub) isNType = true;
			else if(_residue.getType().getSuperclass().equals("Nonulosonate") && isNSub) isNType = true;
			else isNType = false;
		}
		if(_pos == 7) {
			if(_residue.getTypeName().contains("Leg") && isNSub) isNType = true;
			else if(_residue.getType().getSuperclass().equals("Nonulosonate") && isNSub) isNType = true;
			else isNType = false;
		}
		
		return isNType;
	}
	
	private LinkageType checkLinkageTypeOfMAP (SUBST _subst, MS _ms) {
		String skeletonCode = _ms.getSkeletonCode();
		int position = _subst.getPositions().getFirst();
		
		if(position != -1) {
			try {
				if(Integer.parseInt(String.valueOf(skeletonCode.charAt(position - 1))) > 4) return LinkageType.H_LOSE;
			} catch (NumberFormatException e) {}
		}
		
		if(_subst.getMAP().startsWith("*O")) return LinkageType.H_AT_OH;
		
		return LinkageType.DEOXY;
	}
	
	private boolean isNTypes(SubstituentTemplate _subTemp) {
		if(_subTemp.equals(SubstituentTemplate.N_SULFATE)) return true;
		if(_subTemp.equals(SubstituentTemplate.N_AMIDINO)) return true;
		if(_subTemp.equals(SubstituentTemplate.N_ACETYL)) return true;
		if(_subTemp.equals(SubstituentTemplate.N_DIMETHYL)) return true;
		if(_subTemp.equals(SubstituentTemplate.N_FORMYL)) return true;
		if(_subTemp.equals(SubstituentTemplate.N_GLYCOLYL)) return true;
		if(_subTemp.equals(SubstituentTemplate.N_METHYL)) return true;
		if(_subTemp.equals(SubstituentTemplate.N_SUCCINATE)) return true;
		if(_subTemp.equals(SubstituentTemplate.ETHANOLAMINE)) return true;
		
		return false;
	}
}
