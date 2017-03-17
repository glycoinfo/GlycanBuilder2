package org.glycoinfo.application.glycanbuilder.util.canvas;

import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;

public class CompositionUtility {

	public static void onChangeLV3(GlycanDocument theDoc, Glycan a_oGlycan) {
		try {
			if(a_oGlycan == null)
				throw new Exception("This utility is need to select some structure");
			
			LinkedList<Residue> a_aResidues = copy(a_oGlycan);
			a_oGlycan = Glycan.createComposition(a_oGlycan.getMassOptions());
			for(Residue a_oRES : a_aResidues) {
				a_oRES.isComposition(true);
				a_oGlycan.addAntenna(a_oRES);
			}
			
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage(),
					"Error while creating the cyclic unit",
					JOptionPane.ERROR_MESSAGE);
		}
		theDoc.addStructure(a_oGlycan);
		
		return;
	}
	
	public static void onChangeLV4(GlycanDocument theDoc, Glycan a_oGlycan) {
		try {
			if(a_oGlycan == null)
				throw new Exception("This utility is need to select some structure");
			
			LinkedList<Residue> a_aResidues = convertMotif(a_oGlycan);
			a_oGlycan = Glycan.createComposition(a_oGlycan.getMassOptions());
			for(Residue a_oRES : a_aResidues) {
				a_oRES.isComposition(true);
				a_oGlycan.addAntenna(a_oRES);
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage(),
					"Error while creating the cyclic unit",
					JOptionPane.ERROR_MESSAGE);
		}
		theDoc.addStructure(a_oGlycan);
		
		return;
	}
	
	private static LinkedList<Residue> copy(Glycan a_oGlycan) {
		LinkedList<Residue> a_aResidues = new LinkedList<Residue>();
		for(Residue a_oRES : a_oGlycan.getAllResidues()) {
			Residue a_oClone = a_oRES.cloneResidue();
			a_oClone.setAnomericState('?');
			a_aResidues.add(a_oClone);
		}
		
		return a_aResidues;
	}
	
	private static LinkedList<Residue> convertMotif(Glycan a_oGlycan) {
		LinkedList<Residue> a_aResidues = new LinkedList<Residue>();
		
		for(Residue a_oRES : a_oGlycan.getAllResidues()) {
			Residue a_oMotif = null;
			String a_sSuperClass = a_oRES.getType().getSuperclass();

			try {
				if(a_sSuperClass.equals("Hexosamine"))			a_oMotif = ResidueDictionary.newResidue("HexN");
				if(a_sSuperClass.equals("Hexose")) 				a_oMotif = ResidueDictionary.newResidue("Hex");
				if(a_sSuperClass.equals("Hexuronic acid"))		a_oMotif = ResidueDictionary.newResidue("HexA");
				if(a_sSuperClass.equals("N-Acetylhexosamine"))	a_oMotif = ResidueDictionary.newResidue("HexNAc");
				if(a_sSuperClass.equals("Pentose"))				a_oMotif = ResidueDictionary.newResidue("Pen");
				if(a_sSuperClass.equals("6-deoxy-Hex"))			a_oMotif = ResidueDictionary.newResidue("dHex");
				if(a_sSuperClass.equals("Di-deoxyhexose"))		a_oMotif = ResidueDictionary.newResidue("ddHex");
				if(a_sSuperClass.equals("Ketose"))				a_oMotif = ResidueDictionary.newResidue("Ketose");
				if(a_sSuperClass.equals("6-deoxy-HexNAc"))		a_oMotif = ResidueDictionary.newResidue("dHexNAc");
				if(a_sSuperClass.equals("Unknown"))				a_oMotif = ResidueDictionary.newResidue("Unknown");

				if(a_oMotif != null) a_aResidues.add(a_oMotif);
				if(a_sSuperClass.equals("Substituent")) a_aResidues.add(a_oRES);
			} catch (Exception e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
			
		return a_aResidues;
	}
}
