package org.glycoinfo.application.glycanbuilder.dataset;

public enum SubstituentTypeDescriptor {

	OTYPE("O-type"),
	NTYPE("N-type"),
	DEOXYTYPE("Deoxy-type"),
	PSTYPE("P/S-type"),
	UNKNOWN("Unknown");

	private String a_sClass;

	private SubstituentTypeDescriptor(String _a_sClass) {
		this.a_sClass = _a_sClass;
	}

	public String getClassName() {
		return this.a_sClass;
	}

}
