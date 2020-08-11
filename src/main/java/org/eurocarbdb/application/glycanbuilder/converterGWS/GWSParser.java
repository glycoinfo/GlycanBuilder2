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

package org.eurocarbdb.application.glycanbuilder.converterGWS;

import java.awt.Rectangle;
import java.util.*;
import java.util.regex.*;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueHolder;
import org.eurocarbdb.application.glycanbuilder.ResiduePlacement;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParser;
import org.eurocarbdb.application.glycanbuilder.dataset.CrossRingFragmentDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Bond;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.ResAngle;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
   Read and write glycan structures in the GlycoWorkbench internal
   format.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class GWSParser implements GlycanParser {

	private static Pattern residue_pattern;
	private static Pattern link_pattern;

	static {
		String link_old_pattern_str = "-([1-9N?])";
		String link_pattern_str = "--(?:((?:[1-9N?]/)*[1-9N?]=[1-9N?]),)*((?:[1-9N?]/)*[1-9N?])";
		link_pattern = Pattern.compile("(?:" + link_old_pattern_str + ")|(?:" + link_pattern_str + ")");

		String start_repeat_str = "\\[";
		String end_repeat_str = "](?:_(-?[0-9]+))?+(?:\\^(-?[0-9]+))?+";
		String residue_str = "([abo?][1-9N?])?+([DL]-)?+([a-zA-z0-9_#=.]+)(?:,([?opf]))?+";
		String cleaved_str = "/([a-zA-z0-9_#]+)";
		String place_str = "@(-?[0-9]+s?)";
		String cord_str="<bounding_box>([0-9]+),([0-9]+),([0-9]+),([0-9]+)</bounding_box>";

		residue_pattern = Pattern.compile("(?:" + start_repeat_str + ")|(?:" + end_repeat_str + ")|" +
				"(?:" + residue_str + "(?:" + cleaved_str + ")?+)" +
				"(?:" + place_str + ")?+"+"(?:"+cord_str+")?" );
	}

	/**
       Default Constructor
	 */
	public GWSParser() {
	}

	public void setTolerateUnknown(boolean f) {
	}

	public String writeGlycan(Glycan structure) {
		return toString(structure,false,true);
	}

	@Override
	public String writeGlycan(Glycan structure, BBoxManager bboxManager) {
		return toString(structure,false,true, bboxManager);
	}

	/**
       Create a unique representation of a glycan structure using the
       lexical ordering between the children of each residue.
	 */
	public String writeGlycanOrdered(Glycan structure) {
		return toString(structure,true,true);
	}

	public Glycan readGlycan(String str, MassOptions default_mass_options) throws Exception {
		return fromString(str,default_mass_options);
	}

	static public String toString(Glycan structure) {
		return toString(structure, null);
	}

	/**
       Static method for creating string representation of glycan
       structures.
       @param structure the structure to be converted
	 */
	static public String toString(Glycan structure, BBoxManager bboxManager) {
		return toString(structure,false,true, bboxManager);
	}

	static public String toString(Glycan structure, boolean ordered) {
		return toString(structure, ordered, null);
	}

	/**
       Static method for creating string representation of glycan
       structures.
       @param structure the structure to be converted
       @param ordered <code>true</code> if the representation must use
       the lexical ordering between children
	 */
	static public String toString(Glycan structure, boolean ordered, BBoxManager bboxManager) {
		return toString(structure,ordered,true, bboxManager);
	}


	static public String toString(Glycan structure, boolean ordered, boolean add_massopt) {
		return toString(structure, ordered, add_massopt, null);
	}

	/**
       Static method for creating string representation of glycan
       structures.
       @param structure the structure to be converted
       @param ordered <code>true</code> if the representation must use
       the lexical ordering between children
       @param add_massopt <code>true</code> if the representation must
       contain the mass options
	 */
	static public String toString(Glycan structure, boolean ordered, boolean add_massopt, BBoxManager bboxManager) {
		if( structure==null )
			return "";

		StringBuilder ss = new StringBuilder();
		if( structure.getRoot()!=null ) {
			ss.append(writeSubtree(structure.getRoot(),ordered, bboxManager));
			if( structure.getBracket()!=null ) 
				ss.append(writeSubtree(structure.getBracket(),ordered, bboxManager));

			if( add_massopt ) {
				ss.append("$");
				ss.append(structure.getMassOptions().toString());                    
			}
		}
		return ss.toString();
	}

	/**
       Static method for creating glycan structures from their string representation
       @param default_mass_options the mass options to use for the new
       structure if they are not specified in the string
       representation
       @throws Exception if the string cannot be parsed    
	 */
	static public Glycan fromString(String str, MassOptions default_mass_options) throws Exception {
		str = TextUtils.trim(str);

		// read mass options
		MassOptions mass_opt = default_mass_options.clone();
		int ind1 = str.indexOf('$');
		if( ind1!=-1 ) {
			mass_opt = MassOptions.fromString(str.substring(ind1+1));       
			str = str.substring(0,ind1);
		}

		// read structure
		Glycan ret = null;
		int ind2 = str.indexOf('}');
		if( ind2==-1 ) 
			ret = new Glycan(readSubtree(str,true),false,mass_opt);
		else {
			// read structure with bracket
			ret = new Glycan(readSubtree(str.substring(0,ind2),true),
					readSubtree(str.substring(ind2),true),
					false,mass_opt);        
		}    

		return ret;
	}

	static public String writeResidueType(Residue r) {
		String str = "";

		if( r.isBracket() ) str += '}';
		else if( r.isStartRepetition() ) {
			str += '[';
		}
		else if( r.isEndRepetition() ) {
			str += ']';
			if( r.getType().getMinRepetitions()>=0 )
				str += "_" + r.getType().getMinRepetitions();
			if( r.getType().getMaxRepetitions()>=0 )
				str += "^" + r.getType().getMaxRepetitions();
		}
		else if( r.isStartCyclic()) {
			str += '>';
		}
		else if( r.isEndCyclic()) {
			str += '<';
		}
		else if( r.isCleavage() ) {
			Residue cleaved_residue = r.getCleavedResidue();
			str += writeResidueType(cleaved_residue) + "/" + r.getTypeName();    
		}
		else {
			if( r.hasAnomericState() || r.hasAnomericCarbon() )
				str += r.getAnomericState() + "" + r.getAnomericCarbon();        
			if( r.hasChirality() ) 
				str += r.getChirality() + "-";        
			str += r.getTypeName();
			if( r.hasRingSize() ) 
				str += "," + r.getRingSize();            
		}
		return str;
	}

	static public String writeSubtree(Residue r, boolean ordered) {
		return writeSubtree(r, ordered, null);
	}

	static public String writeSubtree(Residue r, boolean ordered, BBoxManager bboxManager ) {
		//------------
		// write typ
		String str = writeResidueType(r);    

		// write placement
		if( r.getCleavedResidue()!=null ) {
			Residue cleaved_residue = r.getCleavedResidue();
			if( cleaved_residue.hasPreferredPlacement() )
				str += "@" + placementToString(cleaved_residue.getPreferredPlacement());    
		}
		else { 
			if( r.hasPreferredPlacement() )
				str += "@" + placementToString(r.getPreferredPlacement());    
		}

		if(bboxManager!=null && bboxManager.border_bboxes.containsKey(r)){
			Rectangle rec=bboxManager.border_bboxes.get(r);
			str+="<bounding_box>"+rec.x+","+rec.y+","+rec.width+","+rec.height+"</bounding_box>";
		}

		//-----------------
		// write children

		ArrayList<String> str_children = new ArrayList();
		for( Linkage l : r.getChildrenLinkages() )
			str_children.add(writeSubtree(l,ordered, bboxManager));

		if( ordered ) 
			Collections.sort(str_children);    

		// add parenthesis    
		for( int i=0; i<r.getChildrenLinkages().size()-1; i++ ) 
			str += "(";       

		// write children
		for( Iterator<String> i=str_children.iterator(); i.hasNext(); ) {
			str += i.next();

			// close parenthesis
			if( i.hasNext() ) str += ")";                
		}    

		return str;
	}

	static public String writeSubtree(Linkage l, boolean ordered) {
		return writeSubtree(l, ordered, null);
	}

	static public String writeSubtree(Linkage l, boolean ordered, BBoxManager bboxManager) {
		return ("--" + toStringLinkage(l) + writeSubtree(l.getChildResidue(),ordered,bboxManager));
	}

	static public String toStringLinkage(Linkage link) {        
		StringBuilder sb = new StringBuilder();
		for( Iterator<Bond> i=link.getBonds().iterator(); i.hasNext(); ) {
			Bond b = i.next();

			if( sb.length()>0 )  sb.append(',');
			//if(b.getProbabilityHigh() < 100 && b.getProbabilityHigh() != 0)
			//	sb.insert(0, "%" + b.getProbabilityHigh() + ",");
			//if(b.getProbabilityLow() < 100 && b.getProbabilityLow() != 0)
			//	sb.insert(0, "%" + b.getProbabilityLow() + ",");
			
			// write parent positions
			char[] p_poss = b.getParentPositions();
			for( int l=0; l<p_poss.length; l++ ) {
				if( l>0 ) sb.append('/');
				sb.append(p_poss[l]);
			}

			// write child position for non-glycosidic bonds
			if( i.hasNext() ) {
				sb.append('=');
				sb.append(b.getChildPosition());
			}
		}        
		return sb.toString();
	}

	static public Residue readSubtree(String str, boolean accept_empty) throws Exception {
		return readSubtree(str, accept_empty, new ResidueHolder());
	}

	static public Residue readSubtree(String str, boolean accept_empty, ResidueHolder startRep) throws Exception {


		if( str.length()==0 ) {
			if( accept_empty ) return null;
			throw new Exception("Empty node");
		}

		Residue ret;
		if( str.charAt(0)=='}' ) {
			ret = ResidueDictionary.createBracket();
			str = str.substring(1);
		}
		else {
			//------------------
			// create residue

			Matcher m = residue_pattern.matcher(str);
			if( !m.lookingAt() ) 
				throw new Exception("Invalid format for string: " + str );
			
			if( str.charAt(0)=='[' ){
				ret = ResidueDictionary.createStartRepetition();  
				startRep.res=ret;
			}else if( str.charAt(0)==']' ) { 
				ret = ResidueDictionary.createEndRepetition(m.group(1),m.group(2));
				startRep.res.setEndRepitionResidue(ret);
				startRep.res=null;
			}
			else {
				// get stereochemistry
				char ret_anom_state = '?';
				char ret_anom_carbon = '?';
				char ret_chirality = '?';        
				if( m.group(3)!=null ) {
					ret_anom_state = m.group(3).charAt(0);
					ret_anom_carbon = m.group(3).charAt(1);
				}
				if( m.group(4)!=null )
					ret_chirality = m.group(4).charAt(0);

				// get type name
				String typename = m.group(5);

				// get ring size
				char ret_ring_size = '?';
				if( m.group(6)!=null ) 
					ret_ring_size = m.group(6).charAt(0);

				// create residue
				ret = ResidueDictionary.newResidue(typename);
				ret.setAnomericState(ret_anom_state);
				ret.setAnomericCarbon(ret_anom_carbon);
				ret.setChirality(ret_chirality);
				ret.setRingSize(ret_ring_size);

				// create cleavage
				String cleavage_typename = m.group(7);

				if( cleavage_typename!=null ) {
					Residue cleavage;
					if(  cleavage_typename.indexOf('_')!=-1 ) 
						cleavage = CrossRingFragmentDictionary.newFragment(cleavage_typename,ret);
					else                
						cleavage = ResidueDictionary.newResidue(cleavage_typename);        

					cleavage.setCleavedResidue(ret);
					ret = cleavage;
				}        
			}

			// get placement
			if( m.group(8)!=null ) {
				ResiduePlacement pref_place = placementFromString(m.group(8));
				if( ret.getCleavedResidue()!=null )
					ret.getCleavedResidue().setPreferredPlacement(pref_place);
				else
					ret.setPreferredPlacement(pref_place);
			}

			if(m.group(9)!=null && m.group(10)!=null && m.group(11)!=null && m.group(12)!=null){
				ret.setCenterPosition(new Rectangle(
						Integer.parseInt(m.group(9)), 
						Integer.parseInt(m.group(10)),
						Integer.parseInt(m.group(11)),
						Integer.parseInt(m.group(12))
						));
			}

			str = str.substring(m.end());
		}

		//-----------------
		// parse children

		// skip open parentheses
		int nopars = 0;
		for( ; nopars<str.length() && str.charAt(nopars)=='('; nopars++ );
		str = str.substring(nopars);

		// add children
		while(str.length()>0) {
			Linkage child_link = null;
			if( nopars>0 ) {
				// find subtree enclosed in parenthesis
				int ind = TextUtils.findClosedParenthesis(str);
				if( ind==-1 ) throw new Exception("Invalid string format: " + str);

				child_link = readSubtreeLinkage(str.substring(0,ind),startRep);
				str = str.substring(ind+1);
				nopars--;
			}
			else { 
				// add last child
				child_link = readSubtreeLinkage(str,startRep); 
				str = "";
			}

			// add child
			child_link.setParentResidue(ret);
			ret.getChildrenLinkages().add(child_link);
		}

		return ret;
	}



	static public Linkage readSubtreeLinkage(String str, ResidueHolder startRep) throws Exception {

		Matcher m = link_pattern.matcher(str);
		if( !m.lookingAt() ) 
			throw new Exception("invalid format for linkage: " + str);

		if( m.group(1)!=null ) {
			// old style

			// parse child
			Residue child = readSubtree(str.substring(m.end()),false,startRep);

			// create linkage
			return new Linkage(null,child,m.group(1).charAt(0));
		}

		// parse bonds
		ArrayList<Bond> ret_bonds = new ArrayList<>();
		for( int i=2; i<=m.groupCount(); i++ ) {
			String str_bond = m.group(i);
			if( i<m.groupCount() ) {
				// parse non glyco bonds
				if( str_bond!=null && str_bond.length()>0 ) {
					String[] fields = str_bond.split("=");
					char[] p_poss = parsePositions(fields[0]);
					char   c_pos  = fields[1].charAt(0);
					ret_bonds.add(new Bond(p_poss,c_pos));
				}

			}
			else {
				// parse glyco bond
				char[] p_poss = parsePositions(str_bond);
				ret_bonds.add(new Bond(p_poss,'?')); // anomeric carbon position is added later
			}
		}

		// parse child
		Residue child = readSubtree(str.substring(m.end()),false,startRep);

		// create linkage
		Linkage ret = new Linkage(null,child);
		ret.setBonds(ret_bonds);
		ret.getChildResidue().setParentLinkage(ret);

		return ret;
	}


	static private char[] parsePositions(String str) throws Exception {
		int c = 0;
		char[] ret = new char[(str.length()+1)/2];
		for( int i=0; i<str.length(); i+=2 ) {
			if( i>0 && str.charAt(i-1)!='/' )
				throw new Exception("Invalid positions string: " + str);
			ret[c++] = str.charAt(i);
		}
		return ret;
	}    


	static private String placementToString(ResiduePlacement rp) {
		if( rp==null ) return "";

		String str = "" + (rp.getPositions()[0].getIntAngle()+360);
		if( rp.isSticky() ) str += 's';
		return str;
	}

	static private ResiduePlacement placementFromString(String str) throws Exception {
		if( str.length()==0 ) return null;

		boolean _sticky = false;
		if( str.charAt(str.length()-1)=='s' ) {
			_sticky = true;
			str = str.substring(0,str.length()-1);
		}

		return new ResiduePlacement(new ResAngle(Integer.parseInt(str)),false,_sticky);
	}
}
