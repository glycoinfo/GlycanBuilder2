package org.eurocarbdb.application.glycanbuilder.util;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jdesktop.layout.GroupLayout;

public class TestDialog extends JFrame{
	
	public TestDialog(){
		JLabel lblName = new JLabel();
		lblName.setText("name");
		JTextField txtName = new JTextField();
		JButton button1 = new JButton();
		button1.setText("button1");
		JLabel lblAddress = new JLabel();
		lblAddress.setText("Address");
		JTextField txtAddress = new JTextField();
		JButton button2 = new JButton();
		button2.setText("button2");
		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);

		layout.setAutocreateGaps(true);
		layout.setAutocreateContainerGaps(true);

		layout.setHorizontalGroup(
			layout.createSequentialGroup()
			.add(layout.createParallelGroup()
				.add(lblName)
				.add(lblAddress)
			)
			.add(layout.createParallelGroup()
				.add(txtName)
				.add(txtAddress)
			)
			.add(layout.createParallelGroup()
				.add(button1)
				.add(button2)
			)
		);

		layout.setVerticalGroup(
			layout.createSequentialGroup()
			.add(layout.createParallelGroup(GroupLayout.BASELINE)
				.add(lblName)
				.add(txtName)
				.add(button1))
			.add(layout.createParallelGroup(GroupLayout.BASELINE)
				.add(lblAddress)
				.add(txtAddress)
				.add(button2)
			)
		);
	}
}
