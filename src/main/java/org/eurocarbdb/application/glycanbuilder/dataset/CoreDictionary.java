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


package org.eurocarbdb.application.glycanbuilder.dataset;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLConnection;

import org.eurocarbdb.application.glycanbuilder.CoreType;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
   The dictionary of all core types available in the application.
   Information about core types is loaded at run time from a
   configuration file. The dictionary is a singleton and all
   information has class-wide access.

   @see CoreType   

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class CoreDictionary {

	static {
		dictionary = new TreeMap<String,CoreType>();
		superclasses = new LinkedList<String>();
		all_cores = new LinkedList<CoreType>();
		all_cores_map = new HashMap<String,LinkedList<CoreType> >();
	}

	private static TreeMap<String,CoreType> dictionary;
	private static LinkedList<String> superclasses;    
	private static LinkedList<CoreType> all_cores;
	private static HashMap<String,LinkedList<CoreType> > all_cores_map;

	// --- Data access

	/**
       Return the core type with a given identifier.
       @throws Exception if the specified core type is not found
	 */
	public static CoreType getCoreType(String type_name) throws Exception{
		if( dictionary.containsKey(type_name) )
			return dictionary.get(type_name);
		throw new Exception("Invalid type: <" + type_name + ">");
	}

	/**
       Return an iterator over the list of core types.
	 */
	public static Iterator<CoreType> iterator() {
		return all_cores.iterator();
	}

	/**
       Return the list of classes of all core types.
	 */
	public static Collection<String> getSuperclasses() {
		return superclasses;
	}

	/**
       Return the list of all core types.
	 */
	public static Collection<CoreType> getCores() {
		return all_cores;
	}

	/**
       Return the list of all core types of a given class.
	 */
	public static Collection<CoreType> getCores(String superclass) {
		return all_cores_map.get(superclass);
	}

	//

	/**
       Create a new structure from a core type with a given identifier.
       @return the root to the subtree.
       @throws Exception if the specified core type is not found       
	 */
	public static Residue newCore(String type_name) throws Exception {
		return getCoreType(type_name).newCore();
	}

	/**
       Create a new structure from a core type with a given identifier.
       @throws Exception if the specified core type is not found       
	 */
	public static Glycan newStructure(String type_name, MassOptions mass_opt) throws Exception {
		return new GWSParser().readGlycan(getCoreType(type_name).getStructure(),mass_opt);
	}    

	//---- init

	private CoreDictionary() {}

	/**
       Load the core types from a configuration file.
	 */
	public static void loadDictionary(String filename) {
		// clear dict
		dictionary.clear();

		superclasses.clear();
		all_cores.clear();
		all_cores_map.clear();

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
					CoreType type = new CoreType(line);
					dictionary.put(type.getName(),type);

					// collect cores
					addSuperclass(type.getSuperclass());         
					all_cores.add(type);
					all_cores_map.get(type.getSuperclass()).add(type);
				}
			}        

			is.close();
		}
		catch(Exception e) {
			LogUtils.report(e);
			dictionary.clear();
		}
	}

	private static void addSuperclass(String superclass) {
		for( Iterator<String> i=superclasses.iterator(); i.hasNext(); ) 
			if( i.next().equals(superclass) ) return;
		superclasses.add(superclass);
		all_cores_map.put(superclass,new LinkedList<CoreType>());
	}

}
