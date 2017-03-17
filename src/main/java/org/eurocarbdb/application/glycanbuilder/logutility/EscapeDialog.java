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

package org.eurocarbdb.application.glycanbuilder.logutility;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
   Specialization of a JDialog that handle the closing of the dialog
   when the Escape button is pressed.
   
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public abstract class EscapeDialog extends JDialog {
    
    protected static final long serialVersionUID = 0L;    

    protected String return_status = "Cancel";

    /**
       Create a new dialog
       @param owner the parent frame
       @param modal if <code>true</code> the new dialog will be modal
     */    
    public EscapeDialog(Frame owner, boolean modal) { 
    super(owner, modal);
    } 

    /**
       Create a new dialog
       @param owner the parent frame
       @param title the title of the dialog
       @param modal if <code>true</code> the new dialog will be modal
     */
    public EscapeDialog(Frame owner, String title, boolean modal) { 
    super(owner, title, modal);
    } 

    protected JRootPane createRootPane() { 
    JRootPane rootPane = new JRootPane();
    KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
    Action actionListener = new AbstractAction() { 
        public void actionPerformed(ActionEvent actionEvent) { 
            setVisible(false);
            return_status = "Cancel";
        } 
        } ;
    InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    inputMap.put(stroke, "ESCAPE");
    rootPane.getActionMap().put("ESCAPE", actionListener);
    
    return rootPane;
    } 
    

    /**
       Return the closing status of the dialog (equals to
       <quote>cancel</quote> if the Escape button is pressed).
     */
    public String getReturnStatus() {
    return return_status;
    }

}