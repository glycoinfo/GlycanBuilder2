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


package org.eurocarbdb.application.glycanbuilder;

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;

import org.eurocarbdb.application.glycanbuilder.exception.InvalidStringFormatException;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.linkage.LinkageMatcher;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.ResAngle;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

import java.util.regex.*;

/**
   This class contains the information about the placement of a
   residue around its parent in a certain notation. The placement
   information will be used by the {@link BookingManager} and {@link
   GlycanRendererAWT} instances to decide the position of a residue. The
   placement will define a set of possible position around the parent
   residue as a list of {@link ResAngle} values. The placement will be
   matched against parent reside, child residue and linkage
   information according to the rules defined by a {@link
   LinkageMatcher}

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class ResiduePlacement {

    private String         rule;
    private LinkageMatcher matcher;
    private ResAngle[]     positions;
    private boolean        on_border;
    private boolean        sticky;

    /**
       Create a default residue placement that will represent a
       residue in position 0 around its parent
     */
    public ResiduePlacement() {
    rule = "";
    matcher = LinkageMatcher.parse(rule);
    positions = new ResAngle[1];
    on_border = false;
    sticky = false;
    }

    /**
       Create a default residue placement that will represent a
       residue in a specified position around its parent
     */
    public ResiduePlacement(ResAngle _position, boolean _on_border, boolean _sticky) {
    rule = "";
    matcher = LinkageMatcher.parse(rule);
    positions = new ResAngle[1]; positions[0] = _position;
    on_border = _on_border;
    sticky = _sticky;
    }

    /**
       Create a new residue placement from an initialization string.
     * @throws InvalidStringFormatException 
       @throws Exception if the string is in the wrong format
     */
    public ResiduePlacement(String init) throws InvalidStringFormatException {
    	LinkedList<String> tokens = TextUtils.tokenize(init,"\t");
    	if( tokens.size()!=4 ) 
    		throw new InvalidStringFormatException("Invalid string format: " + init);

    	rule      = tokens.get(0);    
    	matcher   = LinkageMatcher.parse(rule);
    	positions = parsePositions(tokens.get(1));
    	on_border = parseBoolean(tokens.get(2));
    	sticky    = parseBoolean(tokens.get(3));
    }

    static private boolean parseBoolean(String str) {
    return (str.equals("true") || str.equals("yes"));
    }
    
    static private ResAngle[] parsePositions(String init) {
    	LinkedList<String> tokens = TextUtils.tokenize(init,",");

    	ResAngle[] ret = new ResAngle[tokens.size()];
    	for( int i=0; i<tokens.size(); i++ ) 
    		ret[i] = new ResAngle(tokens.get(i));            
    	return ret;
    }

    /**
       Return a copy of this object
     */
    public ResiduePlacement clone() {
    ResiduePlacement ret = new ResiduePlacement();
    
    ret.rule = this.rule;
    ret.matcher = LinkageMatcher.parse(rule);
    ret.positions = this.positions;
    ret.on_border = this.on_border;
    ret.sticky = this.sticky;
    
    return ret;
    }


    /**
       Return a copy of this object modified for the case that the
       parent has the sticky flag set
     */
    public ResiduePlacement getIfSticky() {
    ResiduePlacement ret = new ResiduePlacement();
    ret.rule = this.rule;
    ret.matcher = this.matcher;

    if( this.on_border ) {
        ret.on_border = true;
        ret.positions = this.positions;
    }
    else {
        ret.on_border = false;
        ret.positions = new ResAngle[] {new ResAngle(0)};
    }

    ret.sticky = true;

    return ret;
    }

    /**
       Return the rule used to generate the {@link LinkageMatcher}
     */
    public String getRule() {
    return rule;
    }

    /**
       Return the {@link LinkageMatcher} used to match the placement
       to a specific residue and its context
     */
    public LinkageMatcher getMatcher() {
    return matcher;
    }

    /**
       Return <code>true</code> if the placements matches a specific
       residue and its context
       @param parent the parent residue
       @param link the linkage to the parent
       @param child the residue for which the placement should be
       determined
     */
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return matcher.matches(parent,link,child);
    }

    /**
       The list of positions in which the residue could be placed
     */
    public ResAngle[] getPositions() {
    return positions;
    }
    
    /**
       The list of positions in which the residue could be placed
       represented as a comma separated list of tokens in a string
     */
    public String getStringPositions() {
    StringBuilder ss = new StringBuilder();
    for( int i=0; i<positions.length; i++ ) {
        if( i>0 )
        ss.append(",");
        ss.append(positions[i]);
    }
    return ss.toString();
    }

    /**
       Return <code>true</code> if this placement allows a specific
       position for a residuex       
     */
    public boolean hasPosition(ResAngle pos) {
    for( int i=0; i<positions.length; i++ )
        if( positions[i].equals(pos) )
        return true;
    return false;           
    }

    /**
       Return <code>true</code> if the residue should be placed on the
       border of its parent
       @see GlycanRendererAWT
     */
    public boolean isOnBorder() {
    return on_border;
    }
    
    /**
       Return <code>true</code> if all the residues in the subtree
       should be placed in position 0
       @see GlycanRendererAWT
     */
    public boolean isSticky() {
    return sticky;
    }

    /*
      public void toggleSticky() {
    sticky = !sticky;
    }
    */

    public String toString() {
    return rule + " " + getStringPositions() + " " + on_border + " "  + sticky;
    }
   
}