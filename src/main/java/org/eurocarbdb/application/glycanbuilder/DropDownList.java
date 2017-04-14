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

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Component implementing a drop down list with multiple selections to be added
 * in a toolbar.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class DropDownList extends JToggleButton implements ActionListener,
		ListSelectionListener, PopupMenuListener {

	private class MyPopupMenu extends JPopupMenu {

		public void setVisible(boolean f) {
			// ignore closing when pressing on the button
			if (f || this_object.getMousePosition(true) == null)
				super.setVisible(f);
		}

		public void makeInvisible() {
			super.setVisible(false);
		}

	}

	private static final long serialVersionUID = 0L;

	private JList theList;
	private MyPopupMenu thePopup;
	private boolean is_changed;
	private boolean ignore_list_events;

	public JComponent this_object;

	/**
	 * Create a new instance and fill the list with a collection of items.
	 * 
	 * @throws IOException
	 */
	public DropDownList(Object[] data) {
		super("---", FileUtils.themeManager.getImageIcon("smalldownarrow",
				10));
		this.setHorizontalTextPosition(SwingConstants.LEADING);
		this_object = this;

		theList = new JList(createListModel(data));
		theList
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		this.addActionListener(this);
		theList.addListSelectionListener(this);

		updateText();

		is_changed = false;
		ignore_list_events = false;
	}

	private DefaultListModel createListModel(Object[] data) {
		DefaultListModel ret = new DefaultListModel();
		for (Object o : data)
			ret.addElement(o);
		return ret;
	}

	/**
	 * Set the content of the list.
	 */
	public void setListModel(Object[] data) {
		theList.setModel(createListModel(data));
	}

	/**
	 * Set the content of the list from a ListModel.
	 */
	public void setListModel(ListModel model) {
		theList.setModel(model);
	}

	private MyPopupMenu createPopup() {
		MyPopupMenu popup = new MyPopupMenu();
		popup.setLayout(new BorderLayout());
		popup.add(theList);
		popup.addPopupMenuListener(this);

		return popup;
	}

	/**
	 * Deselect all items in the list.
	 */
	public void clearSelection() {
		ignore_list_events = true;
		theList.clearSelection();
		updateText();
		ignore_list_events = false;
	}

	/**
	 * Return the collection of selected items.
	 */
	public Object[] getSelectedValues() {
		return theList.getSelectedValues();
	}

	/**
	 * Set the selected items.
	 */
	public void setSelectedValues(Object[] values) {
		ignore_list_events = true;

		theList.clearSelection();
		DefaultListModel dlm = (DefaultListModel) theList.getModel();
		for (int i = 0; i < values.length; i++) {
			int ind = dlm.indexOf(values[i]);
			theList.addSelectionInterval(ind, ind);
		}
		updateText();
		ignore_list_events = false;
	}

	/**
	 * Send an action event to all the listeners.
	 */
	public void fireActionEvent() {
		for (ActionListener al : this.getActionListeners()) {
			if (al != this) {
				if (getActionCommand() != null)
					al.actionPerformed(new ActionEvent(this,
							ActionEvent.ACTION_PERFORMED, getActionCommand()));
				else
					al.actionPerformed(new ActionEvent(this,
							ActionEvent.ACTION_PERFORMED, getText()));
			}
		}
		is_changed = false;
	}

	public void actionPerformed(ActionEvent e) {
		if (this.isSelected()) {
			thePopup = createPopup();

			Rectangle bounds = this.getBounds(null);
			Dimension psize = thePopup.getPreferredSize();
			thePopup.show(this, 0, bounds.height);
			thePopup.setPopupSize(Math.max(bounds.width, psize.width),
					psize.height);
		} else
			thePopup.makeInvisible();
	}

	public void valueChanged(ListSelectionEvent e) {
		if (!ignore_list_events) {
			is_changed = true;
			updateText();
			//thePopup.makeInvisible();
		}
	}

	private void updateText() {
		StringBuilder sb = new StringBuilder();
		for (Object o : theList.getSelectedValues()) {
			if (sb.length() > 0)
				sb.append("/");
			sb.append(o.toString());
		}
		if (sb.length() > 0)
			setText(sb.toString());
		else
			setText("---");

	}

	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		is_changed = false;
	}

	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		if (is_changed)
			fireActionEvent();
		this.setSelected(false);
	}

	public void popupMenuCanceled(PopupMenuEvent e) {
	}

}