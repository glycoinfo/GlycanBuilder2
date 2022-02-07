package org.eurocarbdb.application.glycanbuilder.util;

import java.awt.Frame;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.eurocarbdb.application.glycanbuilder.CustomFocusTraversalPolicy;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParserFactory;
import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;

/**
 * Dialog for API section
 * @author GIC 20211215
 */
public class SelectAPIDialog extends EscapeDialog implements ActionListener{

	public SelectAPIDialog(Frame _frame) {
		super(_frame, "GlyTouCan", true);
		this.initComponents();
		this.fillComponents();
		this.setSelections();
		this.setTraversal();
		this.setActions();
		this.enableItems();

		// set location
		setLocationRelativeTo(_frame);
	}

	private void setTraversal() {
		CustomFocusTraversalPolicy tp = new CustomFocusTraversalPolicy();
		tp.addComponent(this.field_string_format);
		tp.addComponent(this.button_ok);
		tp.addComponent(this.button_cancel);
		this.setFocusTraversalPolicy(tp);
		getRootPane().setDefaultButton(this.button_ok);
	}

	private void enableItems() {
	}

	private void setActions() {
	}

	private void setSelections() {
		this.button_ok.addActionListener(this);
		this.button_cancel.addActionListener(this);
	}
	
	public static Map<String,String> selectAPIDialog() {
		Map<String,String> ret = new TreeMap<String,String>();
		ret.put("https://glytoucan.org/","https://glytoucan.org/");
		ret.put("https://gtc.beta.glycosmos.org/", "https://gtc.beta.glycosmos.org/");
		return ret;
	}

	private void fillComponents() {
		Map<String,String> formats = selectAPIDialog();
		this.field_string_format.setModel(new DefaultComboBoxModel(formats.keySet().toArray()));
	}

	private void closeDialog() {
		setVisible(false);
		dispose();
	}

	private void initComponents() {
		this.jLabel1 = new JLabel();
		this.err = new JLabel();
		this.button_ok = new JButton();
		this.button_cancel = new JButton();
		this.field_string_format = new JComboBox();

		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				closeDialog();
			}
		});

		this.jLabel1.setText("Select API Environment ");
		this.button_ok.setText("OK");
		this.button_cancel.setText("Cancel");
		this.field_string_format.setModel(new DefaultComboBoxModel(new String[] { "glycominds" }));

		GroupLayout layout = new GroupLayout(this.getContentPane());
		this.getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.LEADING)
			.add(layout.createSequentialGroup()
				.add(layout.createParallelGroup(GroupLayout.LEADING)
					.add(layout.createSequentialGroup()
						.addContainerGap()
						.add(layout.createParallelGroup(GroupLayout.LEADING)
							.add(layout.createSequentialGroup()
								.add(this.jLabel1)
								.addPreferredGap(LayoutStyle.RELATED, 200, Short.MAX_VALUE)
								.add(this.field_string_format, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
							)
						)
					.add(layout.createSequentialGroup()
						.add(127, 127, 127)
						.add(this.button_ok)
						.addPreferredGap(LayoutStyle.RELATED)
						.add(this.button_cancel)
						)
					.add(layout.createSequentialGroup()
						.add(127, 127, 127)
						.add(this.err)
						)
					)
					.addContainerGap()
				)
		);

		layout.linkSize(new Component[] {this.button_cancel, this.button_ok}, GroupLayout.HORIZONTAL);

		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.LEADING)
			.add(layout.createSequentialGroup()
				.addContainerGap()
				.add(layout.createParallelGroup(GroupLayout.BASELINE)
					.add(this.jLabel1)
					.add(this.field_string_format, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					)
				.addPreferredGap(LayoutStyle.RELATED, 50, Short.MAX_VALUE)
				.add(17, 17, 17)
				.add(layout.createParallelGroup(GroupLayout.BASELINE)
					.add(this.button_cancel)
					.add(this.button_ok))
				.add(layout.createParallelGroup(GroupLayout.BASELINE)
					.add(this.err))
					.addContainerGap()
				)
		);
		pack();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		if (action == "OK") {
			this.return_status = "Encode";
			this.closeDialog();
		}
		else if (action == "Cancel"){
			this.return_status = "Cancel";
			this.closeDialog();
		}
	}
	
	public boolean isCanceled() {
		return this.return_status.equals("Cancel");
	}
	
	public String getFormat() {
		return (String) this.field_string_format.getSelectedItem();
	}

	private JButton button_cancel;
	private JButton button_ok;	
	private JComboBox field_string_format;
	private JLabel jLabel1;
	public JLabel err;
}
