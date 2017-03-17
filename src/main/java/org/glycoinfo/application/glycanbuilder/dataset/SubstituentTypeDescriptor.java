package org.glycoinfo.application.glycanbuilder.dataset;

import java.util.ArrayList;

public enum SubstituentTypeDescriptor {

	OTYPE("O-type"),
	NTYPE("N-type"),
	ORGANIC("Organic"),
	INORGANIC("Inorganic"),
	UNKNOWN("Unknown");
	
	private String a_sClass;
	
	private SubstituentTypeDescriptor(String _a_sClass) {
		this.a_sClass = _a_sClass;
	}
	
	public String getClassName() {
		return this.a_sClass;
	}
	
	public static ArrayList<SubstituentTypeDescriptor> getTypeList() {
		ArrayList<SubstituentTypeDescriptor> a_aTypes = new ArrayList<SubstituentTypeDescriptor>();
	
		a_aTypes.add(OTYPE);
		a_aTypes.add(NTYPE);
		a_aTypes.add(INORGANIC);
		a_aTypes.add(ORGANIC);
		a_aTypes.add(UNKNOWN);
		
		return a_aTypes;
	}
	
	public static SubstituentTypeDescriptor forClass(String _a_sClass) {
		SubstituentTypeDescriptor[] a_enumArray = SubstituentTypeDescriptor.values();
		
		for(SubstituentTypeDescriptor a_enumStr : a_enumArray) {
			if(_a_sClass.equals(a_enumStr.a_sClass)) return a_enumStr;
		}
		
		return null;
	}
}
