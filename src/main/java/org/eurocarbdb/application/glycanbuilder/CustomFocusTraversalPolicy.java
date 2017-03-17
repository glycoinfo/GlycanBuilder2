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
import java.awt.*;

/**
   Define a custom focus traversal policy for a dialog as a sequence
   of components.
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class CustomFocusTraversalPolicy extends FocusTraversalPolicy {

	private LinkedList<Component> sequence = new LinkedList<Component>();

	/**
       Remove all components from the sequence.
	 */
	public void clear() {
		sequence.clear();
	}

	/**
       Add a component to the sequence.
	 */
	public void addComponent(Component c) {
		if( c!=null ) sequence.add(c);    
	}       

	public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {    
		int ind = sequence.indexOf(aComponent);
		if( ind==-1 ) return getFirstComponent(focusCycleRoot);

		int next = (ind+1==sequence.size()) ?0 :ind+1;
		return sequence.get(next);
	}

	public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
		int ind = sequence.indexOf(aComponent);
		if( ind==-1 ) return getFirstComponent(focusCycleRoot);

		int last = (ind==0) ?sequence.size()-1 :ind-1;
		return sequence.get(last);
	}

	public Component getDefaultComponent(Container focusCycleRoot) {
		return getFirstComponent(focusCycleRoot);
	}

	public Component getLastComponent(Container focusCycleRoot) {
		return sequence.getLast();
	}

	public Component getFirstComponent(Container focusCycleRoot) {
		return sequence.getFirst();
	}
}