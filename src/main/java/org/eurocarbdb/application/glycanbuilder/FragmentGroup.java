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

/**
   Container for a collection of fragments with the same mass/charge
   value grouped by structure
   
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/


class FragmentGroup {


	private LinkedList<LinkedList<FragmentEntry>> fragments;

	/**
       Empty constructor
	 */
	public FragmentGroup() {
		fragments = new LinkedList<LinkedList<FragmentEntry>>();
	}

	/**
       Return the collection of all fragment entries
	 */
	public LinkedList<LinkedList<FragmentEntry>> getFragmentEntries() {
		return fragments;
	}

	/**
       Return the collection of all fragment entries for a specific
       group
       @param s_ind the index of the group
	 */
	public LinkedList<FragmentEntry> getFragmentEntries(int s_ind) {
		return fragments.get(s_ind);
	}

	/**
       Return the collection of all fragment structures for a specific group
       @param s_ind the index of the group
	 */
	public LinkedList<Glycan> getFragments(int s_ind) {
		LinkedList<Glycan> ret = new LinkedList<Glycan>();
		for( FragmentEntry fe : fragments.get(s_ind) )
			ret.add(fe.fragment);
		return ret;
	}

	/**
       Return <code>true</code> if there are no fragment entries in
       the container
	 */
	public boolean isEmpty() {
		for( LinkedList<FragmentEntry> vfe : fragments ) 
			if( vfe.size()>0 )
				return false;
		return true;
	}

	public void assertSize(int s_ind) {
		// make space
		while( fragments.size()<=s_ind )
			fragments.add(new LinkedList<FragmentEntry>());
	}

	public void addFragment(int s_ind, FragmentEntry fe) {
		assertSize(s_ind);
		fragments.get(s_ind).add(fe);
	}

	public void removeFragments(int s_ind) {
		fragments.remove(s_ind);    
	}

	public void removeFragment(int s_ind, FragmentEntry fe) {
		if( s_ind>=fragments.size() )
			return;

		LinkedList<FragmentEntry> vfe = fragments.get(s_ind);
		for( int i=0; i<vfe.size(); i++ ) {
			if( vfe.get(i).equals(fe) ) {
				vfe.remove(i);
				return;
			}
		}
	}

}