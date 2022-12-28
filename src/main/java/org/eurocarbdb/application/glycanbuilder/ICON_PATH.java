/**
 * 
 */
package org.eurocarbdb.application.glycanbuilder;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.tools.ant.launch.Locator;
import org.eurocarbdb.application.glycanbuilder.util.IconProperties;
import org.pushingpixels.flamingo.api.common.icon.ImageWrapperResizableIcon;

public class ICON_PATH {

	// 20221130 S.TSUCHIYA, investigating the error about java.io.FileNotFoundException
	//static final Logger log = Logger.getLogger(ThemeManager.class);
	static final ICON_SIZE iconSizes[] = ICON_SIZE.values();
	/*
	static {
		log.setLevel(Level.WARN);
	}
	 */

	protected URL iconPath;
	protected Class clazz;
	protected ThemeManager themeManager;
	protected HashMap<STOCK_ICON,String> stockIconToLocalName=new HashMap<STOCK_ICON,String>();
	protected HashMap<String,HashMap<Integer,HashSet<EXTENSION>>> nameToSizeToExtension=new HashMap<String,HashMap<Integer,HashSet<EXTENSION>>>();
	protected EXTENSION defaultExtension=EXTENSION.PNG;
	protected boolean searchAllExtensions=true;
	protected boolean searchAllSizes=true;
	protected HashMap<ICON_SIZE,Integer> iconSizeToInteger=new HashMap<ICON_SIZE,Integer>(); 
	
	public enum EXTENSION{
		PNG("png"),GIF("gif"),SVG("svg"),JPG("jpg");
		
		public String toString;
		
		EXTENSION(String _toString){
			toString=_toString;
		}
		
		public String toString(){
			return toString;
		}
		
		public static EXTENSION getExtension(String extension){
			if(extension.equals("jpg")){
				return JPG;
			}else if(extension.equals("gif")){
				return GIF;
			}else if(extension.equals("svg")){
				return SVG;
			}else if(extension.equals("png")){
				return PNG;
			}else{
				return null;
			}
		}
	}
	
	public ICON_PATH(String _iconPath, Class _clazz, ThemeManager _themeManager)
			throws IOException {
		// 20221130 S.TSUCHIYA comment out, investigating the error about java.io.FileNotFoundException
		//log.setLevel(Level.WARN);
		clazz = _clazz;
		themeManager = _themeManager;
		init(_iconPath);
	}

	public ICON_PATH(String _iconPath, Class _clazz) throws IOException {
		// 20221130 S.TSUCHIYA comment out, investigating the error about java.io.FileNotFoundException
		//log.setLevel(Level.WARN);
		clazz = _clazz;
		init(_iconPath);
	}

	private void init(String _iconPath) throws IOException {
		setIconPath(_iconPath);
		parseThemeFile();
		parseCacheFile();
	}

	public void setIconPath(String iconPath) throws MalformedURLException {
		// 20221130 S.TSUCHIYA comment out, investigating the error about java.io.FileNotFoundException
		//ThemeManager.log.setLevel(Level.INFO);

		URL url;
		if (clazz.getResource(iconPath) == null) {
			String urlString = Locator.getClassSource(clazz).getParent()
					+ iconPath;

			if (urlString.contains("!")) {
				urlString = "jar:" + urlString.replaceAll("\\\\", "/");
			} else {
				urlString = "file:" + urlString;
			}

			url = new URL(urlString);
		} else {
			url = clazz.getResource(iconPath);
		}

		this.iconPath = url;

		// 20221130 S.TSUCHIYA comment out, investigating the error about java.io.FileNotFoundException
		//ThemeManager.log.setLevel(Level.WARN);
	}
	

	public void parseCacheFile(String file) throws IOException {
		//System.err.println("parse1");
		try {
			URL url = new URL(file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			String line = null;

			HashSet<Integer> iconSizeSet=new HashSet<Integer>();
			while ((line = reader.readLine()) != null) {
				String[] cols = line.split("~");
				String name=cols[2];
				EXTENSION extension=EXTENSION.getExtension(cols[1]);
			
				int size;
				if(cols[0].equals("DEFAULT")){
					size=0;
				}else{
					size=Integer.parseInt(cols[0]);
				}
				
				if(!nameToSizeToExtension.containsKey(name)){
					nameToSizeToExtension.put(name,new HashMap<Integer, HashSet<EXTENSION>>());
				}
				
				if(!nameToSizeToExtension.get(name).containsKey(size)){
					nameToSizeToExtension.get(name).put(size, new HashSet<EXTENSION>());
				}

				nameToSizeToExtension.get(name).get(size).add(extension);
				if(size!=0)
					iconSizeSet.add(size);
			}
			
			Integer []uniqueIconSizes=iconSizeSet.toArray(new Integer[1]);
			Arrays.sort(uniqueIconSizes);
			for(int i=0;i<uniqueIconSizes.length;i++){
				if(i < iconSizes.length){
					iconSizeToInteger.put(iconSizes[i], uniqueIconSizes[i]);
				}	
			}
			iconSizeToInteger.put(ICON_SIZE.L0, 0);
		} catch (FileNotFoundException fex) {
			// 20221130 S.TSUCHIYA comment out, investigating the error about java.io.FileNotFoundException
			//log.warn("ICON_PATH[parseCacheFile]: Missing theme cache file "
			//		+ file + " for path " + this.iconPath);
		}
		//System.err.println("parse2");
	}

	public void parseCacheFile() throws IOException {
		parseCacheFile(this.iconPath.getProtocol() + ":" + this.iconPath.getPath() + "/" + "theme.cache");
	}

	public void parseThemeFile(String file) throws IOException {
		Properties properties = new Properties();
		try {
			URL url = new URL(file);
			properties.load(url.openStream());
			for (STOCK_ICON icon : STOCK_ICON.values()) {
				if (properties.get(icon.getIdentifier()) != null) {
					this.stockIconToLocalName.put(icon, (String) properties.get(icon.getIdentifier()));
				}
			}
		} catch (FileNotFoundException fex) {
			// 20221130 S.TSUCHIYA comment out, investigating the error about java.io.FileNotFoundException
			//log.warn("ICON_PATH[parseThemeFile]: Missing theme file " + file + " for path " + this.iconPath);
		}
	}
	
	public void parseThemeFile() throws IOException {
		parseThemeFile(this.iconPath.getProtocol() + ":" + this.iconPath.getPath() + "/" + "theme.properties");
	}
	
	//GET RESIZABLE ICON
	public EurocarbResizableIcon getResizableIcon(STOCK_ICON icon,ICON_SIZE iconSize,EXTENSION extension) {
		if(stockIconToLocalName.containsKey(icon)){
			return getResizableIcon(stockIconToLocalName.get(icon), iconSize, extension);
		}else{
			return getResizableIcon(icon.getIdentifier(), iconSize, extension);
		}
	}

	public EurocarbResizableIcon getResizableIcon(STOCK_ICON icon,ICON_SIZE iconSize) {
		return getResizableIcon(icon,iconSize,defaultExtension);
	}
	
	public EurocarbResizableIcon getResizableIcon(String id, ICON_SIZE iconSize) {
		return getResizableIcon(id,iconSize,defaultExtension);
	}

	public EurocarbResizableIcon getResizableIcon(String id, ICON_SIZE iconSize,EXTENSION extension) {
		List<EXTENSION> extensions=new ArrayList<EXTENSION>();
		extensions.add(extension);
		
		if(searchAllExtensions){
			for(EXTENSION altExtension:extensions){
				if(altExtension!=extension)
					extensions.add(extension);
			}
		}
		
		List<ICON_SIZE> sizes=new ArrayList<ICON_SIZE>();
		sizes.add(iconSize);
		sizes.add(ICON_SIZE.L0);
		
		if(searchAllSizes){
			for(ICON_SIZE size:iconSizes){
				if(size!=iconSize && size !=iconSize.L0)
					sizes.add(size);
			}
		}
		
		EXTENSION finalExt=null;
		ICON_SIZE finalIconSize=null;
		
		for(EXTENSION ext: extensions){
			for(ICON_SIZE size: sizes){
				if(isIcon(id,size,ext)){
					finalExt=ext;
					finalIconSize=size;
					break;
				}
			}
			if(finalExt!=null)
				break;
		}
		
		if(finalExt!=null){
			try {
				URL iconUrl=getIconUrl(id,finalIconSize,finalExt);
				IconProperties iconProperties=new IconProperties(iconUrl,true);
				iconProperties.id=id;
				
				EurocarbResizableIcon eurocarbIcon = new EurocarbResizableIcon();
				eurocarbIcon.setResizableIcon(ImageResizableIconReducedMem.getIcon(iconProperties.imgURL, new Dimension(iconSize.getSize(),iconSize.getSize())));
				eurocarbIcon.setIconProperties(iconProperties);
				eurocarbIcon.setThemeManager(this.themeManager);
				
				ImageIcon imageIcon;
				try {
					BufferedImage src = ImageIO.read(iconProperties.imgURL);
					imageIcon=new ImageIcon(src);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

					imageIcon=ThemeManager.getEmptyIcon(iconSize);
				}
				
				eurocarbIcon.imageIcon=imageIcon;
				
				return eurocarbIcon;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	//GET RESIZABLE ICON
	
	//GET IMAGE_ICON
	public ImageIcon getImageIcon(STOCK_ICON icon,ICON_SIZE iconSize,EXTENSION extension) {
		if(stockIconToLocalName.containsKey(icon)){
			return getImageIcon(stockIconToLocalName.get(icon), iconSize, extension);
		}else{
			return getImageIcon(icon.getIdentifier(), iconSize, extension);
		}
	}
	
	public ImageIcon getImageIcon(STOCK_ICON icon,ICON_SIZE iconSize) {
		return getImageIcon(icon,iconSize,defaultExtension);
	}
	
	public ImageIcon getImageIcon(String id, ICON_SIZE iconSize) {
		return getImageIcon(id,iconSize,defaultExtension);
	}

	public ImageIcon getImageIcon(String id, ICON_SIZE iconSize,EXTENSION extension) {
		return getResizableIcon(id, iconSize,extension).imageIcon;
	}
	//GET IMAGE_ICON
	
	public URL getIconUrl(String id, ICON_SIZE iconSize,EXTENSION extension) throws MalformedURLException{
		String url=iconPath.getProtocol()+":"+iconPath.getPath();
		if(iconSize!=ICON_SIZE.L0){
			url=url.concat("/"+iconSize.getSize()+"x"+iconSize.getSize());
		}
		url=url.concat("/"+id+"."+extension);

		return new URL(url);
	}
	
	public boolean isIcon(String id, ICON_SIZE iconSize,EXTENSION extension){
		if(!iconSizeToInteger.containsKey(iconSize)){
			return false;
		}else{
			Integer size=iconSizeToInteger.get(iconSize);
			if(nameToSizeToExtension.containsKey(id) &&
					nameToSizeToExtension.get(id).containsKey(size) &&
						nameToSizeToExtension.get(id).get(size).contains(extension)){
				return true;
			}else{
				return false;
			}
		}
	}

	

	// GETTERS AND SETTERS

	public ThemeManager getThemeManager() {
		return themeManager;
	}

	public void setThemeManager(ThemeManager themeManager) {
		this.themeManager = themeManager;
	}

	/**
	 * Copied from here
	 * http://answers.yahoo.com/question/index?qid=20080327223212AAz04yG
	 * 
	 * @param src
	 * @param toX
	 * @param toY
	 * @return
	 */
	public static BufferedImage scaleImage(BufferedImage src, int toX, int toY) {
		AffineTransform trans = AffineTransform.getScaleInstance((double) toX
				/ src.getWidth(), (double) toY / src.getHeight());

		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();

		int transparency = src.getColorModel().getTransparency();
		BufferedImage dest = gc.createCompatibleImage(toX, toY, transparency);
		Graphics2D g = dest.createGraphics();
		g.drawRenderedImage(src, trans);
		g.dispose();

		return dest;
	}
}