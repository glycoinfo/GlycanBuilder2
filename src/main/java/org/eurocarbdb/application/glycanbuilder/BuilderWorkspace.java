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

import java.awt.Component;
import java.awt.print.*;
import java.util.*;
import java.io.*;
import java.net.URL;

import org.eurocarbdb.application.glycanbuilder.dataset.CoreDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.CrossRingFragmentDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.DictionaryConfiguration;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.ResiduePlacementDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.TerminalDictionary;
import org.eurocarbdb.application.glycanbuilder.fileutil.FileHistory;
import org.eurocarbdb.application.glycanbuilder.linkage.LinkageStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.CompositionOptions;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRenderer;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.eurocarbdb.application.glycanbuilder.util.XMLUtils;
import org.glycoinfo.application.glycanbuilder.dataset.CrossLinkedSubstituentDictionary;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;
import org.w3c.dom.*;

/**
 * A BuilderWorkspace is a container for all the documents, dictionaries and
 * options that are used in the GlycanBuilder application. During initialization
 * the workspace read all the configuration from a file (if provided) and
 * initialize all static dictionaries and options. A workspace must be
 * initialized before any other action using the GlycanBuilder classes.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class BuilderWorkspace extends BaseDocument implements BaseWorkspace,
		BaseDocument.DocumentChangeListener {

	protected static boolean loaded = false;

	protected boolean autosave;

	// style
	protected ResiduePlacementDictionary theResiduePlacementDictionary;
	

	protected ResidueStyleDictionary theResidueStyleDictionary;
	protected LinkageStyleDictionary theLinkageStyleDictionary;

	// configuration
	protected Configuration theConfiguration;
	protected FileHistory theFileHistory;
	protected ResidueHistory theResidueHistory;
	protected MassOptions theMassOptions;
	protected GraphicOptions theGraphicOptions;
	protected CompositionOptions theCompositionOptions;
	protected PrinterJob thePrinterJob;

	// documents
	protected GlycanDocument theStructures;

	// renderers
	protected GlycanRenderer theGlycanRenderer;
	
	protected DictionaryConfiguration dictConfig;

	/**
	 * Empty constructor. Initialize the dictionaries from the default files and
	 * set all the options to their default values.
	 */
	public BuilderWorkspace(GlycanRenderer glycanRenderer) {
		super(false);
		
		init();
		
		theGlycanRenderer=glycanRenderer;
		
		theGlycanRenderer.setGraphicOptions(theGraphicOptions);
		theGlycanRenderer.setResiduePlacementDictionary(theResiduePlacementDictionary);
		theGlycanRenderer.setResidueStyleDictionary(theResidueStyleDictionary);
		theGlycanRenderer.setLinkageStyleDictionary(theLinkageStyleDictionary);
	}
	
	public BuilderWorkspace(String config_file, boolean create,GlycanRenderer glycanRenderer,String residueTypesFile,
					String terminalTypesFile,String coreTypesFile,String crossRingFragmentTypesFile){
		super.init();
	    this.components=new HashMap<String,Component>();
	    
	    dictConfig = new DictionaryConfiguration();
	    
	    if(residueTypesFile!=null)
	    	dictConfig.setDictionaryFile(DictionaryConfiguration.RESIDUE_TYPES_FILE, residueTypesFile);
	    if(coreTypesFile!=null)
	    	dictConfig.setDictionaryFile(DictionaryConfiguration.CORE_TYPES_FILE, coreTypesFile);
	    if(terminalTypesFile!=null)
	    	dictConfig.setDictionaryFile(DictionaryConfiguration.TERMINAL_TYPES_FILE, terminalTypesFile);
	    if(crossRingFragmentTypesFile!=null)
	    	dictConfig.setDictionaryFile(DictionaryConfiguration.CROSS_RING_FRAGMENT_TYPES_FILE, crossRingFragmentTypesFile);
	   
	    
		loaded=false; //TODO: This isn't acceptable in the long run, dictionary loading is an issue in webapp context

		commonInit(config_file,create,glycanRenderer);
	}

	/**
	 * Load the configuration from a file. Initialize the dictionaries from the
	 * default files and set all the options to the values stored in the
	 * configuration.
	 * 
	 * @param config_file
	 *            the configuration file
	 * @param create
	 *            if <code>true</code> create a configuration file from the
	 *            default value in case the file does not exists
	 */
	public BuilderWorkspace(String config_file, boolean create,GlycanRenderer glycanRenderer) {
		super(false);
		commonInit(config_file,create,glycanRenderer);
	}
	
	private void commonInit(String config_file, boolean create,GlycanRenderer glycanRenderer){
		theGlycanRenderer=glycanRenderer;
		
		init(config_file, create, false);
		init();
		
		theGlycanRenderer.setGraphicOptions(theGraphicOptions);
		theGlycanRenderer
				.setResiduePlacementDictionary(theResiduePlacementDictionary);
		theGlycanRenderer.setResidueStyleDictionary(theResidueStyleDictionary);
		theGlycanRenderer.setLinkageStyleDictionary(theLinkageStyleDictionary);
	}
	
	
	// base document

	public int size() {
		return 1;
	}

	public String getName() {
		return "Workspace";
	}

	public Collection<javax.swing.filechooser.FileFilter> getFileFormats() {
		return new Vector<javax.swing.filechooser.FileFilter>();
	}

	public javax.swing.filechooser.FileFilter getAllFileFormats() {
		return null;
	}

	public void initData() {
		init(null, false, true);
	}

	//

	protected void createConfiguration() {
		theConfiguration = new Configuration();
		theFileHistory = new FileHistory();
		theResidueHistory = new ResidueHistory();
		theMassOptions = new MassOptions();
		theGraphicOptions = new GraphicOptions();
		theCompositionOptions = new CompositionOptions();
		
		if(dictConfig==null)
			dictConfig = new DictionaryConfiguration();

		thePrinterJob = null; // printer job is created only the first time is
								// requested
		// this is because applets needs permission from the user
		// to print

	}

	/**
	 * Load the configuration from a file. Initialize the dictionaries from the
	 * default files and set all the options to the values stored in the
	 * configuration.
	 * 
	 * @param config_file
	 *            the configuration file
	 * @param create
	 *            if <code>true</code> create a configuration file from the
	 *            default value in case the file does not exists
	 * @param keep_configuration
	 *            if <code>true</code> the configuration is not reloaded
	 */
	public void init(String config_file, boolean create,
			boolean keep_configuration) {
		if (!keep_configuration || theConfiguration == null) {
			// create configuration instances
			createConfiguration();

			// initialize configuration
			if (config_file != null && theConfiguration.open(config_file)){
				retrieveFromConfiguration();
				loaded=false;
			}else {
				storeToConfiguration(true);
				if (config_file != null && create)
					theConfiguration.save(config_file);
			}

			// initialize dictionaries
			if (!loaded) {

				ResidueDictionary
				.loadDictionary(dictConfig.getDictionaryFile(DictionaryConfiguration.RESIDUE_TYPES_FILE));
				TerminalDictionary
				.loadDictionary(dictConfig.getDictionaryFile(DictionaryConfiguration.TERMINAL_TYPES_FILE));
				CoreDictionary.loadDictionary(dictConfig.getDictionaryFile(DictionaryConfiguration.CORE_TYPES_FILE));
				CrossRingFragmentDictionary
				.loadDictionary(dictConfig.getDictionaryFile(DictionaryConfiguration.CROSS_RING_FRAGMENT_TYPES_FILE));
				NonSymbolicResidueDictionary
				.loadDictionary(dictConfig.getDictionaryFile(DictionaryConfiguration.NON_SYMBOLIC_RESIDUE_TYPES_FILE));
				CrossLinkedSubstituentDictionary
				.loadDictionary(dictConfig.getDictionaryFile(DictionaryConfiguration.CROSS_LINKED_SUBSTITUENT_TYPES_FILE));
				
				loaded = true;
			}

			// initialize style
			theResiduePlacementDictionary = new ResiduePlacementDictionary();
			theResidueStyleDictionary = new ResidueStyleDictionary();
			theLinkageStyleDictionary = new LinkageStyleDictionary();

			loadStyles(theGraphicOptions.NOTATION);
			setDisplay(theGraphicOptions.DISPLAY);
		}

		// initialize documents
		theStructures = new GlycanDocument(this);
	}
	
	public static URL getResource(String resource){
		return BuilderWorkspace.class.getResource(resource);
	}

	protected void retrieveFromConfiguration() {
		theFileHistory.retrieve(theConfiguration);
		theResidueHistory.retrieve(theConfiguration);
		theMassOptions.retrieve(theConfiguration);
		theGraphicOptions.retrieve(theConfiguration);
		theCompositionOptions.retrieve(theConfiguration);
		dictConfig.retrieve(theConfiguration);
	}

	protected void storeToConfiguration(boolean save_options) {
		theFileHistory.store(theConfiguration);
		theResidueHistory.store(theConfiguration);
		if (save_options) {
			theMassOptions.store(theConfiguration);
			theGraphicOptions.store(theConfiguration);
			theCompositionOptions.store(theConfiguration);
			dictConfig.store(theConfiguration);
		}
		
	}

	/**
	 * Store the configuration to a file. If the <code>autosave</code> flag is
	 * set all the option values are first stored in the configuration.
	 * 
	 * @param config_file
	 *            the destination configuration file
	 */
	public void exit(String config_file) {
		if (config_file != null) {
			storeConfiguration(config_file);
		}
	}
	
	public void storeConfiguration(String config_file){
		storeToConfiguration(autosave);
		theConfiguration.save(config_file);
	}

	/**
	 * Return the value of the <code>autosave</code> flag
	 * 
	 * @see #exit(String)
	 */
	public boolean getAutoSave() {
		return autosave;
	}

	/**
	 * Set the value of the <code>autosave</code> flag
	 * 
	 * @see #exit(String)
	 */
	public void setAutoSave(boolean flag) {
		autosave = flag;
	}

	/**
	 * Return the configuration object.
	 */
	public Configuration getConfiguration() {
		return theConfiguration;
	}

	/**
	 * Return the list of recently opened files.
	 */
	public FileHistory getFileHistory() {
		return theFileHistory;
	}

	/**
	 * Store the file history into the configuration.
	 */
	public void storeFileHistory() {
		theFileHistory.store(theConfiguration);
	}

	/**
	 * Return the list of recently added residues.
	 */
	public ResidueHistory getResidueHistory() {
		return theResidueHistory;
	}

	/**
	 * Store the residue history into the configuration.
	 */
	public void storeResidueHistory() {
		theResidueHistory.store(theConfiguration);
	}

	/**
	 * Return the mass options used when creating new structures.
	 */
	public MassOptions getDefaultMassOptions() {
		return theMassOptions;
	}

	/**
	 * Set the mass options values.
	 */
	public void setDefaultMassOptions(MassOptions mass_opt) {
		if (mass_opt != null)
			theMassOptions = mass_opt;
	}

	/**
	 * Store the mass options into the configuration.
	 */
	public void storeDefaultMassOptions() {
		theMassOptions.store(theConfiguration);
	}

	/**
	 * Return the graphic options used to display structures.
	 */
	public GraphicOptions getGraphicOptions() {
		return theGraphicOptions;
	}

	/**
	 * Store the graphic options into the configuration.
	 */
	public void storeGraphicOptions() {
		theGraphicOptions.store(theConfiguration);
	}

	/**
	 * Return the options used to create new glycan compositions.
	 */
	public CompositionOptions getCompositionOptions() {
		return theCompositionOptions;
	}

	/**
	 * Store the composition options into the configuration.
	 */
	public void storeCompositionOptions() {
		theCompositionOptions.store(theConfiguration);
	}

	/**
	 * Return a printer job object with the current printing options. Lazily
	 * initialize the object if necessary.
	 */
	public PrinterJob getPrinterJob() {
		if (thePrinterJob == null) {
			// lazy creation of PrinterJob
			try {
				thePrinterJob = PrinterJob.getPrinterJob();
			} catch (Exception e) {
				LogUtils.report(e);
			}
		}

		return thePrinterJob;
	}

	/**
	 * Return the document containing the glycan structures.
	 */
	public GlycanDocument getStructures() {
		return theStructures;
	}

	/**
	 * Return the renderer used to create graphic representations of the
	 * structures.
	 */
	public GlycanRenderer getGlycanRenderer() {
		return theGlycanRenderer;
	}

	/**
	 * Set the graphical notation used to represent the glycan structures. Load
	 * all style dictionaries with the values corresponding to this notation.
	 * 
	 * @see GraphicOptions#NOTATION
	 */
	public void setNotation(String notation) {
		theGraphicOptions.NOTATION = notation;
		loadStyles(notation);
	}

	/**
	 * Set the way structures are display in a specific notation.
	 * 
	 * @see GraphicOptions#DISPLAY
	 */
	public void setDisplay(String display) {
		theGraphicOptions.setDisplay(display);
	}

	protected void loadStyles(String notation) {
		if (notation.equals(GraphicOptions.NOTATION_UOXF)) {
			theResiduePlacementDictionary
					.loadPlacements(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.UOXF_RESIDUE_PLACEMENTS_FILE));
			theResidueStyleDictionary
					.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.UOXF_RESIDUE_STYLES_FILE));
			theLinkageStyleDictionary
					.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.UOXF_LINKAGE_STYLES_FILE));
		} else if (notation.equals(GraphicOptions.NOTATION_UOXFCOL)) {
			theResiduePlacementDictionary
			.loadPlacements(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.UOXF_RESIDUE_PLACEMENTS_FILE));
			theResidueStyleDictionary
			.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.UOXFCOL_RESIDUE_STYLES_FILE));
			theLinkageStyleDictionary
			.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.UOXF_LINKAGE_STYLES_FILE));
		}

		else if (notation.equals(GraphicOptions.NOTATION_TEXT)) {
			theResiduePlacementDictionary
			.loadPlacements(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.TEXT_RESIDUE_PLACEMENTS_FILE));
			theResidueStyleDictionary
			.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.TEXT_RESIDUE_STYLES_FILE));
			theLinkageStyleDictionary
			.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.TEXT_LINKAGE_STYLES_FILE));
		}
		else if (notation.equals(GraphicOptions.NOTATION_CFGLINK)) {
			theResiduePlacementDictionary
			.loadPlacements(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.CFGLINK_RESIDUE_PLACEMENTS_FILE));
			theResidueStyleDictionary
			.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.CFGLINK_RESIDUE_STYLES_FILE));
			theLinkageStyleDictionary
			.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.CFGLINK_LINKAGE_STYLES_FILE));
		} 
		else if (notation.equals(GraphicOptions.NOTATION_CFGBW)) {
			theResiduePlacementDictionary
			.loadPlacements(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.CFGBW_RESIDUE_PLACEMENTS_FILE));
			theResidueStyleDictionary
			.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.CFGBW_RESIDUE_STYLES_FILE));
			theLinkageStyleDictionary
			.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.CFGBW_LINKAGE_STYLES_FILE));
		} 
		else if (notation.equals(GraphicOptions.NOTATION_CFG)){
			//LogUtils.report(new Exception("Loading..."+getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.CFG_RESIDUE_PLACEMENTS_FILE)));
			//LogUtils.report(new Exception("Loading..."+getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.CFG_RESIDUE_STYLES_FILE)));
			//LogUtils.report(new Exception("Loading..."+getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.CFG_LINKAGE_STYLES_FILE)));
			
			theResiduePlacementDictionary
			.loadPlacements(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.CFG_RESIDUE_PLACEMENTS_FILE));
			theResidueStyleDictionary
			.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.CFG_RESIDUE_STYLES_FILE));
			theLinkageStyleDictionary
			.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.CFG_LINKAGE_STYLES_FILE));
		}
		else {
			theResiduePlacementDictionary
			.loadPlacements(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.SNFG_RESIDUE_PLACEMENTS_FILE));
			theResidueStyleDictionary
			.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.SNFG_RESIDUE_STYLES_FILE));
			theLinkageStyleDictionary
			.loadStyles(getDictionaryConfig().getDictionaryFile(DictionaryConfiguration.SNFG_LINKAGE_STYLES_FILE));
		}
	}

	/**
	 * Return all the documents present in this workspace.
	 */
	public Collection<BaseDocument> getAllDocuments() {
		Vector<BaseDocument> ret = new Vector<BaseDocument>();

		ret.add(this);
		ret.add(theStructures);

		return ret;
	}

	/**
	 * Return all the documents in the workspace that have been changed since
	 * their initialization.
	 */
	public Collection<BaseDocument> getUnsavedDocuments() {
		Vector<BaseDocument> ret = new Vector<BaseDocument>();
		if (theStructures.hasChanged())
			ret.add(theStructures);
		return ret;
	}

	/**
	 * Reset the change status flags for all the documents.
	 */
	public void resetChanges() {
		this.resetStatus();
		theStructures.resetStatus();

		fireDocumentInit();
	}

	// listeners

	public void documentInit(BaseDocument.DocumentChangeEvent e) {
		// there's a change in some internal document
		setChanged(true);
		fireDocumentChanged((BaseDocument) e.getSource());
	}

	public void documentChanged(BaseDocument.DocumentChangeEvent e) {
		// there's a change in some internal document
		setChanged(true);
		fireDocumentChanged((BaseDocument) e.getSource());
	}

	/**
	 * Fire a init event for all the documents in the workspace and for the
	 * workspace object itself.
	 */
	public void fireDocumentInit() {
		super.fireDocumentInit();
		theStructures.fireDocumentInit();
	}

	/**
	 * Fire a init event for the given document in the workspace.
	 */
	public void fireDocumentInit(BaseDocument source) {
		if (source == this)
			fireDocumentInit();
		else
			super.fireDocumentInit(source);
	}

	// serialization

	/**
	 * Create a string representation in XML format of the workspace object
	 * containing the configuration and all the documents.
	 */
	public String toString() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		Document document = XMLUtils.newDocument();
		document.appendChild(toXML(document));
		XMLUtils.write(bos, document);

		return bos.toString();
	}

	/**
	 * Parse a string representation in XML format of the workspace object
	 * containing the configuration and all the documents.
	 */
	public void fromString(String str, boolean merge) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(str.getBytes());

		Document document = XMLUtils.read(bis);
		if (document == null)
			throw new Exception("Cannot read from string");

		fromXML(XMLUtils.assertChild(document, "GlycanWorkspace"), merge);
	}

	/**
	 * Parse an element of a DOM document containing the configuration and all
	 * the workspace documents.
	 */
	public void fromXML(Node w_node, boolean merge) throws Exception {

		resetStatus();

		// set glycan document
		Node gd_node = XMLUtils.findChild(w_node, "Structures");
		if (gd_node != null)
			theStructures.fromXML(gd_node, merge);
	}

	/**
	 * Create an element of a DOM document containing the configuration and all
	 * the workspace documents.
	 */
	public Element toXML(Document document) {
		if (document == null)
			return null;

		// create root node
		Element w_node = document.createElement("GlycanWorkspace");

		// create configuration node
		storeToConfiguration(true);
		w_node.appendChild(theConfiguration.toXML(document));

		// create structures node
		w_node.appendChild(theStructures.toXML(document));

		return w_node;
	}
	
	public ResiduePlacementDictionary getTheResiduePlacementDictionary() {
		return theResiduePlacementDictionary;
	}

	public void setTheResiduePlacementDictionary(
			ResiduePlacementDictionary theResiduePlacementDictionary) {
		this.theResiduePlacementDictionary = theResiduePlacementDictionary;
	}

	public ResidueStyleDictionary getTheResidueStyleDictionary() {
		return theResidueStyleDictionary;
	}

	public void setTheResidueStyleDictionary(
			ResidueStyleDictionary theResidueStyleDictionary) {
		this.theResidueStyleDictionary = theResidueStyleDictionary;
	}

	public LinkageStyleDictionary getTheLinkageStyleDictionary() {
		return theLinkageStyleDictionary;
	}

	public void setTheLinkageStyleDictionary(
			LinkageStyleDictionary theLinkageStyleDictionary) {
		this.theLinkageStyleDictionary = theLinkageStyleDictionary;
	}

	public DictionaryConfiguration getDictionaryConfig(){
		return dictConfig;
	}

	public void setDictionaryConfig(DictionaryConfiguration dictConfig){
		this.dictConfig=dictConfig;
	}
}
