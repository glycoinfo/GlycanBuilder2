/*
 *   EuroCarbDB, a framework for carbohydrate bioinformatics
 *
 *   Copyright (c) 2006-2009, Eurocarb project, or third-party contributors as
 *   indicated by the @author tags or express copyright attribution
 *   statements applied by the authors.  
 *
 *   This copyrighted material is made available to anyone wishing to use, modify,
 *   copy, or redistribute it subject to the terms and conditions of the GNU
 *   Lesser General Public License, as published by the Free Software Foundation.
 *   A copy of this license accompanies this distribution in the file LICENSE.txt.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *   or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 *   for more details.
 *
 *   Last commit: $Rev$ by $Author$ on $Date::             $  
 */

package org.eurocarbdb.application.glycanbuilder;

import java.io.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.*;

import org.eurocarbdb.application.glycanbuilder.BaseDocument.DocumentChangeEvent;
import org.eurocarbdb.application.glycanbuilder.BaseDocument.DocumentChangeListener;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParserFactory;
import org.eurocarbdb.application.glycanbuilder.dataset.CoreDictionary;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRenderer;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.util.ActionManager;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.eurocarbdb.application.glycanbuilder.util.MouseUtils;

import java.applet.Applet;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.print.*;
import java.util.*;
import java.util.Map.Entry;
import java.text.*;
import java.net.*;

//import netscape.javascript.*;

/**
 * Implement an applet in which a {@link GlycanCanvas} is inserted. Provides
 * menus and toolbars for creating and editing a set of glycan structures.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class GlycanBuilderApplet extends JApplet implements ActionListener,
		MouseListener {

	private static final long serialVersionUID = 0L;

	// singletons
	protected BuilderWorkspace theWorkspace;
	protected GlycanDocument theDoc;
	protected ActionManager theActionManager;

	// graphical objects
	protected JMenuBar theMenuBar;
	protected JToolBar theToolBarFile;
	protected JPanel theToolBarPanel;
	protected GlycanCanvas theCanvas;

	private ThemeManager theThemeManager;

	// JS listeners
	// protected HashSet<String> js_listeners = new HashSet<String>();

	private static final int MOD_MASK = MouseEvent.CTRL_MASK
			| MouseEvent.SHIFT_MASK | MouseEvent.ALT_MASK
			| MouseEvent.META_MASK | MouseEvent.ALT_GRAPH_MASK;

	// -------------------

	/**
	 * Default Constructor
	 */
	public GlycanBuilderApplet() {

	}

	public void paint(Graphics g) {

		super.paint(g);

		// Draw a 2-pixel border
		g.setColor(Color.black);

		int width = getSize().width; // Width of the applet.
		int height = getSize().height; // Height of the applet.
		g.drawRect(0, 0, width - 1, height - 1);
		g.drawRect(1, 1, width - 3, height - 3);
	}

	public Insets getInsets() {
		return new Insets(2, 2, 2, 2);
	}

	public void init() {
		super.init();
		try {
			theThemeManager = new ThemeManager(null, this.getClass());
			try {
				theThemeManager.addIconPath("/icons/glycan_builder", this.getClass());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				theThemeManager.addIconPath("/icons/crystal_project", this.getClass());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			FileUtils.themeManager = theThemeManager;
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ThemeManager.lookupNoneCached = false;

		LogUtils.setGraphicalReport(true);
		
		String residueTypesFile=getParameter("residueTypesFile");
		String terminalTypesFile=getParameter("terminalTypesFile");
		String coreTypesFile=getParameter("coreTypesFile");
		String crossRingFragmentTypesFile=getParameter("crossRingFragmentTypesFile");
		String SkeletonCodeDictionaryFile=getParameter("SkeletonCodeDictionaryFile");
		
		// create the default workspace
		theWorkspace = new BuilderWorkspace(null, false,new GlycanRendererAWT(),residueTypesFile,terminalTypesFile,coreTypesFile,crossRingFragmentTypesFile);

		// create singletons
		theDoc = theWorkspace.getStructures();
		theActionManager = new ActionManager();

		// initialize the action set
		createActions();

		// set parameters
		if (getParameter("NOTATION") != null)
			theWorkspace.setNotation(getParameter("NOTATION"));
		if (getParameter("DISPLAY") != null)
			theWorkspace.setDisplay(getParameter("DISPLAY"));
		
		// create interface
		try {
			createUI();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// set document
		if (getParameter("DOCUMENT") != null)
			setDocument(getParameter("DOCUMENT"));
	}

	public void start() {
		super.start();
	}

	public void stop() {
		super.stop();
	}

	public void destroy() {
		super.destroy();
	}

	protected void createUI() throws MalformedURLException {

		theCanvas = new GlycanCanvas(null, theWorkspace, theThemeManager, false);

		String allowUncertainTerminalsString=getParameter("allowUncertainTerminals");
		if(allowUncertainTerminalsString!=null){
			theCanvas.setAllowUncertainTerminals(Boolean.parseBoolean(allowUncertainTerminalsString));
		}
		
		String allowRepeatingUnitsString=getParameter("allowRepeatingUnits");
		if(allowRepeatingUnitsString!=null){
			theCanvas.setAllowRepeatingUnits(Boolean.parseBoolean(allowRepeatingUnitsString));
		}
		
		String allowMultipleStructuresString=getParameter("allowMultipleStructures");
		if(allowMultipleStructuresString!=null){
			theCanvas.setAllowMultipleStructures(Boolean.parseBoolean(allowMultipleStructuresString));
		}
		
		// set the layout
		getContentPane().setLayout(new BorderLayout());

		// set the toolbars
		UIManager.getDefaults().put("ToolTip.hideAccelerator", Boolean.TRUE);
		theToolBarPanel = new JPanel(new BorderLayout());

		theToolBarFile = createToolBarFile();

		JPanel northTbPanel = new JPanel();
		northTbPanel.setLayout(new BoxLayout(northTbPanel, BoxLayout.X_AXIS));
		northTbPanel.add(theToolBarFile);
		northTbPanel.add(theCanvas.getToolBarDocument());

		theToolBarPanel.add(northTbPanel, BorderLayout.NORTH);
		theToolBarPanel.add(theCanvas.getToolBarStructure(),
				BorderLayout.CENTER);
		theToolBarPanel.add(theCanvas.getToolBarProperties(),
				BorderLayout.SOUTH);
		getContentPane().add(theToolBarPanel, BorderLayout.NORTH);

		// set the MenuBar
		theMenuBar = createMenuBar();
		setJMenuBar(theMenuBar);

		// set the canvas
		JScrollPane sp = new JScrollPane(theCanvas);
		theCanvas.setScrollPane(sp);
		getContentPane().add(sp, BorderLayout.CENTER);

		// initialize document
		onNew();
	}

	/**
	 * Return the workspace object containing all documents and options
	 */
	public BuilderWorkspace getWorkspace() {
		return theWorkspace;
	}

	/**
	 * Return the component used to display the glycan structures
	 */
	public GlycanRenderer getGlycanRenderer() {
		return theWorkspace.getGlycanRenderer();
	}

	/**
	 * Return the component used to create and edit the structures
	 */
	public GlycanCanvas getCanvas() {
		return theCanvas;
	}

	private void createActions() {

		// cores
		theActionManager.add("empty", ThemeManager
				.getResizableEmptyIcon(ICON_SIZE.L3), "Empty", -1, "", this);
		for (CoreType t : CoreDictionary.getCores())
			theActionManager.add("new=" + t.getName(), ThemeManager
					.getResizableEmptyIcon(ICON_SIZE.L3), t.getDescription(),
					-1, "", this);

		// file
		theActionManager.add("new", FileUtils.themeManager.getImageIcon("new"),
				"New", KeyEvent.VK_N, "ctrl N", this);
		theActionManager.add("print", FileUtils.themeManager
				.getImageIcon("print"), "Print...", KeyEvent.VK_P, "ctrl P",
				this);

		// help
		theActionManager.add("about", FileUtils.themeManager
				.getImageIcon("about"), "About", KeyEvent.VK_B, "", this);
	}

	private JMenu createNewDocumentMenu() {

		JMenu new_menu = new JMenu("New");
		new_menu.setIcon(FileUtils.themeManager.getImageIcon("new"));

		new_menu.add(theActionManager.get("empty"));
		for (Iterator<String> s = CoreDictionary.getSuperclasses().iterator(); s
				.hasNext();) {
			String superclass = s.next();

			JMenu class_menu = new JMenu(superclass);
			for (Iterator<CoreType> i = CoreDictionary.getCores(superclass)
					.iterator(); i.hasNext();) {
				CoreType t = i.next();
				class_menu.add(theActionManager.get("new=" + t.getName()));
			}
			if (class_menu.getItemCount() > 0)
				new_menu.add(class_menu);
		}

		return new_menu;
	}

	private JMenuBar createMenuBar() {

		JMenuBar menubar = new JMenuBar();

		// file menu
		JMenu file_menu = new JMenu("File");
		file_menu.setMnemonic(KeyEvent.VK_F);
		// file_menu.add(theActionManager.get("new"));
		file_menu.add(createNewDocumentMenu());
		file_menu.addSeparator();
		file_menu.add(theActionManager.get("print"));
		menubar.add(file_menu);

		// canvas menus
		menubar.add(theCanvas.getEditMenu());
		menubar.add(theCanvas.getStructureMenu());
		menubar.add(theCanvas.getViewMenu());

		// help menu
		JMenu help_menu = new JMenu("Help");
		help_menu.setMnemonic(KeyEvent.VK_H);
		help_menu.add(theActionManager.get("about"));
		menubar.add(help_menu);

		return menubar;
	}

	protected JPopupMenu createPopupMenu() {
		return theCanvas.createPopupMenu();
	}

	private JToolBar createToolBarFile() {

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);

		toolbar.add(theActionManager.get("new"));

		toolbar.addSeparator();

		toolbar.add(theActionManager.get("print"));

		return toolbar;
	}

	private String askName(String what) {
		return JOptionPane.showInputDialog(this, "Insert " + what + " name:",
				"", JOptionPane.QUESTION_MESSAGE);
	}

	// actions

	/**
	 * Initialize the structure document
	 */
	public void onNew() {
		// init document
		theDoc.init();
	}

	/**
	 * Initialize the structure document with a core motif
	 * 
	 * @param name
	 *            the identifier of the core motif
	 * @see CoreDictionary
	 */
	public void onNew(String name) {
		try {
			// init document
			theDoc.init();
			if (name != null && name.length() > 0) {
				theDoc.addStructure(CoreDictionary.newCore(name));
				theDoc.setChanged(false);
			}
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	/**
	 * Print the content of the {@link GlycanCanvas}
	 */
	public void onPrint() {
		try {
			PrinterJob pj = theWorkspace.getPrinterJob();
			if (pj != null) {
				pj.setPrintable(theCanvas);
				if (pj.printDialog())
					theCanvas.print(pj);
			}
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	/**
	 * Show the about menu
	 */
	public void onAbout() {

		try {
			JDialog dlg = new JDialog((JFrame) null, "About GlycanBuilder",
					true);
			JEditorPane html = new JEditorPane(GlycanBuilderApplet.class
					.getResource("/html/about_builder.html"));
			html.setEditable(false);
			html.setBorder(new EmptyBorder(20, 20, 20, 20));

			dlg.add(html);
			dlg.setSize(320, 320);
			dlg.setResizable(false);
			dlg.setLocationRelativeTo(this);

			dlg.setVisible(true);
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		if (MouseUtils.isPopupTrigger(e)) {
			theCanvas.enforceSelection(e.getPoint());
			createPopupMenu().show(theCanvas, e.getX(), e.getY());
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (MouseUtils.isPopupTrigger(e)) {
			theCanvas.enforceSelection(e.getPoint());
			createPopupMenu().show(theCanvas, e.getX(), e.getY());
		}
	}

	public void mouseClicked(MouseEvent e) {
	}

	// listeners

	/*
	 * public void addJSListener(String call_back) { if( call_back!=null )
	 * js_listeners.add(call_back); }
	 * 
	 * public void removeJSListener(String call_back) { if( call_back!=null )
	 * js_listeners.remove(call_back); }
	 * 
	 * public void fireJSListeners() { if( js_listeners.size()>0 ) { JSObject
	 * win = JSObject.getWindow(this); for( String l : js_listeners )
	 * win.call(l, null); } }
	 */

	// 

	public boolean isActive() {
		return super.isActive();
	}

	/**
	 * Return the structures encoded into a string in the internal format
	 */
	public String getDocument() {
		if(theDoc.getNoStructures()==0){
			return "";
		}
		
		return theDoc.toString();
	}
	
	public String getDocument(String format){
		if(theDoc.getNoStructures()==0){
			return "";
		}
		
		return theDoc.toString(format);
	}
	
	public String getCurrentCoordinates(String format){
		return theDoc.toStringWithCoordinates(format, theCanvas.theBBoxManager);
	}
	
	public void setDocument(String src, String format) {
		try {
			theDoc.init();
			theDoc.fromString(src, format);
			theDoc.setChanged(false);
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	/**
	 * Setthe structures from a string encoded in the internal format
	 */
	public void setDocument(String src) {
		setDocument(src, "gws");
	}
	
	public void setDocumentFromUrl(String url, String format){
		try {
			theDoc.init();
			
			theDoc.fromURL(url, format);
			
			theDoc.setChanged(false);
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	/**
	 * Return the flag that specify if the linkage information should be
	 * displayed
	 * 
	 * @see GraphicOptions#SHOW_INFO
	 */
	public boolean getShowInfo() {
		return theWorkspace.getGraphicOptions().SHOW_INFO;
	}

	/**
	 * Return the flag that specify if the mass options should be displayed in
	 * the canvas
	 * 
	 * @see GraphicOptions#SHOW_MASSES_CANVAS
	 */
	public boolean getShowMassesCanvas() {
		return theWorkspace.getGraphicOptions().SHOW_MASSES_CANVAS;
	}

	/**
	 * Return the flag that specify if the mass options should be displayed when
	 * exporting
	 * 
	 * @see GraphicOptions#SHOW_MASSES
	 */
	public boolean getShowMasses() {
		return theWorkspace.getGraphicOptions().SHOW_MASSES;
	}

	/**
	 * Specify if the reducing end marker should be displayed in the canvas
	 * 
	 * @see GraphicOptions#SHOW_REDEND_CANVAS
	 */
	public void setShowRedendCanvas(boolean f) {
		theCanvas.setShowRedendCanvas(f);
	}

	/**
	 * Return the flag that specify if the reducing end marker should be
	 * displayed in the canvas
	 * 
	 * @see GraphicOptions#SHOW_REDEND_CANVAS
	 */
	public boolean getShowRedendCanvas() {
		return theWorkspace.getGraphicOptions().SHOW_REDEND_CANVAS;
	}

	/**
	 * Return the flag that specify if the reducing end marker should be
	 * displayed when exporting
	 * 
	 * @see GraphicOptions#SHOW_REDEND
	 */
	public boolean getShowRedend() {
		return theWorkspace.getGraphicOptions().SHOW_REDEND;
	}

	/**
	 * Return the orientation of the structures
	 * 
	 * @see GraphicOptions#ORIENTATION
	 */
	public int getOrientation() {
		return theWorkspace.getGraphicOptions().ORIENTATION;
	}

	/**
	 * Return the cartoon notation used to display the structures
	 * 
	 * @see GraphicOptions#NOTATION
	 */
	public String getNotation() {
		return theWorkspace.getGraphicOptions().NOTATION;
	}

	/**
	 * Specifiy the cartoon notation used to display the structures
	 * 
	 * @see GlycanCanvas#setNotation
	 */
	public void setNotation(String notation) {
		theCanvas.setNotation(notation);
	}

	/**
	 * Return the graphic preset used to display the structures
	 * 
	 * @see GraphicOptions#DISPLAY
	 */
	public String getDisplay() {
		return theWorkspace.getGraphicOptions().DISPLAY;
	}

	/**
	 * Specify the graphic preset used to display the structures
	 * 
	 * @see GraphicOptions#setDisplay
	 */
	public void setDisplay(String display) {
		theCanvas.setDisplay(display);
	}

	public void actionPerformed(ActionEvent e) {

		String action = GlycanAction.getAction(e);
		String param = GlycanAction.getParam(e);

		if (action.equals("empty"))
			onNew();
		else if (action.equals("new"))
			onNew(param);
		else if (action.equals("print"))
			onPrint();

		else if (action.equals("about"))
			onAbout();
	}
	
	public Set<String> getImportFormats(){
		return GlycanParserFactory.getImportFormats().keySet();
	}
	
	public Set<String> getExportFormats(){
		return GlycanDocument.getExportFormats().keySet();
	}
}