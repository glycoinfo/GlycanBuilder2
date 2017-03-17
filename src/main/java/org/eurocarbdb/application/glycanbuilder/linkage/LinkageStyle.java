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

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.LinkageRendererAWT;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

import java.util.regex.*;

/**
   This class contains the information about the graphic style of a
   linkage in a certain notation. The style specify line type, line
   shape, and which linkage information should be shown. The style
   will be used by the {@link LinkageRendererAWT} instances to render a
   linkage. The style will be matched against parent reside, child
   residue and linkage information according to the rules defined by a
   {@link LinkageMatcher}

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class LinkageStyle {

    private String         rule;
    private LinkageMatcher matcher;
    private boolean dashed;
    private String  shape;
    private String  show_info; 

    /**
       Create a default linkage style that will represent a linkage as
       a solid line around which the linkage information is shown when
       known.
     */
    public LinkageStyle() {    
    rule = "";
    matcher = LinkageMatcher.parse(rule);
    dashed  = false;
    shape   = "line";
    show_info = "222";
    }

    /**
      Create a new linkage style from an initialization string.
       @throws Exception if the string is in the wrong format
     */
    public LinkageStyle(String init) throws Exception {
    	LinkedList<String> tokens = TextUtils.tokenize(init,"\t");
    	if( tokens.size()!=4 ) 
    		throw new Exception("Invalid string format: " + init);

    	rule    = tokens.get(0);    
    	matcher = LinkageMatcher.parse(rule);
    	dashed  = (tokens.get(1).equals("yes") || tokens.get(1).equals("true"));
    	shape   = tokens.get(2);
    	show_info = tokens.get(3);
    }

    /**
       Create a default linkage style that will represent a linkage as
       a solid line around no linkage information is shown.
     */
    public LinkageStyle createPlain() {
    LinkageStyle ret = new LinkageStyle();
    show_info = "000";
    return ret;
    }
   
    /**
       Return the rule used to generate the {@link LinkageMatcher}
     */
    public String getRule() {
    return rule;
    }

    /**
       Return the {@link LinkageMatcher} used to match the style to a
       specific linkage
     */
    public LinkageMatcher getMatcher() {
    return matcher;
    }

    /**
       Return <code>true</code> if the style matches a specific
       linkage
       @param parent the parent residue in the linkage
       @param link the linkage for which the style should be retrieved
       @param child the child residue in the linkage       
     */
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return matcher.matches(parent,link,child);
    }
   
    /**
       Return <code>true</code> if the line used to represent the
       linkage should be dashed
     */
    public boolean isDashed() {
    return dashed;
    }

    /**
       Return the shape of the line used to represent the linkage
    */
    public String getShape() {
    return shape;
    }
    
    /**
       Return <code>true</code> if the information about the parent
       linkage position should be shown
       @param link contains the linkage information
     */
    public boolean showParentLinkage(Linkage link) {
    if( show_info.charAt(0)=='0' )
        return false;
    if( show_info.charAt(0)=='1' )
        return true;
    if( show_info.charAt(0)=='2' )
        return (link.hasMultipleBonds() || link.glycosidicBond().getParentPositions().length>1 || link.glycosidicBond().getParentPositions()[0]!=' ');
    if( show_info.charAt(0)=='3' )
        return (link.hasMultipleBonds() || link.glycosidicBond().getParentPositions().length>1 || link.glycosidicBond().getParentPositions()[0]=='?');
    return true;
    }

    /**
       Return <code>true</code> if the information about the anomeric
       state of the child residue should be shown
       @param link contains the linkage information
       @param anomer the anomeric state of the child residue
     */
    public boolean showAnomericState(Linkage link, char anomer) {
    	if(link.getChildResidue().getType().getSuperclass().equals("Bridge")) return false;
    	if( show_info.charAt(1)=='0')
        return false;
    if( show_info.charAt(1)=='1' )
        return link.hasSingleBond();
    if( show_info.charAt(1)=='2' )
        return (link.hasSingleBond()/* && anomer!='?'*/);
    if( show_info.charAt(1)=='3' )
        return (link.hasSingleBond() && anomer=='?');
    return true;
    }
    
    /**
       Return <code>true</code> if the information about the anomeric
       carbon of the child residue should be shown
       @param link contains the linkage information
    */
    public boolean showAnomericCarbon(Linkage link) {
    if( show_info.charAt(2)=='0' )
        return false;
    if( show_info.charAt(2)=='1' )
        return true;
    if( show_info.charAt(2)=='2' )
        return (link.hasMultipleBonds() || link.glycosidicBond().getChildPosition()!='?');
    if( show_info.charAt(2)=='3' )
        return (link.hasMultipleBonds() || link.glycosidicBond().getChildPosition()=='?');
    return true;
    }

    public String toString() {
    return rule + " " + dashed + " " + shape + " " + show_info; 
    }

}
