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

import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;

import javax.swing.*;

/**
   Dialog used to report an error to the user

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class ReportDialog extends EscapeDialog implements ActionListener {

    protected String message = "";

    protected JTextArea text_area;
    protected JButton   copy_button;
    protected JButton   close_button;
    protected JButton   exit_button;

    /**
       Create a new dialog
       @param owner the parent frame
       @param _message the error message
     */
    public ReportDialog(Frame owner, String _message) {
    super(owner,true);
    
    setLayout(new BorderLayout());
    add(new JScrollPane(text_area = new JTextArea()), BorderLayout.CENTER);

    JPanel south_panel = new JPanel();
    south_panel.add(copy_button = new JButton());
    south_panel.add(close_button = new JButton());
    south_panel.add(exit_button = new JButton());
    add(south_panel, BorderLayout.SOUTH) ;

    copy_button.setText("Copy text");
    copy_button.setActionCommand("copy");
    copy_button.addActionListener(this);

    close_button.setText("Close") ;
    close_button.setActionCommand("close");
    close_button.addActionListener(this);

    exit_button.setText("Exit") ;
    exit_button.setActionCommand("exit");
    exit_button.addActionListener(this);

    text_area.setEditable(false);
    text_area.setText(message = _message);
    text_area.setCaretPosition(0);

    setSize(400,300);
    setResizable(true);
    setLocationRelativeTo(owner);

    getRootPane().setDefaultButton(close_button);  
    }

    public void actionPerformed(ActionEvent e) {

    String action = e.getActionCommand();
    if( action.equals("copy") ) 
        ClipUtils.setContents(new StringSelection(message));
    else if( action.equals("close") ) 
        this.setVisible(false);    
    else if( action.equals("exit") ) {
        int retValue = JOptionPane.showOptionDialog(this, "All unsaved data will be lost. Continue anyway?",
                            "Warning", JOptionPane.YES_NO_OPTION, 
                            JOptionPane.WARNING_MESSAGE, null, null, null);  
        if( retValue==JOptionPane.YES_OPTION ) {
        System.gc();
        System.runFinalization();
        System.exit(0);
        }
    }
    }
}