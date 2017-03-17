package org.glycoinfo.application.glycanbuilder.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

public class NonSymbolicResidueDictionary {


	static {
		dictionary = new HashMap<String,ResidueType>();
		all_residues_map = new HashMap<String,ArrayList<ResidueType> >();
		
		initDictionary();
	}

	private static HashMap<String,ResidueType>  dictionary;
	private static HashMap<String,ArrayList<ResidueType>> all_residues_map;
	
	// --- Data access

	/**
       Return the residue type with the given name
       @see ResidueType
       @throws Exception if there is no residue type with such a name
	 */
	public static ResidueType getResidueType(String type_name) throws Exception{ 
		ResidueType ret = findResidueType(type_name);

		if( ret==null ) throw new Exception("Invalid type: <" + type_name + ">");
		return ret;
	}

	/**
       Return <code>true</code> if a residue type with such a name
       exists.
       @see ResidueType
	 */
	public static boolean hasResidueType(String type_name) {
		return (findResidueType(type_name)!=null);
	}

	/**
       Return the residue type with the given name or
       <code>null</null> otherwise
       @see ResidueType
	 */
	public static ResidueType findResidueType(String type_name) {
		return dictionary.get(type_name.toLowerCase());	
	}

	public static Collection<ResidueType> getResidues() {
		return dictionary.values();
	}
	
	public static HashMap<String, ArrayList<ResidueType>> getAllResidues() {
		return all_residues_map;
	}
	
	//---- init

	private NonSymbolicResidueDictionary() {}

	/**
	 * Load the residue types from a configuration file.
	 */
	public static void loadDictionary(String filename) {
		// clear dict
		initDictionary();

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
				if( line.length()>0 && !line.startsWith("%") ) 
					add(new ResidueType(line));    
			}

			is.close();
		}
		catch(Exception e) {
			LogUtils.report(e);
			dictionary.clear();
		}
	}

	private static void add(ResidueType type) {
		dictionary.put(type.getName().toLowerCase(),type);

		for( String s : type.getSynonyms() )
			dictionary.put(s.toLowerCase(),type);

		// add superclass
		String superclass = type.getSuperclass();
		if(!all_residues_map.containsKey(superclass)) {
			all_residues_map.put(superclass,new ArrayList<ResidueType>());
		}

		all_residues_map.get(superclass).add(type);    
	}

	private static void initDictionary() {
		dictionary.clear();
		all_residues_map.clear();
		add(new ResidueType());
	}
}