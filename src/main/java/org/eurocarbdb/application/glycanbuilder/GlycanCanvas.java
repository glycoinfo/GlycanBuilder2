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

import org.eurocarbdb.application.glycanbuilder.dataset.CoreDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.TerminalDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.renderutil.*;
import org.eurocarbdb.application.glycanbuilder.util.*;
import org.glycoinfo.application.glycanbuilder.dataset.CrossLinkedSubstituentDictionary;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;
import org.glycoinfo.application.glycanbuilder.dataset.TextSymbolDescriptor;
import org.glycoinfo.application.glycanbuilder.dialog.ResidueDesignDialog;
import org.glycoinfo.application.glycanbuilder.util.GlycanUtils;
import org.glycoinfo.application.glycanbuilder.util.canvas.CanvasActionDescriptor;
import org.glycoinfo.application.glycanbuilder.util.canvas.CompositionUtility;
import org.glycoinfo.application.glycanbuilder.util.canvas.DebugUtility;
import org.pushingpixels.flamingo.api.common.*;
import org.pushingpixels.flamingo.api.common.JCommandButton.CommandButtonKind;
import org.pushingpixels.flamingo.api.common.icon.ResizableIcon;
import org.pushingpixels.flamingo.api.common.popup.JCommandPopupMenu;
import org.pushingpixels.flamingo.api.common.popup.JPopupPanel;
import org.pushingpixels.flamingo.api.common.popup.PopupPanelCallback;
import org.pushingpixels.flamingo.api.ribbon.*;
import org.pushingpixels.flamingo.api.ribbon.resize.CoreRibbonResizePolicies;
import org.pushingpixels.flamingo.api.ribbon.resize.IconRibbonBandResizePolicy;
import org.pushingpixels.flamingo.api.ribbon.resize.RibbonBandResizePolicy;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.*;
import java.util.List;

import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.*;

/**
 * A component that implement a visual editor of glycan structures. Multiple
 * structures can be created in the same editor. The structures are displayed
 * using the settings specified by the current {@link GraphicOptions}. The
 * actions to create and modify the structures can be accessed by toolbars menus
 * that should be added to the application frame. The default toolbars and menus
 * can be retrieved using: {@link #getToolBarDocument},
 * {@link #getToolBarStructure}, {@link #getToolBarProperties},
 * {@link #getEditMenu}, {@link #getStructureMenu} and {@link #getViewMenu}.
 * Listeners can be registered to react to changes in the structures (through
 * the underlying {@link GlycanDocument}) and in the selections.
 * 
 * @see GlycanDocument
 * @see GlycanRendererAWT
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class GlycanCanvas extends JComponent implements ActionListener,
		BaseDocument.DocumentChangeListener, ResidueHistory.Listener,
		Printable, MouseListener, MouseMotionListener, HyperlinkListener {
	private static ICON_SIZE defaultMenuIconSize = ICON_SIZE.L3;
	
	private boolean allowRepeatingUnits=true;
	private boolean allowMultipleStructures=true;
	private boolean allowUncertainTerminals=true;
	private boolean allowCyclicUnits=true;
	private boolean allowAlternativeUnit = true;
	
	// Classes

	/**
	 * Interface that should be implemented by all objects that want to be
	 * notified when the selection is changed
	 */
	public interface SelectionChangeListener {
		/**
		 * Called by the component when the selection is changed
		 */
		public void selectionChanged(SelectionChangeEvent e);
	}

	/**
	 * Contains the information about a selection change event
	 */
	public static class SelectionChangeEvent {
		private GlycanCanvas src;

		/**
		 * Default constructor
		 * 
		 * @param _src
		 *            the source of the event
		 */
		public SelectionChangeEvent(GlycanCanvas _src) {
			src = _src;
		}

		/**
		 * Return the source of the event
		 */
		public GlycanCanvas getSource() {
			return src;
		}
	}

	// -----------

	protected ImageIcon last;

	protected static final long serialVersionUID = 0L;
	protected GlycanCanvas this_object = null;

	// singletons
	protected JFrame theParent = null;
	protected BuilderWorkspace theWorkspace = null;
	protected GlycanDocument theDoc = null;
	// protected ActionManager theActionManager;
	private ActionManager theActionManager;

	// graphic objects
	protected JScrollPane theScrollPane = null;
	protected JToolBar theToolBarDocument;
	protected JToolBar theToolBarStructure;
	protected JToolBar theToolBarProperties;

	protected JComboBox field_anomeric_state;
	protected JComboBox field_anomeric_carbon;
	protected DropDownList field_linkage_position;
	protected JComboBox field_chirality;
	protected JComboBox field_ring_size;
	protected JCheckBox field_second_bond;
	protected JComboBox field_second_child_position;
	protected DropDownList field_second_parent_position;	
	
	// menus
	protected JMenu theEditMenu = null;
	protected JMenu theStructureMenu = null;
	protected JMenu theViewMenu = null;
	protected JMenu theDebugMenu = null;

	protected JCheckBoxMenuItem show_redend_canvas_button = null;

	protected int recent_residues_index = -1;
	protected int no_recent_residues_buttons = 0;

	protected ButtonGroup display_button_group = null;
	protected HashMap<String, ButtonModel> display_models = null;

	// selection
	protected Residue current_residue;
	protected Linkage current_linkage;
	protected HashSet<Residue> selected_residues;
	protected HashSet<Linkage> selected_linkages;

	// painting
	private GlycanRendererAWT theGlycanRenderer;
	protected Rectangle all_structures_bbox;
	protected BBoxManager theBBoxManager;
	protected PositionManager thePosManager;
	protected boolean is_printing;

	protected JLabel sel_label = new JLabel();

	// events
	private boolean ignore_actions = false;

	protected Point mouse_start_point = null;
	protected Point mouse_end_point = null;

	// DnD

	protected boolean is_dragndrop = false;
	protected boolean was_dragged = false;
	protected Cursor dndcopy_cursor = null;
	protected Cursor dndmove_cursor = null;
	protected Cursor dndnocopy_cursor = null;
	protected Cursor dndnomove_cursor = null;

	// 
	protected LinkedList<SelectionChangeListener> listeners;
	protected ThemeManager themeManager;
	private RibbonTask theEditRibbon;
	private RibbonTask theStructureRibbon;
	private RibbonTask theViewRibbon;
	private JRibbonBand structureSelectionBand;
	//private JRibbonBand structureRibbonBandCFG; /** for init Symbol notation?*/
	private JRibbonBand structureRibbonBandSNFG;
	//private JRibbonBand structureRibbonBandCFGGRY;
	private String STRUCTURE_GALLERY_NAME = "Add structure";
	private String RESIDUE_GALLERY_NAME = "Add residue";
	private String TERMINAL_GAL_NAME = "Add terminal";
	
	private JRibbonBand insertResidueJRibbonBand;
	private JCommandButton orientationButton;
	private JRibbonBand terminalRibbonBand;
	private RibbonContextualTaskGroup theLinkageRibbon;

	protected Set<ContextAwareContainer> contextAwareContainers;
	protected Set<NotationChangeListener> notationChangeListeners;
	protected Map<String,String> themeNameToQualifiedName;
	
	private CanvasCommand a_oCommand;
	
	public void nullAll() {
		listeners = null;
		themeManager = null;
		theEditRibbon = null;
		theStructureRibbon = null;
		theViewRibbon = null;
		structureSelectionBand = null;
		//structureRibbonBandCFG = null;
		structureRibbonBandSNFG = null;
		//structureRibbonBandCFGGRY = null;

		insertResidueJRibbonBand = null;
		orientationButton = null;
		terminalRibbonBand = null;
		theLinkageRibbon = null;

		contextAwareContainers = null;
		notationChangeListeners = null;
	}

	private HashMap<RESIDUE_INSERT_MODES, List<ResidueGalleryIndex>> residueGalleries;

	/**right click utility*/
	private JComboBox field_anomeric_state_r;
	private JComboBox field_anomeric_carbon_r;
	private DropDownList field_linkage_position_r;
	private JComboBox field_chirality_r;
	private JComboBox field_ring_size_r;
	private JCheckBox field_second_bond_r;
	private JComboBox field_second_child_position_r;
	private DropDownList field_second_parent_position_r;
	private HashMap<String, String> qualifiedNameToThemeName;
	private JMenu addResidueMenu;
	private JMenu addTerminalMenu;
	private JMenuItem insertResidueMenu;
	private JMenuItem changeResidueMenu;

	public enum RESIDUE_INSERT_MODES {
		INSERT, REPLACE, ADD, TERMINAL
	}

	public class ResidueGalleryIndex {
		public JRibbonBand band;
		public String galleryName;

		public ResidueGalleryIndex(JRibbonBand _band, String _galleryName) {
			this.band = _band;
			this.galleryName = _galleryName;
		}
	}

	// ---------
	// construction

	public RibbonTask getTheViewRibbon() {
		return theViewRibbon;
	}

	public RibbonTask getTheStructureRibbon() {
		return theStructureRibbon;
	}

	/**
	 * Default constructor
	 * 
	 * @param parent
	 *            the parent frame
	 * @param _workspace
	 *            the workspace containing all documents and options
	 * @throws MalformedURLException
	 */

	public GlycanCanvas(JFrame parent, BuilderWorkspace _workspace,
			ThemeManager _themeManager) {
		initCanvas(parent, _workspace, _themeManager, true);
	}

	public GlycanCanvas(JFrame parent, BuilderWorkspace _workspace,
			ThemeManager _themeManager, boolean enableRibbons)
			throws MalformedURLException {
		initCanvas(parent, _workspace, _themeManager, enableRibbons);
	}

	public void initCanvas(JFrame parent, BuilderWorkspace _workspace,
			ThemeManager _themeManager, boolean enableRibbons) {
		// init
		this.themeManager = _themeManager;
		FileUtils.themeManager=_themeManager;
		try {
			themeManager.addIconPath("/icons/glycan_builder", this.getClass());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			themeManager.addIconPath("/icons/crystal_project", this.getClass());
		} catch (IOException e) {
			e.printStackTrace();
		}

		a_oCommand = new CanvasCommand(this.themeManager, this.getTheActionManager());
		
		this_object = this;
		theParent = parent;
		theWorkspace = _workspace;
		theDoc = theWorkspace.getStructures();
		theDoc.addDocumentChangeListener(this);
		// theActionManager = new ActionManager();
		setTheActionManager(new ActionManager());

		current_residue = null;
		current_linkage = null;
		selected_residues = new HashSet<Residue>();
		selected_linkages = new HashSet<Linkage>();

		setTheGlycanRenderer((GlycanRendererAWT) theWorkspace.getGlycanRenderer());
		thePosManager = new PositionManager();
		theBBoxManager = new BBoxManager();
		all_structures_bbox = null;
		is_printing = false;

		residueGalleries = new HashMap<RESIDUE_INSERT_MODES, List<ResidueGalleryIndex>>();

		for (RESIDUE_INSERT_MODES mode : RESIDUE_INSERT_MODES.values()) {
			residueGalleries.put(mode, new ArrayList<ResidueGalleryIndex>());
		}

		// initialize the action set
		createActions();

		// create toolbars
		theToolBarDocument = createToolBarDocument();
		theToolBarStructure = createToolBarStructure();
		theToolBarProperties = createToolBarProperties();

		// create menus
		theEditMenu = createEditMenu();
		
		if(!enableRibbons){
			theStructureMenu = createStructureMenu();
			theViewMenu = createViewMenu();
			theDebugMenu = createDebugMenu();
		}

		if (enableRibbons) {
			int restoreOrientation=theWorkspace.getGraphicOptions().ORIENTATION;
			if(theWorkspace.getGraphicOptions().ORIENTATION==GraphicOptions.BT || theWorkspace.getGraphicOptions().ORIENTATION==GraphicOptions.TB ){
				theWorkspace.getGraphicOptions().ORIENTATION=GraphicOptions.RL;
			}

			theEditRibbon = createEditRibbonBand();
			theStructureRibbon = createStructureRibbonTask();
			theViewRibbon = createViewRibbonTask();
			theLinkageRibbon = createLinkageRibbon();
			
			theWorkspace.getGraphicOptions().ORIENTATION=restoreOrientation;
		}

		// set the canvas
		this.setOpaque(true);
		this.setBackground(Color.white);

		// load DnD cursors
		dndcopy_cursor = FileUtils.createCursor("dnd-copy");
		dndnocopy_cursor = FileUtils.createCursor("dnd-nocopy");
		dndmove_cursor = FileUtils.createCursor("dnd-move");
		dndnomove_cursor = FileUtils.createCursor("dnd-nomove");

		// init events
		theWorkspace.getResidueHistory().addHistoryChangedListener(this);
		theDoc.addDocumentChangeListener(this);
		addMouseMotionListener(this);
		addMouseListener(this);

		listeners = new LinkedList<SelectionChangeListener>();

		contextAwareContainers = new HashSet<ContextAwareContainer>();
		notationChangeListeners = new HashSet<NotationChangeListener>();
		
	}

	public void addContextAwareContainer(ContextAwareContainer container) {
		contextAwareContainers.add(container);
	}

	public void addNotationChangeListener(
			NotationChangeListener notationChangeListener) {
		notationChangeListeners.add(notationChangeListener);
	}

	public RibbonContextualTaskGroup getTheLinkageRibbon() {
		return theLinkageRibbon;
	}

	public RibbonTask getTheEditRibbon() {
		return theEditRibbon;
	}

	/**
	 * Set the underlying document containing the glycan structures that are
	 * being created and modified
	 */
	public void setDocument(GlycanDocument doc) {
		if (theDoc != null)
			theDoc.removeDocumentChangeListener(this);

		theDoc = doc;

		if (theDoc != null)
			theDoc.addDocumentChangeListener(this);

		resetSelection();
		this.respondToDocumentChange = true;
		repaint();
	}

	/**
	 * Return the underlying document containing the glycan structures that are
	 * being created and modified
	 */
	public GlycanDocument getDocument() {
		return theDoc;
	}

	/**
	 * Return the action manager
	 */
	public ActionManager getActionManager() {
		return getTheActionManager();
	}

	/**
	 * Return the object that is used to render the structures
	 */
	public GlycanRenderer getGlycanRenderer() {
		return getTheGlycanRenderer();
	}

	/**
	 * Set the object that is used to render the structures
	 */
	public void setGlycanRenderer(GlycanRendererAWT r) {
		setTheGlycanRenderer(r);
	}

	/**
	 * Add a scroll pane to the component
	 */
	public void setScrollPane(JScrollPane sp) {
		theScrollPane = sp;
		theScrollPane.getVerticalScrollBar().setUnitIncrement(20);
		theScrollPane.getVerticalScrollBar().setBlockIncrement(20);
		theScrollPane.getHorizontalScrollBar().setUnitIncrement(10);
		theScrollPane.getHorizontalScrollBar().setBlockIncrement(10);
	}

	/**
	 * Return the toolbar containing the default actions to create and modify
	 * the underlying document
	 */
	public JToolBar getToolBarDocument() {
		return theToolBarDocument;
	}

	/**
	 * Return the toolbar containing the default actions to create and modify
	 * the glycan structures
	 */
	public JToolBar getToolBarStructure() {
		return theToolBarStructure;
	}

	/**
	 * Return the toolbar containing the default actions to change the residue
	 * properties
	 */
	public JToolBar getToolBarProperties() {
		return theToolBarProperties;
	}

	/**
	 * Return the menu containing the default actions to create and modify the
	 * underlying document
	 */
	public JMenu getEditMenu() {
		return theEditMenu;
	}

	/**
	 * Return the menu containing the default actions to create and modify the
	 * glycan structures
	 */
	public JMenu getStructureMenu() {
		return theStructureMenu;
	}

	/**
	 * Return the menu containing the default actions to change the graphic
	 * options
	 */
	public JMenu getViewMenu() {
		return theViewMenu;
	}

	public JMenu getDebugMenu() {
		return this.theDebugMenu;
	}
	
	private String getCurrentOrientation() {
		int orientation = theWorkspace.getGraphicOptions().ORIENTATION;
		String iconId;
		if (orientation == GraphicOptions.LR) iconId = "lr";
		else if (orientation == GraphicOptions.RL) iconId = "rl";
		else if (orientation == GraphicOptions.TB) iconId = "tb";
		else if (orientation == GraphicOptions.BT) iconId = "bt";
		else return null;
		return iconId;
	}

	private EurocarbResizableIcon getOrientationIcon() {
		return themeManager.getResizableIcon(getCurrentOrientation(),
				ICON_SIZE.L3);
	}

	private void createActions() {
		CanvasAction a_oAction = new CanvasAction();
		a_oAction.createAction(getTheActionManager(), themeManager, defaultMenuIconSize, this, getTheGlycanRenderer());
	}

	private void updateActions() {
		getTheActionManager().get("undo").setEnabled(theDoc.getUndoManager().canUndo());
		getTheActionManager().get("redo").setEnabled(theDoc.getUndoManager().canRedo());
		getTheActionManager().get("cut").setEnabled(hasSelectedResidues());
		getTheActionManager().get("copy").setEnabled(hasSelectedResidues());
		getTheActionManager().get("delete").setEnabled(hasSelectedResidues());

		if(allowUncertainTerminals==false){
			getTheActionManager().get("bracket").setEnabled(false);
			if(bracketButton!=null) bracketButton.setVisible(false);
			if(bracketMenuItem!=null) bracketMenuItem.setVisible(false);
		}else{
			getTheActionManager().get("bracket").setEnabled(hasCurrentResidue());
			if(bracketButton!=null) bracketButton.setVisible(true);
			if(bracketMenuItem!=null) bracketMenuItem.setVisible(true);
		}
		
		if(allowRepeatingUnits==false){
			getTheActionManager().get("repeat").setEnabled(false);
			if(repeatButton!=null) repeatButton.setVisible(false);
			if(repeatMenuItem!=null) repeatMenuItem.setVisible(false);
		}else{
			getTheActionManager().get("repeat").setEnabled(hasCurrentResidue());
			if(repeatButton!=null) repeatButton.setVisible(true);
			if(repeatMenuItem!=null) repeatMenuItem.setVisible(true);
		}
		
		if(allowCyclicUnits == false) {
			getTheActionManager().get("cyclic").setEnabled(false);
			if(cyclicButton != null) cyclicButton.setVisible(false);
			if(cyclicMenuItem != null) cyclicMenuItem.setVisible(false);
		}else {
			getTheActionManager().get("cyclic").setEnabled(hasCurrentResidue());
			if(cyclicButton != null) cyclicButton.setVisible(true);
			if(cyclicMenuItem != null) cyclicMenuItem.setVisible(true);
		}		
		
		/*if(!allowAlternativeUnit) {
			getTheActionManager().get("alternative").setEnabled(false);
			if(altButton != null) altButton.setVisible(false);
		} else {
			getTheActionManager().get("alternative").setEnabled((getCurrentStructure() != null));
			if(altButton != null) altButton.setVisible(true);
		}*/
		//getTheActionManager().get("stringNotation").setEnabled(true);
		
		if(this.getCurrentResidue() == null || 
				!this.getCurrentResidue().isSaccharide() || 
				!this.getCurrentResidue().hasParent() ||
				!this.getCurrentResidue().getParent().isBracket()) {
			getTheActionManager().get("antennaParent").setEnabled(false);
			if(this.antennaParentButton!=null) this.antennaParentButton.setVisible(false);
			if(this.antennaParentItem!=null) this.antennaParentItem.setVisible(false);
		}else {
			boolean isAntenna = this.getCurrentResidue().getParent().isBracket();
			getTheActionManager().get("antennaParent").setEnabled(isAntenna);
			if(this.antennaParentButton!=null) this.antennaParentButton.setVisible(isAntenna);
			if(this.antennaParentItem!=null) this.antennaParentItem.setVisible(isAntenna);
		}
		
		getTheActionManager().get("properties").setEnabled(hasCurrentResidue());

		// theActionManager.get("orientation").putValue(Action.SMALL_ICON,
		// getOrientationIcon().);

		getTheActionManager().get("massoptstruct").setEnabled(hasSelection());

		getTheActionManager().get("moveccw").setEnabled(hasCurrentSelection());
		// theActionManager.get("resetplace").setEnabled(hasCurrentSelection());
		getTheActionManager().get("movecw").setEnabled(hasCurrentSelection());
		
		if(allowMultipleStructures==false){
			if(theDoc.getNoStructures()==0) setAddStructureStatus(true);
			else if(getSelectedResidues().length==0) setAddStructureStatus(false);
			else setAddStructureStatus(true);
		}else{
			setAddStructureStatus(true);
		}
	}
	
	protected void setAddStructureStatus(boolean enable){
		ResidueRenderer rr = getTheGlycanRenderer().getResidueRenderer();
		for (ResidueType t : ResidueDictionary.allResidues()) {

			getTheActionManager().get("change=" + t.getName()).setEnabled(enable);

			if (t.canHaveParent()) {
				getTheActionManager().get("add=" + t.getName()).setEnabled(enable);
			}
			if (t.canHaveParent() && t.canHaveChildren())
				getTheActionManager().get("insert=" + t.getName()).setEnabled(enable);
			if (t.canBeReducingEnd()) {
				getTheActionManager().get("changeredend=" + t.getName()).setEnabled(enable);
			}
		}
		
		for (CoreType t : CoreDictionary.getCores()) {
			getTheActionManager().get("addstructure=" + t.getName()).setEnabled(enable);
		}
		
		getTheActionManager().get("addstructurestr").setEnabled(enable);
		getTheActionManager().get("getstructurestr").setEnabled(enable);
		getTheActionManager().get("addcomposition").setEnabled(enable);
	}
	
	private void updateOrientation(){
		if(theWorkspace.getGraphicOptions().ORIENTATION==GraphicOptions.LR || theWorkspace.getGraphicOptions().ORIENTATION==GraphicOptions.RL){
			updateStructureRibbonGallery(STRUCTURE_GALLERY_NAME, structureSelectionBand);
			
			for (ResidueGalleryIndex gal : this.residueGalleries.get(RESIDUE_INSERT_MODES.TERMINAL)) {
				updateTerminalRibbonGallery(gal.galleryName, gal.band);
			}
		}
	}

	private void updateResidueActions() {
		// structure
		ResidueRenderer rr = getTheGlycanRenderer().getResidueRenderer();
		ICON_SIZE iconSize = ICON_SIZE.L4;

		for (ResidueType t : ResidueDictionary.allResidues()) {
			ImageResizableIconReducedMem icon = new ImageResizableIconReducedMem(
					rr.getImage(t, iconSize.getSize()), iconSize.getSize(), iconSize.getSize());

			EurocarbResizableIcon eu_icon = new EurocarbResizableIcon(this.themeManager, null, icon);
			eu_icon.setResizableIcon(icon);

			getTheActionManager().update("change=" + t.getName(), eu_icon, t.getDescription(), -1, "");

			if (t.canHaveParent()) {
				getTheActionManager().update("add=" + t.getName(), eu_icon, t.getDescription(), -1,
						(t.getToolbarOrder() != 0) ? ("ctrl " + t.getToolbarOrder()) : "");
				getTheActionManager().get("add=" + t.getName()).putValue(
						Action.SMALL_ICON,
						new ImageIcon(rr.getImage(t, ICON_SIZE.L3.getSize())));
			}
			if (t.canHaveParent() && t.canHaveChildren())
				getTheActionManager().update("insert=" + t.getName(), eu_icon, t
						.getDescription(), -1, "");
			if (t.canBeReducingEnd())
				getTheActionManager().update("redend=" + t.getName(), eu_icon, t
						.getDescription(), -1, "");
		}
		
		structureSelectionBand = createStructureRibbonBand();
		
		updateStructureRibbonGallery(STRUCTURE_GALLERY_NAME, structureSelectionBand);
		
		for (ResidueGalleryIndex gal : this.residueGalleries
				.get(RESIDUE_INSERT_MODES.ADD)) {
			this.updateAddResidueRibbonGallery(gal.galleryName, gal.band);
		}

		for (ResidueGalleryIndex gal : this.residueGalleries
				.get(RESIDUE_INSERT_MODES.REPLACE)) {
			this.updateChangeResidueRibbonGallery(gal.galleryName, gal.band);
		}

		for (ResidueGalleryIndex gal : this.residueGalleries
				.get(RESIDUE_INSERT_MODES.INSERT)) {
			this.updateInsertResidueRibbonGallery(gal.galleryName, gal.band);
		}

		for (ResidueGalleryIndex gal : this.residueGalleries
				.get(RESIDUE_INSERT_MODES.TERMINAL)) {
			updateTerminalRibbonGallery(gal.galleryName, gal.band);
		}
	}

	private String[] toStrings(char[] pos) {
		String[] ret = new String[pos.length];
		for (int i = 0; i < pos.length; i++)
			ret[i] = "" + pos[i];
		return ret;
	}
	
	private ListModel createPositions(Residue parent) {
		DefaultListModel ret = new DefaultListModel();

		// collect available positions
		char[] par_pos = null;
		if (parent == null || parent.getType().getLinkagePositions().length == 0)
			par_pos = new char[] { '1', '2', '3', '4', '5', '6', '7', '8', '9', 'N' };
		else
			par_pos = parent.getType().getLinkagePositions();

		// add elements
		ret.addElement("?");
		for (int i = 0; i < par_pos.length; i++)
			ret.addElement("" + par_pos[i]);

		return ret;
	}

	private void fireContextChanged(Context context, boolean switchToDefault) {
		for (ContextAwareContainer container : contextAwareContainers) {
			container.fireContextChanged(context, switchToDefault);
		}
	}

	private void fireUndoContextChanged(Context context) {
		for (ContextAwareContainer container : contextAwareContainers) {
			container.fireUndoContextChanged(context);
		}
	}

	private void updateToolbarProperties(boolean showControls) {
		ignore_actions = true;
		Residue current = getCurrentResidue();
		if (theParent instanceof JRibbonFrame && showControls && current != null) {
			boolean switchToDefault = true;
			if (lastMouseButton != null && lastMouseButton == MouseEvent.BUTTON3) {
				switchToDefault = false;
			}

			fireContextChanged(Context.GLYCAN_CANVAS_ITEM, switchToDefault);
		}

		if (current != null && (!current.isEndRepetition() || current.isCleavage())) {
			Linkage parent_link = current.getParentLinkage();

			if (parent_link != null) {
				field_linkage_position.setListModel(createPositions(parent_link.getParentResidue()));
				field_second_parent_position
						.setListModel(createPositions(parent_link.getParentResidue()));
			}
			

			// enable items
			boolean can_have_parent_linkage = (parent_link != null
					&& parent_link.getParentResidue() != null && (parent_link
					.getParentResidue().isSaccharide()
					|| parent_link.getParentResidue().isBracket()
					|| parent_link.getParentResidue().isRepetition() || parent_link
					.getParentResidue().isRingFragment()));

			field_linkage_position.setEnabled(can_have_parent_linkage || this.current_residue.isEndRepetition());
			field_anomeric_state.setEnabled(current.isSaccharide() && !current.getType().getSuperclass().equals("Bridge"));
			field_anomeric_carbon.setEnabled(current.isSaccharide() || current.getType().getSuperclass().equals("Bridge"));
			field_chirality.setEnabled(current.isSaccharide() && !current.getType().getSuperclass().equals("Bridge"));
			field_ring_size.setEnabled(current.isSaccharide() && !current.getType().getSuperclass().equals("Bridge"));
			field_second_bond.setEnabled(can_have_parent_linkage);
			field_second_parent_position.setEnabled(can_have_parent_linkage
					&& parent_link.hasMultipleBonds());
			field_second_child_position.setEnabled(can_have_parent_linkage
					&& parent_link.hasMultipleBonds());

			// fill items
			if (parent_link != null)
				field_linkage_position.setSelectedValues(toStrings(parent_link
						.glycosidicBond().getParentPositions()));
			else
				field_linkage_position.clearSelection();
			field_anomeric_state.setSelectedItem("" + current.getAnomericState());
			field_anomeric_carbon.setSelectedItem("" + current.getAnomericCarbon());
			field_chirality.setSelectedItem("" + current.getChirality());
			field_ring_size.setSelectedItem("" + current.getRingSize());
			
			if (parent_link != null) {
				field_second_bond.setSelected(parent_link.hasMultipleBonds());
				field_second_parent_position
						.setSelectedValues(toStrings(parent_link.getBonds()
								.get(0).getParentPositions()));
				field_second_child_position.setSelectedItem(""
						+ parent_link.getBonds().get(0).getChildPosition());
			} else {
				field_second_bond.setSelected(false);
				field_second_parent_position.clearSelection();
				field_second_child_position.setSelectedItem("?");
			}		
		} else {
			if (theParent instanceof JRibbonFrame && !showControls) {
				fireUndoContextChanged(Context.GLYCAN_CANVAS_ITEM);

				// ((JRibbonFrame) theParent).getRibbon().setVisible(
				// this.theLinkageRibbon, false);
			}
			// reset all
			field_linkage_position.setEnabled(false);
			field_anomeric_state.setEnabled(false);
			field_anomeric_carbon.setEnabled(false);
			field_chirality.setEnabled(false);
			field_ring_size.setEnabled(false);
			field_second_bond.setEnabled(false);
			field_second_parent_position.setEnabled(false);
			field_second_child_position.setEnabled(false);
			
			// fill items
			field_linkage_position.clearSelection();
			field_anomeric_state.setSelectedItem("?");
			field_anomeric_carbon.setSelectedItem("");
			field_chirality.setSelectedItem("?");
			field_ring_size.setSelectedItem("?");
			field_second_bond.setSelected(false);
			field_second_parent_position.clearSelection();
			field_second_child_position.setSelectedItem("?");
		}

		if(current != null && current.isEndRepetition()) {
			field_linkage_position.setEnabled(true);
			field_anomeric_carbon.setSelectedItem("1");
		}
		
		ignore_actions = false;
	}

	private JMenu createAddStructureMenu() {
		return this.a_oCommand.createAddStructureMenu(getTheActionManager());
	}

	private JMenu createAddResidueMenu() {
		return this.a_oCommand.createAddResidueMenu(getTheActionManager());
	}

	private JMenu createAddTerminalMenu() {
		return this.a_oCommand.createAddTerminalMenu(this);
	}

	private JMenu createInsertResidueMenu() {
		return this.a_oCommand.createInsertResidueMenu(getTheActionManager());
	}

	private JMenu createChangeResidueTypeMenu() {
		return this.a_oCommand.createChangeResidueTypeMenu(getTheActionManager());
	}
	
	private JMenu createInsertBridgeMenu() {
		return this.a_oCommand.createInsertBridgeMenu(getTheActionManager());
	}

	private void updateRecentResiduesToolbar(JToolBar tb) {
		for (int i = 0; i < no_recent_residues_buttons; i++)
			tb.remove(recent_residues_index);

		no_recent_residues_buttons = 0;
		for (String typename : theWorkspace.getResidueHistory()
				.getRecentResidues()) {
			JButton b = new JButton(getTheActionManager().get("add=" + typename));
			b.setText(null);
			tb.add(b, recent_residues_index + no_recent_residues_buttons++);
		}
		
		//tb.repaint();
	}

	private JMenu createZoomMenu() {
		return this.a_oCommand.createZoomMenu(theWorkspace, getTheActionManager());
	}

	private JMenu createViewMenu() {
		GraphicOptions view_opt = theWorkspace.getGraphicOptions();

		JMenu view_menu = new JMenu("View");
		view_menu.setMnemonic(KeyEvent.VK_V);
		view_menu.add(createZoomMenu());
		view_menu.addSeparator();

		// notation
		JRadioButtonMenuItem last = null;
		ButtonGroup groupn = new ButtonGroup();

		view_menu.add(last = new JRadioButtonMenuItem(getTheActionManager()
				.get("notation=" + GraphicOptions.NOTATION_CFG)));
		last.setSelected(view_opt.NOTATION.equals(GraphicOptions.NOTATION_CFG));
		groupn.add(last);

		view_menu.add(last = new JRadioButtonMenuItem(getTheActionManager()
				.get("notation=" + GraphicOptions.NOTATION_CFGBW)));
		last.setSelected(view_opt.NOTATION.equals(GraphicOptions.NOTATION_CFGBW));
		groupn.add(last);

		view_menu.add(last = new JRadioButtonMenuItem(getTheActionManager()
				.get("notation=" + GraphicOptions.NOTATION_CFGLINK)));
		last.setSelected(view_opt.NOTATION.equals(GraphicOptions.NOTATION_CFGLINK));
		groupn.add(last);

		view_menu.add(last = new JRadioButtonMenuItem(getTheActionManager()
				.get("notation=" + GraphicOptions.NOTATION_UOXF)));
		last.setSelected(view_opt.NOTATION.equals(GraphicOptions.NOTATION_UOXF));
		
		view_menu.add(last = new JRadioButtonMenuItem(getTheActionManager()
				.get("notation=" + GraphicOptions.NOTATION_UOXFCOL)));
		last.setSelected(view_opt.NOTATION.equals(GraphicOptions.NOTATION_UOXFCOL));
		groupn.add(last);

		view_menu.add(last = new JRadioButtonMenuItem(getTheActionManager()
				.get("notation=" + GraphicOptions.NOTATION_TEXT)));
		last.setSelected(view_opt.NOTATION.equals(GraphicOptions.NOTATION_TEXT));
		groupn.add(last);

		/**SNFG notation*/
		view_menu.add(last = new JRadioButtonMenuItem(getTheActionManager()
				.get("notation=" + GraphicOptions.NOTATION_SNFG)));
		last.setSelected(view_opt.NOTATION.equals(GraphicOptions.NOTATION_SNFG));
		groupn.add(last);
		
		view_menu.addSeparator();

		// display

		display_button_group = new ButtonGroup();
		display_models = new HashMap<String, ButtonModel>();

		view_menu.add(last = new JRadioButtonMenuItem(getTheActionManager()
				.get("display=" + GraphicOptions.DISPLAY_COMPACT)));
		last.setSelected(view_opt.DISPLAY.equals(GraphicOptions.DISPLAY_COMPACT));
		display_models.put(GraphicOptions.DISPLAY_COMPACT, last.getModel());
		display_button_group.add(last);

		view_menu.add(last = new JRadioButtonMenuItem(getTheActionManager()
				.get("display=" + GraphicOptions.DISPLAY_NORMAL)));
		last.setSelected(view_opt.DISPLAY.equals(GraphicOptions.DISPLAY_NORMAL));
		display_models.put(GraphicOptions.DISPLAY_NORMAL, last.getModel());
		display_button_group.add(last);

		view_menu.add(last = new JRadioButtonMenuItem(getTheActionManager()
				.get("display=" + GraphicOptions.DISPLAY_NORMALINFO)));
		last.setSelected(view_opt.DISPLAY.equals(GraphicOptions.DISPLAY_NORMALINFO));
		display_models.put(GraphicOptions.DISPLAY_NORMALINFO, last.getModel());
		display_button_group.add(last);

		view_menu.add(last = new JRadioButtonMenuItem(getTheActionManager()
				.get("display=" + GraphicOptions.DISPLAY_CUSTOM)));
		last.setSelected(view_opt.DISPLAY.equals(GraphicOptions.DISPLAY_CUSTOM));
		display_models.put(GraphicOptions.DISPLAY_CUSTOM, last.getModel());
		display_button_group.add(last);

		// view_menu.add( lastcb = new
		// JCheckBoxMenuItem(theActionManager.get("showinfo")) );
		// lastcb.setState(view_opt.SHOW_INFO);

		view_menu.addSeparator();

		// export

		JCheckBoxMenuItem lastcb = null;
		view_menu.add(lastcb = new JCheckBoxMenuItem(getTheActionManager()
				.get("collapsemultipleantennae")));
		lastcb.setState(view_opt.COLLAPSE_MULTIPLE_ANTENNAE);
		view_menu.add(lastcb = new JCheckBoxMenuItem(getTheActionManager()
				.get("showmassescanvas")));
		lastcb.setState(view_opt.SHOW_MASSES_CANVAS);
		view_menu.add(lastcb = new JCheckBoxMenuItem(getTheActionManager()
				.get("showmasses")));
		lastcb.setState(view_opt.SHOW_MASSES);
		view_menu.add(lastcb = new JCheckBoxMenuItem(getTheActionManager()
				.get("showredendcanvas")));
		show_redend_canvas_button = lastcb;
		lastcb.setState(view_opt.SHOW_REDEND_CANVAS);
		view_menu.add(lastcb = new JCheckBoxMenuItem(getTheActionManager()
				.get("showredend")));
		lastcb.setState(view_opt.SHOW_REDEND);

		view_menu.addSeparator();

		// orientation
		view_menu.add(getTheActionManager().get("orientation"));
		view_menu.addSeparator();
		view_menu.add(getTheActionManager().get("displaysettings"));

		return view_menu;
	}

	private RibbonTask createViewRibbonTask() {
		JFlowRibbonBand band1 = new JFlowRibbonBand(
				"Notation format",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(10));

		ArrayList<RibbonBandResizePolicy> resizePolicies1 = new ArrayList<RibbonBandResizePolicy>();
		resizePolicies1.add(new CoreRibbonResizePolicies.FlowTwoRows(band1.getControlPanel()));
		resizePolicies1.add(new IconRibbonBandResizePolicy(band1.getControlPanel()));
		band1.setResizePolicies(resizePolicies1);

		final GraphicOptions view_opt = theWorkspace.getGraphicOptions();

		JCommandButtonPanel panel = new JCommandButtonPanel(CommandButtonDisplayState.TILE);

		panel.addButtonGroup("Active notation");
		panel.addButtonToLastGroup(getTheActionManager().get(
				"notation=" + GraphicOptions.NOTATION_CFG)
				.getJCommandToggleButton("", this,
						view_opt.NOTATION.equals(GraphicOptions.NOTATION_CFG),
						ICON_SIZE.L6));
		panel.addButtonToLastGroup(getTheActionManager().get(
				"notation=" + GraphicOptions.NOTATION_CFGBW)
				.getJCommandToggleButton("", this,
						view_opt.NOTATION.equals(GraphicOptions.NOTATION_CFGBW),
						ICON_SIZE.L6));
		panel.addButtonToLastGroup(getTheActionManager().get(
				"notation=" + GraphicOptions.NOTATION_CFGLINK)
				.getJCommandToggleButton("", this,
						view_opt.NOTATION.equals(GraphicOptions.NOTATION_CFGLINK),
						ICON_SIZE.L6));
	
		panel.addButtonToLastGroup(getTheActionManager().get(
				"notation=" + GraphicOptions.NOTATION_UOXF)
				.getJCommandToggleButton("", this,
						view_opt.NOTATION.equals(GraphicOptions.NOTATION_UOXF),
						ICON_SIZE.L6));
		panel.addButtonToLastGroup(getTheActionManager().get(
				"notation=" + GraphicOptions.NOTATION_UOXFCOL)
				.getJCommandToggleButton("", this,
						view_opt.NOTATION.equals(GraphicOptions.NOTATION_UOXFCOL),
						ICON_SIZE.L6));
		panel.addButtonToLastGroup(getTheActionManager().get(
				"notation=" + GraphicOptions.NOTATION_TEXT)
				.getJCommandToggleButton("", this,
						view_opt.NOTATION.equals(GraphicOptions.NOTATION_TEXT),
						ICON_SIZE.L6));
		
		/**SNFG symbol*/
		panel.addButtonToLastGroup(getTheActionManager().get(
				"notation=" + GraphicOptions.NOTATION_SNFG)
				.getJCommandToggleButton("", this,
						view_opt.NOTATION.equals(GraphicOptions.NOTATION_TEXT),
						ICON_SIZE.L6));
		
		panel.setToShowGroupLabels(false);
		panel.setSingleSelectionMode(true);
		panel.setMaxButtonColumns(7);
		panel.setMaxButtonRows(1);
		band1.addFlowComponent(panel);
		
		int zoom = (int) (theWorkspace.getGraphicOptions().SCALE_CANVAS * 100);
		if (zoom == 145)
			zoom = 150;
		
		final JCommandButton zoomButton=new JCommandButton(String.valueOf(zoom)+"%",
				themeManager.getResizableIcon("magglass", ICON_SIZE.L3).getResizableIcon());
//		zoomButton.setAutoWidthPopupPanel(true);
//		zoomButton.setAlignPopupToRight(true);
		zoomButton.setCommandButtonKind(CommandButtonKind.ACTION_AND_POPUP_MAIN_POPUP);
		zoomButton.setDisplayState(CommandButtonDisplayState.TILE);
		zoomButton.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		zoomButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				theWorkspace.getGraphicOptions().SCALE_CANVAS = 1;
				respondToDocumentChange = true;
				zoomButton.setText("100%");
				repaint();
			}
			
		});
		zoomButton.setPopupCallback(new PopupPanelCallback(){
			@Override
			public JPopupPanel getPopupPanel(JCommandButton arg0) {
				JCommandPopupMenu menu=new JCommandPopupMenu();
				
				ActionListener actionListener=new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e) {
						JCommandMenuButton cb = (JCommandMenuButton) e.getSource();
						String zoomLevel = (String) cb.getText();
						theWorkspace.getGraphicOptions().SCALE_CANVAS = Double
								.parseDouble(zoomLevel.substring(0,
										zoomLevel.length() - 1)) / 100.;
						respondToDocumentChange = true;
						zoomButton.setText(zoomLevel);
						repaint();
					}
				};
				
				int scaleArray[]={300,400,200,150,100,67,50,33,25};
				for(int scale:scaleArray){
					JCommandMenuButton button=new JCommandMenuButton(getTheActionManager().get("scale="+scale).getName(),null);
					button.addActionListener(actionListener);
					menu.addMenuButton(button);
				}
				
				return menu;
			}
			
		});

		updateOrientationButton();
		
		JFlowRibbonBand band3 = new JFlowRibbonBand(
				"Display settings",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(10));

		band3.addFlowComponent(getTheActionManager().get("displaysettings")
				.getJCommandButton(ICON_SIZE.L4, " ", this,
						new RichTooltip("Change display settings", " "), true));

		
		band3.addFlowComponent(getTheActionManager().get("explode")
				.getJCommandButton(ICON_SIZE.L4, " ", this,
						new RichTooltip("Explode panels", " "), true));
		band3.addFlowComponent(getTheActionManager().get("implode")
				.getJCommandButton(ICON_SIZE.L4, " ", this,
						new RichTooltip("Implode panels", " "), true));
		band3.addFlowComponent(orientationButton);
		//band3.addFlowComponent(new JLabel("Zoom"));
		band3.addFlowComponent(zoomButton);
		
		

		ArrayList<RibbonBandResizePolicy> resizePolicies3 = new ArrayList<RibbonBandResizePolicy>();
		resizePolicies3.add(new CoreRibbonResizePolicies.FlowTwoRows(band3.getControlPanel()));
		// //resizePolicies3.add(new
		// CoreRibbonResizePolicies.FlowThreeRows(band3.getControlPanel()));
		resizePolicies3.add(new IconRibbonBandResizePolicy(band3
				.getControlPanel()));
		band3.setResizePolicies(resizePolicies3);

		JFlowRibbonBand band2 = new JFlowRibbonBand(
				"Notation style",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(10));
		ArrayList<RibbonBandResizePolicy> resizePolicies2 = new ArrayList<RibbonBandResizePolicy>();
		resizePolicies2.add(new CoreRibbonResizePolicies.FlowTwoRows(band2.getControlPanel()));
		resizePolicies2.add(new IconRibbonBandResizePolicy(band2.getControlPanel()));
		band2.setResizePolicies(resizePolicies2);

		ButtonGroup group = new ButtonGroup();
		JCheckBox box;

		band2.addFlowComponent(box = getTheActionManager().get(
				"display=" + GraphicOptions.DISPLAY_COMPACT).getJCheckBox(
				"Compact",
				view_opt.DISPLAY.equals(GraphicOptions.DISPLAY_COMPACT), this));
		group.add(box);
		band2.addFlowComponent(box = getTheActionManager().get(
				"display=" + GraphicOptions.DISPLAY_NORMAL).getJCheckBox(
				"Normal",
				view_opt.DISPLAY.equals(GraphicOptions.DISPLAY_NORMAL), this));
		group.add(box);
		band2.addFlowComponent(box = getTheActionManager().get(
				"display=" + GraphicOptions.DISPLAY_NORMALINFO).getJCheckBox(
				"Extended",
				view_opt.DISPLAY.equals(GraphicOptions.DISPLAY_NORMALINFO),
				this));
		group.add(box);
		band2.addFlowComponent(box = getTheActionManager().get(
				"display=" + GraphicOptions.DISPLAY_CUSTOM).getJCheckBox(
				"Custom",
				view_opt.DISPLAY.equals(GraphicOptions.DISPLAY_CUSTOM), this));
		group.add(box);

		JFlowRibbonBand band4 = new JFlowRibbonBand(
				"Show features",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(
						10));
		ArrayList<RibbonBandResizePolicy> resizePolicies4 = new ArrayList<RibbonBandResizePolicy>();
		resizePolicies4.add(new CoreRibbonResizePolicies.FlowThreeRows(band4
				.getControlPanel()));
		resizePolicies4.add(new IconRibbonBandResizePolicy(band4
				.getControlPanel()));

		band4.setResizePolicies(resizePolicies4);

		band4.addFlowComponent(getTheActionManager().get("collapsemultipleantennae")
				.getJCheckBox("Collapse multiple antennae", this));
		band4.addFlowComponent(getTheActionManager().get("showmassescanvas")
				.getJCheckBox("Mass information", this));
		band4.addFlowComponent(getTheActionManager().get("showredendcanvas")
				.getJCheckBox("Reducing end indicator", this));

		getTheActionManager().get("showmassescanvas").setSelected(
				getTheGlycanRenderer().getGraphicOptions().SHOW_MASSES_CANVAS);
		getTheActionManager().get("showredendcanvas").setSelected(
				getTheGlycanRenderer().getGraphicOptions().SHOW_REDEND_CANVAS);
		
		

		JFlowRibbonBand band5 = new JFlowRibbonBand(
				"Switch themes",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(
						10));
		ArrayList<RibbonBandResizePolicy> resizePolicies5 = new ArrayList<RibbonBandResizePolicy>();
		resizePolicies5.add(new CoreRibbonResizePolicies.FlowTwoRows(band5
				.getControlPanel()));
		resizePolicies5.add(new IconRibbonBandResizePolicy(band5
				.getControlPanel()));
		band5.setResizePolicies(resizePolicies5);

		this.themeNameToQualifiedName=new HashMap<String,String>();
		themeNameToQualifiedName.put("OfficeBlue2007Skin", "org.pushingpixels.substance.api.skin.OfficeBlue2007Skin");
		themeNameToQualifiedName.put("AutumnSkin", "org.pushingpixels.substance.api.skin.AutumnSkin");
		themeNameToQualifiedName.put("TwilightSkin", "org.pushingpixels.substance.api.skin.TwilightSkin");
		themeNameToQualifiedName.put("GraphiteGlassSkin", "org.pushingpixels.substance.api.skin.GraphiteGlassSkin");
		themeNameToQualifiedName.put("GraphiteAquaSkin", "org.pushingpixels.substance.api.skin.GraphiteAquaSkin");
		themeNameToQualifiedName.put("EmeraldDuskSkin", "org.pushingpixels.substance.api.skin.EmeraldDuskSkin");
		themeNameToQualifiedName.put("OfficeSilver2007Skin", "org.pushingpixels.substance.api.skin.OfficeSilver2007Skin");
		themeNameToQualifiedName.put("BasicWhiteSkin", "basic.white");
		
		this.qualifiedNameToThemeName=new HashMap<String,String>();
		qualifiedNameToThemeName.put("org.pushingpixels.substance.api.skin.OfficeBlue2007Skin","OfficeBlue2007Skin");
		qualifiedNameToThemeName.put("org.pushingpixels.substance.api.skin.AutumnSkin","AutumnSkin");
		qualifiedNameToThemeName.put("org.pushingpixels.substance.api.skin.TwilightSkin","TwilightSkin");
		qualifiedNameToThemeName.put("org.pushingpixels.substance.api.skin.GraphiteGlassSkin","GraphiteGlassSkin");
		qualifiedNameToThemeName.put("org.pushingpixels.substance.api.skin.GraphiteAquaSkin","GraphiteAquaSkin");
		qualifiedNameToThemeName.put("org.pushingpixels.substance.api.skin.EmeraldDuskSkin","EmeraldDuskSkin");
		qualifiedNameToThemeName.put("org.pushingpixels.substance.api.skin.OfficeSilver2007Skin","OfficeSilver2007Skin");
		qualifiedNameToThemeName.put("basic.white","BasicWhiteSkin");
		
		final JCommandButton themeButton=new JCommandButton(qualifiedNameToThemeName.get(theWorkspace.getGraphicOptions().THEME));
		themeButton.setCommandButtonKind(CommandButtonKind.POPUP_ONLY);
		themeButton.setDisplayState(CommandButtonDisplayState.TILE);
		//themeButton.setAlignPopupToRight(true);
		//themeButton.setAutoWidthPopupPanel(true);
		
		final String themes[] =  {
				"OfficeBlue2007Skin",
				"AutumnSkin",
				"TwilightSkin",
				"GraphiteGlassSkin",
				"GraphiteAquaSkin",
				"EmeraldDuskSkin",
				"OfficeSilver2007Skin",
				"BasicWhiteSkin",
		};
		
		themeButton.setPopupCallback(new PopupPanelCallback(){
			@Override
			public JPopupPanel getPopupPanel(JCommandButton arg0) {
				JCommandPopupMenu menu=new JCommandPopupMenu();
				
				ActionListener actionListener=new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e) {
						JCommandMenuButton cb = (JCommandMenuButton) e.getSource();
						String themeString = (String) cb.getText();
						if(themeNameToQualifiedName.containsKey(themeString)){
							themeString=themeNameToQualifiedName.get(themeString);
//							try {
//								SubstanceLookAndFeel.setSkin(themeString);
//								notifyLAFListeners();
//							} catch (Exception exc) {
//								exc.printStackTrace();
//							}
							theWorkspace.getGraphicOptions().THEME = themeString;
						}
						JOptionPane.showMessageDialog(null, "GlycoWorkbench must be restarted for the new theme to be applied");
					}
				};
				
				for(String theme:themes){
					JCommandMenuButton button=new JCommandMenuButton(theme,null);
					button.addActionListener(actionListener);
					menu.addMenuButton(button);
				}
				return menu;
			}
		});
		
		band5.addFlowComponent(themeButton);

		return new RibbonTask("View", band1, band2, band4, band3, band5);
	}

	private void updateOrientationButton() {
		if (orientationButton == null) {
			orientationButton = getTheActionManager().get("orientation")
					.getJCommandButton(ICON_SIZE.L4, this, " ");
		}
		EurocarbResizableIcon iconR = getOrientationIcon();
		if(iconR==null){
			System.err.println("Icon R is null1");
		}else if(iconR.getIconProperties()==null){
			System.err.println("Icon R is null2");
		}else if(iconR.getIconProperties().id==null){
			System.err.println("Icon R is null3");
		}
		orientationButton.setIcon(iconR.getThemeManager().getResizableIcon(
				iconR.getIconProperties().id, ICON_SIZE.L4).getResizableIcon());

		try {
			getTheActionManager().get("orientation").putValue(
					Action.SMALL_ICON,
					iconR.getThemeManager().getImageIcon(
							iconR.getIconProperties().id, ICON_SIZE.L3));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private RibbonTask createEditRibbonBand() {
		return this.a_oCommand.createEditRibbonBand(getTheActionManager(), this);
	}

	/**
	 * Update the structure ribbon gallery. a)First checks if a gallery with the
	 * given name already exists on the ribbon band and removes it if it does
	 * b)Adds a new gallery with the given name
	 * 
	 * @param galleryName
	 * @param band
	 */
	private void updateStructureRibbonGallery(final String galleryName, final JRibbonBand band) {
		final GlycanCanvas self = this;

		if (band != null && band.getControlPanel() != null) {

			if (band.getControlPanel().getRibbonGallery(galleryName) != null) {
				band.getControlPanel().remove(band.getControlPanel().getRibbonGallery(galleryName));
			}

			ICON_SIZE iconSize = ICON_SIZE.L6;
			final List<StringValuePair<List<JCommandToggleButton>>> galleryButtons = new ArrayList<StringValuePair<List<JCommandToggleButton>>>();
			for (String superclass : CoreDictionary.getSuperclasses()) {
				Collection<CoreType> core_types = CoreDictionary.getCores(superclass);
				if (core_types.size() > 0) {
					List<JCommandToggleButton> galleryButtonsList = new ArrayList<JCommandToggleButton>();
					
					JCommandToggleButtonAction temp=null;
					
					for (CoreType coreType : core_types) {
						BufferedImage image=getTheGlycanRenderer().getImage(
								Glycan.fromString(coreType
										.getStructure()),
								false, false, false);
						ImageResizableIconReducedMem imageIcon=new ImageResizableIconReducedMem(image,
								iconSize.getSize(), 
								iconSize.getSize());
						imageIcon.minScale(1);
						String description=coreType.getDescription();
						description=description.replaceAll("N-glycan", "");
						description=description.replaceAll("O-glycan", "");
						JCommandToggleButtonAction button = new JCommandToggleButtonAction(description, imageIcon);
						button.addActionListener(self);
						
						button.setDisplayState(CommandButtonDisplayState.BIG);

						button.setActionCommand("addstructure=" + coreType.getName());
						if(description.equals(" bisected fucosylated")){
							temp=button;
						}else if(temp!=null){
							galleryButtonsList.add(button);
							galleryButtonsList.add(temp);
							temp=null;
						}else{
							galleryButtonsList.add(button);
						}
					}
					
					galleryButtons.add(new StringValuePair<List<JCommandToggleButton>>(
									superclass, galleryButtonsList));
				}
			}

			final Map<RibbonElementPriority, Integer> visibleButtonCounts = new HashMap<RibbonElementPriority, Integer>();
			visibleButtonCounts.put(RibbonElementPriority.LOW, 4);
			visibleButtonCounts.put(RibbonElementPriority.MEDIUM, 4);
			visibleButtonCounts.put(RibbonElementPriority.TOP, 4);

			//band.addRibbonGallery(galleryName, galleryButtons,
			//			visibleButtonCounts, 4, 4, RibbonElementPriority.TOP);
		}
	}

	/**
	 * Update the residue ribbon gallery. a)First checks if a gallery with the
	 * given name already exists on the ribbon band and removes it if it does
	 * b)Adds a new gallery with the given name
	 * 
	 * @param galleryName
	 * @param band
	 */
	private void updateInsertResidueRibbonGallery(final String galleryName,
			final JRibbonBand band) {
		final GlycanCanvas self = this;

		if (band != null && band.getControlPanel() != null) {

			if (band.getControlPanel().getRibbonGallery(galleryName) != null) {
        band.getControlPanel().remove(band.getControlPanel().getRibbonGallery(galleryName));
				// System.err.println("Removing insert residue gallery");
			} else {
				this.residueGalleries.get(RESIDUE_INSERT_MODES.INSERT).add(
						new ResidueGalleryIndex(band, galleryName));
			}

			ICON_SIZE iconSize = ICON_SIZE.L6;
			final List<StringValuePair<List<JCommandToggleButton>>> galleryButtons = new ArrayList<StringValuePair<List<JCommandToggleButton>>>();
			for (String superclass : ResidueDictionary.getSuperclasses()) {
				Collection<ResidueType> core_types = ResidueDictionary
						.getResidues(superclass);
				if (core_types.size() > 0) {
					List<JCommandToggleButton> galleryButtonsList = new ArrayList<JCommandToggleButton>();
					for (ResidueType t : ResidueDictionary
							.getResidues(superclass)) {
						if (t.canHaveParent() && t.canHaveChildren()) {
							// && t.getMaxLinkages() >= 2
							ResizableIcon icon = new ImageResizableIconReducedMem(this.getGlycanRenderer()
											.getResidueRenderer().getImage(t,
													iconSize.getSize()),
											iconSize.getSize(),
													iconSize.getSize());
							JCommandToggleButtonAction button = new JCommandToggleButtonAction(
									t.getName(), icon);
							button.addActionListener(self);
							button.setActionCommand("insert=" + t.getName());
							galleryButtonsList.add(button);
						}
					}

					if(galleryButtonsList.size()>0){
						galleryButtons
						.add(new StringValuePair<List<JCommandToggleButton>>(
								superclass, galleryButtonsList));
						
					}
				}
			}

			final Map<RibbonElementPriority, Integer> visibleButtonCounts = new HashMap<RibbonElementPriority, Integer>();
			visibleButtonCounts.put(RibbonElementPriority.LOW, 4);
			visibleButtonCounts.put(RibbonElementPriority.MEDIUM, 4);
			visibleButtonCounts.put(RibbonElementPriority.TOP, 4);

			band.addRibbonGallery(galleryName, galleryButtons,
						visibleButtonCounts, 5, 4, RibbonElementPriority.TOP);
			
		}
	}

	/**
	 * Update the residue ribbon gallery. a)First checks if a gallery with the
	 * given name already exists on the ribbon band and removes it if it does
	 * b)Adds a new gallery with the given name
	 * 
	 * @param galleryName
	 * @param band
	 */
	private void updateChangeResidueRibbonGallery(final String galleryName,
			final JRibbonBand band) {
		final GlycanCanvas self = this;

		if (band != null && band.getControlPanel() != null) {

			if (band.getControlPanel().getRibbonGallery(galleryName) != null) {
        band.getControlPanel().remove(band.getControlPanel().getRibbonGallery(galleryName));
				// System.err.println("Removing change residue gallery");
			} else {
				this.residueGalleries.get(RESIDUE_INSERT_MODES.REPLACE).add(
						new ResidueGalleryIndex(band, galleryName));
			}

			ICON_SIZE iconSize = ICON_SIZE.L6;
			final List<StringValuePair<List<JCommandToggleButton>>> galleryButtons = new ArrayList<StringValuePair<List<JCommandToggleButton>>>();
			for (String superclass : ResidueDictionary.getSuperclasses()) {
				Collection<ResidueType> core_types = ResidueDictionary
						.getResidues(superclass);
				if (core_types.size() > 0) {
					List<JCommandToggleButton> galleryButtonsList = new ArrayList<JCommandToggleButton>();
					for (ResidueType t : ResidueDictionary
							.getResidues(superclass)) {
						// if (t.canHaveParent() && t.canHaveChildren()) {
						// && t.getMaxLinkages() >= 2
						ResizableIcon icon = new ImageResizableIconReducedMem(
								this.getGlycanRenderer().getResidueRenderer()
										.getImage(t, iconSize.getSize()),
								iconSize.getSize(), iconSize.getSize());
						JCommandToggleButtonAction button = new JCommandToggleButtonAction(
								t.getName(), icon);
						button.addActionListener(self);
						button.setActionCommand("change=" + t.getName());
						galleryButtonsList.add(button);
						// }
					}
					
					if(galleryButtonsList.size()>0){
						galleryButtons
						.add(new StringValuePair<List<JCommandToggleButton>>(
								superclass, galleryButtonsList));
					}
				}
			}

			final Map<RibbonElementPriority, Integer> visibleButtonCounts = new HashMap<RibbonElementPriority, Integer>();
			visibleButtonCounts.put(RibbonElementPriority.LOW, 4);
			visibleButtonCounts.put(RibbonElementPriority.MEDIUM, 4);
			visibleButtonCounts.put(RibbonElementPriority.TOP, 4);

			
			band.addRibbonGallery(galleryName, galleryButtons,
						visibleButtonCounts, 5, 4, RibbonElementPriority.TOP);
		}
	}

	/**
	 * Update the residue ribbon gallery. a)First checks if a gallery with the
	 * given name already exists on the ribbon band and removes it if it does
	 * b)Adds a new gallery with the given name
	 * 
	 * @param galleryName
	 * @param band
	 */
	private void updateAddResidueRibbonGallery(final String galleryName,
			final JRibbonBand band) {
		final GlycanCanvas self = this;

		if (band != null && band.getControlPanel() != null) {
			if (band.getControlPanel().getRibbonGallery(galleryName) != null) {
				band.getControlPanel().remove(band.getControlPanel().getRibbonGallery(galleryName));
				// System.err.println("Removing add residue gallery");
			} else {
				this.residueGalleries.get(RESIDUE_INSERT_MODES.ADD).add(
						new ResidueGalleryIndex(band, galleryName));
			}

			ICON_SIZE iconSize = ICON_SIZE.L6;
			final List<StringValuePair<List<JCommandToggleButton>>> galleryButtons = new ArrayList<StringValuePair<List<JCommandToggleButton>>>();
			for (String superclass : ResidueDictionary.getSuperclasses()) {
				Collection<ResidueType> core_types = ResidueDictionary.getResidues(superclass);
				if (core_types.size() > 0) {
					List<JCommandToggleButton> galleryButtonsList = new ArrayList<JCommandToggleButton>();
					for (ResidueType t : ResidueDictionary.getResidues(superclass)) {
						if (t.canHaveParent()) {
							// && t.getMaxLinkages() >= 2
							ResizableIcon icon = new ImageResizableIconReducedMem(
								this.getGlycanRenderer().getResidueRenderer().getImage(t, iconSize.getSize()),
								iconSize.getSize(),
								iconSize.getSize());
							JCommandToggleButtonAction button = new JCommandToggleButtonAction(t.getName(), icon);
							button.addActionListener(self);
							button.setActionCommand("add=" + t.getName());
							galleryButtonsList.add(button);
						}
					}
					
					if(galleryButtonsList.size()>0){
						galleryButtons.add(new StringValuePair<List<JCommandToggleButton>>(
							superclass, galleryButtonsList));
					}
				}
			}

			final Map<RibbonElementPriority, Integer> visibleButtonCounts = 
					new HashMap<RibbonElementPriority, Integer>();
			visibleButtonCounts.put(RibbonElementPriority.LOW, 4);
			visibleButtonCounts.put(RibbonElementPriority.MEDIUM, 4);
			visibleButtonCounts.put(RibbonElementPriority.TOP, 4);

			System.out.println(galleryName);
			band.addRibbonGallery(galleryName, galleryButtons,
				visibleButtonCounts, 5, 4, RibbonElementPriority.TOP);
		}
	}
	
	private HashMap<TerminalType,ImageResizableIconReducedMem> getCachedTerminalImages(){
		HashMap<TerminalType,ImageResizableIconReducedMem> map=new HashMap<TerminalType,ImageResizableIconReducedMem>();
		ICON_SIZE iconSize = ICON_SIZE.L6;
		for (String superclass : TerminalDictionary.getSuperclasses()) {
			Collection<TerminalType> terminal_types = TerminalDictionary
					.getTerminals(superclass);
			if (terminal_types.size() > 0) {
				for (TerminalType terminalType : terminal_types) {
					List<JCommandToggleButton> galleryButtonsList = new ArrayList<JCommandToggleButton>();

					ImageResizableIconReducedMem imageIcon = new ImageResizableIconReducedMem(
							getGlycanRenderer().getImage(
									Glycan.fromString(terminalType.getStructure()), false, false,
									false), iconSize.getSize(), iconSize.getSize());
					imageIcon.minScale(1);
					map.put(terminalType, imageIcon);
				}
			}
		}
		
		return map;
	}
	
	private void updateTerminalRibbonGallery(final String galleryName,
			final JRibbonBand band, HashMap<TerminalType,ImageResizableIconReducedMem> map){
		final GlycanCanvas self = this;
		if (band != null && band.getControlPanel() != null) {

			if (band.getControlPanel().getRibbonGallery(galleryName) != null) {
        band.getControlPanel().remove(band.getControlPanel().getRibbonGallery(galleryName));
				// System.err.println("Removing terminal residue gallery");
			} else {
				residueGalleries.get(RESIDUE_INSERT_MODES.TERMINAL).add(
						new ResidueGalleryIndex(band, galleryName));
			}
			
			ICON_SIZE iconSize = ICON_SIZE.L6;
			final List<StringValuePair<List<JCommandToggleButton>>> galleryButtons = new ArrayList<StringValuePair<List<JCommandToggleButton>>>();
			for (String superclass : TerminalDictionary.getSuperclasses()) {
				Collection<TerminalType> terminal_types = TerminalDictionary
						.getTerminals(superclass);
				if (terminal_types.size() > 0) {
					for (TerminalType terminalType : terminal_types) {
						List<JCommandToggleButton> galleryButtonsList = new ArrayList<JCommandToggleButton>();

						ImageResizableIconReducedMem imageIcon=map.get(terminalType);
						JCommandToggleButtonAction button1 = new JCommandToggleButtonAction(
								"x-linked", imageIcon);
						button1.addActionListener(self);
						button1.setActionCommand("addterminal="
								+ terminalType.getName());
						galleryButtonsList.add(button1);
						for (int l = 1; l < 9; l++) {
							JCommandToggleButtonAction button = new JCommandToggleButtonAction(
									l + "-linked", imageIcon);
							button.addActionListener(self);
							button.setActionCommand("addterminal=" + l + "-"
									+ terminalType.getName());
							galleryButtonsList.add(button);
						}

						galleryButtons
								.add(new StringValuePair<List<JCommandToggleButton>>(
										superclass + "["
												+ terminalType.getDescription()
												+ "]", galleryButtonsList));
					}
				}
			}

			final Map<RibbonElementPriority, Integer> visibleButtonCounts = new HashMap<RibbonElementPriority, Integer>();
			visibleButtonCounts.put(RibbonElementPriority.LOW, 3);
			visibleButtonCounts.put(RibbonElementPriority.MEDIUM, 3);
			
			visibleButtonCounts.put(RibbonElementPriority.TOP, 3);
			
			band.addRibbonGallery(galleryName, galleryButtons,
						visibleButtonCounts, 3, 3, RibbonElementPriority.TOP);
		}
	}

	private void updateTerminalRibbonGallery(final String galleryName,
			final JRibbonBand band) {
		boolean updating=false;
		if (band != null && band.getControlPanel() != null) {
			if (band.getControlPanel().getRibbonGallery(galleryName) != null) {
				updating=true;
			}
		}

		if(updating){
			SwingWorker worker=new SwingWorker(){
				protected HashMap<TerminalType,ImageResizableIconReducedMem> map;
				
				@Override
				protected Object doInBackground() throws Exception {
					map=getCachedTerminalImages();
					return null;
				}

				@Override
				protected void done(){
					updateTerminalRibbonGallery(galleryName,band,map);
				}
			};
			
			worker.execute();
		}else{
			updateTerminalRibbonGallery(galleryName,band,getCachedTerminalImages());
		}
	}

	/**
	 * Create original utility
	 * */
	private JMenu createDebugMenu() {
		return this.a_oCommand.createDebugMenu(getTheActionManager());
	}
	
	/**
	 * Initialise the structure selection ribbon band.
	 * 
	 * @return
	 */
	public JRibbonBand createStructureRibbonBand() {
		structureSelectionBand = new JRibbonBand(
				STRUCTURE_GALLERY_NAME,
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(10));
		
		ArrayList<RibbonBandResizePolicy> resizePolicies = new ArrayList<RibbonBandResizePolicy>();
		resizePolicies.add(new CoreRibbonResizePolicies.Mirror(
				structureSelectionBand.getControlPanel()));
		resizePolicies.add(new CoreRibbonResizePolicies.Mid2Low(
				structureSelectionBand.getControlPanel()));
		resizePolicies.add(new IconRibbonBandResizePolicy(
				structureSelectionBand.getControlPanel()));

		structureSelectionBand.setResizePolicies(resizePolicies);
		
		updateStructureRibbonGallery(STRUCTURE_GALLERY_NAME, structureSelectionBand);

		structureSelectionBand.addCommandButton(getTheActionManager().get(
				"addstructurestr").getJCommandButton(ICON_SIZE.L3, "Import glycan sequence",
				this, new RichTooltip(" ", "Import structure from string")),
				RibbonElementPriority.TOP);
		structureSelectionBand.addCommandButton(getTheActionManager().get(
				"getstructurestr").getJCommandButton(ICON_SIZE.L3, "Export glycan sequence",
				this, new RichTooltip(" ", "Get string from structure")), 
				RibbonElementPriority.TOP);
		
		structureSelectionBand.addCommandButton(getTheActionManager().get(
				"addcomposition").getJCommandButton(ICON_SIZE.L3,
				"Create composition", this, new RichTooltip("Open", " ")),
				RibbonElementPriority.TOP);

		return structureSelectionBand;
	}

	private RibbonTask createStructureRibbonTask() {
		//structureRibbonBandCFG = createStructureRibbonBand();
		structureRibbonBandSNFG = createStructureRibbonBand();
		JRibbonBand band1 = createAddResidueBand();
		RibbonTask task = new RibbonTask("Structure", structureRibbonBandSNFG,
				band1,createAddTerminalRibbon());
		//RibbonTask task = new RibbonTask("Structure", structureRibbonBandCFG,
		//		band1,createAddTerminalRibbon());

		return task;
	}

	/*private JRibbonBand createStructureRibbonControls() {
		JRibbonBand band = new JRibbonBand(
				"Edit glycan",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(10));
		ThemeManager.setDefaultResizePolicy(band);

		band.addCommandButton(getTheActionManager().get("bracket").getJCommandButton(
				ICON_SIZE.L3, "Bracket", this, new RichTooltip("Add bracket ", "Insert bracket")
				),
				RibbonElementPriority.TOP);
		band.addCommandButton(getTheActionManager().get("repeat").getJCommandButton(
				ICON_SIZE.L3, "Repeat", this,
				new RichTooltip("Add repeat", "Add repeat unit")),
				RibbonElementPriority.TOP);
		band.addCommandButton(getTheActionManager().get("cyclic").getJCommandButton(
				ICON_SIZE.L3, "Cyclic", this,
				new RichTooltip("Add cyclic", "Add cyclic unit")),
				RibbonElementPriority.TOP);
		band.addCommandButton(getTheActionManager().get("moveccw").getJCommandButton(
				ICON_SIZE.L3, "Rotate CCW", this, 
				new RichTooltip("Rotate residue", "Rotate residue counter-clockwise")),
				RibbonElementPriority.TOP);
		band.addCommandButton(getTheActionManager().get("movecw").getJCommandButton(
				ICON_SIZE.L3, "Rotate CW", this, 
				new RichTooltip("Rotate residue", "Rotate residue clockwise")),
				RibbonElementPriority.TOP);

		band.addCommandButton(getTheActionManager().get("properties").getJCommandButton(
				ICON_SIZE.L3, "Properties", this,
				new RichTooltip("Residue Properties", "Get residue properties")),
				RibbonElementPriority.TOP);
		band.addCommandButton(getTheActionManager().get("massoptstruct").getJCommandButton(
				ICON_SIZE.L3, "Mass options", this,
				new RichTooltip("Mass options", "Mass options of selected structure")),
				RibbonElementPriority.TOP);

		return band;
	}*/

	private JRibbonBand createAddTerminalRibbon() {

		terminalRibbonBand = new JRibbonBand(
				"Add Terminal",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(10));

		ArrayList<RibbonBandResizePolicy> resizePolicies = new ArrayList<RibbonBandResizePolicy>();
		resizePolicies.add(new CoreRibbonResizePolicies.Mirror(
				terminalRibbonBand.getControlPanel()));
		resizePolicies.add(new CoreRibbonResizePolicies.Mid2Low(
				terminalRibbonBand.getControlPanel()));
		resizePolicies.add(new IconRibbonBandResizePolicy(terminalRibbonBand
				.getControlPanel()));
		terminalRibbonBand.setResizePolicies(resizePolicies);
		updateTerminalRibbonGallery(TERMINAL_GAL_NAME, terminalRibbonBand);

		return terminalRibbonBand;
	}

	private JRibbonBand createAddResidueBand() {
		final GlycanCanvas self = this;

		insertResidueJRibbonBand = new JRibbonBand(
				"Add residue",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(10));

		ArrayList<RibbonBandResizePolicy> resizePolicies = new ArrayList<RibbonBandResizePolicy>();
		resizePolicies.add(new CoreRibbonResizePolicies.Mirror(
				insertResidueJRibbonBand.getControlPanel()));
		resizePolicies.add(new CoreRibbonResizePolicies.Mid2Low(
				insertResidueJRibbonBand.getControlPanel()));
		resizePolicies.add(new IconRibbonBandResizePolicy(
				insertResidueJRibbonBand.getControlPanel()));

		insertResidueJRibbonBand.setResizePolicies(resizePolicies);
		updateAddResidueRibbonGallery(RESIDUE_GALLERY_NAME, insertResidueJRibbonBand);

		return insertResidueJRibbonBand;
	}

	private JMenu createEditMenu() {
		return this.a_oCommand.createEditMenu(getTheActionManager());
	}

	private JMenu createStructureMenu() {

		JMenu structure_menu = new JMenu("Structure");
		structure_menu.setMnemonic(KeyEvent.VK_S);

		structure_menu.add(getTheActionManager().get("addcomposition"));
		structure_menu.add(getTheActionManager().get("addstructurestr"));
		structure_menu.add(getTheActionManager().get("getstructurestr"));
		structure_menu.add(createAddStructureMenu());

		structure_menu.addSeparator();

		structure_menu.add(createAddResidueMenu());
		structure_menu.add(createAddTerminalMenu());
		structure_menu.add(createInsertResidueMenu());
		structure_menu.add(createChangeResidueTypeMenu());
		structure_menu.add(createInsertBridgeMenu());
		
		bracketMenuItem=structure_menu.add(getTheActionManager().get("bracket"));
		repeatMenuItem=structure_menu.add(getTheActionManager().get("repeat"));
		antennaParentItem=structure_menu.add(getTheActionManager().get("antennaParent"));
		cyclicMenuItem = structure_menu.add(getTheActionManager().get("cyclic"));
		
		structure_menu.addSeparator();

		structure_menu.add(getTheActionManager().get("properties"));
		structure_menu.add(a_oCommand.createChangeReducingEndMenu(this.getTheActionManager()));//getTheActionManager().get("changeredend="));
		structure_menu.add(getTheActionManager().get("massoptstruct"));

		structure_menu.addSeparator();

		structure_menu.add(getTheActionManager().get("moveccw"));
		//structure_menu.add(theActionManager.get("resetplace"));
		structure_menu.add(getTheActionManager().get("movecw"));

		return structure_menu;
	}

	/**
	 * Return a popup menu to be used with this component
	 */
	public JPopupMenu createPopupMenu() {
		return createPopupMenu(true);
	}
	
	protected HashMap<String,JPopupMenu> cachedMenus=new HashMap<String,JPopupMenu>();
	
	/**
	 * Return a popup menu to be used with this component
	 * 
	 * @param change_properties
	 *            <code>true</code> if the actions to change the properties of
	 *            the current residue should be added to the menu
	 */
	public JPopupMenu createPopupMenu(boolean change_properties) {
		StringBuffer buf=new StringBuffer();
		if (!hasCurrentSelection()) buf.append("0");
		else buf.append("1");
		
		if (!hasCurrentLinkage()) buf.append("0");
		else buf.append("1");
		
		if (hasCurrentResidue()) buf.append("1");
		else buf.append("0");
		
		if (change_properties) buf.append("1");
		else buf.append("0");
		
		if (hasCurrentSelection()) buf.append("1");
		else buf.append("0");
		
		String key=buf.toString();
		if(cachedMenus.containsKey(key)) return cachedMenus.get(key);
		
		JPopupMenu menu = new JPopupMenu();
		menu.setDoubleBuffered(true);

		// edit actions
		menu.add(getTheActionManager().get("cut"));
		menu.add(getTheActionManager().get("copy"));
		menu.add(getTheActionManager().get("paste"));
		menu.add(getTheActionManager().get("delete"));
		menu.addSeparator();

		// add actions
		if (!hasCurrentSelection()) {
			menu.add(getTheActionManager().get("addcomposition"));
			menu.add(createAddStructureMenu());
		}
		if (!hasCurrentLinkage()) {
			if(addResidueMenu==null) addResidueMenu=createAddResidueMenu();
			menu.add(addResidueMenu);
			
			if(addTerminalMenu==null) addTerminalMenu=createAddTerminalMenu();
			menu.add(addTerminalMenu);
		}

		// modify structure
		if (hasCurrentResidue()) {
			if(insertResidueMenu==null) insertResidueMenu=createInsertResidueMenu();
			menu.add(insertResidueMenu);
			
			if(changeResidueMenu==null) changeResidueMenu=createChangeResidueTypeMenu();
			menu.add(changeResidueMenu);
			
			// menu.add(createChangeRedEndMenu());
			menu.add(getTheActionManager().get("bracket"));
			menu.add(getTheActionManager().get("repeat"));
			menu.add(getTheActionManager().get("cyclic"));

			if (change_properties) {
				menu.addSeparator();
				menu.add(getTheActionManager().get("properties"));
				menu.add(getTheActionManager().get("changeredend="));
				menu.add(getTheActionManager().get("massoptstruct"));
			}
		}

		// visual placement
		if (hasCurrentSelection()) {
			menu.addSeparator();
			menu.add(getTheActionManager().get("moveccw"));
			//menu.add(theActionManager.get("resetplace"));
			menu.add(getTheActionManager().get("movecw"));
		}
		
		cachedMenus.put(key,menu);
		
		return menu;
	}

	private JToolBar createToolBarDocument() {
		return this.a_oCommand.createToolBarDocument(getTheActionManager());
	}

	private JToolBar createToolBarStructure() {

		JToolBar toolbar = new JToolBar(){
			@Override
			public Dimension getPreferredSize() {
				int width=getParent().getWidth();
				int maxHeight=Integer.MIN_VALUE;
				int runningWidth=0;
				for(Component component:getComponents()){
					if(component.getHeight()>maxHeight) maxHeight=component.getHeight();
					runningWidth+=component.getWidth()+5;
				}
				
				if(width>0){
					int rows=(int)runningWidth/width;
					if(runningWidth % width>0) rows++;
					return new Dimension(width, (rows*maxHeight)+(rows*6));
				}else{
					return super.getPreferredSize();
				}
			}
		};
		toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		toolbar.setVisible(true);
		toolbar.setFloatable(false);
		for (Iterator<ResidueType> i = ResidueDictionary.directResidues().iterator(); i.hasNext();) {
			ResidueType t = i.next();
			if (t.canHaveParent()) toolbar.add(getTheActionManager().get("add=" + t.getName()));
		}

		toolbar.addSeparator();

		// recent residues
		recent_residues_index = toolbar.getComponentCount();
		no_recent_residues_buttons = 0;
		updateRecentResiduesToolbar(toolbar);
		toolbar.addSeparator();
		bracketButton=toolbar.add(getTheActionManager().get("bracket"));
		repeatButton=toolbar.add(getTheActionManager().get("repeat"));
		cyclicButton=toolbar.add(getTheActionManager().get("cyclic"));
		//altButton = toolbar.add(getTheActionManager().get("alternative"));
		toolbar.add(getTheActionManager().get("properties"));
		
		this.antennaParentButton=toolbar.add(getTheActionManager().get("antennaParent"));
		//toolbar.add(getTheActionManager().get("stringNotation"));
		
		toolbar.addSeparator();
		toolbar.add(getTheActionManager().get("moveccw"));
		//toolbar.add(theActionManager.get("resetplace"));
		toolbar.add(getTheActionManager().get("movecw"));
		toolbar.addSeparator();
		toolbar.add(getTheActionManager().get("orientation"));

		return toolbar;
	}

	private JLabel createLabel(String text, int margin) {
		JLabel ret = new JLabel(text);
		ret.setBorder(new EmptyBorder(0, margin, 0, margin));
		return ret;
	}

	private JToolBar createToolBarProperties() {
		JToolBar toolbar = new JToolBar(){
			
			@Override
			public Dimension getPreferredSize() {
				int width=getParent().getWidth();
			
				int maxHeight=Integer.MIN_VALUE;
				int runningWidth=0;
				for(Component component:getComponents()){
					if(component.getHeight()>maxHeight) maxHeight=component.getHeight();
					runningWidth+=component.getWidth()+6; //was 8, 14
				}
				
				if(width>0){
					int rows=(int)runningWidth/width;
					if(runningWidth % width>0) rows++;
					return new Dimension(width, (rows*maxHeight)+(rows*6));
				}else return super.getPreferredSize();				
//				int maxY=Integer.MIN_VALUE;
//				int height=0;
//				for(Component component:getComponents()){
//					if(component.getY()>maxY){
//						maxY=component.getY();
//						height=component.getHeight();
//					}
//				}
//				
//				SwingUtilities.invokeLater(new Runnable(){
//					public void run(){
//						getParent().repaint();
//					}
//				});
//				
//				System.out.println("h="+(maxY+height+30));
//				
//				return new Dimension(getParent().getWidth(), maxY+height+30);
			}
		};
		toolbar.setFloatable(false);
		toolbar.setLayout(new FlowLayout(FlowLayout.LEFT){
			
		});

		toolbar.add(createLabel("Linkage", 5));
		toolbar.add(field_anomeric_state = new JComboBox(new String[] { "?", "a", "b"}));
		toolbar.add(field_anomeric_carbon = new JComboBox(new String[] { "?", "1", "2", "3" }));
		toolbar.add(createLabel("->", 1));
		toolbar.add(field_linkage_position = new DropDownList(new String[] {
				"?", "1", "2", "3", "4", "5", "6", "7", "8", "9" }));
		toolbar.add(createLabel("Chirality", 5));
		toolbar.add(field_chirality = new JComboBox(new String[] { "?", "D", "L" }));
		toolbar.add(createLabel("Ring", 5));
		toolbar.add(field_ring_size = new JComboBox(new String[] { "?", "p", "f", "o" }));
		toolbar.add(createLabel("2nd bond", 5));
		toolbar.add(field_second_bond = new JCheckBox(""));
		toolbar.add(field_second_child_position = new JComboBox(new String[] { "?", "1", "2", "3" }));
		toolbar.add(createLabel("->", 1));
		toolbar.add(field_second_parent_position = new DropDownList(
						new String[] { "?", "1", "2", "3", "4", "5", "6", "7", "8", "9" }));
		
		field_anomeric_state.setActionCommand("setproperties");
		field_anomeric_carbon.setActionCommand("setproperties");
		field_linkage_position.setActionCommand("setproperties");
		field_chirality.setActionCommand("setproperties");
		field_ring_size.setActionCommand("setproperties");
		field_second_bond.setActionCommand("setproperties");
		field_second_child_position.setActionCommand("setproperties");
		field_second_parent_position.setActionCommand("setproperties");
		
		field_anomeric_state.addActionListener(this);
		field_anomeric_carbon.addActionListener(this);
		field_linkage_position.addActionListener(this);
		field_chirality.addActionListener(this);
		field_ring_size.addActionListener(this);
		field_second_bond.addActionListener(this);
		field_second_child_position.addActionListener(this);
		field_second_parent_position.addActionListener(this);
		
		return toolbar;
	}

	private RibbonContextualTaskGroup createLinkageRibbon() {
		JFlowRibbonBand band1 = new JFlowRibbonBand(
				"Linkage specification",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(10));

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));

		toolbar.add(createLabel("Linkage", 5));
		toolbar.add(field_anomeric_state_r = new JComboBox(new String[] {"?", "a", "b", "o"}));
		toolbar.add(field_anomeric_carbon_r = new JComboBox(new String[] {"?", "1", "2", "3"}));
		toolbar.add(createLabel("->", 1));
		toolbar.add(field_linkage_position_r = 
			new DropDownList(new String[] {"?", "1", "2", "3", "4", "5", "6", "7", "8", "9"}));
		toolbar.add(createLabel("Chirality", 5));
		toolbar.add(field_chirality_r = new JComboBox(new String[] {"?", "D","L"}));
		toolbar.add(createLabel("Ring", 5));
		toolbar.add(field_ring_size_r = new JComboBox(new String[] {"?", "p", "f", "o"}));
		toolbar.add(createLabel("2nd bond", 5));
		toolbar.add(field_second_bond_r = new JCheckBox(""));
		toolbar.add(field_second_child_position_r = new JComboBox(new String[] {"?", "1", "2", "3"}));
		toolbar.add(createLabel("->", 1));
		toolbar.add(field_second_parent_position_r = new DropDownList(
			new String[] { "?", "1", "2", "3", "4", "5", "6", "7", "8", "9" }));

		field_anomeric_state_r.setActionCommand("setproperties_r");
		field_anomeric_carbon_r.setActionCommand("setproperties_r");
		field_linkage_position_r.setActionCommand("setproperties_r");
		field_chirality_r.setActionCommand("setproperties_r");
		field_ring_size_r.setActionCommand("setproperties_r");
		field_second_bond_r.setActionCommand("setproperties_r");
		field_second_child_position_r.setActionCommand("setproperties_r");
		field_second_parent_position_r.setActionCommand("setproperties_r");
		
		field_anomeric_state_r.addActionListener(this);
		field_anomeric_carbon_r.addActionListener(this);
		field_linkage_position_r.addActionListener(this);
		field_chirality_r.addActionListener(this);
		field_ring_size_r.addActionListener(this);
		field_second_bond_r.addActionListener(this);
		field_second_child_position_r.addActionListener(this);
		field_second_parent_position_r.addActionListener(this);
		
		// band1.setLayout(new BorderLayout());
		band1.addFlowComponent(toolbar);

		JRibbonBand band2 = new JRibbonBand(
				"Insert residue",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(10));
		// ArrayList<RibbonBandResizePolicy> resizePolicies = new
		// ArrayList<RibbonBandResizePolicy>();
		// resizePolicies.add(new
		// CoreRibbonResizePolicies.Mirror(insertResidueJRibbonBand.getControlPanel()));
		// resizePolicies.add(new
		// CoreRibbonResizePolicies.Mid2Low(insertResidueJRibbonBand.getControlPanel()));
		// resizePolicies.add(new
		// IconRibbonBandResizePolicy(insertResidueJRibbonBand.getControlPanel()));

		// band2.setResizePolicies(resizePolicies);

		this.updateInsertResidueRibbonGallery("Insert residue", band2);

		JRibbonBand band3 = new JRibbonBand(
				"Add residue",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(10));
		this.updateAddResidueRibbonGallery("Add residue", band3);

		JRibbonBand band4 = new JRibbonBand(
				"Change residue",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(10));
		this.updateChangeResidueRibbonGallery("Change residue", band4);
		RibbonTask task1 = new RibbonTask("Linkage specification", band1);

		return new RibbonContextualTaskGroup("Residue options", Color.GREEN,
				new RibbonTask("Residue input", band2, band3, band4), task1);// ,new //task1
		// RibbonTask("Add residue",band3),new
		// RibbonTask("Change residue",band4));
	}

	// -------------------
	// JComponent

	public Dimension getPreferredSize() {
		return getTheGlycanRenderer().computeSize(all_structures_bbox);
	}

	public Dimension getMinimumSize() {
		return new Dimension(0, 0);
	}

	/**
	 * Return the position manager used by this component to render the
	 * structures
	 */
	public PositionManager getPositionManager() {
		return thePosManager;
	}

	// -------------------
	// clipboard handling

	/**
	 * Return a screenshot of the component
	 */
	public void getScreenshot() {
		ClipUtils.setContents(new GlycanSelection(getTheGlycanRenderer(), theDoc
				.getStructures()));
	}

	/**
	 * Delete the selected residues/structures and copy them to the clipboard
	 */
	public void cut() {
		copy();
		delete();
	}
	
	/**
	 * Copy the selected residues/structures to the clipboard
	 */
	public void copy() {	
		Collection<Glycan> sel = theDoc.extractView(selected_residues);
		ClipUtils.setContents(new GlycanSelection(getTheGlycanRenderer(), sel));
	}

	/**
	 * Copy all selected structures to the clipboard
	 */
	public void copySelectedStructures() {
		ClipUtils.setContents(new GlycanSelection(getTheGlycanRenderer(),
				getSelectedStructures()));
	}

	/**
	 * Copy all structures to the clipboard
	 */
	public void copyAllStructures() {
		ClipUtils.setContents(new GlycanSelection(getTheGlycanRenderer(), theDoc
				.getStructures()));
	}

	/**
	 * Paste the content of the clipboard into the editor
	 */
	public void paste() {
		try {
			Transferable t = ClipUtils.getContents();
			if (t != null
					&& t.isDataFlavorSupported(GlycanSelection.glycoFlavor)) {
				String content = TextUtils.consume((InputStream) t
						.getTransferData(GlycanSelection.glycoFlavor));
				theDoc.addStructures(current_residue, theDoc
						.parseString(content));
			}
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	/**
	 * Delete the selected residues/structures
	 */
	public void delete() {
		Residue new_current = (current_residue != null) ? current_residue
				.getParent() : null;

		// cut
		theDoc.removeResidues(selected_residues);

		// update selection
		if (theDoc.contains(new_current))
			setSelection(new_current, false);
	}

	/**
	 * Copy the selected residues and add them to another residue
	 * 
	 * @param position
	 *            the destination
	 */
	public void copyTo(Residue position) {
		theDoc.copyResidues(position, theBBoxManager
				.getLinkedResidues(position), selected_residues);
	}

	/**
	 * Move the selected residues from their current positions and add them to
	 * another residue
	 * 
	 * @param position
	 *            the destination
	 */
	public void moveTo(Residue position) {
		theDoc.moveResidues(position, theBBoxManager
				.getLinkedResidues(position), selected_residues);
	}

	// -------------------
	// painting

	private void xorRectangle(Point start_point, Point end_point) {
		Graphics g = getGraphics();
		g.setXORMode(Color.white);
		g.setColor(Color.black);

		Rectangle rect = makeRectangle(start_point, end_point);
		g.drawRect(rect.x, rect.y, rect.width, rect.height);
	}

	/**
	 * Return <code>true</code> if the specified residue is displayed around the
	 * border of its parent
	 */
	public boolean isOnBorder(Residue r) {
		return thePosManager.isOnBorder(r);
	}

	protected void paintComponent(Graphics g) {
		if (isOpaque()) { // paint background
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}

		// prepare graphic object
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		// set clipping area
		Rectangle clipRect = new Rectangle();
		g.getClipBounds(clipRect);

		// set scale
		getTheGlycanRenderer().getGraphicOptions().setScale(
				getTheGlycanRenderer().getGraphicOptions().SCALE_CANVAS);

		// draw
		boolean show_masses = is_printing ? getTheGlycanRenderer()
				.getGraphicOptions().SHOW_MASSES : getTheGlycanRenderer()
				.getGraphicOptions().SHOW_MASSES_CANVAS;
		boolean show_redend = is_printing ? getTheGlycanRenderer()
				.getGraphicOptions().SHOW_REDEND : getTheGlycanRenderer()
				.getGraphicOptions().SHOW_REDEND_CANVAS;

		all_structures_bbox = getTheGlycanRenderer().computeBoundingBoxes(theDoc
				.getStructures(), show_masses, show_redend, thePosManager,
				theBBoxManager);
		for (Glycan s : theDoc.getStructures()) {
			getTheGlycanRenderer().paint(new DefaultPaintable(g2d), s, selected_residues,
					selected_linkages, show_masses, show_redend, thePosManager,
					theBBoxManager);
		}
		
		if (!is_printing)
			paintSelection(g2d, show_redend);
		//
		// if(theDoc.has_changed)
		if (this.respondToDocumentChange) {
			this.respondToDocumentChange = false;
			revalidate();
		}

		// dispose graphic object
		g2d.dispose();

		getTheGlycanRenderer().getGraphicOptions().setScale(
				getTheGlycanRenderer().getGraphicOptions().SCALE);
	}

	private void paintSelection(Graphics2D g2d, boolean show_redend) {
		GraphicOptions theGraphicOptions = getTheGlycanRenderer().getGraphicOptions();

		Collection<Glycan> sel_structures = theDoc.findStructuresWith(
				selected_residues, selected_linkages);
		Glycan cur_structure = getCurrentStructure();

		for (Iterator<Glycan> i = theDoc.getStructures().iterator(); i.hasNext();) {
			Glycan s = i.next();
			if (sel_structures.contains(s)) {
				Rectangle bbox = theBBoxManager.getBBox(s, show_redend);
				for (; i.hasNext();) {
					Glycan t = i.next();
					if (sel_structures.contains(t))
						bbox = union(bbox, theBBoxManager.getBBox(t, show_redend));
					else
						break;
				}

				if (bbox != null) {
					g2d.setColor(UIManager.getColor("Table.selectionBackground"));
					g2d.fill(new Rectangle(
							theGraphicOptions.MARGIN_LEFT / 2 - 3, bbox.y, 5,
							bbox.height + theGraphicOptions.MASS_TEXT_SPACE
									+ theGraphicOptions.MASS_TEXT_SIZE));
				}
			}
		}

		// paint cur_structure
		if (cur_structure != null) {
			Rectangle cur_bbox = theBBoxManager.getBBox(cur_structure, show_redend);
			if (cur_bbox != null) {
				UIManager.getBorder("Table.focusCellHighlightBorder")
						.paintBorder(
								sel_label,
								g2d,
								theGraphicOptions.MARGIN_LEFT / 2 - 3,
								cur_bbox.y,
								5,
								cur_bbox.height
										+ theGraphicOptions.MASS_TEXT_SPACE
										+ theGraphicOptions.MASS_TEXT_SIZE);
			}
		}
	}

	// ---------------
	// selection

	/**
	 * Return the residue displayed at the specified position, or
	 * <code>null</code> if none is there
	 */
	public Residue getResidueAtPoint(Point p) {
		for (Glycan g : theDoc.getStructures()) {
			Residue ret = getResidueAtPoint(g.getRoot(), p);
			if (ret != null) return ret;

			ret = getResidueAtPoint(g.getBracket(), p);
			if (ret != null) return ret;
		}
		return null;
	}

	/**
	 * Return the child of a residue that is displayed at the specified
	 * position, or <code>null</code> if none is there
	 * 
	 * @param r
	 *            the parent
	 */
	public Residue getResidueAtPoint(Residue r, Point p) {
		if (r == null) return null;

		Rectangle cur_bbox = theBBoxManager.getCurrent(r);
		if (cur_bbox != null && cur_bbox.contains(p)) return r;

		for (Linkage l : r.getChildrenLinkages()) {
			Residue ret = getResidueAtPoint(l.getChildResidue(), p);
			if (ret != null) return ret;
		}
		return null;
	}

	/**
	 * Return the linkage displayed at the specified position, or
	 * <code>null</code> if none is there
	 */
	public Linkage getLinkageAtPoint(Point p) {
		for (Glycan g : theDoc.getStructures()) {
			Linkage ret = getLinkageAtPoint(g.getRoot(), p);
			if (ret != null) return ret;

			ret = getLinkageAtPoint(g.getBracket(), p);
			if (ret != null) return ret;
		}
		return null;
	}

	/**
	 * Return the linkage from a residue that is displayed at the specified
	 * position, or <code>null</code> if none is there
	 * 
	 * @param r
	 *            the parent
	 */
	public Linkage getLinkageAtPoint(Residue r, Point p) {
		if (r == null) return null;

		Rectangle cur_bbox = theBBoxManager.getCurrent(r);
		for (Linkage l : r.getChildrenLinkages()) {
			Rectangle child_bbox = theBBoxManager.getCurrent(l.getChildResidue());
			if (cur_bbox != null && child_bbox != null
					&& distance(p, center(cur_bbox), center(child_bbox)) < 4.)
				return l;

			Linkage ret = getLinkageAtPoint(l.getChildResidue(), p);
			if (ret != null) return ret;
		}
		return null;
	}

	/**
	 * Return <code>true</code> if any residue or linkage is selected
	 */
	public boolean hasSelection() {
		return (selected_residues.size() > 0 || selected_linkages.size() > 0);
	}

	/**
	 * Clear the selection
	 */
	public void resetSelection() {
		selected_residues.clear();
		selected_linkages.clear();
		current_residue = null;
		current_linkage = null;

		fireUpdatedSelection(true);
	}

	/**
	 * Return <code>true</code> if any residue or linkage has the focus.
	 * Rectangle selection may not give the focus to a specific residue or
	 * linkage
	 */
	public boolean hasCurrentSelection() {
		return (current_residue != null || current_linkage != null);
	}

	/**
	 * Return the residue with the focus. If a linkage has the focus, the child
	 * residue is returned
	 */
	public Residue getCurrentSelection() {
		if (current_residue != null) return current_residue;
		if (current_linkage != null) return current_linkage.getChildResidue();
		return null;
	}

	/**
	 * Return the structure containing the residue with the focus
	 */
	public Glycan getCurrentStructure() {
		if (current_residue != null)
			return theDoc.findStructureWith(current_residue);
		if (current_linkage != null)
			return theDoc.findStructureWith(current_linkage.getChildResidue());
		return null;
	}

	/**
	 * Return all the structures containing the selected residues and linkages
	 */
	public Collection<Glycan> getSelectedStructures() {
		return theDoc.findStructuresWith(selected_residues, selected_linkages);
	}

	/**
	 * Return <code>true</code> if a residue has the focus
	 */
	public boolean hasCurrentResidue() {
		return (current_residue != null);
	}

	/**
	 * Return the residue with the focus
	 */
	public Residue getCurrentResidue() {
		return current_residue;
	}

	/**
	 * Return all the residues that are shown at the same position of the
	 * residue with the focus
	 */
	public ArrayList<Residue> getLinkedResidues() {
		return theBBoxManager.getLinkedResidues(current_residue);
	}

	private void setCurrentResidue(Residue node) {
		if (node != null) selected_residues.add(node);
		selected_linkages.clear();
		current_residue = node;
		current_linkage = null;

		fireUpdatedSelection(false);
	}

	/**
	 * Return <code>true</code> if a linkage has the focus
	 */
	public boolean hasCurrentLinkage() {
		return (current_linkage != null);
	}

	/**
	 * Return the linkage with the focus
	 */
	public Linkage getCurrentLinkage() {
		return current_linkage;
	}

	/**
	 * Return <code>true</code> if the specified residue is selected
	 */
	public boolean isSelected(Residue node) {
		if (node == null) return false;
		return selected_residues.contains(node);
	}

	/**
	 * Return <code>true</code> if the specified linkage is selected
	 */
	public boolean isSelected(Linkage link) {
		if (link == null) return false;
		return selected_linkages.contains(link);
	}

	/**
	 * Return <code>true</code> if any residue is selected
	 */
	public boolean hasSelectedResidues() {
		return !selected_residues.isEmpty();
	}

	/**
	 * Return <code>true</code> if any linkage is selected
	 */
	public boolean hasSelectedLinkages() {
		return !selected_linkages.isEmpty();
	}
	
	/**
	 * Return a list containing the selected residues
	 */
	public Collection<Residue> getSelectedResiduesList() {
		return selected_residues;
	}

	/**
	 * Return an array containing the selected residues
	 */
	public Residue[] getSelectedResidues() {
		return (Residue[]) selected_residues.toArray(new Residue[0]);
	}

	/**
	 * Select a specific set of residues
	 */
	public void setSelection(Collection<Residue> nodes) {
		if (nodes == null || nodes.isEmpty()) {
			resetSelection();
		} else {
			selected_residues.clear();
			selected_linkages.clear();
			current_residue = null;
			current_linkage = null;

			for (Residue node : nodes) {
				selected_residues.add(node);
				selected_residues.addAll(theBBoxManager.getLinkedResidues(node));
			}

			fireUpdatedSelection(false);
		}
	}

	public void setSelection(Residue node) {
		this.setSelection(node, false);
	}

	/**
	 * Select a specific residue and set the focus on it
	 */
	public void setSelection(Residue node, boolean showResidueControls) {
		if (node == null) resetSelection();
		else {
			selected_residues.clear();
			selected_linkages.clear();
			selected_residues.add(node);
			selected_residues.addAll(theBBoxManager.getLinkedResidues(node));
			current_residue = node;
			current_linkage = null;

			fireUpdatedSelection(showResidueControls);
		}
	}

	public void setSelection(Linkage link) {
		setSelection(link, false);
	}

	/**
	 * Select a specific linkage and set the focus to it
	 */
	public void setSelection(Linkage link, boolean showResidueControls) {
		if (link == null) resetSelection();
		else {
			selected_residues.clear();
			selected_linkages.clear();
			selected_linkages.add(link);
			current_residue = null;
			current_linkage = link;

			fireUpdatedSelection(showResidueControls);
		}
	}

	/**
	 * Make sure that the residue at the specified point is selected
	 */
	public void enforceSelection(Point p) {
		Residue r = getResidueAtPoint(p);
		if (r != null) enforceSelection(r);
		else {
			Linkage l = getLinkageAtPoint(p);
			if (l != null) enforceSelection(l);
			else resetSelection();
		}
	}

	/**
	 * Make sure that the specified residue is selected
	 */
	public void enforceSelection(Residue node) {
		if (isSelected(node)) {
			current_residue = node;
			fireUpdatedSelection(false);
		} else setSelection(node);
	}

	/**
	 * Make sure that the specified linkage is selected
	 */
	public void enforceSelection(Linkage link) {
		if (isSelected(link)) {
			current_linkage = link;
			fireUpdatedSelection(false);
		} else setSelection(link);
	}

	/**
	 * Make sure that something is selected. In case select the first residue of
	 * the first structure if any
	 */
	public boolean enforceSelection() {
		if (!hasCurrentSelection()) {
			if (theDoc.getNoStructures() == 0) return false;
			if (theDoc.getFirstStructure().getRoot() == null) return false;
			setSelection(theDoc.getFirstStructure().getRoot());
		}
		return true;
	}

	/**
	 * Add a list of residues to the selection
	 */
	public void addSelection(Collection<Residue> nodes) {
		if (nodes != null) {
			for (Residue node : nodes) {
				selected_residues.add(node);
				selected_residues.addAll(theBBoxManager.getLinkedResidues(node));
			}

			selected_linkages.clear();
			current_residue = null;
			current_linkage = null;

			fireUpdatedSelection(false);
		}
	}

	/**
	 * Add a residue to the selection
	 */
	public void addSelection(Residue node) {
		if (node != null) {
			selected_residues.add(node);
			selected_residues.addAll(theBBoxManager.getLinkedResidues(node));

			selected_linkages.clear();
			current_residue = node;
			current_linkage = null;

			fireUpdatedSelection(false);
			this.respondToDocumentChange = true;
			repaint();
		}
	}

	/**
	 * Add to the selection all the residues that are between the residue with
	 * the focus and the specified residue
	 */
	public void addSelectionPathTo(Residue node) {
		if (node != null) {
			if (current_residue == null) {
				selected_residues.add(node);
				selected_residues.addAll(theBBoxManager.getLinkedResidues(node));
			} else {
				for (Residue r : Glycan.getPath(current_residue, node)) {
					selected_residues.add(r);
					selected_residues.addAll(theBBoxManager.getLinkedResidues(node));
				}
			}
			selected_linkages.clear();
			current_residue = node;
			current_linkage = null;

			fireUpdatedSelection(false);
		}
	}

	/**
	 * Select all the residues from the structure with the focus
	 */
	public void selectStructure() {
		selected_linkages.clear();
		selected_residues.clear();

		Glycan s = getCurrentStructure();
		if (s != null) {
			selectAll(s.getRoot());
			selectAll(s.getBracket());
		}
		current_linkage = null;

		fireUpdatedSelection(true);
	}

	/**
	 * Select all residues from all structures
	 */
	public void selectAll() {
		for (Iterator<Glycan> i = theDoc.iterator(); i.hasNext();) {
			Glycan structure = i.next();
			selectAll(structure.getRoot());
			selectAll(structure.getBracket());
		}
		selected_linkages.clear();
		current_residue = null;
		current_linkage = null;

		if (theDoc.getFirstStructure() != null)
			current_residue = theDoc.getFirstStructure().getRoot();

		fireUpdatedSelection(true);
	}

	private void selectAll(Residue node) {
		if (node == null) return;

		selected_residues.add(node);
		for (Iterator<Linkage> i = node.iterator(); i.hasNext();)
			selectAll(i.next().getChildResidue());
	}

	private Residue findNearest(Point p, Collection<Residue> nodes) {
		if (p == null) return null;

		Residue best_node = null;
		double best_dist = 0.;
		for (Residue cur_node : nodes) {
			Rectangle cur_rect = theBBoxManager.getCurrent(cur_node);
			if (cur_rect != null) {
				double cur_dist = distance(p, cur_rect);
				if (best_node == null || best_dist > cur_dist) {
					best_node = cur_node;
					best_dist = cur_dist;
				}
			}
		}

		return best_node;
	}

	// ---------------
	// visual structure rearrangement

	private Residue getResidueAfter(Residue parent, Residue current,
			ResAngle cur_pos) {

		ArrayList<Residue> linked = theBBoxManager.getLinkedResidues(current);
		for (int i = parent.indexOf(current) + 1; i < parent.getNoChildren(); i++) {
			Residue other = parent.getChildAt(i);
			if (thePosManager.getRelativePosition(other).equals(cur_pos)
					&& !linked.contains(other))
				return other;
		}
		return null;
	}

	private Residue getResidueBefore(Residue parent, Residue current,
			ResAngle cur_pos) {

		ArrayList<Residue> linked = theBBoxManager.getLinkedResidues(current);
		for (int i = parent.indexOf(current) - 1; i >= 0; i--) {
			Residue other = parent.getChildAt(i);
			if (thePosManager.getRelativePosition(other).equals(cur_pos)
					&& !linked.contains(other))
				return other;
		}
		return null;
	}

	private Residue getFirstResidue(Residue parent, Residue current,
			ResAngle cur_pos) {
		for (int i = 0; i < parent.getNoChildren(); i++) {
			Residue other = parent.getChildAt(i);
			if (thePosManager.getRelativePosition(other).equals(cur_pos)
					&& other != current)
				return other;
		}
		return null;
	}

	private Residue getLastResidue(Residue parent, Residue current,
			ResAngle cur_pos) {
		for (int i = parent.getNoChildren() - 1; i >= 0; i--) {
			Residue other = parent.getChildAt(i);
			if (thePosManager.getRelativePosition(other).equals(cur_pos)
					&& other != current)
				return other;
		}
		return null;
	}

	private void updateAndMantainSelection() {
		if (current_residue != null) {
			Residue old_current = current_residue;
			theDoc.fireDocumentChanged();
			setSelection(old_current);
		} else if (current_linkage != null) {
			Linkage old_current = current_linkage;
			theDoc.fireDocumentChanged();
			setSelection(old_current);
		}
	}

	private void swapAndMantainSelection(Residue r1, Residue r2) {
		if (current_residue != null) {
			Residue old_current = current_residue;
			theDoc.swap(r1, r2);
			setSelection(old_current);
		} else if (current_linkage != null) {
			Linkage old_current = current_linkage;
			theDoc.swap(r1, r2);
			setSelection(old_current);
		}
	}

	/*
	 * public void resetPlacement() { Residue current = getCurrentSelection();
	 * if( current == null ) return;
	 * 
	 * current.resetPreferredPlacement(); updateAndMantainSelection(); }
	 */

	/*
	 * public void toggleSticky() { Residue current = getCurrentSelection(); if(
	 * current == null || current.getPreferredPlacement()==null ) return;
	 * 
	 * current_residue.getPreferredPlacement().toggleSticky();
	 * updateAndMantainSelection(); }
	 */

	private void setPlacement(Residue current, ResiduePlacement new_rp) {
		current.setPreferredPlacement(new_rp);
		for (Residue r : theBBoxManager.getLinkedResidues(current))
			r.setPreferredPlacement(new_rp);
	}

	private void setWasSticky(Residue current, boolean f) {
		current.setWasSticky(f);
		for (Residue r : theBBoxManager.getLinkedResidues(current))
			r.setWasSticky(f);
	}

	private void moveBefore(Residue parent, Residue current, Residue other) {
		parent.moveChildBefore(current, other);
		for (Residue r : theBBoxManager.getLinkedResidues(current)) {
			if (r.getParent() == parent) parent.moveChildBefore(r, other);
		}
	}

	private void moveAfter(Residue parent, Residue current, Residue other) {
		parent.moveChildAfter(current, other);
		for (Residue r : theBBoxManager.getLinkedResidues(current)) {
			if (r.getParent() == parent) parent.moveChildAfter(r, other);
		}
	}

	/**
	 * Move counter-clockwise the display position of the residue with the focus
	 */
	public void onMoveCCW() {
		Residue current = getCurrentSelection();
		if (current == null || current.getParent() == null) return;

		Residue parent = current.getParent();
		ResAngle cur_pos = thePosManager.getRelativePosition(current);

		// try to move the children in the list
		Residue other = getResidueBefore(parent, current, cur_pos);
		if (other != null) {
			moveBefore(parent, current, other);
			updateAndMantainSelection();
			return;
		}

		// set preferred position
		if (!current.hasPreferredPlacement())
			setWasSticky(current, thePosManager.isSticky(current));

		ResAngle new_pos = null;
		ResiduePlacement new_rp = null;
		if (thePosManager.isOnBorder(current)) {
			new_pos = (cur_pos.getIntAngle() == -90) ? new ResAngle(90)
					: new ResAngle(-90);
			new_rp = new ResiduePlacement(new_pos, true, false);
		} else {
			new_pos = (cur_pos.getIntAngle() == -90) ? new ResAngle(90)
					: cur_pos.combine(-45);
			new_rp = (new_pos.getIntAngle() == -90 || new_pos.getIntAngle() == 90) ? new ResiduePlacement(
					new_pos, false, current.getWasSticky())
					: new ResiduePlacement(new_pos, false, false);
		}
		setPlacement(current, new_rp);

		// put residue in the correct order
		other = getLastResidue(parent, current, new_pos);
		moveAfter(parent, current, other);

		updateAndMantainSelection();
	}

	/**
	 * Move clockwise the display position of the residue with the focus
	 */
	public void onMoveCW() {

		Residue current = getCurrentSelection();
		if (current == null || current.getParent() == null) return;

		ResAngle cur_pos = thePosManager.getRelativePosition(current);
		Residue parent = current.getParent();

		// try to move the children in the list
		Residue other = getResidueAfter(parent, current, cur_pos);
		if (other != null) {
			moveAfter(parent, current, other);
			updateAndMantainSelection();
			return;
		}

		// set preferred position
		if (!current.hasPreferredPlacement())
			setWasSticky(current, thePosManager.isSticky(current));

		ResAngle new_pos = null;
		ResiduePlacement new_rp = null;
		if (thePosManager.isOnBorder(current)) {
			new_pos = (cur_pos.getIntAngle() == -90) ? new ResAngle(90)
					: new ResAngle(-90);
			new_rp = new ResiduePlacement(new_pos, true, false);
		} else {
			new_pos = (cur_pos.getIntAngle() == 90) ? new ResAngle(-90)
					: cur_pos.combine(45);
			new_rp = (new_pos.getIntAngle() == -90 || new_pos.getIntAngle() == 90) ? new ResiduePlacement(
					new_pos, false, current.getWasSticky())
					: new ResiduePlacement(new_pos, false, false);
		}
		setPlacement(current, new_rp);

		// put residue in the correct order
		other = getFirstResidue(parent, current, new_pos);
		moveBefore(parent, current, other);

		updateAndMantainSelection();
	}

	// ---------------
	// structure navigation

	/**
	 * Scroll the component to the beginning
	 */
	public void goToStart() {
		JViewport vp = theScrollPane.getViewport();
		vp.setViewPosition(new Point(0, 0));
		vp.setViewPosition(new Point(0, 0));
	}

	/**
	 * Scroll the component to the end
	 */
	public void goToEnd() {
		SwingUtilities.invokeLater(new Runnable(){

			@Override
			public void run() {
				JViewport vp = theScrollPane.getViewport();
				Dimension all = getPreferredSize();
				Dimension view = vp.getExtentSize();

				vp.setViewPosition(new Point(0, all.height - view.height));
			}
		});
	}

	/**
	 * Move the focus to the residue immediately above the current one. If no
	 * residue has the focus select the root of the last structure
	 */
	public void onNavigateUp() {
		Residue current = getCurrentSelection();
		if (current == null) {
			Glycan s = theDoc.getLastStructure();
			if (s != null) setSelection(s.getRoot());
		} else {
			Residue best_node = theBBoxManager.getNearestUp(current);
			if (best_node != null) setSelection(best_node);
		}
	}

	/**
	 * Move the focus to the residue immediately below the current one. If no
	 * residue has the focus select the root of the first structure
	 */
	public void onNavigateDown() {
		Residue current = getCurrentSelection();
		if (current == null) {
			Glycan s = theDoc.getFirstStructure();
			if (s != null) setSelection(s.getRoot());
		} else {
			Residue best_node = theBBoxManager.getNearestDown(current);
			if (best_node != null) setSelection(best_node);
		}
	}

	/**
	 * Move the focus to the residue immediately to the left the current one. If
	 * no residue has the focus select the root of the last structure
	 */
	public void onNavigateLeft() {
		Residue current = getCurrentSelection();
		if (current == null) {
			Glycan s = theDoc.getLastStructure();
			if (s != null) setSelection(s.getRoot());
		} else {
			Residue best_node = theBBoxManager.getNearestLeft(current);
			if (best_node != null) setSelection(best_node);
		}
	}

	/**
	 * Move the focus to the residue immediately to the right the current one.
	 * If no residue has the focus select the root of the first structure
	 */
	public void onNavigateRight() {
		Residue current = getCurrentSelection();
		if (current == null) {
			Glycan s = theDoc.getFirstStructure();
			if (s != null) setSelection(s.getRoot());
		} else {
			Residue best_node = theBBoxManager.getNearestRight(current);
			if (best_node != null) setSelection(best_node);
		}
	}

	// -----------------
	// print

	/**
	 * Print the content of the component
	 */
	public void print(PrinterJob job) throws PrinterException {
		// do something before
		is_printing = true;

		job.print();

		// do something after
		is_printing = false;
	}

	public int print(Graphics g, PageFormat pageFormat, int pageIndex)
			throws PrinterException {
		if (pageIndex > 0) {
			return NO_SUCH_PAGE;
		} else {
			Graphics2D g2d = (Graphics2D) g;
			g2d.setBackground(Color.white);
			g2d.translate(pageFormat.getImageableX(), pageFormat
					.getImageableY());

			Dimension td = this.getPreferredSize();
			double sx = pageFormat.getImageableWidth() / td.width;
			double sy = pageFormat.getImageableHeight() / td.height;
			double s = Math.min(sx, sy);
			if (s < 1.)
				g2d.scale(s, s);

			RepaintManager.currentManager(this)
					.setDoubleBufferingEnabled(false);
			this.respondToDocumentChange = true;
			this.paint(g2d);
			RepaintManager.currentManager(this).setDoubleBufferingEnabled(true);

			return PAGE_EXISTS;
		}
	}

	// ----------------------
	// Edit actions

	/**Original method*/
	public void onAddStringNotation() {
		try {	
			ResidueDesignDialog a_RDD = new ResidueDesignDialog(theParent);
			a_RDD.setVisible(true);
			
			Residue toadd = null;
			String a_strStatus = a_RDD.getReturnStatus();
			if(!a_strStatus.equals("Cancel") && !a_strStatus.equals("Accept")) {
				toadd = new Residue(NonSymbolicResidueDictionary.getResidueType(a_RDD.getReturnStatus()));
			}
			
			/*if(a_strStatus.equals("Accept")) {
				if(a_RDD.getResidueCode().equals(""))
					throw new Exception("Please input the ResidueCode in text box");
				
				ResidueCodetoTrivalName a_objRC2TN = new ResidueCodetoTrivalName();
				a_objRC2TN.start(a_RDD.getResidueCode());
				
				/** ResidueCode to monosaccharide trival name*/
			//	toadd = ResidueDictionary.newResidue(a_objRC2TN.getTrivalName());
			//}

			if(toadd == null && !a_strStatus.equals("Cancel"))
				throw new Exception("This residue can not handle in GlycanBuilder");
			
			if (theDoc.addResidue(getCurrentResidue(), getLinkedResidues(), toadd) != null) {
				setSelection(toadd);
				theWorkspace.getResidueHistory().add(toadd);
			}
			
			this.respondToDocumentChange = true;
			repaint();
		} catch(Exception e) {
			JOptionPane.showMessageDialog(theParent, e.getMessage(),
					"Error while convert ResidueCode",
					JOptionPane.ERROR_MESSAGE);
		}
		
		return;
	}
		
	public void onAntennaParent() {
		try {
			//this.getCurrentResidue().getParentsOfFragment().clear();
			LinkedList<Linkage> linkages =
				getSelectedStructures().iterator().next().getBracket().getChildrenLinkages();

			GlycanUtils glycanUtil = new GlycanUtils();
			glycanUtil.getCoreResidue(this.getSelectedStructures());
			Residue root = glycanUtil.getCoreResidues().getFirst();

			for(Linkage linkage : linkages) {
				if(linkage.getChildResidue().equals(getSelectedResiduesList().iterator().next())) {
					int antennaIndex = linkages.indexOf(linkage);
					getCurrentResidue().setAntennaeID(antennaIndex + 1);
					break;
				}
			}		
			
			ResidueSelectorDialog rsd = new ResidueSelectorDialog(
					theParent, "Select parent node",
					"Select parent node of this antenna", 
					new Glycan(root, true, null),
					glycanUtil.getCoreResidues(), true, getTheGlycanRenderer());
			
			rsd.setVisible(true);
			if (!rsd.isCanceled()) {
				if (rsd.getSelectedResidues().size() == 1) {
					throw new Exception("Core node should be selected more than one monosaccharide.");
				}
				this.getCurrentResidue().getParentsOfFragment().clear();
				for(Residue residue : rsd.getSelectedResidues()) {
					if(!residue.isSaccharide()) continue;
					this.getCurrentResidue().addParentOfFragment(residue);
				}
			} //else {
				//this.getCurrentResidue().getParentsOfFragment().clear();
				//this.getCurrentResidue().setAntennaeID(-1);
			//}
		} catch (Exception e) {
			LogUtils.report(e);
		}
		
		//updateResidue();
		return;
	}	
	/***/
	
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if(e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;
		String url = e.getURL().toString();
		
	}
	
	/**
	 * Restore the state of the underlying document after a change
	 */
	public void onUndo() {
		try {
			theDoc.getUndoManager().undo();
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	/**
	 * Apply again the changes after the state of the underlying document has
	 * been restored
	 */
	public void onRedo() {
		try {
			theDoc.getUndoManager().redo();
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	// ------------
	// Display actions

	/**
	 * Set the mass options of a list of structures. Display a
	 * {@link MassOptionsStructureDialog}
	 */
	public boolean onMassOptionsStructures(Collection<Glycan> structures) {
		// open dialog
		MassOptionsStructureDialog mdlg = new MassOptionsStructureDialog(
				theParent, structures, theWorkspace.getDefaultMassOptions());
		mdlg.pack();
		mdlg.setVisible(true);

		if (mdlg.getReturnStatus().equals("OK")) {
			// set mass options for selected structures
			theDoc.setMassOptions(structures, mdlg.getMassOptions());
			
			// set default mass options
			theWorkspace.getDefaultMassOptions().setValues(mdlg.getMassOptions());
			// update view
			this.respondToDocumentChange = true;
			repaint();

			return true;
		}
		return false;
	}

	/**
	 * Set the mass options of all structures. Display a
	 * {@link MassOptionsStructureDialog}
	 */
	public boolean onMassOptionsAllStructures() {
		return onMassOptionsStructures(theDoc.getStructures());
	}

	/**
	 * Set the mass options of the selected structures. Display a
	 * {@link MassOptionsStructureDialog}
	 */
	public boolean onMassOptionsSelectedStructures() {
		return onMassOptionsStructures(getSelectedStructures());
	}

	/**
	 * Change the orientation of the display
	 */
	public void onChangeOrientation() {
		theWorkspace.getGraphicOptions().ORIENTATION = (theWorkspace
				.getGraphicOptions().ORIENTATION + 1) % 4;
		this.respondToDocumentChange = true;
		repaint();
	}

	/**
	 * Change the cartoon notation to the specified value
	 * 
	 * @see GraphicOptions
	 */
	public void setNotation(String notation) {
		theWorkspace.setNotation(notation);
		this.respondToDocumentChange = true;
		
		onChangeReducingEnd();
		
		repaint();
		updateResidueActions();
		
		addResidueMenu=null;
		addTerminalMenu=null;
		insertResidueMenu=null;
		changeResidueMenu=null;
		
		cachedMenus.clear();
	}

	public void fireNotationChangedEvent(String notation) {
		for (NotationChangeListener notationChangeListener : notationChangeListeners) {
			notationChangeListener.notationChanged(notation);
		}
	}

	/**
	 * Change the display style to the specified value
	 * 
	 * @see GraphicOptions
	 */
	public void setDisplay(String display) {
		theWorkspace.setDisplay(display);
		this.respondToDocumentChange = true;
		repaint();
		updateResidueActions();
	}

	/**
	 * Change the graphic settings. Display a {@link GraphicOptionsDialog}
	 */
	public void onChangeDisplaySettings() {
		GraphicOptions options = theWorkspace.getGraphicOptions();
		GraphicOptionsDialog dlg = new GraphicOptionsDialog(theParent, options);

		dlg.setVisible(true);
		if (dlg.getReturnStatus().equals("OK")) {
			theWorkspace.setDisplay(options.DISPLAY);
			display_button_group.setSelected(display_models
					.get(options.DISPLAY), true);
			this.respondToDocumentChange = true;
			repaint();
			updateResidueActions();
		}
	}

	// --------------------
	// Structure

	/**
	 * Add a glycan composition to the component. Display a
	 * {@link CompositionDialog}
	 */
	public void onAddComposition() {
		try {
			CompositionDialog dlg = new CompositionDialog(theParent,
					theWorkspace.getCompositionOptions());
			dlg.setVisible(true);
			
			if (dlg.getReturnStatus().equals("OK")) {
				Glycan a_oComposition = theWorkspace.getCompositionOptions()
						.getCompositionAsGlycan(theWorkspace.getDefaultMassOptions());
				theDoc.addStructure(a_oComposition);
			}
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	/**
	 * Add a new structure from a core motif
	 * 
	 * @param name
	 *            the identifier of the core motif
	 * @see CoreDictionary
	 */
	public void onAddStructure(String name) {
		try {
			Residue a_oNewStructure = CoreDictionary.newCore(name);
			if(a_oNewStructure.isReducingEnd() && a_oNewStructure.getTypeName().equals("redEnd")) {
				a_oNewStructure.firstChild().setAlditol(true);
				a_oNewStructure.firstChild().setAnomericState('?');
				a_oNewStructure.firstChild().setRingSize('o');
			}
			theDoc.addStructure(a_oNewStructure);
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	/**
	 * Add a new structure from its string representation. Display a
	 * {@link ImportStructureDialog}
	 */
	public void onAddStructureFromString() {
		try {
			ImportStructureDialog dlg = new ImportStructureDialog(theParent, this.getTheActionManager());
			dlg.setVisible(true);

			if (!dlg.isCanceled()) {
				for(String s : dlg.getStringEncoded()) theDoc.importFromString(s, dlg.getStringFormat());
			}
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	/**
	 * Get string from current structure
	 */
	public void onGetStringFromStructure() {
		try {
			OutputStringDialog dig = new OutputStringDialog(this.theParent);
			dig.setVisible(true);
			if(!dig.isCanceled()) {
				if(this.getSelectedStructures().isEmpty())
					this.theDoc.exportFromStructure(this.theDoc.getStructures(), dig.getFormat());
				else
					this.theDoc.exportFromStructure(this.getSelectedStructures(), dig.getFormat());			
			}
			
			if(this.theDoc.getString().size() > 0)	{
				JDialog dialog = new JDialog(this.theParent, "Encode string", true);
				dialog.setSize(300, 80);
				dialog.setResizable(true);
				dialog.setLocationRelativeTo(this);
				
				StringBuilder str = new StringBuilder();
				for(String s : this.theDoc.getString()) str.append(s + "\n");
				
				dialog.add(new JScrollPane(new JTextArea(str.toString())));
				dialog.setVisible(true);
				this.theDoc.clearString();
			}
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}
	
	/**
	 * Add a new structure from a terminal motif
	 * 
	 * @param name
	 *            the identifier of the terminal motif
	 * @see TerminalDictionary
	 */
	public void onAddTerminal(String name) {
		try {
			Residue toadd = TerminalDictionary.newTerminal(name);
			Residue current = getCurrentResidue();
			if (theDoc.addResidue(current, getLinkedResidues(), toadd) != null)
				setSelection(current);
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	/**
	 * Add a bracket residue to the structure with the focus
	 */
	public void onAddBracket() {
		Residue bracket = theDoc.addBracket(getCurrentResidue());
		if (bracket != null) setSelection(bracket);
	}

	/**
	 * Add a repeating unit containing the selected residues. If the end point
	 * of the unit cannot be easily determined a {@link ResidueSelectorDialog}
	 * is shown
	 */
	public void onAddRepeat() {
		try {
			Collection<Residue> nodes = getSelectedResiduesList();
			if (!theDoc.createRepetition(null, nodes)) {
				LinkedList<Residue> end_points = new LinkedList<Residue>();
				for (Residue r : nodes) {
					if (r.isSaccharide()) end_points.add(r);
				}
				
				Glycan structure = getSelectedStructures().iterator().next();
				ResidueSelectorDialog rsd = new ResidueSelectorDialog(
						theParent, "Select ending point",
						"Select ending point of the repetition", structure,
						end_points, false, getTheGlycanRenderer());

				rsd.setVisible(true);
				if (!rsd.isCanceled()) {
					theDoc.createRepetition(rsd.getCurrentResidue(), getSelectedResiduesList());
				}
			}			
		} catch (Exception e) {
			JOptionPane.showMessageDialog(theParent, e.getMessage(),
					"Error while creating the repeating unit",
					JOptionPane.ERROR_MESSAGE);
		}
		
		return;
	}

	private void onAddCyclic() {
		try {
			Collection<Residue> nodes = getSelectedResiduesList();
			if(nodes.size() < 2) 
				throw new Exception("Cyclic structure must be contain two or more monosaccharide");
			
			LinkedList<Residue> end_points = new LinkedList<Residue>();
			Glycan a_oGlycan = getCurrentStructure();
			
			for (Residue r : nodes) {
				if(r.isReducingEnd()) throw new Exception("Add cyclic could not handle reducing end");
				if (r.isSaccharide()) end_points.add(r);
			}
			
			Glycan structure = getSelectedStructures().iterator().next();
			ResidueSelectorDialog rsd = new ResidueSelectorDialog(
					theParent, "Select the last node",
					"Select the last node of the cyclic", structure,
					end_points, false, getTheGlycanRenderer());

			rsd.setVisible(true);
			if (!rsd.isCanceled()) {
				theDoc.createCyclic(rsd.getCurrentResidue(), getSelectedResiduesList());	
				if(!a_oGlycan.getRoot().firstChild().isStartCyclic()) {
					Residue a_oFirst =a_oGlycan.getRoot().firstChild();	
					a_oGlycan.getRoot().getChildrenLinkages().remove(a_oGlycan.getRoot().getLinkageAt(0));
					a_oGlycan.getRoot().addChild(a_oFirst.getStartCyclicResidue());
				}
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(theParent, e.getMessage(),
					"Error while creating the cyclic unit",
					JOptionPane.ERROR_MESSAGE);
		}
	
		return;
	}
	
	/**
	 * Add a new child to the residue with the focus.
	 * 
	 * @param sacc_name
	 *            the type name of the new residue to add
	 */
	public void onAdd(String sacc_name) {
		try {
			Residue currentResidue = this.getCurrentResidue();

			if(currentResidue != null) {
				if(currentResidue.getType().getSuperclass().equals("Bridge"))
					throw new Exception(currentResidue.getTypeName() + " is bridge node" );
				if(currentResidue.isEndCyclic() || currentResidue.isStartCyclic())
					throw new Exception(currentResidue.getTypeName() + " could not add any residue");
			}
			
			Residue toadd = ResidueDictionary.newResidue(sacc_name);
			if (theDoc.addResidue(getCurrentResidue(), getLinkedResidues(), toadd) != null) {
				setSelection(toadd);
				theWorkspace.getResidueHistory().add(toadd);
			}

			if (currentResidue != null && currentResidue.isBracket()) {
				GlycanUtils glycanUtils = new GlycanUtils();
				glycanUtils.getCoreResidue(this.getSelectedStructures());

				LinkedList<Residue> core = glycanUtils.getCoreResidues();
				for(Residue coreResidue : core) {
					this.getCurrentResidue().addParentOfFragment(coreResidue);
				}
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(theParent, e.getMessage(), "Error while add residue", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Insert a new residue between the residue with the focus and its parent
	 * 
	 * @param sacc_name
	 *            the type name of the new residue to add
	 */
	public void onInsertBefore(String sacc_name) {
		try {
			Residue toinsert = ResidueDictionary.newResidue(sacc_name);
			Residue current = getCurrentResidue();
			if (theDoc.insertResidueBefore(current, getLinkedResidues(),
					toinsert) != null) {
				setSelection(toinsert);
				theWorkspace.getResidueHistory().add(toinsert);
			}
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	/**
	 * Change the type of the residue with the focus
	 * 
	 * @param sacc_name
	 *            the new type name
	 */
	public void onChangeResidueType(String sacc_name) {
		try {
			ResidueType new_type = ResidueDictionary.getResidueType(sacc_name);
			Residue current = getCurrentResidue();
			if (theDoc.changeResidueType(current, getLinkedResidues(), new_type)) {
				setSelection(current);
				theWorkspace.getResidueHistory().add(current);
			}
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	/**
	 * Change the reducing end of the structure with the focus
	 * 
	 * @param sacc_name
	 *            the type name of the new reducing end marker
	 */
	public void onChangeReducingEnd(String sacc_name) {
		try {
			if(this.getCurrentResidue() == null || !this.getCurrentResidue().isReducingEnd()) 
				throw new Exception("This utility need to select reducing end");
					
			TextSymbolDescriptor a_enumRedType = TextSymbolDescriptor.forRedType(sacc_name);
			
			ResidueType new_type = ResidueDictionary.findResidueType(a_enumRedType.toString());
			Residue current = getCurrentResidue();
			theDoc.changeReducingEndType(current, new_type);
			
			if(a_enumRedType.equals(TextSymbolDescriptor.REDEND)) {
				if(getTheGlycanRenderer().getGraphicOptions().NOTATION.equals(GraphicOptions.NOTATION_SNFG)) {
					current.firstChild().setAlditol(true);
					current.firstChild().setRingSize('o');
					current.firstChild().setAnomericState('?');
				}	
			} else {
				current.firstChild().setAlditol(false);
				if(current.firstChild().getRingSize() == 'o') {
					current.firstChild().setRingSize('?');
					current.firstChild().setAnomericState('?');
				}
			}
			
			//MassOptions a_oMass = new MassOptions(true);
			//a_oMass = a_oCurrent.getMassOptions().clone();
			//a_oMass.setReducingEndType(new_type);
			//theWorkspace.theMassOptions.setReducingEndType(new_type);
			
			//theDoc.setMassOptions(getSelectedStructures(), a_oMass);
			//theWorkspace.getDefaultMassOptions().setValues(a_oMass);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(theParent, e.getMessage(),
					"Error in Change reducing end type",
					JOptionPane.ERROR_MESSAGE);
		}
		
		return;
	}
	
	private void onChangeReducingEnd() {
		for(Glycan a_oGlycan : theDoc.getStructures()) {
			Residue a_oRoot = a_oGlycan.getRoot();

			if(theGlycanRenderer.getGraphicOptions().NOTATION.equals(GraphicOptions.NOTATION_SNFG)) {
				if(a_oRoot.getChildAt(0).isAlditol()) a_oRoot.getChildAt(0).setAnomericState('o');
			}
		}
		
		return;
	}

	/**
	 * Change the properties of the residue with the focus. Display a
	 * {@link ResiduePropertiesDialog} or a {@link RepetitionPropertiesDialog}
	 * depending of the type of the residue.
	 */
	public void onProperties() {
		Residue current = getCurrentResidue();
		if (current == null)
			return;
		if (current.getParent() != null
				&& (!current.isSpecial() || current.isCleavage())) {
			new ResiduePropertiesDialog(theParent, current,
					getLinkedResidues(), theDoc).setVisible(true);
			setSelection(current);
			this.respondToDocumentChange = true;
			repaint();
		} else if (current.isStartRepetition()) {
			new ResiduePropertiesDialog(theParent, current, theDoc).setVisible(true);
			setSelection(current);
			this.respondToDocumentChange = true;
			repaint();
		} else if (current.isEndRepetition()) {
			new RepetitionPropertiesDialog(theParent, current, theDoc).setVisible(true);
			setSelection(current);
			this.respondToDocumentChange = true;
			repaint();
		} else if (current.isEndCyclic()) {
			new ResiduePropertiesDialog(theParent, current,
					getLinkedResidues(), theDoc).setVisible(true);
			current.getStartResidue().getParentLinkage().getBonds().get(0).setParentPosition(current.getParentLinkage().getParentPositionsSingle());
			setSelection(current);
			this.respondToDocumentChange = true;
		}
	}

	private char getSelectedValueChar(JComboBox field) {
		if (field.getSelectedItem() == null)
			return '?';
		return ((String) field.getSelectedItem()).charAt(0);
	}

	private char[] getSelectedPositions(DropDownList field) {
		Object[] sel = field.getSelectedValues();
		if (sel.length == 0)
			return new char[] { '?' };

		char[] ret = new char[sel.length];
		for (int i = 0; i < sel.length; i++)
			ret[i] = ((String) sel[i]).charAt(0);
		return ret;
	}

	/**
	 * Set the properties of the residue with the focus using the values in the
	 * properties toolbar
	 * 
	 * @see #getToolBarProperties
	 */
	public void onSetProperties() {
		Residue current = getCurrentResidue();
		if (current != null) {
			current.setAnomericState(getSelectedValueChar(field_anomeric_state));
			current.setAnomericCarbon(getSelectedValueChar(field_anomeric_carbon));
			current.setChirality(getSelectedValueChar(field_chirality));
			current.setRingSize(getSelectedValueChar(field_ring_size));
			Linkage parent_linkage = current.getParentLinkage();
			if (parent_linkage != null) {
				int a_iProbabilityHigh = parent_linkage.getBonds().get(0).getProbabilityHigh();
				int a_iProbabilityLow = parent_linkage.getBonds().get(0).getProbabilityLow();
				
				char[] sel_linkage_positions = getSelectedPositions(field_linkage_position);
				if (field_second_bond.isSelected()) {
					char[] sel_second_parent_positions = getSelectedPositions(field_second_parent_position);
					char sel_second_child_position = getSelectedValueChar(field_second_child_position);
					parent_linkage.setLinkagePositions(sel_linkage_positions,
							sel_second_parent_positions,
							sel_second_child_position);
					//current.setAldehyde(true);
					//current.setRingSize('o');
				} else
					parent_linkage.setLinkagePositions(sel_linkage_positions);
				
				parent_linkage.getBonds().get(0).setProbabilityHigh(a_iProbabilityHigh);
				parent_linkage.getBonds().get(0).setProbabilityLow(a_iProbabilityLow);
			}
			
			if(current.isSaccharide() && !GlycanUtils.isFacingAnom(current)) {
				if(!current.isAlditol() && getSelectedValueChar(field_ring_size) == 'o') {
					current.setAlditol(true);
					field_anomeric_state.setSelectedItem("" + '?');
					current.setAnomericState('?');
				}
				if(current.isAlditol() && (getSelectedValueChar(field_anomeric_state) != '?' || getSelectedValueChar(field_ring_size) != 'o')) {
					current.setAlditol(false);
					current.setRingSize(current.getRingSize() == 'o' ? '?' : current.getRingSize());
				}
			}
			
			theDoc.fireDocumentChanged();
			setSelection(current);
			this.respondToDocumentChange = true;
			repaint();
		}
	}
	
	/**
	 * Set the properties of the residue with the focus using the values in the
	 * properties toolbar
	 * 
	 * @see #getToolBarProperties
	 */
	public void onSetProperties_r() {

		Residue current = getCurrentResidue();
		if (current != null) {
			current.setAnomericState(getSelectedValueChar(field_anomeric_state_r));
			current.setAnomericCarbon(getSelectedValueChar(field_anomeric_carbon_r));
			current.setChirality(getSelectedValueChar(field_chirality_r));
			current.setRingSize(getSelectedValueChar(field_ring_size_r));
			
			Linkage parent_linkage = current.getParentLinkage();
			if (parent_linkage != null) {
				int a_iProbabilityHigh = parent_linkage.getBonds().get(0).getProbabilityHigh();
				int a_iProbabilityLow = parent_linkage.getBonds().get(0).getProbabilityLow();
				
				char[] sel_linkage_positions = getSelectedPositions(field_linkage_position_r);
				if (field_second_bond_r.isSelected()) {
					char[] sel_second_parent_positions = getSelectedPositions(field_second_parent_position_r);
					char sel_second_child_position = getSelectedValueChar(field_second_child_position_r);
					parent_linkage.setLinkagePositions(sel_linkage_positions,
							sel_second_parent_positions,
							sel_second_child_position);
				} else
					parent_linkage.setLinkagePositions(sel_linkage_positions);
				
				parent_linkage.getBonds().get(0).setProbabilityHigh(a_iProbabilityHigh);
				parent_linkage.getBonds().get(0).setProbabilityLow(a_iProbabilityLow);
			}
			
			theDoc.fireDocumentChanged();
			setSelection(current);
			this.respondToDocumentChange = true;
			repaint();
		}
	}

	/**
	 * Specify if the reducing end marker should be visible in the component
	 */
	public void setShowRedendCanvas(boolean f) {
		theWorkspace.getGraphicOptions().SHOW_REDEND_CANVAS = f;
		getTheActionManager().get("showredendcanvas").setSelected(f);
		this.respondToDocumentChange = true;
		repaint();
	}

	/**
	 * Insert bridge substituent
	 * @throws Exception 
	 * */
	public void insertCrossLinkedSubstituent(String a_sNodeName) {
		try {
			if(this.getSelectedResiduesList().size() > 2)
				throw new Exception("This utility is need to select one or two monosaccharide");

			if(this.getSelectedResiduesList().size() == 1) {
				Residue a_oParent = getSelectedResidues()[0];
				if(!a_oParent.isSaccharide()) throw new Exception(a_oParent.getType().getDescription() + " can not insert cross-linked substituent");
				
				Residue a_oBridge = new Residue(ResidueDictionary.getResidueType(a_sNodeName));
				a_oBridge.setParentLinkage(new Linkage(a_oParent, a_oBridge, new char[] {'?'}, new char[] {'?'}, a_oBridge.getAnomericCarbon()));
				a_oParent.addChild(a_oBridge, a_oBridge.getParentLinkage().getBonds());
			}
			
			if(this.getSelectedResiduesList().size() == 2) {
				Object[] a_aResidues = getSelectedResidues();
				Residue a_oStart = (Residue) a_aResidues[0];
				Residue a_oEnd = (Residue) a_aResidues[1];

				if(a_oStart.getParent().equals(a_oEnd)) {				
					a_oStart = (Residue) a_aResidues[1];
					a_oEnd = (Residue) a_aResidues[0];				
				}

				if(a_oStart.getType().getSuperclass().equals("Bridge"))
					throw new Exception(a_oStart.getTypeName() + " is cross-linked substituent");
				if(a_oEnd.getType().getSuperclass().equals("Bridge"))
					throw new Exception(a_oEnd.getTypeName() + " is cross-linked substituent");

				char a_cParentPos = a_oEnd.getParentLinkage().getParentPositionsSingle();
				Residue a_oBridge = new Residue(CrossLinkedSubstituentDictionary.getCrossLinkedSubstituent(a_sNodeName));

				a_oEnd.getParentLinkage().setSubstituent(a_oBridge);			
				a_oStart.getChildrenLinkages().remove(a_oStart.getChildrenLinkages().indexOf(a_oEnd.getParentLinkage()));

				a_oBridge.setParentLinkage(new Linkage(a_oStart, a_oBridge, a_cParentPos));	
				a_oEnd.setParentLinkage(new Linkage(a_oBridge, a_oEnd, '1'));
				a_oStart.addChild(a_oBridge, a_oBridge.getParentLinkage().getBonds());
				a_oBridge.addChild(a_oEnd, a_oEnd.getParentLinkage().getBonds());
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(theParent, e.getMessage(),
					"Error while insert bridge",
					JOptionPane.ERROR_MESSAGE);
		}

		return;
	}
	
	private void onAddALternative() {
		if(this.getCurrentResidue().hasChildren()) {
			JOptionPane.showMessageDialog(theParent, "This utility need to select non-reducing end", 
					"Error in \"Add alternatice unit\"", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// TODO Auto-generated method stub
		//Residue a_oCurrent = this.getCurrentResidue();
		Residue a_oStartAlt = ResidueDictionary.createAlternativeStart();
		Residue a_oEndAlt = ResidueDictionary.createAlternativeEnd();
		
		//this.getCurrentStructure().addBracket();
		//this.getCurrentStructure().getBracket().setAlternativeEnd(a_oEndAlt);
		//this.getCurrentStructure().setAlt(a_oStartAlt);
		//a_oStartAlt.addChild(a_oEndAlt);
		
		//a_oCurrent.addChild(a_oStartAlt);
		
		return;
	}

	// ---------------
	// events

	public void actionPerformed(ActionEvent e) {
		if (ignore_actions) return;
	
		CanvasActionDescriptor a_enumAction = 
				CanvasActionDescriptor.forActions(GlycanAction.getAction(e));
		String param = GlycanAction.getParam(e);
		
		// editing
		if(a_enumAction.equals(CanvasActionDescriptor.STRINGNOTATION)) onAddStringNotation();
		if(a_enumAction.equals(CanvasActionDescriptor.ANTENNAPARENT)) onAntennaParent();
		if(a_enumAction.equals(CanvasActionDescriptor.UNDO)) onUndo();
		if(a_enumAction.equals(CanvasActionDescriptor.REDO)) onRedo();
		if(a_enumAction.equals(CanvasActionDescriptor.CUT)) cut();
		if(a_enumAction.equals(CanvasActionDescriptor.COPY)) copy();
		if(a_enumAction.equals(CanvasActionDescriptor.PASTE)) paste();
		if(a_enumAction.equals(CanvasActionDescriptor.DELETE)) delete();
		if(a_enumAction.equals(CanvasActionDescriptor.SELECTSTRUCTURE)) selectStructure();
		if(a_enumAction.equals(CanvasActionDescriptor.SELECTALL)) selectAll();
		if(a_enumAction.equals(CanvasActionDescriptor.SELECTNONE)) resetSelection();
		if(a_enumAction.equals(CanvasActionDescriptor.GOTOSTART)) {
			resetSelection();
			goToStart();			
		}
		if(a_enumAction.equals(CanvasActionDescriptor.GOTOEND)) {
			resetSelection();
			goToEnd();
		}
		if(a_enumAction.equals(CanvasActionDescriptor.ORDERSTRUCTURESASC)) theDoc.orderStructures(false);
		if(a_enumAction.equals(CanvasActionDescriptor.ORDERSTRUCTURESDESC)) theDoc.orderStructures(true);

		// structure
		if(a_enumAction.equals(CanvasActionDescriptor.ADDTERMINAL)) onAddTerminal(param);
		if(a_enumAction.equals(CanvasActionDescriptor.ADDCOMPOSITON)) onAddComposition();
		if(a_enumAction.equals(CanvasActionDescriptor.ADDSTRUCTURE)) onAddStructure(param);
		if(a_enumAction.equals(CanvasActionDescriptor.ADDSTRUCTURESTR)) onAddStructureFromString();
		if(a_enumAction.equals(CanvasActionDescriptor.GETSTRUCTURESTR)) onGetStringFromStructure();
		if(a_enumAction.equals(CanvasActionDescriptor.ADD)) onAdd(param);
		if(a_enumAction.equals(CanvasActionDescriptor.INSERT)) onInsertBefore(param);
		if(a_enumAction.equals(CanvasActionDescriptor.CHANGE)) onChangeResidueType(param);
		if(a_enumAction.equals(CanvasActionDescriptor.CHANGEREDEND)) onChangeReducingEnd(param);
		if(a_enumAction.equals(CanvasActionDescriptor.BRACKET)) onAddBracket();
		if(a_enumAction.equals(CanvasActionDescriptor.REPEAT)) onAddRepeat();
		if(a_enumAction.equals(CanvasActionDescriptor.CYCLIC)) onAddCyclic();
		if(a_enumAction.equals(CanvasActionDescriptor.ALTERNATIVE)) onAddALternative();
		//if(a_enumAction.equals(CanvasActionDescriptor.CHANGEREDEND)) onMassOptionsSelectedStructures();
		if(a_enumAction.equals(CanvasActionDescriptor.MASSOPTSTRUCT)) onMassOptionsSelectedStructures();
		
		// display
		if(a_enumAction.equals(CanvasActionDescriptor.NOTATION)) setNotation(param);
		if(a_enumAction.equals(CanvasActionDescriptor.DISPLAY)) setDisplay(param);
		if(a_enumAction.equals(CanvasActionDescriptor.DISPLAYSETTINGS)) onChangeDisplaySettings();
		if(a_enumAction.equals(CanvasActionDescriptor.SHOWINFO)) {		
			theWorkspace.getGraphicOptions().SHOW_INFO = ((JCheckBoxMenuItem) e.getSource()).getState();
			this.respondToDocumentChange = true;
			repaint();
		} 
		if(a_enumAction.equals(CanvasActionDescriptor.SCALE)) {
			theWorkspace.getGraphicOptions().SCALE_CANVAS = Double.parseDouble(param) / 100.;
			this.respondToDocumentChange = true;
			repaint();
		} 
		if(a_enumAction.equals(CanvasActionDescriptor.COLLLAPSE)) {
			theWorkspace.getGraphicOptions().COLLAPSE_MULTIPLE_ANTENNAE = ((JCheckBoxMenuItem) e.getSource()).isSelected();
			this.respondToDocumentChange = true;
			repaint();
		} 
		if(a_enumAction.equals(CanvasActionDescriptor.SHOWMASSCANVAS)) {
			theWorkspace.getGraphicOptions().SHOW_MASSES_CANVAS = ((JCheckBoxMenuItem) e.getSource()).isSelected();
			this.respondToDocumentChange = true;
			repaint();
		} 
		if(a_enumAction.equals(CanvasActionDescriptor.SHOWMASS)) {
			System.out.println(e.getSource());
			theWorkspace.getGraphicOptions().SHOW_MASSES = ((JCheckBoxMenuItem) e.getSource()).isSelected();
			this.respondToDocumentChange = true;		
			repaint();
		} 
		if(a_enumAction.equals(CanvasActionDescriptor.SHOWREDENDCANVAS) ) {
			theWorkspace.getGraphicOptions().SHOW_REDEND_CANVAS = ((JCheckBoxMenuItem) e.getSource()).isSelected();
			this.respondToDocumentChange = true;
			repaint();
		} 
		if(a_enumAction.equals(CanvasActionDescriptor.SHOWREDEND)) {
			theWorkspace.getGraphicOptions().SHOW_REDEND = ((JCheckBoxMenuItem) e.getSource()).isSelected();
			this.respondToDocumentChange = true;
			repaint();
		}
		if(a_enumAction.equals(CanvasActionDescriptor.SAVESPEC)) {
			System.err.println("Save spectra found");
			theWorkspace.getGraphicOptions().SAVE_SPECTRA_CUSTOM= ((JCheckBoxMenuItem) e.getSource()).isSelected();
		}
		if(a_enumAction.equals(CanvasActionDescriptor.ORIENTATION)) {			
			onChangeOrientation();
			updateOrientationButton();
			
			updateOrientation();
			repaint();
		} 
		if(a_enumAction.equals(CanvasActionDescriptor.PROPERTIES)) onProperties();
		if(a_enumAction.equals(CanvasActionDescriptor.SETPROPERTY)) onSetProperties();
		if(a_enumAction.equals(CanvasActionDescriptor.SETPROPERTYR)) onSetProperties_r();
		if(a_enumAction.equals(CanvasActionDescriptor.MOVECCW)) onMoveCCW();
		if(a_enumAction.equals(CanvasActionDescriptor.MOVECW)) onMoveCW();
		if(a_enumAction.equals(CanvasActionDescriptor.NAVUP)) onNavigateUp();
		if(a_enumAction.equals(CanvasActionDescriptor.NAVDOWN)) onNavigateDown();
		if(a_enumAction.equals(CanvasActionDescriptor.NAVLEFT)) onNavigateDown();
		if(a_enumAction.equals(CanvasActionDescriptor.NAVRIGHT)) onNavigateRight();
		if(a_enumAction.equals(CanvasActionDescriptor.EXPLODE)) explode();
		if(a_enumAction.equals(CanvasActionDescriptor.IMPLODE)) implode();
		// else if( action.equals("resetplace") )
		// resetPlacement();
		//else if (action.equals(anObject))
		
		//debug
		if(a_enumAction.equals(CanvasActionDescriptor.CHANGELV3)) CompositionUtility.onChangeLV3(theDoc, getCurrentStructure());
		if(a_enumAction.equals(CanvasActionDescriptor.CHANGELV4)) CompositionUtility.onChangeLV4(theDoc, getCurrentStructure());
		if(a_enumAction.equals(CanvasActionDescriptor.SHOWID)) DebugUtility.shoeIndex(getCurrentStructure());
		if(a_enumAction.equals(CanvasActionDescriptor.REMOVEAANOTATION)) DebugUtility.removeAnotation(getCurrentStructure());
		if(a_enumAction.equals(CanvasActionDescriptor.INSERTBRIDGE)) insertCrossLinkedSubstituent(param);
		//if(a_enumAction.equals(CanvasActionDescriptor.SHOWINDEX))
			
		updateActions();
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				revalidate();
				repaint();
			}
		});
	}

	public void residueHistoryChanged() {
		updateRecentResiduesToolbar(theToolBarStructure);
	}

	public void documentInit(BaseDocument.DocumentChangeEvent e) {
		updateActions();
		resetSelection();
		this.respondToDocumentChange = true;
		repaint();
	}

	protected boolean respondToDocumentChange;

	public void documentChanged(BaseDocument.DocumentChangeEvent e) {
		updateActions();
		resetSelection();

		respondToDocumentChange = true;

		this.respondToDocumentChange = true;
		repaint();
		// moved from repaint, so we only revalidate when we need too.

	}

	/**
	 * Register a listener that will be notified of a change in the selection
	 */
	public void addSelectionChangeListener(SelectionChangeListener l) {
		if (l != null)
			listeners.add(l);
	}

	/**
	 * Deregister one of the listeners that are notified of a change in the
	 * selection
	 */
	public void removeSelectionChangeListener(SelectionChangeListener l) {
		if (l != null)
			listeners.remove(l);
	}

	/**
	 * Send an event to all listeners notifying a change in the selection
	 * 
	 * @param completeStructure
	 */
	public void fireUpdatedSelection(boolean completeStructure) {
		// update actions
		updateActions();
		updateToolbarProperties(!completeStructure);

		// fire events
		for (Iterator<SelectionChangeListener> i = listeners.iterator(); i.hasNext();)
			i.next().selectionChanged(new SelectionChangeEvent(this));

		// update view
		this.respondToDocumentChange = true;
		repaint();
		showSelection();
	}

	/**
	 * Make sure that the selected residues are visible in the component by
	 * moving the scroll pane
	 */
	public void showSelection() {
		// update bounding boxes
		boolean show_masses = is_printing ? getTheGlycanRenderer()
				.getGraphicOptions().SHOW_MASSES : getTheGlycanRenderer()
				.getGraphicOptions().SHOW_MASSES_CANVAS;
		boolean show_redend = is_printing ? getTheGlycanRenderer()
				.getGraphicOptions().SHOW_REDEND : getTheGlycanRenderer()
				.getGraphicOptions().SHOW_REDEND_CANVAS;
		getTheGlycanRenderer().computeBoundingBoxes(theDoc.getStructures(),
				show_masses, show_redend, thePosManager, theBBoxManager);

		//

		Rectangle bbox = null;
		for (Residue r : selected_residues)
			bbox = union(bbox, theBBoxManager.getCurrent(r));
		for (Linkage l : selected_linkages) {
			bbox = union(bbox, theBBoxManager.getCurrent(l.getChildResidue()));
			bbox = union(bbox, theBBoxManager.getCurrent(l.getParentResidue()));
		}

		if (bbox != null) {
			bbox = expand(bbox, 5);

			// show bbox in viewport
			Rectangle view = theScrollPane.getViewport().getViewRect();
			int new_x = left(view);
			int new_y = top(view);
			if (left(view) > left(bbox))
				new_x = left(bbox);
			else if (right(view) < right(bbox)) {
				int min_move = right(bbox) - right(view);
				int max_move = left(bbox) - left(view);
				new_x += Math.min(min_move, max_move);
			}
			if (top(view) > top(bbox))
				new_y = top(bbox);
			else if (bottom(view) < bottom(bbox)) {
				int min_move = bottom(bbox) - bottom(view);
				int max_move = top(bbox) - top(view);
				new_y += Math.min(min_move, max_move);
			}
			theScrollPane.getViewport().setViewPosition(new Point(new_x, new_y));
			theScrollPane.getViewport().setViewPosition(new Point(new_x, new_y));
		}
	}

	// ---------------
	// mouse handling

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		lastMouseButton = e.getButton();
		if (MouseUtils.isPushTrigger(e) || MouseUtils.isCtrlPushTrigger(e)) {
			Residue start_position = getResidueAtPoint(e.getPoint());
			if (start_position != null) {
				// start DnD
				is_dragndrop = true;

				if (!isSelected(start_position)) {
					if (MouseUtils.isCtrlPushTrigger(e)) addSelection(start_position);
					else setSelection(start_position);
				}
			} else {
				// start selection
				mouse_start_point = e.getPoint();
			}
		}
		was_dragged = false;
		lastMouseButton = null;
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
		was_dragged = true;
		lastMouseButton = e.getButton();
		// if is dragging don't update selection
		if (is_dragndrop) {
			// set cursor
			if (MouseUtils.isNothingPressed(e))
				setCursor((canDrop(e)) ? dndmove_cursor : dndnomove_cursor);
			else if (MouseUtils.isCtrlPressed(e))
				setCursor((canDrop(e)) ? dndcopy_cursor : dndnocopy_cursor);
			else
				setCursor(Cursor.getDefaultCursor());
		} else if (mouse_start_point != null) {
			if (mouse_end_point != null)
				xorRectangle(mouse_start_point, mouse_end_point);
			mouse_end_point = e.getPoint();
			xorRectangle(mouse_start_point, mouse_end_point);

		}
		dragAndScroll(e);
		lastMouseButton = null;
	}

	public void mouseReleased(MouseEvent e) {
		lastMouseButton = e.getButton();
		// Drag and drop
		if (is_dragndrop && was_dragged) {
			Residue position = getResidueAtPoint(e.getPoint());
			if (canDrop(e)) {
				if (MouseUtils.isNothingPressed(e)) moveTo(position);
				else if (MouseUtils.isCtrlPressed(e)) copyTo(position);
			}
		} else if (mouse_end_point != null) {
			if (mouse_end_point != null)
				xorRectangle(mouse_start_point, mouse_end_point);

			Rectangle mouse_rect = makeRectangle(mouse_start_point, mouse_end_point);
			if (MouseUtils.isNothingPressed(e))
				setSelection(theBBoxManager.getNodesInside(mouse_rect));
			else if (MouseUtils.isCtrlPressed(e))
				addSelection(theBBoxManager.getNodesInside(mouse_rect));
			setCurrentResidue(findNearest(e.getPoint(), selected_residues));
		}

		// reset
		if (is_dragndrop) setCursor(Cursor.getDefaultCursor());

		is_dragndrop = false;
		was_dragged = false;
		mouse_start_point = null;
		mouse_end_point = null;
		repaint();
		lastMouseButton = null;
	}

	protected Integer lastMouseButton;

	public Integer getLastMouseButton() {
		return lastMouseButton;
	}

	public void setLastMouseButton(Integer lastMouseButton) {
		this.lastMouseButton = lastMouseButton;
	}

	public void mouseClicked(MouseEvent e) {
		lastMouseButton = e.getButton();
		Residue r = getResidueAtPoint(e.getPoint());
		if (r != null) {
			if (MouseUtils.isSelectTrigger(e)) {
				setSelection(r);
			} else if (MouseUtils.isAddSelectTrigger(e)) {
				addSelection(r);
			} else if (MouseUtils.isSelectAllTrigger(e)) {
				addSelectionPathTo(r);
			}
		} else {
			Linkage l = getLinkageAtPoint(e.getPoint());
			if (MouseUtils.isSelectTrigger(e)
					|| MouseUtils.isAddSelectTrigger(e)
					|| MouseUtils.isSelectAllTrigger(e))
				setSelection(l);
		}
		lastMouseButton = null;

	}

	private void dragAndScroll(MouseEvent e) {
		// move view if near borders
		Point point = e.getPoint();
		JViewport view = theScrollPane.getViewport();
		Rectangle inner = view.getViewRect();
		inner.grow(-10, -10);

		if (!inner.contains(point)) {
			Point orig = view.getViewPosition();
			if (point.x < inner.x) orig.x -= 10;
			else if (point.x > (inner.x + inner.width)) orig.x += 10;
			if (point.y < inner.y) orig.y -= 10;
			else if (point.y > (inner.y + inner.height)) orig.y += 10;

			int maxx = getBounds().width - view.getViewRect().width;
			int maxy = getBounds().height - view.getViewRect().height;
			if (orig.x < 0) orig.x = 0;
			if (orig.x > maxx) orig.x = maxx;
			if (orig.y < 0) orig.y = 0;
			if (orig.y > maxy) orig.y = maxy;

			view.setViewPosition(orig);
		}
	}

	private boolean canDrop(MouseEvent e) {
		Residue target = getResidueAtPoint(e.getPoint());
		if (target == null) return true;
		if (isSelected(target) && !e.isControlDown()) return false;
		return theDoc.canAddStructures(target, selected_residues);
	}

	public void clearResidueGalleries() {
		for (List<ResidueGalleryIndex> indexList : this.residueGalleries.values()) {
			for (ResidueGalleryIndex index : indexList) {
				if (index.band.getControlPanel() != null) {
					// System.err.println("Removing gallery " +
					// index.galleryName);
//					index.band.getControlPanel().removeRibbonGallery(
//							index.galleryName);
					index.band.getControlPanel().remove(index.band.getControlPanel().getRibbonGallery(index.galleryName));
				}
			}
		}

		if (structureSelectionBand != null) {
//			structureSelectionBand.getControlPanel().removeRibbonGallery(
//					STRUCTURE_GALLERY_NAME);
      structureSelectionBand.getControlPanel().remove(structureSelectionBand);
			// System.err.println("Removing structure gallery");
		}

		if (terminalRibbonBand != null && terminalRibbonBand.getControlPanel() != null) {
			// System.err.println("Removing terminal gallery");
			terminalRibbonBand.getControlPanel().remove(terminalRibbonBand);
		}
	}
	
	private List<UIActionListener> uiActionListenerList=new ArrayList<UIActionListener>();

	private JMenuItem bracketMenuItem;
	private JMenuItem repeatMenuItem;
	private JMenuItem antennaParentItem;
	private JMenuItem cyclicMenuItem;
	
	private JButton bracketButton;
	private JButton repeatButton;
	private JButton antennaParentButton;
	private JButton cyclicButton;
	private JButton altButton;
	
	public void registerUIListener(UIActionListener uiActionListener){
		uiActionListenerList.add(uiActionListener);
	}

	public void explode(){
		for(UIActionListener uiActionListener:uiActionListenerList){
			uiActionListener.explode();
		}
	}
	
	public void implode(){
		for(UIActionListener uiActionListener:uiActionListenerList){
			uiActionListener.implode();
		}
	}

	public void setTheActionManager(ActionManager theActionManager) {
		this.theActionManager = theActionManager;
	}

	public ActionManager getTheActionManager() {
		return theActionManager;
	}

	public void setTheGlycanRenderer(GlycanRendererAWT theGlycanRenderer) {
		this.theGlycanRenderer = theGlycanRenderer;
	}

	public GlycanRendererAWT getTheGlycanRenderer() {
		return theGlycanRenderer;
	}
	
	public boolean isAllowRepeatingUnits(){
		return allowRepeatingUnits;
	}

	public void setAllowRepeatingUnits(boolean allowRepeatingUnits){
		this.allowRepeatingUnits=allowRepeatingUnits;
		
		updateActions();
	}

	public boolean isAllowMultipleStructures(){
		return allowMultipleStructures;
	}

	public void setAllowMultipleStructures(boolean allowMultipleStructures){
		this.allowMultipleStructures=allowMultipleStructures;
		
		updateActions();
	}

	public boolean isAllowUncertainTerminals(){
		return allowUncertainTerminals;
	}

	public void setAllowUncertainTerminals(boolean allowUncertainTerminals){
		this.allowUncertainTerminals=allowUncertainTerminals;
		
		updateActions();
	}
	
	public JFrame getFrame() {
		return this.theParent;
	}
}
