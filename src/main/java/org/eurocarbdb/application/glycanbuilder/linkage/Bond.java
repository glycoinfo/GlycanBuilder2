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

package org.eurocarbdb.application.glycanbuilder.linkage;

import java.util.Arrays;

/**
 * Objects of this class contains the information about a chemical bond between
 * two atoms of two residues. The bond positions can be undefined or partially
 * defined.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class Bond {

	static protected class Comparator implements java.util.Comparator<Bond> {

		public int compare(Bond o1, Bond o2) {
			return (int) (o1.child_position - o2.child_position);
		}

	}

	private char[] parent_positions; // sorted
	private char child_position;

	int a_iProbabilityLow = 100;
	int a_iProbabilityHigh = 100;
	
	/**
	 * Create completely undefined bond.
	 */
	public Bond() {
		parent_positions = new char[] { '?' };
		child_position = '?';
	}

	/**
	 * Create a new bond between parent and child positions.
	 */
	public Bond(char p_pos, char c_pos) {
		parent_positions = new char[] { p_pos };
		child_position = c_pos;
	}

	/**
	 * Create a new bond between a list of possible parent positions and a child
	 * position.
	 */
	public Bond(char[] p_poss, char c_pos) {
		parent_positions = (char[]) p_poss.clone();
		child_position = c_pos;
		Arrays.sort(parent_positions);
	}

	/**
	 * Create a copy of this object.
	 */
	public Bond clone() {
		return new Bond(parent_positions, child_position);
	}

	/**
	 * Create and undefined bond object.
	 */
	static public java.util.Vector<Bond> single() {
		return new Union(new Bond('?', '?'));
	}

	/**
	 * Create and partially defined bond object where the parent position is
	 * specified.
	 */
	static public java.util.Vector<Bond> single(char p_pos) {
		return new Union(new Bond(p_pos, '?'));
	}

	/**
	 * Return the set of parent positions.
	 */
	public char[] getParentPositions() {
		return parent_positions;
	}

	/**
	 * Set the parent position.
	 */
	public void setParentPosition(char pos) {
		parent_positions = new char[] { pos };
	}

	/**
	 * Set the parent positions.
	 */
	public void setParentPositions(char[] poss) {
		parent_positions = (char[]) poss.clone();
		Arrays.sort(parent_positions);
	}

	/**
	 * Return the child position.
	 */
	public char getChildPosition() {
		return child_position;
	}

	/**
	 * Set the child position.
	 */
	public void setChildPosition(char pos) {
		child_position = pos;
	}

	public void setProbabilityLow(double a_dLow) {
		this.a_iProbabilityLow = (int)(a_dLow * 100);
	}
	
	public void setProbabilityHigh(double a_dHigh) {
		this.a_iProbabilityHigh = (int) (a_dHigh * 100);
	}
	
	public void setProbabilityLow(int a_iLow) {
		this.a_iProbabilityLow = a_iLow;
	}
	
	public void setProbabilityHigh(int a_iHigh) {
		this.a_iProbabilityHigh = a_iHigh;
	}
	
	public int getProbabilityLow() {
		return this.a_iProbabilityLow;
	}
	
	public int getProbabilityHigh() {
		return this.a_iProbabilityHigh;
	}
	
	private boolean fuzzyMatch(char c1, char c2) {
		return match(c1, c2, true);
	}

	private boolean match(char c1, char c2, boolean fuzzy) {
		return (c1 == c2 || (fuzzy && (c1 == '?' || c2 == '?')));
	}

	public boolean fuzzyMatch(Bond other) {
		return match(other, true);
	}

	/**
	 * Return <code>true</code> if the two objects represents the same bond. And
	 * undefined position represent a wildcard.
	 */
	public boolean match(Bond other, boolean fuzzy) {
		if (other == null)
			return false;

		// match child position
		if (!match(this.child_position, other.child_position,true))
			return false;

		// match parent positions
		if (this.parent_positions.length < other.parent_positions.length)
			return false;

		int matched = 0;
		for (int l = 0, i = 0; l < other.parent_positions.length; l++) {
			for (; i < this.parent_positions.length; i++) {
				if (match(this.parent_positions[i],
						other.parent_positions[l], fuzzy)) {
					matched++;
					i++;
					break;
				}
			}
		}
		return (matched == other.parent_positions.length);
	}

	public boolean equals(Object other) {
		if (other == null || !(other instanceof Bond))
			return false;

		Bond bother = (Bond) other;
		if (this.child_position != bother.child_position)
			return false;
		if (this.parent_positions.length != bother.parent_positions.length)
			return false;
		for (int i = 0; i < this.parent_positions.length; i++) {
			if (this.parent_positions[i] != bother.parent_positions[i])
				return false;
		}
		return true;
	}

	public int hashCode() {
		int ret = 1;
		ret *= (int) child_position;
		for (char c : parent_positions)
			ret *= (int) c;
		return ret;
	}

}