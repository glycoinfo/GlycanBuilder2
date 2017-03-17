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
   Implement a simple monitor that is used by the application to lock
   the user interface during long computations that must run
   exclusively

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class Monitor {

    private Object owner = null;
    private int no_holds = 0;    

    /**
       Default constructor
       @param _owner the application that manages the user interface
     */
    public Monitor(Object _owner) {
    owner = _owner;
    no_holds = 0;
    }

    /**
       Return the owner of this monitor
     */
    public Object getOwner() {
    return owner;
    }

    /**
       Return <code>true</code> if the monitor has not been locked
     */
    public boolean isFree() {
    return (no_holds==0);
    }

    /**
       Lock the monitor. The monitor can be locked multiple times and
       must be released the same amount of times to be freed.
     */
    public void hold() {
    no_holds++;
    }

    /**
       Release the monitor. The monitor can be locked multiple times
       and must be released the same amount of times to be freed.
     */
    public void release() {
    if( no_holds>0 )
        no_holds--;
    }
        
}
