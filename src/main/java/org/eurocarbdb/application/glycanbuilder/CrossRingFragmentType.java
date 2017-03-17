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

import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
   Specification of a cross ring frament type.
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class CrossRingFragmentType extends ResidueType {

	//

	private String str_residues;
	private LinkedList<String> residues;        
	private char     anomeric_carbon;
	private char    ring_size;

	private char    fragment_type; // a/x
	private int     first_pos;
	private int     last_pos;    

	//--

	/**
       Empty constructor.
	 */
	public CrossRingFragmentType() {
		super();

		str_residues = "";
		residues = new LinkedList<String>();
		anomeric_carbon = '?';
		ring_size = '?';        

		fragment_type = '-';
		first_pos = 0;
		last_pos = 0;    
	}

	/**
       Create a new residue type from an initialization string.   
	 */
	public CrossRingFragmentType(String init) throws Exception {
		super();

		LinkedList<String> tokens = TextUtils.tokenize(init,"\t");
		if( tokens.size()!=12 ) 
			throw new Exception("Invalid string format: " + init);

		str_residues    = tokens.get(0);
		residues        = parseArray(tokens.get(0));
		anomeric_carbon = tokens.get(1).charAt(0);
		ring_size       = Character.toLowerCase(tokens.get(2).charAt(0));
		fragment_type   = Character.toUpperCase(tokens.get(3).charAt(0));
		first_pos       = Integer.parseInt(tokens.get(4));
		last_pos        = Integer.parseInt(tokens.get(5));
		nmethyls        = Integer.parseInt(tokens.get(6));
		nacetyls        = Integer.parseInt(tokens.get(7));
		nlinkages       = Integer.parseInt(tokens.get(8));    
		linkage_pos     = parseCharArray(tokens.get(9));
		charges_pos     = parseCharArray(tokens.get(10));
		composition     = tokens.get(11);

		//

		name = "#" + Character.toLowerCase(fragment_type) + "cleavage_" + first_pos + "_" + last_pos;
		superclass = "cleavage";
		is_saccharide = false;
		is_cleavable = false;
		is_labile    = false;
		bar_order = 0;
		drop_methylated = false;
		drop_acetylated = false;
		can_redend = (fragment_type == 'A');
		can_parent = !can_redend;    
		description = fragment_type + "_" + first_pos + "_" + last_pos + " cleavage of C" + anomeric_carbon + " " + str_residues;

		this.updateMolecule();    
	}   

	private LinkedList<String> parseArray(String str) throws Exception {
		if( str.equals("-") || str.equals("none") || str.equals("empty") )
			return new LinkedList<String>();
		return TextUtils.tokenize(str,",");
	}

	/**
       Return <code>true</code> if the ring fragment type can be
       applied to the given residue.
	 */
	public boolean matches(Residue r) {
		return (residues.contains(r.getTypeName()) && r.getAnomericCarbon()==anomeric_carbon && r.getRingSize()==ring_size);
	}    

	/**
       Return <code>true</code> if the ring fragment type matches the
       given information.
       @param _fragment_type type of cross ring fragment (A/B)
       @param _first_pos position of the first cleavage in the ring
       @param _last_pos position of the second cleavage in the ring
       @param r residue for which the ring fragment is computed
	 */
	public boolean matches(char _fragment_type, int _first_pos, int _last_pos, Residue r) {
		_fragment_type = Character.toUpperCase(_fragment_type);
		return ( fragment_type==_fragment_type && first_pos==_first_pos && last_pos==_last_pos && matches(r) );        
	}

	//

	/**
       Return the list of residue type identifiers to which this ring
       fragment can be applied.
	 */
	public Collection<String> getResidues() {
		return residues;
	}

	/**
       Return <code>true</code> if this is an A ring fragment.
	 */
	public boolean isAFragmentType() {
		return (fragment_type=='A');
	}

	/**
       Return <code>true</code> if this is an X ring fragment.
	 */
	public boolean isXFragmentType() {
		return (fragment_type=='X');
	}

	/**
       Return the type (A/X) of ring fragment.
	 */
	public String getRingFragmentType() {
		return "" + fragment_type;
	}   

	/**
       Return a text description of the ring fragment.
	 */
	public String getCleavageType() {
		return  "^{" + first_pos + "," + last_pos + "}" + fragment_type;
	}   

	/**
       Return the position of the first cleavage in the ring.
	 */
	public int getFirstPos() {
		return first_pos;
	}

	/**
       Return the position of the second cleavage in the ring.
	 */
	public int getLastPos() {
		return last_pos;
	}

	int getStartPos() {
		int ret = -1;
		int anom = (int)(anomeric_carbon-'0');
		if( fragment_type=='A' ) 
			return ((first_pos>=anom) ?first_pos :last_pos);
		else 
			return ((first_pos>=anom) ?last_pos :first_pos);    
	}

	int getEndPos() {
		int anom = (int)(anomeric_carbon-'0');
		if( fragment_type=='A' ) 
			return ((first_pos>=anom) ?last_pos :first_pos);
		else 
			return ((first_pos>=anom) ?first_pos :last_pos);    
	}

	/*
    static public ResidueType createACleavage(char first_pos, char last_pos) {
    ResidueType ret = new ResidueType();
    ret.name = "#zcleavage";
    ret.description = "Z Cleavage";
    ret.superclass  = "cleavage";
    ret.can_redend = false;
    ret.can_parent = true;
    return ret;
    } 
	 */   
}