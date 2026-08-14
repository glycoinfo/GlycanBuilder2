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

import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import java.util.*;
import java.awt.*;

import org.eurocarbdb.application.glycanbuilder.linkage.Bond;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;

/*
   Objects of this class constitute the components of glycan
   molecules. Each residue object has a {linkplain ResidueType residue
   type} and hold the non-static information about a saccharide or
   substituente. A residue can have a parent and multiple children.

   @see ResidueDictionary
   @see ResidueType
   @see Glycan
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class Residue {

	private static int class_id;       
	static {
		class_id=0;    
	}

	// Unique id of this residue object
	public final int id;

	// properties
	private ResidueType type;

	private char anomeric_state;    
	private char anomeric_carbon;           
	private char chirality;           
	private char ring_size;
	private boolean alditol;
	private boolean a_bIsAldehyde;
	private boolean isComposition;
	
	// anotation
	private int antennaeID;
	private int a_iID = 0;
	private LinkedList<Residue> a_aParentsInFragment;
	
	// structure
	private Linkage parent_linkage;
	private LinkedList<Linkage> children_linkages;
	
	//annotation
	private final ArrayList<String> modifications = new ArrayList<>();
	
	// cleavage
	private String legend = null;

	private Residue cleaved_residue = null;

	// positioning
	private ResiduePlacement preferred_placement = null;
	private boolean was_sticky;
	
	// repeating util
	private Residue endRepetitionResidue;
	private Residue startRepititionResidue;

	// cyclic
	private Residue startCyclic;
	private Residue endCyclic;
	private Residue startRES;
	
	// alternative
	private Residue altStart;
	private Residue altEnd;
	
	private Rectangle centerPos;

	// ----

	/*
       Empty constructor.
	 */

	public Residue() {
		id = class_id++;

		// init
		type = new ResidueType(); // empty type

		anomeric_state = '?';
		anomeric_carbon = '?';
		chirality = '?';
		ring_size = '?';
		alditol = false;
		this.was_sticky = false;
		this.isComposition = false;
		
		parent_linkage = null;
		children_linkages = new LinkedList<>();
		this.a_aParentsInFragment = new LinkedList<>();
		
		this.antennaeID = -1;
	}

	/*
       Create a new residue of a specific type.
	 */

	public Residue(ResidueType _type) {
		id = class_id++;

		// init
		type = _type;

		anomeric_state = '?';
		anomeric_carbon = type.getAnomericCarbon();
		chirality = type.getChirality();
		ring_size = type.getRingSize();
		alditol = false;
		this.was_sticky =
				_type.getName().contains("Fuc") || _type.getName().contains("Xyl");
		this.isComposition = false;
		
		parent_linkage = null;
		children_linkages = new LinkedList<>();
		this.a_aParentsInFragment = new LinkedList<>();
		
		this.antennaeID = -1;
	}

	/*
       Create a new residue of a specific type and set the additional
       information about the saccharide chemistry.
	 */

	public Residue(ResidueType _type, char _anomeric_state, char _anomeric_carbon, char _chirality, char _ring_size) {
		id = class_id++;

		// init
		type = _type;

		anomeric_state = _anomeric_state;
		anomeric_carbon = _anomeric_carbon;
		chirality = _chirality;
		ring_size = _ring_size;
		this.was_sticky =
				_type.getName().contains("Fuc") || _type.getName().contains("Xyl");
		
		parent_linkage = null;
		children_linkages = new LinkedList<>();
		this.a_aParentsInFragment = new LinkedList<>();
		
		this.antennaeID = -1;
	}

	// -------------
	// properties access	
	public void setAntennaeID(int _antennaeID) {
		this.antennaeID = _antennaeID;
	}
	
	public void setID (int _a_iID) {
		this.a_iID = _a_iID;
	}
	
	public int getID () {
		return this.a_iID;
	}
	
	public int getAntennaeID() {
		return this.antennaeID;
	}
	
	public void addModification(String a_sMOD) {
		this.modifications.add(a_sMOD);
	}
	
	public ArrayList<String> getModifications() {
		return this.modifications;
	}
	
	public void isComposition(boolean _isComposition) {
		this.isComposition = _isComposition;
	}
	
	public boolean isComposition() {
		return this.isComposition;
	}
	
	public void setStartResidue(Residue a_oStartRES) {
		this.startRES = a_oStartRES;
	}
	
	public Residue getStartResidue() {
		return this.startRES;
	}

	public Residue getEndResidue() {
		return this.endCyclic;
	}
	
	/*
       Return the type name.
       @see ResidueType#getName
	 */
	public String getTypeName() {
		return type.getName();
	}

	/*
       Return the residue name.
       @see ResidueType#getResidueName
	 */
	public String getResidueName() {
		return type.getResidueName();
	}

	/*
       Return the residue type.
	 */   
	public ResidueType getType() {
		return type;
	}

	/*
       Set the residue type.
	 */
	public void setType(ResidueType _type) {
		type = _type;
	}

	/*
	 * Set parent residue list
	 */
	public void setParentListinAntenna(LinkedList<Residue> _parents) {
		this.a_aParentsInFragment = _parents;
	}
	
	/*
	 * Set backbone name in fazzy monosaccharide
	 */
	/*public void setMotifName(String _motif) {
		this.str_baseShape = _motif;
	}*/
	
	/*
	 	Set parent residue in antenna
	 */
	public void addParentOfFragment(Residue _res) {
		if(this.a_aParentsInFragment.contains(_res)) return;
		if(_res != null)
			this.a_aParentsInFragment.add(_res);
	}
	
	/*
	 	Return the parent antenna list
	 */
	public LinkedList<Residue> getParentsOfFragment() {
		return this.a_aParentsInFragment;
	}
	
	/*
       Return the cleavage type.
       see ResidueType#getCleavageType()
	 */ 
	public String getCleavageType() {
		return type.getCleavageType();
	}

	/*
       Return the anomeric state.
	 */
	public char getAnomericState() {
		return anomeric_state;
	}

	/*
       Set the anomeric state as [a - alpha, b - beta, ?  -
       unspecified].
	 */
	public void setAnomericState(char _anomeric_state) {
		anomeric_state = _anomeric_state;
	}

	/*
       Return <code>true</code> if the anomeric state is specified.
	 */
	public boolean hasAnomericState() {
		return anomeric_state!='?';
	} 

	/*
       Return the anomeric carbon position.
	 */
	public char getAnomericCarbon() {
		return anomeric_carbon;
	}

	/*
       Set the anomeric carbon position. 
	 */
	public void setAnomericCarbon(char _anomeric_carbon) {
		anomeric_carbon = _anomeric_carbon;
		if( parent_linkage!=null )
			parent_linkage.setAnomericCarbon(anomeric_carbon);
	}

	/*
       Return <code>true</code> if the anomeric carbon position is specified.
	 */
	public boolean hasAnomericCarbon() {
		return anomeric_carbon!='?';
	} 

	/*
       Return the chirality configuration.
	 */
	public char getChirality() {
		return chirality;
	}

	/*
       Set the chirality configuration as [D - dextro, L - levo, ? -
       unspecified].
	 */
	public void setChirality(char _chirality) {
		chirality = _chirality;
	}

	/*
       Return <code>true</code> if the chirality configuration is specified.
	 */
	public boolean hasChirality() {
		return chirality!='?';
	} 

	/*
       Return the ring size.
	 */
	public char getRingSize() {
		return ring_size;
	}

	/*
       Set the ring size as [p - pyranose, f - furanose, o - alditol, a - open
       chain, ? - unspecified].

       The two acyclic forms are also carried as flags, which is what the
       exporters read: a Glc flagged alditol writes [h2122h] and one flagged
       aldehyde writes [o2122h], where the ring letter alone reaches neither.
       Setting them here keeps the one fact in one state, so that a caller
       which sets only the ring size - the GWS parser, and any front end that
       is not GlycanCanvas - does not leave a residue that says it is acyclic
       and writes as a ring.
	 */
	public void setRingSize(char _ring_size) {
		ring_size = _ring_size;
		alditol = (_ring_size == 'o');
		a_bIsAldehyde = (_ring_size == 'a');
	}

	/*
       Return <code>true</code> if the ring size is specified.
	 */
	public boolean hasRingSize() {
		return ring_size!='?';
	} 

	/*
       Set to <code>true</code> if this residue is a alditol.
	 */
	public void setAlditol(boolean a) {
		alditol = a;
	}

	/*
       Return <code>true</code> if this residue is a alditol.
	 */
	public boolean isAlditol() {
		return alditol;
	}

	public void setAldehyde(boolean a_bIsAldehyde) {
		this.a_bIsAldehyde = a_bIsAldehyde;
	}
	
	public boolean isAldehyde() {
		return this.a_bIsAldehyde;
	}
	
	/*
       Return the maxinum number of available linkage position.
       @see ResidueType#getMaxLinkages
	 */
	public int getMaxLinkages() {
		return type.getMaxLinkages();
	}

	/*
       Return <code>true</code> if this residue is a saccharide.
       @see ResidueType#isSaccharide
	 */
	public boolean isSaccharide() {
		return type.isSaccharide();
	}

	/*
       Return <code>true</code> if this residue is a substituent.
       @see ResidueType#isSubstituent
	 */
	public boolean isSubstituent() {
		return type.isSubstituent();
	}

	/*
	Return <core>true</code> id this residue is a bridge.
	@see ResidueType#isBridge
	 */
	public boolean isBridge () {
		return type.isBridge();
	}

	/*
       Return <code>true</code> if this residue can be cleaved off the
       structure.
       @see ResidueType#isCleavable
	 */
	public boolean isCleavable() {
		return type.isCleavable();
	}

	/*
       Return <code>true</code> if this residue is of a specialy type.
       @see ResidueType#isSpecial
	 */
	public boolean isSpecial() {
		return type.isSpecial();
	}

	/*
       Return <code>true</code> if this residue is labile during
       fragmentation.
       @see ResidueType#isLabile
	 */
	public boolean isLabile() {
		return (type.isLabile() && !hasChildren());
	}

	/*
       Return <code>true</code> if this residuecan have a parent.
       @see ResidueType#canHaveParent
	 */
	public boolean canHaveParent() {
		return type.canHaveParent();
	}

	/*
       Return <code>true</code> if this residue can have children.
       @see ResidueType#canHaveChildren
	 */
	public boolean canHaveChildren() {
		return type.canHaveChildren();
	}

	/*
       Return <code>true</code> if this residue can be used as a
       reducing end marker.
       @see ResidueType#canBeReducingEnd
	 */
	public boolean canBeReducingEnd() {
		return type.canBeReducingEnd();
	}

	/*
       Return <code>true</code> if this residue represent a free
       reducing end.
       @see ResidueType#isFreeReducingEnd
	 */
	public boolean isFreeReducingEnd() {
		return type.isFreeReducingEnd();
	}

	/*
       Return <code>true</code> if this residue represent reducing end
       marker.
	 */
	public boolean isReducingEnd() {
		return (parent_linkage==null && type.canBeReducingEnd());
	}

	/*
       Return <code>true</code> if this residue represent the beginning
       or the end of a repeat block.
       @see ResidueType#isRepetition
	 */
	public boolean isRepetition() {
		return type.isRepetition();
	}

	/*
       Return <code>true</code> if this residue represent the
       beginning of a repeat block.
       @see ResidueType#isStartRepetition
	 */
	public boolean isStartRepetition() {
		return type.isStartRepetition();
	}

	/*
       Return <code>true</code> if this residue represent the end of a repeat block.
       @see ResidueType#isEndRepetition
	 */
	public boolean isEndRepetition() {
		return type.isEndRepetition();
	}

	public boolean isEndCyclic() {
		return type.isEndCyclic();
	}
	
	public boolean isStartCyclic() {
		return type.isStartCyclic();
	}
	
	/*
       Return the lower bound of a repeat block range, applies only to
       end repetition residues.
       @return -1 if the type is not an end repetition.
       @see ResidueType#getMinRepetitions
	 */
	public int getMinRepetitions() {
		return type.getMinRepetitions();
	}

	/*
       Set the lower bound of a repeat block range, applies only to
       end repetition types.
       @see ResidueType#setMinRepetitions
	 */
	public void setMinRepetitions(String min) {
		type.setMinRepetitions(min);
	}

	/*
       Return the upper bound of a repeat block range, applies only to
       end repetition residues.
       @return -1 if the type is not an end repetition.
       @see ResidueType#getMaxRepetitions
	 */
	public int getMaxRepetitions() {
		return type.getMaxRepetitions();
	}

	/*
       Set the upper bound of a repeat block range, applies only to
       end repetition types.
       @see ResidueType#setMaxRepetitions
	 */
	public void setMaxRepetitions(String max) {
		type.setMaxRepetitions(max);
	}    

	/*
       Return <code>true</code> if this residue represent a bracket node.
       @see ResidueType#isBracket
	 */
	public boolean isBracket() {
		return type.isBracket();
	}

	/**
       The description the WURCS importer gives the marker it hangs off a
       composition to record that no linkages are known.
       @see #isCompositionMarker
	 */
	public static final String NO_GLYCOSIDIC_LINKAGES = "no glycosidic linkages";

	/**
       Return <code>true</code> if this residue is the label a composition
       carries to say that no linkages among its members are known, rather
       than a member of the composition itself.
       <p>
       {@code WURCSSequence2ToGlycan} attaches it when reading a composition.
       It is not a residue of the glycan: it weighs nothing, it is not one of
       the N members whose N-1 implicit bonds a composition's mass accounts
       for, it is not drawn, and no notation has a spelling for it. Everything
       that walks a structure has to leave it out, and each place that forgot
       has been a fault of its own - the mass read one water and one
       derivatization group light, and the GWS written for a composition could
       not be read back.
	 */
	public boolean isCompositionMarker() {
		return type != null && NO_GLYCOSIDIC_LINKAGES.equals(type.getDescription());
	}

	/*
       Return <code>true</code> if this residue is contained in a
       terminal structure linked to a bracket residue.
	 */
	public boolean isAntenna() {
		if( parent_linkage==null ) 
			return false;
		if( parent_linkage.getParentResidue().isBracket() )
			return true;
		return parent_linkage.getParentResidue().isAntenna();
	}

	/*
       Return <code>true</code> if this residue represent an attach point.
       @see ResidueType#isAttachPoint
	 */
	public boolean isAttachPoint() {
		return type.isAttachPoint();
	}

	/*
       Return <code>true</code> if this residue represent a glycosidic
       cleavage marker.
       @see ResidueType#isGlycosidicCleavage
	 */
	public boolean isGlycosidicCleavage() {
		return type.isGlycosidicCleavage();
	}

	/*
       Return <code>true</code> if this residue represent a cleavage marker.
       @see ResidueType#isCleavage
	 */
	public boolean isCleavage() {
		return type.isCleavage();
	}

	/*
       Return <code>true</code> if this residue represent a labile
       cleavage marker.
       @see ResidueType#isLCleavage
	 */
	public boolean isLCleavage() {
		return type.isLCleavage();
	}

	/*
       Return <code>true</code> if this residue represent a ring
       fragment type.
       @see ResidueType#isRingFragment
	 */
	public boolean isRingFragment() {
		return type.isRingFragment();
	}

	/*
       Return the residue that was present at this position before
       cleavage.
	 */       
	public Residue getCleavedResidue() {
		return cleaved_residue;
	}

	/*
       Set the residue that was present at this position before
       cleavage.
	 */ 
	public void setCleavedResidue(Residue _cleaved_residue) {
		cleaved_residue = _cleaved_residue;
	}

	/* 
       Return <code>true</code> if any one of the connected residues
       (parent and children) is a glycosidic cleavage.
	 */
	public boolean hasGlycosidicCleavages() {
		for(Linkage l : children_linkages) {
			if( l.getChildResidue().isGlycosidicCleavage() )
				return true;
		}
		Residue parent = getParent();
		return (parent!=null && parent.isGlycosidicCleavage());
	}

	/* 
       Return <code>true</code> if any one of the connected residues
       (parent and children) is a ring fragment
	 */
	public boolean hasRingFragments() {
		for(Linkage l : children_linkages) {
			if( l.getChildResidue().isRingFragment() )
				return true;
		}
		Residue parent = getParent();
		return (parent!=null && parent.isRingFragment());
	}

	/* 
       Return <code>true</code> if any one of the children is a
       saccharide
	 */
	public boolean hasSaccharideChildren() {
		for(Linkage l : children_linkages) {
			if( l.getChildResidue().isSaccharide() )
				return true;
		}
		return false;
	}

	/*
       Return <code>true</code> if the residue has a preferred
       placement position for displaying.
       @see GlycanRendererAWT
       @see BBoxManager
	 */
	public boolean hasPreferredPlacement() {
		return (preferred_placement!=null);
	}

	/*
       Clear the preferred placement position for displaying.
       @see GlycanRendererAWT
       @see BBoxManager
	 */
	public void resetPreferredPlacement() {
		preferred_placement = null;
	}

	/*
       Set the preferred placement position for displaying.
       @see GlycanRendererAWT
       @see BBoxManager
	 */
	public void setPreferredPlacement(ResiduePlacement new_place) {
		preferred_placement = new_place;
	}

	/*
       Return the preferred placement position for displaying.
       @see GlycanRendererAWT
       @see BBoxManager
	 */
	public ResiduePlacement getPreferredPlacement() {
		return preferred_placement;
	}   

	public void setWasSticky(boolean flag) {
		was_sticky = flag;
	}

	public boolean getWasSticky() {
		return was_sticky;
	}

	// -------------
	// structure access

	/*
       Set the {@link Linkage linkage} to the parent.
	 */
	public void setParentLinkage(Linkage _parent_linkage) {
		parent_linkage = _parent_linkage;
	}

	/*
       Return the {@link Linkage linkage} to the parent.
	 */
	public Linkage getParentLinkage() {
		return parent_linkage;
	}

	/*
       Return the parent residue.
	 */
	public Residue getParent() {
		if( parent_linkage!=null )
			return parent_linkage.getParentResidue();
		return null;
	}
	
	/*
	  Return the saccharide parent residue;
	 */
	public Residue getSaccharideParent() {
		if(this.parent_linkage!= null) {
			Residue tmp = this.parent_linkage.getParentResidue();
			if( (tmp.isStartRepetition() && tmp.getParent().isReducingEnd()) ||	
				(tmp.isStartRepetition() && tmp.getParent().isStartCyclic()) || tmp.isReducingEnd()) 
				return parent_linkage.getChildResidue();
			if(tmp.isBracket()) return this.parent_linkage.getChildResidue();	
			while(!tmp.isSaccharide()) tmp = tmp.getParent();
			return tmp;
		}
		return null;
	}

	/*
       Return the children {@link Linkage linkages}.
	 */
	public LinkedList<Linkage> getChildrenLinkages() {
		return children_linkages;
	}

	/*
	 * Return motif name
	 */
	/*public String getMotifName() {
		return this.str_baseShape;
	}*/
	
	/*

	 */
	public void sortChildLinkage() {
		if(this.children_linkages.size() == 1) return;		

		LinkedList<Linkage> lst_sorted = new LinkedList<>();
		
		String[] lst_parentPos = new String[this.children_linkages.size()];
		for(Linkage lin : this.children_linkages) 
			lst_parentPos[this.children_linkages.indexOf(lin)] = lin.getParentPositionsString();

		Arrays.sort(lst_parentPos);
		
		for(String pos : lst_parentPos) {
			for(Linkage lin : new ArrayList<Linkage>(this.children_linkages)) {
				if(!pos.equals(lin.getParentPositionsString())) continue;
				
				if(pos.equals("?")) lst_sorted.addFirst(lin);
				else lst_sorted.addLast(lin);
				this.children_linkages.remove(lin);
			}
		}
		
		this.children_linkages = lst_sorted;
	}
	
	/*
       Return an iterator over the children {@link Linkage linkages}.
	 */
	public Iterator<Linkage> iterator() {
		return children_linkages.iterator();
	}   

	/*
       Return the first children {@link Linkage linkage} or
       <code>null</code> if none are present.
	 */
	public Linkage firstLinkage() {
		return ( children_linkages.size()>0 ) ?children_linkages.get(0) :null;
	}

	/*
       Return the first children or <code>null</code> if none are
       present.
	 */
	public Residue firstChild() {
		return ( children_linkages.size()>0 ) ?children_linkages.get(0).getChildResidue() :null;
	}

	/*
       Return the last children or <code>null</code> if none are
       present.
	 */
	public Residue lastChild() {
		return ( children_linkages.size()>0 ) ?children_linkages.get(children_linkages.size()-1).getChildResidue() :null;
	}


	/*
       Return the first saccharide children or <code>null</code> if
       none are present.
	 */
	public Residue firstSaccharideChild() {
		for( Linkage l : children_linkages ) 
			if( l.getChildResidue().isSaccharide() )
				return l.getChildResidue();
				return null;
	}

	/*
       Return the child with index <code>ind</code>.
	 */
	public Residue getChildAt(int ind) {
		return children_linkages.get(ind).getChildResidue();
	}


	/*
       Return the child {@link Linkage linkage} with index <code>ind</code>.
	 */
	public Linkage getLinkageAt(int ind) {
		return children_linkages.get(ind);
	}

	/*
       Return the index of the <code>child</code> residue or -1 if it
       is not in the children collection.
	 */
	public int indexOf(Residue child) {
		if( child==null )
			return -1;
		for(int i=0; i<children_linkages.size(); i++ )
			if( getChildAt(i)==child )
				return i;
		return -1;
	}                        

	/*
       Return <code>true</code> if the residue has a parent.
	 */
	public boolean hasParent() {
		return (parent_linkage!=null && parent_linkage.getParentResidue()!=null);
	}

	public boolean hasSaccharideParent() {
		return this.getSaccharideParent() != null;
	}
	
	/*
       Return <code>true</code> if the residue has at least one child.
	 */
	public boolean hasChildren() {
		return !children_linkages.isEmpty();
	}

	/*
       Return the number of children.
	 */
	public int getNoChildren() {
		return children_linkages.size();
	}
	
	/*
	 	Return the number of parent for antena root
	 */
	public int getNoParent(Glycan a_objGlycan) {
		if(!this.parent_linkage.getChildResidue().isAntenna()) return 0;	
		return this.countChildAtDepth(a_objGlycan.getRoot(), 0);
	}
	
	private int countChildAtDepth(Residue root, int count) {
		for(Linkage a_objLIN : root.getChildrenLinkages()) {
			count++;
			if(a_objLIN.getChildResidue().hasChildren())
				count = this.countChildAtDepth(a_objLIN.getChildResidue(), count);
		}
		
		return count;
	}
	
	/*
       Return the number of saccharide children.
	 */
	public int getNoSaccharideChildren() {
		int count = 0;
		for( Linkage l : children_linkages ) {
			if( l.getChildResidue().isSaccharide() )
				count++;
		}
		return count;
	}

	/*
       Return the number of connected residues, comprising the parent.
	 */
	public int getNoLinkages() {
		if( parent_linkage==null )
			return children_linkages.size();
		return (children_linkages.size()+1);
	}    

	/*
       Return the total number of chemical bonds with all the
       connected residues, comprising the parent.
	 */
	public int getNoBonds() {
		int ret = 0;
		if( parent_linkage!=null )
			ret += parent_linkage.getNoBonds();
		for( Linkage l : children_linkages )
			ret += l.getNoBonds();
				return ret;    
	}

	protected int countLeftBrothers(Residue sub_root, int depth) {
		Residue parent = getParent();
		if( parent==null || this==sub_root )
			return 0;

		int ret = 0;
		int ind = parent.indexOf(this);
		for( int i=0; i<ind; i++ ) 
			ret += parent.getChildAt(i).countLeafs(depth);    

		return ret + parent.countLeftBrothers(sub_root,depth+1);
	}

	protected int countRightBrothers(Residue sub_root, int depth) {
		Residue parent = getParent();
		if( parent==null || this==sub_root )
			return 0;

		int ret = 0;
		int ind = parent.indexOf(this);
		for( int i=ind+1; i<parent.getNoChildren(); i++ ) 
			ret += parent.getChildAt(i).countLeafs(depth);    

		ret += parent.countRightBrothers(sub_root,depth+1);
		return ret;
	}

	protected int countLeafs(int max_depth) {
		if( max_depth==0 || getNoChildren()==0 )
			return 1;

		int ret=0;
		for( Iterator<Linkage> i=children_linkages.iterator(); i.hasNext(); ) 
			ret += i.next().getChildResidue().countLeafs(max_depth-1);
		return ret;
	}    

	protected boolean hasLinkedParent() {
		return( parent_linkage!=null && parent_linkage.getParentResidue()!=null && 
				(parent_linkage.getParentResidue().getTypeName().equals("freeEnd") ||  
						parent_linkage.getParentResidue().getTypeName().equals("redEnd")) );
	}

	/*
       Return <code>true</code> if all linkage position are valid and
       defined.
	 */
	public boolean checkLinkages() {
		if( isBracket() ) {
			// check that all children have linkage position set
			for( Linkage l : children_linkages ) {
				if( l.hasUncertainParentPositions() )
					return false;
			}
		}
		else if( isSaccharide() ) {

			// get linkages
			Vector<Character> all_pos = new Vector<Character>(0,1);    
			if( hasLinkedParent() ) {
				if( parent_linkage.hasUncertainChildPositions() )
					return false;
				all_pos.addAll(parent_linkage.getChildPositions());
			}

			for( Linkage l : children_linkages ) {
				if( l.hasUncertainParentPositions() )
					return false;
				all_pos.addAll(l.getParentPositions());
			}

			// check for conflicts
			HashSet<Character> set = new HashSet<Character>();
			for( Character c : all_pos ) {
				char pos = c.charValue();        
				if( set.contains(pos) ) 
					return false;
				set.add(pos);
			}

			// check linkage positions
			for( Character pos : all_pos ) 
				if( !type.isValidPosition(pos.charValue()) ) 
					return false;        
		}

		return true;
	}

	/*
       Return <code>true</code> if the linkage position are valid and
       defined for all the residues in the subtree.
	 */
	public boolean checkLinkagesSubtree() {

		// check current
		if( !checkLinkages() )
			return false;

		// check children
		for( Linkage l : children_linkages ) {
			if( !l.getChildResidue().checkLinkagesSubtree() )
				return false;
		}

		return true;
	}

	/*
       Return <code>true</code> if all linkage position are defined.
	 */

	public boolean isFullySpecified() {
		if( isBracket() )
			return false;

		if( hasLinkedParent() && parent_linkage.hasUncertainChildPositions() )
			return false;

		for( Linkage l : children_linkages ) {
			if( l.hasUncertainParentPositions() )
				return false;
		}

		return true;
	}

	/*
       Return <code>true</code> if the linkage position are defined
       for all the residues in the subtree.
	 */
	public boolean isFullySpecifiedSubtree() {

		// check current
		if( !isFullySpecified() )
			return false;

		// check children
		for( Iterator<Linkage> i=children_linkages.iterator(); i.hasNext(); ) {
			if( !i.next().getChildResidue().isFullySpecified() )
				return false;
		}

		return true;
	}

	//----------
	// structure navigation

	private boolean fuzzyMatch(char c1, char c2) {
		return match(c1, c2, true);
	}

	private boolean match(char c1, char c2, boolean fuzzy) {
		return (c1==c2 || (fuzzy && (c1=='?' || c2=='?' )));        
	}

	private boolean fuzzyMatch(ResidueType rt1, ResidueType rt2) {
		return match(rt1,rt2,true);
	}

	private boolean match(ResidueType rt1, ResidueType rt2, boolean fuzzy) {
		return rt1.getName().equals(rt2.getName()) || ( fuzzy &&(
				rt1.getCompositionClass().equals(rt2.getName()) ||
				rt1.getName().equals(rt2.getCompositionClass())));    
	}

	public boolean fuzzyMatch(Residue other) {
		return match(other,true);
	}

	/*
       Return <code>true</code> if the two residues match, considering
       undefined stereochemistry configurations and residue super
       classes as wildcards.
	 */
	public boolean match(Residue other, boolean fuzzy) {
		if( other==null )
			return false;

		if( !match(this.getType(),other.getType(),fuzzy) )
			return false;
		if( !match(this.anomeric_state,other.anomeric_state,fuzzy) )
			return false;
		if( !match(this.anomeric_carbon,other.anomeric_carbon,fuzzy) )
			return false;
		return match(this.chirality, other.chirality, fuzzy);
		//if( !fuzzyMatch(this.ring_size,other.ring_size) )
		//return false;
	}


	/*
       Return <code>true</code> if the subtree rooted at this residue
       contains one residue that matches <code>node</code>
	 */
	public boolean subtreeContains(Residue node) {
		if( this==node )
			return true;
		for( Linkage l : children_linkages ) {
			if( l.getChildResidue().subtreeContains(node) )
				return true;
		}
		return false;
	}

	/*
       Return <code>true</code> if the two residues match exactly.
	 */
	public boolean typeEquals(Residue other) {
		if( other==null )
			return false;

		if( !this.getTypeName().equals(other.getTypeName()) )
			return false;
		if( this.anomeric_state!=other.anomeric_state )
			return false;
		if( this.anomeric_carbon!=other.anomeric_carbon )
			return false;
		if( this.chirality!=other.chirality )
			return false;
		return this.ring_size == other.ring_size;
	}

	/*
       Return <code>true</code> if the subtree rooted at this residue
       contains one residue that matches <code>node</code> exactly.
	 */
	public boolean subtreeEquals(Residue other) {
		if( !this.typeEquals(other) )
			return false;

		if( this.getNoChildren()!=other.getNoChildren() )
			return false;

		for( int i=0; i<this.children_linkages.size(); i++ ) {
			if( !this.getLinkageAt(i).subtreeEquals(other.getLinkageAt(i)) )
				return false;
		}
		return true;
	}

	/*
       Return the root of the glycan tree to which this residue belongs.
	 */
	public Residue getTreeRoot() {
		if( parent_linkage==null )
			return this;
		return parent_linkage.getParentResidue().getTreeRoot();
	}

	/*
       Return <code>true</code> if this residue is part of a repeat
       block.
	 */
	public boolean isInRepetition() {
		return (findStartRepetition()!=null);
	}

	/*
       Return the start of the repeat block to which this residue
       belong or <code>null</code> otherwise.
	 */
	public Residue findStartRepetition() {    
		return findStartRepetition(false);
	}

	private Residue findStartRepetition(boolean stop_at_end) {    
		if( this.isStartRepetition() )
			return this;
		if( stop_at_end && this.isEndRepetition() )
			return null;
		if( this.getParent()==null )
			return null;
		return this.getParent().findStartRepetition(true);
	}

	/*
       Return the end of the repeat block to which this residue
       belong or <code>null</code> otherwise.
	 */
	public Residue findEndRepetition() {
		return findEndRepetition(false);
	}

	private Residue findEndRepetition(boolean stop_at_start) {
		if( this.isEndRepetition() )
			return this;
		if( stop_at_start && this.isStartRepetition() )
			return null;
		for( Linkage l : children_linkages ) {
			Residue ret = l.getChildResidue().findEndRepetition(true);
			if( ret!=null )
				return ret;
		}
		return null;
	}

	//---------------
	// structure modification


	/*
       Add a child residue.
       @return <code>true</code> if the operation was successful
	 */
	public boolean addChild(Residue child) {
		return addChild(child,Bond.single());
	}

	/*
       Return <code>true</code> if <code>child</code> can be added to
       this residue.
	 */
	public boolean canAddChild(Residue child) {
		return canAddChild(child,Bond.single());
	}

	/*
       Add a child residue at a specific linkage position.
       @return <code>true</code> if the operation was successful
	 */
	public boolean addChild(Residue child, char parent_link_pos) {
		return addChild(child,Bond.single(parent_link_pos));
	}

	/*
       Return <code>true</code> if <code>child</code> can be added to
       this residue at a specific linkage position.
	 */
	public boolean canAddChild(Residue child, char parent_link_pos) {
		return canAddChild(child,Bond.single(parent_link_pos));
	}

	/*
       Add a child residue with specific bonds.
       @return <code>true</code> if the operation was successful
	 */
	public boolean addChild(Residue child, Collection<Bond> bonds) {
		return attachChild(child,bonds,true);
	}

	/**
	 * Attach a child that is being copied from a structure that already holds it.
	 *
	 * <p>A copy makes no new claim about a molecule, so the rules that decide what <em>may</em> be
	 * attached do not apply to it: the question was settled when the original was made. Copying through
	 * {@link #addChild} instead meant that a structure the rules would now refuse lost a residue on the
	 * way through - silently, since a clone has nowhere to report - and {@code Glycan.clone()} is
	 * copy-and-paste, undo and redo (#211).
	 *
	 * <p>Measured on a Glc carrying two substituents at position 2: 1.35.2 and 1.36.0 copied both,
	 * 1.37.0 and 1.38.0 copied one and said nothing.
	 *
	 * <p>The distinction is between validating a change and reproducing a fact. Reading a file has
	 * always been on the second side of it - a document containing such a structure still opens - and
	 * copying belongs there too.
	 */
	protected boolean copyChild(Residue child, Collection<Bond> bonds) {
		return attachChild(child,bonds,false);
	}

	/**
	 * @param checkPosition
	 *            whether the position rules apply. False when copying, where the structure already
	 *            exists and nothing is being claimed.
	 */
	private boolean attachChild(Residue child, Collection<Bond> bonds, boolean checkPosition) {
		if( child==null )
			return false;

		// remove attachment
		if( child.isAttachPoint() ) {
			if( !child.hasChildren() )
				return false;

			Linkage link = child.children_linkages.get(0);
			return attachChild(link.getChildResidue(),link.getBonds(),checkPosition);
		}

		//TODO: Investigate this further, this stop structures with repeat units from being copied 
		// cannot add to a repetition if there's already another child

		if(this.isStartRepetition() && this.getChildrenLinkages().size()>0)
			return false;

		//    if( this.isRepetition() && this.getNoChildren()!=0 )
		//        return false;

		// cannot add a reducing end
		if( child.isReducingEnd() && !child.canHaveParent() )
			return this.attachChild(child.firstChild(),bonds,checkPosition);

		// add labile back to lcleavage
		if( isLCleavage() && cleaved_residue.getTypeName().equals(child.getTypeName()) ) {
			this.copyResidue(cleaved_residue);
			return true;
		}

		// a carbon carries one glycosidic bond, so a position another child already occupies is not
		// one this child can be given (#34). Checked here as well as in canAddChild because this
		// does not consult it - the two have grown apart, and adding is the path that makes the
		// structure
		if( checkPosition && positionIsNotAvailable(child,bonds) )
			return false;

		// check for available space
		//if( (getNoLinkages()+bonds.size())<getMaxLinkages() )

		Linkage link = new Linkage(this,child,bonds);
		link.getBonds().get(0).setProbabilityHigh(bonds.iterator().next().getProbabilityHigh());
		link.getBonds().get(0).setProbabilityLow(bonds.iterator().next().getProbabilityLow());

		if(child.getParentLinkage() != null) {
				//link.setChildResidue(child.getParentLinkage().getChildResidue());
				//link.setAnomericCarbon(child.getParentLinkage().getAnomericCarbon());
			try {
				link.setParentLinkageType(child.getParentLinkage().getParentLinkageType());
				link.setChildLinkageType(child.getParentLinkage().getChildLinkageType());
			} catch (Exception e) {
				LogUtils.report(e);
			}
		}
		
		children_linkages.add(link);
		child.parent_linkage = link;
		
		return true;
	}

	/*
       Return <code>true</code> if <code>child</code> can be added to
       this residue with specific bonds.
	 */
	public boolean canAddChild(Residue child, Collection<Bond> bonds) {
		if( child==null )
			return false;

		// remove attachment
		if( child.isAttachPoint() ) {
			if( !child.hasChildren() )
				return false;

			Linkage link = child.children_linkages.get(0);     
			return canAddChild(link.getChildResidue(),link.getBonds());
		}

		// cannot add to a repetition if there's already another child
		if( this.isRepetition() && this.getNoChildren()!=0 )
			return false;

		// cannot add a reducing end
		if( child.isReducingEnd() && !child.canHaveParent() )
			return this.canAddChild(child.firstChild(),bonds);

		// add labile back to lcleavage
		if( isLCleavage() && cleaved_residue.getTypeName().equals(child.getTypeName()) )
			return true;

		// a carbon carries one glycosidic bond, so a position another child already occupies is not
		// one this child can be given (#34)
		if( positionIsNotAvailable(child,bonds) )
			return false;

		// check for available space
		//return ( (getNoLinkages()+bonds.size())<getMaxLinkages() );
		return true;
	}

	/**
	 * Whether any of these bonds would put a child where another child already is.
	 *
	 * <p>The position list offered when a linkage is edited was built from what the residue type
	 * allows without asking what its other children had taken, so the same position could be given
	 * twice - a Man with two branches both at 4, which is not a molecule. It draws, and the WURCS
	 * export then writes nothing at all, which is how it was usually noticed.
	 *
	 * <p>Only a position that is actually stated counts. An unknown position - {@code '?'}, which is
	 * most of what a structure read from a database carries - says nothing about what is where, so
	 * two of those do not conflict; deciding they did would refuse structures that are perfectly
	 * ordinary. A bond that names several positions at once is the same case: it is a statement of
	 * "one of these", not of any one of them.
	 *
	 * @param child The residue about to be added, which is not counted against itself.
	 * @param bonds The bonds it would be added by.
	 * @return Returns whether one of them names a position another child already has.
	 */
	/**
	 * The positions this residue can take an ordinary glycosidic bond at, as it stands.
	 *
	 * <p>Everything that decides this in one place, so that a structure built by any route obeys the
	 * same rules. It had been in three: the residue type's list, which only the linkage dialog asked;
	 * the ring and anomeric rules, which only that dialog knew; and what a sibling had taken, which
	 * only the model knew. A structure drawn through the dialog therefore obeyed rules the model did
	 * not enforce, and one built any other way obeyed almost none - which is how a Man came to carry
	 * two branches at position 4 (#34).
	 *
	 * <p>What is taken into account, and why each is not the same as the others:
	 *
	 * <ul>
	 *   <li><b>The residue type's list.</b> It already knows what the sugar is made of - GlcNAc omits
	 *       2 for its N-acetyl, Xyl has no 6 - and whether a built-in substituent closes its position
	 *       is a chemical judgement made per residue rather than a rule: GlcA keeps 6 open, because a
	 *       carboxyl can be esterified. 47 of the 134 types declare no list at all, mostly reducing
	 *       ends and substituents; no list means no constraint, not no positions.</li>
	 *   <li><b>The ring.</b> Its oxygen occupies a position and that position takes no glycosidic
	 *       bond: 5 for a pyranose closing from 1, 6 from 2; 4 for a furanose from 1, 5 from 2. An
	 *       open chain closes nothing. The ring atom is not always oxygen - it can be nitrogen - but
	 *       that is a fact about what the residue is, not about what can hang off it.</li>
	 *   <li><b>An alditol</b> has no ring and no anomeric centre, so its 1 is an ordinary hydroxyl and
	 *       does take a bond, which the type's list does not say because in a ring it does not.</li>
	 *   <li><b>What a sibling holds.</b> A carbon carries one glycosidic bond, and a methyl at 4 is
	 *       the same claim on the same atom as a branch at 4.</li>
	 * </ul>
	 *
	 * <p>This describes ordinary glycosidic bonds. A <b>bridge</b> is not one and is not bound by it -
	 * an anhydro attaches at the anomeric carbon, which no type's list offers, and 1,6-anhydro is a
	 * real structure. See {@link #acceptsPosition}.
	 *
	 * @return Returns the positions, in order, without {@code '?'} - which is always acceptable and
	 *         is not a position.
	 */
	public char[] availableLinkagePositions() {
		StringBuilder available = new StringBuilder();

		char[] fromType = (type==null) ? new char[0] : type.getLinkagePositions();
		if( fromType.length==0 ) {
			// No list is no constraint. Reducing ends and substituents mostly have none.
			for( char position='1'; position<='9'; position++ )
				available.append(position);
			available.append('N');
		}
		else
			available.append(fromType);

		// an alditol has no ring and no anomeric centre, so position 1 is an ordinary hydroxyl
		if( isAlditol() && available.indexOf("1")<0 )
			available.insert(0,'1');

		char closedByRing = ringPosition();
		if( closedByRing!='\0' ) {
			int at = available.indexOf(String.valueOf(closedByRing));
			if( at>=0 ) available.deleteCharAt(at);
		}

		for( Linkage taken : children_linkages ) {
			for( Bond bond : taken.getBonds() ) {
				char[] positions = bond.getParentPositions();
				if( positions==null || positions.length!=1 || positions[0]=='?' )
					continue;

				int at = available.indexOf(String.valueOf(positions[0]));
				if( at>=0 ) available.deleteCharAt(at);
			}
		}

		char[] ret = new char[available.length()];
		available.getChars(0,available.length(),ret,0);
		Arrays.sort(ret);

		return ret;
	}

	/**
	 * The position the ring occupies, which takes no glycosidic bond.
	 *
	 * <p>An alditol and an open chain have no ring, and a ring size this does not recognise is not
	 * one it will guess at.
	 *
	 * @return Returns the position, or {@code '\0'} where no ring closes one.
	 */
	private char ringPosition() {
		if( isAlditol() )
			return '\0';

		char anomeric = getAnomericCarbon();
		if( ring_size=='p' ) {
			if( anomeric=='1' ) return '5';
			if( anomeric=='2' ) return '6';
		}
		if( ring_size=='f' ) {
			if( anomeric=='1' ) return '4';
			if( anomeric=='2' ) return '5';
		}

		return '\0';
	}

	/**
	 * Whether a child can be given this position.
	 *
	 * @param position The position asked for. {@code '?'} is always acceptable: it says nothing about
	 *        where anything is, so it can neither collide nor be out of range.
	 * @param child The residue that would go there, since a bridge is not bound by the same rules -
	 *        it may attach at the anomeric carbon, which no type's list offers. May be null, which is
	 *        read as an ordinary residue.
	 * @return Returns whether the position is available.
	 */
	public boolean acceptsPosition(char position, Residue child) {
		if( position=='?' )
			return true;

		// A bridge is not an ordinary glycosidic bond: 1,6-anhydro attaches at the anomeric carbon,
		// and the type's list describes what an ordinary bond may do. Only the sibling rule applies.
		boolean isBridge = child!=null && child.getType()!=null && child.getType().isBridge();
		if( isBridge )
			return !positionHeldByAChild(position,child);

		// Nor is substituting this residue's own nitrogen. GlcN's 2 is left out of the list because
		// the amine is there, and acylating that amine is how a GlcNAc is built up a step at a time:
		// "Glc の C2 位の OH が、NHAc に置換され、GlcNAc となります" (I. Yamada, 2026-08-15). The
		// position names the site, and what sits there is the group being modified - which is how the
		// exporter has always read it, writing exactly GlcNAc's WURCS for a GlcN with an Ac at 2, up
		// until 1.37.0 refused the attachment (#211).
		if( substitutesOwnNitrogen(position,child) )
			return !positionHeldByAChild(position,child);

		for( char available : availableLinkagePositions() )
			if( available==position ) return true;

		return false;
	}

	/**
	 * Whether attaching this child at this position means substituting the nitrogen this residue
	 * already carries there, rather than claiming the carbon.
	 *
	 * <p>Both halves are read from the dictionary rather than reasoned about, which is the rule this
	 * corner has taught twice now - the type's list is not a general test of what may attach, and
	 * chemistry that is derived rather than declared has been wrong here before:
	 *
	 * <ul>
	 *   <li><b>Where the nitrogen is</b>: the type's IUPAC name spells the built-in group and its
	 *       position - {@code Glc$2N} is a free amine at 2, {@code Glc$2NAc} an N-acetyl at 2,
	 *       {@code Neu$5NAc} one at 5.</li>
	 *   <li><b>Whether it has room</b>: the type offers {@code N} among its linkage positions when it
	 *       does. GlcN and NeuAc offer it; GlcNAc does not, so its 2 stays closed and a methyl there
	 *       is still refused.</li>
	 * </ul>
	 *
	 * <p>Only a substituent qualifies. A monosaccharide at a nitrogen would be a glycosidic bond to
	 * something that is not a hydroxyl, and nothing measured here supports it.
	 */
	private boolean substitutesOwnNitrogen(char position, Residue child) {
		if( child==null || !child.isSubstituent() || type==null )
			return false;
		if( position!=nitrogenPosition() )
			return false;

		for( char offered : type.getLinkagePositions() )
			if( offered=='N' ) return true;

		return false;
	}

	/**
	 * The position at which this residue's type declares a nitrogen of its own, or {@code '\0'}.
	 *
	 * <p>Taken from the IUPAC name's group suffix: the digit in front of an {@code N} names the
	 * position, as in {@code Glc$2N}, {@code Glc$2NAc} and {@code Neu$5NAc}. A type that declares no
	 * group - {@code Mur$}, {@code Neu$} - has no such position, and keeps the plain carbon in its
	 * list instead.
	 */
	private char nitrogenPosition() {
		if( type==null || !type.hasIupacName() )
			return '\0';

		String name = type.getIupacName();
		int at = name.indexOf('$');
		if( at<0 )
			return '\0';

		for( int i=at+1; i<name.length()-1; i++ )
			if( Character.isDigit(name.charAt(i)) && name.charAt(i+1)=='N' )
				return name.charAt(i);

		return '\0';
	}

	/**
	 * @param position A stated position.
	 * @param except A residue not counted against itself, or null.
	 * @return Returns whether some other child already holds it.
	 */
	private boolean positionHeldByAChild(char position, Residue except) {
		for( Linkage taken : children_linkages ) {
			if( taken.getChildResidue()==except )
				continue;

			for( Bond bond : taken.getBonds() ) {
				char[] positions = bond.getParentPositions();
				if( positions!=null && positions.length==1 && positions[0]==position )
					return true;
			}
		}

		return false;
	}

	/**
	 * Whether any of these bonds names a position this residue will not give the child.
	 *
	 * <p>Every rule about what a position can take is in {@link #acceptsPosition}; this only walks
	 * the bonds. A bond naming several positions at once is left alone - "one of these" is not a
	 * claim on any one of them, and refusing it would refuse structures that say less rather than
	 * something wrong.
	 *
	 * @param child The residue about to be added, which is not counted against itself.
	 * @param bonds The bonds it would be added by.
	 * @return Returns whether one of them asks for a position that is not available.
	 */
	private boolean positionIsNotAvailable(Residue child, Collection<Bond> bonds) {
		if( bonds==null )
			return false;

		for( Bond bond : bonds ) {
			char[] positions = bond.getParentPositions();
			if( positions==null || positions.length!=1 )
				continue;
			if( !acceptsPosition(positions[0],child) )
				return true;
		}

		return false;
	}


	/*
       Move the <code>child</code> residue before <code>other</code>
       in the children list.
       @return <code>false</code> if the residues are not children of this object
	 */
	public boolean moveChildBefore(Residue child, Residue other) {
		if( child==null || other==null || child.getParent()!=this || other.getParent()!=this )
			return false;

		int child_ind = indexOf(child);
		children_linkages.remove(child_ind);        
		int other_ind = indexOf(other);
		children_linkages.add(other_ind,child.getParentLinkage());    
		return true;
	}

	/*
       Move the <code>child</code> residue after <code>other</code>
       in the children list.
       @return <code>false</code> if the residues are not children of this object
	 */
	public boolean moveChildAfter(Residue child, Residue other) {
		if( child==null || other==null || child.getParent()!=this || other.getParent()!=this )
			return false;

		int child_ind = indexOf(child);
		children_linkages.remove(child_ind);        
		int other_ind = indexOf(other);
		children_linkages.add(other_ind+1,child.getParentLinkage());    
		return true;
	}

	/*
       Insert the <code>child</code> residue at the specified position
       in the children list.
       @return <code>true</code> if the operation was successful       
	 */
	public boolean insertChildAt(Residue child, int ind) {
		return insertChildAt(child,Bond.single(),ind);
	}

	/*
       Insert the <code>child</code> residue at the specified position
       in the children list and with specific linkage position.
       @return <code>true</code> if the operation was successful
	 */
	public boolean insertChildAt(Residue child, char parent_link_pos, int ind) {
		return insertChildAt(child,Bond.single(parent_link_pos),ind);
	}

	/*
       Insert the <code>child</code> residue at the specified position
       in the children list and with specific bonds.
       @return <code>true</code> if the operation was successful
	 */
	public boolean insertChildAt(Residue child, Collection<Bond> bonds, int ind) {
		if( child==null )
			return false;

		// cannot add a reducing end
		if( child.isReducingEnd() && !child.canHaveParent() )
			return insertChildAt(child.firstChild(),ind);

		// check for available space
		//if( (getNoLinkages()+bonds.size())<getMaxLinkages() ) {
		Linkage link = new Linkage(this,child,bonds);    
		children_linkages.add(ind,link);
		child.parent_linkage = link;
		return true;
	}

	/*
       Remove a child residue.
       @return <code>true</code> if the operation was successful
	 */
	public boolean removeChild(Residue toremove) {
		if( toremove==null)
			return false;

		// remove child from this node
		int ind = indexOf(toremove);
		if( ind!=-1 ) {
			Linkage parent_link_bk = 
					(toremove.getType().getSuperclass().equals("Bridge") && toremove.hasChildren()) ? 
							toremove.parent_linkage : null;
			
			// unlink child from this node
			children_linkages.remove(ind);
			toremove.parent_linkage = null;

			// connect grand children to this node
			if( this.isStartRepetition() && !toremove.isEndRepetition() ) {
				// start repetition can have only one children

				// find the one on the backbone
				Residue newchild = null;
				for( Linkage l : toremove.children_linkages ) {
					if( l.getChildResidue().findEndRepetition()!=null )
						newchild = l.getChildResidue();
				}

				// connect it to this residue
				this.children_linkages.add(newchild.getParentLinkage());
				newchild.getParentLinkage().setParentResidue(this);

				// connect other children to the backbone
				for( Linkage l : toremove.children_linkages ) {
					if( l.getChildResidue()!=newchild ) {
						newchild.children_linkages.add(l);
						l.setParentResidue(newchild);
					}
				}
			}
			else {
				for( int l=0; l<toremove.getNoChildren(); l++ ){
					Linkage grand_child_link = toremove.children_linkages.get(l);
					if(parent_link_bk != null)
						grand_child_link.getBonds().get(0).setParentPositions(new char[] {parent_link_bk.getParentPositionsSingle()});
					
					this.children_linkages.add(ind+l,grand_child_link);
					grand_child_link.setParentResidue(this);
				}
			}

			// remove other side of repetition
			if( toremove.isStartRepetition() ) 
				removeChild(this.findEndRepetition());        
			else if( toremove.isEndRepetition() ) {
				Residue start = this.findStartRepetition(); 
				if( start!=null )
					start.getParent().removeChild(start);
			}


			if( this.isStartRepetition() ) {        
				// remove empty repetition
				boolean removed = false;
				for( int l=0; l<this.getNoChildren(); l++ ) {
					if( this.getChildAt(l).isEndRepetition() ) {
						removed = true;
						removeChild(this.getChildAt(l));
					}
				}
			}

			return true;
		}

		// navigate down the tree
		for( Linkage l : children_linkages ) {
			if( l.getChildResidue().removeChild(toremove) ) 
				return true;
		}
		return false;
	}
	
	/*
       Insert a residue between this and its parent.
       @return <code>true</code> if the operation was successful.
	 */
	public boolean insertParent(Residue toinsert) {
		return insertParent(toinsert,Bond.single());
	}

	/*
       Insert a residue with a specific linkage position between this
       and its parent.
       @return <code>true</code> if the operation was successful.
	 */
	public boolean insertParent(Residue toinsert, char parent_link_pos) {
		return insertParent(toinsert,Bond.single(parent_link_pos));
	}

	/*
       Insert a residue with specific bonds between this and its
       parent.
       @return <code>true</code> if the operation was successful.
	 */
	public boolean insertParent(Residue toinsert, Collection<Bond> bonds) {

		if( parent_linkage==null )
			return false; 

		// check for compatibility
		if( !toinsert.canHaveParent() || !toinsert.canHaveChildren() || toinsert.getMaxLinkages()<2 )
			return false;           

		// add 
		Residue grand_parent = parent_linkage.getParentResidue();
		int ind = grand_parent.indexOf(this);

		// unlink this from grand parent
		grand_parent.children_linkages.remove(ind);

		// link this to new parent
		parent_linkage.setParentResidue(toinsert);
		toinsert.children_linkages.add(parent_linkage);

		// link new parent to grand parent
		grand_parent.insertChildAt(toinsert,bonds,ind);

		return true;
	}        

	/*
       Swap positions of the two residues in the children list
       @return <code>false</code> if the residues are not children of this object
	 */
	public boolean swapChildren(Residue child1, Residue child2) {
		if( child1==null || child2==null ) 
			return false;

		int ind1 = indexOf(child1);
		int ind2 = indexOf(child2);
		if( ind1==-1 || ind2==-1 )
			return false;

		Linkage link1 = children_linkages.get(ind1);
		Linkage link2 = children_linkages.get(ind2);
		children_linkages.set(ind2,link1);
		children_linkages.set(ind1,link2);
		return true;
	}

	/*
       Create a new residue that is a copy of the current one.
	 */
	/**
	 * How this residue is named in the legend drawn beside a structure, which is the residue's own
	 * name for itself when it has one, and otherwise what its type is called.
	 *
	 * <p>A residue drawn without a symbol is identified only by that legend, and what it should say
	 * depends on the residue rather than on its type: two structures can hold the same type of
	 * residue - the same 4-deoxy hexose - as an alpha in one and a beta in the other. Writing it onto
	 * the type instead, which every residue of that name shares, made the second overwrite the first
	 * and destroyed the type's own description along the way.
	 */
	public String getLegend() {
		return (this.legend != null) ? this.legend : this.type.getDescription();
	}

	public void setLegend(String a_sLegend) {
		this.legend = a_sLegend;
	}

	public Residue cloneResidue() {
		Residue ret = new Residue(this.type);

		ret.anomeric_state = this.anomeric_state;
		ret.anomeric_carbon = this.anomeric_carbon;
		ret.chirality = this.chirality;
		ret.ring_size = this.ring_size;
		ret.legend = this.legend;

		ret.cleaved_residue = (this.cleaved_residue!=null) ?this.cleaved_residue.cloneResidue() :null;

		ret.preferred_placement = (this.preferred_placement!=null) ?this.preferred_placement.clone() :null;
		ret.was_sticky = this.was_sticky;

		return ret;
	}

	/*
       Copy the information about the current residue into the other
       residue.
	 */
	public void copyResidue(Residue other) {

		this.type = other.type;

		this.anomeric_state = other.anomeric_state;
		this.anomeric_carbon = other.anomeric_carbon;
		this.chirality = other.chirality;
		this.ring_size = other.ring_size;

		this.cleaved_residue = (other.cleaved_residue!=null) ?other.cleaved_residue.cloneResidue() :null;

		this.preferred_placement = (other.preferred_placement!=null) ?other.preferred_placement.clone() :null;
		this.was_sticky = other.was_sticky;

	}

	/*
       Create a copy of the subtree rooted at this residue.
	 */
	public Residue cloneSubtree() {
		return cloneSubtree(null, null,new ResidueHolder());
	}

	protected Residue cloneSubtree(Residue stop_el, ResidueType stop_type) {
		Residue stop = new Residue(stop_type);
		if( stop.isCleavage() && stop_el!=null )
			stop.setCleavedResidue(stop_el.cloneResidue());
		return cloneSubtree(stop_el,stop,new ResidueHolder()); 
	}

	protected Residue cloneSubtree(Residue stop_el, Residue stop) {
		return cloneSubtree(stop_el,stop,new ResidueHolder());
	}

	protected Residue cloneSubtree(Residue stop_el, Residue stop, ResidueHolder startRep) {
		if( this==stop_el )         
			return stop;    

		// clone this
		Residue clone = this.cloneResidue();
		if(clone.isStartRepetition()){
			startRep.res=clone;
		}else if(clone.isEndRepetition()){
			startRep.res.setEndRepitionResidue(clone);
			startRep.res=null;
		}

		// clone children
		for( Linkage l : children_linkages ){
			clone.copyChild(l.getChildResidue().cloneSubtree(stop_el,stop,startRep),l.getBonds());

		}

		return clone;    
	}  

	protected Residue cloneSubtreeAdd(Residue add_el, Residue toadd, char toadd_link,ResidueHolder startRep) {
		return cloneSubtreeAdd(add_el,toadd,Bond.single(toadd_link),startRep);
	}

	protected Residue cloneSubtreeAdd(Residue add_el, Residue toadd, Collection<Bond> toadd_bonds, ResidueHolder startRep) {
		// clone this
		Residue clone = this.cloneResidue();

		if(clone.isStartRepetition()){
			startRep.res=clone;
		}else if(clone.isEndRepetition()){
			startRep.res.setEndRepitionResidue(clone);
			startRep.res=null;
		}

		// clone children
		for( Linkage l : children_linkages )
			clone.copyChild(l.getChildResidue().cloneSubtreeAdd(add_el,toadd,toadd_bonds,startRep),l.getBonds());  

				// add child where necessary
				if( this==add_el && toadd!=null ) {
					Linkage link = new Linkage(clone,toadd,toadd_bonds);
					clone.children_linkages.add(link);
					toadd.parent_linkage = link;        
				}

				return clone;    
	}

	public void setEndRepitionResidue(Residue end) {
		endRepetitionResidue=end;

		end.setStartRepetiionResidue(this);
	}  

	public Residue getEndRepitionResidue(){
		return endRepetitionResidue;
	}

	public void setStartRepetiionResidue(Residue start){
		startRepititionResidue=start;
	}

	public Residue getStartRepetitionResidue(){
		return startRepititionResidue;
	}

	public void setStartCyclicResidue(Residue _start) {
		this.startCyclic = _start;
	}
	
	public Residue getStartCyclicResidue() {
		return this.startCyclic;
	}
	
	public void setEndCyclicResidue(Residue _end) {
		this.endCyclic = _end;
	}
	
	public void setAlternativeStart(Residue _start) {
		this.altStart = _start;
	}
	
	public Residue getAlternativeStart() {
		return this.altStart;
	}
	
	public void setAlternativeEnd(Residue _end) {
		this.altEnd = _end;
	}
	
	public Residue getAlternativeEnd() {
		return this.altEnd;
	}
	
	public Residue getEndCyclicResidue() {
		return this.endCyclic;
	}
	
	public void setCenterPosition(Rectangle rectangle) {
		centerPos=rectangle;
	}

	public Rectangle getCenterPosition() {
		return centerPos;
	}
	
	public boolean isAlternative() {
		return (type.getSuperclass().equals("Alternative"));
	}
	
	public boolean isCyclic() {
		return (type.getSuperclass().equals("Cyclic"));
	}
	//-----------------
	// serialization    

}
