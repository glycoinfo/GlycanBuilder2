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
   Contains all information about a component of a {@link Molecule}
   object.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class Atom implements Comparable<Atom> {
   
    private String symbol;
    private String name;
    private double main_mass;
    private double avg_mass;
    
    /**
       Create a new object by parsing the information from a
       initialization string.
       @param init initialization string composed of 4 tab separated
       values: chemical symbol, name, mono-isotopic mass,
       average mass
       @throws Exception if the initialization string is not the
       correct format
     */
    public Atom(String init) throws Exception {
    	LinkedList<String> tokens = TextUtils.tokenize(init,"\t");
    	if( tokens.size()!=4 ) 
    		throw new Exception("Invalid line: " + init);

    	symbol = tokens.get(0);
    	name = tokens.get(1);
    	main_mass = Double.valueOf(tokens.get(2));
    	avg_mass = Double.valueOf(tokens.get(3));
    }
    
    /**
       Return <code>true</code> if the two objects represent the same
       atom.
     */
    
    public boolean equals(Object o) {
    if( o==null )
        return false;
    if( !(o instanceof Atom) )
        return false;
    return this.symbol.equals(((Atom)o).symbol);
    }
    
    /**
       Lexicographic comparison of the chemical symbols of the two
       atoms.
     */

    public int compareTo(Atom a) {
    if( a==null )
        return 1;
    return this.symbol.compareTo(a.symbol);
    }
    
    /**
       Return the chemical symbol of this atom.
     */

    public String getSymbol() {
    return symbol;
    }
    
    /**
       Return the name of this atom.
     */
    public String getName() {
    return name;
    }
    
    /**
       Return the mass of this atom given the current isotopic
       settings.
     */

    public double getMass() {
    return main_mass;
    }

    /**
       Return the mono-isotopic mass of this atom.
     */

    public double getMainMass() {
    return main_mass;
    }
    
    /**
       Return the average mass of this atom.
     */

    public double getAverageMass() {
    return avg_mass;
    }
}