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

import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.application.glycanbuilder.PermutationGenerator;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.GWSParser;

import java.util.*;

/**
   Objects of this class contain the information about the linkage
   between two residues. A linkage can be formed by multipled chemical
   bonds between different atoms of the two residues. The linkage
   information can be partially or completely undefined.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class Linkage {

	//----------------------------
	// internal classes       

	/**
       Compare two linkage objects.
	 */
	static public class LinkageComparator implements java.util.Comparator<Linkage> {

		public int compare(Linkage o1, Linkage o2) {
			return GWSParser.toStringLinkage(o1).compareTo(GWSParser.toStringLinkage(o2));
		}

	}

	//----------------------------
	// members

	private Residue      parent;
	private Residue      child;
	private Vector<Bond> bonds; // the last bond is the glycosidic bond    
	private Residue		 a_oSubstituent; //Substituent bridge
	
	private LinkageType  a_enumParentType = LinkageType.UNVALIDATED;
	private LinkageType  a_enumChildType = LinkageType.UNVALIDATED;
	
	//----------------------------
	// construction

	/**
       Empty constructor.
	 */
	public Linkage() {
		parent = null;
		child = null;
		this.a_oSubstituent = null;

		setLinkagePositions('?');
	}

	/**
       Create a new linkage between a parent and child residue.
	 */
	public Linkage(Residue _parent, Residue _child) {
		parent = _parent;
		child = _child;
		this.a_oSubstituent = null;
		
		setLinkagePositions('?');
	}

	/**
       Create a new linkage between a parent and child residue at a
       specified parent position.
	 */
	public Linkage(Residue _parent, Residue _child, char link_pos) {
		parent = _parent;
		child = _child;
		this.a_oSubstituent = null;

		setLinkagePositions(link_pos);
	}

	/**
       Create a new linkage between a parent and child residue given a
       set of parent positions.
	 */
	public Linkage(Residue _parent, Residue _child, char[] link_poss) {
		parent = _parent;
		child = _child;
		this.a_oSubstituent = null;

		setLinkagePositions(link_poss);
	}

	/**
       Create a new linkage between a parent and child residue with
       chemical bonds between two pair of atoms.
       @param link_poss parent positions
       @param second_p_poss parent positions of the second bond
       @param second_c_pos child position of the second bond       
	 */
	public Linkage(Residue _parent, Residue _child, char[] link_poss, char[] second_p_poss, char second_c_pos) {
		parent = _parent;
		child = _child;
		this.a_oSubstituent = null;

		setLinkagePositions(link_poss,second_p_poss,second_c_pos);
	}

	/**
       Create a new linkage between a parent and child residue given a
       list of chemical bonds between them.
	 */
	public Linkage(Residue _parent, Residue _child, Collection<Bond> _bonds) { 
		parent =  _parent; 
		child = _child;
		this.a_oSubstituent = null;

		setLinkagePositions(_bonds);
	}


	//----------------------------
	// Members access

	/**
       Return the parent residue.
	 */
	public Residue getParentResidue() {
		return parent;
	}

	/**
       Set the parent residue.
	 */
	public void setParentResidue(Residue _parent) {
		parent = _parent;
	}

	/**
       Return the child residue.
	 */
	public Residue getChildResidue() {
		return child;
	}

	/**
       Set the child residue.
	 */
	public void setChildResidue(Residue _child) {
		child = _child;
	}

	public void setSubstituent(Residue _a_oSubstituent) {
		this.a_oSubstituent = _a_oSubstituent;
	}
	
	public Residue getSubstituent() {
		return this.a_oSubstituent;
	}
	
	/**
       Return the list of chemical bonds between the two residues.
	 */
	public Vector<Bond> getBonds() {
		return bonds;
	}

	/**
       Return an ordered list of chemical bonds between the two residues.
	 */
	public Vector<Bond> getBondsSorted() {
		Vector<Bond> ret = new Vector<Bond>(bonds);
		Collections.sort(ret,new Bond.Comparator());
		return ret;
	}
	
	/**
       Set the list of chemical bonds between the two residues.
	 */
	public void setBonds(Vector<Bond> _bonds) {
		if( _bonds!=null && _bonds.size()>0 ) {
			bonds = _bonds;
			setAnomericCarbon(child.getAnomericCarbon());
		}
		else
			setLinkagePositions('?');
	}

	/**
       Return the chemical bond between the anomeric carbon of the
       child residue and the parent residue.
	 */
	public Bond glycosidicBond() {
		return bonds.get(bonds.size()-1);
	}

	/**
       Return the number of chemical bonds forming the linkage.
	 */
	public int getNoBonds() {
		return bonds.size();
	}

	/**
       Return <code>true</code> if the linkage is formed by a single
       chemical bond.
	 */
	public boolean hasSingleBond() {
		return bonds.size()==1;
	}

	/**
       Return <code>true</code> if the linkage is formed by multiple
       chemical bonds.
	 */
	public boolean hasMultipleBonds() {
		return bonds.size()>1;
	}  

	/**
       Set the parent position for this linkage.
	 */
	public void setLinkagePositions(char link_pos) {
		bonds = new Vector<Bond>(0,1);

		char c_pos = (child==null) ?'?' :child.getAnomericCarbon();
		bonds.add(new Bond(link_pos,c_pos));
	}

	/**
       Set the parent positions for this linkage.
	 */
	public void setLinkagePositions(char[] link_poss) {
		bonds = new Vector<Bond>(0,1);

		char c_pos = (child==null) ?'?' :child.getAnomericCarbon();
		bonds.add(new Bond(link_poss,c_pos));
	}    

	/**
       Set the positions for the chemical bonds forming this linkage.
       @param link_poss parent positions
       @param second_p_poss parent positions of the second bond
       @param second_c_pos child position of the second bond       
	 */
	public void setLinkagePositions(char[] link_poss, char[] second_p_poss, char second_c_pos) {
		bonds = new Vector<Bond>(0,1);

		// add second bond
		bonds.add(new Bond(second_p_poss,second_c_pos));

		// add glycosidic bond
		char c_pos = (child==null) ?'?' :child.getAnomericCarbon();
		bonds.add(new Bond(link_poss,c_pos));
	}

	/**
       Set the bonds forming this linkage.
	 */
	public void setLinkagePositions(Collection<Bond> _bonds) {
		bonds = new Vector<Bond>(0,1);

		for( Bond toadd : _bonds ) bonds.add(toadd.clone());
		if( bonds.size()==0 ) bonds.add(new Bond());
		if( child!=null ) setAnomericCarbon(child.getAnomericCarbon());
	}

	/**
       Set the anomeric carbon position for this linkage.
	 */
	public void setAnomericCarbon(char pos) {
		glycosidicBond().setChildPosition(pos);
	}

	/**
       Return the anomeric carbon position for this linkage.
	 */
	public char getAnomericCarbon() {
		return glycosidicBond().getChildPosition();
	}

	/**
       Return the list of all parent positions for all bonds forming
       this linkage.
	 */
	public Collection<Character> getParentPositions() {

		Vector<Character> ret = new Vector<Character>();
		for( Bond b: bonds ) {
			char[] p_poss = b.getParentPositions();
			for( int i=0; i<p_poss.length; i++ ) 
				ret.add(p_poss[i]);
		}
		return ret;
	}

	/**
       Return the list of all child positions for all bonds forming
       this linkage.
	 */
	public Collection<Character> getChildPositions() {

		Vector<Character> ret = new Vector<Character>();
		for( int i=0; i<bonds.size(); i++ )
			ret.add(bonds.get(i).getChildPosition());

		return ret;
	}


	/**
       Return the list of all parent positions for all bonds forming
       this linkage as a comma separated string.
	 */
	public String getParentPositionsString() {
		StringBuilder sb = new StringBuilder();
		for( Bond b : getBondsSorted() ) {
			if( sb.length()>0 ) sb.append(',');

			char[] p_poss = b.getParentPositions();
			for( int i=0; i<p_poss.length; i++ ) {
				if( i>0 ) sb.append('/');
				sb.append(p_poss[i]);
			}
		}
		return sb.toString();
	}

	/**
       Return the list of all child positions for all bonds forming
       this linkage as a comma separated string.
	 */
	public String getChildPositionsString() {
		StringBuilder sb = new StringBuilder();
		for( Bond b : getBondsSorted() ) {
			if( sb.length()>0 ) sb.append(',');
			sb.append(b.getChildPosition());        
		}
		return sb.toString();
	}

	/**
       Return <code>true</code> if the linkage is formed by a single
       bond with a single parent position.
	 */
	public boolean hasSingleLinkagePosition() {
		return ( bonds.size()==1 && bonds.get(0).getParentPositions().length==1 );
	}

	/**
       Return the parent position of this linkage as a single
       character.
       @return undefined if there's more than one parent position 
	 */
	public char getParentPositionsSingle() {
		if( bonds.size()==1 && bonds.get(0).getParentPositions().length==1 )
			return bonds.get(0).getParentPositions()[0];
		return '?';
	}

	/**
       Return the child position of this linkage as a single
       character.
       @return undefined if there's more than one child position 
	 */
	public char getChildPositionsSingle() {
		if( bonds.size()==1 )
			return bonds.get(0).getChildPosition();
		return '?';
	}    

	/**
       Return <code>true</code> if some bonds have uncertain parent
       positions.
	 */
	public boolean hasUncertainParentPositions() {
		for( Bond b : bonds ) {
			if( b.getParentPositions().length>1 || b.getParentPositions()[0]=='?' )
				return true;
		}
		return false;
	}

	/**
       Return <code>true</code> if some bonds have uncertain child
       positions.
	 */
	public boolean hasUncertainChildPositions() {
		for( Bond b : bonds ) {
			if( b.getChildPosition()=='?' )
				return true;
		}
		return false;
	}

	/**
       Return <code>true</code> if the two objects contains the same
       information. Propagates to the subtrees starting at the
       children residues.
	 */
	public boolean subtreeEquals(Linkage other) {
		if( other==null ) return false;

		if( this.bonds.size()!=other.bonds.size() ) return false;

		for( int i=0; i<this.bonds.size(); i++ ) {
			if( !this.bonds.get(i).equals(other.bonds.get(i)) ) return false;
			if( !this.child.subtreeEquals(other.child) ) return false;
		}
		return true;
	}    

	public boolean fuzzyMatch(Linkage other) {
		return match(other, true);
	}

	/**
       Return <code>true</code> if the two objects are
       similar. Undefined positions are treated as wildcards.
	 */
	public boolean match(Linkage other,boolean fuzzy) {
		if( other==null ) return false;
		if( this.bonds.size()!=other.bonds.size() ) return false;

		// try all bond permutations
		PermutationGenerator cg = new PermutationGenerator(other.bonds.size());
		while( cg.hasMore() ) {        
			int[] indices = cg.getNext();  
			int i=0;
			for( ; i<this.bonds.size(); i++ ) {
				if( !this.bonds.get(i).match(other.bonds.get(indices[i]),fuzzy) )
					break;
			}
			if( i==this.bonds.size() )
				return true; // all bonds matches        
		}
		return false; // no permutation matches
	}
	
	//----------------------------
	// serialization

	/**
       Create a string representation of this linkage object in IUPAC
       notation.
	 */
	public String toIupac() {
		StringBuilder sb = new StringBuilder();

		if( hasSingleLinkagePosition() && child.getAnomericState()!='?' && !hasUncertainChildPositions() && !hasUncertainParentPositions() )
			sb.append(child.getAnomericState());

		if( !hasUncertainChildPositions() && !hasUncertainParentPositions() )
			sb.append(getChildPositionsString());

		if( hasSingleLinkagePosition() ) 
			sb.append("-");
		else
			sb.append("=");

		if( !hasUncertainParentPositions() ) 
			sb.append(getParentPositionsString());

		return sb.toString();
	}
	
	public void setParentLinkageType(LinkageType _a_enumParentType) throws Exception {
		if(_a_enumParentType == null) throw new Exception("Null is not allowed");
		else this.a_enumParentType = _a_enumParentType;
	}
	
	public void setChildLinkageType(LinkageType _a_enumChildType) throws Exception {
		if(_a_enumChildType == null) throw new Exception("Null is not allowed");
		else this.a_enumChildType = _a_enumChildType;	
	}
	
	public LinkageType getParentLinkageType() {
		return this.a_enumParentType;
	}
	
	public LinkageType getChildLinkageType() {
		return this.a_enumChildType;
	}
}

