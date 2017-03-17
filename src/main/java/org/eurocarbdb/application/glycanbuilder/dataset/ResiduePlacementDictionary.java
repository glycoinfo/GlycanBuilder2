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
/**
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

package org.eurocarbdb.application.glycanbuilder.dataset;

import java.util.*;
import java.util.List;
import java.text.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.image.*;

import javax.swing.*;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResiduePlacement;
import org.eurocarbdb.application.glycanbuilder.exception.InvalidStringFormatException;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
   The dictionary of the residue placements available in the
   application for a certain cartoon notation. Information about
   residue placements is loaded at run time from a configuration
   file. There is a single dictionary for each notation. The style
   dictionaries are stored in the workspace and passed to the
   renderers.

   @see ResiduePlacement

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class ResiduePlacementDictionary {

    
//    private LinkedList<ResiduePlacement> placements = new LinkedList<ResiduePlacement>();
  private List<ResiduePlacement> placements = Collections.synchronizedList(new LinkedList<ResiduePlacement>());
    
    //---- init

    /**
       Load the dictionary from a configuration file
     */

    public void loadPlacements(String filename) {
    // clear dict
    placements.clear();
    
    try {
    	BufferedReader is;
    	if(filename.startsWith("http")){
    		URLConnection conn;
          conn = new URL(filename).openConnection();
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
        if( line.length()>0 && !line.startsWith("%") ) 
//            placements.addLast(new ResiduePlacement(line));
           placements.add(new ResiduePlacement(line));        
        }

        is.close();
    } catch (IOException e) {
      LogUtils.report(e);
      placements.clear();
    } catch (InvalidStringFormatException e) {
      LogUtils.report(e);
      placements.clear();
    }
    }
   
    //------------------
    // Member access

    /**
       Return a residue placement for a specific residue or a default
       one if none is found.
       @param link the linkage from the residue to its parent
       @see ResiduePlacement
    */
    public ResiduePlacement getPlacement(Linkage link) {
    return getPlacement(link.getParentResidue(),link,link.getChildResidue());
    }

    /**
       Return a residue placement for a specific residue or a default
       one if none is found.
       @param link the linkage from the residue to its parent
       @param sticky specify if the parent placement had the sticky
       flag set
       @see ResiduePlacement
    */
    public ResiduePlacement getPlacement(Linkage link, boolean sticky) {
    return getPlacement(link.getParentResidue(),link,link.getChildResidue(),sticky);
    }

    /**
       Return a residue placement for a specific residue or a default
       one if none is found.
       @param parent the parent residue
       @param link the linkage to the parent
       @param child the residue for which the placement should be
       @see ResiduePlacement
    */
    public ResiduePlacement getPlacement(Residue parent, Linkage link, Residue child) {
    	for(ResiduePlacement a_objPlace : this.placements) {
    		if(a_objPlace.matches(parent, link, child)) return a_objPlace;
    	}
    	return new ResiduePlacement();
    }
    
    /**
       Return a residue placement for a specific residue or a default
       one if none is found.
       @param parent the parent residue
       @param link the linkage to the parent
       @param child the residue for which the placement should be
       @param sticky specify if the parent placement had the sticky
       flag set
       @see ResiduePlacement
    */
    public ResiduePlacement getPlacement(Residue parent, Linkage link, Residue child, boolean sticky) {
      for (Iterator iterator = placements.iterator(); iterator.hasNext();) {
        ResiduePlacement residuePlacement = (ResiduePlacement) iterator.next();
        if(residuePlacement.matches(parent, link, child)) return (sticky) ? residuePlacement.getIfSticky() : residuePlacement;
      }
    	return new ResiduePlacement();
    }
}