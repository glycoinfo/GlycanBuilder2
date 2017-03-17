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

import org.eurocarbdb.application.glycanbuilder.util.GlycanUndoRedoEvent;
import org.eurocarbdb.application.glycanbuilder.util.GlycanUndoRedoListener;


/**
   Simple implementation of a manager for undo/redo of a {@link
   BaseDocument} object. Every time the underlying document changes the
   complete state of the object is save using the {@link
   BaseDocument#toString} method. The previous state of the document
   is restored using the {@link BaseDocument#fromString(String)}
   method. The manage can store a limited number of previous document
   states.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class GlycanUndoManager implements BaseDocument.DocumentChangeListener {

	private static final int MAXIMUM_NUMBER_OF_STATES = 20;

	//

	protected BaseDocument theDoc = null;

	protected boolean doing = false;

	protected int cur_state = -1;
	protected int no_actions = 0;
	protected boolean was_changed = false;
	protected LinkedList<String> states = new LinkedList<String>();

	protected LinkedList<GlycanUndoRedoListener> listeners = new LinkedList<GlycanUndoRedoListener>();

	//

	/**
       Default constructor
       @param _theDoc the document was state changes are observed by
       this undo manager
	 */
	public GlycanUndoManager(BaseDocument _theDoc) {
		theDoc = _theDoc;
		theDoc.addDocumentChangeListener(this);

		reset();
	}

	/**
       Register a listener that is notified every time the underlying
       document's state is changed
	 */
	public void addUndoRedoListener(GlycanUndoRedoListener l) {
		if( l!=null ) listeners.add(l);
	}

	/**
       De-reegister one of the listeners that are notified every time
       the underlying document's state is changed
	 */
	public void removeUndoRedoListener(GlycanUndoRedoListener l) {
		if( l!=null ) listeners.remove(l);
	}

	/**
       Reset the state of the undo manager. Clear the all the stored
       information
	 */
	public void reset() {
		states.clear();

		states.add(theDoc.toString());
		cur_state = 0;
		no_actions = 0;
		was_changed = theDoc.hasChanged();
	}

	/**
       Return <code>true</code> if the state of the underlying
       document has changed and can be restored
	 */
	public boolean canUndo() {
		return (cur_state>0);
	}

	/**
       Restore the state of the underlying document if possible
	 */
	public void undo() throws Exception {
		if( cur_state<=0 ) return;

		doing = true;
		cur_state--;
		no_actions--;
		theDoc.fill(states.get(cur_state),(no_actions==0 && !was_changed));
		doing = false;

		fireUndoRedoAction(true);
	}

	/**
       Return <code>true</code> if the state of the underlying
       document has been restored to a previous state and the changes
       can be applied again
	 */
	public boolean canRedo() {
		return (cur_state<(states.size()-1));
	}

	/**
       Apply the saved changes to the underlying document if it has
       been restored to a previous state
	 */
	public void redo() throws Exception {
		if( cur_state>=(states.size()-1) ) return;

		doing = true;
		cur_state++;
		no_actions++;
		theDoc.fill(states.get(cur_state),false);
		doing = false;

		fireUndoRedoAction(false);
	}

	/**
       Return <code>true</code> if the underlying document is changed
       from the previous stored state 
	 */
	public boolean isChanged() {
		if( cur_state>=0 ) {
			String last_state_str = states.get(cur_state);
			String cur_state_str = theDoc.toString();
			return !last_state_str.equals(cur_state_str);
		}
		return true;
	}

	//

	/**
       Notify all listeners that either an undo or redo action has
       been performed
	 */
	public void fireUndoRedoAction(boolean is_undo) {
		GlycanUndoRedoEvent e = new GlycanUndoRedoEvent(this,is_undo);
		for(Iterator<GlycanUndoRedoListener> i=listeners.iterator(); i.hasNext(); ) 
			i.next().undoRedoHappened(e);    
	}

	/**
       React to the initialization of the underlying document by
       clearing the saved states. This event can happens either after
       the document has been renewed or saved.  
       @see #reset
	 */
	public void documentInit(BaseDocument.DocumentChangeEvent e) {
		if( e.getSource()==theDoc )
			reset();
	}

	/**
       Listen for changes in the underlying document. Store the
       current document state.
	 */
	public void documentChanged(BaseDocument.DocumentChangeEvent e) {
		if( e.getSource()==theDoc && !doing && isChanged() ) {

			// clear following actions
			while( cur_state<(states.size()-1) ) 
				states.removeLast();        

			// add new action
			states.addLast(theDoc.toString());

			// limit the size of the queue
			while( states.size()>MAXIMUM_NUMBER_OF_STATES )
				states.removeFirst();

			// update indices
			cur_state = states.size()-1;    
			no_actions++;
		}
	}
}