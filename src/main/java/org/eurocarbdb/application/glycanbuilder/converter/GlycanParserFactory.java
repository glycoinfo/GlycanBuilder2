/*
 *   EuroCarbDB, a framework for carbohydrate bioinformatics
 *
 *   Copyright (c) 2006-2009, Eurocarb project, or third-party contributors as
 *   indicated by the @author tags or express copyright attribution
 *   statements applied by the authors.  
 *
 *   This copyrighted material is made available to anyone wishing to use, modify,
 *   copy, or redistribute it subject to the terms and conditions of the GNU
 *   Lesser General Public License, as published by the Free Software Foundation.
 *   A copy of this license accompanies this distribution in the file LICENSE.txt.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *   or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 *   for more details.
 *
 *   Last commit: $Rev$ by $Author$ on $Date::             $  
 */

package org.eurocarbdb.application.glycanbuilder.converter;

import java.util.*;

import org.eurocarbdb.application.glycanbuilder.converterGlycoCT.GlycoCTParser;
import org.eurocarbdb.application.glycanbuilder.converterGlycoCT.MolecularFrameworkParser;
import org.eurocarbdb.application.glycanbuilder.converterGlycoMinds.GlycoMindsParser;
import org.eurocarbdb.application.glycanbuilder.converterLINUCS.LinucsParser;
import org.eurocarbdb.application.glycanbuilder.converterGWS.GWSParser;
import org.glycoinfo.application.glycanbuilder.converterWURCS1.WURCSParser;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2ParserViaCT;

/**
   Factory class used to create instances of parsers for glycan
   structure encoding formats.
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class GlycanParserFactory {

	/**
       Return a map of the supported formats for importing glycan
       structures. The map contains the identifier and the description
       of each format.
	 */
	public static Map<String,String> getImportFormats() {
		return getImportFormats(false);
	}

	/**
       Return a map of the supported formats for exporting glycan
       structures. The map contains the identifier and the description
       of each format.
       @param add_internal if <code>true</code> add the internal
       GlycoWorkbench formats to the map
	 */
	public static Map<String,String> getImportFormats(boolean add_internal) {
		Map<String,String> ret = MolecularFrameworkParser.getImportFormats();
		if( add_internal )
			ret.put("GWS","GlycoWorkbench sequence");
		ret.put("glycominds","Glycominds");
		ret.put("gwlinucs","Linucs");
//		ret.put("rings","KCF Encoding");
		ret.put("wurcs1","WURCS1.0 Encoding");
		ret.put("wurcs2","WURCS2.0 Encoding");
		return ret;
	}

	/**
       Return a map of the supported formats for exporting glycan
       structures. The map contains the identifier and the description
       of each format.
	 */
	public static Map<String,String> getExportFormats() {
		Map<String,String> ret =  MolecularFrameworkParser.getExportFormats();
		ret.put("GWS", "GlycoWorkbench sequence");
		ret.put("glycominds","Glycominds");
//		ret.put("rings","KCF Encoding");
		ret.put("wurcs1","WURCS1.0 Encoding");
		ret.put("wurcs2 via GlycoCT","WURCS2.0 Encoding(with GlycoCT)");
		ret.put("wurcs2","WURCS2.0 Encoding");
		return ret;
	}

	/**
       Return a map of all the supported formats for glycan
       structures. The map contains the identifier and the description
       of each format.
	 */
	static public Map<String,String> getFormats() {
		Map<String,String> ret = MolecularFrameworkParser.getFormats();
		ret.put("GWS","GlycoWorkbench sequence");
		ret.put("glycominds","Glycominds");
		ret.put("gwlinucs","Linucs");
//		ret.put("rings","KCF");
		ret.put("wurcs1", "WURCS1.0");
		ret.put("wurcs2 via GlycoCT","WURCS2.0(with GlycoCT)");
		ret.put("wurcs2","WURCS2.0");
		return ret;
	}

	/**
       Return <code>true</code> if the string identifies a supported
       format.
	 */
	static public boolean isSequenceFormat(String format) {
		return getFormats().containsKey(format);
	}

	public enum GlycanSequenceFormat {
		GWS("gws"),
		GlycoMinds("Glycominds"),
		GwLinucs("Linucs"),
//		RINGS("KCF"),
		WURCS1("WURCS1"),
		WURCS2VIAGlLYCOCT("WURCS2 via GlycoCT"),
		WURCS2("WURCS2");

		String format;
		GlycanSequenceFormat(String format){
			this.format=format;
		}

		public String toString(){
			return this.format;
		}

	}

	/**
       Create a new instance of a glycan structure parser for a given
       format (Call getParser(GlycanSequenceFormat) instead.)
       @param format the identifier of the encoding format
       @throws Exception if the identifier does not represent a valid format
       @deprecated
	 */
	static public GlycanParser getParser(String format) throws Exception{

		// molecular framework formats
		if( MolecularFrameworkParser.isSequenceFormat(format) )
			return new MolecularFrameworkParser(format);

		// internal formats
		if( format.compareToIgnoreCase("gws")==0 ) 
			return new GWSParser();
		else if( format.compareToIgnoreCase("gwlinucs")==0 ) 
			return new LinucsParser();
		else if( format.compareToIgnoreCase("glycominds")==0 ) 
			return new GlycoMindsParser();
		else if( format.compareToIgnoreCase("glycoct")==0 ) 
			return new GlycoCTParser(false);
		else if( format.compareToIgnoreCase("glycoct_condensed")==0 ) 
			return new GlycoCTParser(false);
//		else if( format.compareToIgnoreCase("rings")==0 ) 
//			return new KCFParser();
		else if( format.compareToIgnoreCase("wurcs1")==0 ) 
			return new WURCSParser();
		else if( format.compareToIgnoreCase("wurcs2")==0 )
			return new WURCS2Parser();
		else if( format.compareToIgnoreCase("wurcs2 via GlycoCT") == 0 )
			return new WURCS2ParserViaCT();
		
		throw new Exception("Unsupported format-"+format+"-");
	}

	static public GlycanParser getParser(GlycanSequenceFormat glycanSequenceFormat) throws Exception{
		return getParser(glycanSequenceFormat.toString());
	}
}