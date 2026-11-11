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

import org.glycoinfo.application.glycanbuilder.util.SkeletonModificationComposition;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.LinkageTypeOptimizer;

import java.awt.Rectangle;
import java.util.*;
import java.util.regex.*;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueHolder;
import org.eurocarbdb.application.glycanbuilder.ResiduePlacement;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParser;
import org.eurocarbdb.application.glycanbuilder.dataset.CrossRingFragmentDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Bond;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.ResAngle;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;
import org.glycoinfo.application.glycanbuilder.dataset.CrossLinkedSubstituentDictionary;

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
		// Ring forms: pyranose, furanose, alditol and the open chain, which a residue can be set to
		// through setRingSize and which this used to write without being able to read back.
		// A residue name. Two forms are admitted beyond the historical character class, because the
		// writer has always been able to emit them and this reader could not read them back - drawing
		// a heptose, saving and reopening lost the file (#256). See docs/gws-format.md 1.3.
		//
		//   -   when it is not the start of a linkage. "--" opens one and "-[1-9N?]" is the pre-2007
		//       form, so both are excluded and nothing that reads today stops reading. No name in any
		//       dictionary has a "-" followed by a digit, N or ?, so the exclusion costs nothing.
		//   (c) a parenthesised single character, for (S)Lac, (R)Lac and (X)Lac. Deliberately narrow:
		//       parentheses delimit branches, and a branch always opens "(--", so this cannot collide
		//       with one. Admitting them unconstrained does collide - a name then reads as "Man(".
		//
		// The a-zA-z span is left as it is. It is a 2007 typo spanning ASCII 65-122, so it also admits
		// [ \ ] ^ and `; narrowing it to a-zA-Z would be correct and would stop reading files that
		// read today, which is the one thing this format may not do.
		String name_str = "((?:[a-zA-z0-9_#=.]|-(?![-0-9N?])|\\([A-Za-z0-9]\\))+)";
		String residue_str = "([abo?][1-9N?])?+([DL]-)?+" + name_str + "(?:,([?opfa]))?+";
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

	/**
	 * Where the root's name ends: at the first "--", which begins its first linkage, or at the "(" of
	 * "(--" before it, which begins a branch. A name cannot hold "--" (ResidueType.escapeAglyconName).
	 * With no "--" the root is alone, which only a composition's is - a structure of one reducing end
	 * is no structure, and text of no "--" at all is more likely not GWS than an aglycon. Then, unless
	 * the file is of the pre-2007 form, "-4" for "--4", which is read as it always was, the whole of it
	 * is the root's; otherwise -1.
	 */
	private static int endOfRoot(String _tree, boolean _beforeABracket) {
		int dash = _tree.indexOf("--");
		if( dash>0 )
			return ( _tree.charAt(dash-1)=='(' ) ? dash-1 : dash;
		if( dash==0 || _tree.isEmpty() || !_beforeABracket )
			return -1;

		Matcher m = residue_pattern.matcher(_tree);
		if( m.lookingAt() ) {
			String rest = _tree.substring(m.end());
			if( rest.matches("^\\(?-[1-9N?].*") )
				return -1;
		}
		return _tree.length();
	}

	/**
	 * Whether a root reads as the residue it has always read as: the residue grammar takes all of it,
	 * and names something a dictionary has - freeEnd, redEnd, 2AB, a cleavage. Anything else is an
	 * aglycon named by the whole of it, "Ser/Thr=87.0320u", "Cer(d18:1/16:0)", "D-Ser", as GlycanCore
	 * reads it (#305), where the grammar used to take it apart: "/" as a cleavage, "D-" as a
	 * configuration, "," as a ring.
	 */
	private static boolean readsAsAResidue(String _root) {
		Matcher m = residue_pattern.matcher(_root);
		if( !m.matches() || m.group(5)==null ) return false;
		String typename = m.group(5);
		if( m.group(4)!=null && !isKnownResidue(typename) && isKnownResidue(m.group(4) + typename) )
			typename = m.group(4) + typename;
		return isKnownResidue(typename);
	}

	/** Whether either dictionary has a residue of this name. */
	private static boolean isKnownResidue(String _name) {
		return ResidueDictionary.hasResidueType(_name)
				|| org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary.hasResidueType(_name);
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
		String tree = ( ind2==-1 ) ? str : str.substring(0,ind2);

		// A root that does not read as a residue is an aglycon, by the whole of its name (#305).
		ResidueType aglycon = null;
		int rootEnd = endOfRoot(tree, ind2!=-1);
		if( rootEnd>0 && !readsAsAResidue(tree.substring(0,rootEnd)) ) {
			aglycon = ResidueDictionary.findReducingEndType(tree.substring(0,rootEnd));
			tree = "freeEnd" + tree.substring(rootEnd);
		}
		Residue treeRoot = readSubtree(tree,true);
		if( aglycon!=null && treeRoot!=null )
			treeRoot.setType(aglycon);

		if( ind2==-1 )
			ret = new Glycan(treeRoot,false,mass_opt);
		else {
			// read structure with bracket
			ret = new Glycan(treeRoot,
					readSubtree(str.substring(ind2),true),
					false,mass_opt);
		}

		// The reducing end's type implies the sugar's form: redEnd and every reductive-amination
		// label reduce their sugar, leaving it acyclic. A file saved since the ring letter began
		// carrying that fact says ",o" and needs nothing here; one saved before 1.30.0 - or written
		// by hand - says ",p", and reading it back turned the alditol into a ring again, quietly:
		// the WURCS reverted from [h2122h] to a cyclic residue and the mass lost the reduction's
		// two hydrogens (#133). This is setReducingEndType's own rule, applied on the way in.
		Residue root = ret.getRoot();
		if( root!=null && root.isReducingEnd() && !root.isCleavage()
				&& root.getNoChildren()>0 && root.getType().makesAlditol() ) {
			Residue sugar = root.getChildAt(0);
			if( sugar.isSaccharide() && !sugar.isAlditol() ) {
				sugar.setAnomericState('?');
				sugar.setRingSize('o');
			}
		}

		// Deoxies written as children "--6m" go back onto their residue (#258, #263). Before the
		// linkage types are worked out, which would otherwise be worked out for children that are
		// about to go.
		foldDeoxies(ret.getRoot());
		foldDeoxies(ret.getBracket());

		// The repeat markers carry the carbon the repeat closes on (#301)
		giveRepeatsTheirCarbon(ret);

		// Say what each bond is made of, rather than leaving every one of them UNVALIDATED (#4).
		// A structure read from WURCS has its linkage types worked out; one read from GWS did not,
		// so the same glycan carried different bonds depending on which format it arrived in, and
		// anything that reads a linkage type - an exporter, a renderer - was reading a placeholder.
		// The same pass the WURCS writer runs is applied here, on the way in.
		new LinkageTypeOptimizer().start(ret);

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
		return writeSubtree(r, ordered, bboxManager,
				java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Residue, Boolean>()));
	}

	static private String writeSubtree(Residue r, boolean ordered, BBoxManager bboxManager,
			java.util.Set<Residue> visited) {
		// The walk below assumes a tree. Hand it a cyclic graph - which a faulty import can build -
		// and it used to descend forever, dying as a StackOverflowError far from the cause (#125).
		// GWS has no spelling for a residue that is its own ancestor, so this says so instead.
		if (!visited.add(r))
			throw new IllegalArgumentException("the structure contains a cycle"
					+ " (a residue reachable from itself), which GWS cannot write");

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

		// A composition carries a marker saying that no linkages among its members are known. It
		// is a label, not a residue, and GWS has no spelling for it: written out like a child it
		// became "--?no glycosidic linkages", where the parser reads everything after "--?" as a
		// linkage, meets a sentence and stops - so a composition's own GWS could not be read back
		// (#162). It is left out here, as it is left out of the drawing and of the mass.
		ArrayList<String> str_children = new ArrayList();
		boolean deoxiesWritten = !r.hasSkeletonModifications();
		for( Linkage l : r.getChildrenLinkages() ) {
			if( l.getChildResidue()!=null && l.getChildResidue().isCompositionMarker() )
				continue;
			// before the first monosaccharide child, which is where the WURCS reader used to attach them
			if( !deoxiesWritten && l.getChildResidue()!=null && l.getChildResidue().isSaccharide() ) {
				str_children.addAll(deoxiesAsChildren(r));
				deoxiesWritten = true;
			}
			str_children.add("--" + toStringLinkage(l)
					+ writeSubtree(l.getChildResidue(), ordered, bboxManager, visited));
		}
		if( !deoxiesWritten )
			str_children.addAll(deoxiesAsChildren(r));

		if( ordered ) 
			Collections.sort(str_children);    

		// add parenthesis - one fewer than the children actually written, which is not necessarily
		// how many the residue has
		for( int i=0; i<str_children.size()-1; i++ ) 
			str += "(";       

		// write children
		for( Iterator<String> i=str_children.iterator(); i.hasNext(); ) {
			str += i.next();

			// close parenthesis
			if( i.hasNext() ) str += ")";                
		}    

		return str;
	}

	/**
	 * A residue's deoxies, written the way this file has always carried them: as children, "--6m".
	 *
	 * <p>Before #258 and #263 the WURCS reader built a deoxy the residue's name does not imply as a
	 * child residue named "m" or "d", and GWS wrote it like any child. The model keeps it on the residue
	 * now, which GWS has no other way to say - so it is written exactly as it was, and {@link
	 * #foldDeoxies} takes it back onto the residue when the file is read. A file is the same text
	 * whichever version wrote it, and one written before the change reads correctly after it.
	 */
	static private ArrayList<String> deoxiesAsChildren(Residue r) {
		ArrayList<String> ret = new ArrayList<String>();
		// and the carbons of a double bond, "--4e" and "--5E", the same way (#292). GWS has no other way
		// to say one; an older reader sees a child it does not know, as it did a deoxy's
		ArrayList<String> carbons = new ArrayList<String>(r.getDeoxies());
		carbons.addAll(r.getDoubleBonds());
		for( String deoxy : carbons ) {
			Character position = SkeletonModificationComposition.positionOf(deoxy);
			Character code = SkeletonModificationComposition.codeOf(deoxy);
			if( position!=null && code!=null )
				ret.add("--" + position + code);
		}
		return ret;
	}

	/**
	 * Tell each repeat's markers which residue the repeat starts with, and the carbon it closes on.
	 *
	 * <p>The WURCS writer closes a repeat on the residue its end marker names as the repeat's first,
	 * at the carbon the markers hold as their anomeric carbon. Drawing a repeat sets both, and so does
	 * reading one from WURCS. GWS writes "[" and "]" with neither, and reading it paired the two markers
	 * and no more. So a repeat read from GWS was written open - {@code c4~n}, with nothing on its other
	 * side, a WURCS that WURCSFramework calls invalid - and a structure with a repeat, saved and opened
	 * again, could not be exported (#301).
	 */
	static void giveRepeatsTheirCarbon(Glycan _structure) {
		for (Residue marker : _structure.getAllResidues()) {
			if (!marker.isStartRepetition() && !marker.isEndRepetition()) continue;

			Residue start = marker.isStartRepetition() ? marker : startOf(marker);
			Residue first = firstOf(start);
			if (first == null) continue;

			if (marker.isEndRepetition() && marker.getStartResidue() == null) marker.setStartResidue(first);
			if (marker.isStartRepetition() && first.getStartRepetitionResidue() == null)
				first.setStartRepetiionResidue(marker);

			char carbon = first.getAnomericCarbon();
			if (marker.getAnomericCarbon() != '?' || !Character.isDigit(carbon)) continue;
			marker.setAnomericCarbon(carbon);
			if (marker.getParentLinkage() != null) marker.getParentLinkage().setAnomericCarbon(carbon);
		}
	}

	/** The start of the repeat an end marker closes: the one it was paired with, or the nearest above it. */
	private static Residue startOf(Residue _end) {
		if (_end.getStartRepetitionResidue() != null) return _end.getStartRepetitionResidue();
		for (Residue up = _end.getParent(); up != null; up = up.getParent())
			if (up.isStartRepetition()) return up;
		return null;
	}

	/** A repeat's first monosaccharide, or null when there is none. */
	private static Residue firstOf(Residue _start) {
		if (_start == null) return null;
		for (Linkage link : _start.getChildrenLinkages()) {
			Residue first = link.getChildResidue();
			if (first.isSaccharide() && !first.isRepetition()) return first;
		}
		return null;
	}

	/**
	 * Put back onto their residue the deoxies GWS carries as children - see {@link #deoxiesAsChildren}.
	 *
	 * <p>A child is taken to be one when it is named as a deoxy code, "d" or "m", has nothing attached to
	 * it, is not a monosaccharide, hangs off a monosaccharide, and is attached at a single position. No
	 * dictionary has a residue of either name, so nothing that is a residue can be mistaken for one.
	 */
	/** A deoxy, d or m, or a carbon of a double bond, e E z Z - each written as a child of one letter. */
	private static boolean isSkeletonCode(char c) {
		return SkeletonModificationComposition.isDeoxy(c) || SkeletonModificationComposition.isDoubleBond(c);
	}

	static void foldDeoxies(Residue r) {
		if( r==null ) return;

		for( Linkage l : new ArrayList<Linkage>(r.getChildrenLinkages()) ) {
			Residue child = l.getChildResidue();
			if( r.isSaccharide() && child!=null && !child.isSaccharide() && !child.hasChildren()
					&& child.getTypeName().length()==1
					&& isSkeletonCode(child.getTypeName().charAt(0))
					&& l.getBonds().size()==1 ) {
				String carbon = l.getParentPositionsSingle() + "*" + child.getTypeName();
				if( SkeletonModificationComposition.isDeoxy(child.getTypeName().charAt(0)) ) r.addDeoxy(carbon);
				else r.addDoubleBond(carbon);
				r.getChildrenLinkages().remove(l);
				continue;
			}
			foldDeoxies(child);
		}
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

				// A residue whose own name begins with a configuration - D-gro-D-galHep, L-gro-D-manNon -
				// is written with no prefix when its configuration is unknown, and the "D-" was then
				// read as the configuration: the residue came back as an unknown "gro-D-galHep" of no
				// mass, though the text written back was the same (#252). The prefix is taken as part
				// of the name only when the name without it is no residue and the name with it is one,
				// so nothing that read as a known residue before reads differently now.
				if( m.group(4)!=null && !isKnownResidue(typename) && isKnownResidue(m.group(4) + typename) ) {
					typename = m.group(4) + typename;
					ret_chirality = '?';
				}

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

			// Try to change Substituent to CrossLinkedSubstituent
			if ( ret.getType().getSuperclass().equals("Substituent")
			  && CrossLinkedSubstituentDictionary.hasCrossLinkedSubstituent(ret.getTypeName()) )
					ret.setType(CrossLinkedSubstituentDictionary.getCrossLinkedSubstituent(ret.getTypeName()));

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
			// old style: "-4" where a file written today says "--4". Read so that files from before the
			// double dash still open, which the name grammar above goes out of its way to protect.

			// parse child
			Residue child = readSubtree(str.substring(m.end()),false,startRep);

			// create linkage, and tell the child about it. The modern branch below always did the
			// second half and this one never did, so the child came back with no parent linkage and
			// the first thing to ask for it - LinkageTypeOptimizer, on every load - threw a
			// NullPointerException that fromString swallowed. The file opened as empty, with no
			// message (#274).
			Linkage ret = new Linkage(null,child,m.group(1).charAt(0));
			child.setParentLinkage(ret);

			return ret;
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
