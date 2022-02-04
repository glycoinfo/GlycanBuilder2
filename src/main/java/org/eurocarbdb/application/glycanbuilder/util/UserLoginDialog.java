package org.eurocarbdb.application.glycanbuilder.util;

import java.awt.Frame;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.eurocarbdb.application.glycanbuilder.CustomFocusTraversalPolicy;
import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;

/**
 * Dialog for UserData Entry
 */
public class UserLoginDialog extends EscapeDialog implements ActionListener{

	public UserLoginDialog(Frame _frame, String apikey, String contributor_id) {
		super(_frame, "GlyTouCanUserProfile", true);
		this.initComponents(apikey, contributor_id);
		this.setSelections();
		this.setTraversal();
		this.setActions();
		this.enableItems();

		// set location
		setLocationRelativeTo(_frame);
	}

	private void setTraversal() {
		CustomFocusTraversalPolicy tp = new CustomFocusTraversalPolicy();
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

	private void closeDialog() {
		setVisible(false);
		dispose();
	}

	private void initComponents(String apikey, String contributor_id) {
		this.jLabel1 = new JLabel();
		this.jTextField1 = new javax.swing.JTextField();
		this.jLabel2 = new JLabel();
		this.jTextField2 = new javax.swing.JTextField();
		
		this.button_ok = new JButton();
		this.button_cancel = new JButton();

		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				closeDialog();
			}
		});

		this.jLabel1.setText("Contributor ID : ");
		this.jLabel2.setText("API Key: ");
		
		this.button_ok.setText("OK");
		this.button_cancel.setText("Cancel");
		jTextField1.setColumns(40);
		jTextField1.setText(contributor_id);
		jTextField2.setColumns(40);
		jTextField2.setText(apikey);

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
								.addPreferredGap(LayoutStyle.RELATED, 50, Short.MAX_VALUE)
								.add(this.jTextField1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
							.add(layout.createSequentialGroup()
									.add(this.jLabel2)
									.addPreferredGap(LayoutStyle.RELATED, 50, Short.MAX_VALUE)
									.add(this.jTextField2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
							)
						)
					.add(layout.createSequentialGroup()
						.add(127, 127, 127)
						.add(this.button_ok)
						.addPreferredGap(LayoutStyle.RELATED)
						.add(this.button_cancel)
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
					.add(this.jTextField1)
					)
				.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
				.add(layout.createParallelGroup(GroupLayout.BASELINE)
						.add(this.jLabel2)
						.add(this.jTextField2)
						) 
				.addPreferredGap(LayoutStyle.RELATED)
				.add(17, 17, 17)
				.add(layout.createParallelGroup(GroupLayout.BASELINE)
					.add(this.button_cancel)
					.add(this.button_ok))
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
	
	public String getContributorId() {
		return this.jTextField1.getText();
	}
	
	public String getApikey() {
		return this.jTextField2.getText();
	}
	
	private JButton button_cancel;
	private JButton button_ok;	
	private JLabel jLabel1;
	private JLabel jLabel2;
	private JTextField jTextField1;
	private JTextField jTextField2;
}
