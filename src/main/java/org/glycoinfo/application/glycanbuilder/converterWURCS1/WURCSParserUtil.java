package org.glycoinfo.application.glycanbuilder.converterWURCS1;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.glycoinfo.WURCSFramework.util.residuecontainer.SuperClass;
import org.glycoinfo.WURCSFramework.util.subsumption.MSStateDeterminationUtility;
import org.glycoinfo.WURCSFramework.util.subsumption.StereoBasetype;

public class WURCSParserUtil {
	
	public Residue convertSkeletonCodetoResidue(String str_BMU) throws Exception {
		Residue a_objRoot = new Residue();

		Matcher mat_BMU = Pattern.compile("([\\d\\w]+)(\\|(.+))?").matcher(str_BMU);
		if(mat_BMU.find()) {
			if(mat_BMU.group(3) != null) {
				LinkedList<String> lst_mod = new LinkedList<String>();
				String[] lst_str = mat_BMU.group(3).split("\\|");
				for(String s : lst_str) {
					if(!s.contains(",")) lst_mod.addLast(s);//this.lst_modification.addLast(s);
				}
				
				/**define anomer position*/
				char anomerState = ' ';
				if(lst_str[0].contains(",") && !lst_str[0].contains("x")) anomerState = lst_str[0].toCharArray()[0];
				else if(str_BMU.contains("X")) anomerState = String.valueOf(str_BMU.indexOf("X") + 1).charAt(0);
				else anomerState = '?';
				
				/**Define monosaccharide name*/
				String str_SC = mat_BMU.group(1);
				if(str_SC.contains("x") && !str_SC.contains("X"))
					a_objRoot = ResidueDictionary.newResidue(SuperClass.getBaseType(str_SC).getName());
							//BaseType_old.getBaseType(str_SC.substring(0, str_SC.indexOf("h"))).getName());
				else 
					a_objRoot = ResidueDictionary.newResidue(this.convertSekeletonCodetoBasetype(str_SC, anomerState, lst_mod));
				
				/**Define anomer position*/
				a_objRoot.setAnomericCarbon(anomerState);
				
				/**Define anomer carbon*/
				String str_anomerPos = "";
				if(a_objRoot.getAnomericCarbon() != '?') {
					str_anomerPos = String.valueOf(a_objRoot.getAnomericCarbon());
					if(mat_BMU.group(1).charAt(Integer.parseInt(str_anomerPos) - 1) == '1') a_objRoot.setAnomericState('a');
					else if(mat_BMU.group(1).charAt(Integer.parseInt(str_anomerPos) - 1) == '2') a_objRoot.setAnomericState('b');
					else a_objRoot.setAnomericState('?');
				}else a_objRoot.setAnomericState('?');
				
				/**Define chilarity*/
				if(str_SC.charAt(str_SC.length() - 2) == '1') a_objRoot.setChirality('L');
				if(str_SC.charAt(str_SC.length() - 2) == '2') a_objRoot.setChirality('D');
				if(a_objRoot.getChirality() == ' ') a_objRoot.setChirality('?');
				
				/**Define ring size*/
				if(lst_str[0].contains("1,5") || lst_str[0].contains("2,6")) a_objRoot.setRingSize('p');
				else if(lst_str[0].contains("1,4") || lst_str[0].contains("2,5")) a_objRoot.setRingSize('f');
				else a_objRoot.setRingSize('?');
				
				/**Define modification and substituent*/
				a_objRoot = this.generateSubstituent(a_objRoot, lst_mod);
				
				/**check sticky*/
				if(a_objRoot.getResidueName().equals("Fuc")) a_objRoot.setWasSticky(true);
				
				/**check alditol*/
				if(str_BMU.startsWith("H")) a_objRoot.setAlditol(true);
			}
		}	
		return a_objRoot;
	}
	
	private String checkNativeModification(String str_base, LinkedList<String> lst_mod) {
		/**native modification*/
		if(lst_mod.contains("6*m")) {
			lst_mod.remove("6*m");
			if(str_base.contains("gal")) str_base = "qui";
			if(str_base.contains("glc")) str_base = "fuc";
			if(str_base.contains("man")) str_base = "rha";
		}
		if(lst_mod.contains("3*d")) {
			if(str_base.equals("KDO") || str_base.equals("neu")) lst_mod.remove("3*d");
		}
		if(lst_mod.contains("6*A")) {
			lst_mod.remove("6*A");
			return str_base += "A";
		}
		if(lst_mod.contains("2*N")) {
			lst_mod.remove("2*N");
			return str_base += "N";
		}
		if(lst_mod.contains("5*NCC/3=O")) {
			lst_mod.remove("5*NCC/3=O");
			return str_base + "ac";
		}
		if(lst_mod.contains("5*NCCO/3=O")) {
			lst_mod.remove("5*NCCO/3=O");
			return str_base + "gc";
		}
		if(lst_mod.contains("2*NCC/3=O")) {
			lst_mod.remove("2*NCC/3=O");
			return str_base += "nac";
		}		
		return str_base;
	}
	
	private String convertSekeletonCodetoBasetype(String str_sc, char anomerPos, LinkedList<String> lst_mod) {
		int pos = -1;
		if(anomerPos != '?') pos = Integer.parseInt(String.valueOf(anomerPos)) - 1;
		
		String str_SC = "";
		for(int i = 0; i < str_sc.length(); i++) {
			char unit = str_sc.charAt(i);
			if(i != pos)
				str_SC += unit == '1' ? 
					'3' : unit == '2' ? 
					'4' : ' ';
			if(!String.valueOf(unit).matches("\\d")) lst_mod.addLast(i + 1 + "*" + unit);
		}
		
		MSStateDeterminationUtility a_objMSSDU = new MSStateDeterminationUtility();
		LinkedList<StereoBasetype> lst_SBT = a_objMSSDU.extractStereoBasetype(str_SC);
		
		String ret = "";
		if(lst_SBT.size() > 1) {
			for(StereoBasetype unit : lst_SBT) ret += unit.getThreeLetterCode();
			if(ret.equals("galgro")) ret = "neu";
		}else ret = lst_SBT.getFirst().getThreeLetterCode().toLowerCase();
		
		if(anomerPos == '2') {
			if(ret.equals("man")) ret = "KDO";
			if(ret.equals("ara")) ret = "Fru";
		}
		
		return this.checkNativeModification(ret, lst_mod);
	}
	
	private Residue generateSubstituent(Residue a_objResidue, LinkedList<String> lst_mod) throws Exception {
		Linkage obj_modLIN = new Linkage();
		if(lst_mod.size() == 0) return a_objResidue;
		for(String str_MAP : lst_mod) {
			String[] mod_unit = str_MAP.split("\\*");
			WURCS1Substituent basetype = WURCS1Substituent.getBaseType("*" + mod_unit[1]);
			if(basetype == null) continue;
			
			if(mod_unit[0].contains(",") || mod_unit[0].contains("-")) {

				return a_objResidue;
			}
			if(mod_unit[0].contains("\\")) {
				String[] ambiguous_pos = mod_unit[0].split("\\\\");
				char[] pos = new char[ambiguous_pos.length];
				for(int i = 0; i < ambiguous_pos.length; i++) pos[i] = ambiguous_pos[i].charAt(0);
				obj_modLIN.setLinkagePositions(pos);
				a_objResidue.addChild(ResidueDictionary.newResidue(basetype.getCanvasName()), obj_modLIN.getBonds());
				return a_objResidue;
			}
			/**normal substituent*/
			obj_modLIN.setLinkagePositions(mod_unit[0].charAt(0));
			a_objResidue.addChild(ResidueDictionary.newResidue(basetype.getCanvasName()), obj_modLIN.getBonds());
		}
		return a_objResidue;
	}
	
	public Glycan makeGlycan(Residue first, LinkedList<Residue> lst_Red, MassOptions mass_opt) throws Exception {
		Residue root = first.isAlditol() ? 
				ResidueDictionary.newResidue("redEnd") : ResidueDictionary.newResidue("freeEnd");
		root.addChild(first.hasParent() ? first.getStartRepetitionResidue() : first);
		
		Glycan ret = new Glycan(root, false, mass_opt);
		
		for(Residue r : lst_Red) {
			if(r.isInRepetition()) continue;
			if(!r.hasSaccharideParent()) ret.addAntenna(r, r.getParentLinkage().getBonds());
		}
		
		return ret;
	}
	
}
