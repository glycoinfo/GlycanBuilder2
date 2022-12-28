package org.eurocarbdb.application.glycanbuilder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.pushingpixels.flamingo.api.ribbon.JRibbonBand;
import org.pushingpixels.flamingo.api.ribbon.resize.CoreRibbonResizePolicies;
import org.pushingpixels.flamingo.api.ribbon.resize.IconRibbonBandResizePolicy;
import org.pushingpixels.flamingo.api.ribbon.resize.RibbonBandResizePolicy;

public class ThemeManager {

	// 20221130 S.TSUCHIYA, investigating the error about java.io.FileNotFoundException
	/*
	static final Logger log = Logger.getLogger( ThemeManager.class );
	static {
		log.setLevel(Level.ALL);	}

	 */
	
	public static ThemeManager defaultManager;
	
	protected List<ICON_PATH> iconPaths;
	protected Class clazz;
	
	public static boolean lookupNoneCached=true;

	/**
	 * Initialise a ThemeManager with an initial icon path and the associated class who's class loader it should be located with.
	 * @param _iconPath
	 * @param _defaultClazz
	 * @throws MalformedURLException
	 */
	public ThemeManager(String _iconPath, Class _defaultClazz) throws MalformedURLException {
		// 20221130 S.TSUCHIYA comment out, investigating the error about java.io.FileNotFoundException
		//log.setLevel(Level.ALL);
		iconPaths=new ArrayList<ICON_PATH>();
		clazz=_defaultClazz;
		
		if(_iconPath!=null){
			try {
				addIconPath(_iconPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Add a new icon path, to be located with the default class's class loader (clazz field) 
	 * @param _iconPath
	 * @throws IOException
	 */
	public void addIconPath(String _iconPath) throws IOException{
		addIconPath(_iconPath,clazz);
	}
	
	/**
	 * Add a new icon path, to be located with the given class's class loader.
	 * @param _iconPath
	 * @param _defaultClazz
	 * @throws IOException
	 */
	public void addIconPath(String _iconPath, Class _defaultClazz) throws IOException{
		ICON_PATH iconPath=new ICON_PATH(_iconPath, _defaultClazz,this);
		iconPaths.add(iconPath);
	}
	
	
	
	/**
	 * Get a EurocarbResizableIcon for the given icon enum and icon size enum (size ignored as required)
	 * @param icon
	 * @param iconSize
	 * @return
	 */
	public EurocarbResizableIcon getResizableIcon(STOCK_ICON icon,ICON_SIZE iconSize) {
		EurocarbResizableIcon resizableIcon=null;
		
		for(ICON_PATH iconPath:iconPaths){
			resizableIcon=iconPath.getResizableIcon(icon, iconSize);
			if(resizableIcon!=null)
				break;
		}
		
		if(resizableIcon!=null){
			return resizableIcon;
		}else{
			//System.exit(0);
			return ThemeManager.getResizableEmptyIcon(iconSize);
		}
	}
	
	/**
	 * Get a EurocarbResizableIcon by icon identifier and icon size enum.
	 * TODO: Allow for the selection of icons by preloaded path
	 * @param id (icon name without path and extension.
	 * @param iconSize
	 * @return
	 */
	public EurocarbResizableIcon getResizableIcon(String id,ICON_SIZE iconSize) {
		EurocarbResizableIcon resizableIcon=null;
		
		for(ICON_PATH iconPath:iconPaths){
			resizableIcon=iconPath.getResizableIcon(id, iconSize);
			if(resizableIcon!=null)
				break;
		}
		
		if(resizableIcon!=null){
			return resizableIcon;
		}else{
			//if(!id.contains("changeredend") && !id.contains("massoptstruct"))
			//	System.exit(0);
			return ThemeManager.getResizableEmptyIcon(iconSize);
		}
	}

	/**
	 * Get a EurocarbResizableIcon, with a blank icon of the given icon size enum.
	 * @param iconSize
	 * @return
	 */
	public static EurocarbResizableIcon getResizableEmptyIcon(ICON_SIZE iconSize) {
		EurocarbResizableIcon eurocarbIcon = new EurocarbResizableIcon();
		eurocarbIcon.setResizableIcon(new ImageResizableIconReducedMem(getEmptyIcon(iconSize).getImage(), iconSize.getSize(), iconSize.getSize()));
		eurocarbIcon.setIconProperties(null);

		return eurocarbIcon;
	}
	
	/**
	 * Get an ImageIcon object, matching the idenitifer and icon size enum.
	 * @param id (minus path and extension)
	 * @param iconSize
	 * @return
	 * @throws IOException
	 */
	public ImageIcon getImageIcon(String id, ICON_SIZE iconSize) throws IOException{
		EurocarbResizableIcon iconR=this.getResizableIcon(id, iconSize);

		BufferedImage image=ImageIO.read(iconR.getIconProperties().imgURL);
		return new ImageIcon(ICON_PATH.scaleImage(image, iconSize.getSize(),iconSize.getSize()));
	}

	/**
	 * Get an ImageIcon object, matching the idenitifer and icon size enum.
	 * @param id (minus path and extension)
	 * @param iconSize
	 * @return
	 * @throws IOException
	 */
	public ImageIcon getImageIcon(String id, int size) {
		EurocarbResizableIcon iconR=this.getResizableIcon(id, ICON_SIZE.L3);

		BufferedImage image;
		try {
			image = ImageIO.read(iconR.getIconProperties().imgURL);
			return new ImageIcon(ICON_PATH.scaleImage(image, size,size));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Get an ImageIcon object, matching the idenitifer and icon size enum.
	 * @param id (minus path and extension)
	 * @param iconSize
	 * @return
	 * @throws IOException
	 */
	public ImageIcon getImageIcon(String id){
		EurocarbResizableIcon iconR=this.getResizableIcon(id, ICON_SIZE.L3);

		return iconR.imageIcon;
	}
	

	
	/**
	 * Get an ImageIcon object, with a blank image of the given icon enum size.
	 * @param iconSize
	 * @return
	 */
	public static ImageIcon getEmptyIcon(ICON_SIZE iconSize) {
		if(iconSize==null){
			iconSize=ICON_SIZE.L2;
		}
		return new ImageIcon(new BufferedImage(iconSize.getSize(), iconSize.getSize(), BufferedImage.TYPE_INT_ARGB));
	}
	

	/**
	 * Convenience method to set a good candidate for the default ResizePolicy of a JRibbonBand
	 * @param band
	 */
	public static void setDefaultResizePolicy(JRibbonBand band){
		ArrayList<RibbonBandResizePolicy> resizePolicies = new ArrayList<RibbonBandResizePolicy>();
		resizePolicies.add(new CoreRibbonResizePolicies.Mirror(band
				.getControlPanel()));
		resizePolicies.add(new CoreRibbonResizePolicies.Mid2Low(band
				.getControlPanel()));
		resizePolicies.add(new IconRibbonBandResizePolicy(band
				.getControlPanel()));
		band.setResizePolicies(resizePolicies);
	}
}