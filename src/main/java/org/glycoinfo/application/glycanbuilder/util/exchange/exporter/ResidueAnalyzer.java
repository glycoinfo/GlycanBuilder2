package org.glycoinfo.application.glycanbuilder.util.exchange.exporter;

import org.glycoinfo.application.glycanbuilder.util.SkeletonModificationComposition;

import org.eurocarbdb.MolecularFramework.sugar.BaseType;
import org.eurocarbdb.MolecularFramework.sugar.GlycoconjugateException;
import org.eurocarbdb.MolecularFramework.sugar.ModificationType;
import org.eurocarbdb.MolecularFramework.sugar.Superclass;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.glycoinfo.application.glycanbuilder.dataset.NativeMonosaccharideDictionary;
import org.glycoinfo.WURCSFramework.util.exchange.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;

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
		for(String modification : this.extractNativeModification(_residue)) {
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

		StringBuilder stereo = new StringBuilder(this.stereoOfResidue(_residue));
		int j = 0;
		for(int i = 2; i < this.backbone; i++) {
			if(this.pos2char.containsKey(i)) continue;
			// A configurational-prefix name covers the centres nearest C1 and says nothing of the rest:
			// manHep gives C2 to C5, and C6 is unknown (#265).
			char cd = (stereo.length() == 0) ? 'x'
					: (j >= stereo.length() && NativeMonosaccharideDictionary.isPrefixName(_residue.getTypeName())) ? 'x'
					: stereo.charAt(j);
			this.pos2char.put(i, cd);
			j++;
		}

		for(int i = 0; i < this.backbone; i++) {
			this.skeletonCode += this.pos2char.get(i + 1);
		}

		// A residue read from a skeleton its type cannot say alone is written back as read: relative
		// stereo the way round it was read (#286), and the centres an unknown-stereo type leaves as x
		// (#265). Only while what was built still agrees with it: a residue changed since it was read
		// builds something else, and is written as built.
		String read = _residue.getReadSkeleton();
		if(read != null && !read.equals(this.skeletonCode)
				&& (read.equals(turnedOver(this.skeletonCode)) || refines(read, this.skeletonCode)))
			this.skeletonCode = read;

		if(stereo.length() != 0 && pos2char.size() != backbone)
			throw new WURCSExchangeException("error");
	}

	/** Whether a skeleton says the same as another and more: a digit where the other has x, and the rest alike. */
	static boolean refines(String _read, String _built) {
		if (_read.length() != _built.length()) return false;
		boolean more = false;
		for (int i = 0; i < _read.length(); i++) {
			char r = _read.charAt(i), b = _built.charAt(i);
			if (r == b) continue;
			if ((b == 'x' || b == 'X') && "1234".indexOf(r) >= 0) { more = true; continue; }
			return false;
		}
		return more;
	}

	/** A skeleton code with every relative digit the other way round, 3 for 4 and 4 for 3. */
	static String turnedOver(String _skeletonCode) {
		StringBuilder turned = new StringBuilder(_skeletonCode.length());
		for (char carbon : _skeletonCode.toCharArray())
			turned.append(carbon == '3' ? '4' : carbon == '4' ? '3' : carbon);
		return turned.toString();
	}

	private ArrayList<String> extractNativeModification(Residue _residue) {
		ArrayList<String> modifications = new ArrayList<>();

		// for alditol
		if(_residue.isAlditol())
			modifications.add("1*aldi");

		for(String modification : _residue.getModifications()) {
			modifications.add(modification);
		}

		// what the residue's name says about its carbons - the acid, the ketone and the deoxy that
		// make it a nonulosonate, the 6*m that makes it a deoxyhexose
		NativeMonosaccharideDictionary.Entry entry =
				NativeMonosaccharideDictionary.forResidueName(_residue.getTypeName());
		if (entry != null && !entry.getModifications().isEmpty()) {
			for (String unit : entry.getModifications().split("_")) {
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





	private void convertSingleModificationToCarbonDescriptor(String a_sModification) {
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

	private char convertModificationNameToCarbonDescriptor(String _mod) {
		// a carbon of a double bond is written as the skeleton had it (#292)
		if (_mod.length() == 1 && SkeletonModificationComposition.isDoubleBond(_mod.charAt(0))) return _mod.charAt(0);
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

	/**
	 * The stereo digits for this residue's backbone carbons, from our own dictionary.
	 *
	 * <p>This used to be three steps: turn the residue's name into a parent sugar's name, look that
	 * up in MolecularFramework's base types, and read a stereo code off whichever of the D, L or X
	 * forms the configuration selected. The dictionary records the digits directly instead, and
	 * derives the other configurations from them.
	 */
	private String stereoOfResidue(Residue _residue) throws Exception {
		NativeMonosaccharideDictionary.Entry entry =
				NativeMonosaccharideDictionary.forResidueName(_residue.getTypeName());
		if (entry == null)
			throw new WURCSExchangeException(_residue.getTypeName()
					+ " has no stereo recorded for it, so it has no skeleton to write.");

		this.isomer = entry.getIsomer();
		return entry.getStereo(_residue.getChirality());
	}
}
