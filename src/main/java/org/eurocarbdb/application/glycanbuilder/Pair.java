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
   Store a pair of object of generic types

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class Pair<F,S> {

    protected F first;
    protected S second;

    /**
       Empty constructor
     */
    public Pair() {
    }

    /**
       Create a new pair of two objects
     */
    public Pair(F _first, S _second) {
    first = _first;
    second = _second;
    }   

    /**
       Return the first component of the pair
     */
    public F getFirst() {
    return first;
    }

    /**
       Set the first component of the pair
     */
    public void setFirst(F _first) {
    first = _first;    
    }
    
    /**
       Return the second component of the pair
     */
    public S getSecond() {
    return second;
    }

    /**
       Set the second component of the pair
     */
    public void setSecond(S _second) {
    second = _second;
    }

    /**
       Return a string representation of the pair by using the {@link
       Object#toString} method of the two components.
     */
    public String toString() {
    return "(" + first + "," + second + ")";
    }
    
}