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

import java.util.Collection;

import org.eurocarbdb.application.glycanbuilder.CustomFocusTraversalPolicy;

/**
 * Dialog to change the graphic settings
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class GraphicOptionsDialog extends EscapeDialog implements
		java.awt.event.ActionListener, java.awt.event.ItemListener,
		javax.swing.event.ChangeListener {

	private GraphicOptions theGraphicOptions = null;
	private boolean ignore_change = false;

	/**
	 * Creates a new dialog
	 * 
	 * @param parent
	 *            the parent frame
	 * @param options
	 *            the options to be changed
	 */
	public GraphicOptionsDialog(java.awt.Frame parent, GraphicOptions options) {
		super(parent, true);

		theGraphicOptions = options;

		initComponents();
		initData(theGraphicOptions);
		setTraversal();
		setActions();
		enableItems();

		// set location
		setLocationRelativeTo(parent);
	}

	private Object[] generateValues(int min, int max) {

		Object[] values = new Object[max - min + 1];
		for (int i = min; i <= max; i++)
			values[i - min] = i;
		return values;
	}

	private void initData(GraphicOptions model) {
		java.util.Vector<String> availableFonts=GraphicOptions.getAllFontFaces();
		// set models
		field_display.setModel(new javax.swing.DefaultComboBoxModel(
				new String[] { GraphicOptions.DISPLAY_NORMALINFO,
						GraphicOptions.DISPLAY_NORMAL,
						GraphicOptions.DISPLAY_COMPACT,
						GraphicOptions.DISPLAY_CUSTOM }));

		field_node_size.setModel(new javax.swing.SpinnerNumberModel(1, 1, 100,
				1));
		field_node_font_size.setModel(new javax.swing.SpinnerNumberModel(1, 1,
				40, 1));
		field_node_font_face.setModel(new javax.swing.DefaultComboBoxModel(availableFonts));

		field_composition_font_size
				.setModel(new javax.swing.SpinnerNumberModel(1, 1, 40, 1));
		field_composition_font_face
				.setModel(new javax.swing.DefaultComboBoxModel(availableFonts));

		field_linkage_info_size.setModel(new javax.swing.SpinnerNumberModel(1,
				1, 40, 1));
		field_linkage_info_font_face
				.setModel(new javax.swing.DefaultComboBoxModel(availableFonts));

		field_node_space.setModel(new javax.swing.SpinnerNumberModel(1, 1, 100,
				1));
		field_node_sub_space.setModel(new javax.swing.SpinnerNumberModel(1, 1,
				40, 1));
		field_structures_space.setModel(new javax.swing.SpinnerNumberModel(1,
				1, 100, 1));

		field_mass_text_space.setModel(new javax.swing.SpinnerNumberModel(1, 1,
				100, 1));
		field_mass_text_size.setModel(new javax.swing.SpinnerNumberModel(1, 1,
				40, 1));
		field_mass_text_font_face
				.setModel(new javax.swing.DefaultComboBoxModel(availableFonts));

		// set selections
		GraphicOptions options = new GraphicOptions(model);
		options.setScale(1.);

		ignore_change = true;
		field_display.setSelectedItem(options.DISPLAY);

		field_show_info.setSelected(options.SHOW_INFO);

		field_node_size.setValue(options.NODE_SIZE);
		field_node_font_size.setValue(options.NODE_FONT_SIZE);
		field_node_font_face.setSelectedItem(options.NODE_FONT_FACE);

		field_composition_font_size.setValue(options.COMPOSITION_FONT_SIZE);
		field_composition_font_face
				.setSelectedItem(options.COMPOSITION_FONT_FACE);

		field_linkage_info_size.setValue(options.LINKAGE_INFO_SIZE);
		field_linkage_info_font_face
				.setSelectedItem(options.LINKAGE_INFO_FONT_FACE);

		field_node_space.setValue(options.NODE_SPACE);
		field_node_sub_space.setValue(options.NODE_SUB_SPACE);
		field_structures_space.setValue(options.STRUCTURES_SPACE);

		field_mass_text_space.setValue(options.MASS_TEXT_SPACE);
		field_mass_text_size.setValue(options.MASS_TEXT_SIZE);
		field_mass_text_font_face.setSelectedItem(options.MASS_TEXT_FONT_FACE);

		ignore_change = false;
		pack();
	}

	private void setTraversal() {
		CustomFocusTraversalPolicy tp = new CustomFocusTraversalPolicy();
		tp.addComponent(field_display);
		tp.addComponent(field_show_info);
		tp.addComponent(field_node_size);
		tp.addComponent(field_node_font_size);
		tp.addComponent(field_node_font_face);
		tp.addComponent(field_composition_font_size);
		tp.addComponent(field_composition_font_face);
		tp.addComponent(field_linkage_info_size);
		tp.addComponent(field_linkage_info_font_face);
		tp.addComponent(field_node_space);
		tp.addComponent(field_node_sub_space);
		tp.addComponent(field_structures_space);
		tp.addComponent(field_mass_text_space);
		tp.addComponent(field_mass_text_size);
		tp.addComponent(field_mass_text_font_face);
		tp.addComponent(button_ok);
		tp.addComponent(button_cancel);
		this.setFocusTraversalPolicy(tp);

		getRootPane().setDefaultButton(button_ok);
	}

	private void setActions() {
		field_display.addItemListener(this);

		// listen for changes in settings
		field_show_info.addActionListener(this);
		field_node_size.addChangeListener(this);
		field_node_font_size.addChangeListener(this);
		field_node_font_face.addItemListener(this);
		field_composition_font_size.addChangeListener(this);
		field_composition_font_face.addItemListener(this);
		field_linkage_info_size.addChangeListener(this);
		field_linkage_info_font_face.addItemListener(this);
		field_node_space.addChangeListener(this);
		field_node_sub_space.addChangeListener(this);
		field_structures_space.addChangeListener(this);
		field_mass_text_space.addChangeListener(this);
		field_mass_text_size.addChangeListener(this);
		field_mass_text_font_face.addItemListener(this);

		button_ok.addActionListener(this);
		button_cancel.addActionListener(this);
	}

	private void enableItems() {

	}

	private void retrieveData() {
		theGraphicOptions.setDisplay((String) field_display.getSelectedItem());
		if (field_display.getSelectedItem().equals(
				GraphicOptions.DISPLAY_CUSTOM)) {
			theGraphicOptions.SHOW_INFO_CUSTOM = field_show_info.isSelected();

			theGraphicOptions.NODE_SIZE_CUSTOM = (Integer) field_node_size
					.getValue();
			theGraphicOptions.NODE_FONT_SIZE_CUSTOM = (Integer) field_node_font_size
					.getValue();
			theGraphicOptions.NODE_FONT_FACE_CUSTOM = (String) field_node_font_face
					.getSelectedItem();

			theGraphicOptions.COMPOSITION_FONT_SIZE_CUSTOM = (Integer) field_composition_font_size
					.getValue();
			theGraphicOptions.COMPOSITION_FONT_FACE_CUSTOM = (String) field_composition_font_face
					.getSelectedItem();

			theGraphicOptions.LINKAGE_INFO_SIZE_CUSTOM = (Integer) field_linkage_info_size
					.getValue();
			theGraphicOptions.LINKAGE_INFO_FONT_FACE_CUSTOM = (String) field_linkage_info_font_face
					.getSelectedItem();

			theGraphicOptions.NODE_SPACE_CUSTOM = (Integer) field_node_space
					.getValue();
			theGraphicOptions.NODE_SUB_SPACE_CUSTOM = (Integer) field_node_sub_space
					.getValue();
			theGraphicOptions.STRUCTURES_SPACE_CUSTOM = (Integer) field_structures_space
					.getValue();

			theGraphicOptions.MASS_TEXT_SPACE_CUSTOM = (Integer) field_mass_text_space
					.getValue();
			theGraphicOptions.MASS_TEXT_SIZE_CUSTOM = (Integer) field_mass_text_size
					.getValue();
			theGraphicOptions.MASS_TEXT_FONT_FACE_CUSTOM = (String) field_mass_text_font_face
					.getSelectedItem();
		}
	}

	public void itemStateChanged(java.awt.event.ItemEvent e) {
		if (!ignore_change) {
			if (e.getSource() == field_display) {
				initData(new GraphicOptions((String) field_display
						.getSelectedItem()));
			} else {
				ignore_change = true;
				field_display.setSelectedItem(GraphicOptions.DISPLAY_CUSTOM);
				ignore_change = false;
			}
		}
	}

	public void stateChanged(javax.swing.event.ChangeEvent e) {
		if (!ignore_change) {
			ignore_change = true;
			field_display.setSelectedItem(GraphicOptions.DISPLAY_CUSTOM);
			ignore_change = false;
		}
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {
		String action = e.getActionCommand();

		if (e.getSource() == field_show_info) {
			if (!ignore_change) {
				ignore_change = true;
				field_display.setSelectedItem(GraphicOptions.DISPLAY_CUSTOM);
				ignore_change = false;
			}
		} else {
			if (action == "OK") {
				return_status = action;
				retrieveData();
				closeDialog();
			} else if (action == "Cancel") {
				return_status = action;
				closeDialog();
			}
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	private void initComponents() {
		jLabel1 = new javax.swing.JLabel();
		field_show_info = new javax.swing.JCheckBox();
		field_node_size = new javax.swing.JSpinner();
		jLabel2 = new javax.swing.JLabel();
		field_node_font_size = new javax.swing.JSpinner();
		jLabel3 = new javax.swing.JLabel();
		field_node_font_face = new javax.swing.JComboBox();
		jLabel4 = new javax.swing.JLabel();
		field_linkage_info_size = new javax.swing.JSpinner();
		jLabel5 = new javax.swing.JLabel();
		field_linkage_info_font_face = new javax.swing.JComboBox();
		jLabel6 = new javax.swing.JLabel();
		field_node_space = new javax.swing.JSpinner();
		jLabel7 = new javax.swing.JLabel();
		field_node_sub_space = new javax.swing.JSpinner();
		jLabel8 = new javax.swing.JLabel();
		field_structures_space = new javax.swing.JSpinner();
		jLabel9 = new javax.swing.JLabel();
		field_mass_text_space = new javax.swing.JSpinner();
		jLabel10 = new javax.swing.JLabel();
		field_mass_text_size = new javax.swing.JSpinner();
		jLabel11 = new javax.swing.JLabel();
		jLabel12 = new javax.swing.JLabel();
		button_ok = new javax.swing.JButton();
		button_cancel = new javax.swing.JButton();
		jSeparator1 = new javax.swing.JSeparator();
		field_mass_text_font_face = new javax.swing.JComboBox();
		field_composition_font_face = new javax.swing.JComboBox();
		jLabel13 = new javax.swing.JLabel();
		field_composition_font_size = new javax.swing.JSpinner();
		jLabel14 = new javax.swing.JLabel();
		field_display = new javax.swing.JComboBox();
		jLabel15 = new javax.swing.JLabel();

		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				closeDialog();
			}
		});

		jLabel1.setText("Show linkage info"); // NOI18N

		field_show_info.setBorder(javax.swing.BorderFactory.createEmptyBorder(
				0, 0, 0, 0));

		jLabel2.setText("Residue size"); // NOI18N

		jLabel3.setText("Residue text size"); // NOI18N

		field_node_font_face.setModel(new javax.swing.DefaultComboBoxModel(
				new String[] { "Arial", "Serif" }));

		jLabel4.setText("Residue text font"); // NOI18N

		jLabel5.setText("Linkage info text size"); // NOI18N

		field_linkage_info_font_face
				.setModel(new javax.swing.DefaultComboBoxModel(new String[] {
						"Item 1", "Item 2", "Item 3", "Item 4" }));

		jLabel6.setText("Linkage info text font"); // NOI18N

		jLabel7.setText("Space between residues"); // NOI18N

		jLabel8.setText("Space between border residues"); // NOI18N

		jLabel9.setText("Space between structures"); // NOI18N

		jLabel10.setText("Space before mass text"); // NOI18N

		jLabel11.setText("Mass text size"); // NOI18N

		jLabel12.setText("Mass text font"); // NOI18N

		button_ok.setText("OK"); // NOI18N

		button_cancel.setText("Cancel"); // NOI18N

		field_mass_text_font_face
				.setModel(new javax.swing.DefaultComboBoxModel(new String[] {
						"Item 1", "Item 2", "Item 3", "Item 4" }));

		field_composition_font_face
				.setModel(new javax.swing.DefaultComboBoxModel(new String[] {
						"Arial", "Serif" }));

		jLabel13.setText("Composition text font"); // NOI18N

		jLabel14.setText("Composition text size"); // NOI18N

		field_display.setModel(new javax.swing.DefaultComboBoxModel(
				new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

		jLabel15.setText("Display type"); // NOI18N

		org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(
				this.getContentPane());
		this.getContentPane().setLayout(layout);
		layout
				.setHorizontalGroup(layout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(
								layout
										.createSequentialGroup()
										.addContainerGap()
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.LEADING)
														.add(
																jSeparator1,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																315,
																Short.MAX_VALUE)
														.add(
																layout
																		.createSequentialGroup()
																		.add(
																				layout
																						.createParallelGroup(
																								org.jdesktop.layout.GroupLayout.LEADING)
																						.add(
																								jLabel1)
																						.add(
																								jLabel2)
																						.add(
																								jLabel3)
																						.add(
																								jLabel4)
																						.add(
																								jLabel14)
																						.add(
																								jLabel13)
																						.add(
																								jLabel15)
																						.add(
																								layout
																										.createParallelGroup(
																												org.jdesktop.layout.GroupLayout.LEADING)
																										.add(
																												org.jdesktop.layout.GroupLayout.TRAILING,
																												layout
																														.createSequentialGroup()
																														.add(
																																jLabel5)
																														.addPreferredGap(
																																org.jdesktop.layout.LayoutStyle.RELATED))
																										.add(
																												jLabel6))
																						.add(
																								jLabel7)
																						.add(
																								jLabel9)
																						.add(
																								jLabel10)
																						.add(
																								jLabel11)
																						.add(
																								jLabel12)
																						.add(
																								layout
																										.createSequentialGroup()
																										.add(
																												76,
																												76,
																												76)
																										.add(
																												button_ok)
																										.addPreferredGap(
																												org.jdesktop.layout.LayoutStyle.RELATED)
																										.add(
																												button_cancel))
																						.add(
																								jLabel8))
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				layout
																						.createParallelGroup(
																								org.jdesktop.layout.GroupLayout.TRAILING)
																						.add(
																								field_mass_text_font_face,
																								0,
																								113,
																								Short.MAX_VALUE)
																						.add(
																								field_linkage_info_size,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								113,
																								Short.MAX_VALUE)
																						.add(
																								org.jdesktop.layout.GroupLayout.LEADING,
																								field_show_info)
																						.add(
																								org.jdesktop.layout.GroupLayout.LEADING,
																								field_node_font_size,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								113,
																								Short.MAX_VALUE)
																						.add(
																								org.jdesktop.layout.GroupLayout.LEADING,
																								field_node_font_face,
																								0,
																								113,
																								Short.MAX_VALUE)
																						.add(
																								field_composition_font_size,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								113,
																								Short.MAX_VALUE)
																						.add(
																								field_node_size,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								113,
																								Short.MAX_VALUE)
																						.add(
																								field_composition_font_face,
																								0,
																								113,
																								Short.MAX_VALUE)
																						.add(
																								field_linkage_info_font_face,
																								0,
																								113,
																								Short.MAX_VALUE)
																						.add(
																								field_node_space,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								113,
																								Short.MAX_VALUE)
																						.add(
																								field_structures_space,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								113,
																								Short.MAX_VALUE)
																						.add(
																								field_mass_text_space,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								113,
																								Short.MAX_VALUE)
																						.add(
																								field_mass_text_size,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								113,
																								Short.MAX_VALUE)
																						.add(
																								field_node_sub_space,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								113,
																								Short.MAX_VALUE)
																						.add(
																								field_display,
																								0,
																								113,
																								Short.MAX_VALUE))))
										.addContainerGap()));

		layout.linkSize(new java.awt.Component[] { button_cancel, button_ok },
				org.jdesktop.layout.GroupLayout.HORIZONTAL);

		layout
				.setVerticalGroup(layout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(
								org.jdesktop.layout.GroupLayout.TRAILING,
								layout
										.createSequentialGroup()
										.addContainerGap()
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel15)
														.add(
																field_display,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.LEADING)
														.add(jLabel1)
														.add(field_show_info))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel2)
														.add(
																field_node_size,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel3)
														.add(
																field_node_font_size,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(
																field_node_font_face,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
														.add(jLabel4))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(
																field_composition_font_size,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
														.add(jLabel14))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel13)
														.add(
																field_composition_font_face,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel5)
														.add(
																field_linkage_info_size,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel6)
														.add(
																field_linkage_info_font_face,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel7)
														.add(
																field_node_space,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel8)
														.add(
																field_node_sub_space,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel9)
														.add(
																field_structures_space,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel10)
														.add(
																field_mass_text_space,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel11)
														.add(
																field_mass_text_size,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel12)
														.add(
																field_mass_text_font_face,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												jSeparator1,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
												10,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(button_cancel)
														.add(button_ok))
										.addContainerGap()));

		pack();
	}// </editor-fold>

	/** Closes the dialog */
	private void closeDialog() {// GEN-FIRST:event_closeDialog
		setVisible(false);
		dispose();
	}// GEN-LAST:event_closeDialog

	// Variables declaration - do not modify
	private javax.swing.JButton button_cancel;
	private javax.swing.JButton button_ok;
	private javax.swing.JComboBox field_composition_font_face;
	private javax.swing.JSpinner field_composition_font_size;
	private javax.swing.JComboBox field_display;
	private javax.swing.JComboBox field_linkage_info_font_face;
	private javax.swing.JSpinner field_linkage_info_size;
	private javax.swing.JComboBox field_mass_text_font_face;
	private javax.swing.JSpinner field_mass_text_size;
	private javax.swing.JSpinner field_mass_text_space;
	private javax.swing.JComboBox field_node_font_face;
	private javax.swing.JSpinner field_node_font_size;
	private javax.swing.JSpinner field_node_size;
	private javax.swing.JSpinner field_node_space;
	private javax.swing.JSpinner field_node_sub_space;
	private javax.swing.JCheckBox field_show_info;
	private javax.swing.JSpinner field_structures_space;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel10;
	private javax.swing.JLabel jLabel11;
	private javax.swing.JLabel jLabel12;
	private javax.swing.JLabel jLabel13;
	private javax.swing.JLabel jLabel14;
	private javax.swing.JLabel jLabel15;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;
	private javax.swing.JLabel jLabel8;
	private javax.swing.JLabel jLabel9;
	private javax.swing.JSeparator jSeparator1;
	// End of variables declaration

}
