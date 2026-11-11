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
import org.glycoinfo.application.glycanbuilder.util.SkeletonModificationComposition;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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

		this.analyzeModificaitons(_gres, _residue);
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

		// A bare star is a hydroxyl taken away and nothing put in its place: a deoxy, written as a MAP
		// where its carbon is not known - "?*", as compositions of dHex have it. It is the residue's,
		// as the deoxies of #258 are, not a substituent of no kind.
		if(_subst.getMAP().equals("*")) {
			_residue.addDeoxy(positions[0] + "*d");
			return;
		}

		ResidueType substituentType = SubstituentMAPDictionary.findResidueTypeByMAP(_subst.getMAP());
		if(substituentType == null)
			throw new Exception("This MAP is not support in the GlycanBuilder2:" + _subst.getMAP());

		// A C-linked group, *C or *CO, here replaces the hydroxyl, as N-linked ones do. On a carbon
		// whose descriptor has no hydrogen - 5 to 8, X - the hydroxyl stays and the group takes the
		// hydrogen's place, a branch this builder has no way to hold: read as the other, it weighed
		// an oxygen wrong and wrote a different skeleton back. Refused, saying so.
		if("C-type".equals(substituentType.getCompositionClass()) && _subst.getPositions().size() == 1
				&& _subst.getPositions().getFirst() > 0) {
			String skeleton = _ms.getSkeletonCode();
			int at = _subst.getPositions().getFirst();
			char carbon = (at <= skeleton.length()) ? skeleton.charAt(at - 1) : '?';
			if("5678X".indexOf(carbon) >= 0)
				throw new Exception(skeleton + " has a C-linked " + substituentType.getDescription()
						+ " on carbon " + at + ", which keeps its hydroxyl (" + carbon + " in WURCS): a branch "
						+ "GlycanBuilder2 cannot represent");
		}

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
		attach(_residue, substituent, substituent.getParentLinkage().getBonds(), String.valueOf(positions));
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
		attach(_residue, bridge, linkage.getBonds(), new String(startPos) + "-" + new String(endPos));
	}
	
	/**
	 * The carbons of a double bond as the skeleton writes them: "4,5" in a21eEA is "4*e" and "5*E".
	 * Null when the positions are not two carbons of the skeleton that are each a double-bond carbon of
	 * known geometry - and when either is a terminal carbon or the anomeric one. A double bond there, a
	 * glycal's C1=C2, changes what a carbon that is not a CHOH has, which the composition measured here
	 * does not describe: kept, zZ122h_1-5 weighed a water light. It is read as it was before.
	 */
	static List<String> doubleBondCarbons(GRES _gres, String _positions) {
		String skeleton = _gres.getMS().getCoreStructure().getSkeletonCode();
		int anomeric = _gres.getMS().getCoreStructure().getAnomericPosition();
		String[] positions = _positions.split(",");
		if(positions.length != 2) return null;
		List<String> carbons = new ArrayList<String>();
		for(String position : positions) {
			int carbon;
			try { carbon = Integer.parseInt(position); } catch (NumberFormatException notACarbon) { return null; }
			if(carbon <= 1 || carbon >= skeleton.length() || carbon == anomeric) return null;
			char code = skeleton.charAt(carbon - 1);
			if(!SkeletonModificationComposition.isDoubleBond(code)) return null;
			carbons.add(carbon + "*" + code);
		}
		return carbons;
	}

	/**
	 * Attach a group the WURCS gives this residue, or refuse the structure saying why.
	 *
	 * <p>The answer of addChild was not looked at, so a group the residue would not take was left out
	 * and the structure read without it: a Leg5,7Ac2 came back as a bare Leg, 84 Da light, because
	 * Leg did not offer 5 and 7 (#323). A structure that cannot be read as written is refused, never
	 * read as something else.
	 */
	private static void attach(Residue _residue, Residue _group, java.util.List<org.eurocarbdb.application.glycanbuilder.linkage.Bond> _bonds, String _where)
			throws WURCSToGlycanException {
		if(_residue.addChild(_group, _bonds)) return;
		// A row the writer has no entry for - Ole, Cym, whose composition already holds the 3-O-methyl
		// the WURCS gives them - is left as it was: the group was dropped and the mass came out right,
		// and what these rows should be is their own question (#312).
		if(NativeMonosaccharideDictionary.forResidueName(_residue.getTypeName()) == null) return;
		throw new WURCSToGlycanException(_group.getTypeName() + " at " + _where + " of " + _residue.getTypeName()
				+ " cannot be attached, as " + _residue.getTypeName() + " takes no group there, so it is not read");
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
		
	private void analyzeModificaitons(GRES _gres, Residue _residue) throws Exception {
		NativeMonosaccharideDictionary.Entry entry =
				NativeMonosaccharideDictionary.forResidueName(_residue.getTypeName());
		ArrayList<String> deoxies = new ArrayList<String>();

		for(String mod : this.modifications) {
			Linkage linkage = new Linkage();
			String[] subNotations = mod.split("\\*");

			// a modification the residue's name already implies is recorded on the residue and not
			// built as a residue of its own - the exporter puts it back from the same table
			if(entry != null) {
				_residue.addModification(mod);
				if(entry.getModifications().contains(mod)) continue;
			}

			// A deoxy the name does not imply stays on the residue too. It was built as a child named
			// "d" or "m", which no dictionary knows: drawn as "2d" or "6m", weighed as nothing while
			// its bond still took a water away, and refused by the WURCS writer (#258, #263). Other
			// skeleton modifications are still built as before: what they do to the hydroxyls a
			// derivatization reaches is not yet settled here, and a deoxy's is.
			if(SkeletonModificationComposition.isDeoxy(mod)) {
				deoxies.add(mod);
				continue;
			}

			// A double bond, "4,5*en", stays on the residue too, carbon by carbon as the skeleton has
			// it - "4*e" and "5*E" - since which of them keeps its oxygen is what the composition and
			// the derivatization sites depend on, and "en" does not say (#292). It was neither kept
			// nor built: the residue was the saturated acid, two hydrogens heavy.
			if(subNotations.length == 2 && subNotations[1].equals("en")) {
				List<String> carbons = doubleBondCarbons(_gres, subNotations[0]);
				if(carbons != null) {
					for(String carbon : carbons) _residue.addDoubleBond(carbon);
					continue;
				}
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
			// a skeleton modification built as a child, "en": kept as it was - what to do with one is
			// #292's, and refusing it here would refuse every structure with one
			_residue.addChild(substituent, linkage.getBonds());
		}

		// Not every deoxy the reader reports is one the type lacks. It is told which modifications a
		// name implies only for the names it has a descriptor for, so a 2,3,6-trideoxy hexose comes
		// back as Rho - whose composition already has all three - together with all three deoxies. Kept
		// as extra, they were taken away a second time.
		int extra = extraDeoxies(_gres, _residue, deoxies.size());
		for(int i = 0; i < deoxies.size(); i++) {
			if(i < extra)
				_residue.addDeoxy(deoxies.get(i));
			else if(!_residue.getModifications().contains(deoxies.get(i)))
				_residue.addModification(deoxies.get(i));  // implied by the type; the writer reads it
		}
	}

	/**
	 * How many of the reported deoxies the residue's type does not already account for.
	 *
	 * <p>Answered by weighing, against WURCSFramework: the residue as built so far - its type, with the
	 * substituents hung off it and no deoxy - and the monosaccharide as the WURCS describes it. Every
	 * oxygen the model has too many is a deoxy the type lacks. Weighing is what makes this exact for a
	 * type whose composition carries more than its skeleton: Cym, a 2,6-dideoxy 3-O-methyl hexose, has
	 * its methyl in its composition, C7H14O4, and no reading of the formula alone tells that apart from
	 * a seven-carbon skeleton.
	 *
	 * <p>Answered here, at reading, because this is the only point at which the WURCS is in hand. When
	 * the difference is not a whole number of oxygens - another modification the model does not yet
	 * weigh correctly, a type with no composition - every deoxy is taken as extra, as before.
	 */
	private static int extraDeoxies(GRES _gres, Residue _residue, int _reported) {
		if(_reported == 0) return 0;
		try {
			if(_residue.getType().getMolecule() == null) return _reported;

			double described = org.glycoinfo.WURCSFramework.util.array.mass.WURCSMassCalculator.calcMassWURCS(
					new org.glycoinfo.WURCSFramework.util.WURCSFactory(
							"WURCS=2.0/1,1,0/[" + _gres.getMS().getString() + "]/1/").getArray()).doubleValue();

			Residue root = ResidueDictionary.createReducingEnd("freeEnd");
			root.addChild(_residue.cloneSubtree());
			// underivatized: a new MassOptions is permethylated, and weighed that way the difference was
			// methyls, not oxygens
			org.eurocarbdb.application.glycanbuilder.massutil.MassOptions underivatized =
					new org.eurocarbdb.application.glycanbuilder.massutil.MassOptions();
			underivatized.setDerivatization(org.eurocarbdb.application.glycanbuilder.massutil.MassOptions.NO_DERIVATIZATION);
			org.eurocarbdb.application.glycanbuilder.Glycan built = new org.eurocarbdb.application.glycanbuilder.Glycan(
					root, false, underivatized);
			// a clone's linkages come back untyped, and an untyped bond to a methyl weighs it an oxygen
			// light (#281); typed as the readers type them, it weighs what it is
			new org.glycoinfo.application.glycanbuilder.converterWURCS2.LinkageTypeOptimizer().start(built);
			double modelled = built.computeMass();

			double oxygen = org.eurocarbdb.application.glycanbuilder.massutil.MassUtils.getAtom("O").getMainMass();
			long extra = Math.round((modelled - described) / oxygen);
			if(extra < 0 || extra > _reported || Math.abs(modelled - described - extra * oxygen) > 1e-3)
				return _reported;
			return (int) extra;
		} catch (Exception cannotBeWeighed) {
			return _reported;
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

	/**
	 * Whether an N-linked group at this position hangs off the residue's own amine - so that what the
	 * MAP adds is the O-linked equivalent on that nitrogen - rather than bringing a nitrogen of its
	 * own.
	 *
	 * <p>Asked of what the residue's name says it carries: GlcN has its amine at 2, Bac at 2 and 4, Neu
	 * at 5, Leg at 5 and 7. It was a list of classes and positions - a hexosamine's 2 and 4, a
	 * nonulosonate's 5 and 7 - which gave GlcN an amine at 4 and every nonulosonate one at 7, so a
	 * 4-acetamido on a GlcN or a 7-acetamido on a NeuAc was read as an O-acetyl: the nitrogen lost,
	 * 0.9840 heavy, written back as another molecule (#319).
	 */
	private boolean isNSubstituent(Residue _residue, String _map, int _pos) {
		if(!this.isNTypes(_map)) return false;

		NativeMonosaccharideDictionary.Entry entry =
				NativeMonosaccharideDictionary.forResidueName(_residue.getTypeName());
		if(entry != null) return hasAmineAt(entry, _pos);

		// A type the dictionary does not describe - 8eLeg and the other non-symbolic rows - keeps the
		// rule it had: they are not the residues #319 found wrong, and changing it for them unmeasured
		// gave 8eLeg5,7Ac2 two nitrogens at each amine.
		return legacyAmineAt(_residue, _pos);
	}

	/** The class-and-position rule this used for every residue before #319. */
	private static boolean legacyAmineAt(Residue _residue, int _pos) {
		String superclass = _residue.getType().getSuperclass();
		if(superclass.equals("Hexuronic acid") || superclass.equals("N-Acetylhexosamine")) return false;
		if(_pos == 2 || _pos == 4)
			return _residue.getTypeName().equals("Bac") || superclass.equals("Hexosamine")
					|| _residue.getTypeName().equals("Mur");
		if(_pos == 5)
			return _residue.getTypeName().contains("Leg") || _residue.getTypeName().equals("Neu")
					|| superclass.equals("Nonulosonate");
		if(_pos == 7)
			return _residue.getTypeName().contains("Leg") || superclass.equals("Nonulosonate");
		return false;
	}

	/** Whether the name's own substituents include a bare amine at this position, "2*N". */
	static boolean hasAmineAt(NativeMonosaccharideDictionary.Entry _entry, int _pos) {
		for(String unit : _entry.getSubstituents().split("_"))
			if(unit.equals(_pos + "*N")) return true;
		return false;
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
