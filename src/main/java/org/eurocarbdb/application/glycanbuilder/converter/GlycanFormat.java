package org.eurocarbdb.application.glycanbuilder.converter;

public enum GlycanFormat {
	GWS("gws"),
	GW_LINUCS("gwlinucs"),
	GLYCOMINDS("glycominds"),
	GLYCOCT("glycoct"),
	GLYCOCT_CONDENSED("glycoct_condensed"),
//	RINGS("kcf"),
	WURCS1("wurcs1"),
	WURCS2("wurcs2");
	
	
	String format;
	GlycanFormat(String _format){
		format=_format;
	}
	
	public String toString(){
		return format;
	}
}
