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

import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.application.glycanbuilder.converterGlycoCT.GlycoCTCondensedParser;
import org.eurocarbdb.application.glycanbuilder.converterGlycoCT.GlycoCTParser;
import org.eurocarbdb.application.glycanbuilder.dataset.GWSParser;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Bond;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassAware;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.massutil.MassUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.Molecule;
import org.eurocarbdb.application.glycanbuilder.util.SAXUtils;
import org.eurocarbdb.application.glycanbuilder.util.XMLUtils;

import java.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.sax.TransformerHandler;

/**
   This class contains all information about an intact or fragmented
   glycan molecule. The glycan can be partially specified. Uncertain
   linkages, residue superclasses, uncertain connections are
   supported. All residues whose parent is not known are connected to
   a special <i>bracket</i> residue. A glycan molecule where all
   residues are connected to the bracket represents a glycan
   composition.

   @see Residue
   @see Linkage

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class Glycan implements Comparable, SAXUtils.SAXWriter, MassAware {


	//----------------------
	// members

	private Residue root = null;
	private Residue bracket = null;   

	private MassOptions mass_options = new MassOptions();
	private String name;

	// ------------------------
	// construction

	/**
       Empty constructor.
	 */

	public Glycan() {
	}        

	/**
       Create a new glycan structure from a specified tree of residues.

       @param _root the root of the tree representing the new structure
       @param add_redend if <code>true</code> a reducing end modifier
       will be added in case the current tree does not have one
       @param mass_opt mass settings for this glycan structure
	 */

	public Glycan(Residue _root, boolean add_redend, MassOptions mass_opt) {
		// set root
		if( _root==null ) {
			root = null;
		}else if( _root.isReducingEnd() ) {
			// all types of reducing ends even fragments
			if( _root.hasChildren() || _root.isRingFragment() )
				root = _root;
			else 
				root = null;
		}else if( !add_redend ) {
			root = _root;       
		}else {
			root = ResidueDictionary.createReducingEnd(null);
			root.addChild(_root);
		}

		// set bracket
		bracket = null;

		// set mass options
		if( mass_opt!=null ) 
			mass_options.setValues(mass_opt);

		if( !mass_options.getReducingEndType().isFreeReducingEnd() ) 
			setReducingEndType(mass_options.getReducingEndType());
		else 
			mass_options.synchronize(this);
	}    

	/**
       Create a new glycan structure from a specified tree of residues.

       @param _root the root of the tree representing the new structure
       @param _bracket the container for all residues with undefined
       connectivity
       @param add_redend if <code>true</code> a reducing end modifier
       will be added in case the current tree does not have one
       @param mass_opt mass settings for this glycan structure
	 */

	public Glycan(Residue _root, Residue _bracket, boolean add_redend, MassOptions mass_opt) {
		// set root
		if(_root.isStartCyclic()) 
			add_redend = false;
		
		if( _root==null ) {
			root = null;
		} else if( _root.isReducingEnd() ) {
			// all types of reducing ends even fragments
			if( _bracket!=null || _root.hasChildren() || _root.isRingFragment() ) 
				root = _root;        
			else 
				root = null;
		}
		else if( !add_redend ) {
			root = _root;        
		}else {
			root = ResidueDictionary.createReducingEnd(null);
			root.addChild(_root);
		}

		// set bracket
		if( root!=null )
			bracket = _bracket;
		else
			bracket = null;

		// set mass options
		if( mass_opt!=null ) 
			mass_options.setValues(mass_opt);

		if( !mass_options.getReducingEndType().isFreeReducingEnd() ) 
			setReducingEndType(mass_options.getReducingEndType());    
		else 
			mass_options.synchronize(this);
	}    

	/**
       Create an empty glycan object representing a composition.
       @param mass_opt mass settings for this glycan structure
	 */

	static public Glycan createComposition(MassOptions mass_opt) {
		Residue bracket = ResidueDictionary.createBracket();
		Residue root = ResidueDictionary.createReducingEnd(null);
		bracket.isComposition(true);
		root.isComposition(true);
		return new Glycan(root,bracket,true,mass_opt);
	}

	/**
       Return the composition of this glycan structure. All residues
       are trasformed into their superclasses.
       @return a new glycan object representing the composition
	 */

	public Glycan getComposition() {
		return getComposition(true);
	}


	/**
       Retrieve the composition of this glycan structure. 

       @param show_superclasses if <code>true</code> all residues are
       trasformed into their superclasses.
       @return a new glycan object representing the composition
	 */

	public Glycan getComposition(boolean show_superclasses) {
		try {
			Glycan ret = createComposition(this.mass_options);
			for( Residue r : getAllResidues() ) {
				if( r.getType().isAttachPoint() || r.getType().isBracket() ) 
					continue;            
				else if( r.getType().isCleavage() ) {
					if( r.getType().canBeReducingEnd() )
						ret.setRoot(r.cloneResidue());
					else
						ret.addAntenna(r.cloneResidue());
				}
				else if( r.isReducingEnd() ) 
					ret.setRoot(r.cloneResidue());        
				else {
					if( show_superclasses )
						ret.addAntenna(ResidueDictionary.newResidue(r.getType().getCompositionClass()));
					else
						ret.addAntenna(ResidueDictionary.newResidue(r.getType().getName()));
				}
			}
			return ret;
		}    
		catch(Exception e) {
			return createComposition(this.mass_options);      
		}
	}

	/**
       Return a collection of all residues in the structure.
	 */

	public Collection<Residue> getAllResidues() {
		LinkedList<Residue> ret = new LinkedList<Residue>();
		getAllResidues(ret,root);
		getAllResidues(ret,bracket);
		return ret;
	}


	private void getAllResidues(LinkedList<Residue> dest, Residue node) {
		if( node!=null ) {
			dest.addLast(node);
			
			for( Linkage l : node.getChildrenLinkages() ) {
				getAllResidues(dest,l.getChildResidue());
			}
		}
	}

	public LinkedList<Residue> getAllSaccharide() {
		TreeMap<Integer, Residue> map = new TreeMap<Integer, Residue>();
		this.getAllSaccharide(map, root.getChildAt(0));
		return this.sortResidue(map);
	}
	
	private void getAllSaccharide(TreeMap<Integer, Residue> lst_red, Residue node) {
		if(node == null || node.isBracket() || node.equals(this.root)) return;
		if(node.isSaccharide() && !lst_red.containsValue(node)) lst_red.put(node.id, node);
		for(Linkage l : node.getChildrenLinkages()) {
			this.getAllSaccharide(lst_red, l.getChildResidue());
		}
		
		return;
	}
	
	private LinkedList<Residue> sortResidue(TreeMap<Integer, Residue> map_red) {
		LinkedList<Residue> ret = new LinkedList<Residue>();
		for(Residue r : map_red.values()) ret.addLast(r);
		return ret;
	}
	
	/**
       Compare this glycan to another object
       @see Comparable#compareTo
	 */

	public int compareTo(Object o) {
		if( o==null || !(o instanceof Glycan))
			return 1;
		String s1 = this.toStringOrdered();
		String s2 = ((Glycan)o).toStringOrdered();
		return s1.compareTo(s2);
	}

	/**
       Compare this glycan to another object ignoring the charge
       configuration.
       @see Comparable#compareTo
	 */

	public int compareToIgnoreCharges(Object o) {
		if( o==null || !(o instanceof Glycan))
			return 1;
		String s1 = this.toStringOrdered(false);
		String s2 = ((Glycan)o).toStringOrdered(false);
		return s1.compareTo(s2);
	}

	/**
       Create a new glycan object that is a copy of the current one.
	 */
	public Glycan clone() {
		return clone(false);
	}

	/**
       Create a new glycan object that is a copy of the current one.
       @param add_redend if <code>true</code> a reducing end modifier
       will be added in case the current tree does not have one
	 */
	public Glycan clone(boolean add_redend) {
		Glycan ret = null;

		if( root==null )
			ret = new Glycan(null,add_redend,this.mass_options);
		else 
			ret = new Glycan(root.cloneSubtree(),(bracket!=null) ?this.bracket.cloneSubtree() :null,add_redend,this.mass_options);
		
		return ret;
	}

	/**
       Return <code>true</code> if the two glycan structures have the
       same structure. The <code>equals</code> method from the
       <code>Object</code> class is not redefined.
	 */
	public boolean equalsStructure(Glycan og) {
		if( og==null )
			return false;
		if( root==null )
			return (og.root==null);
		if( !root.subtreeEquals(og.root) )
			return false;
		if( bracket==null )
			return og.bracket==null;
		return bracket.subtreeEquals(og.bracket);
	}

	// Access members

	/**
       Return the mass settings for the glycan structure.
	 */
	public MassOptions getMassOptions() {
		return mass_options;
	}

	/**
       Set the mass settings for the glycan structure.
	 */
	public boolean setMassOptions(MassOptions mass_opt) {
		boolean changed = mass_options.setValues(mass_opt);
		changed |= setReducingEndType(mass_options.getReducingEndType());
		return changed;
	}

	/**
       Set the reducing end marker for this glycan structure. The mass
       settings are updated.
	 */
	public boolean setReducingEndType(ResidueType new_type) { 
		Residue redend = getRoot();     
		if( redend!=null && redend.isReducingEnd() && !redend.isCleavage() && !redend.getTypeName().equals(new_type.getName()) ) {
			redend.setType(new_type);
			if(redend.getTypeName().equals("redEnd") || redend.getTypeName().equals("d")) {
				redend.getChildAt(0).setAnomericState('?');
			}
			if(redend.getTypeName().equals("redEnd"))
				redend.getChildAt(0).setAlditol(true);
			
			return true;
		}		
		
		return false;
	}

	/**
       Reset the reducing end marker to a free reducing end.
	 */
	public void removeReducingEndModification() {
		setReducingEndType(ResidueType.createFreeReducingEnd());
	}

	/**
       Return a copy of the current glycan structure with a free
       reducing end.
	 */
	public Glycan withNoReducingEndModification() {
		Glycan ret = this.clone();
		ret.removeReducingEndModification();
		return ret;
	}

	/**
       Return the root residue.
	 */
	public Residue getRoot() {
		return root;
	}

	/**
       Return the root residue excluding free reducing end markers and
       attach points.
	 */
	public Residue getRoot(boolean ret_redend) {
		if( root==null )
			return null;

		//    if( ret_redend || !(root.getTypeName().equals("freeEnd") || root.getTypeName().equals("redEnd") || root.getTypeName().equals("#attach")) )
		/**
		 * TODO: One user has asked for reducing end residues not to be shown regardless of type when the hide reducing end indicator check box is ticked
		 *       Is this a behaviour everyone would agree one?
		 */
		//if( ret_redend || !(root.getTypeName().equals("freeEnd") || root.getTypeName().equals("redEnd") ||root.getTypeName().equals("#attach")) )
		//	return root;
		if(ret_redend){
			return root;
		}

		return root.firstChild();
	}

	/**
       Set the root residue.
	 */
	public void setRoot(Residue new_root) {
		//if( new_root==null || new_root.isReducingEnd() )
			root = new_root;
	}
	
	/**
       Return the bracket residue
       @see ResidueType#createBracket
	 */
	public Residue getBracket() {
		return bracket;
	}

	/**
       Return <code>true</code> if the structure contains no residues.
	 */
	public boolean isEmpty() {
		if( root==null )
			return true;
		if( root.hasChildren() )
			return false;
		if( bracket==null )
			return true;
		if( bracket.hasChildren() )
			return false;
		return true;
	}

	/**
       Return <code>true</code> if this object represents a glycan
       composition.
	 */
	public boolean isComposition() {
		return (bracket!=null && root!=null && !root.hasChildren());
	}

	/**
       Return <code>true</code> if some residue have unspecified
       connectivity.
	 */
	public boolean isFuzzy() {
		return isFuzzy(false);
	}

	/**
       Return <code>true</code> if some residue have unspecified
       connectivity. If <code>tolerate_labiles</code> is
       <code>true</code> the labile residues with unspeficied
       connectivity will not be considered.
       @see #detachLabileResidues
	 */

	public boolean isFuzzy(boolean tolerate_labiles) {
		if( bracket==null )
			return false;
		if( !tolerate_labiles )
			return true;

		// check if all children of bracket are labiles
		for( Linkage l : bracket.getChildrenLinkages() ) 
			if( !l.getChildResidue().isLabile() )
				return true;

		// check if all labiles can be assigned to positions in the structure
		TypePattern lp = getDetachedLabilesPattern();
		TypePattern lpp = getLabilePositionsPattern();
		return !lpp.contains(lp);
	}

	/**
       Return <code>true</code> if this structure is a fragment of a
       glycan.
	 */
	public boolean isFragment() {
		return isFragmentSubtree(root);    
	}

	private boolean isFragmentSubtree(Residue node) {
		if( node.isCleavage() )
			return true;
		for( Linkage link : node.getChildrenLinkages() ) {
			if( isFragmentSubtree(link.getChildResidue()) )
				return true;
		}
		return false;
	}

	/**
       Return <code>true</code> if this structure contains repeat
       blocks.
	 */
	public boolean hasRepetition() {
		return hasRepetition(root) || hasRepetition(bracket);    
	}
	
	private boolean hasRepetition(Residue node) {
		if( node==null )
			return false;
		if( node.isRepetition() )
			return true;
		for( Linkage link : node.getChildrenLinkages() ) {
			if( hasRepetition(link.getChildResidue()) )
				return true;
		}
		return false;
	}

	/**
       Return the number of uncertain antennae of this structure,
       i.e. the number of children of the bracket residue.
	 */
	public int getNoAntennae() {
		if( bracket==null )
			return 0;
		return bracket.getChildrenLinkages().size();
	}

	/**
       Return the {@link Linkage linkages} to the uncertain antennae.
	 */
	public LinkedList<Linkage> getAntennaeLinkages() {
		if( bracket!=null )
			return bracket.getChildrenLinkages();
		return new LinkedList<Linkage>();
	}

	/**
       Return <code>true</code> if the structure contain a residue
       matching <code>node</code>
       @see Residue#subtreeContains
	 */
	public boolean contains(Residue node) {
		return ( (root!=null && root.subtreeContains(node)) || 
				(bracket!=null && bracket.subtreeContains(node)) );
	}

	/**
       Return <code>true</code> if all linkages are valid and
       specified.
       @see Residue#checkLinkagesSubtree
	 */
	public boolean checkLinkages() {
		if( root!=null && !root.checkLinkagesSubtree() )
			return false;
		if( bracket!=null && !bracket.checkLinkagesSubtree() )
			return false;
		return true;
	}

	/**
       Return <code>true</code> if all linkages are specified.
       @return <code>false</code> if some residues have unspecified connectivity
       @see Residue#isFullySpecifiedSubtree
	 */
	public boolean isFullySpecified() {
		if( isFuzzy() )
			return false;
		return root.isFullySpecifiedSubtree();
	}

	static protected Collection<Residue> getPath(Residue a, Residue b) {
		//if( isEmpty() ) 
		//return new Vector<Residue>();    

		// get paths from A to root
		Residue nav; 
		Stack<Residue> pra = new Stack<Residue>();
		for(nav=a; nav!=null; nav = nav.getParent()) 
			pra.push(nav);    

		// get paths from B to root
		Stack<Residue> prb = new Stack<Residue>();
		for(nav=b; nav!=null; nav = nav.getParent()) 
			prb.push(nav);

		// check if root is the same
		if( pra.peek()!=prb.peek() )
			return new Vector<Residue>();    

		// remove common steps
		Residue common = null;
		while( !pra.empty() && !prb.empty() && pra.peek()==prb.peek() ) {
			common = pra.pop();
			prb.pop();
		}
		pra.push(common);

		// create path
		Vector<Residue> path = new Vector<Residue>();
		for(Iterator<Residue> i=pra.iterator(); i.hasNext(); ) 
			path.add(i.next());    
		while(!prb.empty()) 
			path.add(prb.pop());

		return path;    
	}

	/**
       Return the maximum distance between the root and a leaf.
	 */
	public int getDepth() {
		return getDepth(root) + (getDepth(bracket)-1);    
	}

	private int getDepth(Residue current) {
		if( current==null )
			return 0;

		int depth = 0;
		for( Linkage l : current.getChildrenLinkages() )
			depth = Math.max(depth,getDepth(l.getChildResidue()));
		return depth+1;
	}

	/**
       Return the number of residues in the structure.
	 */
	public int getCount() {
		return getCount(root) + (getCount(bracket)-1);
	}

	private int getCount(Residue current) {
		if( current==null ) 
			return 0;

		int tot_count = 1;
		for( Linkage l : current.getChildrenLinkages() )
			tot_count += getCount(l.getChildResidue());

		return tot_count;
	}

	/**
       Resete the preferred display placement for all residues in the
       structure.
       @see Residue#resetPreferredPlacement
	 */

	public void removePlacements() {
		removePlacements(root);    
		removePlacements(bracket);
	}

	private void removePlacements(Residue current) {
		if( current==null )
			return;

		current.resetPreferredPlacement();
		for( Linkage l : current.getChildrenLinkages()) 
			removePlacements(l.getChildResidue());            
	}

	/**
       Count the number of residues of a specific type
	 */
	public int countResidues(String typename) {
		return countResidues(root,typename) + countResidues(bracket,typename);
	}

	private int countResidues(Residue current, String typename) {
		int count = 0;
		if( current==null )
			return count;

		if( current.getTypeName().equals(typename) )
			count++;

		for( Linkage l : current.getChildrenLinkages()) 
			count += countResidues(l.getChildResidue(),typename);

		return count;
	}   

	public boolean contains(Glycan other, boolean include_redend, boolean include_all_leafs) {
		return contains(other, include_redend, include_all_leafs, true);
	}
	
	/**
       Return <code>true</code> if this structure contain
       <code>other</code>.  A full substructure search is performed,
       with fuzzy matching between residues.
       @param include_redend <code>true</code> if the matching part
       must begint from the reducing end. 
       @param include_all_leafs <code>true</code> if the matching part
       must contain all the leaf of the other structure.
       @see Residue#fuzzyMatch       
	 */
	public boolean contains(Glycan other, boolean include_redend, boolean include_all_leafs, boolean fuzzy) {
		return (other==null ||
				(countSubtree(this.getRoot(false),other.getRoot(false),include_redend,include_all_leafs,true,fuzzy)!=0 &&
						contains(this.getBracket(),other.getBracket(),include_all_leafs,fuzzy)) ||
						(other.getBracket()==null && (!include_redend || this.getRoot(false)==null) &&
								countSubtree(this.getBracket(),other.getRoot(false),include_redend,include_all_leafs,true,fuzzy)!=0)
		);        
	}

	public int count(Glycan other, boolean include_redend, boolean include_all_leafs) {
		return count(other, include_redend, include_all_leafs, true);
	}
	
	/**
       Return the number of times this structure contain
       <code>other</code>. A full substructure search is performed,
       with fuzzy matching between residues.
       @param include_redend <code>true</code> if the matching part
       must begint from the reducing end. 
       @param include_all_leafs <code>true</code> if the matching part
       must contain all the leaf of the other structure.
       @see Residue#fuzzyMatch       
	 */
	public int count(Glycan other, boolean include_redend, boolean include_all_leafs, boolean fuzzy) {
		if( other==null )
			return 1;

		int count = countSubtree(this.getRoot(false),other.getRoot(false),include_redend,include_all_leafs,false,fuzzy);
		if( count!=0 && contains(this.getBracket(),other.getBracket(),include_all_leafs,fuzzy) )
			return count;
		else if( other.getBracket()==null && (!include_redend || this.getRoot(false)==null) )
			return countSubtree(this.getBracket(),other.getRoot(false),include_redend,include_all_leafs,true,fuzzy);
		else
			return 0;
	}

	private int countSubtree(Residue container, Residue terminal, boolean include_redend, boolean include_all_leafs, boolean stop_at_first) {
		return countSubtree(container, terminal, include_redend, include_all_leafs, stop_at_first, true);
	}

	private int countSubtree(Residue container, Residue terminal, boolean include_redend, boolean include_all_leafs, boolean stop_at_first, boolean fuzzy) {
		int count = 0;

		if( contains(container,terminal,include_all_leafs,fuzzy) ) {
			if( stop_at_first )        
				return 1;    
			count = 1;
		}

		if( container==null ) {
			return count;    
		}

		if( !include_redend ) {
			// explore the tree
			for( Linkage l : container.getChildrenLinkages() ) {
				count += countSubtree(l.getChildResidue(),terminal,false,include_all_leafs,stop_at_first,fuzzy);
				if( count!=0 && stop_at_first )                
					return 1;    
			}
		}
		return count;
	}

	private boolean contains(Linkage container, Linkage other, boolean include_all_leafs) {
		return contains(container, other, include_all_leafs, true);
	}
	
	private boolean contains(Linkage container, Linkage other, boolean include_all_leafs, boolean fuzzy) {
		if( !container.match(other,fuzzy) ) {
			//System.out.println("link mismatch " + GWSParser.toStringLinkage(container) + " " + GWSParser.toStringLinkage(other) + " at " + GWSParser.writeResidueType(container.getParentResidue()));
			return false;
		}
		return contains(container.getChildResidue(), other.getChildResidue(), include_all_leafs,fuzzy);
	}
	
	private boolean contains(Residue container, Residue other, boolean include_all_leafs) {
		return contains(container, other, include_all_leafs, true);
	}

	private boolean contains(Residue container, Residue other, boolean include_all_leafs, boolean fuzzy) {
		if( other==null ) {
			//System.out.println("other null: " + container);
			return (container==null || !include_all_leafs);
		}
		if( container==null ) {
			//System.out.println("empty container");    
			return false;
		}

		// match current nodes
		if( !container.match(other,fuzzy) ) {
			//System.out.println("residue mismatch " + GWSParser.writeResidueType(container));
			return false;
		}

		// match children
		if( (include_all_leafs && container.getNoChildren()!=other.getNoChildren()) || 
				container.getNoChildren()<other.getNoChildren() ) {
			//System.out.println("number of children mismatch at " + GWSParser.writeResidueType(container) );
			return false;
		}

		if( other.getNoChildren()==0 )
			return true;

		// try all possible permutations with no repetitions
		PermutationGenerator cg = new PermutationGenerator(other.getNoChildren());
		while( cg.hasMore() ) {
			int[] indices = cg.getNext();
			//System.out.println(TextUtils.toString(indices,' '));

			int matched = 0;
			for( int l=0,i=0; l<other.getNoChildren(); l++ ) {
				boolean contains = false;
				for( ; i<container.getNoChildren(); i++ ) {
					if( contains(container.getLinkageAt(i),other.getLinkageAt(indices[l]),include_all_leafs,fuzzy) ) {
						matched++;
						i++;
						break;        
					}    
				}
			}     
			if( matched==other.getNoChildren() )         
				return true; // found all
				//else
					//System.out.println("matched " + matched + "/" + other.getNoChildren());
		}
		//System.out.println("no permutations match at " + GWSParser.writeResidueType(container) );
		return false;
	}


	// Modify structure

	/**
       Add a bracket residue to the structure.
       @see ResidueType#createBracket
	 */
	public Residue addBracket() {
		if( bracket==null ) {
			bracket = ResidueDictionary.createBracket();
			return bracket;
		}
		return null;
	}

	/**
       Remove the bracket residue from the structure.
       @see ResidueType#createBracket
	 */
	public boolean removeBracket() {
		if( bracket==null )
			return false;
		bracket = null;
		return true;
	}

	/**
       Add a uncertain antenna to the structure.
       @see ResidueType#createBracket
       @see Residue#addChild(Residue)
	 */
	public boolean addAntenna(Residue antenna) {
		return addAntenna(antenna,Bond.single());
	}

	/**
       Add a uncertain antenna to the structure with a specific
       linkage position.
       @see ResidueType#createBracket
       @see Residue#addChild(Residue,char)  
	 */
	public boolean addAntenna(Residue antenna, char parent_link_pos) {
		return addAntenna(antenna,Bond.single(parent_link_pos));
	}

	/**
       Add a uncertain antenna to the structure with specific
       bonds.
       @see ResidueType#createBracket
       @see Residue#addChild(Residue,Collection)  
	 */
	public boolean addAntenna(Residue antenna, Collection<Bond> bonds) {
		if( bracket==null ) 
			addBracket();
		return bracket.addChild(antenna,bonds);
	}

	/**
       Remove a residue from the structure.
       @return <code>true</code> if the operation was successful
	 */
	public boolean removeResidue(Residue toremove) {
		if( root==null )
			return false;
		if( toremove==null || root==toremove )
			return false;
		if( toremove==bracket ) {
			if( bracket.hasChildren() )
				return false;
			return removeBracket();
		}    
		if( root.removeChild(toremove) ) {
			if( !root.hasChildren() ) 
				root = null;
			return true;
		}
		if( bracket!=null && bracket.removeChild(toremove) ) 
			return true;    
		return false;
	}

	/**
       Remove a collection of residues from the structure.
       @return <code>true</code> if the operation was successful
	 */
	public boolean removeResidues(Collection<Residue> toremove) {
		boolean removed = false;
		boolean had_antennae = (bracket!=null && bracket.hasChildren());
		for(Iterator<Residue> i=toremove.iterator(); i.hasNext(); ) {
			if( removeResidue(i.next()) ) 
				removed = true;        
		}
		if( !removed )
			return false;

		if( had_antennae && bracket!=null && !bracket.hasChildren() ) 
			removeResidue(bracket);
		return true;
	}

	public void removeUnpairedRepetitions() {
		removeUnpairedRepetitions(root);
		removeUnpairedRepetitions(bracket);
	}

	private void removeUnpairedRepetitions(Residue cur) {
		if( cur==null )
			return;
		if( (cur.isStartRepetition() && cur.findEndRepetition()==null) ||
				(cur.isEndRepetition() && cur.findStartRepetition()==null) )
			removeResidue(cur);    
		for( int i=0; i<cur.getNoChildren(); i++ )
			removeUnpairedRepetitions(cur.getChildAt(i));
	}

	public LinkedList<Glycan> splitMultipleRoots() {
		LinkedList<Glycan> new_structures = new LinkedList<Glycan>();
		while(root.getNoChildren()>1) {
			// remove one child from the root
			Residue child = root.getChildAt(1);
			root.getChildrenLinkages().remove(1);
			child.setParentLinkage(null);

			// add new structure
			new_structures.addLast(new Glycan(child,true,mass_options));
		}
		return new_structures;     
	}

	// Functions   

	/**
       Try attaching the uncertain antennae in all positions of the
       structure.
       @return all possible resulting configuration
	 */
	public Collection<Glycan> placeAntennae() {
		LinkedList<Glycan> ret = new LinkedList<Glycan>();
		this.placeAntennae(ret);
		return ret;
	}

	/**
       Try attaching the uncertain antennae in all positions of the
       structure.
       @param structures all possible resulting configuration
	 */
	public void placeAntennae(LinkedList<Glycan> structures) {
		structures.clear();

		if( bracket==null ) 
			structures.add(this.clone());
		else if( root!=null )
			placeAntennae(root,root,new LinkedList<Linkage>(bracket.getChildrenLinkages()),structures);        
	}

	private void placeAntennae(Residue root, Residue current, LinkedList<Linkage> antennae, LinkedList<Glycan> structures) {
		if( current==null )
			return;

		if( antennae.size()==0 ) {
			structures.add(new Glycan(root,false,mass_options));
			return;
		}

		// place antenna
		Linkage link = antennae.getFirst();
		Residue antenna = link.getChildResidue();
		Collection<Bond> ant_pos = link.getBonds();        
		if( current.isSaccharide() && current.canAddChild(antenna,ant_pos) ) {
			antennae.removeFirst();
			Residue new_root = root.cloneSubtreeAdd(current,antenna.cloneSubtree(),ant_pos,new ResidueHolder());
			placeAntennae(new_root,new_root,antennae,structures);
			antennae.addFirst(link);
		}

		// recursive traversal
		for( Linkage l : current.getChildrenLinkages() ) 
			placeAntennae(root,l.getChildResidue(),antennae,structures);    
	}
	
	/**
       Return <code>true</code> if this structure represent a fragment
       with no intact saccharides.
	 */
	public boolean isSmallRingFragment() {    
		if( bracket!=null || root==null )
			return false;

		if( root.isRingFragment() )        
			return !root.hasSaccharideChildren();

		if( root.getNoChildren()==1 && root.firstChild().isRingFragment() )
			return !root.firstChild().hasSaccharideChildren();

		return false;
	}

	/**
       Return all cleavage markers.
	 */
	public Vector<Residue> getCleavages() {
		Vector<Residue> ret = new Vector<Residue>();
		getCleavages(ret,root);
		return ret;
	}

	static private void getCleavages(Vector<Residue> buffer, Residue node) {
		if( node==null || buffer==null )
			return;

		if( node.isCleavage() )
			buffer.add(node);

		for( Linkage l : node.getChildrenLinkages() ) 
			getCleavages(buffer,l.getChildResidue());
	}

	// ----------
	// mass computation

	/**
       Return the derivatization.
       @see MassOptions#DERIVATIZATION
	 */
	public String getDerivatization() {
		return mass_options.DERIVATIZATION;
	}

	/**
       Return the associated charges.
       @see MassOptions#ION_CLOUD
	 */
	public IonCloud getCharges() {
		return mass_options.ION_CLOUD;
	}

	/**
       Set the associated charges.
       @see MassOptions#ION_CLOUD
	 */
	public void setCharges(IonCloud ic) {
		if( ic!=null )
			mass_options.ION_CLOUD = ic;
		else
			mass_options.ION_CLOUD = new IonCloud();

	}

	/**
       Return the associated neutral exchanges.
       @see MassOptions#NEUTRAL_EXCHANGES
	 */
	public IonCloud getNeutralExchanges() {
		return mass_options.NEUTRAL_EXCHANGES;
	}

	/**
       Set the associated neutral exchanges.
       @see MassOptions#NEUTRAL_EXCHANGES
	 */
	public void setNeutralExchanges(IonCloud ne) {
		if( ne!=null )
			mass_options.NEUTRAL_EXCHANGES = ne;
		else
			mass_options.NEUTRAL_EXCHANGES = new IonCloud();
	}

	/**
       Count the number of charges associated to the structure
	 */
	public int countCharges() {
		return countChargesSubtree(root,false) + countChargesSubtree(bracket,false);
	}

	/**
       Count the number of charges associated to the structure
       @param allow_virtual_charges if <code>true</code> include in
       the count the charges associated with acidic groups 
	 */
	public int countCharges(boolean allow_virtual_charges) {
		return countChargesSubtree(root,allow_virtual_charges) + countChargesSubtree(bracket,allow_virtual_charges);
	}

	static private int countChargesSubtree(Residue node, boolean allow_virtual_charges) {
		if( node==null )
			return 0;

		int no_charges = node.getType().getNoCharges();
		if( no_charges==0 && allow_virtual_charges && node.isLCleavage() )
			no_charges = node.getCleavedResidue().getType().getNoCharges();

		for( Linkage l : node.getChildrenLinkages() ) 
			no_charges += countChargesSubtree(l.getChildResidue(),allow_virtual_charges);

		return no_charges;
	}
	
	/**
	 * David R. Damerell: We shouldn't really share Glycans across threads but we do when no manipulation of
	 * the glycan is going to occur.  This is a fringe case were we do change the mass options of the structure
	 * which might be an issue when sharing Glycan instances as a "in memory library".  For the time being we
	 * will incur the penalty of a clone operation.
	 */
	public double computeMass(String type) {
		Glycan cloneOf=clone();
		cloneOf.setMassOptions(mass_options.clone());
		cloneOf.getMassOptions().setIsotope(type);
		
		return cloneOf.computeMass();
	}

	/**
       Compute the mass of the molecule given the current mass
       settings.
	 */
	public double computeMass() {
		if( hasRepetition() && (areAllRepetitionsConstant(root)==false || areAllRepetitionsConstant(bracket)==false)){
			
			return -1.;
		}
		return computeMass(root,1) + computeMass(bracket,1);
	}

	/**
       Return the number of positions available for methylation.
	 */
	public int computeNoMethylPositions() {
		return computeNoMethylPositions(root) + computeNoMethylPositions(bracket);
	}

	/**
       Return the number of positions available for acetylation.
	 */
	public int computeNoAcetylPositions() {
		return computeNoAcetylPositions(root) + computeNoAcetylPositions(bracket);
	}

	/**
       Compute the mass-to-charge ratio given the current mass settings.
	 */
	public double computeMZ() {
		double mass = computeMass();		
		return mass_options.ION_CLOUD.and(mass_options.NEUTRAL_EXCHANGES).computeMZ(mass);
	}   

	/**
       Compute the chemical formula for this structure.
       @see Molecule
	 */
	public Molecule computeMolecule() throws Exception {
		Molecule ret = new Molecule();
		Molecule subst_mol = substitutionMolecule();    
		computeMolecule(ret,root,subst_mol);
		computeMolecule(ret,bracket,subst_mol);
		return ret;
	}

	/**
       Compute the chemical formula for this structure comprising
       associated charges and neutral exchanges.
       @see Molecule
	 */
	public Molecule computeIon() throws Exception {
		Molecule ret = computeMolecule();
		ret.add(mass_options.ION_CLOUD.getMolecule());
		ret.add(mass_options.NEUTRAL_EXCHANGES.getMolecule());
		return ret;
	}        

	/**
       Return all associated charges and neutral exchanges
	 */
	public IonCloud getChargesAndExchanges() {
		return mass_options.ION_CLOUD.and(mass_options.NEUTRAL_EXCHANGES);
	}       

	private boolean isDropped(ResidueType type) {
		if( type.isDroppedWithMethylation() &&
				(mass_options.DERIVATIZATION.equals(MassOptions.PERMETHYLATED) ||
						mass_options.DERIVATIZATION.equals(MassOptions.PERDMETHYLATED) ||
							mass_options.DERIVATIZATION.equals(MassOptions.HEAVYPERMETHYLATION)) )
			return true;
		if( type.isDroppedWithAcetylation() &&
				(mass_options.DERIVATIZATION.equals(MassOptions.PERACETYLATED) ||
						mass_options.DERIVATIZATION.equals(MassOptions.PERACETYLATED)) )
			return true;
		return false;                
	}

	private int noSubstitutions(ResidueType type) {
		if( mass_options.DERIVATIZATION.equals(MassOptions.PERMETHYLATED) ||
				mass_options.DERIVATIZATION.equals(MassOptions.PERDMETHYLATED) ||
						mass_options.DERIVATIZATION.equals(MassOptions.HEAVYPERMETHYLATION) )
			return type.getNoMethyls();
		if( mass_options.DERIVATIZATION.equals(MassOptions.PERACETYLATED) ||
				mass_options.DERIVATIZATION.equals(MassOptions.PERDACETYLATED) )
			return type.getNoAcetyls();
		return 0;    
	}

	private double substitutionMass() {
		if( mass_options.DERIVATIZATION.equals(MassOptions.PERMETHYLATED) ) 
			return (MassUtils.methyl.getMass() - MassUtils.hydrogen.getMass());
		if( mass_options.DERIVATIZATION.equals(MassOptions.PERDMETHYLATED) ) 
			return (MassUtils.dmethyl.getMass() - MassUtils.hydrogen.getMass());
		if( mass_options.DERIVATIZATION.equals(MassOptions.PERACETYLATED) ) 
			return (MassUtils.acetyl.getMass() - MassUtils.hydrogen.getMass());
		if( mass_options.DERIVATIZATION.equals(MassOptions.PERDACETYLATED) ) 
			return (MassUtils.dacetyl.getMass() - MassUtils.hydrogen.getMass());
		if( mass_options.DERIVATIZATION.equals(MassOptions.HEAVYPERMETHYLATION) ) 
			return (MassUtils.heavyMethyl.getMass() - MassUtils.hydrogen.getMass());
		return 0.;
	}

	private Molecule substitutionMolecule() throws Exception {
		if( mass_options.DERIVATIZATION.equals(MassOptions.PERMETHYLATED) ) 
			return MassUtils.methyl.and(MassUtils.hydrogen,-1);
		if( mass_options.DERIVATIZATION.equals(MassOptions.PERDMETHYLATED) ) 
			return MassUtils.dmethyl.and(MassUtils.hydrogen,-1);
		if( mass_options.DERIVATIZATION.equals(MassOptions.PERACETYLATED) ) 
			return MassUtils.acetyl.and(MassUtils.hydrogen,-1);
		if( mass_options.DERIVATIZATION.equals(MassOptions.PERDACETYLATED) ) 
			return MassUtils.dacetyl.and(MassUtils.hydrogen,-1);
		if( mass_options.DERIVATIZATION.equals(MassOptions.HEAVYPERMETHYLATION) ) 
			return MassUtils.heavyMethyl.and(MassUtils.hydrogen,-1);
		return new Molecule();
	}

	private double computeMass(Residue node) {
		if( node==null )
			return 0.;

		ResidueType type = node.getType();
		int no_bonds = node.getNoBonds();

		// add mass of the saccharide    
		double mass =  type.getMass();

		// modify for alditol
		if( node.isReducingEnd() && node.getType().makesAlditol() )
			mass += 2*MassUtils.hydrogen.getMass();

		if( node.isBracket() ) {
			int no_linked_labiles = Math.min(countLabilePositions(),countDetachedLabiles());
			mass -= (no_bonds-no_linked_labiles)*substitutionMass();
		}
		else if( node.isCleavage() && !node.isRingFragment() ) {
			// cleavages have no derivatization
			if( node.isReducingEnd() && !node.hasChildren() ) {
				// fix for composition
				//mass += MassOptions.H2O;    
				mass += substitutionMass();
			}
		}
		else {
			// add groups
			if( isDropped(type) )
				mass -= (type.getMass() - MassUtils.water.getMass() - substitutionMass());
			else
				mass += (noSubstitutions(type)-no_bonds)*substitutionMass();
		}    

		// add children
		for( Linkage l : node.getChildrenLinkages() ) {
			mass -= MassUtils.water.getMass()*l.getNoBonds(); // remove a water molecule for each bond                
			mass += computeMass(l.getChildResidue());
		}

		return mass;
	}
	
	public boolean areAllRepetitionsConstant(Residue node){
		if(node==null)
			return true;
		
		if(node.isEndRepetition() && (node.getMinRepetitions()==-1 || node.getMaxRepetitions()==-1 || (node.getMinRepetitions()!=node.getMaxRepetitions()))){
			return false;
		}else{
			for(Linkage l:node.getChildrenLinkages()) {
				if(areAllRepetitionsConstant(l.getChildResidue())==false){
					return false;
				}
			}

			return true;
		}
	}
	
	private double computeMass(Residue node, double multipler) {
		if( node==null || node.getTypeName().equals("Sugar"))
			return 0.;
		
		if(node.isStartRepetition()){
			Residue end=node.getEndRepitionResidue();
			if(end.getMaxRepetitions()==end.getMinRepetitions()){
				multipler=end.getMaxRepetitions();
			}else{
				//will throw something here!
			}
		}else if(node.isEndRepetition()){
			multipler=1;
		}

		ResidueType type = node.getType();
		int no_bonds = node.getNoBonds();
		
		//repetition notes
		//we assume a signal bond between the end of one block and the start of another
		
		double mass=0.;
		
		if(!node.isRepetition() || checkCompositionResidue(node)){
			mass =  type.getMass();
		}
		
		// modify for alditol
		if( node.isReducingEnd() && node.getType().makesAlditol() )
			mass += 2*MassUtils.hydrogen.getMass();

		if( node.isBracket() && !node.isComposition()) {
			int no_linked_labiles = Math.min(countLabilePositions(),countDetachedLabiles());
			mass -= (no_bonds-no_linked_labiles)*substitutionMass();
		}
		else if( node.isCleavage() && !node.isRingFragment() ) {
			// cleavages have no derivatization
			if( node.isReducingEnd() && !node.hasChildren() ) {
				// fix for composition
				//mass += MassOptions.H2O;    
				mass += substitutionMass();
			}
		}
		else {
			if(node.isRepetition()==false){
				// add groups
				if( isDropped(type) )
					mass -= (type.getMass() - MassUtils.water.getMass() - substitutionMass());
				else
					mass += ((noSubstitutions(type)-no_bonds)*substitutionMass());
			}
		}    

		mass=mass*multipler;
		
		if(node.getParent()!=null){
			if(node.getParent().isStartRepetition()) {
				Residue startRepResidue=node.getParent();
				int noBonds=startRepResidue.getLinkageAt(0).getNoBonds(); //B
				int repetitions=startRepResidue.getEndRepitionResidue().getMaxRepetitions(); //n
			
				mass+=(repetitions-1)*(noBonds-1)*MassUtils.water.getMass();
				mass+=(repetitions-2)*(noBonds-1)*substitutionMass();
			}
		}
		
		// add children
		for( Linkage l : node.getChildrenLinkages() ) {
			if(l.getChildResidue().isEndCyclic()) continue;
			if(l.getChildResidue().isRepetition()){
				if(l.getChildResidue().isEndRepetition()){
					//Correct for the number of bonds the final residue actually has
					mass+=(no_bonds-(l.getChildResidue().getNoBonds()+no_bonds-2))*substitutionMass();
				}
			}
			if(isDehydrationBond(l)) {
				mass -= MassUtils.water.getMass()*l.getNoBonds()*multipler; // remove a water molecule for each bond
			}
			
			if(!l.getChildResidue().getType().getComposition().contains("O")) { 
				if(l.getParentLinkageType().equals(LinkageType.H_AT_OH) || l.getParentLinkageType().equals(LinkageType.H_LOSE)) {
					mass += MassUtils.water.getMass()*l.getNoBonds()*multipler;
					mass -= MassUtils.hydrogen.getMass()*l.getNoBonds()*multipler*2;
				}
			}
			mass += computeMass(l.getChildResidue(),multipler);
		}
		
		return mass;
	}

	private boolean checkCompositionResidue (Residue node) {
		if(node.isComposition()) {
			//if(node.isReducingEnd()) return false;
			if(node.isBracket()) return false;
			return true;
		}
		
		return false;
	}
	
	private boolean isDehydrationBond(Linkage childLinkage) {
		Residue child = childLinkage.getChildResidue();
		if(child.isRepetition()) return false;
		if(child.isComposition()) { 
			if(child.getParentsOfFragment().isEmpty()) return false;
			if(child.getParentsOfFragment().contains(child) && child.getParentsOfFragment().size() == 1) return false;
		}
		
		return true;
	}
	
	private void computeMolecule(Molecule ret, Residue node, Molecule substitution_molecule) throws Exception {
		if( node==null )
			return;

		ResidueType type = node.getType();
		int no_bonds = node.getNoBonds();

		// add residue
		if( type.getMolecule()==null )
			throw new Exception("Cannot compute molecule for residue: " + node.getTypeName());
		ret.add(type.getMolecule());    

		// modify for alditol
		if( node.isReducingEnd() && node.getType().makesAlditol() )
			ret.add(MassUtils.hydrogen,2);

		// fix for persubstitutions
		if( node.isBracket() ) {
			// modify for labiles
			int no_linked_labiles = Math.min(countLabilePositions(),countDetachedLabiles());
			ret.remove(substitution_molecule,no_bonds-no_linked_labiles);
		}
		else if( node.isCleavage() && !node.isRingFragment() ) {
			// cleavages have no derivatization        
			if( node.isReducingEnd() && !node.hasChildren() ) {
				// modify for composition
				ret.add(substitution_molecule);
			}        
		}
		else {
			if( isDropped(type) ) {
				ret.remove(type.getMolecule());
				ret.add(MassUtils.water);
				ret.add(substitution_molecule);
			}
			else 
				ret.add(substitution_molecule,noSubstitutions(type)-no_bonds);
		}    

		// add children
		for( Linkage l : node.getChildrenLinkages() ) {
			ret.remove(MassUtils.water,l.getNoBonds());        // remove a water molecule for each bond                
			computeMolecule(ret,l.getChildResidue(),substitution_molecule);
		}
	}

	private int computeNoMethylPositions(Residue node) {
		if( node==null )
			return 0;

		int ret = 0;
		int no_bonds = node.getNoBonds();

		if( node.isBracket() ) {
			int no_linked_labiles = Math.min(countLabilePositions(),countDetachedLabiles());
			ret -= (no_bonds-no_linked_labiles);
		}
		else {
			if( node.getType().isDroppedWithMethylation() )
				ret -= 1;
			else 
				ret += node.getType().getNoMethyls()-no_bonds;        
		}    

		// add children
		for( Linkage l : node.getChildrenLinkages() ) 
			ret += computeNoMethylPositions(l.getChildResidue());    

		return ret;
	}

	private int computeNoAcetylPositions(Residue node) {
		if( node==null )
			return 0;

		int ret = 0;
		int no_bonds = node.getNoBonds();

		if( node.isBracket() ) {
			int no_linked_labiles = Math.min(countLabilePositions(),countDetachedLabiles());
			ret -= (no_bonds-no_linked_labiles);
		}
		else {
			if( node.getType().isDroppedWithAcetylation() )
				ret -= 1;
			else 
				ret += node.getType().getNoAcetyls()-no_bonds;        
		}    

		// add children
		for( Linkage l : node.getChildrenLinkages() ) 
			ret += computeNoMethylPositions(l.getChildResidue());    

		return ret;
	}

	//--------------------
	// handling of labile residues   

	/**
       Return true if any of the residue can drop during
       fragmentation.
	 */
	public boolean hasLabileResidues() {
		return hasLabileResidues(root) || hasLabileResidues(bracket);
	}

	private boolean hasLabileResidues(Residue current) {
		if( current==null )
			return false;
		if( current.isLabile() ) 
			return true;

		for( Linkage l : current.getChildrenLinkages() ) 
			if( hasLabileResidues(l.getChildResidue()) )
				return true;
		return false;
	}

	/**
       Detach all labile residues in the structure, move the residues
       to the bracket and put labile cleavage markers in their place.       
	 */
	public Glycan detachLabileResidues() {
		Glycan ret = this.clone();
		ret.detachLabileResidues(ret.getRoot());    
		return ret;
	}

	private void detachLabileResidues(Residue current) {
		if( current==null )
			return;

		if( current.isLabile() ) {
			// create cleavage
			Residue l_leaf = new Residue(ResidueType.createLCleavage());
			l_leaf.setCleavedResidue(current);

			// attach cleavage to structure
			current.getParentLinkage().setChildResidue(l_leaf);
			l_leaf.setParentLinkage(current.getParentLinkage());
			current.setParentLinkage(null);

			// add to labiles set
			addAntenna(current.cloneResidue());
		}
		else {
			for( Linkage l : current.getChildrenLinkages() ) 
				detachLabileResidues(l.getChildResidue());
		}
	}


	/**
       Return the configuration of detached labile residues.
       @see #detachLabileResidues
	 */
	public TypePattern getDetachedLabilesPattern() {
		TypePattern conf = new TypePattern();
		if( bracket==null )
			return conf;

		for( Linkage l : bracket.getChildrenLinkages() ) 
			if( l.getChildResidue().isLabile() )
				conf.add(l.getChildResidue().getTypeName());
		return conf;
	}

	/**
       Return the configuration of labile residues contained in the
       structure.
       @see #detachLabileResidues
	 */
	public TypePattern getAllLabilesPattern() {
		TypePattern conf = new TypePattern();
		getAllLabilesPattern(root,conf);
		getAllLabilesPattern(bracket,conf);
		return conf;
	}

	private void getAllLabilesPattern(Residue current, TypePattern conf) {
		if( current==null )
			return;

		if( current.isLabile() )
			conf.add(current.getTypeName());

		for( Linkage l : current.getChildrenLinkages() ) 
			getAllLabilesPattern(l.getChildResidue(),conf);
	}

	/**
       Return the configuration of empty labile positions as marked by
       labile cleavages.
       @see #detachLabileResidues
	 */
	public TypePattern getLabilePositionsPattern() {
		TypePattern conf = new TypePattern();
		getLabilePositionsPattern(root,conf);
		return conf;
	}

	private void getLabilePositionsPattern(Residue current, TypePattern conf) {
		if( current==null )
			return;

		if( current.isLCleavage() )
			conf.add(current.getCleavedResidue().getTypeName());

		for( Linkage l : current.getChildrenLinkages() ) 
			getLabilePositionsPattern(l.getChildResidue(),conf);
	}

	/**
       Return the number of detached labile residues.
       @see #detachLabileResidues
	 */
	public int countDetachedLabiles() {
		int count = 0;
		if( bracket!=null ) {
			for( Linkage l : bracket.getChildrenLinkages() ) 
				if( l.getChildResidue().isLabile() )
					count++;
		}
		return count;
	}

	/**
       Count the number empty labile positions as marked by labile
       cleavages.
       @see #detachLabileResidues
	 */
	public int countLabilePositions() {
		return countLabilePositions(root) + countLabilePositions(bracket);
	}

	private int countLabilePositions(Residue current) {
		if( current==null )
			return 0;
		if( current.isLCleavage() )
			return 1;

		int count = 0;
		for( Linkage l : current.getChildrenLinkages() ) 
			count += countLabilePositions(l.getChildResidue());
		return count;
	}

	/**
       Return a copy of the current structure where all detached
       labile residues are removed from the bracket.
       @see #detachLabileResidues
	 */
	public Glycan removeDetachedLabiles() {
		Glycan ret = this.clone();
		if( ret.bracket==null ) 
			return ret;

		for( Iterator<Linkage> link_enum = ret.bracket.getChildrenLinkages().iterator(); link_enum.hasNext(); ) {
			Linkage l = link_enum.next();
			if( l.getChildResidue().isLabile() ) {
				l.setParentResidue(null);
				link_enum.remove();
			}
		}

		if( ret.bracket.getChildrenLinkages().size()==0 )
			ret.bracket = null;

		return ret;
	}

	private void removeDetachedLabile(String typename) {
		if( bracket==null ) 
			return;
		for(Linkage l : bracket.getChildrenLinkages() ) {
			if( l.getChildResidue().getTypeName().equals(typename) ) {
				bracket.removeChild(l.getChildResidue());
				break;
			}
		}
		if( !bracket.hasChildren() )
			bracket = null;    
	}

	/**
       Return all possible structure resulting from the placement of
       a combination of the labile residues contained in <code>avail_labiles</code> on
       <code>structure</code>
	 */

	static public Collection<Glycan> getAllLabilesConfigurations(Glycan structure, TypePattern avail_labiles) {
		if( structure==null )
			return new Vector<Glycan>();    
		return structure.getAllLabilesConfigurations(avail_labiles);
	}

	/**
       Return all possible structures resulting from the placement of
       a combination of the labile residues contained in this
       structure.
	 */
	public Collection<Glycan> getAllLabilesConfigurations() {
		Glycan dest = this.detachLabileResidues();
		return dest.getAllLabilesConfigurations(dest.getDetachedLabilesPattern());
	}

	protected Collection<Glycan> getAllLabilesConfigurations(TypePattern avail_labiles) {
		Vector<Glycan> ret = new Vector<Glycan>();    

		// init
		Glycan dest = this.removeDetachedLabiles();
		int no_labiles = avail_labiles.size();

		// simplest case, no labiles
		ret.add(dest);

		// general case all permutations test
		if( no_labiles>0 ) {
			TypePattern labile_pos = dest.getLabilePositionsPattern();
			for( int i=1; i<=no_labiles && i<=labile_pos.size(); i++ ) {
				if( i==labile_pos.size() )             
					ret.add(dest.reattachAllLabileResidues());
				else {
					for( TypePattern conf : labile_pos.subPatterns(i) ) 
						ret.add(dest.addLabileResidues(conf));        
				}
			}
		}

		return ret;
	}

	private Glycan addLabileResidues(TypePattern conf) {
		Glycan ret = this.clone();
		try {
			if( conf!=null ) {
				for( String typename : conf.getTypes() ) 
					ret.addAntenna(ResidueDictionary.newResidue(typename));        
			}
		}
		catch(Exception e) {
			LogUtils.report(e);
		}
		return ret;
	}

	public Glycan reattachAllLabileResidues() {
		Glycan ret = this.clone();
		ret.reattachAllLabileResidues(ret.getRoot());    
		return ret;
	}

	private void reattachAllLabileResidues(Residue current) {
		if( current==null  )
			return;

		if( current.isLCleavage() ) {

			// attach labile residue
			current.getParentLinkage().setChildResidue(current.getCleavedResidue());
			current.getCleavedResidue().setParentLinkage(current.getParentLinkage());

			// detach cleavage
			current.setParentLinkage(null);

			// remove a labile residue of the same type from bracket
			removeDetachedLabile(current.getCleavedResidue().getTypeName());
		}
		else {
			for( Linkage l : current.getChildrenLinkages() ) 
				reattachAllLabileResidues(l.getChildResidue());
		}
	}


	//------------------------
	// serialization

	/**
       Create a new glycan structure from a string.
       @see GWSParser#fromString
	 */
	static public Glycan fromString(String str) {
		try {
			return GWSParser.fromString(str,new MassOptions());
		}
		catch(Exception e) {
			LogUtils.report(e);
			return null;
		}
	}

	/**
       Create a new glycan structure from a string with the specific mass settings.
       @see GWSParser#fromString
	 */
	static public Glycan fromString(String str, MassOptions default_mass_options) {
		try {
			return GWSParser.fromString(str,default_mass_options);
		}
		catch(Exception e) {
			LogUtils.report(e);
			//Logger log=Logger.getLogger("test");
			//log.setLevel(Level.INFO);
			//log.info("BAD:"+str);
			return null;
		}
	}

	/**
       Return a string representation of the structure.
       @see GWSParser#toString
	 */
	public String toString() {
		return GWSParser.toString(this,false);
	}

	/**
       Return a string representation of the structure where each
       subtree is ordered.
       @see GWSParser#toString
	 */
	public String toStringOrdered() {
		return GWSParser.toString(this,true);
	}

	/**
       Return a string representation of the structure where each
       subtree is ordered.
       @param add_massopt if <code>false</code> do not add the mass
       settings to the string representation
       @see GWSParser#toString
	 */
	public String toStringOrdered(boolean add_massopt) {
		return GWSParser.toString(this,true,add_massopt);
	}

	/**
       Return a string representation of the structure in the GlycoCT format
       @see GlycoCTParser#toGlycoCT
	 */
	public String toGlycoCT() {
		return new GlycoCTParser(false).toGlycoCT(this);
	}

	/**
       Return a string representation of the structure in the GlycoCT
       condensed format
       @see GlycoCTCondensedParser#toGlycoCTCondensed
	 */
	public String toGlycoCTCondensed() {
		return new GlycoCTCondensedParser(false).toGlycoCTCondensed(this);
	}

	/**
       Return a {@link Sugar Sugar} object representing this structure.
       @see GlycoCTParser#toSugar
	 */
	public Sugar toSugar() throws Exception {
		return new GlycoCTParser(false).toSugar(this);
	}

	/**
       Create a new glycan structure from a string in GlycoCT format.
       @see GlycoCTParser#fromGlycoCT
	 */
	static public Glycan fromGlycoCT(String str) {
		try {
			return new GlycoCTParser(false).fromGlycoCT(str,new MassOptions());
		}
		catch(Exception e) {
			e.printStackTrace();
			LogUtils.report(e);
			return null;
		}
	}

	/**
       Create a new glycan structure from a string in GlycoCT format
       with the specific mass settings.
       @see GlycoCTParser#fromGlycoCT
	 */
	static public Glycan fromGlycoCT(String str, MassOptions default_mass_options) {
		try {
			return new GlycoCTParser(false).fromGlycoCT(str,default_mass_options);
		}
		catch(Exception e) {
			LogUtils.report(e);
			return null;
		}
	}

	/**
       Create a new glycan structure from a string in GlycoCT
       condensed format.
       @see GlycoCTCondensedParser#fromGlycoCTCondensed
	 */
	static public Glycan fromGlycoCTCondensed(String str) {
		try {
			return new GlycoCTCondensedParser(false).fromGlycoCTCondensed(str,new MassOptions());
		}
		catch(Exception e) {
			e.printStackTrace();
			LogUtils.report(e);
			return null;
		}
	}

	/**
       Create a new glycan structure from a string in GlycoCT
       condensed format.
       @param tolerate_unknown if <code>true</code> tolerate residues
       of a type that is not specified in the dictionary
       @see ResidueDictionary
       @see GlycoCTCondensedParser#fromGlycoCTCondensed
	 */
	static public Glycan fromGlycoCTCondensed(String str, boolean tolerate_unknown) {
		try {
			return new GlycoCTCondensedParser(tolerate_unknown).fromGlycoCTCondensed(str,new MassOptions());
		}
		catch(Exception e) {
			e.printStackTrace();
			LogUtils.report(e);
			return null;
		}
	}

	/**
       Create a new glycan structure from a string in GlycoCT
       condensed format with the specific mass settings.
       @see GlycoCTCondensedParser#fromGlycoCTCondensed
	 */
	static public Glycan fromGlycoCTCondensed(String str, MassOptions default_mass_options) {
		try {
			return new GlycoCTCondensedParser(false).fromGlycoCTCondensed(str,default_mass_options);
		}
		catch(Exception e) {
			LogUtils.report(e);
			return null;
		}
	}


	/**
       Create a new glycan structure from its XML representation as
       part of a DOM tree.
	 */
	static public Glycan fromXML(Node s_node, MassOptions default_mass_options) throws Exception {
		Glycan ret = fromString(XMLUtils.getAttribute(s_node,"structure"),default_mass_options);
		return ret;
	}

	/**
       Create an XML representation of the current glycan structure to
       be part of a DOM tree.
	 */
	public Element toXML(Document document) {
		if( document==null )
			return null;

		Element s_node = document.createElement("Glycan");
		if( s_node==null )
			return null;

		s_node.setAttribute("structure", toString());

		return s_node;    
	}


	/**
       Default SAX handler to read a representation of this object
       from an XML stream.
	 */    
	public static class SAXHandler extends SAXUtils.ObjectTreeHandler {    

		private MassOptions default_mass_options;

		/**
       Default constructor.
		 */
		public SAXHandler(MassOptions _default_mass_options) {
			default_mass_options = _default_mass_options;
		}

		protected boolean isElement(String namespaceURI, String localName, String qName) {
			return qName.equals(getNodeElementName());
		}

		public static String getNodeElementName() {
			return "Glycan";
		}

		protected void initContent(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
			super.initContent(namespaceURI,localName,qName,atts);
			object = Glycan.fromString(atts.getValue("structure"),default_mass_options);
		}

	}

	/**
       Write a representation of this object into an XML stream using
       a SAX handler.
	 */
	public void write(TransformerHandler th) throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("","","structure","CDATA",this.toString());
		th.startElement("","","Glycan",atts);
		th.endElement("","","Glycan");
	}

	@Override
	public boolean equals(MassAware aware){
		if((aware instanceof Glycan)==false){
			return false;
		}else{
			return toGlycoCTCondensed().equals(((Glycan)aware).toGlycoCT()) && computeMZ()==((Glycan)aware).computeMZ();
		}
	}

	@Override
	public String getName() {
		return name;
	}
	
	public void setName(String name){
		this.name=name;
	}
}
