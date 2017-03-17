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

import java.text.DecimalFormat;

/**
   Type class representing a fraction as a pair of integer numbers
   
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class IntPair implements Comparable<IntPair> {

	protected int n;
	protected int d;
	protected double p;

	/**
       Create a new object representing the fraction 1/1
	 */
	public IntPair() {
		n = 1;
		d = 1;
		p = 100.;
	}

	/**
       Create a new object as a fraction of two integer number
       @param _n the numerator
       @param _d the denominator
	 */
	public IntPair(int _n, int _d) {
		n = _n;
		d = _d;
		p = 100.*n/d;
	}

	public int compareTo(IntPair other) {
		if( this.p<other.p )
			return -1;
		if( this.p>other.p )
			return +1;
		return 0;
	}

	/**
       Return the value of the numerator
	 */
	public int getN() {
		return n;
	}

	/**
       Return the value of the denominator
	 */
	public int getD() {
		return d;
	}

	/**
       Return the value of the fraction as a percentage
	 */
	public double getP() {
		return p;
	}

	public String toString() {
		return ("" + getN() + "/" + getD() + " (" + new DecimalFormat("0.00").format(getP()) + "%)");
	}
}