package org.eurocarbdb.application.glycanbuilder.util;

import java.awt.Image;

import org.eurocarbdb.application.glycanbuilder.HasActionProperty;
import org.pushingpixels.flamingo.api.common.JCommandButton;
import org.pushingpixels.flamingo.api.common.JCommandToggleButton;
import org.pushingpixels.flamingo.api.common.icon.ResizableIcon;


public class JCommandToggleButtonAction extends JCommandToggleButton implements HasActionProperty{
	public JCommandToggleButtonAction(ResizableIcon icon) {
		super(icon);
		// TODO Auto-generated constructor stub
	}
	
	public JCommandToggleButtonAction(String alt,ResizableIcon icon) {
		super(alt,icon);
		// TODO Auto-generated constructor stub
	}
	
	

	public JCommandToggleButtonAction(String label) {
		// TODO Auto-generated constructor stub
		super(label);
	}




	protected String actionCommand;

	public String getActionCommand() {
		return actionCommand;
	}

	public void setActionCommand(String actionCommand) {
		this.actionCommand = actionCommand;
	}
}
