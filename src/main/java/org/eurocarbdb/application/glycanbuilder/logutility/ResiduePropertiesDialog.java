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

import javax.swing.*;

import org.eurocarbdb.application.glycanbuilder.CustomFocusTraversalPolicy;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;

import java.util.*;

/**
   Dialog to change the properties of a residue

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class ResiduePropertiesDialog extends EscapeDialog implements java.awt.event.ActionListener, java.awt.event.ItemListener  {
    
    // data members
    
    private Linkage parent_link;
    private Residue current;
    private Vector<Residue> linked;
    private GlycanDocument theDoc;

    /** 
    Creates a new dialog
    @param parent the parent frame
    @param _current the residue whose properties need to be change
    @param _theDoc the document containing the residue. The
    document will be notified of a change in the residue
    properties
    */
    public ResiduePropertiesDialog(java.awt.Frame parent, Residue _current, GlycanDocument _theDoc) {
        super(parent, true);
    
    // set data
    parent_link = _current.getParentLinkage();
    current = _current;
    linked  = null;
    theDoc  = _theDoc;

    // init values
        initComponents();
    fillComponents();
    setSelections();
    setTraversal();
    setActions();
    enableItems();

    // set location
    setLocationRelativeTo(parent);
    }

    /** 
    Creates a new dialog
    @param parent the parent frame
    @param _current the residue whose properties need to be change
    @param _linked the list of residues that are displayed at the
    same position of the current residue. Used for multiple
    identical antennae with uncertain linkage position
    @param _theDoc the document containing the residue. The
    document will be notified of a change in the residue
    properties
    */
    public ResiduePropertiesDialog(java.awt.Frame parent, Residue _current, Vector<Residue> _linked, GlycanDocument _theDoc) {
    this(parent,_current,_theDoc);
     
    linked = _linked;
    }

    private void fillComponents() {
    field_linkage_position.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);    
    field_second_parent_position.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    
    // first link
    if( parent_link!=null )
        field_linkage_position.setModel(createPositions(parent_link.getParentResidue()));
    field_anomeric_state.setModel(new DefaultComboBoxModel(new String[] { "?", "a", "b" }));
        field_anomeric_carbon.setModel(new DefaultComboBoxModel(new String[] { "?", "1", "2", "3" }));
        field_chirality.setModel(new DefaultComboBoxModel(new String[] { "?", "D", "L" }));
        field_ring_size.setModel(new DefaultComboBoxModel(new String[] { "?", "p", "f", "o" }));

    if( parent_link!=null )
        field_second_parent_position.setModel(createPositions(parent_link.getParentResidue()));
        field_second_child_position.setModel(new DefaultComboBoxModel(new String[] { "?", "1", "2", "3" }));
    }

    private ListModel createPositions(Residue parent) {
    DefaultListModel ret = new DefaultListModel();

    // collect available positions
    char[] par_pos = null;
    if( parent==null || parent.getType().getLinkagePositions().length==0 )
        par_pos = new char[] { '1', '2', '3', '4', '5', '6', '7', '8', '9', 'N' };
    else 
        par_pos = parent.getType().getLinkagePositions();

    // add elements
    ret.addElement("?");    
    for( int i=0; i<par_pos.length; i++ ) 
        ret.addElement("" + par_pos[i]);
    
    return ret;
    }

    private void setSelections() {
    if( parent_link!=null )        
        selectPositions(field_linkage_position,parent_link.glycosidicBond().getParentPositions());        

    field_anomeric_state.setSelectedItem(""+current.getAnomericState());
    field_anomeric_carbon.setSelectedItem(""+current.getAnomericCarbon());
    field_chirality.setSelectedItem(""+current.getChirality());
    field_ring_size.setSelectedItem(""+current.getRingSize());

    if( parent_link!=null ) {
        field_second_bond.setSelected(parent_link.hasMultipleBonds());
        selectPositions(field_second_parent_position,parent_link.getBonds().get(0).getParentPositions());
        field_second_child_position.setSelectedItem(""+parent_link.getBonds().get(0).getChildPosition());
    }
    }

    private void selectPositions(JList field, char[] pos) {
    DefaultListModel dlm = (DefaultListModel)field.getModel();
    for( int i=0; i<pos.length; i++ ) {
        int ind = dlm.indexOf("" + pos[i]);
        field.addSelectionInterval(ind,ind);
    }
    }


    private void setTraversal() {
    CustomFocusTraversalPolicy tp = new CustomFocusTraversalPolicy();
    
    boolean can_have_parent_linkage = (parent_link!=null && parent_link.getParentResidue()!=null && (parent_link.getParentResidue().isSaccharide() || parent_link.getParentResidue().isBracket() || parent_link.getParentResidue().isRepetition() || parent_link.getParentResidue().isRingFragment()));

    if( can_have_parent_linkage )
        tp.addComponent(field_linkage_position);
    if( current.isSaccharide() ) {        
        tp.addComponent(field_anomeric_state);
        tp.addComponent(field_anomeric_carbon);
        tp.addComponent(field_chirality);
        tp.addComponent(field_ring_size);
    }

    if( can_have_parent_linkage ) {
        tp.addComponent(field_second_parent_position);
        tp.addComponent(field_second_child_position);
    }
    
    tp.addComponent(button_ok);
    tp.addComponent(button_cancel);
    this.setFocusTraversalPolicy(tp);

    getRootPane().setDefaultButton(button_ok);  
    }

    private void setActions() {
    field_second_bond.addItemListener(this);    
    field_ring_size.addItemListener(this);
    button_ok.addActionListener(this);
    button_cancel.addActionListener(this);
    }

    private void enableItems() {
    boolean can_have_parent_linkage = (parent_link!=null && parent_link.getParentResidue()!=null && (parent_link.getParentResidue().isSaccharide() || parent_link.getParentResidue().isBracket() || parent_link.getParentResidue().isRepetition() || parent_link.getParentResidue().isRingFragment()));
    
    field_linkage_position.setEnabled(can_have_parent_linkage);

    field_anomeric_state.setEnabled(current.isSaccharide());    
    field_anomeric_carbon.setEnabled(current.isSaccharide());
    field_chirality.setEnabled(current.isSaccharide());
    field_ring_size.setEnabled(current.isSaccharide());

    field_second_bond.setEnabled(can_have_parent_linkage);
    field_second_parent_position.setEnabled(can_have_parent_linkage && field_second_bond.isSelected());
    field_second_child_position.setEnabled(can_have_parent_linkage && field_second_bond.isSelected());
    }    

    private void setProperties(Residue r) {

    char sel_anomeric_state = getSelectedValueChar(field_anomeric_state);
    r.setAnomericState(sel_anomeric_state);
        
    char sel_anomeric_carbon = getSelectedValueChar(field_anomeric_carbon);
    r.setAnomericCarbon(sel_anomeric_carbon);
    
    char sel_chirality = getSelectedValueChar(field_chirality);
    r.setChirality(sel_chirality);
    
    char sel_ring_size = getSelectedValueChar(field_ring_size);
    r.setRingSize(sel_ring_size);
    
    Linkage plr = r.getParentLinkage();
    if( plr!=null ) {
        char[] sel_linkage_positions = getSelectedPositions(field_linkage_position);
        if( field_second_bond.isSelected() ) {
        char[] sel_second_parent_positions = getSelectedPositions(field_second_parent_position);
        char   sel_second_child_position = getSelectedValueChar(field_second_child_position);
        
        plr.setLinkagePositions(sel_linkage_positions,sel_second_parent_positions,sel_second_child_position);
        }
        else
        plr.setLinkagePositions(sel_linkage_positions);        
    }
    }
    
    private void retrieveData() {
    setProperties(current); 
    if( linked!=null ) {
        for( Residue r : linked ) 
        setProperties(r);
    }
    
    theDoc.fireDocumentChanged();        
    }

    private char getSelectedValueChar(JComboBox field) {
    return ((String)field.getSelectedItem()).charAt(0);
    }

    private char[] getSelectedPositions(JList field) {
    Object[] sel = field.getSelectedValues();
    if( sel.length==0 )
        return new char[] { '?' };
    
    char[] ret = new char[sel.length];
    for( int i=0; i<sel.length; i++ ) 
        ret[i] = ((String)sel[i]).charAt(0);
    return ret;
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
    String action = e.getActionCommand();
    
    if (action == "OK") {
        retrieveData();
        closeDialog();
    }
    else if (action == "Cancel"){
        closeDialog();
    }
    }        

    public void itemStateChanged(java.awt.event.ItemEvent e) {
    if( e.getSource()==field_ring_size ) {
        char ring_size = getSelectedValueChar(field_ring_size);
        if( ring_size =='o' ) {
        field_anomeric_state.setSelectedItem("?");    
        field_anomeric_carbon.setSelectedItem("?");    
        }
        else {
        field_anomeric_state.setSelectedItem(""+current.getAnomericState());
        field_anomeric_carbon.setSelectedItem(""+current.getAnomericCarbon());
        }
    }
        
    enableItems();
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
    jLabel1 = new JLabel();
        jLabel2 = new JLabel();
        jLabel3 = new JLabel();
        jLabel4 = new JLabel();
        field_anomeric_state = new JComboBox();
        field_chirality = new JComboBox();
        field_anomeric_carbon = new JComboBox();
        button_ok = new JButton();
        button_cancel = new JButton();
        field_ring_size = new JComboBox();
        jLabel5 = new JLabel();
        jSeparator1 = new JSeparator();
        jScrollPane1 = new JScrollPane();
        jScrollPane2 = new JScrollPane();
        field_linkage_position = new JList();
        field_second_bond = new JCheckBox();
        jLabel6 = new JLabel();
        field_second_parent_position = new JList();
        field_second_child_position = new JComboBox();
        jLabel7 = new JLabel();

        setResizable(false);
        setTitle("Residue properties");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog();
            }
        });

        jLabel1.setText("Anomeric state");

        jLabel2.setText("Chirality");

        jLabel3.setText("Anomeric carbon");

        jLabel4.setText("Parent position");

        field_anomeric_state.setModel(new DefaultComboBoxModel(new String[] { "?", "a", "b" }));

        field_chirality.setModel(new DefaultComboBoxModel(new String[] { "?", "D", "L" }));

        field_anomeric_carbon.setModel(new DefaultComboBoxModel(new String[] { "?", "1", "2", "3" }));

        button_ok.setText("OK");

        button_cancel.setText("Cancel");

        field_ring_size.setModel(new DefaultComboBoxModel(new String[] { "?", "p", "f", "o" }));

        jLabel5.setText("Ring size");

        field_linkage_position.setModel(new AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        field_linkage_position.setVisibleRowCount(3);
        jScrollPane1.setViewportView(field_linkage_position);

        field_second_bond.setText("Second bond");
        field_second_bond.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        field_second_bond.setMargin(new java.awt.Insets(0, 0, 0, 0));

        jLabel6.setText("Linkage position");

        field_second_parent_position.setModel(new AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        field_second_parent_position.setVisibleRowCount(3);
        jScrollPane2.setViewportView(field_second_parent_position);

        field_second_child_position.setModel(new DefaultComboBoxModel(new String[] { "?", "1", "2", "3" }));

        jLabel7.setText("Child position");
        jLabel7.getAccessibleContext().setAccessibleName("Child position");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jLabel6)
                        .add(15, 15, 15)
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel3)
                                    .add(jLabel2))
                                .add(15, 15, 15)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(field_chirality, 0, 66, Short.MAX_VALUE)
                                    .add(field_anomeric_carbon, 0, 66, Short.MAX_VALUE)))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                .add(jLabel5)
                                .add(63, 63, 63)
                                .add(field_ring_size, 0, 66, Short.MAX_VALUE))
                            .add(layout.createSequentialGroup()
                                .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 108, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(field_anomeric_state, 0, 66, Short.MAX_VALUE))))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(field_second_bond))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jLabel4)
                        .add(25, 25, 25)
                        .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jLabel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 23, Short.MAX_VALUE)
                        .add(field_second_child_position, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(12, 12, 12)
                                .add(button_ok)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(button_cancel))
                            .add(jSeparator1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE))))
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {button_cancel, button_ok}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 53, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel6))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(field_anomeric_state, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(field_anomeric_carbon, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(field_chirality, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel5)
                    .add(field_ring_size, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(6, 6, 6)
                .add(field_second_bond)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 51, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(field_second_child_position, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel7))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(button_ok)
                    .add(button_cancel))
                .add(27, 27, 27))
        );
        pack();       
    }// </editor-fold>//GEN-END:initComponents
    
    /** Closes the dialog */
    private void closeDialog() {//GEN-FIRST:event_closeDialog
        setVisible(false);
        dispose();
    }//GEN-LAST:event_closeDialog
        
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton button_cancel;
    private JButton button_ok;
    private JComboBox field_anomeric_carbon;
    private JComboBox field_anomeric_state;
    private JComboBox field_chirality;
    private JList field_linkage_position;
    private JComboBox field_ring_size;
    private JCheckBox field_second_bond;
    private JComboBox field_second_child_position;
    private JList field_second_parent_position;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JLabel jLabel4;
    private JLabel jLabel5;
    private JLabel jLabel6;
    private JLabel jLabel7;
    private JScrollPane jScrollPane1;
    private JScrollPane jScrollPane2;
    private JSeparator jSeparator1;
    // End of variables declaration//GEN-END:variables
    
}
