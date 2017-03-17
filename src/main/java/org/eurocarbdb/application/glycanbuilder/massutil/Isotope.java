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

package org.eurocarbdb.application.glycanbuilder.massutil;

import java.util.*;

import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
   Contains all information about an isotope.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class Isotope {

    private String symbol;
    private String atom_symbol;
    private boolean is_stable;
    private int atomic_number;
    private double mass;
    private double abundance;
    
    /**
       Create a new object by parsing the information from a
       initialization string.
       @param init initialization string composed of 6 tab separated
       values: chemical symbol of the isotope, chemical symbol of the
       atom, atomic number, <code>true</code> if the isotope is stable,
       mass, relative abundance.
       @throws Exception if the initialization string is not the
       correct format
     */

    public Isotope(String init) throws Exception {
    	LinkedList<String> tokens = TextUtils.tokenize(init,"\t");
    	if( tokens.size()!=6 ) 
    		throw new Exception("Invalid line: " + init);

    	symbol = tokens.get(0);
    	atom_symbol = tokens.get(1);
    	atomic_number = Integer.valueOf(tokens.get(2));
    	is_stable = tokens.get(3).equals("1") || tokens.get(3).equals("true") || tokens.get(3).equals("yes");
    	mass = Double.valueOf(tokens.get(4));
    	abundance = Double.valueOf(tokens.get(5));
    }    
    
    /**
       Return <code>true</code> if the two objects represent the same
       isotope.
    */
    public boolean equals(Object o) {
    if( o==null )
        return false;
    if( !(o instanceof Isotope) )
        return false;
    return this.symbol.equals(((Isotope)o).symbol);
    }
    
    /**
       Lexicographic comparison of the chemical symbols of the two
       isotopes.
    */
    public int compareTo(Isotope a) {
    if( a==null )
        return 1;
    return this.symbol.compareTo(a.symbol);
    }
    
    /**
       Return the chemical symbol of this isotope.
     */

    public String getSymbol() {
    return symbol;
    }
    
    /**
       Return the chemical symbol of the atom represented by this isotope.
     */

    public String getAtomSymbol() {
    return atom_symbol;
    }
    

    /**
       Return the atomic number.
     */
    public int getAtomicNumber() {
    return atomic_number;
    }
    
    /**
       Return <code>true</code> if the isotope is stable.
    */
    public boolean isStable() {
    return is_stable;
    }
    
    /**
       Return the mass of the isotope.
    */
    public double getMass() {
    return mass;
    }

    /**
       Return the relative abundance of this isotope.
    */
    
    public double getAbundance() {
    return abundance;
    }
}