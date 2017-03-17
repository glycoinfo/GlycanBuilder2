package org.eurocarbdb.application.glycanbuilder.util;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.eurocarbdb.application.glycanbuilder.CustomFocusTraversalPolicy;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;

public class ReadDialog extends EscapeDialog implements ActionListener, ItemListener {
	
	private JButton button_cancel;
	private JButton button_ok;
	private JComboBox list_format;
	private JTextField field_input_text;
	private JLabel jLabel1;
	private JLabel jLabel2;
	private JSeparator jSeparator1;
	
	private String str_input = "";
	private GlycanDocument a_objGD;
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		enableItems();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		if (action == "OK") {
			this.retrieveData();
			this.closeDialog();
		}
		else if (action == "Cancel"){
			this.closeDialog();
		}
	}

	public ReadDialog(Frame owner, GlycanDocument _a_objGD) {
		super(owner, true);

		this.a_objGD = _a_objGD;
		
		// init values
		this.initComponents();
		this.fillComponents();
		this.setTraversal();
		this.setActions();
		this.enableItems();

		// set location
		this.setLocationRelativeTo(owner);
	}
	
	private void setActions() {
	    this.button_ok.addActionListener(this);
	    this.button_cancel.addActionListener(this);
	}
	
	private void fillComponents() {
		this.list_format.setModel(
				new DefaultComboBoxModel(
						new String[] {"GlycoCT{condensed}", "LinearCode", "KCF", "LINUCS", "WURCS1.0", "WURCS2.0"}));
	}

	private void setTraversal() {
		CustomFocusTraversalPolicy tp = new CustomFocusTraversalPolicy();
		tp.addComponent(this.field_input_text);
		tp.addComponent(this.button_ok);
		tp.addComponent(this.button_cancel);
		this.setFocusTraversalPolicy(tp);
		getRootPane().setDefaultButton(this.button_ok);
	}

	private void enableItems() {
		this.list_format.setEnabled(true);
	}
	
	private void closeDialog() {
		setVisible(false);
        dispose();
	}

	private void retrieveData() {
		this.str_input = this.field_input_text.getText();
		if(this.str_input.equals("")) return;

		String format = this.getSelectedValueChar(this.list_format);
		this.a_objGD.importFrom(this.str_input, format);
	}

	private String getSelectedValueChar(JComboBox field) {
		return String.valueOf(field.getSelectedItem());
	}
	
	private void initComponents() {
		this.jLabel1 = new JLabel();
		this.jLabel2 = new JLabel();
		this.field_input_text = new JTextField();
		this.list_format = new JComboBox();
		this.jSeparator1 = new JSeparator();
		this.button_ok = new JButton();
		this.button_cancel = new JButton();

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				closeDialog();
			}
		});

		this.jLabel1.setText("Input text");
		this.jLabel2.setText("Format");
		this.field_input_text.setText("");
		this.button_ok.setText("OK");
		this.button_cancel.setText("Cancel");
		this.list_format.setModel(
				new DefaultComboBoxModel(
						new String[] {"GlycoCT", "LinearCode", "KCF", "LINUCS", "WURCS1.0", "WURCS2.0"}));

		GroupLayout layout = new GroupLayout(this.getContentPane());
		this.getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.LEADING)
			.add(layout.createSequentialGroup()
				.addContainerGap()
				.add(layout.createParallelGroup(GroupLayout.LEADING)
					.add(layout.createSequentialGroup()
						.add(12, 12, 12)
						.add(button_ok)
						.addPreferredGap(LayoutStyle.RELATED)
						.add(button_cancel))
					.add(jSeparator1, GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE)
					.add(layout.createSequentialGroup()
						.add(layout.createParallelGroup(GroupLayout.LEADING)
							.add(jLabel1)
							.add(this.jLabel2))
						.addPreferredGap(LayoutStyle.RELATED)
						.add(layout.createParallelGroup(GroupLayout.LEADING)
							.add(this.field_input_text, GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE)
							.add(this.list_format, 0, 66, Short.MAX_VALUE))
						)
					)
				.addContainerGap())
		);

		layout.linkSize(new Component[] {button_cancel, button_ok}, GroupLayout.HORIZONTAL);

		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.LEADING)
			.add(layout.createSequentialGroup()
				.addContainerGap()
				.add(layout.createParallelGroup(GroupLayout.BASELINE)
					.add(jLabel1)
					.add(this.field_input_text, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.RELATED)
				.add(layout.createParallelGroup(GroupLayout.BASELINE)
					.add(this.jLabel2)
					.add(this.list_format, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.RELATED)
				.add(jSeparator1, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.RELATED)
				.add(layout.createParallelGroup(GroupLayout.BASELINE)
					.add(button_ok)
					.add(button_cancel))
				.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
		);
		pack();
	}
}
