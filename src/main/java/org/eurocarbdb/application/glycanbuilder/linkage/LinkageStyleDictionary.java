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


package org.eurocarbdb.application.glycanbuilder.linkage;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
   The dictionary of the linkage styles available in the application
   for a certain cartoon notation. Information about linkage styles is
   loaded at run time from a configuration file. There is a single
   dictionary for each notation. The style dictionaries are stored in
   the workspace and passed to the renderers.

   @see LinkageStyle

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class LinkageStyleDictionary {
    
    private LinkedList<LinkageStyle> styles = new LinkedList<LinkageStyle>();
        
    //---- init

    /**
       Load the dictionary from a configuration file
     */
    public void loadStyles(String filename) {
    // clear dict
    styles.clear();
    
    try {
    	BufferedReader is;
    	if(filename.startsWith("http")){
    		URLConnection conn=new URL(filename).openConnection();
    		is=new BufferedReader(new InputStreamReader(conn.getInputStream()));
    	}else{
    		// open file
            java.net.URL file_url = ResidueDictionary.class.getResource(filename);
            if( file_url==null ){
            	File file=new File(filename);
            	if(file.exists()==false){
            		throw new FileNotFoundException(filename);
            	}else{
            		is=new BufferedReader(new FileReader(file));
            	}
            }else{
            	is = new BufferedReader(new InputStreamReader(file_url.openStream()));
            }
            
            
    	}
        
        // read dictionary
        String line;
        while( (line=is.readLine())!=null ) {
	        line = TextUtils.trim(line);
	        if( line.length()>0 && !line.startsWith("%") ) {
	            LinkageStyle toadd = new LinkageStyle(line);
	            styles.addLast(toadd);
	        }
        }

        is.close();
    }
    catch(Exception e) {
        LogUtils.report(e);
        styles.clear();
    }
    }
      
    
    // --- Data access

    /**
       Return a residue style with a give identifier or a default one
       (simple straight line to represent an edge) if none is found.
       @param link the linkage for which the style should be
       retrieved, the information about parent residue, and child
       residue is also used
       @see LinkageStyle
     */
    public LinkageStyle getStyle(Linkage link) {
    return getStyle(link.getParentResidue(),link,link.getChildResidue());
    }

    /**
       Return a residue style with a give identifier or a default one
       (simple straight line to represent an edge) if none is found.
       @param parent the parent residue in the linkage
       @param link the linkage for which the style should be retrieved
       @param child the child residue in the linkage       
       @see LinkageStyle#matches
     */
    public LinkageStyle getStyle(Residue parent, Linkage link, Residue child) {
      for (Iterator iterator = styles.iterator(); iterator.hasNext();) {
        LinkageStyle a_objLinkage = (LinkageStyle) iterator.next();
        if(null != a_objLinkage && a_objLinkage.matches(parent, link, child)) return a_objLinkage;
    	}
    	return new LinkageStyle();
    }
}
