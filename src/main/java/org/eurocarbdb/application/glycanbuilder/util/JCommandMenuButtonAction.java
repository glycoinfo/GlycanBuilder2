package org.eurocarbdb.application.glycanbuilder.util;

import org.eurocarbdb.application.glycanbuilder.HasActionProperty;
import org.eurocarbdb.application.glycanbuilder.ICON_SIZE;
import org.eurocarbdb.application.glycanbuilder.ThemeManager;
import org.pushingpixels.flamingo.api.common.JCommandButton;
import org.pushingpixels.flamingo.api.common.JCommandMenuButton;
import org.pushingpixels.flamingo.api.common.icon.ResizableIcon;


public class JCommandMenuButtonAction extends JCommandMenuButton implements HasActionProperty{
	public JCommandMenuButtonAction(ResizableIcon icon) {
		super("null", icon);
		// TODO Auto-generated constructor stub
	}
	
	public JCommandMenuButtonAction(String alt,ResizableIcon icon) {
		super(alt,icon);
		// TODO Auto-generated constructor stub
	}
	
	

	public JCommandMenuButtonAction(String label) {
		// TODO Auto-generated constructor stub
		super(label,ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3).getResizableIcon());
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
