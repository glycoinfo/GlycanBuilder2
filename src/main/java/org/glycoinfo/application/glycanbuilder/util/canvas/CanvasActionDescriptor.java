package org.glycoinfo.application.glycanbuilder.util.canvas;

public enum CanvasActionDescriptor {

	STRINGNOTATION("stringNotation"),
	ANTENNAPARENT("antennaParent"),
	UNDO("undo"),
	REDO("redo"),
	CUT("cut"),
	COPY("copy"),
	PASTE("paste"),
	DELETE("delete"),
	SELECTSTRUCTURE("selectstructure"),
	SELECTALL("selectall"),
	SELECTNONE("selectnone"),
	GOTOSTART("gotostart"),
	GOTOEND("gotoend"),
	ORDERSTRUCTURESASC("orderstructuresasc"),
	ORDERSTRUCTURESDESC("orderstructuresdesc"),
	ADDTERMINAL("addterminal"),
	ADDCOMPOSITON("addcomposition"),
	ADDSTRUCTURE("addstructure"),
	ADDSTRUCTURESTR("addstructurestr"),
	GETSTRUCTURE("getstructure"),
	GETSTRUCTURESTR("getstructurestr"),
	ADD("add"),
	INSERT("insert"),
	CHANGE("change"),
	REDEND("redend"),
	BRACKET("bracket"),
	REPEAT("repeat"),
	CYCLIC("cyclic"),
	ALTERNATIVE("alternative"),
	CHANGEREDEND("changeredend"),
	MASSOPTSTRUCT("massoptstruct"),
	
	NOTATION("notation"),
	DISPLAY("display"),
	DISPLAYSETTINGS("displaysettings"),
	SHOWINFO("showinfo"),
	SCALE("scale"),
	COLLLAPSE("collapsemultipleantennae"),
	SHOWMASSCANVAS("showmassescanvas"),
	SHOWMASS("showmasses"),
	SHOWREDENDCANVAS("showredendcanvas"),
	SHOWREDEND("showredend"),
	SAVESPEC("savespec"),
	ORIENTATION("orientation"),
	PROPERTIES("properties"),
	SETPROPERTY("setproperties"),
	SETPROPERTYR("setproperties_r"),
	MOVECCW("moveccw"),
	MOVECW("movecw"),
	NAVUP("navup"),
	NAVDOWN("navdown"),
	NAVLEFT("navleft"),
	NAVRIGHT("navright"),
	EXPLODE("explode"),
	IMPLODE("implode"),
	
	CHANGELV2("changeLV2"),
	CHANGELV3("changeLV3"),
	CHANGELV4("changeLV4"),
	SHOWID("showid"),
	SHOWINDEX("showindex"),
	REMOVEAANOTATION("removeanotation"),
	INSERTBRIDGE("bridge");
	
	String a_sAction;
	
	private CanvasActionDescriptor(String _a_sAction) {
		this.a_sAction = _a_sAction;
	}
	
	public static CanvasActionDescriptor forActions(String _a_sAction) {
		CanvasActionDescriptor[] enum_array = 
				CanvasActionDescriptor.values();
		
		for(CanvasActionDescriptor enum_str : enum_array) {
			if(_a_sAction.equals(enum_str.a_sAction)) return enum_str;
		}
		
		return null;
	}
}
