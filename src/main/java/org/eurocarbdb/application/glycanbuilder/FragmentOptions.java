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

/**
   Contain the set of option used to specify which type of fragments
   the {@link Fragmenter} should compute
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class FragmentOptions {

	/** Specify if type A ring fragments should be computed (default = false)*/
	public boolean ADD_AFRAGMENTS = false;
	/** Specify if type B cleavages should be computed (default = true)*/
	public boolean ADD_BFRAGMENTS = true; 
	/** Specify if type B cleavages should be computed (default = true)*/
	public boolean ADD_CFRAGMENTS = true; 
	/** Specify if type X ring fragments should be computed (default = false)*/
	public boolean ADD_XFRAGMENTS = false;
	/** Specify if type Y cleavages should be computed (default = true)*/
	public boolean ADD_YFRAGMENTS = true; 
	/** Specify if type Z cleavages should be computed (default = true)*/
	public boolean ADD_ZFRAGMENTS = true;    

	/** Specify if fragments with internal cross ring cleavages
    should be computed (default = false) */
	public boolean INTERNAL_FRAGMENTS = false;

	/** Specify the maximum number of glycosidic cleavages to compute
    for a fragment (default = 2) */
	public int MAX_NO_CLEAVAGES  = 2;
	/** Specify the maximum number of cross-ring cleavages to compute
    for a fragment (default = 1) */
	public int MAX_NO_CROSSRINGS = 1;   

	public boolean ITERATE_ION_COMBINATIONS=false;

	// pojo

	/** Return <code>true</code> if type A ring fragments should be
    computed */
	public boolean getAddAFragments() {
		return ADD_AFRAGMENTS;
	}

	/** Specify if type A ring fragments should be computed */
	public void setAddAFragments(boolean f) {
		ADD_AFRAGMENTS = f;
	}

	/** Return <code>true</code> if type B cleavages should be
    computed */
	public boolean getAddBFragments() {
		return ADD_BFRAGMENTS;
	}

	/** Specify if type B cleavages should be computed */
	public void setAddBFragments(boolean f) {
		ADD_BFRAGMENTS = f;
	}

	/** Return <code>true</code> if type C cleavages should be
    computed */
	public boolean getAddCFragments() {
		return ADD_CFRAGMENTS;
	}

	/** Specify if type C cleavages should be computed */
	public void setAddCFragments(boolean f) {
		ADD_CFRAGMENTS = f;
	}

	/** Return <code>true</code> if type X ring fragments should be
    computed */
	public boolean getAddXFragments() {
		return ADD_XFRAGMENTS;
	}

	/** Specify if type A ring fragments should be computed */
	public void setAddXFragments(boolean f) {
		ADD_XFRAGMENTS = f;
	}

	/** Return <code>true</code> if type Y cleavages should be
    computed */
	public boolean getAddYFragments() {
		return ADD_YFRAGMENTS;
	}

	/** Specify if type Y cleavages should be computed */
	public void setAddYFragments(boolean f) {
		ADD_YFRAGMENTS = f;
	}

	/** Return <code>true</code> if type Z cleavages should be
    computed */
	public boolean getAddZFragments() {
		return ADD_XFRAGMENTS;
	}

	/** Specify if type Z cleavages should be computed */
	public void setAddZFragments(boolean f) {
		ADD_ZFRAGMENTS = f;
	}

	/** Return <code>true</code> if fragments with internal cross ring
    cleavages should be computed */
	public boolean getInternalFragments() {
		return INTERNAL_FRAGMENTS;
	}

	/** Specify if fragments with internal cross ring cleavages
    should be computed */
	public void setInternalFragments(boolean f) {
		INTERNAL_FRAGMENTS = f;
	}

	/** Return the maximum number of glycosidic cleavages to compute
    for a fragment */
	public int getMaxNoCleavages() {
		return MAX_NO_CLEAVAGES;
	}

	/** Specify the maximum number of glycosidc cleavages to compute
    for a fragment */
	public void setMaxNoCleavages(int i) {
		MAX_NO_CLEAVAGES = i;
	}

	/** Return the maximum number of crossring cleavages to compute
    for a fragment  */
	public int getMaxNoCrossrings() {
		return MAX_NO_CROSSRINGS;
	}

	/** Specify the maximum number of crossring cleavages to compute
    for a fragment  */
	public void setMaxNoCrossrings(int i) {
		MAX_NO_CROSSRINGS = i;
	}



	// serialization

	public boolean isITERATE_ION_COMBINATIONS() {
		return ITERATE_ION_COMBINATIONS;
	}

	public void setITERATE_ION_COMBINATIONS(boolean iTERATE_ION_COMBINATIONS) {
		ITERATE_ION_COMBINATIONS = iTERATE_ION_COMBINATIONS;
	}

	public void store(Configuration config) {
		config.put("FragmentOptions","add_afragments",ADD_AFRAGMENTS);
		config.put("FragmentOptions","add_bfragments",ADD_BFRAGMENTS);
		config.put("FragmentOptions","add_cfragments",ADD_CFRAGMENTS);
		config.put("FragmentOptions","add_xfragments",ADD_XFRAGMENTS);
		config.put("FragmentOptions","add_yfragments",ADD_YFRAGMENTS);
		config.put("FragmentOptions","add_zfragments",ADD_ZFRAGMENTS);

		config.put("FragmentOptions","internal_fragments",INTERNAL_FRAGMENTS);

		config.put("FragmentOptions","max_no_cleavages",MAX_NO_CLEAVAGES);
		config.put("FragmentOptions","max_no_crossrings",MAX_NO_CROSSRINGS);
	}

	public void retrieve(Configuration config) {
		ADD_AFRAGMENTS = config.get("FragmentOptions","add_afragments",ADD_AFRAGMENTS);
		ADD_BFRAGMENTS = config.get("FragmentOptions","add_bfragments",ADD_BFRAGMENTS);
		ADD_CFRAGMENTS = config.get("FragmentOptions","add_cfragments",ADD_CFRAGMENTS);
		ADD_XFRAGMENTS = config.get("FragmentOptions","add_xfragments",ADD_XFRAGMENTS);
		ADD_YFRAGMENTS = config.get("FragmentOptions","add_yfragments",ADD_YFRAGMENTS);
		ADD_ZFRAGMENTS = config.get("FragmentOptions","add_zfragments",ADD_ZFRAGMENTS);

		INTERNAL_FRAGMENTS = config.get("FragmentOptions","internal_fragments",INTERNAL_FRAGMENTS);

		MAX_NO_CLEAVAGES  = config.get("FragmentOptions","max_no_cleavages",MAX_NO_CLEAVAGES);
		MAX_NO_CROSSRINGS = config.get("FragmentOptions","max_no_crossrings",MAX_NO_CROSSRINGS);
	}
}