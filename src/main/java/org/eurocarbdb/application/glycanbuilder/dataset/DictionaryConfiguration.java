package org.eurocarbdb.application.glycanbuilder.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.Configuration;
import org.eurocarbdb.application.glycanbuilder.fileutil.FileConstants;

public class DictionaryConfiguration{
	//Dictionary files
	public static String RESIDUE_TYPES_FILE="residueTypesFile";
	public static String TERMINAL_TYPES_FILE="terminalTypesFile";
	public static String CORE_TYPES_FILE="coreTypesFile";
	public static String CROSS_RING_FRAGMENT_TYPES_FILE="crossRingFragmentTypesFile";
	public static String CROSS_LINKED_SUBSTITUENT_TYPES_FILE = "crossLinkedSubstituentTypesFile";
	public static String NON_SYMBOLIC_RESIDUE_TYPES_FILE = "nonSymbolicResidueTypesFile";
	
	//UOXF
	public static String UOXF_RESIDUE_PLACEMENTS_FILE="uoxf_residuePlacementsFile";
	public static String UOXF_RESIDUE_STYLES_FILE="uoxf_residueStylesFile";
	public static String UOXF_LINKAGE_STYLES_FILE="uoxf_linkageStylesFile";
	
	//UOXF_COL
	public static String UOXFCOL_RESIDUE_PLACEMENTS_FILE="uoxfcol_residuePlacementsFile";
	public static String UOXFCOL_RESIDUE_STYLES_FILE="uoxfcol_residueStylesFile";
	public static String UOXFCOL_LINKAGE_STYLES_FILE="uoxfcol_linkageStylesFile";
	
	//TEXT
	public static String TEXT_RESIDUE_PLACEMENTS_FILE="text_residuePlacementsFile";
	public static String TEXT_RESIDUE_STYLES_FILE="text_residueStylesFile";
	public static String TEXT_LINKAGE_STYLES_FILE="text_linkageStylesFile";
	
	//CFG_LINK
	public static String CFGLINK_RESIDUE_PLACEMENTS_FILE="cfgLink_residuePlacementsFile";
	public static String CFGLINK_RESIDUE_STYLES_FILE="cfgLink_residueStylesFile";
	public static String CFGLINK_LINKAGE_STYLES_FILE="cfgLink_linkageStylesFile";
	
	//CFG
	public static String CFG_RESIDUE_PLACEMENTS_FILE="cfg_residuePlacementsFile";
	public static String CFG_RESIDUE_STYLES_FILE="cfg_residueStylesFile";
	public static String CFG_LINKAGE_STYLES_FILE="cfg_linkageStylesFile";
	
	//CFGBW
	public static String CFGBW_RESIDUE_PLACEMENTS_FILE="cfgBW_residuePlacementsFile";
	public static String CFGBW_RESIDUE_STYLES_FILE="cfgBW_residueStylesFile";
	public static String CFGBW_LINKAGE_STYLES_FILE="cfgBW_linkageStylesFile";
	
	//SNFG
	public static String SNFG_RESIDUE_PLACEMENTS_FILE= "snfg_residuePlacementsFile";
	public static String SNFG_RESIDUE_STYLES_FILE = "snfg_residueStylesFile";
	public static String SNFG_LINKAGE_STYLES_FILE = "snfg_linkageStylesFile";
	
	private HashMap<String, String> keyToFile=new HashMap<String,String>(){
		@Override
		public String get(Object key) {
			if(containsKey(key)){
				return super.get(key);
			}else{
				return "";
			}
		}
	};
	
	public DictionaryConfiguration(){
		for(String dictionaryName:getDictionaryNameList()){
			keyToFile.put(dictionaryName, DictionaryConfiguration.getDefaultDictionaryFile(dictionaryName));
		}
	}
	
	public static String DICT_SECT="dictionaries";
	
	public static List<String> getDictionaryNameList(){
		List<String> list=new ArrayList<String>();
		
		list.add(RESIDUE_TYPES_FILE);
		list.add(TERMINAL_TYPES_FILE);
		list.add(CORE_TYPES_FILE);
		list.add(CROSS_RING_FRAGMENT_TYPES_FILE);
		list.add(CROSS_LINKED_SUBSTITUENT_TYPES_FILE);
		list.add(NON_SYMBOLIC_RESIDUE_TYPES_FILE);
		list.add(UOXF_RESIDUE_PLACEMENTS_FILE);
		list.add(UOXF_RESIDUE_STYLES_FILE);
		list.add(UOXF_LINKAGE_STYLES_FILE);
		list.add(UOXFCOL_RESIDUE_PLACEMENTS_FILE);
		list.add(UOXFCOL_RESIDUE_STYLES_FILE);
		list.add(UOXFCOL_LINKAGE_STYLES_FILE);
		list.add(TEXT_RESIDUE_PLACEMENTS_FILE);
		list.add(TEXT_RESIDUE_STYLES_FILE);
		list.add(TEXT_LINKAGE_STYLES_FILE);
		list.add(CFGLINK_RESIDUE_PLACEMENTS_FILE);
		list.add(CFGLINK_RESIDUE_STYLES_FILE);
		list.add(CFGLINK_LINKAGE_STYLES_FILE);
		list.add(CFG_RESIDUE_PLACEMENTS_FILE);
		list.add(CFG_RESIDUE_STYLES_FILE);
		list.add(CFG_LINKAGE_STYLES_FILE);
		list.add(CFGBW_RESIDUE_PLACEMENTS_FILE);
		list.add(CFGBW_RESIDUE_STYLES_FILE);
		list.add(CFGBW_LINKAGE_STYLES_FILE);
		list.add(SNFG_RESIDUE_PLACEMENTS_FILE);
		list.add(SNFG_RESIDUE_STYLES_FILE);
		list.add(SNFG_LINKAGE_STYLES_FILE);

		return list;
	}
	
	public void setDictionaryFile(String dictionaryName,String fileName){
		keyToFile.put(dictionaryName, fileName);
	}
	
	public String getDictionaryFile(String dictionaryName){
		return keyToFile.get(dictionaryName);
	}
	
	

	public void store(Configuration theConfiguration){
		theConfiguration.put(DICT_SECT, RESIDUE_TYPES_FILE, keyToFile.get(RESIDUE_TYPES_FILE));
		theConfiguration.put(DICT_SECT, TERMINAL_TYPES_FILE, keyToFile.get(TERMINAL_TYPES_FILE));
		theConfiguration.put(DICT_SECT, CORE_TYPES_FILE, keyToFile.get(CORE_TYPES_FILE));
		theConfiguration.put(DICT_SECT, CROSS_RING_FRAGMENT_TYPES_FILE, keyToFile.get(CROSS_RING_FRAGMENT_TYPES_FILE));
		theConfiguration.put(DICT_SECT, CROSS_LINKED_SUBSTITUENT_TYPES_FILE, keyToFile.get(CROSS_LINKED_SUBSTITUENT_TYPES_FILE));
		theConfiguration.put(DICT_SECT, NON_SYMBOLIC_RESIDUE_TYPES_FILE, keyToFile.get(NON_SYMBOLIC_RESIDUE_TYPES_FILE));

		theConfiguration.put(DICT_SECT, UOXF_RESIDUE_PLACEMENTS_FILE, keyToFile.get(UOXF_RESIDUE_PLACEMENTS_FILE));
		theConfiguration.put(DICT_SECT, UOXF_RESIDUE_STYLES_FILE, keyToFile.get(UOXF_RESIDUE_STYLES_FILE));
		theConfiguration.put(DICT_SECT, UOXF_LINKAGE_STYLES_FILE, keyToFile.get(UOXF_LINKAGE_STYLES_FILE));
		
		theConfiguration.put(DICT_SECT, UOXFCOL_RESIDUE_PLACEMENTS_FILE, keyToFile.get(UOXFCOL_RESIDUE_PLACEMENTS_FILE));
		theConfiguration.put(DICT_SECT, UOXFCOL_RESIDUE_STYLES_FILE, keyToFile.get(UOXFCOL_RESIDUE_STYLES_FILE));
		theConfiguration.put(DICT_SECT, UOXFCOL_LINKAGE_STYLES_FILE, keyToFile.get(UOXFCOL_LINKAGE_STYLES_FILE));
		
		theConfiguration.put(DICT_SECT, TEXT_RESIDUE_PLACEMENTS_FILE, keyToFile.get(TEXT_RESIDUE_PLACEMENTS_FILE));
		theConfiguration.put(DICT_SECT, TEXT_RESIDUE_STYLES_FILE, keyToFile.get(TEXT_RESIDUE_STYLES_FILE));
		theConfiguration.put(DICT_SECT, TEXT_LINKAGE_STYLES_FILE, keyToFile.get(TEXT_LINKAGE_STYLES_FILE));

		theConfiguration.put(DICT_SECT, CFGLINK_RESIDUE_PLACEMENTS_FILE, keyToFile.get(CFGLINK_RESIDUE_PLACEMENTS_FILE));
		theConfiguration.put(DICT_SECT, CFGLINK_RESIDUE_STYLES_FILE, keyToFile.get(CFGLINK_RESIDUE_STYLES_FILE));
		theConfiguration.put(DICT_SECT, CFGLINK_LINKAGE_STYLES_FILE, keyToFile.get(CFGLINK_LINKAGE_STYLES_FILE));
		
		theConfiguration.put(DICT_SECT, CFG_RESIDUE_PLACEMENTS_FILE, keyToFile.get(CFG_RESIDUE_PLACEMENTS_FILE));
		theConfiguration.put(DICT_SECT, CFG_RESIDUE_STYLES_FILE, keyToFile.get(CFG_RESIDUE_STYLES_FILE));
		theConfiguration.put(DICT_SECT, CFG_LINKAGE_STYLES_FILE, keyToFile.get(CFG_LINKAGE_STYLES_FILE));
		
		theConfiguration.put(DICT_SECT, CFGBW_RESIDUE_PLACEMENTS_FILE, keyToFile.get(CFGBW_RESIDUE_PLACEMENTS_FILE));
		theConfiguration.put(DICT_SECT, CFGBW_RESIDUE_STYLES_FILE, keyToFile.get(CFGBW_RESIDUE_STYLES_FILE));
		theConfiguration.put(DICT_SECT, CFGBW_LINKAGE_STYLES_FILE, keyToFile.get(CFGBW_LINKAGE_STYLES_FILE));

		theConfiguration.put(DICT_SECT, SNFG_RESIDUE_PLACEMENTS_FILE, keyToFile.get(SNFG_RESIDUE_PLACEMENTS_FILE));
		theConfiguration.put(DICT_SECT, SNFG_RESIDUE_STYLES_FILE, keyToFile.get(SNFG_RESIDUE_STYLES_FILE));
		theConfiguration.put(DICT_SECT, SNFG_LINKAGE_STYLES_FILE, keyToFile.get(SNFG_LINKAGE_STYLES_FILE));
	}
	
	public void retrieve(Configuration theConfiguration){
		keyToFile.put(RESIDUE_TYPES_FILE,theConfiguration.get(DICT_SECT, RESIDUE_TYPES_FILE, FileConstants.RESIDUE_TYPES_FILE));
		keyToFile.put(TERMINAL_TYPES_FILE,theConfiguration.get(DICT_SECT, TERMINAL_TYPES_FILE, FileConstants.TERMINAL_TYPES_FILE));
		keyToFile.put(CORE_TYPES_FILE,theConfiguration.get(DICT_SECT, CORE_TYPES_FILE, FileConstants.CORE_TYPES_FILE));
		keyToFile.put(CROSS_RING_FRAGMENT_TYPES_FILE,theConfiguration.get(DICT_SECT, CROSS_RING_FRAGMENT_TYPES_FILE, FileConstants.CROSS_RING_FRAGMENT_TYPES_FILE));
		keyToFile.put(CROSS_LINKED_SUBSTITUENT_TYPES_FILE,theConfiguration.get(DICT_SECT, CROSS_LINKED_SUBSTITUENT_TYPES_FILE, FileConstants.CROSS_LINKED_SUBSTITUENT_TYPES_FILE));
		keyToFile.put(NON_SYMBOLIC_RESIDUE_TYPES_FILE,theConfiguration.get(DICT_SECT, NON_SYMBOLIC_RESIDUE_TYPES_FILE, FileConstants.NON_SYMBOLIC_RESIDUE_TYPES_FILE));
		
		keyToFile.put(UOXF_RESIDUE_STYLES_FILE,theConfiguration.get(DICT_SECT, UOXF_RESIDUE_STYLES_FILE, FileConstants.RESIDUE_STYLES_FILE_UOXF));
		keyToFile.put(UOXF_RESIDUE_PLACEMENTS_FILE,theConfiguration.get(DICT_SECT, UOXF_RESIDUE_PLACEMENTS_FILE, FileConstants.RESIDUE_PLACEMENTS_FILE_UOXF));
		keyToFile.put(UOXF_LINKAGE_STYLES_FILE,theConfiguration.get(DICT_SECT, UOXF_LINKAGE_STYLES_FILE, FileConstants.LINKAGE_STYLES_FILE_UOXF));
		
		keyToFile.put(UOXFCOL_RESIDUE_STYLES_FILE,theConfiguration.get(DICT_SECT, UOXFCOL_RESIDUE_STYLES_FILE, FileConstants.RESIDUE_STYLES_FILE_UOXFCOL));
		keyToFile.put(UOXFCOL_RESIDUE_PLACEMENTS_FILE,theConfiguration.get(DICT_SECT, UOXFCOL_RESIDUE_PLACEMENTS_FILE, FileConstants.RESIDUE_PLACEMENTS_FILE_UOXF));
		keyToFile.put(UOXFCOL_LINKAGE_STYLES_FILE,theConfiguration.get(DICT_SECT, UOXFCOL_LINKAGE_STYLES_FILE, FileConstants.LINKAGE_STYLES_FILE_UOXF));
		
		keyToFile.put(TEXT_RESIDUE_STYLES_FILE,theConfiguration.get(DICT_SECT, TEXT_RESIDUE_STYLES_FILE, FileConstants.RESIDUE_STYLES_FILE_TEXT));
		keyToFile.put(TEXT_RESIDUE_PLACEMENTS_FILE,theConfiguration.get(DICT_SECT, TEXT_RESIDUE_PLACEMENTS_FILE, FileConstants.RESIDUE_PLACEMENTS_FILE_TEXT));
		keyToFile.put(TEXT_LINKAGE_STYLES_FILE,theConfiguration.get(DICT_SECT, TEXT_LINKAGE_STYLES_FILE, FileConstants.LINKAGE_STYLES_FILE_TEXT));
		
		keyToFile.put(CFGLINK_RESIDUE_STYLES_FILE,theConfiguration.get(DICT_SECT, CFGLINK_RESIDUE_STYLES_FILE, FileConstants.RESIDUE_STYLES_FILE_CFGLINK));
		keyToFile.put(CFGLINK_RESIDUE_PLACEMENTS_FILE,theConfiguration.get(DICT_SECT, CFGLINK_RESIDUE_PLACEMENTS_FILE, FileConstants.RESIDUE_PLACEMENTS_FILE_CFGLINK));
		keyToFile.put(CFGLINK_LINKAGE_STYLES_FILE,theConfiguration.get(DICT_SECT, CFGLINK_LINKAGE_STYLES_FILE, FileConstants.LINKAGE_STYLES_FILE_CFGLINK));
		
		keyToFile.put(CFGBW_RESIDUE_STYLES_FILE,theConfiguration.get(DICT_SECT, CFGBW_RESIDUE_STYLES_FILE, FileConstants.RESIDUE_STYLES_FILE_CFGBW));
		keyToFile.put(CFGBW_RESIDUE_PLACEMENTS_FILE,theConfiguration.get(DICT_SECT, CFGBW_RESIDUE_PLACEMENTS_FILE, FileConstants.RESIDUE_PLACEMENTS_FILE_CFGBW));
		keyToFile.put(CFGBW_LINKAGE_STYLES_FILE,theConfiguration.get(DICT_SECT, CFGBW_LINKAGE_STYLES_FILE, FileConstants.LINKAGE_STYLES_FILE_CFGBW));

		keyToFile.put(CFG_RESIDUE_STYLES_FILE,theConfiguration.get(DICT_SECT, CFG_RESIDUE_STYLES_FILE, FileConstants.RESIDUE_STYLES_FILE_CFG));
		keyToFile.put(CFG_RESIDUE_PLACEMENTS_FILE,theConfiguration.get(DICT_SECT, CFG_RESIDUE_PLACEMENTS_FILE, FileConstants.RESIDUE_PLACEMENTS_FILE_CFG));
		keyToFile.put(CFG_LINKAGE_STYLES_FILE,theConfiguration.get(DICT_SECT, CFG_LINKAGE_STYLES_FILE, FileConstants.LINKAGE_STYLES_FILE_CFG));
		
		keyToFile.put(SNFG_RESIDUE_STYLES_FILE,theConfiguration.get(DICT_SECT, SNFG_RESIDUE_STYLES_FILE, FileConstants.RESIDUE_STYLES_FILE_SNFG));
		keyToFile.put(SNFG_RESIDUE_PLACEMENTS_FILE,theConfiguration.get(DICT_SECT, SNFG_RESIDUE_PLACEMENTS_FILE, FileConstants.RESIDUE_PLACEMENTS_FILE_SNFG));
		keyToFile.put(SNFG_LINKAGE_STYLES_FILE,theConfiguration.get(DICT_SECT, SNFG_LINKAGE_STYLES_FILE, FileConstants.LINKAGE_STYLES_FILE_SNFG));
	}
	
	public static String getDefaultDictionaryFile(String dictionaryName){
		if(RESIDUE_TYPES_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_TYPES_FILE;
		if(TERMINAL_TYPES_FILE.equals(dictionaryName))
			return FileConstants.TERMINAL_TYPES_FILE;
		if(CORE_TYPES_FILE.equals(dictionaryName))
			return FileConstants.CORE_TYPES_FILE;
		if(CROSS_RING_FRAGMENT_TYPES_FILE.equals(dictionaryName))
			return FileConstants.CROSS_RING_FRAGMENT_TYPES_FILE;
		if(CROSS_LINKED_SUBSTITUENT_TYPES_FILE.equals(dictionaryName))
			return FileConstants.CROSS_LINKED_SUBSTITUENT_TYPES_FILE;
		if(NON_SYMBOLIC_RESIDUE_TYPES_FILE.equals(dictionaryName))
			return FileConstants.NON_SYMBOLIC_RESIDUE_TYPES_FILE;
		
		if(UOXF_RESIDUE_STYLES_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_STYLES_FILE_UOXF;
		if(UOXF_RESIDUE_PLACEMENTS_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_PLACEMENTS_FILE_UOXF;
		if(UOXF_LINKAGE_STYLES_FILE.equals(dictionaryName))
			return FileConstants.LINKAGE_STYLES_FILE_UOXF;
		
		if(UOXFCOL_RESIDUE_STYLES_FILE.equals(dictionaryName))
			return  FileConstants.RESIDUE_STYLES_FILE_UOXFCOL;
		if(UOXFCOL_RESIDUE_PLACEMENTS_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_PLACEMENTS_FILE_UOXF;
		if(UOXFCOL_LINKAGE_STYLES_FILE.equals(dictionaryName))
			return FileConstants.LINKAGE_STYLES_FILE_UOXF;
		
		if(TEXT_RESIDUE_STYLES_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_STYLES_FILE_TEXT;
		if(TEXT_RESIDUE_PLACEMENTS_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_PLACEMENTS_FILE_TEXT;
		if(TEXT_LINKAGE_STYLES_FILE.equals(dictionaryName))
			return FileConstants.LINKAGE_STYLES_FILE_TEXT;
		
		if(CFGLINK_RESIDUE_STYLES_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_STYLES_FILE_CFGLINK;
		if(CFGLINK_RESIDUE_PLACEMENTS_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_PLACEMENTS_FILE_CFGLINK;
		if(CFGLINK_LINKAGE_STYLES_FILE.equals(dictionaryName))
			return FileConstants.LINKAGE_STYLES_FILE_CFGLINK;
		
		if(CFGBW_RESIDUE_STYLES_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_STYLES_FILE_CFGBW;
		if(CFGBW_RESIDUE_PLACEMENTS_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_PLACEMENTS_FILE_CFGBW;
		if(CFGBW_LINKAGE_STYLES_FILE.equals(dictionaryName))
			return FileConstants.LINKAGE_STYLES_FILE_CFGBW;

		if(CFG_RESIDUE_STYLES_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_STYLES_FILE_CFG;
		if(CFG_RESIDUE_PLACEMENTS_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_PLACEMENTS_FILE_CFG;
		if(CFG_LINKAGE_STYLES_FILE.equals(dictionaryName))
			return FileConstants.LINKAGE_STYLES_FILE_CFG;
		
		if(SNFG_RESIDUE_STYLES_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_STYLES_FILE_SNFG;
		if(SNFG_RESIDUE_PLACEMENTS_FILE.equals(dictionaryName))
			return FileConstants.RESIDUE_PLACEMENTS_FILE_SNFG;
		if(SNFG_LINKAGE_STYLES_FILE.equals(dictionaryName))
			return FileConstants.LINKAGE_STYLES_FILE_SNFG;
		
		return null;
	}
	
	public boolean isDefault(String dictionaryName){
		return keyToFile.get(dictionaryName).equals(DictionaryConfiguration.getDefaultDictionaryFile(dictionaryName));
	}
	
	public boolean isRemoteResource(String dictionaryName){
		return keyToFile.get(dictionaryName).startsWith("http://");
	}
	
	public String getDictionaryResourceAsString(String dictionaryName) throws MalformedURLException, IOException{
		String filename=keyToFile.get(dictionaryName);
		
		return getResourceAsString(filename);
	}
	
	public static String getResourceAsString(String filename) throws MalformedURLException, IOException{
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
		
		StringBuffer buf=new StringBuffer();
		
		String line;
		while((line=is.readLine())!=null){
			buf.append(line+"\n");
		}
		
		return buf.toString();
	}
}
