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

import javax.swing.*;
import javax.swing.border.*;

import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRenderer;
import org.eurocarbdb.application.glycanbuilder.util.EscapeDialog;

import java.awt.*;
import java.awt.event.*;

/**
   A dialog that allows the user to select a range of residues in a
   glycan structure

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class ResidueSelectorDialog extends EscapeDialog implements ResidueSelector.SelectionChangeListener, ActionListener {

    // components
    private ResidueSelector theSelector;
    private JLabel theMessage;
    private JButton ok_button;
    private JButton cancel_button;

    /**
       Creates a new dialog
       @param parent the parent frame
       @param title the title of the dialog
       @param message a text message to be shown to the user
       @param structure the glycan structure from where to select the
       residues
       @param actives used to restrict the residues that can be
       selected. If <code>null</code> all residues can be selected
       @param multiple_sel <code>true</code> if multiple residues can be selected
       @param gr the glycan renderer that will be used to render the structure       
     */
    public ResidueSelectorDialog(JFrame parent, String title, String message, Glycan structure, Collection<Residue> actives, boolean multiple_sel, GlycanRenderer gr) {
    	super(parent,title,true);
    	
    	// add components
    	this.getContentPane().setLayout(new BorderLayout());

    	theMessage = new JLabel(message);
    	theMessage.setBorder(new EmptyBorder(10,10,10,10));
    	this.getContentPane().add(theMessage,BorderLayout.NORTH);

    	theSelector = new ResidueSelector(structure,actives,multiple_sel);
    	theSelector.setGlycanRenderer(gr);
    	theSelector.setBorder(new BevelBorder(BevelBorder.LOWERED));
    	this.getContentPane().add(theSelector,BorderLayout.CENTER);

    	JPanel buttons_panel = new JPanel(new FlowLayout());

    	ok_button = new JButton(new GlycanAction("OK",ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3),"OK",-1,"",this));
    	cancel_button = new JButton(new GlycanAction("Cancel",ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3),"Cancel",-1,"",this));
    	buttons_panel.add(ok_button);
    	buttons_panel.add(cancel_button);
    	this.getContentPane().add(buttons_panel,BorderLayout.SOUTH);

    	getRootPane().setDefaultButton(ok_button);

    	// set dialog

    	updateActions();
    	theSelector.addSelectionChangeListener(this);

    	pack();
    	setResizable(false);    
    	setLocationRelativeTo(parent);
    }

    /**
       Return <code>true</code> if the cancel button was pressed
     */
    public boolean isCanceled() {
    return return_status.equals("Cancel");
    }

    /**
       Return the list of residues selected by the user
     */
    public Collection<Residue> getSelectedResidues() {
    return theSelector.getSelectedResiduesList();
    }

    /**
       Return the last residue selected by the user
     */
    public Residue getCurrentResidue() {
    return theSelector.getCurrentResidue();
    }


    private void updateActions() {
    ok_button.getAction().setEnabled(theSelector.hasSelection());
    }

    public void selectionChanged(ResidueSelector.SelectionChangeEvent e) {
    updateActions();
    }

    public void actionPerformed(ActionEvent e) {

    String action = GlycanAction.getAction(e);

    if( action.equals("OK") ) {
        return_status = action;
        setVisible(false);
    }
    else if( action.equals("Cancel") ) {
        return_status = action;
        setVisible(false);
    }

    }
}