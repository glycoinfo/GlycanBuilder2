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
package org.eurocarbdb.application.glycanbuilder.fileutil;

/**
   Contains the paths to the configuration files used to initialize
   the dictionaries.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/


public class FileConstants {

    // files 
    public static final String RESIDUE_TYPES_FILE = "/conf/residue_types";
    public static final String CORE_TYPES_FILE = "/conf/core_types";
    public static final String TERMINAL_TYPES_FILE = "/conf/terminal_types";
    public static final String CROSS_RING_FRAGMENT_TYPES_FILE = "/conf/cross_ring_fragment_types";
    public static final String CROSS_LINKED_SUBSTITUENT_TYPES_FILE = "/conf/cross_linked_substituent_types";
    public static final String NON_SYMBOLIC_RESIDUE_TYPES_FILE = "/conf/non_symbolic_residue_types";
    
    public static final String RESIDUE_STYLES_FILE_CFG     = "/conf/residue_styles_cfg";
    public static final String RESIDUE_STYLES_FILE_CFGBW   = "/conf/residue_styles_cfgbw";
    public static final String RESIDUE_STYLES_FILE_CFGLINK = "/conf/residue_styles_cfg";
    public static final String RESIDUE_STYLES_FILE_UOXF    = "/conf/residue_styles_uoxf";
    public static final String RESIDUE_STYLES_FILE_TEXT    = "/conf/residue_styles_text";
    public static final String RESIDUE_STYLES_FILE_UOXFCOL = "/conf/residue_styles_uoxfcol";
    public static final String RESIDUE_STYLES_FILE_SNFG = "/conf/residue_styles_snfg";

    public static final String RESIDUE_PLACEMENTS_FILE_CFG     = "/conf/residue_placements_cfg";
    public static final String RESIDUE_PLACEMENTS_FILE_CFGBW   = "/conf/residue_placements_cfg";
    public static final String RESIDUE_PLACEMENTS_FILE_CFGLINK = "/conf/residue_placements_uoxf";
    public static final String RESIDUE_PLACEMENTS_FILE_UOXF    = "/conf/residue_placements_uoxf";
    public static final String RESIDUE_PLACEMENTS_FILE_TEXT    = "/conf/residue_placements_cfg";
    public static final String RESIDUE_PLACEMENTS_FILE_SNFG    = "/conf/residue_placements_snfg";
    
    public static final String LINKAGE_STYLES_FILE_CFG     = "/conf/linkage_styles_cfg";
    public static final String LINKAGE_STYLES_FILE_CFGBW   = "/conf/linkage_styles_cfg";
    public static final String LINKAGE_STYLES_FILE_CFGLINK = "/conf/linkage_styles_cfglink";
    public static final String LINKAGE_STYLES_FILE_UOXF    = "/conf/linkage_styles_uoxf";
    public static final String LINKAGE_STYLES_FILE_TEXT    = "/conf/linkage_styles_cfg";    
    public static final String LINKAGE_STYLES_FILE_SNFG    = "/conf/linkage_styles_snfg";
}
    
