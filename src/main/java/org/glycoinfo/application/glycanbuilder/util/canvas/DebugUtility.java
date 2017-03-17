package org.glycoinfo.application.glycanbuilder.util.canvas;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;

public class DebugUtility {

	public static void shoeIndex(Glycan a_oGlycan) {
		for(Residue a_oRES : a_oGlycan.getAllSaccharide()) {
			a_oRES.setAntennaID(a_oGlycan.getAllSaccharide().indexOf(a_oRES) + 1);
		}
		
		return;
	}
	
	public static void showID(Glycan a_oGlycan) {
		for(Residue a_oRES : a_oGlycan.getAllSaccharide()) {
			a_oRES.setAntennaID(a_oGlycan.getAllSaccharide().indexOf(a_oRES) + 1);
		}
		
		return;
	}
	
	public static void removeAnotation(Glycan a_oGlycan) {
		for(Residue a_oRES : a_oGlycan.getAllSaccharide()) {
			a_oRES.setAntennaID(-1);
		}
		
		return;
	}
}
