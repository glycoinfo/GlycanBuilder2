package org.glycoinfo.application.glycanbuilder.util.exchange.exporter;

import org.eurocarbdb.MolecularFramework.sugar.BaseType;
import org.eurocarbdb.MolecularFramework.sugar.GlycoconjugateException;
import org.eurocarbdb.MolecularFramework.sugar.ModificationType;
import org.eurocarbdb.MolecularFramework.sugar.Superclass;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.GlycanFormatconverter.util.exchange.SugarToWURCSGraph.BaseTypeForRelativeConfiguration;
import org.glycoinfo.WURCSFramework.util.exchange.*;
import org.glycoinfo.GlycanFormatconverter.util.TrivialName.TrivialNameDictionary;
import org.glycoinfo.GlycanFormatconverter.util.TrivialName.ModifiedMonosaccharideDescriptor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResidueAnalyzer {

	private String skeletonCode = "";

	private boolean isAldose = true;

	private int anomPos = 0;
	private char anomState = 'x';
	private int backbone = 0;
	private char isomer = '?';

	private LinkedList<Integer> anomList = new LinkedList();
	private TreeMap<Integer, Character> pos2char = new TreeMap();
	private LinkedList<String> unknownMAPs = new LinkedList();

	private TrivialNameDictionary trivDict;

	public int getAnomericPosition() {
		return this.anomPos;
	}

	public char getAnomericSymbol() {
		return this.anomState;
	}

	public char getConfiguration() {
		return this.isomer;
	}

	public int getNumberOfCarbons() {
		return this.backbone;
	}

	public String getSkeletonCode() {
		return this.skeletonCode;
	}

	public LinkedList<String> getUnknownMAPs() {
		return this.unknownMAPs;
	}

	public boolean isAldose() {
		return this.isAldose;
	}

	public void ResidueToSkeletonCode(Residue _residue) throws Exception {
		this.clear();

		this.anomPos = _residue.isAldehyde() ? checkAnomerPosition('?') : checkAnomerPosition(_residue.getAnomericCarbon());
		this.anomState = _residue.isAldehyde() ? checkAnomerSymbol('?') : checkAnomerSymbol(_residue.getAnomericState());
		this.isomer = _residue.getChirality();

		Superclass superclass = Superclass.forName(_residue.getType().getCompositionClass().toLowerCase());

		this.backbone = superclass.getCAtomCount();

		this.pos2char.put(1, 'h');
		this.pos2char.put(backbone, 'h');

		// extract native modification
		//TODO : 修飾の上書きをする場合, enxの解析と組み込み
		for(String modification : this.extractNativeModificaiton(_residue)) {
			if(modification.equals("")) continue;
			this.convertSingleModificationToCarbonDescriptor(modification);
		}

		if(!this.anomList.isEmpty() && this.anomList.get(0) != 1)
			this.isAldose = false;

		if(this.isAldose) {
			this.pos2char.put(1, 'o');
			this.anomList.addFirst(1);
		}

		if(this.anomList.isEmpty()) {
			this.anomPos = 0;
			this.anomState = 'o';
		}

		if( this.anomPos != 0) {
			int anomPosLocal = this.anomPos;
			char cd = this.pos2char.get(anomPosLocal);
			if(cd == 'o' || cd == 'O')
				this.pos2char.put(anomPosLocal, 'a');
		}

		StringBuilder stereo = new StringBuilder(this.convertBasetypesToStereoCode(defineBaseTypeFromResidue(_residue)));
		int j = 0;
		for(int i = 2; i < this.backbone; i++) {
			if(this.pos2char.containsKey(i)) continue;
			char cd = (stereo.length() == 0) ? 'x' : stereo.charAt(j);
			this.pos2char.put(i, cd);
			j++;
		}

		for(int i = 0; i < this.backbone; i++) {
			this.skeletonCode += this.pos2char.get(i + 1);
		}

		if(stereo.length() != 0 && pos2char.size() != backbone)
			throw new WURCSExchangeException("error");
	}

	private ArrayList<String> extractNativeModificaiton(Residue _residue) {
		ArrayList<String> modifications = new ArrayList();

		// for alditol
		if(_residue.isAlditol())
			modifications.add("1*aldi");

		for(Linkage a_oLIN : _residue.getChildrenLinkages()) {
			if(!a_oLIN.getChildResidue().isModificaiton()) continue;
			modifications.add(a_oLIN.getParentPositionsString() + "*" + a_oLIN.getChildResidue().getTypeName());
		}

		for(String modification : _residue.getModifications()) {
			modifications.add(modification);
		}

		// extract native modification from residue
		// group1: core name
		// group2: ring size
		// group3: substituent with position
		// group4: substituent (notation)
		// Galp2NAc -> Gal, p, 2NAc, NAc
		String typeName = _residue.getTypeName();
		Matcher matCore = Pattern.compile("[A-Z][a-z]{2}(N|NAc|[GA]c|A)$").matcher(typeName);
		if (matCore.find()) {
			typeName = typeName.replace(matCore.group(1), "");
		}
		TrivialNameDictionary trivDict = TrivialNameDictionary.forThreeLetterCode(typeName);
		if (trivDict != null) {
			this.trivDict = trivDict;
			for (String unit : trivDict.getModifications().split("_")) {
				modifications.add(unit);
			}
		}

		// for acidic sugar
		if(_residue.getType().getSuperclass().equals("Hexuronate"))
			modifications.add("6*a");

		// for deoxy monosaccharide
		if(_residue.getType().getSuperclass().equals("DeoxyhexNAc") || _residue.getType().getSuperclass().equals("Deoxyhexose")) {
			modifications.add("6*d");
		}
		if (_residue.getType().getSuperclass().equals("Di-deoxynonulosonate") || _residue.getType().getSuperclass().equals("Nonulosonate")) {
			modifications.add("1*A");
			modifications.add("2*O");
			modifications.add("3*d");
			if(_residue.getType().getName().equals("ddNon")) modifications.add("9*d");
		}
		if(_residue.getType().getName().equals("ddHex")) {
			modifications.add("2*d");
			modifications.add("6*d");
		}

		return modifications;
	}

	private ArrayList<BaseType> defineBaseTypeFromResidue(Residue _residue) throws Exception {
		ArrayList<BaseType> baseTypes = new ArrayList();
		String sugarName = this.checkMonosaccharideName(_residue);
		Superclass superclass = Superclass.forName(sugarName);
		char isomer = this.isomer == '?' ? 'x' : this.isomer;

		if(superclass != null) return baseTypes;

		if (sugarName.contains("_")) {
			for (String unit : sugarName.split("_")) {
				baseTypes.add(BaseType.forName(unit.toLowerCase()));
			}
		} else {
			if (sugarName.length() == 3) {
				if(isomer == '?') sugarName = 'x' + sugarName;
				else sugarName = isomer + sugarName;
			}

			baseTypes.add(BaseType.forName(sugarName.toLowerCase()));
		}

		return baseTypes;
	}

	private String checkMonosaccharideName(Residue _residue) throws Exception {
		String name = _residue.getTypeName().toLowerCase();

		if (this.trivDict != null) {
			return this.trivDict.getStereos();
		}

		ModifiedMonosaccharideDescriptor modDesc = ModifiedMonosaccharideDescriptor.forTrivialName(name);
		if (modDesc != null) {
			return modDesc.getStereos();
		}

		// for hexnac
		if (name.toLowerCase().equals("hexnac") || name.toLowerCase().equals("dhexnac")) {
			modDesc = ModifiedMonosaccharideDescriptor.HEXNAC;
			return modDesc.getStereos();
		}

		// for di-deoxy
		if (name.startsWith("dd")) {
			return name.substring(2, name.length());
		}

		// for deoxy
		if (name.startsWith("d")) {
			return name.substring(1, name.length());
		}

		String size = _residue.getType().getCompositionClass();
		Superclass superclass = Superclass.forName(size);
		if(!name.equals(superclass.getName())) {
			if(name.contains(superclass.getName() + "a"))
				name = name.replace(size.toLowerCase() + "a", "");
			if(!name.startsWith("d") && name.contains(superclass.getName()))
				name = name.replace(size.toLowerCase(), "");
		}

		return name;
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
		this.isomer = dl.charAt(0);
		return a_sStereoCode;
	}

	private void convertSingleModificationToCarbonDescriptor(String a_sModification) throws GlycoconjugateException {
		if(a_sModification.contains(",")) return;

		String[] mod = a_sModification.split("\\*");
		int pos = Integer.parseInt(mod[0]);
		boolean isTerminal = (pos == 1 || pos == this.backbone);
		char carbonDescriptor = this.convertModificationNameToCarbonDescriptor(mod[1]);

		if(carbonDescriptor == 'd' && isTerminal)
			carbonDescriptor = 'm';

		if(carbonDescriptor == 'O') {
			if(this.anomPos != 1) this.anomList.add(pos);
			if(isTerminal) carbonDescriptor = 'o';
		}

		if(pos == 1)
			this.isAldose = false;

		if(pos == 0 || pos == -1) {
			this.unknownMAPs.add("*");
		}

		this.pos2char.put(pos, carbonDescriptor);
	}

	private char convertModificationNameToCarbonDescriptor(String _mod) throws GlycoconjugateException {
		try {
		    ModificationType modType = null;
		    if (_mod.equals("O")) modType = ModificationType.KETO;
		    else if (_mod.equals("h")) modType = ModificationType.ALDI;
		    else modType = ModificationType.forName(_mod);

			//ModificationType a_enumModType = ModificationType.forName(_mod.equals("O") ? "keto" : _mod);
			if(modType == ModificationType.DEOXY) return 'd';
			if(modType == ModificationType.ALDI) return 'h';
			if(modType == ModificationType.KETO) return 'O';
			if(modType == ModificationType.ACID) return 'A';
		}catch (GlycoconjugateException e){
			if(_mod.equals("m")) return 'd';
		}

		return ' ';
	}

	private int checkAnomerPosition (char a_cAnomPos) {
		if(a_cAnomPos == '?') return 0;
		return Integer.parseInt(String.valueOf(a_cAnomPos));
	}

	private char checkAnomerSymbol (char a_cAnomSymbol) {
		if(this.anomPos != 0 && a_cAnomSymbol == '?')
			return 'x';
		return a_cAnomSymbol;
	}

	private void clear() {
		this.isAldose = true;

		this.anomList = new LinkedList<Integer>();
		this.pos2char = new TreeMap<Integer, Character>();
	}
}
