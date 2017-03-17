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

import java.util.Vector;
import java.util.Collection;

/**
   Extension to the Vector class that add some functionalities to
   create lists on a single line of code.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class Union<T> extends Vector<T> {

    /**
       Empty constructor
     */
    public Union() {
    super();
    }

    /**
       Create a new list with a single object.
     */
    public Union(T o) {
    this.add(o);
    }
    
    /**
       Create a new list from a collection of objects.
     */
    public Union(Collection<T> c) {
    this.addAll(c);
    }

    /**
       Return a copy of this list to which an additional object is
       added
     */
    public Union<T> and(T o) {
    Union<T> ret = new Union<T>();
    ret.addAll(this);
    ret.add(o);
    return ret;
    }   

    /**
       Return a copy of this list to which a collection of objects is
       added
     */
    public Union<T> and(Collection<T> c) {
    Union<T> ret = new Union<T>();
    ret.addAll(this);
    ret.addAll(c);
    return ret;
    }

    /**
       Return a list containing the objects present in both this and
       the other list.
     */
    public Union<T> intersect(Collection<T> b) {
    Union<T> ret = new Union<T>();
    for( T e : this ) {
        if( b.contains(e) )
        ret.add(e);
    }
    return ret;
    }

}

