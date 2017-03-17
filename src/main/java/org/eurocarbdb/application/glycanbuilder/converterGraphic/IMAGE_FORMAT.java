/**
 * 
 */
package org.eurocarbdb.application.glycanbuilder.converterGraphic;

public enum IMAGE_FORMAT{
	PNG(".png"),
	GIF(".gif"),
	JPG(".jpg"),
	SVG(".svg");
	
	String extension;
	
	IMAGE_FORMAT(String extension){
		this.extension=extension;
	}
}