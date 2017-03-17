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

package org.eurocarbdb.application.glycanbuilder.converterGlycoCT;

import org.eurocarbdb.MolecularFramework.io.*;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
   Read and write glycan structures in the several formats supported
   by the MolecularFramework library.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class MolecularFrameworkParser extends GlycoCTParser {

    /**
       Return a map of the supported formats for importing glycan
       structures. The map contains the identifier and the description
       of each format.
     */
    public static Map<String,String> getImportFormats() {
    	TreeMap<String,String> map = new TreeMap<String,String>();
    	for( CarbohydrateSequenceEncoding cse : SugarImporterFactory.getSupportedEncodings() ) 
    		map.put(cse.getId(),cse.getName());
    	return map;    
    }

    /**
       Return a map of the supported formats for exporting glycan
       structures. The map contains the identifier and the description
       of each format.
     */
    public static Map<String,String> getExportFormats() {
    	TreeMap<String,String> map = new TreeMap<String,String>();
    	for( CarbohydrateSequenceEncoding cse : SugarExporterFactory.getSupportedEncodings() ) 
    		map.put(cse.getId(),cse.getName());
    	return map;    
    }

    /**
       Return a map of all the supported formats for glycan
       structures. The map contains the identifier and the description
       of each format.
     */
    public static Map<String,String> getFormats() {
    	TreeMap<String,String> map = new TreeMap<String,String>();
    	for( CarbohydrateSequenceEncoding cse : SugarImporterFactory.getSupportedEncodings() ) 
    		map.put(cse.getId(),cse.getName());
    	for( CarbohydrateSequenceEncoding cse : SugarExporterFactory.getSupportedEncodings() ) 
    		map.put(cse.getId(),cse.getName());
    	return map;    
    }

    /**
       Return <code>true</code> if the string identifies a supported
       format.
     */
    public static boolean isSequenceFormat(String format) {
    	try {
    		return (CarbohydrateSequenceEncoding.forId(format)!=null);
    	}
    	catch(Exception e) {
    		return false;
    	}
    }

    // -----

    private CarbohydrateSequenceEncoding encoding = null;

    /**
       Create a new parser specific for a certain format
       @param format one of the encoding formats supported by the
       MolecularFramework library
     */
    public MolecularFrameworkParser(String format) {
    	super(false);
    
    	try {        
    		encoding = CarbohydrateSequenceEncoding.forId(format);
    	}
    	catch(Exception e) {
    		LogUtils.report(e);
    	}    
    }   

    public String writeGlycan(Glycan structure) {
    	try {
    		Sugar s = toSugar(structure);
    		return SugarExporterFactory.exportSugar(s,encoding);
    	}
    	catch(Exception e) {
    		LogUtils.report(e);
    		return "";
    	}
    }

    public Glycan readGlycan(String buffer, MassOptions default_mass_options) throws Exception {
		Sugar s = SugarImporterFactory.importSugar(buffer, encoding);
		return fromSugar(s, default_mass_options);
    }
}