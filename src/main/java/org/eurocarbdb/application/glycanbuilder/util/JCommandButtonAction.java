package org.eurocarbdb.application.glycanbuilder.util;

import org.eurocarbdb.application.glycanbuilder.HasActionProperty;
import org.pushingpixels.flamingo.api.common.JCommandButton;
import org.pushingpixels.flamingo.api.common.icon.ResizableIcon;


public class JCommandButtonAction extends JCommandButton implements HasActionProperty{
	public JCommandButtonAction(ResizableIcon icon) {
		super(icon);
		// TODO Auto-generated constructor stub
	}
	
	public JCommandButtonAction(String alt,ResizableIcon icon) {
		super(alt,icon);
		// TODO Auto-generated constructor stub
	}
	
	

	public JCommandButtonAction(String label) {
		// TODO Auto-generated constructor stub
		super(label);
	}



	protected String actionCommand;

	public String getActionCommand() {
		return actionCommand;
	}
	
	@Override
	public void setActionCommand(String actionCommand) {
		this.actionCommand = actionCommand;
	}

}
