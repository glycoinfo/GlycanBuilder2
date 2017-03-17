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

package org.eurocarbdb.application.glycanbuilder.util;

import org.eurocarbdb.application.glycanbuilder.GlycanUndoManager;

/**
   Interface that should be implemented by all objects that want to
   register to receive events from a {@link GlycanUndoManager} in case
   an undo/redo action is performed

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public interface GlycanUndoRedoListener {
    
    /**
       Method called by the {@link GlycanUndoManager} in case an
       undo/redo action is performed
     */
    public void undoRedoHappened(GlycanUndoRedoEvent e);

    
}