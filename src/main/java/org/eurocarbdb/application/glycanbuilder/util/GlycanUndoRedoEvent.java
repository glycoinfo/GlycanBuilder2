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
   Contains information about an undo/redo action performed by a
   {@link GlycanUndoManager}

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class GlycanUndoRedoEvent {

	private GlycanUndoManager src;
	private boolean is_undo;

	/**
       Default constructor
       @param _src the source of the event
       @param _is_undo <code>true</code> if the cause of the event was
       an undo action
	 */
	public GlycanUndoRedoEvent(GlycanUndoManager _src, boolean _is_undo) {
		src = _src;
		is_undo = _is_undo;
	}

	/**
       Return <code>true</code> if the cause of the event was an undo
       action
	 */
	public boolean isUndo() {
		return is_undo;
	}

	/**
       Return the source of the event     
	 */
	public GlycanUndoManager getSource() {
		return src;
	}


}