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

import org.eurocarbdb.application.glycanbuilder.massutil.MassUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.Molecule;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
   This class contains all the static information about a residue.  A
   residue can be any component of a glycan molecule that can be
   represented visually. Saccharides, substituents, modifications,
   cleavage points, reducing end markers, etc, are all valid residue
   types.

   @see Residue

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class ResidueType {

	//

	protected String  name;
	protected String  superclass;
	protected String  composition_class;
	protected String  composition;
	protected String[] synonyms;
	protected String  iupac_name;
	protected char    anomeric_carbon;
	protected char    chirality;
	protected char    ring_size;
	protected boolean is_saccharide;
	protected boolean is_cleavable;
	protected boolean is_labile;
	protected int     bar_order;
	protected double  res_mass_main;
	protected double  res_mass_avg;
	protected int     nmethyls;
	protected boolean drop_methylated;
	protected int     nacetyls;
	protected boolean drop_acetylated;
	protected int     nlinkages;
	protected char[]  linkage_pos;
	protected char[]  charges_pos;
	protected boolean make_alditol;
	protected boolean can_redend;
	protected boolean can_parent;
	protected String  description;
	/** WURCS MAP code of a substituent or bridge, empty when the type has none. */
	protected String  map = "";
	/**
	 * Whether this is an aglycon of the user's, a reducing end no dictionary defines: one given a name
	 * and a mass, "Ser=87.0320u", or one known only by its name, "Ser" (#305).
	 */
	protected boolean is_aglycon = false;
	/** Whether this aglycon's mass is not known - "Ser/Thr=NaNu", or a name with no mass at all. */
	protected boolean mass_unknown = false;
	/** Whether this aglycon's name carries its mass, "=87.0320u" or "=NaNu", as one the user named does. */
	protected boolean named_mass = false;

	protected Molecule molecule;

	//--

	/**
       Empty constructor.
	 */
	 public ResidueType() {
		name = "#empty";
		superclass = "special";    
		composition_class = null;
		composition = "?";
		synonyms = new String[0];
		iupac_name = "-";
		anomeric_carbon = '?';
		chirality = '?';
		ring_size = '?';
		is_saccharide = false;
		is_cleavable = false;
		is_labile = false;
		bar_order = 0;
		res_mass_main = 0.;
		res_mass_avg = 0.;
		nmethyls = 0;
		drop_methylated = false;
		nacetyls = 0;
		drop_acetylated = false;
		nlinkages = 1;    
		linkage_pos = new char[0];
		charges_pos = new char[0];
		make_alditol = false;
		can_redend = false;
		can_parent = false;
		description = "Empty";    
		molecule = null;
	 }

	 /**
       Create a new residue type from an initialization string.
	  */
	 public ResidueType(String init) throws Exception {
		 LinkedList<String> tokens = TextUtils.tokenize(init,"\t");
		 // The MAP is an optional last column: substituents and bridges carry one, everything else
		 // (and any dictionary written before the column existed) simply stops at the description.
		 if( tokens.size()!=24 && tokens.size()!=25 ) throw new Exception("Invalid string format: " + init);

		 name            = tokens.get(0);
		 superclass      = tokens.get(1);
		 composition_class = tokens.get(2);
		 composition     = tokens.get(3);
		 synonyms        = parseStringArray(tokens.get(4));
		 iupac_name      = tokens.get(5);
		 anomeric_carbon = tokens.get(6).charAt(0);
		 chirality       = tokens.get(7).charAt(0);
		 ring_size       = tokens.get(8).charAt(0);
		 is_saccharide   = parseBoolean(tokens.get(9));
		 is_cleavable    = parseBoolean(tokens.get(10));
		 is_labile       = parseBoolean(tokens.get(11));
		 bar_order       = Integer.parseInt(tokens.get(12));
		 nmethyls        = Integer.parseInt(tokens.get(13));
		 drop_methylated = parseBoolean(tokens.get(14));
		 nacetyls        = Integer.parseInt(tokens.get(15));
		 drop_acetylated = parseBoolean(tokens.get(16));
		 nlinkages       = Integer.parseInt(tokens.get(17));    
		 linkage_pos     = parseCharArray(tokens.get(18));
		 charges_pos     = parseCharArray(tokens.get(19));
		 make_alditol    = parseBoolean(tokens.get(20));
		 can_redend      = parseBoolean(tokens.get(21));
		 can_parent      = parseBoolean(tokens.get(22));
		 description     = tokens.get(23);
		 map             = (tokens.size()==25) ? tokens.get(24) : "";
		 res_mass_main   = 0.;
		 res_mass_avg    = 0.;
		 
		 this.updateMolecule();
		 
		 if( bar_order>9 || bar_order<0 ) 
			 throw new Exception("Invalid toolbar order: " + bar_order);    
	 }

	 protected boolean parseBoolean(String str) {
		 return (str.equals("true") || str.equals("yes"));
	 }

	 protected char[] parseCharArray(String str) throws Exception {
		 if( str.equals("-") || str.equals("none") || str.equals("empty") )
			 return new char[0];

		 // tokenize the array

		 LinkedList<String> tokens = TextUtils.tokenize(str,",");

		 int ind = 0;
		 char[] pos = new char[tokens.size()];
		 for( Iterator<String> i=tokens.iterator(); i.hasNext(); ind++ ) {
			 String token = i.next();
			 if( token.length()!=1 ) 
				 throw new Exception("Linkage position must be a single char");
			 pos[ind] = token.charAt(0);
		 }
		 return pos;
	 }

	 protected String[] parseStringArray(String str) throws Exception {
		 if( str.equals("-") || str.equals("none") || str.equals("empty") )
			 return new String[0];

		 // tokenize the array    
		 return TextUtils.tokenize(str,",").toArray(new String[0]);
	 }

	 /**
       Create a generic saccharide type with unspecified mass and name
       <code>_name</code>
	  */
	 static public ResidueType createSaccharide(String _name) {
		 ResidueType ret = new ResidueType();
		 ret.name = _name;
		 ret.superclass = "Saccharide";
		 ret.is_saccharide = true;
		 ret.is_cleavable = true;
		 ret.nlinkages = 5;    
		 ret.linkage_pos = new char[] {'1','2','3','4','6'};
		 ret.can_redend = false;
		 ret.can_parent = true;
		 ret.description = _name;
		 return ret;
	 }

	 /**
       Create a generic residue type with unspecified mass and name
       <code>_name</code>
	  */
	 static public ResidueType createUnknown(String _name) {
		 ResidueType ret = new ResidueType();
		 ret.name = _name;
		 ret.superclass = "Unknown";
		 ret.nlinkages = 10;    
		 ret.linkage_pos = new char[] {'1','2','3','4','6','7','8','9','N'};
		 ret.can_redend = true;
		 ret.can_parent = true;
		 ret.description = _name;
		 return ret;
	 }

	/**
	 Create a generic residue type with unspecified mass and name
	 <code>_name</code>
	 */
	static public ResidueType createAssigned (String _name) {
		ResidueType ret = new ResidueType();
		ret.name = _name;
		ret.superclass = "Assigned";
		ret.nlinkages = 0;
		ret.linkage_pos = new char[] {};
		ret.can_redend = false;
		ret.can_parent = true;
		ret.description = _name;
		ret.is_saccharide = true;
		return ret;
	}

	 /**
       Create a generic substituent type with unspecified mass and name
       <code>_name</code>
	  */
	 static public ResidueType createSubstituent(String _name) {
		 ResidueType ret = new ResidueType();
		 ret.name = _name;
		 ret.superclass = "Substituent";
		 ret.is_cleavable = true;
		 ret.nlinkages = 1;    
		 ret.linkage_pos = new char[] {'1'};
		 ret.can_redend = false;
		 ret.can_parent = false;
		 ret.description = _name;
		 return ret;
	 }

	 /**
       Create a free reducing end type.
	  */
	 static public ResidueType createFreeReducingEnd() {
		 ResidueType ret = new ResidueType();
		 ret.name = "freeEnd";
		 ret.superclass = "Reducing end";
		 ret.composition_class = "freeEnd";
		 ret.composition = "H2O";
		 ret.nmethyls = 2;
		 ret.nacetyls = 2;
		 ret.nlinkages = 1;    
		 ret.can_redend = true;
		 ret.can_parent = true;
		 ret.description = "Free reducing end";
		 ret.updateMolecule();
		 return ret;
	 }   

	 /**
       Create a generic reducing end type with name <code>name</code>
       and mass <code>mass</code>.

       <p>The name is the user's - an aglycon, "Ser/Thr", "Cer(d18:1/16:0)" - and is kept in the type's
       name as typed, which is what GWS writes, but for the few characters GWS cannot hold anywhere
       (see {@link #escapeAglyconName}). {@link #getResidueName} gives it back as typed. A mass that is
       not known, as for Ser/Thr, is {@link Double#NaN}.
	  */
	 static public ResidueType createOtherReducingEnd(String name, double mass) {
		 name = TextUtils.trim(name);
		 if( name==null || name.length()==0 )
			 name = "XXX";

		 return otherReducingEnd(escapeAglyconName(name), mass);
	 }

	 /**
       The reducing end a stored name stands for: "Ser/Thr=87.0320u", as GWS writes it, whose name is
       already escaped where it needs to be and is not escaped again.
	  */
	 static public ResidueType otherReducingEndFromStoredName(String storedName, double mass) {
		 return otherReducingEnd(storedName, mass);
	 }

	 private static ResidueType otherReducingEnd(String escapedName, double mass) {
		 ResidueType ret = aglycon(escapedName + "=" + massText(mass) + "u", mass);
		 ret.named_mass = true;
		 ret.description = "Other reducing end";
		 return ret;
	 }

	 /**
       A reducing end known only by its name, as GlycanCore reads one: a root no dictionary has, "Ser",
       "Cer(d18:1/16:0)", is the aglycon of that name, whose mass is not known (#305). The name is kept
       as it was read and written back the same.
	  */
	 static public ResidueType createUndefinedReducingEnd(String storedName) {
		 ResidueType ret = aglycon(storedName, Double.NaN);
		 ret.description = "Undefined reducing end";
		 return ret;
	 }

	 /**
       An aglycon of the user's. One whose mass is not known weighs as nothing but its water, the
       glycan's own mass, and says so through {@link #hasUnknownMass}: a NaN would reach every sum the
       structure is part of, and a figure is not shown for it anyway.
	  */
	 private static ResidueType aglycon(String name, double mass) {
		 ResidueType ret = new ResidueType();
		 ret.name = name;
		 ret.superclass = "Reducing end";
		 ret.is_aglycon = true;
		 ret.mass_unknown = Double.isNaN(mass);
		 double known = ret.mass_unknown ? 0. : mass;
		 ret.res_mass_main = known + MassUtils.water.getMainMass();
		 ret.res_mass_avg = known + MassUtils.water.getAverageMass();
		 ret.nmethyls = 1;
		 ret.nacetyls = 1;
		 ret.nlinkages = 1;
		 ret.can_redend = true;
		 ret.can_parent = true;
		 return ret;
	 }

	 /**
       Create a generic residue type with name <code>name</code>
       and mass <code>mass</code>.
	  */
	 static public ResidueType createOtherResidue(String name, double mass) {
		 name = TextUtils.trim(name);
		 if( name==null || name.length()==0 )
			 name = "Or";

		 ResidueType ret = new ResidueType();
		 ret.name = name +"=" + new java.text.DecimalFormat("0.0000").format(mass) + "u";
		 ret.superclass = "Residue";
		 ret.res_mass_main = mass + MassUtils.water.getMainMass();
		 ret.res_mass_avg = mass + MassUtils.water.getAverageMass();
		 ret.nmethyls = 1;
		 ret.nacetyls = 1;
		 ret.nlinkages = 1;    
		 ret.can_redend = false;
		 ret.can_parent = true;
		 ret.description = "Other residue";
		 return ret;
	 }


	 /**
       Create an attach point type, used only for drawing purposes.
	  */
	 static public ResidueType createAttachPoint() {
		 ResidueType ret = new ResidueType();
		 ret.name = "#attach";
		 ret.description = "Attach Point";
		 ret.composition = "0";
		 ret.nlinkages = 2;
		 ret.can_parent = true;
		 ret.can_redend = false;
		 ret.updateMolecule();
		 return ret;
	 }  

	 /**
       Create a bracket type, used for creating containers of residues
       with unspecified connectivity.
	  */
	 static public ResidueType createBracket() {
		 ResidueType ret = new ResidueType();
		 ret.name = "#bracket";
		 ret.description = "Bracket";
		 ret.composition = "0";
		 ret.nlinkages = 99;
		 ret.updateMolecule();
		 return ret;
	 }

	 /**
       Create a Y cleavage type.
	  */
	 static public ResidueType createYCleavage() {
		 ResidueType ret = new ResidueType();
		 ret.name = "#ycleavage";
		 ret.description = "Y Cleavage";
		 ret.superclass  = "cleavage";
		 ret.composition = "H2O";
		 ret.can_redend = false;
		 ret.can_parent = true;
		 ret.updateMolecule();
		 return ret;
	 }    

	 /**
       Create a B cleavage type.
	  */
	 static public ResidueType createBCleavage() {
		 ResidueType ret = new ResidueType();
		 ret.name = "#bcleavage";
		 ret.description = "B Cleavage";
		 ret.superclass  = "cleavage";
		 ret.composition = "0";
		 ret.can_redend = true;
		 ret.can_parent = false;
		 ret.updateMolecule();
		 return ret;
	 }    

	 /**
       Create a Z cleavage type.
	  */
	 static public ResidueType createZCleavage() {
		 ResidueType ret = new ResidueType();
		 ret.name = "#zcleavage";
		 ret.description = "Z Cleavage";
		 ret.superclass  = "cleavage";
		 ret.composition = "";
		 ret.can_redend = false;
		 ret.can_parent = true;
		 ret.updateMolecule();
		 return ret;
	 }    

	 /**
       Create a C cleavage type.
	  */
	 static public ResidueType createCCleavage() {
		 ResidueType ret = new ResidueType();
		 ret.name = "#ccleavage";
		 ret.description = "C Cleavage";
		 ret.superclass  = "cleavage";
		 ret.composition = "H2O";
		 ret.can_redend = true;
		 ret.can_parent = false;
		 ret.updateMolecule();
		 return ret;
	 }    

	 /**
       Create an L cleavage type, used for labile residues.
	  */
	 static public ResidueType createLCleavage() {
		 ResidueType ret = new ResidueType();
		 ret.name = "#lcleavage";
		 ret.description = "L Cleavage";
		 ret.superclass  = "cleavage";
		 ret.composition = "H2O";
		 ret.can_redend = false;
		 ret.can_parent = true;
		 ret.updateMolecule();
		 return ret;
	 }    

	 /**
       Create a start repetition type, used to identify the begin of a
       repeat block.
	  */
	 static public ResidueType createStartRepetition() {
		 ResidueType ret = new ResidueType();
		 ret.name = "#startrep";       
		 ret.description = "Start repetition";
		 ret.nlinkages = 2    ;
		 ret.can_parent = true;
		 return ret;
	 }  

	 /**
       Create an end repetition type, used to identify the begin of a
       repeat block.
	  */
	 static public ResidueType createEndRepetition() {
		 return createEndRepetition(null,null);
	 }

	 /**
       Create an end repetition type for a repeat block with range
       between <code>min</code> and <code>max</code>.
	  */
	 static public ResidueType createEndRepetition(String min, String max) {
		 ResidueType ret = new ResidueType();
		 ret.name = "#endrep";
		 ret.name += "_" + (TextUtils.isPositiveInteger(min) ?min :"?");
		 ret.name += "_" + (TextUtils.isPositiveInteger(max) ?max :"?");
		 ret.description = "End repetition";
		 ret.nlinkages = 999;
		 ret.can_parent = true;
		 return ret;
	 }  

	 /**
       Return the lower bound of a repeat block range, applies only to
       end repetition types.
       @return -1 if the type is not an end repetition.
	  */
	 public int getMinRepetitions() {
		 if( !isEndRepetition() )
			 return -1;

		 String min_str = TextUtils.tokenize(name,"_").get(1);
		 if( min_str.equals("?") )
			 return -1;
		 return Integer.parseInt(min_str);
	 }

	 /**
       Set the lower bound of a repeat block range, applies only to
       end repetition types.
	  */
	 public void setMinRepetitions(String min) {
		 if( !isEndRepetition() )
			 return;

		 String new_name = "#endrep";
		 new_name += "_" + (TextUtils.isPositiveInteger(min) ?min :"?");
		 new_name += "_" + getMaxRepetitions();

		 name = new_name;
	 }

	 /**
       Return the upper bound of a repeat block range, applies only to
       end repetition types.
       @return -1 if the type is not an end repetition.
	  */
	 public int getMaxRepetitions() {
		 if( !isEndRepetition() )
			 return -1;

		 String max_str = TextUtils.tokenize(name,"_").get(2);
		 if( max_str.equals("?") )
			 return -1;
		 return Integer.parseInt(max_str);
	 }


	 /**
       Set the upper bound of a repeat block range, applies only to
       end repetition types.
	  */
	 public void setMaxRepetitions(String max) {
		 if( !isEndRepetition() )
			 return;

		 String new_name = "#endrep";
		 new_name += "_" + getMinRepetitions();
		 new_name += "_" + (TextUtils.isPositiveInteger(max) ?max :"?");

		 name = new_name;
	 }

	 static public ResidueType createStartCyclic() {
		 ResidueType ret = new ResidueType();
		 ret.name = "#startcyclic";       
		 ret.superclass = "Cyclic";
		 ret.description = "Start cyclic";
		 ret.nlinkages = 2    ;
		 ret.can_parent = true;
		 ret.can_redend = false;
		 return ret;
	 }
	 
	 static public ResidueType createEndCyclic() {
		 ResidueType ret = new ResidueType();
		 ret.name = "#endcyclic";
		 ret.superclass = "Cyclic";
		 ret.description = "End cyclic";
		 ret.nlinkages = 999;
		 ret.can_parent = false;
		 return ret;
	 }
	 
	 static public ResidueType createAlternativeStart() {
		 ResidueType a_oAltStart = new ResidueType();
		 a_oAltStart.name = "#altstart";
		 a_oAltStart.superclass = "Alternative";
		 a_oAltStart.description = "Alternative Start";
		 a_oAltStart.can_parent = true;
		 a_oAltStart.can_redend = false;
		 
		 return a_oAltStart;
	 }
	 
	 static public ResidueType createAlternativeEnd() {
		 ResidueType a_oAltEnd = new ResidueType();
		 a_oAltEnd.name = "#altend";
		 a_oAltEnd.superclass = "Alternative";
		 a_oAltEnd.description = "Alternative End";
		 a_oAltEnd.nlinkages = 1;
		 a_oAltEnd.can_parent = true;
		 
		 return a_oAltEnd;
	 }
	 
	 /**
       Return <code>true</code> if this type represent a free reducing
       end.
	  */
	 public boolean isFreeReducingEnd() {
		 return name.equals("freeEnd");
	 }

	 /**
       Return <code>true</code> if this type represent a unmodified
       reducing end marker.
	  */
	 public boolean isReducingEndMarker() {
		 return name.equals("#freeEnd") || name.equals("#redEnd");
	 }

	 /**
       Return <code>true</code> if this type represent an attach point.
	  */
	 public boolean isAttachPoint() {
		 return name.equals("#attach");
	 }

	 /**
       Return <code>true</code> if this type represent a saccharide.
	  */
	 public boolean isSaccharide() {
		 return is_saccharide;
	 }

	 /**
       Return <code>true</code> if a residue of this type can be
       cleaved off the structure.
	  */
	 public boolean isCleavable() {
		 return is_cleavable;
	 }


	 /**
       Return <code>true</code> if a residue of this type is labile
       during fragmentation.
	  */
	 public boolean isLabile() {
		 return is_labile;
	 }

	 /**
       Return <code>true</code> if this type represent a substituent.
	  */
	 public boolean isSubstituent() {
		 return superclass.equals("Substituent");
	 }

	/**
       Return <code>true</code> if this type represent special residue
       type.
	  */
	 public boolean isSpecial() {
		 return name.startsWith("#");
	 }    

	 /**
       Return <code>true</code> if this type represent the beginning
       or the end of a repeat block.
	  */
	 public boolean isRepetition() {
		 return (isStartRepetition() | isEndRepetition());
	 }

	 /**
       Return <code>true</code> if this type represent the beginning
       of a repeat block.
	  */
	 public boolean isStartRepetition() {
		 return name.equals("#startrep");
	 }

	 /**
       Return <code>true</code> if this type represent the end of a
       repeat block.
	  */
	 public boolean isEndRepetition() {
		 return name.startsWith("#endrep");
	 }

	 public boolean isStartCyclic() {
		 return name.startsWith("#startcyclic");
	 }
	 
	 public boolean isEndCyclic() {
		 return name.startsWith("#endcyclic");
	 }
	 
	 /**
       Return <code>true</code> if this type represent a bracket node.
	  */
	 public boolean isBracket() {
		 return name.equals("#bracket");
	 }

	/**
	 * Return <code>true</> if this type represent a bridge node.
	 */
	public boolean isBridge () { return superclass.equals("Bridge"); }

	/**
       Return <code>true</code> if this type represent a cleavage marker.
	  */
	 public boolean isCleavage() {
		 return superclass.equals("cleavage");
	 }    

	 /**
       Return <code>true</code> if this type represent a glycosidic
       cleavage marker.
	  */
	 public boolean isGlycosidicCleavage() {
		 return (isBCleavage() || isCCleavage() || isYCleavage() || isZCleavage());
	 }

	 /**
       Return the type of cleavage marker for cleavage types or the
       empty string otherwise.
	  */
	 public String getCleavageType() {    
		 String ret = "";
		 if( isCleavage() ) {
			 ret = "" + Character.toUpperCase(name.charAt(1));
			 if( isACleavage() || isXCleavage() )
				 ret += name.substring(10);
		 }
		 return ret;
	 }

	 /**
       Return <code>true</code> if this type represent an A ring
       fragment type.
	  */
	 public boolean isACleavage() {
		 return name.startsWith("#acleavage");
	 }        

	 /**
       Return <code>true</code> if this type represent a B cleavage
       type.
	  */
	 public boolean isBCleavage() {
		 return name.equals("#bcleavage");
	 }    

	 /**
       Return <code>true</code> if this type represent a C cleavage
       type.
	  */
	 public boolean isCCleavage() {
		 return name.equals("#ccleavage");
	 }    

	 /**
       Return <code>true</code> if this type represent an X ring
       fragment type.
	  */
	 public boolean isXCleavage() {
		 return name.startsWith("#xcleavage");
	 }        

	 /**
       Return <code>true</code> if this type represent a Y cleavage
       type.
	  */
	 public boolean isYCleavage() {
		 return name.equals("#ycleavage");
	 }    

	 /**
       Return <code>true</code> if this type represent a Z cleavage
       type.
	  */
	 public boolean isZCleavage() {
		 return name.equals("#zcleavage");
	 }    

	 /**
       Return <code>true</code> if this type represent a labile
       cleavage type.
	  */
	 public boolean isLCleavage() {
		 return name.equals("#lcleavage");
	 }    

	 /**
       Return <code>true</code> if this type represent a ring fragment
       type.
	  */
	 public boolean isRingFragment() {
		 return isACleavage() || isXCleavage();
	 }

	 /**
       Return <code>true</code> if <code>pos</code> is a valid linkage
       position for this residue type.
	  */
	 public boolean isValidPosition(char pos) {
		 if( linkage_pos.length>0 && pos!='0' ) {
			 for( int i=0; i<linkage_pos.length; i++ )
				 if( linkage_pos[i]==pos )
					 return true;
			 return false;
		 }
		 return true;
	 }

	 /**
       Return <code>true</code> if <code>poss</code> contains only
       valid linkage position for this residue type.
	  */
	 public boolean areValidPositions(Collection<Character> poss) {
		 for( Character pos : poss ) {
			 boolean found = false;
			 for( int i=0; i<linkage_pos.length && !found; i++ ) {
				 if( linkage_pos[i]==pos ) 
					 found = true;
			 }
			 if( !found )
				 return false;
		 }
		 return true;
	 }

	 /**
       Return <code>true</code> if <code>poss</code> contains at least
       one valid linkage position for this residue type.
	  */
	 public boolean anyValidPosition(Collection<Character> poss) {
		 for( Character pos : poss ) {
			 for( int i=0; i<linkage_pos.length; i++ ) {
				 if( linkage_pos[i]==pos ) 
					 return true;
			 }
		 }
		 return false;
	 }

	 protected void updateMolecule() {
		 try {
			 if( this.composition.equals("?") ) {
				 return;
			 }

			 // parse molecule
			 molecule = new Molecule(this.composition);

			 // update residue masses with high accuracy ones        
			 res_mass_main = molecule.getMainMass();
			 res_mass_avg = molecule.getAverageMass();
		 }
		 catch(Exception e){
			 molecule = null;
		 }
	 }

	 //

	 /**
       Return <code>true</code> if this residue type has been created
       with an optional name and mass.
	  */
	 public boolean isCustomType() {
		 // An aglycon read by its name alone is not one, whatever its name holds (#305).
		 if( is_aglycon )
			 return !(mass_unknown && !named_mass);
		 return ( name.indexOf("=")!=-1 );        
	 }

	 /** The name of a custom type without its "=mass u": up to the last "=", since a name may hold one. */
	 private String customName() {
		 return name.substring(0, name.lastIndexOf('='));
	 }

	 /**
       Return the identifier of this residue type.
	  */
	 public String getName() {
		 return name;
	 }

	 /**
       Return the identifier of this residue type removing the
       information about optional name and mass.
	  */
	 public String getResidueName() {
		 if( isCustomType() )
			 return unescapeAglyconName(customName());
		 if( is_aglycon )
			 return unescapeAglyconName(name);
		 return name;
	 }

	 /**
       Whether this is an aglycon of the user's: a reducing end given a name and a mass, or one read
       by a name no dictionary has (#305).
	  */
	 public boolean isAglycon() {
		 return is_aglycon;
	 }

	 /** Whether this is an aglycon whose mass is not known, as for Ser/Thr (#305). */
	 public boolean hasUnknownMass() {
		 return mass_unknown;
	 }

	 /** A mass as the type's name carries it: four decimals with a point, whatever the locale, or NaN. */
	 private static String massText(double mass) {
		 if( Double.isNaN(mass) )
			 return "NaN";
		 return new java.text.DecimalFormat("0.0000", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.ROOT)).format(mass);
	 }

	 /**
       An aglycon's name as GWS writes it: as typed, "Ser/Thr", "Cer(d18:1/16:0)", "β-Ala", but for the
       few characters GWS cannot hold anywhere in it (#305; docs/gws-format.md, "the escape").

       <p>The reader takes the root's name to run up to the first "--", so the name may hold what a
       residue name may not - a "/", a space, brackets, any letter. What it may not hold is what splits
       the string before the name is looked at: ";" separates the structures of a document, "$" opens
       the mass options, "}" closes a composition's bracket, "," separates the mass options' fields,
       and "--" begins a linkage. A name holding any of those, or a control character, is written with
       each of them as "_XX_" in hexadecimal and the whole prefixed "_e_": "Ser,Thr" is
       "_e_Ser_2C_Thr". Within it a "_" that would read as an escape is escaped too, and a name typed
       with "_e_" at its start is escaped for that alone.

       <p>Only a name so prefixed is ever decoded, so a name written before - "Ser_2F_Thr", meant as
       typed - reads as it always did.
	  */
	 public static String escapeAglyconName(String _name) {
		 boolean prefixed = _name.startsWith(ESCAPED);
		 boolean needed = prefixed || _name.contains("--");
		 for( int i = 0; i < _name.length() && !needed; i++ )
			 needed = splitsTheText(_name.charAt(i));
		 if( !needed )
			 return _name;

		 StringBuilder escaped = new StringBuilder(ESCAPED);
		 for( int i = 0; i < _name.length(); i++ ) {
			 char c = _name.charAt(i);
			 boolean secondDash = c == '-' && i > 0 && _name.charAt(i - 1) == '-';
			 boolean looksEscaped = c == '_' && i + 3 < _name.length() && _name.charAt(i + 3) == '_'
					 && isHex(_name.charAt(i + 1)) && isHex(_name.charAt(i + 2));
			 if( splitsTheText(c) || secondDash || looksEscaped || (prefixed && i == 0) )
				 escaped.append('_').append(String.format("%02X", (int) c)).append('_');
			 else
				 escaped.append(c);
		 }
		 return escaped.toString();
	 }

	 /** What marks a name as escaped. */
	 private static final String ESCAPED = "_e_";

	 private static boolean splitsTheText(char _c) {
		 return _c == ';' || _c == '$' || _c == '}' || _c == ',' || _c < 0x20;
	 }

	 /** The name as typed, from {@link #escapeAglyconName}; a name not prefixed "_e_" is itself. */
	 public static String unescapeAglyconName(String _stored) {
		 if( !_stored.startsWith(ESCAPED) )
			 return _stored;
		 // as bytes, decoded as UTF-8: this writes only ASCII escaped, and a name escaped byte by byte,
		 // "_e__E3__82__BB_", as the format's notes first proposed, reads as the letter it was
		 String escaped = _stored.substring(ESCAPED.length());
		 java.io.ByteArrayOutputStream typed = new java.io.ByteArrayOutputStream();
		 int i = 0;
		 while( i < escaped.length() ) {
			 if( i + 3 < escaped.length() && escaped.charAt(i) == '_' && escaped.charAt(i + 3) == '_'
					 && isHex(escaped.charAt(i + 1)) && isHex(escaped.charAt(i + 2)) ) {
				 typed.write(Integer.parseInt(escaped.substring(i + 1, i + 3), 16));
				 i += 4;
				 continue;
			 }
			 byte[] literal = escaped.substring(i, i + 1).getBytes(java.nio.charset.StandardCharsets.UTF_8);
			 typed.write(literal, 0, literal.length);
			 i++;
		 }
		 return new String(typed.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
	 }

	 private static boolean isHex(char _c) {
		 return (_c >= '0' && _c <= '9') || (_c >= 'A' && _c <= 'F');
	 }

	 /**
       Return the composition class of this residue type (e.g. Hex for
       all the hexoses).
	  */
	 public String getCompositionClass() {
		 if( composition_class!=null && composition_class.length()>0 && !composition_class.equals("-") )
			 return composition_class;
		 return name;
	 }

	 /**
       Return the atomic composition associated with this residue type.
	  */
	 public String getComposition() {
		 return composition;
	 }

	 /**
       Return the set of possible synonyms for this residue type.
	  */
	 public String[] getSynonyms() {
		 return synonyms;
	 }

	 /**
       Return the standard IUPAC name for this residue type. The name
       can contain dollar symbol to identify the position of the ring
       size indicator.
	  */
	 public String getIupacName() {
		 return iupac_name;
	 }

	 /**
       Return true if a IUPAC name is specified for this residue type.
	  */
	 public boolean hasIupacName() {
		 return !iupac_name.equals("-");
	 }

	 /**
       Return true if a default chirality is specified for this
       residue type.
	  */
	 public boolean hasChirality() {
		 return (chirality!='?');
	 }

	 /**
       Return the class of this residue type.
	  */
	 public String getSuperclass() {
		 return superclass;
	 }

	 /**
       Return the default anomeric carbon for this residue type.
	  */
	 public char getAnomericCarbon() {
		 return anomeric_carbon;
	 }

	 /**
       Return the default chirality for this residue type.
	  */
	 public char getChirality() {
		 return chirality;
	 }

	 /**
       Return the default ring size for this residue type.
	  */
	 public char getRingSize() {
		 return ring_size;
	 }

	 public int getToolbarOrder() {
		 return bar_order;
	 }    

	 /**
       Set the mono-isotopic residual mass for this residue
       type. 
       @see #getResidueMassMain
	  */
	 public void setResidueMassMain(double mass) {
		 res_mass_main = mass;
	 }

	 /**
       Return the mono-isotopic residual mass for this residue
       type. This is equivalent to the mass of the residue in
       isolation minus a water molecule.
	  */
	 public double getResidueMassMain() {
		 return res_mass_main;
	 }

	 /**
       Set the average residual mass for this residue type.
       @see #getResidueMassAvg
	  */
	 public void setResidueMassAvg(double mass) {
		 res_mass_avg = mass;
	 }

	 /**
       Return the average residual mass for this residue
       type. This is equivalent to the mass of the residue in
       isolation minus a water molecule.
	  */
	 public double getResidueMassAvg() {
		 return res_mass_avg;
	 }

	 /**
       Return the residual mass for this residue
       type given the current settings.
	  */
	 public double getMass() {
		 return res_mass_main;
	 }

	 /**
       Return the number of positions available for permethylation.
	  */
	 public int getNoMethyls() {
		 return nmethyls;
	 }

	 /**
       Return <code>true</code> if this residue type is dropped during
       permethylation.
	  */
	 public boolean isDroppedWithMethylation() {
		 return drop_methylated;
	 }

	 /**
       Return the number of positions available for peracetylation.
	  */
	 public int getNoAcetyls() {
		 return nacetyls;
	 }

	 /**
       Return <code>true</code> if this residue type is dropped during
       peracetylation.
	  */
	 public boolean isDroppedWithAcetylation() {
		 return drop_acetylated;
	 }

	 /**
       Return the maximum number of available linkage position.
	  */
	 public int getMaxLinkages() {
		 return nlinkages;
	 }    

	 /**
       Return the list of available linkage position.
	  */
	 public char[] getLinkagePositions() {
		 return linkage_pos;
	 }

	 /**
       Return the list of positions carrying a charge.
	  */
	 public char[] getChargePositions() {
		 return charges_pos;
	 }

	 /**
       Return the number of positions carrying a charge.
	  */
	 public int getNoCharges() {
		 return charges_pos.length;
	 }

	 /**
       Return <code>true</code> if this residue type implies an
       alditol if specified as reducing end marker.
	  */
	 public boolean makesAlditol() {
		 return make_alditol;
	 }

	 /**
       Return <code>true</code> if this residue type can be used as
       reducing end marker.
	  */
	 public boolean canBeReducingEnd() {
		 return can_redend;
	 }

	 /**
       Return <code>true</code> if a residue of this type can have
       children.
	  */
	 public boolean canHaveChildren() {
		 return (nlinkages>1);
	 }

	 /**
       Return <code>true</code> if a residue of this type can have
       a parent residue.
	  */
	 public boolean canHaveParent() {
		 return can_parent;
	 }

	 /**
       Return the description of this residue type.
	  */
	 public String getDescription() {
		 return description;
	 }    
	 
	 public void changeDescription(String a_sNotation) {
		 this.description = a_sNotation;
	 }

	 /**
       Return the WURCS MAP code of this residue type, or an empty string when it has none.
       Only substituents and bridge substituents carry one.
	  */
	 public String getMAP() {
		 return map;
	 }

	 /**
       Return a molecule representation of this residue.
	  */
	 public Molecule getMolecule() {
		 return molecule;
	 }

	 /**
       Return a string representation of this residue. The
       representation does not contain the complete information and
       cannot be used for serialization.
	  */
	 public String toString() {
		 return description;
	 }
}
