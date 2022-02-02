package org.eurocarbdb.application.glycanbuilder;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;

import org.eurocarbdb.application.glycanbuilder.dataset.CoreDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.ResidueRenderer;
import org.eurocarbdb.application.glycanbuilder.util.ActionManager;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.application.glycanbuilder.dataset.CrossLinkedSubstituentDictionary;

public class CanvasAction {

	public void createAction(ActionManager a_oActionManager, ThemeManager a_oTheme, ICON_SIZE defaultMenuIconSize, ActionListener a_oListerner, GlycanRendererAWT a_oGlycanRenderer) {
		this.createEditAction(a_oActionManager, a_oTheme, defaultMenuIconSize, a_oListerner);
		this.createStructureAction(a_oActionManager, a_oTheme, defaultMenuIconSize, a_oListerner);
		this.createViewAction(a_oActionManager, a_oTheme, defaultMenuIconSize, a_oListerner);
		this.createOtherAction(a_oActionManager, a_oTheme, defaultMenuIconSize, a_oListerner);
		this.createMonosaccharideAction(a_oActionManager, a_oTheme, a_oListerner, a_oGlycanRenderer);
	}
	
	private void createEditAction(ActionManager a_oActionManager, ThemeManager a_oTheme, ICON_SIZE defaultMenuIconSize, ActionListener a_oListener) {
		a_oActionManager.add("explode", a_oTheme.getResizableIcon("explode", defaultMenuIconSize), "Explode", KeyEvent.VK_E, "ctrl E", a_oListener);
		a_oActionManager.add("implode", a_oTheme.getResizableIcon("implode", defaultMenuIconSize), "Implode", KeyEvent.VK_E, "ctrl shift E", a_oListener);
		a_oActionManager.add("undo", a_oTheme.getResizableIcon(STOCK_ICON.UNDO, defaultMenuIconSize), "Undo", KeyEvent.VK_U, "ctrl Z", a_oListener);
		a_oActionManager.add("redo", a_oTheme.getResizableIcon(STOCK_ICON.REDO, defaultMenuIconSize), "Redo", KeyEvent.VK_R, "ctrl Y", a_oListener);
		a_oActionManager.add("cut", a_oTheme.getResizableIcon(STOCK_ICON.CUT, defaultMenuIconSize), "Cut", KeyEvent.VK_T, "ctrl X", a_oListener);
		a_oActionManager.add("copy", a_oTheme.getResizableIcon(STOCK_ICON.COPY, defaultMenuIconSize), "Copy", KeyEvent.VK_C, "ctrl C", a_oListener);
		a_oActionManager.add("paste", a_oTheme.getResizableIcon(STOCK_ICON.PASTE, defaultMenuIconSize), "Paste", KeyEvent.VK_P, "ctrl V", a_oListener);
		a_oActionManager.add("delete", a_oTheme.getResizableIcon("eraser", defaultMenuIconSize), "Delete", KeyEvent.VK_L, "DELETE", a_oListener);
		a_oActionManager.add("orderstructuresasc", a_oTheme.getResizableIcon("sort_ascending", defaultMenuIconSize), "Sort structures by m/z in ascending order", KeyEvent.VK_A, "", a_oListener);
		a_oActionManager.add("orderstructuresdesc", a_oTheme.getResizableIcon("sort_descending", defaultMenuIconSize), "Sort structures by m/z in descending order", KeyEvent.VK_D, "", a_oListener);
		a_oActionManager.add("selectstructure", a_oTheme.getResizableIcon("selectstructure", defaultMenuIconSize), "Select current structure", KeyEvent.VK_S, "ctrl W", a_oListener);
		a_oActionManager.add("selectall", a_oTheme.getResizableIcon("selectall", defaultMenuIconSize), "Select all", KeyEvent.VK_A, "ctrl A", a_oListener);
		a_oActionManager.add("selectnone", a_oTheme.getResizableIcon("deselect", defaultMenuIconSize), "Select none", KeyEvent.VK_N, "ESCAPE", a_oListener);
		a_oActionManager.add("gotostart", a_oTheme.getResizableIcon("go-first", defaultMenuIconSize), "Show beginning of the canvas", KeyEvent.VK_B, "ctrl HOME", a_oListener);
		a_oActionManager.add("gotoend", a_oTheme.getResizableIcon("go-last", defaultMenuIconSize), "Show end of the canvas", KeyEvent.VK_E, "ctrl END", a_oListener);
		
		//a_oActionManager.add("stringNotation", a_oTheme.getResizableIcon("glycandoc", ICON_SIZE.L3), "Non symbol notation", -1, "", a_oListener);
		a_oActionManager.add("antennaParent", a_oTheme.getResizableIcon(STOCK_ICON.FAMILY, ICON_SIZE.L2), "SelectParent", -1, "", a_oListener);
	}
	
	private void createStructureAction (ActionManager a_oActionManager, ThemeManager themeManager, ICON_SIZE defaultMenuIconSize, ActionListener a_oListener) {
		/** Original method*/
		a_oActionManager.add("addstructurestr", themeManager.getResizableIcon("import", defaultMenuIconSize), "Add structure from string", KeyEvent.VK_S, "", a_oListener);
		a_oActionManager.add("getstructurestr", themeManager.getResizableIcon("export", defaultMenuIconSize), "Get string from structure", -1, "", a_oListener);
		a_oActionManager.add("addcomposition", themeManager.getResizableIcon("piechart", defaultMenuIconSize), "Add composition", KeyEvent.VK_C, "", a_oListener);
		a_oActionManager.add("bracket", themeManager.getResizableIcon("bracket", defaultMenuIconSize), "Add bracket", KeyEvent.VK_B, "ctrl B", a_oListener);
		a_oActionManager.add("repeat", themeManager.getResizableIcon("repeat", defaultMenuIconSize), "Add repeating unit", KeyEvent.VK_U, "ctrl R", a_oListener);
		a_oActionManager.add("cyclic", themeManager.getResizableIcon("cyclic", defaultMenuIconSize), "Add cyclic symbol", KeyEvent.VK_C, "ctrl C", a_oListener);
		//a_oActionManager.add("alternative", themeManager.getResizableIcon("alternative", defaultMenuIconSize), "Add alternative unit", KeyEvent.VK_L, "ctrl L", a_oListener);
		a_oActionManager.add("properties", themeManager.getResizableIcon("properties", defaultMenuIconSize), "Residue properties", KeyEvent.VK_P, "ctrl ENTER", a_oListener);
		a_oActionManager.add("moveccw", themeManager.getResizableIcon("rotateccw", defaultMenuIconSize), "Move residue counter-clockwise", KeyEvent.VK_K, "ctrl shift LEFT", a_oListener);
		a_oActionManager.add("movecw", themeManager.getResizableIcon("rotatecw", defaultMenuIconSize), "Move residue clockwise", KeyEvent.VK_W, "ctrl shift RIGHT", a_oListener);
		//a_oActionManager.add("changeredend=", themeManager.getResizableIcon("changeredend", defaultMenuIconSize), "Change reducing end type", KeyEvent.VK_Y, "", a_oListener);
		a_oActionManager.add("massoptstruct", themeManager.getResizableIcon("massoptions", defaultMenuIconSize), "Mass options of selected structures", KeyEvent.VK_M, "", a_oListener);

		// structure
		for (CoreType t : CoreDictionary.getCores()) {
			a_oActionManager.add("addstructure=" + t.getName(), ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), t.getDescription(), -1, "", a_oListener);
		}
	}
	
	private void createViewAction (ActionManager a_oActionManager, ThemeManager themeManager, ICON_SIZE defaultMenuIconSize, ActionListener a_oListener) {
		a_oActionManager.add("notation=" + GraphicOptions.NOTATION_CFG, themeManager.getResizableIcon("CFG_color", defaultMenuIconSize), "CFG notation", KeyEvent.VK_C, "", a_oListener);
		a_oActionManager.add("notation=" + GraphicOptions.NOTATION_CFGBW, themeManager.getResizableIcon("CFG_greyscale", defaultMenuIconSize), "CFG black and white notation", KeyEvent.VK_B, "", a_oListener);
		a_oActionManager.add("notation=" + GraphicOptions.NOTATION_CFGLINK, themeManager.getResizableIcon("CFG_linkage", defaultMenuIconSize), "CFG with linkage placement notation", KeyEvent.VK_L, "", a_oListener);
		a_oActionManager.add("notation=" + GraphicOptions.NOTATION_UOXF, themeManager.getResizableIcon("uoxf", defaultMenuIconSize), "UOXF notation", KeyEvent.VK_O, "", a_oListener);
		a_oActionManager.add("notation=" + GraphicOptions.NOTATION_UOXFCOL, themeManager.getResizableIcon("uoxfcol", defaultMenuIconSize), "UOXFCOL notation", KeyEvent.VK_O, "", a_oListener);
		a_oActionManager.add("notation=" + GraphicOptions.NOTATION_TEXT, themeManager.getResizableIcon("text", defaultMenuIconSize), "Text only notation", KeyEvent.VK_T, "", a_oListener);
		a_oActionManager.add("notation=" + GraphicOptions.NOTATION_SNFG, themeManager.getResizableIcon("", defaultMenuIconSize), "SNFG notation", KeyEvent.VK_S, "", a_oListener);
	
		a_oActionManager.add("display=" + GraphicOptions.DISPLAY_COMPACT, ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "compact view", KeyEvent.VK_O, "", a_oListener);
		a_oActionManager.add("display=" + GraphicOptions.DISPLAY_NORMAL, ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "normal view", KeyEvent.VK_N, "", a_oListener);
		a_oActionManager.add("display=" + GraphicOptions.DISPLAY_NORMALINFO, ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "normal view with linkage info", KeyEvent.VK_I, "", a_oListener);
		a_oActionManager.add("display=" + GraphicOptions.DISPLAY_CUSTOM, ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "custom view with user settings", KeyEvent.VK_U, "", a_oListener);

		a_oActionManager.add("scale=400", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "400%", -1, "", a_oListener);
		a_oActionManager.add("scale=300", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "300%", -1, "", a_oListener);
		a_oActionManager.add("scale=200", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "200%", -1, "", a_oListener);
		a_oActionManager.add("scale=150", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "150%", -1, "", a_oListener);
		a_oActionManager.add("scale=100", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "100%", -1, "", a_oListener);
		a_oActionManager.add("scale=67", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "67%", -1, "", a_oListener);
		a_oActionManager.add("scale=50", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "50%", -1, "", a_oListener);
		a_oActionManager.add("scale=33", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "33%", -1, "", a_oListener);
		a_oActionManager.add("scale=25", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "25%", -1, "", a_oListener);

		a_oActionManager.add("collapsemultipleantennae", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "Collapse multiple antennae", KeyEvent.VK_A, "", a_oListener);
		a_oActionManager.add("showmassescanvas", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "Show masses in the drawing canvas", KeyEvent.VK_V, "", a_oListener);
		a_oActionManager.add("showmasses", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "Show masses when exporting", KeyEvent.VK_M, "", a_oListener);
		a_oActionManager.add("showredendcanvas", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "Show reducing end indicator in the drawing canvas", KeyEvent.VK_R, "", a_oListener);
		a_oActionManager.add("showredend", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "Show reducing end indicator when exporting", KeyEvent.VK_E, "", a_oListener);
		a_oActionManager.add("orientation", themeManager.getResizableIcon("rl", ICON_SIZE.L3), "Change orientation", KeyEvent.VK_O, "", a_oListener);
		a_oActionManager.add("displaysettings", themeManager.getResizableIcon("display", ICON_SIZE.L4), "Change display settings", KeyEvent.VK_D, "", a_oListener);
		a_oActionManager.add("savespec",themeManager.getResizableIcon( "savespec", ICON_SIZE.L4), "Save spectra", KeyEvent.VK_D, "", a_oListener);

	}
	
	private void createOtherAction (ActionManager a_oActionManager, ThemeManager themeManager, ICON_SIZE defaultMenuIconSize, ActionListener a_oListener) {
		// help
		a_oActionManager.add("about", themeManager.getResizableIcon("about", ICON_SIZE.L4), "About", KeyEvent.VK_B, "", a_oListener);

		//debug
		a_oActionManager.add("changeLV2", themeManager.getResizableIcon("changeLV2", ICON_SIZE.L4), "changeLV2", KeyEvent.VK_C, "", a_oListener);
		a_oActionManager.add("changeLV3", themeManager.getResizableIcon("changeLV3", ICON_SIZE.L4), "changeLV3", KeyEvent.VK_C, "", a_oListener);
		a_oActionManager.add("changeLV4", themeManager.getResizableIcon("changeLV4", ICON_SIZE.L4), "changeLV4", KeyEvent.VK_C, "", a_oListener);	
		a_oActionManager.add("showid", themeManager.getResizableIcon("showid", ICON_SIZE.L4), "Show ID", KeyEvent.VK_C, "", a_oListener);
		a_oActionManager.add("showindex", themeManager.getResizableIcon("showindex", ICON_SIZE.L4), "Show Index", KeyEvent.VK_C, "", a_oListener);
		a_oActionManager.add("removeanotation", themeManager.getResizableIcon("removeanotation", ICON_SIZE.L4), "Remove Anotation", KeyEvent.VK_C, "", a_oListener);
	}
	
	private void createMonosaccharideAction (ActionManager a_oActionManager, ThemeManager a_oTheme, ActionListener a_oListerner, GlycanRendererAWT a_oGlycanRender) {
		ICON_SIZE iconSize = ICON_SIZE.L3;
		ResidueRenderer rr = a_oGlycanRender.getResidueRenderer();

		/** bridge */
		for(ResidueType a_oRT : CrossLinkedSubstituentDictionary.getCrossLinkedSubstituents()) {
			ImageResizableIconReducedMem icon = 
					new ImageResizableIconReducedMem(rr.getImage(a_oRT, iconSize.getSize()), iconSize.getSize(), iconSize.getSize());
			
			EurocarbResizableIcon eu_icon = new EurocarbResizableIcon(a_oTheme, null, icon);
			eu_icon.setResizableIcon(icon);
			
			a_oActionManager.add("bridge=" + a_oRT.getName(), eu_icon, a_oRT.getDescription(), -1, "",  a_oListerner);
		}
		
		for (ResidueType t : ResidueDictionary.allResidues()) {
			ImageResizableIconReducedMem icon = 
					new ImageResizableIconReducedMem(rr.getImage(t, iconSize.getSize()), iconSize.getSize(), iconSize.getSize());

			EurocarbResizableIcon eu_icon = new EurocarbResizableIcon(a_oTheme, null, icon);
			eu_icon.setResizableIcon(icon);

			a_oActionManager.add("change=" + t.getName(), eu_icon, t.getDescription(), -1, "", a_oListerner);

			if (t.canHaveParent()) {
				a_oActionManager.add("add=" + t.getName(), eu_icon, t.getDescription(), -1, (t.getToolbarOrder() != 0) ? ("ctrl " + t.getToolbarOrder()) : "", a_oListerner);
				//ImageIcon last = new ImageIcon(rr.getImage(t, iconSize.getSize()));
				a_oActionManager.get("add=" + t.getName()).putValue(Action.SMALL_ICON, new ImageIcon(rr.getImage(t, ICON_SIZE.L3.getSize())));
			}
			if (t.canHaveParent() && t.canHaveChildren())
				a_oActionManager.add("insert=" + t.getName(), eu_icon, t.getDescription(), -1, "", a_oListerner);
			if (t.canBeReducingEnd())
				a_oActionManager.add("changeredend=" + t.getName(), eu_icon, t.getDescription(), -1, "", a_oListerner);
		}
	}
	
}
