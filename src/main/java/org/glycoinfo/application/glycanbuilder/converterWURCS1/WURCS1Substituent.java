package org.glycoinfo.application.glycanbuilder.converterWURCS1;

public enum WURCS1Substituent {
	/**from paper of WURCS1.0*/
	HYDROGEN("*H", "hydrogen", "h", "modification"),
	HYDROXYL("*OH", "hydroxyl", "o", "modification"),
	ETHER("*O*", "ethyl", "o", "modificaiton"),
	METHYL_deoxy("*C", "methyl", "Me", "substituent"),
	METHYL("*OC", "methyl", "Me", "substituent"),
	ACETYL("*OCC/3=O", "acetyl", "Ac", "substituent"),
	N_ACETYL("*NCC/3=O", "n-acethyl", "NAc", "substituent"),
	N_GLYCOLYL("*NCCO/3=O", "n-glycolyl", "NGc", "substituent"),
	PHOSPHATE("*OPO/3O/3=O", "phosphate", "P", "substituent"),
	SULFATE("*OSO/3=O/3=O", "sulfate", "S", "substituent"),
	PYRUVATE("*OCCC/4=O/3=O", "pyruvate", "Pyr", "substituent"),
	PHOSPHOCHOLINE("*OPOCCNC/7C/7C/3O/3=O", "phosphocholine", "PC", "substituent"),
	DIPHOSPHOETHANOLAMINE("*OPOPOCCN/5O/5=O/3O/3=O", "diphosphoethanolamine", "PPEtn", "substituent"),
	PHOSPHOETHANOLAMINE("*OPOCCN/3O/3=O", "phosphoethanolamine", "PEtn", "substituent"),
	AMINE("*N", "amine", "N", "substituent"),
	GLYCOLYL("*OCCO/3=O", "glycolyl", "Gc", "substituent");
	
	private String str_MAP;
	private String str_ctName;
	private String str_canvasName;
	private String str_type;
	
	public void setSugarName(String in) {
		this.str_canvasName = in;
	}
	
	public String getBaseType() {
			return this.str_MAP;
	}
		
	public String getBuilderBasetype() {
		return this.str_ctName;
	}

	public String getCanvasName() {
		return this.str_canvasName;
	}
	
	public String getType() {
		return this.str_type;
	}
	
	public boolean isSubstituent() {
		if(this.str_type.equals("substituent")) return true;
		return false;
	}
	
	//monosaccharide construct
	private WURCS1Substituent(String _basetype, String _builderbasetype, String _sugarname, String _type) {
		this.str_MAP = _basetype;
		this.str_ctName = _builderbasetype;
		this.str_canvasName = _sugarname;
		this.str_type = _type;
	}
		
	/*
	 * @param : SkeletonCode
	 * @return : Glycan builder base sugar basetype
	 */
	public static WURCS1Substituent getBaseType(String str_basetype) {
		WURCS1Substituent[] enumArray = WURCS1Substituent.values();

		for(WURCS1Substituent enumStr : enumArray)
			if (str_basetype.equals(enumStr.str_MAP.toString())) return enumStr;
		return null;
	}
	
	public static WURCS1Substituent getModificationName(String str_modificaiton) {
		WURCS1Substituent[] enumArray = WURCS1Substituent.values();

		for(WURCS1Substituent enumStr : enumArray)
			if (str_modificaiton.equals(enumStr.str_ctName.toString())) return enumStr;
		return null;
	}
		
	public static WURCS1Substituent getCanvasName(String str_canvasName) {
		WURCS1Substituent[] enumArray = WURCS1Substituent.values();

		for(WURCS1Substituent enumStr : enumArray)
			if (str_canvasName.equals(enumStr.str_canvasName.toString())) return enumStr;
		return null;
	}
	
	@Override
	public String toString() {
		return str_MAP;
	}
	
}
