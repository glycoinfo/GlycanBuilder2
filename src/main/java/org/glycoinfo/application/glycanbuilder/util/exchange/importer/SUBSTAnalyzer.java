package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.GlycanFormatconverter.Glycan.BaseCrossLinkedTemplate;
import org.glycoinfo.GlycanFormatconverter.util.exchange.WURCSGraphToGlyContainer.MAPAnalyzer;
import org.glycoinfo.WURCSFramework.util.array.WURCSImporter;
import org.glycoinfo.WURCSFramework.wurcs.array.LIP;
import org.glycoinfo.WURCSFramework.wurcs.array.LIPs;
import org.glycoinfo.WURCSFramework.wurcs.array.MOD;
import org.glycoinfo.WURCSFramework.wurcs.array.MS;
import org.glycoinfo.WURCSFramework.wurcs.graph.CarbonDescriptor;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.BRIDGE;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GLIN;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.SUBST;
import org.glycoinfo.application.glycanbuilder.dataset.CrossLinkedSubstituentDictionary;
import org.glycoinfo.application.glycanbuilder.dataset.NativeMonosaccharideDictionary;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;

import java.util.ArrayList;
import java.util.LinkedList;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.glycoinfo.application.glycanbuilder.dataset.SubstituentMAPDictionary;

public class SUBSTAnalyzer {

	private final ArrayList<String> modifications;

	public SUBSTAnalyzer(ArrayList<String> _a_aModifications) {
		this.modifications = _a_aModifications;
	}
	
	public SUBSTAnalyzer() {
		 this.modifications = new ArrayList<>();
	}

	/**
	 * Attaches a substituent named by this builder's own MAP table, for the ones the converter's
	 * table has no template for.
	 * @param _subst Substituent as it appears in the WURCS.
	 * @param _residue Residue it hangs off.
	 * @param _ms Monosaccharide being read, for the linkage type.
	 */
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
		
		// Only the ring is written without being a substituent. Skipping every bridge that starts at
		// the anomeric carbon also discarded a 1,6-anhydro, which genuinely starts there.
		BRIDGE ring = RingBridge.find(_gres.getMS().getCoreStructure());
		for(BRIDGE bridge : _gres.getMS().getCoreStructure().getDivalentSubstituents()) {
			if(bridge == ring) continue;
			this.analyzeBRIDGE(bridge, _residue);
		}
		
		for(BRIDGE bridge : _gres.getMS().getDivalentSubstituents()) {
			if(bridge.getMAP().equals("")) continue;
			this.analyzeBRIDGE(bridge, _residue);
		}

		this.analyzeModificaitons(_residue);
	}
	
	/** 
	 * create bridge substituent 
	 * @param _glin
	 * @return
	 * @throws Exception
	 */
	public Residue MAPToBridge(GLIN _glin) throws Exception {
		if(_glin.getMAP().equals("")) return null;

		MAPAnalyzer mapAnalyzer = new MAPAnalyzer();
		mapAnalyzer.start(_glin.getMAP());
		BaseCrossLinkedTemplate crossTemp = mapAnalyzer.getCrossTemplate();

		return new Residue(CrossLinkedSubstituentDictionary.getCrossLinkedSubstituent(crossTemp.getIUPACnotation()));
	}
	
	/**
	 * create substituent for fragment
	 * @param _glin
	 * @return
	 * @throws Exception
	 */
	public Residue MAPToFragment(GLIN _glin) throws Exception {
		if(_glin.getMAP().equals("")) return null;

		ResidueType substituentType = SubstituentMAPDictionary.findResidueTypeByMAP(_glin.getMAP());
		if(substituentType == null)
			throw new Exception("This MAP is not support in the GlycanBuilder2:" + _glin.getMAP());

		return ResidueDictionary.newResidue(substituentType.getName());
	}
	
	private void analyzeSUBST(SUBST _subst, Residue _residue, MS _ms) throws Exception {
		Linkage linkage = new Linkage();

		char[] positions = this.makePosition(_subst.getPositions());
		NativeMonosaccharideDictionary.Entry entry =
				NativeMonosaccharideDictionary.forResidueName(_residue.getTypeName());

		// A group the name owns as a bare MAP is part of the residue and has no substituent behind it
		// - apiose's hydroxymethyl branch. Asking for one would fail, so ask before resolving.
		if(entry != null && entry.getOwnMAPs().contains(positions[0] + "*" + _subst.getMAP().substring(1)))
			return;

		ResidueType substituentType = SubstituentMAPDictionary.findResidueTypeByMAP(_subst.getMAP());
		if(substituentType == null)
			throw new Exception("This MAP is not support in the GlycanBuilder2:" + _subst.getMAP());

		String subNotation = positions[0] + "*" + substituentType.getName();

		// A substituent the residue's name already owns must not be drawn as well: the exporter takes
		// it from the same table, so drawing it would write it out twice.
		if(entry != null && entry.owns(subNotation)) return;

		// A residue that already carries an amine where this substituent lands is not carrying an
		// N-linked group of its own: the nitrogen in the MAP is the residue's, and what hangs off it
		// is the O-linked equivalent. GlcN with 2*NSO/3=O/3=O is GlcN bearing a sulfate, not an NS.
		if(isNSubstituent(_residue, _subst.getMAP(), _subst.getPositions().get(0))) {
			substituentType = SubstituentMAPDictionary.findResidueTypeByMAP(
					_subst.getMAP().replaceFirst("N", "O"));
			if(substituentType == null)
				throw new Exception("This MAP is not support in the GlycanBuilder2:" + _subst.getMAP());
		}

		linkage.setLinkagePositions(positions);

		// set LinkageType
		linkage.setParentLinkageType(checkLinkageTypeOfMAP(_subst, _ms));
		linkage.setChildLinkageType(LinkageType.NONMONOSACCHARID);

		// set probability annotation
		this.extractProbabilityAnnotation(_subst, _ms, linkage);

		Residue substituent = ResidueDictionary.newResidue(substituentType.getName());
		this.checkNode(substituent, substituentType.getName());
		substituent.setParentLinkage(linkage);
		_residue.addChild(substituent, substituent.getParentLinkage().getBonds());
	}
	
	private void analyzeBRIDGE(BRIDGE _bridge, Residue _residue) throws Exception {
		Linkage linkage = new Linkage();
		MAPAnalyzer mapAnalyzer = new MAPAnalyzer();
		mapAnalyzer.start(_bridge.getMAP().equals("") ? "*O*" : _bridge.getMAP());
		BaseCrossLinkedTemplate crossTemp = mapAnalyzer.getCrossTemplate();

		if (crossTemp == null)
			throw new Exception("This MAP is not support in the GlycanBuilder2:" + _bridge.getMAP());

		char[] startPos = this.makePosition(_bridge.getStartPositions());
		char[] endPos = this.makePosition(_bridge.getEndPositions());
		
		linkage.setLinkagePositions(endPos, startPos, '1');
		
		Residue bridge = new Residue(CrossLinkedSubstituentDictionary.getCrossLinkedSubstituent(crossTemp.getIUPACnotation()));
		this.checkNode(bridge, crossTemp.getIUPACnotation());
		_residue.addChild(bridge, linkage.getBonds());
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
		NativeMonosaccharideDictionary.Entry entry =
				NativeMonosaccharideDictionary.forResidueName(_residue.getTypeName());

		for(String mod : this.modifications) {
			Linkage linkage = new Linkage();
			String[] subNotations = mod.split("\\*");

			// a modification the residue's name already implies is recorded on the residue and not
			// built as a residue of its own - the exporter puts it back from the same table
			if(entry != null) {
				_residue.addModification(mod);
				if(entry.getModifications().contains(mod)) continue;
			}

			if(subNotations[0].contains(",")) {
				char[] positions = new char[subNotations[0].length()];
				for(int i = 0; i < subNotations[0].length(); i++) {
					if(subNotations[0].charAt(i) != ',') positions[i] = subNotations[0].charAt(i);
				}				
				
				linkage.setLinkagePositions(new char[]{positions[0]}, new char[]{positions[2]}, '1');
			}else 
				linkage.setLinkagePositions(new char[] {subNotations[0].charAt(0)});
			
			Residue substituent = ResidueDictionary.newResidue(subNotations[1]);
			this.checkNode(substituent, subNotations[1]);
			_residue.addChild(substituent, linkage.getBonds());
		}
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
	}
	
	private void checkNode(Residue _residue, String _map) throws WURCSToGlycanException {
		if(_residue.getTypeName().equals("Sugar"))
			throw new WURCSToGlycanException(_map + " is not handled in GlycanBuilder");
	}

	private boolean isNSubstituent(Residue _residue, String _map, int _pos) {
		boolean isNType = false;
		boolean isNSub = this.isNTypes(_map);
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
			else isNType = _residue.getType().getSuperclass().equals("Nonulosonate") && isNSub;
		}
		if(_pos == 7) {
			if(_residue.getTypeName().contains("Leg") && isNSub) isNType = true;
			else isNType = _residue.getType().getSuperclass().equals("Nonulosonate") && isNSub;
		}
		
		return isNType;
	}

	private LinkageType checkLinkageTypeOfMAP (SUBST _subst, MS _ms) {
		String skeletonCode = _ms.getSkeletonCode();
		int position = _subst.getPositions().getFirst();
		if(position != -1) {
			char carbondescriptor = skeletonCode.charAt(position - 1);
			CarbonDescriptor cdDesc = CarbonDescriptor.forCharacter(carbondescriptor, (position == 1 || skeletonCode.length() == position));
			if(cdDesc.equals(CarbonDescriptor.SS3_CHIRAL_S_U) || cdDesc.equals(CarbonDescriptor.SS3_CHIRAL_R_U) ||
					cdDesc.equals(CarbonDescriptor.SS3_CHIRAL_s_U) || cdDesc.equals(CarbonDescriptor.SS3_CHIRAL_r_U)) {
				return LinkageType.H_LOSE;
			}
		}
		
		if(_subst.getMAP().startsWith("*O")) return LinkageType.H_AT_OH;
		
		return LinkageType.DEOXY;
	}

	/**
	 * Whether the substituent hangs off a nitrogen and carries something beyond it - an N-sulfate, an
	 * N-acetyl and so on, but not a bare amine, which is a nitrogen and nothing more.
	 *
	 * <p>This used to be a list of nine templates named in glycanformatconverter. Read off the MAP it
	 * is the same set: compared over every substituent MAP residue_types holds, all 33, the two
	 * agree without exception.
	 */
	private boolean isNTypes(String _map) {
		return _map.startsWith("*N") && _map.length() > "*N".length();
	}
}
