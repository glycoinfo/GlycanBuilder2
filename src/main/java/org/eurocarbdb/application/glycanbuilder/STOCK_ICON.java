/**
 * 
 */
package org.eurocarbdb.application.glycanbuilder;

public enum STOCK_ICON {
	
	DOCUMENT_NEW("actions/document-new"),
	DOCUMENT_OPEN("actions/document-open"),
	DOCUMENT_OPEN_ADDITIONAL("actions/document-open-additional"),
	DOCUMENT_SAVE("actions/document-save"),
	DOCUMENT_SAVE_AS("actions/document-save-as"),
	QUIT("actions/quit"),
	DOCUMENT_PRINT("actions/document-print"),
	HELP_CONTENTS("actions/help-contents"),
	HELP_ABOUT("actions/help-about"),
	UNDO("actions/undo"),
	REDO("actions/redo"),
	CUT("actions/cut"),
	COPY("actions/copy"),
	PASTE("actions/paste"),
	DELETE("actions/delete"),
	REFRESH("actions/refresh"),
	
	FAMILY("actions/agt_family");
	//DB("apps/database");
	
	
	private String identifier;
	public String getIdentifier() {
		return identifier;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	private STOCK_ICON(String identifier){
		this.identifier=identifier;
	}
}