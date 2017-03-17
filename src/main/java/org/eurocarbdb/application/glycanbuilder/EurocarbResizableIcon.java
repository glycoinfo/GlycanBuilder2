package org.eurocarbdb.application.glycanbuilder;

import javax.swing.ImageIcon;

import org.eurocarbdb.application.glycanbuilder.util.IconProperties;
import org.pushingpixels.flamingo.api.common.icon.ImageWrapperResizableIcon;
import org.pushingpixels.flamingo.api.common.icon.ResizableIcon;

public class EurocarbResizableIcon {
	public ResizableIcon resizableIcon;
	public ICON_PATH iconPath;
	public ImageIcon imageIcon;
	
	public ICON_PATH getIconPath() {
		return iconPath;
	}
	public void setIconPath(ICON_PATH iconPath) {
		this.iconPath = iconPath;
	}
	public EurocarbResizableIcon(ThemeManager themeManager2, IconProperties pro,
			ResizableIcon icon) {
		this.themeManager=themeManager2;
		this.iconProperties=pro;
		this.resizableIcon=icon;
	}
	public EurocarbResizableIcon() {
		// TODO Auto-generated constructor stub
	}
	public ThemeManager getThemeManager() {
		return themeManager;
	}
	public void setThemeManager(ThemeManager themeManager) {
		this.themeManager = themeManager;
	}
	public IconProperties iconProperties;
	public IconProperties getIconProperties() {
		return iconProperties;
	}
	public void setIconProperties(IconProperties iconProperties) {
		this.iconProperties = iconProperties;
	}
	public ThemeManager themeManager;
	
	public ResizableIcon getResizableIcon() {
		return resizableIcon;
	}
	public void setResizableIcon(ResizableIcon resizableIcon) {
		this.resizableIcon = resizableIcon;
	}
	
	
}
