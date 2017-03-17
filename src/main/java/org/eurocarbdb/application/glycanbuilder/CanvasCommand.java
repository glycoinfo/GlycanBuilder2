package org.eurocarbdb.application.glycanbuilder;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;

import org.eurocarbdb.application.glycanbuilder.dataset.CoreDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.TerminalDictionary;
import org.eurocarbdb.application.glycanbuilder.util.ActionManager;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.application.glycanbuilder.dataset.CrossLinkedSubstituentDictionary;
import org.glycoinfo.application.glycanbuilder.dataset.SubstituentTypeDescriptor;
import org.pushingpixels.flamingo.api.common.RichTooltip;
import org.pushingpixels.flamingo.api.ribbon.JRibbonBand;
import org.pushingpixels.flamingo.api.ribbon.RibbonElementPriority;
import org.pushingpixels.flamingo.api.ribbon.RibbonTask;
import org.pushingpixels.flamingo.api.ribbon.resize.CoreRibbonResizePolicies;
import org.pushingpixels.flamingo.api.ribbon.resize.IconRibbonBandResizePolicy;
import org.pushingpixels.flamingo.api.ribbon.resize.RibbonBandResizePolicy;

public class CanvasCommand {
	
	public CanvasCommand(ThemeManager _a_oTheme, ActionManager _a_oActionManager) {
	}
	
	public JMenu createAddStructureMenu(ActionManager a_oActionManager) {

		JMenu add_menu = new JMenu("Add structure");
		add_menu.setMnemonic(KeyEvent.VK_A);
		add_menu.setIcon(ThemeManager.getEmptyIcon(null));

		boolean a_bIsSet = false;
		
		for (String superclass : CoreDictionary.getSuperclasses()) {
			JMenu class_menu = new JMenu(superclass);
			for (CoreType t : CoreDictionary.getCores(superclass)) {
				String a_sDescription = t.getDescription();
				if((!a_sDescription.contains("Dermatan sulfate") && 
						!a_sDescription.contains("Heparan sulfate")) && a_bIsSet == true) a_bIsSet = false;
				if(a_bIsSet) continue;

				if(a_sDescription.contains("Dermatan sulfate") || a_sDescription.contains("Heparan sulfate")) {
					class_menu.add(this.extractGAGs(a_sDescription.substring(0, a_sDescription.indexOf("(")), a_oActionManager));
					a_bIsSet = true;
				}else
					class_menu.add(a_oActionManager.get("addstructure=" + t.getName()));
			}
			if (class_menu.getItemCount() > 0) add_menu.add(class_menu);
		}

		return add_menu;
	}
	
	private JMenu extractGAGs(String a_sUnitType, ActionManager a_oActionManager) {
		JMenu a_oGAGsMenu = new JMenu(a_sUnitType);
		
		for(CoreType a_oCore : CoreDictionary.getCores("GAGs")) {
			if(a_oCore.getDescription().contains(a_sUnitType))
				a_oGAGsMenu.add(a_oActionManager.get("addstructure=" + a_oCore.getName()));
		}
		
		return a_oGAGsMenu;
	}
	
	public JMenu createAddResidueMenu(ActionManager a_oActionManager) {
		JMenu a_oMenu = new JMenu("Add residue");
		a_oMenu.setMnemonic(KeyEvent.VK_R);
		a_oMenu.setIcon(ThemeManager.getEmptyIcon(null));
		
		return this.extractResidue(a_oActionManager, "add=", a_oMenu);
	}

	public JMenu createInsertResidueMenu(ActionManager a_oActionManager) {
		JMenu a_oMenu = new JMenu("Insert residue before");
		a_oMenu.setMnemonic(KeyEvent.VK_I);
		a_oMenu.setIcon(ThemeManager.getEmptyIcon(null));
		
		return this.extractResidue(a_oActionManager, "insert=", a_oMenu);
	}
	
	public JMenu createChangeResidueTypeMenu(ActionManager a_oActionManager) {
		JMenu a_oMenu = new JMenu("Change residue type");
		a_oMenu.setMnemonic(KeyEvent.VK_H);
		a_oMenu.setIcon(ThemeManager.getEmptyIcon(null));

		return this.extractResidue(a_oActionManager, "change=", a_oMenu);
	}
	
	public JMenu createChangeReducingEndMenu(ActionManager a_oActionManager) {
		JMenu a_oMenu = new JMenu("Change reducing end type");
		a_oMenu.setMnemonic(KeyEvent.VK_E);
		a_oMenu.setIcon(ThemeManager.getEmptyIcon(null));
		
		for(ResidueType a_oRT : ResidueDictionary.getReducingEnds()) {
			JMenuItem a_oRedEndMenu = new JMenuItem(a_oActionManager.get("changeredend=" + a_oRT.getName()));
			a_oMenu.add(a_oRedEndMenu);
		}
		
		return a_oMenu;
	}
	
	public JMenu createInsertBridgeMenu(ActionManager a_oActionManager) {
		JMenu a_oInsertBridge = new JMenu("Insert bridge");
		a_oInsertBridge.setMnemonic(KeyEvent.VK_B);
		a_oInsertBridge.setIcon(ThemeManager.getEmptyIcon(null));
		
		for(ResidueType a_oRT : CrossLinkedSubstituentDictionary.getCrossLinkedSubstituents()) {
			JMenuItem a_oMenu = new JMenuItem(a_oActionManager.get("bridge=" + a_oRT.getName()));
			a_oInsertBridge.add(a_oMenu);
		}

		return a_oInsertBridge;
	}

	private JMenu extractResidue (ActionManager a_oActionManager, String a_sActionType, JMenu a_oMenu) {
		for (String superclass : ResidueDictionary.getSuperclasses()) {
			if(superclass.equals("Reducing end")) continue;
			if(a_sActionType.equals("insert=")) {
				if(superclass.equals("Substituent") || superclass.equals("Modification")) continue;
			}

			JMenu class_menu = new JMenu(superclass);
			
			if(superclass.equals("Substituent")) {
				for(SubstituentTypeDescriptor a_enumSub : SubstituentTypeDescriptor.getTypeList()) {
					JMenu a_Item = new JMenu(a_enumSub.getClassName());
					for(ResidueType t : ResidueDictionary.getResidues(superclass)) {
						if(a_enumSub.equals(SubstituentTypeDescriptor.forClass(t.getCompositionClass()))) {
							a_Item.add(new JMenuItem(a_oActionManager.get(a_sActionType + t.getName())));
						}
					}
					class_menu.add(a_Item);
				}
			}else {
				for (ResidueType t : ResidueDictionary.getResidues(superclass)) {
					if (t.canHaveParent())
						class_menu.add(a_oActionManager.get(a_sActionType + t.getName()));
				} 
			}
			if (class_menu.getItemCount() > 0) a_oMenu.add(class_menu);
		}
		
		return a_oMenu;
	}
	
	public JMenu createDebugMenu(ActionManager a_oActionManager) {
		JMenu debug_menu = new JMenu("");
		debug_menu.setMnemonic(KeyEvent.VK_D);

		debug_menu.add(a_oActionManager.get("changeLV2"));
		debug_menu.add(a_oActionManager.get("changeLV3"));
		debug_menu.add(a_oActionManager.get("changeLV4"));
		debug_menu.addSeparator();

		debug_menu.add(a_oActionManager.get("showid"));
		debug_menu.add(a_oActionManager.get("showindex"));
		debug_menu.add(a_oActionManager.get("removeanotation"));
		debug_menu.addSeparator();

		debug_menu.add(a_oActionManager.get("insertbridge"));

		return debug_menu;
	}
	
	public JMenu createEditMenu(ActionManager a_oActionManager) {

		JMenu edit_menu = new JMenu("Edit");
		edit_menu.setMnemonic(KeyEvent.VK_E);
		edit_menu.add(a_oActionManager.get("undo"));
		edit_menu.add(a_oActionManager.get("redo"));
		edit_menu.addSeparator();
		edit_menu.add(a_oActionManager.get("cut"));
		edit_menu.add(a_oActionManager.get("copy"));
		edit_menu.add(a_oActionManager.get("paste"));
		edit_menu.add(a_oActionManager.get("delete"));
		edit_menu.addSeparator();
		edit_menu.add(a_oActionManager.get("orderstructuresasc"));
		edit_menu.add(a_oActionManager.get("orderstructuresdesc"));
		edit_menu.addSeparator();
		edit_menu.add(a_oActionManager.get("selectstructure"));
		edit_menu.add(a_oActionManager.get("selectall"));
		edit_menu.add(a_oActionManager.get("selectnone"));
		edit_menu.add(a_oActionManager.get("gotostart"));
		edit_menu.add(a_oActionManager.get("gotoend"));
		/*edit_menu.addSeparator();
		edit_menu.add(a_oActionManager.get("navup"));
		edit_menu.add(a_oActionManager.get("navdown"));
		edit_menu.add(a_oActionManager.get("navleft"));
		edit_menu.add(a_oActionManager.get("navright"));
		*/// edit_menu.addSeparator();
		// edit_menu.add(theActionManager.get("screenshot"));

		return edit_menu;
	}
	
	public JToolBar createToolBarDocument(ActionManager a_oActionManager) {
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.addSeparator();
		toolbar.add(a_oActionManager.get("addstructurestr"));
		toolbar.add(a_oActionManager.get("getstructurestr"));
		toolbar.addSeparator();
		toolbar.add(a_oActionManager.get("undo"));
		toolbar.add(a_oActionManager.get("redo"));
		toolbar.addSeparator();
		toolbar.add(a_oActionManager.get("cut"));
		toolbar.add(a_oActionManager.get("copy"));
		toolbar.add(a_oActionManager.get("paste"));
		toolbar.add(a_oActionManager.get("delete"));

		return toolbar;
	}
	
	public JMenu createAddTerminalMenu(ActionListener a_oActionListener) {

		JMenu add_menu = new JMenu("Add terminal");
		add_menu.setMnemonic(KeyEvent.VK_T);
		add_menu.setIcon(ThemeManager.getEmptyIcon(null));

		for (String superclass : TerminalDictionary.getSuperclasses()) {
			JMenu class_menu = new JMenu(superclass);
			for (TerminalType t : TerminalDictionary.getTerminals(superclass)) {
				JMenu terminal_menu = new JMenu(t.getDescription());

				JMenuItem nlinked_terminal = new JMenuItem("Unknown linkage");
				nlinked_terminal.setActionCommand("addterminal=" + t.getName());
				nlinked_terminal.addActionListener(a_oActionListener);

				terminal_menu.add(nlinked_terminal);
				for (int l = 1; l < 9; l++) {
					nlinked_terminal = new JMenuItem(l + "-linked");
					nlinked_terminal.setActionCommand("addterminal=" + l + "-" + t.getName());
					nlinked_terminal.addActionListener(a_oActionListener);
					terminal_menu.add(nlinked_terminal);
				}
				class_menu.add(terminal_menu);
			}
			if (class_menu.getItemCount() > 0)
				add_menu.add(class_menu);
		}

		return add_menu;
	}
	
	public JMenu createZoomMenu(BuilderWorkspace a_oWorkspace, ActionManager a_oActionManager) {
		GraphicOptions view_opt = a_oWorkspace.getGraphicOptions();

		JMenu zoom_menu = new JMenu("Zoom");
		zoom_menu.setMnemonic(KeyEvent.VK_Z);

		JRadioButtonMenuItem last = null;
		ButtonGroup group = new ButtonGroup();

		zoom_menu.add(last = new JRadioButtonMenuItem(a_oActionManager.get("scale=400")));
		last.setSelected(view_opt.SCALE_CANVAS == 4.);
		group.add(last);

		zoom_menu.add(last = new JRadioButtonMenuItem(a_oActionManager.get("scale=300")));
		last.setSelected(view_opt.SCALE_CANVAS == 3.);
		group.add(last);

		zoom_menu.add(last = new JRadioButtonMenuItem(a_oActionManager.get("scale=200")));
		last.setSelected(view_opt.SCALE_CANVAS == 2.);
		group.add(last);

		zoom_menu.add(last = new JRadioButtonMenuItem(a_oActionManager.get("scale=150")));
		last.setSelected(view_opt.SCALE_CANVAS == 1.5);
		group.add(last);

		zoom_menu.add(last = new JRadioButtonMenuItem(a_oActionManager.get("scale=100")));
		last.setSelected(view_opt.SCALE_CANVAS == 1.);
		group.add(last);

		zoom_menu.add(last = new JRadioButtonMenuItem(a_oActionManager.get("scale=67")));
		last.setSelected(view_opt.SCALE_CANVAS == 0.67);
		group.add(last);

		zoom_menu.add(last = new JRadioButtonMenuItem(a_oActionManager.get("scale=50")));
		last.setSelected(view_opt.SCALE_CANVAS == 0.5);
		group.add(last);

		zoom_menu.add(last = new JRadioButtonMenuItem(a_oActionManager.get("scale=33")));
		last.setSelected(view_opt.SCALE_CANVAS == 0.33);
		group.add(last);

		zoom_menu.add(last = new JRadioButtonMenuItem(a_oActionManager.get("scale=25")));
		last.setSelected(view_opt.SCALE_CANVAS == 0.25);
		group.add(last);

		return zoom_menu;
	}
	
	/** create canvas band contents*/
	public RibbonTask createEditRibbonBand(ActionManager a_oActionManager, ActionListener a_oListener) {
		JRibbonBand band2 = new JRibbonBand(
				"Actions",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(
						10));

		ArrayList<RibbonBandResizePolicy> resizePolicies = new ArrayList<RibbonBandResizePolicy>();
		resizePolicies.add(new CoreRibbonResizePolicies.Mirror(band2
				.getControlPanel()));
		resizePolicies.add(new CoreRibbonResizePolicies.Mid2Low(band2
				.getControlPanel()));
		resizePolicies.add(new IconRibbonBandResizePolicy(band2
				.getControlPanel()));
		band2.setResizePolicies(resizePolicies);

		band2.addCommandButton(a_oActionManager.get("undo").getJCommandButton(
				ICON_SIZE.L3, "Undo", a_oListener, null), RibbonElementPriority.TOP);
		band2.addCommandButton(a_oActionManager.get("redo").getJCommandButton(
				ICON_SIZE.L3, "Redo", a_oListener, null), RibbonElementPriority.TOP);
		band2.addCommandButton(a_oActionManager.get("cut").getJCommandButton(
				ICON_SIZE.L3, "Cut", a_oListener, null), RibbonElementPriority.TOP);
		band2.addCommandButton(a_oActionManager.get("copy").getJCommandButton(
				ICON_SIZE.L3, "Copy", a_oListener, null), RibbonElementPriority.TOP);
		band2.addCommandButton(a_oActionManager.get("paste").getJCommandButton(
				ICON_SIZE.L3, "Paste", a_oListener, null), RibbonElementPriority.TOP);
		band2.addCommandButton(a_oActionManager.get("delete")
				.getJCommandButton(ICON_SIZE.L3, "Delete", a_oListener, null),
				RibbonElementPriority.TOP);

		JRibbonBand band3 = new JRibbonBand(
				"Structures",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(
						10));

		resizePolicies = new ArrayList<RibbonBandResizePolicy>();
		resizePolicies.add(new CoreRibbonResizePolicies.Mirror(band3
				.getControlPanel()));
		resizePolicies.add(new CoreRibbonResizePolicies.Mid2Low(band3
				.getControlPanel()));
		resizePolicies.add(new IconRibbonBandResizePolicy(band3
				.getControlPanel()));
		band3.setResizePolicies(resizePolicies);

		band3.addCommandButton(a_oActionManager.get("orderstructuresasc")
				.getJCommandButton(ICON_SIZE.L3, "Order ASC", a_oListener, null),
				RibbonElementPriority.TOP);
		band3.addCommandButton(a_oActionManager.get("orderstructuresdesc")
				.getJCommandButton(ICON_SIZE.L3, "Order DSC", a_oListener, null),
				RibbonElementPriority.TOP);
		band3.addCommandButton(a_oActionManager.get("selectstructure")
				.getJCommandButton(ICON_SIZE.L3, "Select", a_oListener, null),
				RibbonElementPriority.TOP);
		band3.addCommandButton(a_oActionManager.get("selectall")
				.getJCommandButton(ICON_SIZE.L3, "Select All", a_oListener, null),
				RibbonElementPriority.TOP);
		band3.addCommandButton(a_oActionManager.get("selectnone")
				.getJCommandButton(ICON_SIZE.L3, "deselect", a_oListener, null),
				RibbonElementPriority.TOP);
		band3.addCommandButton(a_oActionManager.get("gotostart")
				.getJCommandButton(ICON_SIZE.L3, "Start", a_oListener, null),
				RibbonElementPriority.TOP);
		band3.addCommandButton(a_oActionManager.get("gotoend")
				.getJCommandButton(ICON_SIZE.L3, "End", a_oListener, null),
				RibbonElementPriority.TOP);
		return new RibbonTask("Edit", band2, band3,
				createStructureRibbonControls(a_oActionManager, a_oListener));
	}
	
	private JRibbonBand createStructureRibbonControls(ActionManager a_oActionManager, ActionListener a_oListener) {
		JRibbonBand band = new JRibbonBand(
				"Edit glycan",
				new org.pushingpixels.flamingo.api.common.icon.EmptyResizableIcon(10));
		ThemeManager.setDefaultResizePolicy(band);

		band.addCommandButton(a_oActionManager.get("bracket").getJCommandButton(
				ICON_SIZE.L3, "Bracket", a_oListener, new RichTooltip("Add bracket ", "Insert bracket")),
				RibbonElementPriority.TOP);
		band.addCommandButton(a_oActionManager.get("repeat").getJCommandButton(
				ICON_SIZE.L3, "Repeat", a_oListener, new RichTooltip("Add repeat", "Add repeat unit")),
				RibbonElementPriority.TOP);
		band.addCommandButton(a_oActionManager.get("cyclic").getJCommandButton(
				ICON_SIZE.L3, "Cyclic", a_oListener, new RichTooltip("Add cyclic", "Add cyclic symbol")),
				RibbonElementPriority.TOP);
		//band.addCommandButton(a_oActionManager.get("alternative").getJCommandButton(
		//		ICON_SIZE.L3, "Error", a_oListener, new RichTooltip("Add alternaticve", "Add alternative unit")),
		//		RibbonElementPriority.TOP);
		band.addCommandButton(a_oActionManager.get("moveccw").getJCommandButton(
				ICON_SIZE.L3, "Rotate CCW", a_oListener, new RichTooltip("Rotate residue", "Rotate residue counter-clockwise")),
				RibbonElementPriority.TOP);
		band.addCommandButton(a_oActionManager.get("movecw").getJCommandButton(
				ICON_SIZE.L3, "Rotate CW", a_oListener, new RichTooltip("Rotate residue", "Rotate residue clockwise")),
				RibbonElementPriority.TOP);
		band.addCommandButton(a_oActionManager.get("properties").getJCommandButton(
				ICON_SIZE.L3, "Properties", a_oListener, new RichTooltip("Residue Properties", "Get residue properties")),
				RibbonElementPriority.TOP);
		band.addCommandButton(a_oActionManager.get("massoptstruct").getJCommandButton(
				ICON_SIZE.L3, "Mass options", a_oListener, new RichTooltip("Mass options", "Mass options of selected structure")),
				RibbonElementPriority.TOP);

		return band;
	}
}
