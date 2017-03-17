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

import org.eurocarbdb.application.glycanbuilder.util.ActionManager;
import org.eurocarbdb.application.glycanbuilder.util.MouseUtils;
import chrriis.dj.nativeswing.swtimpl.NativeInterface;

import org.eurocarbdb.application.glycanbuilder.dataset.CoreDictionary;
import org.eurocarbdb.application.glycanbuilder.fileutil.ExtensionFileFilter;
import org.eurocarbdb.application.glycanbuilder.fileutil.FileHistory;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.SVGUtils;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.util.*;
import java.net.*;


/**
   Main application class. Implement a frame in which a {@link
   GlycanCanvas} is inserted. Provides menus and toolbars for creating
   and editing a set of glycan structures, and export the results to
   files in various formats.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/


public class GlycanBuilder extends JFrame implements ActionListener, BaseDocument.DocumentChangeListener, FileHistory.Listener, MouseListener {

	private static final long serialVersionUID = 0L;    

	// singletons
	protected BuilderWorkspace theWorkspace;
	protected GlycanDocument theDoc;
	protected ActionManager theActionManager;  

	// graphical objects
	protected JMenuBar theMenuBar;
	protected JToolBar theToolBarFile;
	protected JPanel   theToolBarPanel;
	protected GlycanCanvas  theCanvas;

	// menus
	protected JMenu recent_files_menu;
	protected String last_exported_file = null;

	private Monitor halt_interactions = null;

	protected Set<ContextAwareContainer> contextAwareListeners;
	
	static{
		
	}
	
	// -------------------

	/**
       Initialize the application, the documents and the graphic
       components. 
	 * @throws MalformedURLException 
	 */

	public GlycanBuilder() throws MalformedURLException {

		ThemeManager themeManager= new ThemeManager(null, this.getClass());

		ThemeManager.lookupNoneCached = false;
		FileUtils.themeManager=themeManager;

		try {
			themeManager.addIconPath("/icons/glycan_builder", this.getClass());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			themeManager.addIconPath("/icons/crystal_project", this.getClass());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		LogUtils.setReportOwner(this);
		LogUtils.setGraphicalReport(true);       

		// create the default workspace
		theWorkspace = new BuilderWorkspace(
				new File("config.xml").getAbsolutePath(), true, new GlycanRendererAWT());
				/*FileUtils.getRootDir(),true,new GlycanRendererAWT());*/
		
		theWorkspace.setAutoSave(true);

		// create singletons 
		theDoc = theWorkspace.getStructures(); 
		theActionManager = new ActionManager();
		halt_interactions = new Monitor(this);

		// initialize the action set
		createActions();

		// set the layout
		getContentPane().setLayout(new BorderLayout());

		// create canvas
		//TODO : 
		theCanvas = new GlycanCanvas(this,theWorkspace, themeManager, false);     

		// set the toolbars
		UIManager.getDefaults().put("ToolTip.hideAccelerator",Boolean.TRUE);
		theToolBarPanel = new JPanel(new BorderLayout());

		theToolBarFile = createToolBarFile();

		JPanel northTbPanel = new JPanel();
		northTbPanel.setLayout(new BoxLayout(northTbPanel,BoxLayout.X_AXIS));
		northTbPanel.add(theToolBarFile);
		northTbPanel.add(theCanvas.getToolBarDocument());

		theToolBarPanel.add(northTbPanel, BorderLayout.NORTH);
		theToolBarPanel.add(theCanvas.getToolBarStructure(), BorderLayout.CENTER);
		theToolBarPanel.add(theCanvas.getToolBarProperties(), BorderLayout.SOUTH);
		getContentPane().add(theToolBarPanel,BorderLayout.NORTH);

		// set the MenuBar
		theMenuBar = createMenuBar();
		setJMenuBar(theMenuBar);        

		// set the canvas
		JScrollPane sp = new JScrollPane(theCanvas);
		theCanvas.setScrollPane(sp);
		getContentPane().add(sp,BorderLayout.CENTER);

		// add listeners
		theDoc.addDocumentChangeListener(this);
		theCanvas.addMouseListener( this ); 
		theWorkspace.addDocumentChangeListener(this);
		theWorkspace.getFileHistory().addHistoryChangedListener(this);

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				onExit();
			}
		});

		// setto la dimensione e la posizione della finestra
		setIconImage(FileUtils.themeManager.getImageIcon("logo").getImage());
		setSize(800, 600);        
		setLocationRelativeTo(null);

		// initialize document
		onNew(theDoc);
		theWorkspace.setChanged(false);

		updateActions();

		NativeInterface.open();

		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
	}

	/**
       Exit the application immediately with a specified error
       level. Save the configuration to file       
	 */   
	public void exit(int err_level) {    
		// save configurations
		//theWorkspace.exit(new File("config.xml").getAbsolutePath());
				//FileUtils.getRootDir() + "/config.xml");

		// clear memory
		theWorkspace.init();
		System.gc();
		System.runFinalization();

		// exit
		System.exit(err_level);
	}

	/**
       Return the workspace object containing all documents and
       options
	 */
	public BuilderWorkspace getWorkspace() {
		return theWorkspace;
	}   

	/**
       Return the component used to create and edit the structures
	 */
	public GlycanCanvas getCanvas() {
		return theCanvas;
	}    

	/**
       Halt the user interaction and display a hourglass cursor
	 */
	public void haltInteractions() {

		// display the wait cursor and block user input
		if( halt_interactions.isFree() ) {
			Component glassPane = getGlassPane();
			glassPane.addMouseListener( new MouseAdapter() { }  );
			glassPane.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
			glassPane.setVisible( true );
		}    
		halt_interactions.hold();
	}

	/**
       Restore the user interactions
	 */
	public void restoreInteractions() {        

		// restore normal user interaction
		halt_interactions.release();
		if( halt_interactions.isFree() )
			getGlassPane().setVisible(false);        
	}


	private void createActions() {

		// file
		theActionManager.add("empty",ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3),"Empty",-1,"",this);
		for(CoreType t : CoreDictionary.getCores() ) 
			theActionManager.add("new=" + t.getName(),ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3),t.getDescription(),-1,"",this);

		theActionManager.add("new",FileUtils.themeManager.getImageIcon("new"),"New",KeyEvent.VK_N, "ctrl N",this);
		theActionManager.add("open",FileUtils.themeManager.getImageIcon("open"),"Open document...",KeyEvent.VK_O, "ctrl O",this);
		theActionManager.add("openinto",FileUtils.themeManager.getImageIcon("openinto"),"Open additional document...",KeyEvent.VK_I, "ctrl I",this);
		theActionManager.add("save",FileUtils.themeManager.getImageIcon("save"),"Save",KeyEvent.VK_S, "ctrl S",this);
		theActionManager.add("saveas",FileUtils.themeManager.getImageIcon("saveas"),"Save as...",KeyEvent.VK_A, "shift ctrl S",this);
		theActionManager.add("print",FileUtils.themeManager.getImageIcon("print"),"Print...",KeyEvent.VK_P, "ctrl P",this);
		theActionManager.add("quit",FileUtils.themeManager.getImageIcon("quit"),"Quit",KeyEvent.VK_Q, "ctrl Q",this);
		
		// import/export
		for(java.util.Map.Entry<String,String> e : GlycanDocument.getImportFormats().entrySet() )
			theActionManager.add("import=" + e.getKey(),ThemeManager.getEmptyIcon(null),"Import from " + e.getValue() + "...",-1, "",this);
		for(java.util.Map.Entry<String,String> e : GlycanDocument.getExportFormats().entrySet() )
			theActionManager.add("export=" + e.getKey(),ThemeManager.getEmptyIcon(null),"Export to " + e.getValue() + "...",-1, "",this);
		for(java.util.Map.Entry<String,String> e : SVGUtils.getExportFormats().entrySet() )
			theActionManager.add("export=" + e.getKey(),ThemeManager.getEmptyIcon(null),"Export to " + e.getValue() + "...",-1, "",this);

		// help
		theActionManager.add("about",FileUtils.themeManager.getImageIcon("about"),"About",KeyEvent.VK_B,"",this);
	}   

	private void updateActions() {
		theActionManager.get("save").setEnabled(theDoc.hasChanged());
	}    

	private void updateRecentFileMenu() {
		// update recent files menu

		recent_files_menu.removeAll();

		FileHistory theFileHistory = theWorkspace.getFileHistory();
		for( Iterator<String> i = theFileHistory.iterator(); i.hasNext(); ) {
			String file_path = i.next();
			String file_type = theFileHistory.getFileType(file_path);

			JMenuItem mi = new JMenuItem(theFileHistory.getAbbreviatedName(file_path));
			if( file_type.equals("Workspace") ) mi.setActionCommand("openall=" + file_path);
			else if( file_type.equals("Structures") ) mi.setActionCommand("openstruct=" + file_path);
			mi.addActionListener(this);
			mi.setToolTipText(file_path);

			recent_files_menu.add(mi);
		}
		if( recent_files_menu.getItemCount()==0 ) recent_files_menu.add(new JMenuItem("<empty>"));                    
	}

	private JMenu createNewDocumentMenu() {

		JMenu new_menu = new JMenu("New");
		new_menu.setMnemonic(KeyEvent.VK_N);
		new_menu.setIcon(FileUtils.themeManager.getImageIcon("new"));

		new_menu.add(theActionManager.get("empty"));
		for( String superclass : CoreDictionary.getSuperclasses() ) {
			JMenu class_menu = new JMenu(superclass);        
			for( CoreType core_type : CoreDictionary.getCores(superclass) ) 
				class_menu.add(theActionManager.get("new=" + core_type.getName()));        

			if( class_menu.getItemCount()>0 ) new_menu.add(class_menu);
		}

		return new_menu;
	}

	private JMenu createImportSequenceMenu() {
		JMenu import_menu = new JMenu("Import from sequence formats");    
		import_menu.setMnemonic(KeyEvent.VK_M);
		import_menu.setIcon(FileUtils.themeManager.getImageIcon("import"));

		for(java.util.Map.Entry<String,String> e : GlycanDocument.getImportFormats().entrySet() )
			import_menu.add(theActionManager.get("import="+e.getKey()));

		return import_menu;
	}

	private JMenu createExportSequenceMenu() {
		JMenu export_menu = new JMenu("Export to sequence formats");    
		export_menu.setMnemonic(KeyEvent.VK_X);
		export_menu.setIcon(FileUtils.themeManager.getImageIcon("export"));

		for(java.util.Map.Entry<String,String> e : GlycanDocument.getExportFormats().entrySet() )
			export_menu.add(theActionManager.get("export="+e.getKey()));

		return export_menu;
	}

	private JMenu createExportDrawingMenu() {

		JMenu export_menu = new JMenu("Export to graphical formats");    
		export_menu.setMnemonic(KeyEvent.VK_G);
		export_menu.setIcon(FileUtils.themeManager.getImageIcon("export"));

		for(java.util.Map.Entry<String,String> e : SVGUtils.getExportFormats().entrySet() )
			export_menu.add(theActionManager.get("export="+e.getKey()));

		return export_menu;
	}

	private JMenu createFileMenu() {
		recent_files_menu = new JMenu("Recent files");
		recent_files_menu.setMnemonic(KeyEvent.VK_R);
		recent_files_menu.setIcon(ThemeManager.getEmptyIcon(null));
		updateRecentFileMenu();

		JMenu file_menu = new JMenu("File");
		file_menu.setMnemonic(KeyEvent.VK_F);
		file_menu.add(createNewDocumentMenu());
		file_menu.add(theActionManager.get("open"));
		file_menu.add(theActionManager.get("openinto"));
		file_menu.add(recent_files_menu);
		file_menu.addSeparator();
		file_menu.add(theActionManager.get("save"));
		file_menu.add(theActionManager.get("saveas"));
		file_menu.addSeparator();
		file_menu.add(createImportSequenceMenu());
		file_menu.add(createExportSequenceMenu());
		file_menu.add(createExportDrawingMenu());
		file_menu.addSeparator();
		file_menu.add(theActionManager.get("print"));
		file_menu.addSeparator();
		file_menu.add(theActionManager.get("quit"));    

		return file_menu;
	}


	private JMenuBar createMenuBar() {

		JMenuBar menubar = new JMenuBar();           

		menubar.add(createFileMenu());
		menubar.add(theCanvas.getEditMenu());
		menubar.add(theCanvas.getStructureMenu());
		menubar.add(theCanvas.getViewMenu());    

		// help menu
		JMenu help_menu = new JMenu("Help");
		help_menu.setMnemonic(KeyEvent.VK_H);
		help_menu.add(theActionManager.get("about"));
		menubar.add(help_menu);    

		menubar.add(theCanvas.getDebugMenu());
		
		return menubar;
	}    

	private JPopupMenu createPopupMenu() {
		return theCanvas.createPopupMenu();
	}

	private JToolBar createToolBarFile() {

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);

		toolbar.add(theActionManager.get("new"));
		toolbar.add(theActionManager.get("open"));    
		toolbar.add(theActionManager.get("openinto"));
		toolbar.add(theActionManager.get("save"));
		toolbar.add(theActionManager.get("saveas"));
		toolbar.addSeparator();
		toolbar.add(theActionManager.get("print"));
		
		return toolbar;
	}


	private String askName(String what) {
		return JOptionPane.showInputDialog(this,"Insert " + what + " name:", "", JOptionPane.QUESTION_MESSAGE);
	}     

	//----------------------------
	// Document handling actions

	private void updateTitle() {
		String title = FileHistory.getAbbreviatedName(theDoc.getFileName()) + " - GlycanBuilder";
		if( theDoc.hasChanged() ) title = "* " + title;
		setTitle(title);
	}

	private File getLastExportedFile() {
		if( last_exported_file!=null && last_exported_file.length()>0 ) {
			return new File(last_exported_file);                
		}
		return null;
	}

	private void setLastExportedFile(String name) {
		last_exported_file = name;
	}    


	private boolean checkDocumentChanges(BaseDocument doc) {
		if( doc.hasChanged() && !doc.isEmpty() ) {
			int ret = JOptionPane.showConfirmDialog(this,"Save changes to " + doc.getName().toLowerCase() + "?", null, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if( ret == JOptionPane.CANCEL_OPTION ) return false;        
			if( ret == JOptionPane.YES_OPTION ) {
				if( !onSaveAs(doc) ) return false;
				return true;
			}
			if( ret == JOptionPane.NO_OPTION ) return true;
			return false;
		}    
		return true;
	}


	private boolean checkExisting(String filename) { 
		if( !(new File(filename)).exists() ) {
			JOptionPane.showMessageDialog(this,"The file selected is not existing.", "File not found", JOptionPane.ERROR_MESSAGE);
			theWorkspace.getFileHistory().remove(filename);      
			return false;
		}
		return true;
	}

	/**
       Initialize the selected document
       @return <code>true</code> if the operation was successful
	 */
	public boolean onNew(BaseDocument doc) {
		if( doc==null ) return false;
		if( !checkDocumentChanges(doc) ) return false;

		// init document
		doc.init();
		return true;
	}

	/**
       Initialize the structure document with a core motif
       @param name the identifier of the core motif 
       @see CoreDictionary
       @return <code>true</code> if the operation was successful
	 */
	public boolean onNew(String name) {
		try {
			if( !checkDocumentChanges(theDoc) ) return false;

			// add structure from template
			if( name!=null && name.length()>0 ) 
				return theDoc.init(CoreDictionary.getCoreType(name).getStructure());

			theDoc.init();
			return true;
		}    
		catch(Exception e) {
			LogUtils.report(e);
			return false;
		}
	}

	/*
    public boolean tryOpen(String filename, boolean merge) {

    try {
        if( !checkExisting(filename) ) 
        return false;

        // try to open one document
        if( theWorkspace.open(filename,merge,false) ) {
        theWorkspace.getFileHistory().add(filename,theWorkspace.getName());
        return true;
        }
        if( theWorkspace.getStructures().open(filename,merge,false) ) {
        theWorkspace.getFileHistory().add(filename,theWorkspace.getStructures().getName());
        return true;
        }
        throw new Exception("Unrecognized file format");    
    }
    catch(Exception e) {
        LogUtils.report(e);
        return false;
    }
    }
	 */

	private boolean onOpenDocument(Collection<BaseDocument> documents, boolean merge) {
		if( documents==null || documents.size()==0 ) return false;

		// collect file formats
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setAcceptAllFileFilterUsed(false);

		HashMap<javax.swing.filechooser.FileFilter,BaseDocument> all_ff = new HashMap<javax.swing.filechooser.FileFilter,BaseDocument>();
		for( BaseDocument doc : documents) {
			javax.swing.filechooser.FileFilter ff = doc.getAllFileFormats();    
			fileChooser.addChoosableFileFilter(ff);
			all_ff.put(ff,doc);    
		}

		// open file chooser
		fileChooser.setCurrentDirectory(theWorkspace.getFileHistory().getRecentFolder());
		if( fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION ) return false;

		// retrieve file path and document type
		String filename = fileChooser.getSelectedFile().getAbsolutePath();
		BaseDocument document = all_ff.get(fileChooser.getFileFilter());

		// open the file
		return onOpen(filename,document,merge);    
	}


	private boolean onOpen(String filename, BaseDocument doc, boolean merge) {
		if( doc==null ) return false;    

		if( filename==null ) {
			if( doc.getFileFormats().size()==0 ) return false;

			// imposto la dialog per l'apertura del file      
			JFileChooser fileChooser = new JFileChooser();
			for( javax.swing.filechooser.FileFilter ff : doc.getFileFormats() )         
				fileChooser.addChoosableFileFilter(ff);
			fileChooser.setCurrentDirectory(theWorkspace.getFileHistory().getRecentFolder());

			// visualizzo la dialog
			if( fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION ) 
				return false;

			// retrieve file path
			filename = fileChooser.getSelectedFile().getAbsolutePath();
		}

		if( !checkExisting(filename) ) return false;
		if( filename.equals(doc.getFileName()) ) {
			// ask for reload if document has changed
			if( !doc.hasChanged() ) return false;        
			int retValue = JOptionPane.showOptionDialog(this, "Reload document from the file: " + filename + "?", "Load document", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);  
			if( retValue!=JOptionPane.YES_OPTION ) return false;
		}
		else if( !checkDocumentChanges(doc) ) return false;

		// open document        
		haltInteractions();    
		if( !doc.open(filename,merge,true) ) {
			restoreInteractions();
			return false;
		}
		restoreInteractions();

		// update history
		theWorkspace.getFileHistory().add(filename,doc.getName());         
		return true;
	}

	/**
       Save the selected document to file. Use the same file if the
       document was previously saved, otherwise show a file save
       dialog
       @return <code>true</code> if the operation was successful
	 */
	public boolean onSave(BaseDocument doc) {
		return onSave(doc,true);
	}

	/**
       Save the selected document to file. Use the same file if the
       document was previously saved.
       @param ask_filename if <code>true</code> a file save dialog is
       showed in case the document was not previously saved
       @return <code>true</code> if the operation was successful
	 */
	public boolean onSave(BaseDocument doc, boolean ask_filename) {
		if( doc==null ) return false;

		// controlla se e' possibile salvare il file nella stessa posizione da cui e' stato aperto
		File cur = doc.getFile();                
		if( cur!=null && cur.canWrite() ) {
			// salvo il documento su file
			doc.save(cur.getAbsolutePath());       
			return true;
		}

		// non e' stato possibile salvare il file nella posizione da cui e' stato aperto. Richiedo il salvataggio con scelta file
		if( ask_filename ) return onSaveAs(doc);        
		return false;
	}

	/**
       Save the selected document to file. Show a file save dialog to
       select the destination
       @return <code>true</code> if the operation was successful
	 */
	public boolean onSaveAs(BaseDocument doc) {
		if( doc==null ) return false;

		// imposto la dialog per il salvataggio del file
		JFileChooser fileChooser = new JFileChooser();
		for( javax.swing.filechooser.FileFilter ff : doc.getFileFormats() )         
			fileChooser.addChoosableFileFilter(ff);
		fileChooser.setCurrentDirectory(theWorkspace.getFileHistory().getRecentFolder());

		// visualizzo la dialog
		if( fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION ) {

			// aggiunge l'estension        
			String filename = fileChooser.getSelectedFile().getAbsolutePath();
			javax.swing.filechooser.FileFilter ff = fileChooser.getFileFilter();
			if( ff!=fileChooser.getAcceptAllFileFilter() && (ff instanceof ExtensionFileFilter) ) 
				filename = FileUtils.enforceExtension(filename,((ExtensionFileFilter)ff).getDefaultExtension());

			// chiede conferma prima di sovrascrivere il file
			File file = new File(filename);                    
			if( file.exists() ) {
				int retValue = JOptionPane.showOptionDialog(this, "File exists. Overwrite file: " + filename + "?",
						"Salva documento", JOptionPane.YES_NO_CANCEL_OPTION, 
						JOptionPane.QUESTION_MESSAGE, null, null, null);  
				if( retValue!=JOptionPane.YES_OPTION ) return false;
			}            

			// salvo il documento su file
			if( !doc.save(filename) ) return false;

			// update history
			theWorkspace.getFileHistory().add(filename,doc.getName());     
			return true;
		}
		return false;
	}    

	/**
       Import structures from a file in a specified format. Show a
       file open dialog to select the origin
       @param format the encoding format
       @return <code>true</code> if the operation was successful
       @see GlycanDocument#importFrom
	 */
	public boolean onImportFrom(String format) {
		// imposto la dialog per l'apertura del file      
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(theWorkspace.getFileHistory().getRecentFolder());

		// visualizzo la dialog
		if( fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION ) return false;

		// retrieve file path
		String filename = fileChooser.getSelectedFile().getAbsolutePath();    

		//if( filename.equals(theDoc.getFileName()) )
		//return false;
		//if( !checkDocumentChanges(theDoc) ) 
		//return false;
		if( !checkExisting(filename) ) return false;

		// import structure into the document
		return theDoc.importFrom(filename,format);
	}

	/**
       Export the structures to a file in a specified format. Show a
       file open dialog to select the origin
       @return <code>true</code> if the operation was successful
       @see GlycanDocument#importFrom
       @see SVGUtils#export
	 */
	public boolean onExportTo(String format) {

		if( theDoc.getStructures().size()>1 && !theDoc.supportMultipleStructures(format) ) {
			int retValue = JOptionPane.showOptionDialog(this, "The selected format does not support multiple structures.\n" +
					"Only the first structure will be exported. Continue?",
					"Cannot export all structures", JOptionPane.YES_NO_OPTION, 
					JOptionPane.WARNING_MESSAGE, null, null, null);  

			if( retValue!=JOptionPane.YES_OPTION )
				return false;
		}

		// imposto la dialog per il salvataggio del file
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.addChoosableFileFilter(new ExtensionFileFilter(format));
		fileChooser.setCurrentDirectory(theWorkspace.getFileHistory().getRecentFolder());    

		// visualizzo la dialog
		int returnVal = fileChooser.showSaveDialog(this);        
		if (returnVal == JFileChooser.APPROVE_OPTION) {

			// aggiunge l'estension
			String filename = fileChooser.getSelectedFile().getAbsolutePath();
			filename = FileUtils.enforceExtension(filename,format);

			// chiede conferma prima di sovrascrivere il file
			File file = new File(filename);                    
			if (file.exists()) {
				int retValue = JOptionPane.showOptionDialog(this, "File exists. Overwrite file: " + filename + "?",
						"Salva documento", JOptionPane.YES_NO_CANCEL_OPTION, 
						JOptionPane.QUESTION_MESSAGE, null, null, null);  
				if( retValue!=JOptionPane.YES_OPTION )
					return false;
			}            

			// esporta il documento su file
			if( theDoc.isSequenceFormat(format) ) {
				if( theDoc.exportTo(filename,format) )
					setLastExportedFile(filename);
				return true;
			}
			else if( SVGUtils.export((GlycanRendererAWT) theWorkspace.getGlycanRenderer(),filename,theDoc.getStructures(),theWorkspace.getGraphicOptions().SHOW_MASSES,theWorkspace.getGraphicOptions().SHOW_REDEND,format) ) {
				setLastExportedFile(filename);
				return true;
			}        
		}
		return false;
	}

	/**
       Print the content of the {@link GlycanCanvas}
	 */
	public void onPrint() {
		try {       
			PrinterJob pj = theWorkspace.getPrinterJob();
			if( pj!=null ) {
				pj.setPrintable(theCanvas);
				if( pj.printDialog() ) theCanvas.print(pj);
			}
		}
		catch(Exception e) {
			LogUtils.report(e);
		}
	}

	/**
       Check for changed document to save and then exit the
       application
	 */
	public void onExit() {
		if( checkDocumentChanges(theDoc) ) this.exit(0);
	}
			
	//------------------
	// Help actions

	/**
       Show the about menu
	 */
	public void onAbout() {

		try {
			JDialog dlg = new JDialog(this, "About GlycanBuilder", true);        
			JEditorPane html = new JEditorPane(this.getClass().getResource("/html/about_builder.html"));
			html.setEditable(false);
			html.setBorder(new EmptyBorder(20,20,20,20));

			dlg.add(html);
			dlg.setSize(320,320);
			dlg.setResizable(false);
			dlg.setLocationRelativeTo(this);        

			dlg.setVisible(true);
		}
		catch(Exception e) {
			LogUtils.report(e);
		}
	}

	//--------------------------
	// Listeners

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		if( MouseUtils.isPopupTrigger(e) ) {       
			theCanvas.enforceSelection(e.getPoint());
			createPopupMenu().show(theCanvas, e.getX(), e.getY());   
		}
	}

	public void mouseReleased(MouseEvent e) {
		if( MouseUtils.isPopupTrigger(e) ) {       
			theCanvas.enforceSelection(e.getPoint());
			createPopupMenu().show(theCanvas, e.getX(), e.getY());   
		}
	}

	public void mouseClicked(MouseEvent e) {      
	}

	public void documentInit(BaseDocument.DocumentChangeEvent e) {
		if( e.getSource() == theWorkspace ) {
			theDoc = theWorkspace.getStructures(); 
			theDoc.addDocumentChangeListener(this);
			theCanvas.setDocument(theDoc);     
		}
		updateTitle();
		updateActions();
	}

	public void documentChanged(BaseDocument.DocumentChangeEvent e) {
		updateTitle();
		updateActions();
	}   

	public void fileHistoryChanged() {
		updateRecentFileMenu();
	}   

	public void actionPerformed(ActionEvent e) {

		String action = GlycanAction.getAction(e);
		String param  = GlycanAction.getParam(e);

		// workspace
		if( action.equals("empty") ) onNew(theDoc);
		// document handling
		else if( action.equals("new") ) onNew(param);
		else if( action.equals("open") ) onOpenDocument(theWorkspace.getAllDocuments(),false);
		//onOpen(param,theDoc,false);
		else if( action.equals("openinto") ) onOpenDocument(theWorkspace.getAllDocuments(),true);
		else if( action.equals("openstruct") ) onOpen(param,theWorkspace.getStructures(),false);
		else if( action.equals("save") ) onSave(theDoc,true);
		else if( action.equals("saveas") ) onSaveAs(theDoc);
		else if( action.equals("print") ) onPrint();
		else if( action.equals("import") ) onImportFrom(param);
		else if( action.equals("export") ) onExportTo(param);
		else if( action.equals("quit") ) onExit();
		
		// help
		else if( action.equals("about") ) onAbout();
		updateActions();
	}

	///-------------------------

	/**
       Run the application. Open the application frame
	 * @throws MalformedURLException 
	 */
	public static void main(String[] args) throws MalformedURLException {    
		new GlycanBuilder().setVisible(true);
		NativeInterface.runEventPump();
	}   
}

