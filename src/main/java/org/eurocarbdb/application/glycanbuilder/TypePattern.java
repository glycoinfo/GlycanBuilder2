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
   Store a pattern defined by a certain number of objects of different
   types. Each object type is identified by a string.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class TypePattern implements Comparable<TypePattern>  {
    
    private TreeMap<String,Integer> pattern = new TreeMap<String,Integer>();
    private int count = 0;
    
    /**
       Empty constructor
     */
    public TypePattern() {
    }

    /**
       Return a copy of this object
     */
    public TypePattern clone() {
    TypePattern ret = new TypePattern();
    ret.pattern = (TreeMap<String,Integer>)pattern.clone();
    ret.count = count;
    return ret;
    }
    
    public boolean equals(Object other) {
    if( !(other instanceof TypePattern) )
        return false;
    return toString().equals(other.toString());
    }

    public int hashCode() {
    return toString().hashCode();
    }
    
    /**
       Return <code>true<code> if the other pattern is contained in
       this one. Inclusion is defined by the quantities of each typed
       object.
     */
    public boolean contains(TypePattern other) {
    if( other==null )
        return true;

    if( count<other.count )
        return false;
    
    for(Map.Entry<String,Integer> entry : other.pattern.entrySet() ) {
        Integer value = pattern.get(entry.getKey());
        if( value==null || value.intValue()<entry.getValue().intValue() )
        return false;
    }
    return true;
    }

    public int compareTo(TypePattern other) {
    if( other==null )
        return +1;
    if( count<other.count )
        return -1;

    for(Map.Entry<String,Integer> entry : other.pattern.entrySet() ) {
        Integer value = pattern.get(entry.getKey());
        if( value==null || value.intValue()<entry.getValue().intValue() )
        return -1;
    }
    if( count==other.count )
        return 0;
    return 1;
    }

    /**
       Return the total number of typed objects in this pattern
     */
    public int size() {
    return count;
    }

    /**
       Add a typed object to the pattern
       @param type the string representing the object type
     */
    public void add(String type) {
    add(type,1);
    }
    
    /**
       Add a typed object to the pattern
       @param type the string representing the object type
       @param num the number of objects to add
     */
    public void add(String type, int num) {
    if( num==0 )
        return;
    
    Integer old_num = pattern.get(type);
    if( old_num==null )
        pattern.put(type,num);
    else {
        if( (old_num+num)==0 )
        pattern.remove(type);
        else
        pattern.put(type,old_num+num);    
    }
    count +=num;
    }

    /**
       Return a copy of this object to which another typed object has
       been added
       @param type the string representing the object type
     */
    public TypePattern and(String type) {
    return and(type,1);
    }

    /**
       Return a copy of this object to which another typed object has
       been added
       @param num the number of objects to add
       @param type the string representing the object type
     */
    public TypePattern and(String type, int num) {
    TypePattern ret = this.clone();
    ret.add(type,num);
    return ret;
    }

    /**
       Return the list of the types of all objects in the pattern
     */
    public Collection<String> getTypes() {
    Vector<String> types = new Vector<String>();
    for( Map.Entry<String,Integer> e : pattern.entrySet() ) 
        for( int i=0; i<e.getValue(); i++ )
        types.add(e.getKey());
    return types;
    }

    /**
       Return all subpatterns of this pattern with a certain size. A
       sub-pattern is a combination of the typed objects which is
       included in this pattern
       @see #contains       
     */
    public Collection<TypePattern> subPatterns(int size) {
    Vector<TypePattern> sub_patterns = new Vector<TypePattern>();
    subPatterns(new TypePattern(),pattern.entrySet().iterator(),size,sub_patterns);
    return sub_patterns;
    }

    private void subPatterns(TypePattern toadd, Iterator<Map.Entry<String,Integer>> current, int size, Vector<TypePattern> buffer) {
    if( size==0 )
        buffer.add(toadd);
    else if( current.hasNext() ) {
        Map.Entry<String,Integer> type_num = current.next();
        for( int i=0; i<=type_num.getValue() && i<=size; i++ )
        subPatterns(toadd.and(type_num.getKey(),i),current,size-i,buffer);

    }
    }

    /**
       Subtract another object from the current one. Subtraction is
       performed by computing the differences in quantities for each
       typed object
     */
    public TypePattern subtract(TypePattern other) {
    TypePattern ret = this.clone();
    for( Map.Entry<String,Integer> e : other.pattern.entrySet() ) 
        ret.add(e.getKey(),-e.getValue());
    return ret;
    }
    
    public String toString() {
    StringBuilder sb = new StringBuilder();
    for( Map.Entry<String,Integer> e : pattern.entrySet() ) {
        if( e.getValue()>0 )
        sb.append("+");
        else if( e.getValue()==-1 )
        sb.append("-");

        if( e.getValue()!=1 && e.getValue()!=-1 )
        sb.append(e.getValue());
        sb.append(e.getKey());
    }

    return sb.toString();
    }

}