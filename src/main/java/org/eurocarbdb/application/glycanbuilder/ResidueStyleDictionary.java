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

import java.util.Iterator;
import java.util.TreeMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLConnection;

import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
   The dictionary of the residue styles available in the application
   for a certain cartoon notation. Information about residue styles is
   loaded at run time from a configuration file. There is a single
   dictionary for each notation. The style dictionaries are stored in
   the workspace and passed to the renderers.

   @see ResidueStyle

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */


public class ResidueStyleDictionary {

	//private TreeMap<String,ResidueStyle> styles = new TreeMap<String,ResidueStyle>();
	private TreeMap<String, String> styles = new TreeMap<String, String>();
	
	//---- init

	/**
       Load the dictionary from a configuration file
	 */

	public void loadStyles(String filename) {
		// clear dict
		styles.clear();

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
					String[] str_tokens = line.split("\t");
					this.styles.put(str_tokens[0], line);
					
					//ResidueStyle toadd = new ResidueStyle(line);
					//styles.put(toadd.getName(),toadd);
				}
			}

			is.close();
		}catch(Exception e) {
			LogUtils.report(e);
			styles.clear();
		}
	}

	// --- Data access

	/**
       Return the residue style for a specific residue, or a default
       one (text representation only) if none is found.
       @param node the residue for which the style should be
       retrieved, the type name is used to identify the style
	 * @see ResidueStyle
	 */

	public ResidueStyle getStyle(Residue node) {
		if( node==null ) return new ResidueStyle();

		ResidueStyle a_oRS = null;
		ResidueType type = node.getType();
		String type_name = type.getName();

		if( styles.containsKey(type_name)) {
			try {
				return new ResidueStyle(this.styles.get(type_name));
			} catch (Exception e) {
				LogUtils.report(e);
			}
		}
		
		if( type_name.startsWith("#startrep") )
			return ResidueStyle.createStartRepetition();        
		if( type_name.startsWith("#endrep") )
			return ResidueStyle.createEndRepetition();
		if( type_name.startsWith("#startcyclic"))
			return ResidueStyle.createStartCyclic();
		if( type_name.startsWith("#endcyclic"))
			return ResidueStyle.createEndCyclic();
		if( type_name.equals("#attach") )
			return ResidueStyle.createAttachPoint();        
		if( type_name.equals("#redend") )
			return ResidueStyle.createReducingEnd();
		if( type_name.equals("#altstart"))
			return ResidueStyle.createStartAlternative();
		if( type_name.equals("#altend"))
			return ResidueStyle.createEndAlternative();
		if( type_name.equals("#bracket") )
			return ResidueStyle.createBracket();

		if( type_name.startsWith("#acleavage") ) {
			CrossRingFragmentType crt = (CrossRingFragmentType)type;
			return ResidueStyle.createACleavage(crt.getStartPos(),crt.getEndPos());
		}
		if( type_name.equals("#bcleavage") )
			return ResidueStyle.createBCleavage();
		if( type_name.equals("#ccleavage") )
			return ResidueStyle.createCCleavage();

		if( type_name.startsWith("#xcleavage") ) {
			CrossRingFragmentType crt = (CrossRingFragmentType)type;
			return ResidueStyle.createXCleavage(crt.getStartPos(),crt.getEndPos());
		}
		if( type_name.equals("#ycleavage") )
			return ResidueStyle.createYCleavage();
		if( type_name.equals("#zcleavage") )
			return ResidueStyle.createZCleavage();
		if( type.isCustomType() ) 
			return ResidueStyle.createText(type.getResidueName());    

		/** set basetype symbol*/
		String a_sStyle = "";
		if(a_oRS == null) {
			a_sStyle = this.styles.get(type.getSuperclass());
		}
		
		/** set text notation */
		if(a_sStyle == null || a_sStyle.equals("")) {
			a_sStyle = type_name + 
									"\t-\t" + 
									"0,0,0\t" + 
									"empty\t" + 
									"no\t" + 
									"255,255,255\t" + 
									node.getTypeName() + 
									"\t0,0,0";

		}
		
		try {
			a_oRS = new ResidueStyle(a_sStyle);
		} catch (Exception e) {
			LogUtils.report(e);
		}

		if(node.getID() != 0) a_oRS.setText(String.valueOf(node.getID()));
		
		return a_oRS;
	}

	public boolean containsResidue(Residue node) {
		return this.styles.containsKey(node.getTypeName());
	}
	
	/**
       Return an iterator of all residue styles contained in the
       dictionary.
	 */
	public Iterator<String> iterator() {
		return styles.values().iterator();
	}
	//public Iterator<ResidueStyle> iterator() {
	//	return styles.values().iterator();
	//}

}