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

		// This builder's own dictionary answers first: it knows every residue the exporter can
		// write, which is more than the naming converter covers - a substituent it does not know,
		// a skeleton it cannot read, or a bridge across the anomeric carbon each made it throw.
		MonosaccharideMSDictionary.Match match =
				MonosaccharideMSDictionary.match(_gres.getMS().getString());

		String trivialName;
		char configuration;
		TrivialNameConverter trinConv = null;

		if(match != null) {
			trivialName = match.getResidueType().getName();
			// from the description rather than from the type: the same residue writes a different
			// skeleton in each configuration, and only the description says which one this is
			configuration = match.getConfiguration();
		} else {
			trinConv = new TrivialNameConverter();
			trinConv.start(_gres);
			trivialName = trinConv.getTrivialName().replace("5", "");
			configuration = getConfiguration(((Monosaccharide) trinConv.getNode()).getStereos());
		}

		ResidueType newType = ResidueDictionary.findResidueType(trivialName);
		Residue residue = new Residue(newType);

		// The legend names this residue in the box drawn beside the structure, for a residue with no
		// symbol of its own. It belongs to the residue: writing it onto the type, which every residue
		// of that name shares, meant reading a second structure changed what the first one said.
		if(NonSymbolicResidueDictionary.hasResidueType(trivialName)) {
			String legend = this.makeLegend(_gres, trinConv);
			if(legend != null) residue.setLegend(legend);
		}

		if(!_gres.getMS().getString().contains("<Q>")  && residue.getTypeName().equals("Sugar"))
			throw new WURCSToGlycanException(_gres.getMS().getString() + " is not handled in GlycanBuilder");
		
		residue.setWasSticky(isSticky(trivialName));
		residue.setAlditol(this.isAlditol());
		residue.setAldehyde(this.isAldehyde());
	
		residue.setAnomericCarbon(this.checkAnomerPosition());
		residue.setAnomericState(this.checkAnomerSymbol());
		residue.setChirality(configuration);
		residue.setRingSize(residue.isAlditol() ? 'o' : this.ringSize);

		// the skeleton is matched whole, so a recognised residue leaves no core modification behind
		this.modifications = (trinConv == null) ? new ArrayList<String>() : trinConv.getModifications();
		this.residue = residue;
	}

	/**
	 * The IUPAC name shown as a legend under residues drawn without a symbol. It comes from a
	 * converter that does not cover every residue this builder can draw, so one it cannot name goes
	 * without a legend rather than taking the structure down.
	 * @param _gres Residue being read.
	 * @param _trivialNameConverter Converter already run for this residue, or null.
	 * @return Returns the legend, or null when none could be built.
	 */
	private String makeLegend(GRES _gres, TrivialNameConverter _trivialNameConverter) {
		try {
			TrivialNameConverter converter = _trivialNameConverter;
			if(converter == null) {
				converter = new TrivialNameConverter();
				converter.start(_gres);
			}

			return converter.getIUPACNotation();
		} catch (Exception cannotBeNamed) {
			return null;
		}
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

	private char getConfiguration (LinkedList<String> _stereos) {
		if (_stereos.isEmpty()) return '?';
		String ret = _stereos.getFirst();
		if (ret.length() == 3) return '?';
		return ret.substring(0, 1).toUpperCase().charAt(0);
	}
}
