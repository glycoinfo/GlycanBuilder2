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
		refuseARingLargerThanAPyranose(mscore);
		refuseAnotherAtomInTheRing(mscore);

		// This builder's own dictionary answers first: it knows every residue the exporter can
		// write, which is more than the naming converter covers - a substituent it does not know,
		// a skeleton it cannot read, or a bridge across the anomeric carbon each made it throw.
		MonosaccharideMSDictionary.Match match =
				MonosaccharideMSDictionary.match(_gres.getMS().getString());

		// A ketose with its C1 deoxy, 1-deoxy-D-fructose's ma122h, is named by the converter as an
		// aldose read from the other end - araHex with a deoxy at 1 - and was written back as hx112m,
		// another monosaccharide (#354). Its own name is the ketose's whose C1 is a CH2OH: looked up
		// with that carbon restored, and the deoxy kept, as a 6-deoxy is kept on Fru.
		ArrayList<String> terminalDeoxies = new ArrayList<String>();
		// An aldonic acid is named by the dictionary as the uronic acid whose reduction it is, read from
		// the other end - A2122h, D-gluconic acid, as an L-GulA alditol - and every position on it then
		// counts from that end: a 2,6-anhydro came back as a 1,5-anhydro (#341). The converter names it
		// from the WURCS's own C1, D-Glc with its C1 a carboxyl, and is asked instead.
		if(match != null && match.getResidueType().getAnomericCarbon() == '1'
				&& isAnAcidOfAnAlditol(mscore.getSkeletonCode(), this.anomPosition, match.getResidueType()))
			match = null;
		if(match == null) {
			MonosaccharideMSDictionary.Match ketose = ketoseWithItsC1Restored(_gres.getMS().getString());
			if(ketose != null) {
				match = ketose;
				String skeleton = mscore.getSkeletonCode();
				terminalDeoxies.add("1*m");
				// and its last carbon, a 1,6-dideoxy hexulose's, restored with it
				if(skeleton.charAt(skeleton.length() - 1) == 'm') terminalDeoxies.add(skeleton.length() + "*m");
			}
		}

		String trivialName;
		char configuration;
		TrivialNameConverter trinConv = null;

		UnknownStereoNames.Naming unknownStereo = (match == null)
				? UnknownStereoNames.name(mscore.getSkeletonCode()) : null;
		if(match == null && unknownStereo == null && UnknownStereoNames.hasUnknownStereo(mscore.getSkeletonCode()))
			throw new WURCSToGlycanException(UnknownStereoNames.whyNot(mscore.getSkeletonCode()));

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
			trivialName = nameOfTheChain(trivialName, mscore.getSkeletonCode());
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
		refuseAnAcidOfAnotherLength(newType, mscore.getSkeletonCode(), this.anomPosition, trivialName);
		configuration = configurationOfTheSkeleton(trivialName, mscore.getSkeletonCode(), configuration);
		if(trinConv != null)
			refuseANameOfAnotherSkeleton(newType, mscore.getSkeletonCode(), this.anomPosition, trivialName);
		Residue residue = new Residue(newType);

		// the skeleton is matched whole, so a recognised residue leaves no core modification behind
		ArrayList<String> leftOver = (trinConv != null) ? trinConv.getModifications()
				: (unknownStereo != null) ? new ArrayList<String>(unknownStereo.deoxies) : new ArrayList<String>();
		leftOver.addAll(terminalDeoxies);

		// The legend names this residue in the box drawn beside the structure, for a residue with no
		// symbol of its own. It belongs to the residue: writing it onto the type, which every residue
		// of that name shares, meant reading a second structure changed what the first one said.
		//
		// A residue carrying a deoxy its name does not imply has no symbol of its own either - a
		// 6-deoxy allose is not an allose - so it is named the same way (#258).
		boolean carriesDeoxy = false;
		for (String modification : leftOver)
			if (SkeletonModificationComposition.isDeoxy(modification) || modification.endsWith("*en")) carriesDeoxy = true;
		if(NonSymbolicResidueDictionary.hasResidueType(trivialName) || carriesDeoxy) {
			String legend = nameOf(_gres);
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
		// An open chain whose name's carbonyl is a carbonyl is that name's open form, not its alditol:
		// D-fructose's hO122h was read as an alditol, two hydrogens heavy (#342). One whose name's
		// carbonyl is reduced - a CH2OH, a deoxy or an acid - is the alditol, whatever else is oxidised:
		// h1O21h, a hex-3-ulose, is a xylo-hexitol with its C3 a ketone, and mO12h, which the converter
		// names thrPen, a threo-pentitol with its C1 a deoxy and its C2 a ketone.
		boolean openCarbonyl = carbonylAtTheNamesAnomericCarbon(mscore.getSkeletonCode(), this.anomPosition, newType);
		residue.setRingSize((!openCarbonyl && (this.isAlditol()
				|| reducedAtTheNamesAnomericCarbon(mscore.getSkeletonCode(), this.anomPosition, newType)
				|| isAnAcidOfAnAlditol(mscore.getSkeletonCode(), this.anomPosition, newType)))
				? 'o' : (openCarbonyl || this.isAldehyde()) ? 'a' : this.ringSize);

		this.modifications = leftOver;
		this.residue = residue;
	}

	/**
	 * The configuration whose stereo, as the writer will write it, is the skeleton's own (#313).
	 *
	 * <p>A residue is written in the configuration it carries since #313, so the configuration read
	 * must be the one that writes the skeleton back. Taken from the last stereocentre, it was not
	 * always: a 7-deoxy L-gro-D-manHep, a11221m, came back as L by its C6, and an acetylated apiose,
	 * a26h, as D - harmless while those names ignored their configuration, the other enantiomer once
	 * they followed it. So the skeleton's stereo digits are compared with the name's in D and in L; when
	 * one matches and the other does not, that is the configuration. Otherwise what was read stands.
	 */
	static char configurationOfTheSkeleton(String _name, String _skeleton, char _read) {
		org.glycoinfo.application.glycanbuilder.dataset.NativeMonosaccharideDictionary.Entry entry =
				org.glycoinfo.application.glycanbuilder.dataset.NativeMonosaccharideDictionary.forResidueName(_name);
		if (entry == null || _skeleton == null) return _read;
		StringBuilder digits = new StringBuilder();
		for (char c : _skeleton.toCharArray())
			if ("12345678".indexOf(c) >= 0) digits.append(c);
		String inD = entry.getStereo('D'), inL = entry.getStereo('L');
		if (inD.equals(inL) || inD.isEmpty()) return _read;
		boolean isD = digits.toString().equals(inD), isL = digits.toString().equals(inL);
		if (isD && !isL) return 'D';
		if (isL && !isD) return 'L';
		return _read;
	}

	/**
	 * Refuse an open-chain acid whose name has another number of carbons (#341). The converter names a
	 * 2-keto acid by the configurational prefix of its stereocentres, so a 2-keto tetronic acid, AO1h,
	 * came back as Gro, which has three, and was written back as AOh, a molecule of three carbons.
	 */
	static void refuseAnAcidOfAnotherLength(ResidueType _type, String _skeleton, int _anomericPosition, String _name)
			throws WURCSToGlycanException {
		if (!isAnAcidOfAnAlditol(_skeleton, _anomericPosition, _type)) return;
		Integer carbons = carbonsOf(_type);
		if (carbons == null || carbons.intValue() == _skeleton.length()) return;
		throw new WURCSToGlycanException(_skeleton + " has " + _skeleton.length() + " carbons, and the name it was"
				+ " given, " + _name + ", is a residue of " + carbons + ": no residue this application knows is this"
				+ " monosaccharide, so it is not read");
	}

	/** The number of carbons a type's composition class says it has, or null when it says none. */
	private static Integer carbonsOf(ResidueType _type) {
		try {
			String compositionClass = _type.getCompositionClass();
			org.eurocarbdb.MolecularFramework.sugar.Superclass superclass = (compositionClass == null) ? null
					: org.eurocarbdb.MolecularFramework.sugar.Superclass.forName(compositionClass.toLowerCase());
			return (superclass == null || superclass.getCAtomCount() <= 0) ? null : superclass.getCAtomCount();
		} catch (Exception noClass) {
			return null;
		}
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
		// A name whose entry says what its carbons are - Fru's 1*h_2*O, a ketose; Kdo's 1*A_2*O_3*d, an
		// ulosonic acid - is not this monosaccharide where the skeleton says otherwise. The converter
		// names by the stereo, and an octonic acid with D-glycero-D-galacto stereo, Ad21122h, came back
		// as D-gro-D-galOct, an octulose (#275): its carboxyl a child nothing can write, and 34 Da light.
		String contradicted = carbonContradicting(_name, _skeleton);
		if (contradicted != null)
			throw new WURCSToGlycanException(_skeleton + " has " + contradicted + ", and the name it was given, "
					+ _name + ", says otherwise: no residue this application knows is this monosaccharide, so it"
					+ " is not read");

		if (!(otherLength && otherCarbonyl)) return;

		throw new WURCSToGlycanException(_skeleton + " has " + _skeleton.length() + " carbons and its anomeric"
				+ " carbon at C" + _carbonyl + ", and the name it was given, " + _name + ", is a residue of "
				+ (carbons != null ? carbons + " carbons" : "another skeleton") + " with its anomeric carbon at C"
				+ anomeric + ": no residue this application knows is this monosaccharide, so it is not read");
	}

	/**
	 * The ketose this skeleton is with its C1 made a CH2OH again - and its last carbon, when that is a
	 * deoxy as well - when it is a ketose whose C2 is the anomeric carbon of its ring, or of a ring not
	 * known, and whose C1 is a deoxy. Null otherwise, and when the restored skeleton is no ketose this application knows.
	 */
	static MonosaccharideMSDictionary.Match ketoseWithItsC1Restored(String _ms) {
		// and the open chain, mO122h, now that a ketose is read in its open form (#342)
		if(_ms == null || _ms.length() < 4 || _ms.charAt(0) != 'm' || "aUO".indexOf(_ms.charAt(1)) < 0) return null;
		// the skeleton code runs to the first "-" or "_"; its last carbon, a deoxy as well, is restored too
		int end = _ms.length();
		for(int i = 0; i < _ms.length(); i++)
			if(_ms.charAt(i) == '-' || _ms.charAt(i) == '_') { end = i; break; }
		String skeleton = "h" + _ms.substring(1, end);
		if(skeleton.endsWith("m")) skeleton = skeleton.substring(0, skeleton.length() - 1) + "h";
		MonosaccharideMSDictionary.Match restored = MonosaccharideMSDictionary.match(skeleton + _ms.substring(end));
		if(restored == null || restored.getResidueType().getAnomericCarbon() != '2') return null;
		return restored;
	}

	/**
	 * The first carbon the name's entry describes and the skeleton contradicts, as "C1 A", or null. A
	 * deoxy the name does not imply is no contradiction - a 1-deoxy Fru is still Fru, with a deoxy - nor
	 * is a ketone's carbon written as the anomeric carbon of a ring or as unknown.
	 */
	static String carbonContradicting(String _name, String _skeleton) {
		org.glycoinfo.application.glycanbuilder.dataset.NativeMonosaccharideDictionary.Entry entry =
				org.glycoinfo.application.glycanbuilder.dataset.NativeMonosaccharideDictionary.forResidueName(_name);
		if (entry == null || entry.getModifications().isEmpty()) return null;
		for (String unit : entry.getModifications().split("_")) {
			int star = unit.indexOf('*');
			if (star < 1 || star != unit.length() - 2) continue;
			int carbon;
			try {
				carbon = Integer.parseInt(unit.substring(0, star));
			} catch (NumberFormatException notACarbon) {
				continue;
			}
			if (carbon < 1 || carbon > _skeleton.length()) continue;
			char found = _skeleton.charAt(carbon - 1);
			String allowed;
			switch (unit.charAt(star + 1)) {
			case 'h': allowed = "hm"; break;
			case 'O': allowed = "aOU"; break;
			case 'A': allowed = "A"; break;
			case 'd': allowed = "d"; break;
			case 'm': allowed = "m"; break;
			default: continue;
			}
			if (allowed.indexOf(found) < 0) return "C" + carbon + " " + found;
		}
		return null;
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
	public static String nameOf(GRES _gres) {
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
	public static String coreOf(GRES _gres) {
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
	
	/**
	 * Whether this skeleton is an open chain with a carboxyl at an end and no aldehyde, named as an
	 * aldose: an aldonic acid, A2122h, an aldaric acid, A22A, a 2-keto acid, AOd2A, and their lactones
	 * (#341). Each is an alditol of the name with an end oxidised - D-gluconic acid is D-glucitol with
	 * its C1 a carboxyl - and is read as one, the oxidised carbons kept on the residue.
	 *
	 * <p>The dictionary names a hexonic acid by the uronic acid whose reduction it is, A2122h as an
	 * L-GulA alditol, and the writer writes that alditol as A2122h. It was read as the uronic acid,
	 * with its aldehyde, two hydrogens light. The converter names the others by the aldose, Thr for
	 * A21h, and the carboxyl was built as a child named "A" that weighed nothing, or was left out.
	 *
	 * <p>A name whose anomeric carbon is not C1 - Kdo's open chain, a ketose's - is not an alditol here.
	 */
	static boolean isAnAcidOfAnAlditol(String _skeleton, int _anomericPosition, ResidueType _type) {
		if (_skeleton == null || _skeleton.length() < 2 || _anomericPosition != 0 || _type == null) return false;
		if (_skeleton.charAt(0) != 'A' && _skeleton.charAt(_skeleton.length() - 1) != 'A') return false;
		// an aldehyde at the far end, AOd22o, is an oxidised carbon of the chain as well (#342)
		for (int i = 0; i < _skeleton.length(); i++)
			if ("auU".indexOf(_skeleton.charAt(i)) >= 0 || (_skeleton.charAt(i) == 'o' && i != _skeleton.length() - 1))
				return false;
		return _type.getAnomericCarbon() == '1';
	}

	/**
	 * Whether this open chain has a carbonyl at the carbon its name's carbonyl is at - an aldose's C1,
	 * a ketose's C2 - so that it is that monosaccharide's open form rather than its alditol (#342).
	 */
	static boolean carbonylAtTheNamesAnomericCarbon(String _skeleton, int _anomericPosition, ResidueType _type) {
		if (_skeleton == null || _anomericPosition != 0 || _type == null) return false;
		for (char carbon : _skeleton.toCharArray())
			if ("auU".indexOf(carbon) >= 0) return false;
		char anomeric = _type.getAnomericCarbon();
		if (!Character.isDigit(anomeric)) return false;
		int at = anomeric - '0';
		return at >= 1 && at <= _skeleton.length() && "oO".indexOf(_skeleton.charAt(at - 1)) >= 0;
	}

	/**
	 * Whether this open chain has its name's carbonyl reduced - an aldose's C1 a CH2OH, a deoxy or an
	 * acid - and no carbonyl but an oxidised carbon its name does not have, so that it is that name's
	 * alditol with the rest of the chain as it is (#342).
	 */
	static boolean reducedAtTheNamesAnomericCarbon(String _skeleton, int _anomericPosition, ResidueType _type) {
		if (_skeleton == null || _skeleton.length() < 2 || _anomericPosition != 0 || _type == null) return false;
		for (int i = 0; i < _skeleton.length(); i++)
			if ("auU".indexOf(_skeleton.charAt(i)) >= 0 || (_skeleton.charAt(i) == 'o' && i != _skeleton.length() - 1))
				return false;
		char anomeric = _type.getAnomericCarbon();
		if (anomeric != '1') return false;
		return "hmA".indexOf(_skeleton.charAt(0)) >= 0;
	}

	private static final java.util.regex.Pattern PARENT_ALDOSE =
			java.util.regex.Pattern.compile("^(Gro|Ery|Thr|Rib|Ara|Xyl|Lyx|All|Alt|Glc|Man|Gul|Ido|Gal|Tal)$");
	private static final String[] CHAIN = { null, null, null, null, "Tet", "Pen", "Hex", "Hep", "Oct", "Non", "Dec" };

	/**
	 * The name of this chain's length for a converter's name that is the configurational prefix of its
	 * stereocentres, when the skeleton has a free carbonyl. The converter names D-arabino-hexos-2-ulose,
	 * aO122h, by its three centres as Ara, a pentose; it is araHex with its C2 a ketone (#342). The name
	 * is kept when it is already of this length, or when no residue has the other name.
	 */
	static String nameOfTheChain(String _name, String _skeleton) {
		if (_name == null || _skeleton == null || !PARENT_ALDOSE.matcher(_name).matches()) return _name;
		boolean freeCarbonyl = false;
		for (int i = 1; i < _skeleton.length(); i++)
			if (_skeleton.charAt(i) == 'O' || (_skeleton.charAt(i) == 'o' && i == _skeleton.length() - 1)) freeCarbonyl = true;
		if (!freeCarbonyl) return _name;
		ResidueType named = ResidueDictionary.findResidueType(_name);
		Integer carbons = (named == null) ? null : carbonsOf(named);
		if (carbons == null || carbons.intValue() == _skeleton.length()) return _name;
		if (_skeleton.length() >= CHAIN.length || CHAIN[_skeleton.length()] == null) return _name;
		String ofTheChain = _name.toLowerCase() + CHAIN[_skeleton.length()];
		return (ResidueDictionary.hasResidueType(ofTheChain) || NonSymbolicResidueDictionary.hasResidueType(ofTheChain))
				? ofTheChain : _name;
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
		// an open chain only: indexOf counts from 0, and a ring's anomeric carbon from 1, so a ring
		// with a ketone at C2, aO122h-1b_1-5, was taken for an open chain and written back without its
		// ring (#342)
		if(this.anomPosition != 0)
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

	/**
	 * Refuse a residue whose ring is larger than a pyranose, saying so (#344).
	 *
	 * <p>A septanose, a2221h-1a_1-6, closes its ring from C1 to C6. A residue here has a furanose, a
	 * pyranose, or a ring of size not known, and no other; the 1-6 was taken for a 1,6-anhydro beside a
	 * ring not known, so the residue weighed a water light and was written back as a2221h-1a_1-6_1-?.
	 * The maintainer's decision (2026-11-28): such a residue is refused, saying why, rather than read as
	 * something else; a seven-membered ring is GB3's.
	 */
	static void refuseARingLargerThanAPyranose(MSCORE _core) throws WURCSToGlycanException {
		int anomeric = _core.getAnomericPosition();
		if (anomeric < 1 || RingBridge.find(_core) != null) return;
		for (BRIDGE bridge : _core.getDivalentSubstituents()) {
			if (!bridge.getMAP().equals("") || !bridge.getStartPositions().contains(anomeric)) continue;
			if (bridge.getEndPositions().isEmpty() || bridge.getEndPositions().get(0) == null) continue;
			int span = RingBridge.span(bridge, anomeric);
			if (span < 6) continue;
			int end = bridge.getEndPositions().get(0);
			throw new WURCSToGlycanException(_core.getSkeletonCode() + " closes its ring from C" + anomeric + " to C"
					+ end + ", a ring of " + (span + 1) + " members, which GlycanBuilder2 cannot represent: it has"
					+ " furanoses and pyranoses only, so it is not read");
		}
	}

	/**
	 * Refuse a residue whose ring closes through another atom than its oxygen, saying so (#345).
	 *
	 * <p>5-Thio-D-glucopyranose, a2122h-1a_1-5*S*, closes its ring from C1 to C5 through a sulfur. A
	 * residue here has its ring closed through its oxygen and no other, so the sulfur was read as a
	 * thio bridge beside a ring not known, and weighed 14 Da heavy. As for a septanose (#344), the
	 * maintainer's rule (2026-11-28) is to refuse what cannot be represented rather than read it as
	 * something else. A bridge with an atom that does not start at the anomeric carbon - the
	 * 1,5-imino of a dideoxy-imino-alditol, the 1,5-lactam of a neuraminic acid - is a bridge, and is
	 * read as one.
	 */
	static void refuseAnotherAtomInTheRing(MSCORE _core) throws WURCSToGlycanException {
		int anomeric = _core.getAnomericPosition();
		if (anomeric < 1 || RingBridge.find(_core) != null) return;
		for (BRIDGE bridge : _core.getDivalentSubstituents()) {
			if (!bridge.getStartPositions().contains(anomeric)) continue;
			// one atom where the ring oxygen would be, *S* or *N*, with or without what hangs off it
			if (!bridge.getMAP().matches("\\*[A-Z][a-z]?\\*(/.*)?")) continue;
			if (bridge.getEndPositions().isEmpty() || bridge.getEndPositions().get(0) == null) continue;
			int span = RingBridge.span(bridge, anomeric);
			if (span != 4 && span != 5) continue;
			throw new WURCSToGlycanException(_core.getSkeletonCode() + " closes its ring from C" + anomeric + " to C"
					+ bridge.getEndPositions().get(0) + " through " + bridge.getMAP() + " in place of the ring oxygen,"
					+ " which GlycanBuilder2 cannot represent, so it is not read");
		}
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
