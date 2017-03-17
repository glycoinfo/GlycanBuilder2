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

import java.io.*;
import java.util.*;

import javax.swing.*;

/**
   Manage the recently added residue history. Other classes can
   register to listen for changes to the history. The history mantains
   only the 3 most recently added residue.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class ResidueHistory {
    
    // classes    
    /**
       Listener for events raised when the recently added residue
       history is changed.
    */
    public interface Listener {
    
    /**
       Called when the residue history has changed.
    */
    public void residueHistoryChanged();
    }


    // constants 
    static private final int RECENT_RESIDUES_QUEUE_SIZE = 3;

    // members

    protected LinkedList<String> recent_residues;    
    protected Vector<ResidueHistory.Listener> listeners;

    // methods

    /**
       Default constructor
     */
    public ResidueHistory() {
    recent_residues = new LinkedList<String>();
    listeners = new Vector<ResidueHistory.Listener>();
    }    

    /**
       Register a new listener for changes to the recently added
       residue history
    */
    public void addHistoryChangedListener(ResidueHistory.Listener l) {
    if( l!=null )
        listeners.add(l);
    }

    /**
       Deregister a listener for changes to the recently added residue
       history
    */
    public void removeHistoryChangedListener(ResidueHistory.Listener l) {
    if( l!=null )
        listeners.remove(l);
    }

    /**
       Clear the history
     */
    public void clear() {
    recent_residues.clear();
    }

    /**
       Return the actual number of entries in the history
     */
    public int size() {
    return recent_residues.size();
    }

    /**
       Add a new residue to the history       
     */
    public void add(Residue r) {
    if( r!=null )
        add(r.getType());
    }
    
    /**
       Add a new residue type to the history
     */
    public void add(ResidueType type) {
    if( type!=null && type.getToolbarOrder()==0 && !recent_residues.contains(type.getName()) ) {
        // update list
        if( recent_residues.size()==RECENT_RESIDUES_QUEUE_SIZE ) 
        recent_residues.removeLast();
        recent_residues.addFirst(type.getName());
    
        // fire event
        for(Iterator<ResidueHistory.Listener> i=listeners.iterator(); i.hasNext(); ) 
        i.next().residueHistoryChanged();
    }
    }
       
    /**
       Return the list type names of the recently added residues
     */
    public Collection<String> getRecentResidues() {
    return recent_residues;
    }
    
    /**
       Return an iterator over the list of type names of the recently
       added residues
     */
    public Iterator<String> iterator() {
    return recent_residues.iterator();
    }
   
    /**
       Store the history in the configuration
    */
    public void store(Configuration config) {
    int c = 0;    
    config.put("ResidueHistory", "queue_size", recent_residues.size());
    for( String typename : recent_residues ) {
        config.put("ResidueHistory", "type_name" + c, typename);
        c++;
    }
    }

    /**
       Retrieve the history from the configuration
    */
    public void retrieve(Configuration config) {    
    recent_residues.clear();
    int queue_size = config.get("ResidueHistory", "queue_size", 0);
    for( int c=0; c<queue_size && c<RECENT_RESIDUES_QUEUE_SIZE; c++ ) 
        recent_residues.add(config.get("ResidueHistory", "type_name" + c));    
    }
    
}