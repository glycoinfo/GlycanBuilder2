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

	public void createAction(ActionManager _actionManager, ThemeManager _theme, ICON_SIZE defaultMenuIconSize, ActionListener _listerner, GlycanRendererAWT _glycanRenderer) {
		this.createEditAction(_actionManager, _theme, defaultMenuIconSize, _listerner);
		this.createStructureAction(_actionManager, _theme, defaultMenuIconSize, _listerner);
		this.createViewAction(_actionManager, _theme, defaultMenuIconSize, _listerner);
		this.createOtherAction(_actionManager, _theme, defaultMenuIconSize, _listerner);
		this.createMonosaccharideAction(_actionManager, _theme, _listerner, _glycanRenderer);
		this.createGlyTouCanAction(_actionManager, _theme, defaultMenuIconSize, _listerner); //GIC added 20210105
	}
	
	private void createEditAction(ActionManager _actionManager, ThemeManager _theme, ICON_SIZE defaultMenuIconSize, ActionListener _listener) {
		_actionManager.add("explode", _theme.getResizableIcon("explode", defaultMenuIconSize), "Explode", KeyEvent.VK_E, "ctrl E", _listener);
		_actionManager.add("implode", _theme.getResizableIcon("implode", defaultMenuIconSize), "Implode", KeyEvent.VK_E, "ctrl shift E", _listener);
		_actionManager.add("undo", _theme.getResizableIcon(STOCK_ICON.UNDO, defaultMenuIconSize), "Undo", KeyEvent.VK_U, "ctrl Z", _listener);
		_actionManager.add("redo", _theme.getResizableIcon(STOCK_ICON.REDO, defaultMenuIconSize), "Redo", KeyEvent.VK_R, "ctrl Y", _listener);
		_actionManager.add("cut", _theme.getResizableIcon(STOCK_ICON.CUT, defaultMenuIconSize), "Cut", KeyEvent.VK_T, "ctrl X", _listener);
		_actionManager.add("copy", _theme.getResizableIcon(STOCK_ICON.COPY, defaultMenuIconSize), "Copy", KeyEvent.VK_C, "ctrl C", _listener);
		_actionManager.add("paste", _theme.getResizableIcon(STOCK_ICON.PASTE, defaultMenuIconSize), "Paste", KeyEvent.VK_P, "ctrl V", _listener);
		_actionManager.add("delete", _theme.getResizableIcon("eraser", defaultMenuIconSize), "Delete", KeyEvent.VK_L, "DELETE", _listener);
		_actionManager.add("orderstructuresasc", _theme.getResizableIcon("sort_ascending", defaultMenuIconSize), "Sort structures by m/z in ascending order", KeyEvent.VK_A, "", _listener);
		_actionManager.add("orderstructuresdesc", _theme.getResizableIcon("sort_descending", defaultMenuIconSize), "Sort structures by m/z in descending order", KeyEvent.VK_D, "", _listener);
		_actionManager.add("selectstructure", _theme.getResizableIcon("selectstructure", defaultMenuIconSize), "Select current structure", KeyEvent.VK_S, "ctrl W", _listener);
		_actionManager.add("selectall", _theme.getResizableIcon("selectall", defaultMenuIconSize), "Select all", KeyEvent.VK_A, "ctrl A", _listener);
		_actionManager.add("selectnone", _theme.getResizableIcon("deselect", defaultMenuIconSize), "Select none", KeyEvent.VK_N, "ESCAPE", _listener);
		_actionManager.add("gotostart", _theme.getResizableIcon("go-first", defaultMenuIconSize), "Show beginning of the canvas", KeyEvent.VK_B, "ctrl HOME", _listener);
		_actionManager.add("gotoend", _theme.getResizableIcon("go-last", defaultMenuIconSize), "Show end of the canvas", KeyEvent.VK_E, "ctrl END", _listener);
		
		//_actionManager.add("stringNotation", _theme.getResizableIcon("glycandoc", ICON_SIZE.L3), "Non symbol notation", -1, "", _listener);
		_actionManager.add("antennaParent", _theme.getResizableIcon(STOCK_ICON.FAMILY, ICON_SIZE.L2), "SelectParent", -1, "", _listener);
	}
	
	private void createStructureAction (ActionManager _actionManager, ThemeManager themeManager, ICON_SIZE defaultMenuIconSize, ActionListener _listener) {
		_actionManager.add("write", themeManager.getResizableIcon("write", defaultMenuIconSize), "Add structure from string", KeyEvent.VK_S, "", _listener);
		_actionManager.add("addstructurestr", themeManager.getResizableIcon("import", defaultMenuIconSize), "Add structure from string", KeyEvent.VK_S, "", _listener);
		_actionManager.add("getstructurestr", themeManager.getResizableIcon("export", defaultMenuIconSize), "Get string from structure", -1, "", _listener);
		_actionManager.add("addcomposition", themeManager.getResizableIcon("piechart", defaultMenuIconSize), "Add composition", KeyEvent.VK_C, "", _listener);
		_actionManager.add("bracket", themeManager.getResizableIcon("bracket", defaultMenuIconSize), "Add bracket", KeyEvent.VK_B, "ctrl B", _listener);
		_actionManager.add("repeat", themeManager.getResizableIcon("repeat", defaultMenuIconSize), "Add repeating unit", KeyEvent.VK_U, "ctrl R", _listener);
		_actionManager.add("cyclic", themeManager.getResizableIcon("cyclic", defaultMenuIconSize), "Add cyclic symbol", KeyEvent.VK_C, "ctrl C", _listener);
		//_actionManager.add("alternative", themeManager.getResizableIcon("alternative", defaultMenuIconSize), "Add alternative unit", KeyEvent.VK_L, "ctrl L", _listener);
		_actionManager.add("properties", themeManager.getResizableIcon("properties", defaultMenuIconSize), "Residue properties", KeyEvent.VK_P, "ctrl ENTER", _listener);
		_actionManager.add("moveccw", themeManager.getResizableIcon("rotateccw", defaultMenuIconSize), "Move residue counter-clockwise", KeyEvent.VK_K, "ctrl shift LEFT", _listener);
		_actionManager.add("movecw", themeManager.getResizableIcon("rotatecw", defaultMenuIconSize), "Move residue clockwise", KeyEvent.VK_W, "ctrl shift RIGHT", _listener);
		//_actionManager.add("changeredend=", themeManager.getResizableIcon("changeredend", defaultMenuIconSize), "Change reducing end type", KeyEvent.VK_Y, "", _listener);
		_actionManager.add("massoptstruct", themeManager.getResizableIcon("massoptions", defaultMenuIconSize), "Mass options of selected structures", KeyEvent.VK_M, "", _listener);

		// structure
		for (CoreType t : CoreDictionary.getCores()) {
			_actionManager.add("addstructure=" + t.getName(), ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), t.getDescription(), -1, "", _listener);
		}
	}
	
	private void createViewAction (ActionManager _actionManager, ThemeManager themeManager, ICON_SIZE defaultMenuIconSize, ActionListener _listener) {
		_actionManager.add("notation=" + GraphicOptions.NOTATION_CFG, themeManager.getResizableIcon("CFG_color", defaultMenuIconSize), "CFG notation", KeyEvent.VK_C, "", _listener);
		_actionManager.add("notation=" + GraphicOptions.NOTATION_CFGBW, themeManager.getResizableIcon("CFG_greyscale", defaultMenuIconSize), "CFG black and white notation", KeyEvent.VK_B, "", _listener);
		_actionManager.add("notation=" + GraphicOptions.NOTATION_CFGLINK, themeManager.getResizableIcon("CFG_linkage", defaultMenuIconSize), "CFG with linkage placement notation", KeyEvent.VK_L, "", _listener);
		_actionManager.add("notation=" + GraphicOptions.NOTATION_UOXF, themeManager.getResizableIcon("uoxf", defaultMenuIconSize), "UOXF notation", KeyEvent.VK_O, "", _listener);
		_actionManager.add("notation=" + GraphicOptions.NOTATION_UOXFCOL, themeManager.getResizableIcon("uoxfcol", defaultMenuIconSize), "UOXFCOL notation", KeyEvent.VK_O, "", _listener);
		_actionManager.add("notation=" + GraphicOptions.NOTATION_TEXT, themeManager.getResizableIcon("text", defaultMenuIconSize), "Text only notation", KeyEvent.VK_T, "", _listener);
		_actionManager.add("notation=" + GraphicOptions.NOTATION_SNFG, themeManager.getResizableIcon("SNFG_color", defaultMenuIconSize), "SNFG notation", KeyEvent.VK_S, "", _listener);
		//2022XXXX, S.TSUCHIYA added
		_actionManager.add("notation=" + GraphicOptions.NOTATION_SNFGLINK, themeManager.getResizableIcon("SNFG_linkage", defaultMenuIconSize), "SNFG with linkage placement notation", KeyEvent.VK_S, "", _listener);

		_actionManager.add("display=" + GraphicOptions.DISPLAY_COMPACT, ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "compact view", KeyEvent.VK_O, "", _listener);
		_actionManager.add("display=" + GraphicOptions.DISPLAY_NORMAL, ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "normal view", KeyEvent.VK_N, "", _listener);
		_actionManager.add("display=" + GraphicOptions.DISPLAY_NORMALINFO, ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "normal view with linkage info", KeyEvent.VK_I, "", _listener);
		_actionManager.add("display=" + GraphicOptions.DISPLAY_CUSTOM, ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "custom view with user settings", KeyEvent.VK_U, "", _listener);

		_actionManager.add("scale=400", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "400%", -1, "", _listener);
		_actionManager.add("scale=300", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "300%", -1, "", _listener);
		_actionManager.add("scale=200", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "200%", -1, "", _listener);
		_actionManager.add("scale=150", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "150%", -1, "", _listener);
		_actionManager.add("scale=100", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "100%", -1, "", _listener);
		_actionManager.add("scale=67", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "67%", -1, "", _listener);
		_actionManager.add("scale=50", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "50%", -1, "", _listener);
		_actionManager.add("scale=33", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "33%", -1, "", _listener);
		_actionManager.add("scale=25", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "25%", -1, "", _listener);

		_actionManager.add("collapsemultipleantennae", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "Collapse multiple antennae", KeyEvent.VK_A, "", _listener);
		_actionManager.add("showmassescanvas", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "Show masses in the drawing canvas", KeyEvent.VK_V, "", _listener);
		_actionManager.add("showmasses", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "Show masses when exporting", KeyEvent.VK_M, "", _listener);
		_actionManager.add("showredendcanvas", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "Show reducing end indicator in the drawing canvas", KeyEvent.VK_R, "", _listener);
		_actionManager.add("showredend", ThemeManager.getResizableEmptyIcon(ICON_SIZE.L3), "Show reducing end indicator when exporting", KeyEvent.VK_E, "", _listener);
		_actionManager.add("orientation", themeManager.getResizableIcon("rl", ICON_SIZE.L3), "Change orientation", KeyEvent.VK_O, "", _listener);
		_actionManager.add("displaysettings", themeManager.getResizableIcon("display", ICON_SIZE.L4), "Change display settings", KeyEvent.VK_D, "", _listener);
		_actionManager.add("savespec",themeManager.getResizableIcon( "savespec", ICON_SIZE.L4), "Save spectra", KeyEvent.VK_D, "", _listener);

	}
	
	private void createOtherAction (ActionManager _actionManager, ThemeManager themeManager, ICON_SIZE defaultMenuIconSize, ActionListener _listener) {
		// help
		_actionManager.add("about", themeManager.getResizableIcon("about", ICON_SIZE.L4), "About", KeyEvent.VK_B, "", _listener);

		//debug
		_actionManager.add("changeLV2", themeManager.getResizableIcon("changeLV2", ICON_SIZE.L4), "changeLV2", KeyEvent.VK_C, "", _listener);
		_actionManager.add("changeLV3", themeManager.getResizableIcon("changeLV3", ICON_SIZE.L4), "changeLV3", KeyEvent.VK_C, "", _listener);
		_actionManager.add("changeLV4", themeManager.getResizableIcon("changeLV4", ICON_SIZE.L4), "changeLV4", KeyEvent.VK_C, "", _listener);
		_actionManager.add("showid", themeManager.getResizableIcon("showid", ICON_SIZE.L4), "Show ID", KeyEvent.VK_C, "", _listener);
		_actionManager.add("showindex", themeManager.getResizableIcon("showindex", ICON_SIZE.L4), "Show Index", KeyEvent.VK_C, "", _listener);
		_actionManager.add("removeanotation", themeManager.getResizableIcon("removeanotation", ICON_SIZE.L4), "Remove Anotation", KeyEvent.VK_C, "", _listener);
	}
	
	private void createMonosaccharideAction (ActionManager _actionManager, ThemeManager _theme, ActionListener _listerner, GlycanRendererAWT _glycanRender) {
		ICON_SIZE iconSize = ICON_SIZE.L3;
		ResidueRenderer rr = _glycanRender.getResidueRenderer();

		// bridge
		for(ResidueType a_oRT : CrossLinkedSubstituentDictionary.getCrossLinkedSubstituents()) {
			ImageResizableIconReducedMem icon = 
					new ImageResizableIconReducedMem(rr.getImage(a_oRT, iconSize.getSize()), iconSize.getSize(), iconSize.getSize());
			
			EurocarbResizableIcon eu_icon = new EurocarbResizableIcon(_theme, null, icon);
			eu_icon.setResizableIcon(icon);
			
			_actionManager.add("bridge=" + a_oRT.getName(), eu_icon, a_oRT.getDescription(), -1, "",  _listerner);
		}
		
		for (ResidueType t : ResidueDictionary.allResidues()) {
			ImageResizableIconReducedMem icon = 
					new ImageResizableIconReducedMem(rr.getImage(t, iconSize.getSize()), iconSize.getSize(), iconSize.getSize());

			EurocarbResizableIcon eu_icon = new EurocarbResizableIcon(_theme, null, icon);
			eu_icon.setResizableIcon(icon);

			_actionManager.add("change=" + t.getName(), eu_icon, t.getDescription(), -1, "", _listerner);

			if (t.canHaveParent()) {
				_actionManager.add("add=" + t.getName(), eu_icon, t.getDescription(), -1, (t.getToolbarOrder() != 0) ? ("ctrl " + t.getToolbarOrder()) : "", _listerner);
				//ImageIcon last = new ImageIcon(rr.getImage(t, iconSize.getSize()));
				_actionManager.get("add=" + t.getName()).putValue(Action.SMALL_ICON, new ImageIcon(rr.getImage(t, ICON_SIZE.L3.getSize())));
			}
			if (t.canHaveParent() && t.canHaveChildren())
				_actionManager.add("insert=" + t.getName(), eu_icon, t.getDescription(), -1, "", _listerner);
			if (t.canBeReducingEnd())
				_actionManager.add("changeredend=" + t.getName(), eu_icon, t.getDescription(), -1, "", _listerner);
		}
	}
	
	/**
	 * Create GlyTouCan Button Action
	 * @author GIC 20210105
	 */
	private void createGlyTouCanAction(ActionManager _actionManager, ThemeManager _theme, ICON_SIZE defaultMenuIconSize, ActionListener _listener) {
		_actionManager.add("selectapidialog", _theme.getResizableIcon("export", defaultMenuIconSize), "Send", KeyEvent.VK_E, "", _listener);
		_actionManager.add("glycanidlist", _theme.getResizableIcon("report", defaultMenuIconSize), "GlyTouCanID List", KeyEvent.VK_E, "", _listener);
		_actionManager.add("edituser", _theme.getResizableIcon("actions/edit_user", defaultMenuIconSize), "Change User", KeyEvent.VK_E, "", _listener);
	}
}
