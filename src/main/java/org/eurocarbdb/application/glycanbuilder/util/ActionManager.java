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
/**
 @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

package org.eurocarbdb.application.glycanbuilder.util;

import java.util.*;

import javax.swing.*;

import org.eurocarbdb.application.glycanbuilder.EurocarbResizableIcon;
import org.eurocarbdb.application.glycanbuilder.GlycanAction;
import org.eurocarbdb.application.glycanbuilder.ICON_SIZE;
import org.eurocarbdb.application.glycanbuilder.ThemeManager;

import test.common.IconWrapperResizableIcon;

import java.awt.event.*;

/**
 * Objects of this class are used by interface components to manage sets of
 * action objects.
 * 
 * @see GlycanAction
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class ActionManager {

	protected HashMap<String, GlycanAction> theActions;

	/**
	 * Default constructor.
	 */
	public ActionManager() {
		theActions = new HashMap<String, GlycanAction>();
	}

	/**
	 * Return all the actions.
	 */
	public Collection<GlycanAction> actions() {
		return theActions.values();
	}

	/**
	 * Return the action with the given identifier.
	 */
	public GlycanAction get(String action) {
		return theActions.get(action);
	}

	/**
	 * Add a new action to the list.
	 * 
	 * @param action
	 *            action identifier
	 * @param i
	 *            icon associated with the action
	 * @param label
	 *            label used to display the action in menus
	 * @param mnemonic
	 *            character of the label used to shortcut the action in a menu
	 * @param accelerator
	 *            keyboard combination used to launch the action
	 * @param l
	 *            the listener that will be notified when the action is
	 *            performed
	 */
	public GlycanAction add(String action, EurocarbResizableIcon i, String label, int mnemonic,
			String accelerator, ActionListener l) {
		if (i == null) {
			i = ThemeManager.getResizableEmptyIcon(ICON_SIZE.L1);
		}
		GlycanAction a = theActions.get(action);
		if (a == null){
			//System.err.println("INIT");	
			a = new GlycanAction(action, i, label, mnemonic, accelerator, l);
		}else{
			
			a.init(action, i, label, mnemonic, accelerator, l);
		}
		theActions.put(action, a);

		return a;
	}

	/**
	 * Add a new action to the list.
	 * 
	 * @param parent
	 *            parent action, used to chain multiple actions
	 * @param action
	 *            action identifier
	 * @param mnemonic
	 *            character of the label used to shortcut the action in a menu
	 * @param accelerator
	 *            keyboard combination used to launch the action
	 * @param l
	 *            the listener that will be notified when the action is
	 *            performed
	 */
	public GlycanAction add(GlycanAction parent, String action, int mnemonic,
			String accelerator, ActionListener l) {

		GlycanAction toadd = new GlycanAction(parent, action, mnemonic,
				accelerator, l);
		theActions.put(toadd.getActionCommand(), toadd);
		return toadd;
	}

	/**
	 * Update the information about a given action.
	 * 
	 * @param action
	 *            action identifier
	 * @param i
	 *            icon associated with the action
	 * @param label
	 *            label used to display the action in menus
	 * @param mnemonic
	 *            character of the label used to shortcut the action in a menu
	 * @param accelerator
	 *            keyboard combination used to launch the action
	 */
	public void update(String action, EurocarbResizableIcon i, String label, int mnemonic,
			String accelerator) {
		GlycanAction a = theActions.get(action);
		if (a != null)
			a.init(action, i, label, mnemonic, accelerator, null);
	}

	public void add(String action, ImageIcon icon, String label, int mnemonic,
			String accelerator, ActionListener l) {
		// TODO Auto-generated method stub
		EurocarbResizableIcon iconR;
		if (icon == null) {
			iconR = ThemeManager.getResizableEmptyIcon(ICON_SIZE.L1);
		}else{
			iconR = new EurocarbResizableIcon();
			iconR.iconProperties=null;
			iconR.resizableIcon=new IconWrapperResizableIcon(icon);
		}
		
		GlycanAction a = theActions.get(action);
		if (a == null)
			a = new GlycanAction(action, iconR, label, mnemonic, accelerator, l);
		else
			a.init(action, iconR, label, mnemonic, accelerator, l);
		theActions.put(action, a);
		
	}

	public void add(String action, Icon icon, String label,
			int mnemonic, String accelerator, ActionListener l) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		EurocarbResizableIcon iconR;
		if (icon == null) {
			iconR = ThemeManager.getResizableEmptyIcon(ICON_SIZE.L1);
		}else{
			iconR = new EurocarbResizableIcon();
			iconR.iconProperties=null;
			iconR.resizableIcon=new IconWrapperResizableIcon(icon);
		}
		
		GlycanAction a = theActions.get(action);
		if (a == null)
			a = new GlycanAction(action, iconR, label, mnemonic, accelerator, l);
		else
			a.init(action, iconR, label, mnemonic, accelerator, l);
		theActions.put(action, a);
		
	}

}