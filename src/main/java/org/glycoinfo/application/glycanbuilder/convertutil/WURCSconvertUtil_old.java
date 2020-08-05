package org.glycoinfo.application.glycanbuilder.convertutil;

import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.glycoinfo.GlycanFormatconverter.util.exchange.SugarToWURCSGraph.BaseTypeForRelativeConfiguration;
import org.glycoinfo.WURCSFramework.util.residuecontainer.ResidueContainer;
import org.glycoinfo.WURCSFramework.util.residuecontainer.SuperClass;

public class WURCSconvertUtil_old {

	static public StringBuilder convertSugartoBase(LinkedList<String> lst_name, String str_DLconfiguration) {
		StringBuilder str_SC = new StringBuilder();
		
		try {
			for(String str_unit : lst_name) {
				String str_DLconfigurationTMP = WURCSconvertUtil_old.checkChilarity(str_unit);
				str_unit = WURCSconvertUtil_old.convertNativeModificaiton(
						str_unit.replaceFirst(str_DLconfigurationTMP, "").substring(0, 3));
				SuperClass enum_base = SuperClass.getName(str_unit);//BaseType_old.getName(str_unit);
				if(enum_base != null) str_SC.append(enum_base.getBasetype());
				else {
					BaseTypeForRelativeConfiguration enum_BTFRC =
							BaseTypeForRelativeConfiguration.forName("x" + str_unit);
					str_SC.append(enum_BTFRC.getName().contains("gro") && str_DLconfigurationTMP.equals("d") ? 4 : 
						enum_BTFRC.getName().contains("gro") && str_DLconfigurationTMP.equals("l") ? 3 :
						enum_BTFRC.getStereoCode());
				}
			}
			
			/**check chilarity and convert relative position*/
			str_SC = convertRelationalPositon(str_SC.toString(), str_DLconfiguration);
			str_SC = convertAbsoluteConfiguration(str_SC.toString(), str_DLconfiguration);
		} catch (Exception e) {
			LogUtils.report(e);
		}
		
		return str_SC;
	}
	
	static public boolean checkSugarType(String str_sugarName) {
		if(str_sugarName.contains("Neu")) return true;
		if(str_sugarName.contains("KDO")) return true;
		if(str_sugarName.contains("Fru")) return true;
		
		return false;
	}
	
	static public String convertNativeModificaiton(String str_name) {
		if(str_name.equals("fru")) return "ara";
		if(str_name.equals("rha")) return "man";
		if(str_name.equals("qui")) return "glc";
		if(str_name.equals("fuc")) return "gal";
		
		return str_name;
	}
	
	static public String checkChilarity(String str_mono) {
		if(str_mono.startsWith("l")) return "l";
		if(str_mono.startsWith("d")) return "d";
		
		return "";
	}
	
	static public StringBuilder checkComposition(StringBuilder str_SC, ResidueContainer a_objWJE) {
		String str_sugarName = a_objWJE.getIUPACExtendedNotation();
		
		if(str_sugarName.contains("Neu") || str_sugarName.contains("Kdo") ||
				str_sugarName.contains("Non")) str_SC.insert(0, "Ad");
		if(str_sugarName.contains("dgro-dtalOct")) str_SC.insert(0, "A");
		
		if(str_sugarName.contains("Qui") || 
				str_sugarName.contains("Fuc") || 
				str_sugarName.contains("Rha")) return str_SC.append("m");
		else str_SC.append("h");

		if(str_sugarName.contains("Fru")) str_SC.insert(0, "h");
		
		return str_SC;
	}
	
	static private StringBuilder convertAbsoluteConfiguration(String str_SC, String str_DLconfiguration) {
		StringBuilder ret = new StringBuilder();
		if(str_DLconfiguration.equals("?")) return ret.append(str_SC);
		for(String s : str_SC.split("")) {
			if(s.equals("3")) ret.append("1");
			if(s.equals("4")) ret.append("2");
			
			if(!s.equals("3") && !s.equals("4")) ret.append(s);
		}
		
		return ret;
	}
	
	static private StringBuilder convertRelationalPositon(String str_SC, String str_DLconfiguration){
		StringBuilder ret = new StringBuilder();
		if(!str_DLconfiguration.toLowerCase().equals("l")) return ret.append(str_SC);
		for(String s : str_SC.split("")) {
			if(s.equals("3")) ret.append("4");
			if(s.equals("4")) ret.append("3");
			
			if(!s.equals("3") && !s.equals("4")) ret.append(s);
		}
		
		return ret;
	}
	
	static public char getAnomerSymbol(char char_anomerSymbol) {
		if(char_anomerSymbol == ' ' || char_anomerSymbol == 'o') return '?';
		return char_anomerSymbol;		
	}
	
	static public char getAnomerPosition(int int_anomerPosition) {
		if(int_anomerPosition == 0) return '?';
		else return String.valueOf(int_anomerPosition).charAt(0);
	}
}
