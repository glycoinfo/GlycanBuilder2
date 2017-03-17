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

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;

import org.eurocarbdb.application.glycanbuilder.CrossRingFragmentType;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
   The dictionary of all cross ring fragments types available in the
   application. Information about cross ring fragment types is loaded
   at run time from a configuration file. The dictionary is a
   singleton and all information has class-wide access.

   @see CrossRingFragmentType   

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class CrossRingFragmentDictionary {

	static {
		dictionary = new Vector<CrossRingFragmentType>();
	}

	private static Vector<CrossRingFragmentType> dictionary;

	//---- init

	private CrossRingFragmentDictionary() {}

	/**
       Load the dictionary from a configuration file.
	 */
	public static void loadDictionary(String filename) {
		// clear dict
		dictionary.clear();

		try {
			BufferedReader is;
			if(filename.startsWith("http")){
				URLConnection conn=new URL(filename).openConnection();
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
				if( line.length()>0 && !line.startsWith("%") ) {
					CrossRingFragmentType toadd = new CrossRingFragmentType(line);
					dictionary.add(toadd);
				}
			}

			is.close();
		}
		catch(Exception e) {
			LogUtils.report(e);
			dictionary.clear();
		}
	}


	// --- Data access


	/**
       Get all cross ring fragment types available for the given residue.
	 */
	public static Collection<CrossRingFragmentType> getCrossRingFragmentTypes(Residue r) {
		Vector<CrossRingFragmentType> ret = new Vector<CrossRingFragmentType>();
		for(CrossRingFragmentType crt : dictionary ) {
			if( crt.matches(r) ) 
				ret.add(crt);
		}
		return ret;
	}

	/**
       Get all A cross ring fragment types available for the given residue.
	 */
	public static Collection<CrossRingFragmentType> getCrossRingFragmentTypesA(Residue r) {
		Vector<CrossRingFragmentType> ret = new Vector<CrossRingFragmentType>();
		for(CrossRingFragmentType crt : dictionary ) {
			if( crt.matches(r) && crt.isACleavage() ) 
				ret.add(crt);
		}
		return ret;
	}

	/**
       Get all X cross ring fragment types available for the given residue.
	 */
	public static Collection<CrossRingFragmentType> getCrossRingFragmentTypesX(Residue r) {
		Vector<CrossRingFragmentType> ret = new Vector<CrossRingFragmentType>();
		for(CrossRingFragmentType crt : dictionary ) {
			if( crt.matches(r) && crt.isXCleavage() ) 
				ret.add(crt);
		}
		return ret;
	}

	/**
       Get a specific cross ring fragment type.
       @param fragment_type type of cross ring fragment (A/B)
       @param first_pos position of the first cleavage in the ring
       @param last_pos position of the second cleavage in the ring
       @param r residue for which the ring fragment is computed
	 */
	public static CrossRingFragmentType getCrossRingFragmentType(char fragment_type, int first_pos, int last_pos, Residue r) {
		for(CrossRingFragmentType crt : dictionary ) {
			if( crt.matches(fragment_type,first_pos,last_pos,r) ) 
				return crt;
		}
		return null;
	}

	/**
       Create a new residue representing a specific cross ring fragment type.
       @param fragment_type type of cross ring fragment (A/B)
       @param first_pos position of the first cleavage in the ring
       @param last_pos position of the second cleavage in the ring
       @param r residue for which the ring fragment is computed
	 */
	public static Residue newCrossRingFragment(char fragment_type, int first_pos, int last_pos, Residue r) throws Exception {
		if( r==null )
			throw new Exception("Cannot create a cross ring fragment without a residue");

		ResidueType type = getCrossRingFragmentType(fragment_type,first_pos,last_pos,r);
		if( type==null )
			throw new Exception("Invalid " + fragment_type + " cross-ring cleavage at positions " + first_pos + "," + last_pos + " of residue " + r.getTypeName());

		Residue ret = new Residue(type);
		ret.setCleavedResidue(r);
		return ret;
	}


	static Residue newFragment(String type_name, Residue cleaved) {
		if( type_name==null || cleaved==null )
			return null;

		if( !type_name.startsWith("#acleavage") && !type_name.startsWith("#xcleavage") ) 
			return null;

		LinkedList<String> tokens = TextUtils.tokenize(type_name,"_");
		char fragment_type = type_name.charAt(1);    
		int first_pos = Integer.parseInt(tokens.get(1));
		int last_pos  = Integer.parseInt(tokens.get(2));

		CrossRingFragmentType crt = getCrossRingFragmentType(fragment_type,first_pos,last_pos,cleaved);
		return (crt!=null ) ?new Residue(crt) :null;
	}

}