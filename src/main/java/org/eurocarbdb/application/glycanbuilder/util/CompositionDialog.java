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


import javax.swing.*;
import javax.swing.event.*;

import org.eurocarbdb.application.glycanbuilder.CustomFocusTraversalPolicy;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.CompositionOptions;

import java.awt.Frame;
import java.awt.event.*;

/**
 * Dialog that collects the composition options.
 * @author  aceroni
 */

public class CompositionDialog extends EscapeDialog implements ActionListener, ChangeListener {

	private CompositionOptions theOptions;

	public CompositionDialog(Frame parent, CompositionOptions opt) {
		super(parent, true);

		theOptions =  opt;

		initComponents();
		setTraversal();
		setData();    
		setActions();
		enableItems();
		this.resetData();		
		
		// set location
		setLocationRelativeTo(parent);
		pack();
	}

	private void setTraversal() {
		CustomFocusTraversalPolicy tp = new CustomFocusTraversalPolicy();

		tp.addComponent(field_pen);
		tp.addComponent(field_hex);
		tp.addComponent(field_hep);
		tp.addComponent(field_hexn);
		tp.addComponent(field_hexnac);
		tp.addComponent(field_dpen);
		tp.addComponent(field_dhex);
		tp.addComponent(field_ddhex);
		tp.addComponent(field_mehex);

		/*tp.addComponent(field_or1);
    tp.addComponent(field_or2);
    tp.addComponent(field_or3);
    tp.addComponent(field_or1_name);
    tp.addComponent(field_or1_mass);    
    tp.addComponent(field_or2_name);
    tp.addComponent(field_or2_mass);
    tp.addComponent(field_or3_name);
    tp.addComponent(field_or3_mass);
		 */

		tp.addComponent(field_hexa);
		tp.addComponent(field_dhexa);
		tp.addComponent(field_neu5gc);
		tp.addComponent(field_neu5ac);
		tp.addComponent(field_neu5gclac);
		tp.addComponent(field_neu5aclac);
		tp.addComponent(field_kdo);
		tp.addComponent(field_kdn);
		tp.addComponent(field_mur);

		/*tp.addComponent(field_s);
    tp.addComponent(field_p);
    tp.addComponent(field_ac);
    tp.addComponent(field_pyr);
    tp.addComponent(field_pc);
		 */
		tp.addComponent(button_ok);
		tp.addComponent(button_cancel);

		this.setFocusTraversalPolicy(tp);

		getRootPane().setDefaultButton(button_ok);
	}

	private void setData() {               
		field_pen.setModel(new SpinnerNumberModel(theOptions.PEN,0,99,1));
		field_hex.setModel(new SpinnerNumberModel(theOptions.HEX,0,99,1));
		field_hep.setModel(new SpinnerNumberModel(theOptions.HEP,0,99,1));
		field_hexn.setModel(new SpinnerNumberModel(theOptions.HEXN,0,99,1));
		field_hexnac.setModel(new SpinnerNumberModel(theOptions.HEXNAC,0,99,1));
		field_dpen.setModel(new SpinnerNumberModel(theOptions.DPEN,0,99,1));
		field_dhex.setModel(new SpinnerNumberModel(theOptions.DHEX,0,99,1));
		field_ddhex.setModel(new SpinnerNumberModel(theOptions.DDHEX,0,99,1));
		field_mehex.setModel(new SpinnerNumberModel(theOptions.MEHEX,0,99,1));

		/*field_or1.setModel(new SpinnerNumberModel(theOptions.OR1,0,99,1));
    field_or2.setModel(new SpinnerNumberModel(theOptions.OR2,0,99,1));
    field_or3.setModel(new SpinnerNumberModel(theOptions.OR3,0,99,1));
    field_or1_name.setText("" + theOptions.OR1_NAME);
    field_or2_name.setText("" + theOptions.OR2_NAME);
    field_or3_name.setText("" + theOptions.OR3_NAME);
    field_or1_mass.setText("" + theOptions.OR1_MASS);
    field_or2_mass.setText("" + theOptions.OR2_MASS);
    field_or3_mass.setText("" + theOptions.OR3_MASS);
		 */
		field_hexa.setModel(new SpinnerNumberModel(theOptions.HEXA,0,99,1));
		field_dhexa.setModel(new SpinnerNumberModel(theOptions.DHEXA,0,99,1));
		field_neu5gc.setModel(new SpinnerNumberModel(theOptions.NEU5GC,0,99,1));
		field_neu5ac.setModel(new SpinnerNumberModel(theOptions.NEU5AC,0,99,1));
		field_neu5gclac.setModel(new SpinnerNumberModel(theOptions.NEU5GCLAC,0,99,1));
		field_neu5aclac.setModel(new SpinnerNumberModel(theOptions.NEU5ACLAC,0,99,1));
		field_kdo.setModel(new SpinnerNumberModel(theOptions.KDO,0,99,1));
		field_kdn.setModel(new SpinnerNumberModel(theOptions.KDN,0,99,1));
		field_mur.setModel(new SpinnerNumberModel(theOptions.MUR,0,99,1));

		/*field_s.setModel(new SpinnerNumberModel(theOptions.S,0,99,1));
    field_p.setModel(new SpinnerNumberModel(theOptions.P,0,99,1));
    field_ac.setModel(new SpinnerNumberModel(theOptions.AC,0,99,1));
    field_pyr.setModel(new SpinnerNumberModel(theOptions.PYR,0,99,1));
    field_pc.setModel(new SpinnerNumberModel(theOptions.PC,0,99,1));
		 */
	}

	private void setActions() {
		button_ok.addActionListener(this);
		button_cancel.addActionListener(this);
		//field_or1.addChangeListener(this);
		//field_or2.addChangeListener(this);
		//field_or3.addChangeListener(this);
	}

	private void enableItems() {
		/*field_or1_name.setEnabled(((Integer)field_or1.getValue())>0);
    field_or2_name.setEnabled(((Integer)field_or2.getValue())>0);
    field_or3_name.setEnabled(((Integer)field_or3.getValue())>0);
    field_or1_mass.setEnabled(((Integer)field_or1.getValue())>0);
    field_or2_mass.setEnabled(((Integer)field_or2.getValue())>0);
    field_or3_mass.setEnabled(((Integer)field_or3.getValue())>0);
		 */}


	private boolean retrieveData() {    

		/*if( ResidueDictionary.findResidueType(field_or1_name.getText())!=null ) {
        JOptionPane.showMessageDialog(this,"Invalid residue name", "The identifier " + field_or1_name.getText() + " is already in use.", JOptionPane.ERROR_MESSAGE);
        return false;
    }
    else if( ResidueDictionary.findResidueType(field_or2_name.getText())!=null ) {
        JOptionPane.showMessageDialog(this,"Invalid residue name", "The identifier " + field_or2_name.getText() + " is already in use.", JOptionPane.ERROR_MESSAGE);
        return false;
    }
    else if( ResidueDictionary.findResidueType(field_or3_name.getText())!=null ) {        
        JOptionPane.showMessageDialog(this,"Invalid residue name", "The identifier " + field_or3_name.getText() + " is already in use.", JOptionPane.ERROR_MESSAGE);
        return false;
    }  */      


		theOptions.PEN = (Integer)field_pen.getValue();
		theOptions.HEX = (Integer)field_hex.getValue();
		theOptions.HEP = (Integer)field_hep.getValue();
		theOptions.HEXN = (Integer)field_hexn.getValue();
		theOptions.HEXNAC = (Integer)field_hexnac.getValue();
		theOptions.DPEN = (Integer)field_dpen.getValue();
		theOptions.DHEX = (Integer)field_dhex.getValue();
		theOptions.DDHEX = (Integer)field_ddhex.getValue();
		theOptions.MEHEX = (Integer)field_mehex.getValue();

		/* theOptions.OR1 = (Integer)field_or1.getValue();
    theOptions.OR2 = (Integer)field_or2.getValue();
    theOptions.OR3 = (Integer)field_or3.getValue();
    theOptions.OR1_NAME = (field_or1_name.getText()==null || field_or1_name.getText().length()==0) ?"Or1" :field_or1_name.getText(); 
    theOptions.OR2_NAME = (field_or2_name.getText()==null || field_or2_name.getText().length()==0) ?"Or2" :field_or2_name.getText(); 
    theOptions.OR3_NAME = (field_or3_name.getText()==null || field_or3_name.getText().length()==0) ?"Or3" :field_or3_name.getText(); 
    theOptions.OR1_MASS = Double.valueOf(field_or1_mass.getText());
    theOptions.OR2_MASS = Double.valueOf(field_or2_mass.getText());
    theOptions.OR3_MASS = Double.valueOf(field_or3_mass.getText());
		 */
		theOptions.HEXA = (Integer)field_hexa.getValue();
		theOptions.DHEXA = (Integer)field_dhexa.getValue();
		theOptions.NEU5GC = (Integer)field_neu5gc.getValue();
		theOptions.NEU5AC = (Integer)field_neu5ac.getValue();
		theOptions.NEU5GCLAC = (Integer)field_neu5gclac.getValue();
		theOptions.NEU5ACLAC = (Integer)field_neu5aclac.getValue();
		theOptions.KDO = (Integer)field_kdo.getValue();
		theOptions.KDN = (Integer)field_kdn.getValue();
		theOptions.MUR = (Integer)field_mur.getValue();

		/* theOptions.S = (Integer)field_s.getValue();
    theOptions.P = (Integer)field_p.getValue();
    theOptions.AC = (Integer)field_ac.getValue();
    theOptions.PYR = (Integer)field_pyr.getValue();
    theOptions.PC = (Integer)field_pc.getValue();
		 */
		return true;
	}

	public void stateChanged(ChangeEvent e) {
		enableItems();
	}


	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();

		if (action == "OK") {
			if( retrieveData() ) {
				return_status = action;
				closeDialog();
			}
		}
		else if (action == "Cancel"){
			return_status = action;
			closeDialog();
		}
		else
			enableItems();
	}  

	private void resetData() {
		field_pen.setModel(new SpinnerNumberModel(0,0,99,1));
		field_hex.setModel(new SpinnerNumberModel(0,0,99,1));
		field_hep.setModel(new SpinnerNumberModel(0,0,99,1));
		field_hexn.setModel(new SpinnerNumberModel(0,0,99,1));
		field_hexnac.setModel(new SpinnerNumberModel(0,0,99,1));
		field_dpen.setModel(new SpinnerNumberModel(0,0,99,1));
		field_dhex.setModel(new SpinnerNumberModel(0,0,99,1));
		field_ddhex.setModel(new SpinnerNumberModel(0,0,99,1));
		field_mehex.setModel(new SpinnerNumberModel(0,0,99,1));
		field_hexa.setModel(new SpinnerNumberModel(0,0,99,1));
		field_dhexa.setModel(new SpinnerNumberModel(0,0,99,1));
		field_neu5gc.setModel(new SpinnerNumberModel(0,0,99,1));
		field_neu5ac.setModel(new SpinnerNumberModel(0,0,99,1));
		field_neu5gclac.setModel(new SpinnerNumberModel(0,0,99,1));
		field_neu5aclac.setModel(new SpinnerNumberModel(0,0,99,1));
		field_kdo.setModel(new SpinnerNumberModel(0,0,99,1));
		field_kdn.setModel(new SpinnerNumberModel(0,0,99,1));
		field_mur.setModel(new SpinnerNumberModel(0,0,99,1));
	}
	
	public String getReturnStatus() {
		return return_status;
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
	private void initComponents() {
		jLabel4 = new javax.swing.JLabel();
		jLabel8 = new javax.swing.JLabel();
		field_pen = new javax.swing.JSpinner();
		jLabel9 = new javax.swing.JLabel();
		jLabel10 = new javax.swing.JLabel();
		field_hex = new javax.swing.JSpinner();
		jLabel11 = new javax.swing.JLabel();
		jLabel13 = new javax.swing.JLabel();
		field_hep = new javax.swing.JSpinner();
		jLabel14 = new javax.swing.JLabel();
		jLabel15 = new javax.swing.JLabel();
		field_hexn = new javax.swing.JSpinner();
		jLabel16 = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		field_hexnac = new javax.swing.JSpinner();
		jLabel17 = new javax.swing.JLabel();
		jLabel18 = new javax.swing.JLabel();
		field_dpen = new javax.swing.JSpinner();
		jLabel19 = new javax.swing.JLabel();
		jLabel20 = new javax.swing.JLabel();
		field_dhex = new javax.swing.JSpinner();
		jLabel21 = new javax.swing.JLabel();
		jLabel22 = new javax.swing.JLabel();
		field_ddhex = new javax.swing.JSpinner();
		jLabel23 = new javax.swing.JLabel();
		jLabel24 = new javax.swing.JLabel();
		field_hexa = new javax.swing.JSpinner();
		jLabel28 = new javax.swing.JLabel();
		jLabel29 = new javax.swing.JLabel();
		field_dhexa = new javax.swing.JSpinner();
		jLabel30 = new javax.swing.JLabel();
		jLabel31 = new javax.swing.JLabel();
		field_neu5gc = new javax.swing.JSpinner();
		jLabel32 = new javax.swing.JLabel();
		jLabel33 = new javax.swing.JLabel();
		field_neu5ac = new javax.swing.JSpinner();
		jLabel34 = new javax.swing.JLabel();
		jLabel35 = new javax.swing.JLabel();
		field_neu5gclac = new javax.swing.JSpinner();
		jLabel36 = new javax.swing.JLabel();
		jLabel37 = new javax.swing.JLabel();
		field_neu5aclac = new javax.swing.JSpinner();
		jLabel38 = new javax.swing.JLabel();
		jLabel39 = new javax.swing.JLabel();
		field_mehex = new javax.swing.JSpinner();
		jLabel40 = new javax.swing.JLabel();
		jLabel41 = new javax.swing.JLabel();
		field_kdo = new javax.swing.JSpinner();
		jLabel42 = new javax.swing.JLabel();
		jLabel43 = new javax.swing.JLabel();
		field_kdn = new javax.swing.JSpinner();
		jLabel44 = new javax.swing.JLabel();
		jLabel45 = new javax.swing.JLabel();
		field_mur = new javax.swing.JSpinner();
		//jLabel46 = new javax.swing.JLabel();
		jSeparator2 = new javax.swing.JSeparator();
		button_ok = new javax.swing.JButton();
		button_cancel = new javax.swing.JButton();
		/*jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        jLabel53 = new javax.swing.JLabel();
        jLabel54 = new javax.swing.JLabel();
        jLabel55 = new javax.swing.JLabel();
        jLabel56 = new javax.swing.JLabel();
        jLabel57 = new javax.swing.JLabel();
        jLabel58 = new javax.swing.JLabel();
        jLabel59 = new javax.swing.JLabel();
        jLabel60 = new javax.swing.JLabel();
        jLabel61 = new javax.swing.JLabel();
        jLabel65 = new javax.swing.JLabel();
        jLabel66 = new javax.swing.JLabel();
        jLabel67 = new javax.swing.JLabel();
        jLabel62 = new javax.swing.JLabel();
        jLabel63 = new javax.swing.JLabel();
        jLabel64 = new javax.swing.JLabel();
        field_s = new javax.swing.JSpinner();
        field_p = new javax.swing.JSpinner();
        field_ac = new javax.swing.JSpinner();
        field_pyr = new javax.swing.JSpinner();
        field_pc = new javax.swing.JSpinner();
        field_or1 = new javax.swing.JSpinner();
        field_or2 = new javax.swing.JSpinner();
        field_or3 = new javax.swing.JSpinner();
        field_or1_mass = new javax.swing.JTextField();
        field_or2_mass = new javax.swing.JTextField();
        field_or3_mass = new javax.swing.JTextField();
        field_or1_name = new javax.swing.JTextField();
        field_or2_name = new javax.swing.JTextField();
        field_or3_name = new javax.swing.JTextField();
		 */
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				closeDialog();
			}
		});

		jLabel4.setText("Pentose");
		jLabel8.setText("Pen");
		jLabel9.setText("Hexose");
		jLabel10.setText("Hex");
		jLabel11.setText("Heptose");
		jLabel13.setText("Hep");
		jLabel14.setText("Hexosamine");
		jLabel15.setText("HexN");
		jLabel16.setText("N-Acetyl Hexosamine");
		jLabel2.setText("HexNAc");
		jLabel17.setText("Deoxy-Pentose");
		jLabel18.setText("dPen");
		jLabel19.setText("Deoxy-Hexose");
		jLabel20.setText("dHex");
		jLabel21.setText("DiDeoxy-Hexose");
		jLabel22.setText("ddHex");
		jLabel23.setText("Hexuronic Acid");
		jLabel24.setText("HexA");
		jLabel28.setText("Dehydro Hexuronic Acid");
		jLabel29.setText("dHexA");
		jLabel30.setText("N-Glycolyl Neuraminic Acid");
		jLabel31.setText("Neu5Gc");
		jLabel32.setText("N-Acetyl Neuraminic acid");
		jLabel33.setText("Neu5Ac");
		jLabel34.setText("Lactonized Neu5Gc");
		jLabel35.setText("Neu5Gc-Lac");
		jLabel36.setText("Lactonized Neu5Ac");
		jLabel37.setText("Neu5Ac-Lac");
		jLabel38.setText("Methyl-Hexose");
		jLabel39.setText("MeHex");
		jLabel40.setText("KDO");
		jLabel41.setText("KDO");
		jLabel42.setText("KDN");
		jLabel43.setText("KDN");
		jLabel44.setText("Muramic Acid");
		jLabel45.setText("Mur");
		//jLabel46.setText("Solphate");
		button_ok.setText("OK");
		button_cancel.setText("Cancel");
		/*jLabel47.setText("S");
        jLabel48.setText("Other residue");
        jLabel49.setText("Or1");
        jLabel50.setText("Other residue");
        jLabel51.setText("Or2");
        jLabel52.setText("Other residue");
        jLabel53.setText("Or3");
        jLabel54.setText("Phosphate");
        jLabel55.setText("Acetate");
        jLabel56.setText("Pyruvate");
        jLabel57.setText("Phosphocholine");
        jLabel58.setText("P");
        jLabel59.setText("Ac");
        jLabel60.setText("Pyr");
        jLabel61.setText("PC");
        jLabel65.setText("Or1 name");
        field_or1_mass.setText("jTextField1");
        field_or2_mass.setText("jTextField1");
        jLabel66.setText("Or2 name");
        jLabel67.setText("Or3 name");
        field_or3_mass.setText("jTextField1");
        field_or1_name.setText("jTextField1");
        field_or1_name.setMinimumSize(new java.awt.Dimension(4, 64));
        jLabel62.setText("mass");
        field_or2_name.setText("jTextField1");
        field_or2_name.setMinimumSize(new java.awt.Dimension(4, 64));
        field_or3_name.setText("jTextField1");
        field_or3_name.setMinimumSize(new java.awt.Dimension(4, 64));
        jLabel63.setText("mass");
        jLabel64.setText("mass");
		 */

		org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this.getContentPane());
		this.getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(layout.createSequentialGroup()
						.addContainerGap()
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
								.add(jSeparator2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 654, Short.MAX_VALUE)
								.add(layout.createSequentialGroup()
										.add(203, 203, 203)
										.add(button_ok)
										.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
										.add(button_cancel))
								.add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
										.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
												/*.add(layout.createSequentialGroup()
                                .add(jLabel66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 69, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(field_or2_name, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 72, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            )
                            .add(jLabel52, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
                            .add(jLabel50, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
                            .add(jLabel48, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
												 */.add(jLabel38, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
												 .add(jLabel21, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
												 .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel14, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
												 .add(jLabel16, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
												 .add(jLabel11, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
												 .add(jLabel9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
												 .add(jLabel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
												 .add(jLabel17, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
												 .add(jLabel19, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
												 /*.add(layout.createSequentialGroup()
                                .add(jLabel65, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 69, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(field_or1_name, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 72, Short.MAX_VALUE))
                            .add(layout.createSequentialGroup()
                                .add(jLabel67, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 69, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(field_or3_name, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 72, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            )*/
												)
										.add(12, 12, 12)
										/*.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jLabel64)
                            .add(jLabel62)
                            .add(jLabel63)
                        )
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
										 */.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
												 .add(layout.createSequentialGroup()
														 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
																 .add(jLabel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 58, Short.MAX_VALUE)
																 .add(jLabel15, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																 .add(jLabel13, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																 .add(jLabel10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																 .add(jLabel8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																 .add(jLabel18, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																 .add(jLabel20, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																 .add(jLabel22, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																 .add(jLabel39, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																 //       .add(jLabel49, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																 //       .add(jLabel51, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																 //       .add(jLabel53)
																 )
														 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
														 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
																 .add(field_hex, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																 .add(field_pen, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																 .add(field_hep, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																 .add(field_hexn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																 .add(field_hexnac, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																 .add(field_dpen, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																 .add(field_dhex, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																 .add(field_ddhex, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																 .add(field_mehex, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																 //        .add(field_or1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																 //        .add(field_or2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																 //        .add(field_or3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																 )
														 )
												 //.add(org.jdesktop.layout.GroupLayout.TRAILING, field_or3_mass, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE)
												 //.add(org.jdesktop.layout.GroupLayout.TRAILING, field_or2_mass, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE)
												 //.add(org.jdesktop.layout.GroupLayout.TRAILING, field_or1_mass, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE)
												 )
										 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
										 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
												 .add(jLabel28, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
												 .add(jLabel30, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
												 .add(jLabel32, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
												 .add(jLabel34, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
												 .add(jLabel23, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 .add(jLabel36, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 .add(jLabel40, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 .add(jLabel42, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 .add(jLabel44, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 /*    .add(jLabel46, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel54, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel55, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel56, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel57)
												  */    )
										 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
										 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
												 .add(jLabel41, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 .add(jLabel37, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 .add(jLabel24, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 .add(jLabel29, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 .add(jLabel31, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 .add(jLabel33, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 .add(jLabel35, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 .add(jLabel43, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 .add(jLabel45, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												 /*    .add(jLabel47, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel58, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel59, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel60, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel61)
												  */    )
										 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
										 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
												 .add(field_dhexa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												 .add(field_hexa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												 .add(field_neu5gc, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												 .add(field_neu5ac, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												 .add(field_neu5gclac, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												 .add(field_neu5aclac, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												 .add(field_kdo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												 .add(field_kdn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												 .add(field_mur, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												 /*   .add(field_s, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(field_p, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(field_ac, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(field_pyr, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(field_pc, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												  */)
										)
								)
						.addContainerGap()
						)
				);

		layout.linkSize(new java.awt.Component[] {button_cancel, button_ok}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

		layout.setVerticalGroup(
				layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(layout.createSequentialGroup()
						.addContainerGap()
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								.add(jLabel4)
								.add(jLabel8)
								.add(field_pen, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jLabel23)
								.add(jLabel24)
								.add(field_hexa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								.add(jLabel9)
								.add(jLabel10)
								.add(field_hex, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jLabel28)
								.add(jLabel29)
								.add(field_dhexa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								.add(jLabel11)
								.add(jLabel13)
								.add(field_hep, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jLabel30)
								.add(jLabel31)
								.add(field_neu5gc, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								.add(jLabel14)
								.add(jLabel15)
								.add(field_hexn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jLabel32)
								.add(jLabel33)
								.add(field_neu5ac, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								.add(jLabel16)
								.add(jLabel2)
								.add(field_hexnac, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jLabel34)
								.add(jLabel35)
								.add(field_neu5gclac, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								.add(jLabel17)
								.add(jLabel18)
								.add(field_dpen, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jLabel36)
								.add(jLabel37)
								.add(field_neu5aclac, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								.add(jLabel19)
								.add(jLabel20)
								.add(field_dhex, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jLabel40)
								.add(jLabel41)
								.add(field_kdo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								.add(jLabel21)
								.add(jLabel22)
								.add(field_ddhex, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jLabel42)
								.add(jLabel43)
								.add(field_kdn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								.add(jLabel38)
								.add(jLabel39)
								.add(field_mehex, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jLabel44)
								.add(jLabel45)
								.add(field_mur, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						// .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								//     .add(jLabel48)
								//     .add(jLabel49)
								//     .add(field_or1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								//     .add(jLabel46)
								//     .add(jLabel47)
								//     .add(field_s, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								//     )
						// .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						// .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								//     .add(jLabel50)
								//     .add(jLabel51)
								//     .add(field_or2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
						//     .add(jLabel54)
						//     .add(jLabel58)
						//     .add(field_p, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
						// .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						// .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
						//     .add(jLabel52)
						//    .add(jLabel53)
						//   .add(field_or3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
						//   .add(jLabel55)
						//   .add(jLabel59)
						//   .add(field_ac, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
						//.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						// .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
						//    .add(jLabel56)
						//    .add(jLabel60)
						//    .add(field_pyr, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
						//    .add(jLabel65)
						//    .add(jLabel62)
						//    .add(field_or1_name, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
						//    .add(field_or1_mass, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
						//.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						/* .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel57)
                    .add(jLabel61)
                    .add(field_pc, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel66)
                    .add(field_or2_mass, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel63)
                    .add(field_or2_name, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel67)
                    .add(field_or3_mass, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel64)
                    .add(field_or3_name, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						 */.add(jSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
						 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								 .add(button_ok)
								 .add(button_cancel))
						 .addContainerGap())
				);

		layout.linkSize(new java.awt.Component[] {button_cancel, button_ok}, org.jdesktop.layout.GroupLayout.VERTICAL);

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
	//private JSpinner field_ac;
	private JSpinner field_ddhex;
	private JSpinner field_dhex;
	private JSpinner field_dhexa;
	private JSpinner field_dpen;
	private JSpinner field_hep;
	private JSpinner field_hex;
	private JSpinner field_hexa;
	private JSpinner field_hexn;
	private JSpinner field_hexnac;
	private JSpinner field_kdn;
	private JSpinner field_kdo;
	private JSpinner field_mehex;
	private JSpinner field_mur;
	private JSpinner field_neu5ac;
	private JSpinner field_neu5aclac;
	private JSpinner field_neu5gc;
	private JSpinner field_neu5gclac;
	/*
    private JSpinner field_or1;
    private JTextField field_or1_mass;
    private JTextField field_or1_name;
    private JTextField field_or1_name1;
    private JSpinner field_or2;
    private JTextField field_or2_mass;
    private JTextField field_or2_name;
    private JSpinner field_or3;
    private JTextField field_or3_mass;
    private JTextField field_or3_name;
    private JSpinner field_p;
    private JSpinner field_pc;
	 */
	private JSpinner field_pen;
	/*
    private JSpinner field_pyr;
    private JSpinner field_s;
	 */
	private JLabel jLabel10;
	private JLabel jLabel11;
	private JLabel jLabel13;
	private JLabel jLabel14;
	private JLabel jLabel15;
	private JLabel jLabel16;
	private JLabel jLabel17;
	private JLabel jLabel18;
	private JLabel jLabel19;
	private JLabel jLabel2;
	private JLabel jLabel20;
	private JLabel jLabel21;
	private JLabel jLabel22;
	private JLabel jLabel23;
	private JLabel jLabel24;
	private JLabel jLabel28;
	private JLabel jLabel29;
	private JLabel jLabel30;
	private JLabel jLabel31;
	private JLabel jLabel32;
	private JLabel jLabel33;
	private JLabel jLabel34;
	private JLabel jLabel35;
	private JLabel jLabel36;
	private JLabel jLabel37;
	private JLabel jLabel38;
	private JLabel jLabel39;
	private JLabel jLabel4;
	private JLabel jLabel40;
	private JLabel jLabel41;
	private JLabel jLabel42;
	private JLabel jLabel43;
	private JLabel jLabel44;
	private JLabel jLabel45;
	/*private JLabel jLabel46;
    private JLabel jLabel47;
    private JLabel jLabel48;
    private JLabel jLabel49;
    private JLabel jLabel50;
    private JLabel jLabel51;
    private JLabel jLabel52;
    private JLabel jLabel53;
    private JLabel jLabel54;
    private JLabel jLabel55;
    private JLabel jLabel56;
    private JLabel jLabel57;
    private JLabel jLabel58;
    private JLabel jLabel59;
    private JLabel jLabel60;
    private JLabel jLabel61;
    private JLabel jLabel62;
    private JLabel jLabel63;
    private JLabel jLabel64;
    private JLabel jLabel65;
    private JLabel jLabel66;
    private JLabel jLabel67;
	 */
	private JLabel jLabel8;
	private JLabel jLabel9;
	private JSeparator jSeparator2;
	// End of variables declaration//GEN-END:variables

}
