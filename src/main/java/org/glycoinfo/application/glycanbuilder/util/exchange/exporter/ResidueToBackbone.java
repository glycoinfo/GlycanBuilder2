package org.glycoinfo.application.glycanbuilder.util.exchange.exporter;

import java.util.Collection;
import java.util.LinkedList;

import org.eurocarbdb.MolecularFramework.sugar.*;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.wurcs.graph.Backbone;
import org.glycoinfo.WURCSFramework.util.property.AtomicProperties;
import org.glycoinfo.WURCSFramework.wurcs.graph.BackboneCarbon;
import org.glycoinfo.WURCSFramework.wurcs.graph.CarbonDescriptor;
import org.glycoinfo.WURCSFramework.wurcs.graph.Modification;

public class ResidueToBackbone {

	private Backbone backbone;
	
	private char anomSymbol = '?';
	private int anomPosition = 0;
	private char configuration = '?';
	
	private Residue residue;
	private boolean a_bIsRootOfFragment = false;

	private LinkedList<Modification> unknownModPos = new LinkedList<Modification>();
	
	public Residue getResidue() {
		return this.residue;
	}
	
	public Backbone getBackbone() {
		return this.backbone;
	}
	
	public LinkedList<Modification> getCoreModifications() {
		return this.unknownModPos;
	}
	
	public void setRootOfFramgents() {
		this.a_bIsRootOfFragment = true;
	}
	
	public void start(Residue _residue) throws Exception {
		if (_residue.getType().getSuperclass().equals("Unknown")) {
			throw new Exception (_residue.getTypeName() + " can not be converted to SkeletonCode.");
		}

		this.residue = _residue;
		this.anomPosition = checkAnomericSymbolCharactor(_residue.getAnomericCarbon());
		this.anomSymbol = (_residue.isAldehyde() || _residue.isAlditol()) ? 'o' : _residue.getAnomericState();
		this.configuration = _residue.getChirality();
			
		String classNotation = (_residue.getType().getCompositionClass().equals("Sugar")) ? "sug" : _residue.getType().getCompositionClass();
		Superclass superClass = Superclass.forName(classNotation.toLowerCase());
		if (_residue.getType().getName().equals("Assigned")) superClass = Superclass.SUG;

		int carbon = superClass.getCAtomCount();
		
		if(carbon == 0) {
			this.backbone = new Backbone();
			this.backbone.setAnomericSymbol(this.anomSymbol);
			return;
		}
		
		ResidueAnalyzer residueAnalyzer = new ResidueAnalyzer();
		residueAnalyzer.ResidueToSkeletonCode(_residue);

		this.anomPosition = residueAnalyzer.getAnomericPosition();
		this.anomSymbol = residueAnalyzer.getAnomericSymbol();
		this.configuration = residueAnalyzer.getConfiguration();
		String skeletonCode = residueAnalyzer.getSkeletonCode();
		
		for(String map : residueAnalyzer.getUnknownMAPs()) {
			this.unknownModPos.add( new Modification(map));
		}
		
		// check unknown anomeric position
		if(this.anomPosition == 0 && this.anomSymbol == '?') {
			if(!hasParent()) {
				if(!_residue.isAldehyde() && !_residue.isAlditol()) {
					skeletonCode = skeletonCode.replaceAll("o", "u");
					skeletonCode = skeletonCode.replaceAll("O", "U");
				}
			}else if (this.hasParent() && 
					(_residue.getParent().getType().getSuperclass().equals("Bridge"))) {
			} else {
				if(!_residue.isAldehyde()) {
					if(skeletonCode.contains("o")) {
						this.anomPosition = skeletonCode.indexOf("o") + 1;
						skeletonCode = skeletonCode.replaceFirst("o", "a");
					}else if(skeletonCode.contains("O")) {
						this.anomPosition = skeletonCode.indexOf("O") + 1;
						skeletonCode = skeletonCode.replaceFirst("O", "a");
					}
				}
			}
		}
		
		StringBuilder scNotation = new StringBuilder(skeletonCode);
		// extract substituent
		if(_residue.getParentLinkage() != null) {
			this.replaceCarbonDescriptorByLinkage(scNotation, _residue.getParentLinkage(), true);
		}
		for(Linkage linkage : _residue.getChildrenLinkages()) {
			this.replaceCarbonDescriptorByLinkage(scNotation, linkage, false);
		}
		
		// make Backbone
		Backbone backbone_tbd = new Backbone();
		backbone_tbd.setAnomericPosition(this.anomPosition);
		backbone_tbd.setAnomericSymbol(this.anomSymbol);
		for(int i = 0; i < carbon; i ++) {
			char carbonDescriptor = scNotation.charAt(i);
			CarbonDescriptor carbonDescriptor_tbd = CarbonDescriptor.forCharacter(carbonDescriptor, ( i == 0 || i == carbon - 1));
			BackboneCarbon _backboneCarbon = new BackboneCarbon(backbone_tbd, carbonDescriptor_tbd);
			backbone_tbd.addBackboneCarbon(_backboneCarbon);
		}
	
		this.backbone = backbone_tbd;
	}
	
	private void replaceCarbonDescriptorByLinkage(StringBuilder _scNotation, Linkage _linkage, boolean _isParent) throws Exception {
		Residue residue = (_isParent) ? _linkage.getParentResidue() : _linkage.getChildResidue();
		Residue substituent = (residue.isSubstituent()) ? residue : null;
		boolean isSwap = false;
		
		Collection<Character> positions = _linkage.getParentPositions();
		if(positions.size() > 1) return;
		
		isSwap = (this.compareConnectAtom(_linkage, substituent, _isParent) < 0);
		Linkage parentLink = _linkage;
		
		if(_linkage.getParentResidue().isRepetition())
			parentLink = _linkage.getParentResidue().getParentLinkage();
		
		int pos = positions.iterator().next() == '?' ? -1 : Integer.parseInt(String.valueOf(positions.iterator().next()));
		
		if(pos == -1) return;
		char cd = _scNotation.charAt(pos - 1);
		char newCD = cd;
		
		LinkageType type0 = (_isParent) ? _linkage.getChildLinkageType() : parentLink.getParentLinkageType();
		LinkageType type1 = (_isParent) ? parentLink.getParentLinkageType() : _linkage.getChildLinkageType();
		
		if(type0 == LinkageType.H_LOSE) {
			newCD = this.replaceCarbonDescriptorByHydrogenLose(cd, isSwap);
		} else if(type0 == LinkageType.DEOXY && type1 != LinkageType.H_AT_OH) {
			newCD = (cd == 'c') ? 'x' :
					   (cd == 'C') ? 'X' : newCD;
		}
		
		_scNotation.replace(pos - 1, pos, newCD+"");
		
		if(!_isParent) return;
		if(this.anomPosition == 0 || this.anomPosition == -1) return;
		if(this.anomSymbol != 'a' || this.anomSymbol != 'b') return;
		
		char a_cAnomCD = _scNotation.charAt(this.anomPosition - 1);
		if(a_cAnomCD == 'x' || a_cAnomCD == 'X') return;
		
		char a_cAnomStereo = (this.anomSymbol == 'a') ? '1' : '2';
		if(this.configuration == 'L')
			a_cAnomStereo = (a_cAnomStereo == '1') ? '2' : '1';
		if(isSwap)
			a_cAnomStereo = (a_cAnomStereo == '1') ? '2' : '1';
		if(a_cAnomCD == 'X')
			a_cAnomStereo = (a_cAnomStereo == '1') ? '5' : '6';
		
		_scNotation.replace(this.anomPosition - 1, this.anomPosition, a_cAnomStereo+"");
	}
	
	private char replaceCarbonDescriptorByHydrogenLose(char _carbonDescriptor, boolean _isSwap) {
		char newCarbonDescriptor = (_carbonDescriptor == '1') ? '5' :
						(_carbonDescriptor == '2') ? '6' :
						(_carbonDescriptor == '3') ? '7' :
						(_carbonDescriptor == '4') ? '8' :
						(_carbonDescriptor == 'x') ? 'X' :
						(_carbonDescriptor == 'C') ? 'C' :
						(_carbonDescriptor == 'm') ? 'h' :
						(_carbonDescriptor == 'h') ? 'c' : _carbonDescriptor;
		if(_isSwap) {
			newCarbonDescriptor = (newCarbonDescriptor == '5') ? '6' :
					   (newCarbonDescriptor == '6') ? '5' :
					   (newCarbonDescriptor == '7') ? '8' :
					   (newCarbonDescriptor == '8') ? '7' : newCarbonDescriptor;
		}
		
		return newCarbonDescriptor;
	}
	
	private int compareConnectAtom(Linkage _linkage, Residue _substituent, boolean _isParent) throws Exception {
		LinkageType parentLinkType = (_isParent) ? _linkage.getChildLinkageType() : _linkage.getParentLinkageType();
		LinkageType childLinkType = (_isParent) ? _linkage.getParentLinkageType() : _linkage.getChildLinkageType();
		if(parentLinkType == LinkageType.H_AT_OH || childLinkType == LinkageType.H_AT_OH) return 0;
		
		if(_substituent == null) return 0;
		return this.compareConnectAtomOfSubstituent(_substituent, _isParent);
	}
	
	private int compareConnectAtomOfSubstituent(Residue _substituent, boolean _isParent) throws Exception {
		ResidueToModification res2mod = new ResidueToModification();
		res2mod.setSubstituentTemplate(_substituent);
		
		res2mod.start(_substituent);
		String a_sConnAtom = (_isParent) ? res2mod.getTailAtom() : res2mod.getHeadAtom();
		
		int a_iNumberOfFirstAtom = AtomicProperties.forSymbol(a_sConnAtom).getAtomicNumber();
		if(a_iNumberOfFirstAtom > 16) return 1;
		return -1;
	}
	
	private int checkAnomericSymbolCharactor(char _anomericPosition) {
		if(_anomericPosition == '?') return 0;
		return Integer.parseInt(String.valueOf(_anomericPosition));
	}
	
	private boolean hasParent() {
		if(this.residue.getParent() == null || this.residue.getParent().isReducingEnd() || residue.isComposition())
			return false;
		return true;
	}
}
