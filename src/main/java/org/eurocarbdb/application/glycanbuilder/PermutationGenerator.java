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

import java.math.BigInteger;

/**
   Systematically generate permutations of N integer numbers.  The
   code is taken from <a href="http://www.merriampark.com/comb.htm">http://www.merriampark.com/comb.htm</a>
*/

public class PermutationGenerator {

    private int[] a;
    private int n;
    private int k;
    private BigInteger numLeft;
    private BigInteger total;
        
    /**
       Create a new generator 
       @param n the number of integer numbers in the permutation
     */
    public PermutationGenerator (int n) {
    
    this.n = n;
    a = new int[n];
    total = getFactorial (n);
    reset ();
    }

    /**
       Restart the generator
     */
    public void reset () {
    k = 0;
    for (int i = 0; i < a.length; i++) {
        a[i] = i;
    }
    numLeft = new BigInteger (total.toString ());
    }

    /**
       Return the number of permutations not yet generated
    */
    public BigInteger getNumLeft () {
    return numLeft;
    }

    /**
       Return <code>true</code> if there are more permutations yet to
       generate
    */
    public boolean hasMore () {
    return numLeft.compareTo (BigInteger.ZERO) == 1;
    }
    
    /**
       Return the total number of permutations that will be generated
     */
    public BigInteger getTotal () {
    return total;
    }

    /**
       Compute the factorial of an integer
    */    
    private static BigInteger getFactorial (int n) {
    BigInteger fact = BigInteger.ONE;
    for (int i = n; i > 1; i--) {
        fact = fact.multiply (new BigInteger (Integer.toString (i)));
    }
    return fact;
    }

    private void swap(int[] a, int i, int j) {
    int h = a[i];
    a[i] = a[j];
    a[j] = h;
    }

    /**
       Generate the next permutation (wiki)
    */
    public int[] getNext () {
    if (numLeft.equals (BigInteger.ZERO)) {
        return null;
    }

    if( n>1 ) {
        swap(a,k,k+1);           
        k = (k+1) % (n-1);
    }
    numLeft = numLeft.subtract (BigInteger.ONE);
    return a;
    
    }
}
