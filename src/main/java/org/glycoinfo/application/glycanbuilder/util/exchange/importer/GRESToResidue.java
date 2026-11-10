package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.glycoinfo.GlycanFormatconverter.Glycan.Monosaccharide;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.BRIDGE;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.MSCORE;
import org.glycoinfo.application.glycanbuilder.dataset.MonosaccharideMSDictionary;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;

import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;
import org.glycoinfo.application.glycanbuilder.util.SkeletonModificationComposition;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;

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

		refuseUnknownDoubleBond(mscore.getSkeletonCode());

		// This builder's own dictionary answers first: it knows every residue the exporter can
		// write, which is more than the naming converter covers - a substituent it does not know,
		// a skeleton it cannot read, or a bridge across the anomeric carbon each made it throw.
		MonosaccharideMSDictionary.Match match =
				MonosaccharideMSDictionary.match(_gres.getMS().getString());

		String trivialName;
		char configuration;
		TrivialNameConverter trinConv = null;

		UnknownStereoNames.Naming unknownStereo = (match == null)
				? UnknownStereoNames.name(mscore.getSkeletonCode()) : null;

		if(match != null) {
			trivialName = match.getResidueType().getName();
			// from the description rather than from the type: the same residue writes a different
			// skeleton in each configuration, and only the description says which one this is
			configuration = match.getConfiguration();
		} else if(unknownStereo != null) {
			// stereocentres of unknown configuration, which the converter refuses (#265)
			trivialName = unknownStereo.typeName;
			configuration = unknownStereo.configuration;
		} else {
			trinConv = new TrivialNameConverter();
			trinConv.start(_gres);
			trivialName = trinConv.getTrivialName().replace("5", "");
			configuration = getConfiguration(((Monosaccharide) trinConv.getNode()).getStereos());
			// A skeleton whose stereo is relative only - 3 and 4, which say how the centres stand to
			// one another and not which enantiomer this is - has no D or L. The converter names one
			// anyway, and the residue was then written back with absolute digits: ad3dh came back as
			// ad1dh, a different molecule, with nothing to say so. Found once araHex and its kind could
			// be written at all (#261); until then the write failed instead.
			if(isRelativeOnly(_gres.getMS().getCoreStructure().getSkeletonCode()))
				configuration = '?';
		}

		// A skeleton with no absolute stereo - no 1 or 2, only 3 and 4, x, or no centres at all - has
		// no D or L, whichever way it was named. The dictionary's match gave one for a heptose of
		// unknown stereo, a "D-Hept", only because that is the configuration it was indexed under.
		if(!hasAbsoluteStereo(mscore.getSkeletonCode())) configuration = '?';

		ResidueType newType = ResidueDictionary.findResidueType(trivialName);
		if(trinConv != null)
			refuseANameOfAnotherSkeleton(newType, mscore.getSkeletonCode(), this.anomPosition, trivialName);
		Residue residue = new Residue(newType);

		// the skeleton is matched whole, so a recognised residue leaves no core modification behind
		ArrayList<String> leftOver = (trinConv != null) ? trinConv.getModifications()
				: (unknownStereo != null) ? new ArrayList<String>(unknownStereo.deoxies) : new ArrayList<String>();

		// The legend names this residue in the box drawn beside the structure, for a residue with no
		// symbol of its own. It belongs to the residue: writing it onto the type, which every residue
		// of that name shares, meant reading a second structure changed what the first one said.
		//
		// A residue carrying a deoxy its name does not imply has no symbol of its own either - a
		// 6-deoxy allose is not an allose - so it is named the same way (#258).
		boolean carriesDeoxy = false;
		for (String modification : leftOver)
			if (SkeletonModificationComposition.isDeoxy(modification)) carriesDeoxy = true;
		if(NonSymbolicResidueDictionary.hasResidueType(trivialName) || carriesDeoxy) {
			String legend = this.makeLegend(_gres);
			if(legend != null) residue.setLegend(legend);
		}

		if(!_gres.getMS().getString().contains("<Q>")  && residue.getTypeName().equals("Sugar"))
			throw new WURCSToGlycanException(_gres.getMS().getString() + " is not handled in GlycanBuilder");
		
		residue.setWasSticky(isSticky(trivialName));

		residue.setAnomericCarbon(this.checkAnomerPosition());
		residue.setAnomericState(this.checkAnomerSymbol());
		residue.setChirality(configuration);
		// what the type and configuration cannot say: which way round a relative skeleton was written
		// (#286), and the known centres among unknown ones (#265)
		String skeleton = _gres.getMS().getCoreStructure().getSkeletonCode();
		if(isRelativeOnly(skeleton) || (unknownStereo != null && unknownStereo.keepSkeleton))
			residue.setReadSkeleton(skeleton);
		// The ring letter carries the acyclic forms now, and setting it sets the flags that go with
		// them - so the two are set together here rather than either being able to undo the other.
		residue.setRingSize(this.isAlditol() ? 'o' : this.isAldehyde() ? 'a' : this.ringSize);

		this.modifications = leftOver;
		this.residue = residue;
	}

	/**
	 * Refuse a name the converter gave to a skeleton it does not describe (#310).
	 *
	 * <p>The naming converter names a hept-2-ulose by the aldohexose whose four stereocentres match its
	 * C3 to C6: ha1122h, D-manno-hept-2-ulose, came back as Man, with nothing left over to say that it
	 * has a seventh carbon and a ketone. The residue was a hexose, weighed CH2O light and could not be
	 * written back, and nothing said so. A type that has another number of carbons, or its carbonyl at
	 * another carbon, is not this monosaccharide; and GlycanBuilder2 refuses what it cannot read rather
	 * than reading it as something else.
	 *
	 * <p>Refused when the type differs in both, its number of carbons and the carbon its carbonyl is at.
	 *
	 * @param _type The type the converter's name resolves to.
	 * @param _skeleton The skeleton read, "ha1122h".
	 * @param _carbonyl Where the skeleton's ring closes from - its anomeric carbon - or 0 or less when it
	 *        has none.
	 */
	static void refuseANameOfAnotherSkeleton(ResidueType _type, String _skeleton, int _carbonyl, String _name)
			throws WURCSToGlycanException {
		if (_type == null || _skeleton == null) return;

		Integer carbons = null;
		try {
			String compositionClass = _type.getCompositionClass();
			org.eurocarbdb.MolecularFramework.sugar.Superclass superclass = (compositionClass == null) ? null
					: org.eurocarbdb.MolecularFramework.sugar.Superclass.forName(compositionClass.toLowerCase());
			if (superclass != null) carbons = superclass.getCAtomCount();
		} catch (Exception noClass) {
			// a class the framework does not know says nothing either way
		}
		char anomeric = _type.getAnomericCarbon();

		boolean otherLength = carbons != null && carbons.intValue() > 0 && carbons.intValue() != _skeleton.length();
		boolean otherCarbonyl = _carbonyl > 0 && Character.isDigit(anomeric) && (anomeric - '0') != _carbonyl;
		// Both, and not either. A type of another carbonyl alone - araHep for a deoxy heptulose, eryHex
		// for a deoxy hexulose - is an isomer: it weighs right, and araHep is written back as read.
		// Another length alone is a row's composition class counting a substituent's carbon, as Cym's
		// does. A hexose for a heptulose is neither: it is another monosaccharide.
		if (!(otherLength && otherCarbonyl)) return;

		throw new WURCSToGlycanException(_skeleton + " has " + _skeleton.length() + " carbons and its anomeric"
				+ " carbon at C" + _carbonyl + ", and the name it was given, " + _name + ", is a residue of "
				+ (carbons != null ? carbons + " carbons" : "another skeleton") + " with its anomeric carbon at C"
				+ anomeric + ": no residue this application knows is this monosaccharide, so it is not read");
	}

	/**
	 * The IUPAC name shown as a legend under residues drawn without a symbol. It comes from a
	 * converter that does not cover every residue this builder can draw, so one it cannot name goes
	 * without a legend rather than taking the structure down.
	 *
	 * <p>Named from the monosaccharide alone - skeleton, anomer and ring - and not from the whole
	 * residue as written. A substituent is drawn beside the symbol, "3Me", and naming it in the footnote
	 * as well said it twice: "2d 3Me" on the symbol and "2-deoxy-D-araHexp3Me" below (#263). What
	 * belongs in the footnote is what the symbol cannot show, which is the monosaccharide.
	 *
	 * @param _gres Residue being read.
	 * @return Returns the legend, or null when none could be built.
	 */
	private String makeLegend(GRES _gres) {
		try {
			GRES core = new WURCSFactory("WURCS=2.0/1,1,0/[" + coreOf(_gres) + "]/1/")
					.getSequence().getGRESs().getFirst();
			TrivialNameConverter converter = new TrivialNameConverter();
			converter.start(core);
			return converter.getIUPACNotation();
		} catch (Exception cannotBeNamed) {
			return null;
		}
	}

	/**
	 * The monosaccharide part of a residue as WURCS writes it: the skeleton and anomer, and the ring
	 * bridge, without the substituents and the other bridges, which this builder draws as residues of
	 * their own.
	 */
	static String coreOf(GRES _gres) {
		String[] parts = _gres.getMS().getString().split("_");
		BRIDGE ring = RingBridge.find(_gres.getMS().getCoreStructure());
		String ringText = (ring == null) ? null
				: position(ring.getStartPositions().getFirst()) + "-" + position(ring.getEndPositions().getFirst());

		StringBuilder core = new StringBuilder(parts[0]);
		for (int i = 1; i < parts.length; i++)
			if (!parts[i].contains("*") && parts[i].equals(ringText)) core.append('_').append(parts[i]);
		return core.toString();
	}

	private static String position(Integer _position) {
		return (_position == null || _position < 0) ? "?" : String.valueOf(_position);
	}
	
	private boolean isSticky(String _trivialName) {
		return _trivialName.contains("Fuc") || _trivialName.contains("Xyl");
	}
	
	private char makeRingSize(MSCORE _mscore) {
		int anomPos = _mscore.getAnomericPosition();

		// 3 : anomeric position -> WURCS=2.0/1,1,0/[h2a1221h-3x_3-8]/1/
		if(anomPos == 0 || anomPos == -1 || anomPos == 3) return '?';

		// Picking the last bridge that started at the anomeric carbon read a 1,6-anhydro as the
		// ring, and the residue lost its ring size along with the bridge.
		BRIDGE ring = RingBridge.find(_mscore);
		if(ring == null) return '?';

		//1-4, 2-5 is franose
		//1-5, 2-6 is pyranose
		int span = RingBridge.span(ring, anomPos);
		if(span == 4) return 'f';
		if(span == 5) return 'p';
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

	/**
	 * Refuse a skeleton with a carbon-carbon double bond of unknown geometry, WURCS's {@code F} and
	 * {@code f}, saying so (#266).
	 *
	 * <p>A residue here has no way to carry a double bond, and the naming converter refused these with
	 * "hxFFxh could not handled", which named nothing. Until a double bond can be held - the same
	 * question as #292's 4,5-unsaturated uronic acids - the refusal at least says what is not
	 * supported, and where.
	 */
	static void refuseUnknownDoubleBond(String _skeletonCode) throws WURCSToGlycanException {
		java.util.List<String> carbons = new java.util.ArrayList<String>();
		for (int i = 0; i < _skeletonCode.length(); i++) {
			char c = _skeletonCode.charAt(i);
			if (c == 'F' || c == 'f') carbons.add("C" + (i + 1));
		}
		if (carbons.isEmpty()) return;
		throw new WURCSToGlycanException(_skeletonCode + " has a carbon-carbon double bond of unknown geometry at "
				+ String.join(" and ", carbons) + " (F and f in WURCS), which GlycanBuilder2 cannot represent");
	}

	/** Whether a skeleton has a stereocentre of absolute configuration, a 1 or a 2. */
	static boolean hasAbsoluteStereo(String _skeletonCode) {
		return _skeletonCode.indexOf('1') >= 0 || _skeletonCode.indexOf('2') >= 0;
	}

	/** Whether a skeleton's stereocentres are all written relative, 3 or 4, and at least one is there. */
	static boolean isRelativeOnly(String _skeletonCode) {
		boolean relative = false;
		for (char carbon : _skeletonCode.toCharArray()) {
			if (carbon == '1' || carbon == '2') return false;
			if (carbon == '3' || carbon == '4') relative = true;
		}
		return relative;
	}

	private char getConfiguration (LinkedList<String> _stereos) {
		if (_stereos.isEmpty()) return '?';
		String ret = _stereos.getFirst();
		if (ret.length() == 3) return '?';
		return ret.substring(0, 1).toUpperCase().charAt(0);
	}
}
