package org.glycoinfo.application.glycanbuilder.dataset;

public enum TextSymbolDescriptor {

	FREEEND("freeEnd"),
	REDEND("redEnd"),
	PA("PA"),
	TAB("2AB"),
	AA("AA"),
	DAP("DAP"),
	FAB("4AB"),
	DAPMAB("DAPMAB"),
	AMC("AMC"),
	AQ("6AQ"),
	AAC("2AAc"),
	FMC("FMC"),
	DH("DH"),
	REDENDDEOXY("d"),
	ENDREP("#endrep_?_?"),
	STARTREP("#startrep"),
	BRACKET("#bracket"),
	STARTCYCLIC("#startcyclic"),
	ENDCYCLIC("#endcyclic"),
	STARTALT("#altstart"),
	ENDALT("#altend");
	
	String a_sRedType;
	
	private TextSymbolDescriptor(String _a_sRedType) {
		this.a_sRedType = _a_sRedType;
	}
	
	public static TextSymbolDescriptor forRedType(String _a_sRedType) {
		TextSymbolDescriptor[] enum_array = TextSymbolDescriptor.values();
		
		for(TextSymbolDescriptor enum_str : enum_array) {
			if(_a_sRedType.equals(enum_str.a_sRedType)) return enum_str;
		}
		
		return TextSymbolDescriptor.FREEEND;
	}
	
	public String toString() {
		return this.a_sRedType;
	}
}
