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

package org.eurocarbdb.application.glycanbuilder.util;

import org.eurocarbdb.application.glycanbuilder.Configuration;
import org.eurocarbdb.application.glycanbuilder.renderutil.ResAngle;


/**
 * This class contains the graphic options used to determine the display of the
 * glycan structures. Objects of this class are used by the renderers to
 * determine the aspect of residues and linkages. To change the aspect of glycan
 * structures the {@link #NOTATION} and {@link #DISPLAY} variables are used. To
 * change the value of the graphic options and specify a custom aspect the
 * {@link #DISPLAY} variale should be set to {@link #DISPLAY_CUSTOM} and the
 * user should set the variables ending with _CUSTOM.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class GraphicOptions {

	// visualization options
	/**
	 * Cartoon notation used to display the structures, influences the graphic
	 * style of residues and linkages
	 */
	public String NOTATION = NOTATION_SNFG;
	/** CFG notation (default) */
	public static final String NOTATION_CFG = "cfg";
	/** Black and white CFG notation */
	public static final String NOTATION_CFGBW = "cfgbw";
	/** CFG notation with residue position depending on linkages */
	public static final String NOTATION_CFGLINK = "cfglink";
	/** UOXF notation */
	public static final String NOTATION_UOXF = "uoxf";
	/** 2D text only notation */
	public static final String NOTATION_TEXT = "text";
	/** UOXF colour */
	public static final String NOTATION_UOXFCOL = "uoxfcol";
	/** SNFG notation */
	public static final String NOTATION_SNFG = "snfg";

	public String THEME = "org.pushingpixels.substance.api.skin.OfficeBlue2007Skin";

	/**
	 * Specify one of the possible preset variations on the basic cartoon
	 * notation. To set the value of this variable the setDisplay() method must
	 * be used
	 */
	public String DISPLAY = DISPLAY_NORMALINFO;
	/** Use the custom settings defined by the user */
	public static final String DISPLAY_CUSTOM = "custom";
	/**
	 * Reduce the space between residues and do not display the linkage
	 * information
	 */
	public static final String DISPLAY_COMPACT = "compact";
	/** Do not display the linkage information */
	public static final String DISPLAY_NORMAL = "normal";
	/** Default representation */
	public static final String DISPLAY_NORMALINFO = "normalinfo";

	/** Specify the orientation of the structure */
	public int ORIENTATION = RL;
	/** Right-to-left orientation (default) */
	public static final int RL = 0;
	/** Bottom-to-top orientation */
	public static final int BT = 1;
	/** Left-to-right orientation */
	public static final int LR = 2;
	/** Top-to-bottom orientation */
	public static final int TB = 3;

	// canvas options
	/**
	 * Specify the scaling factor to be applied to the drawing canvas only
	 * (default = 1)
	 */
	public double SCALE_CANVAS = 1.;
	/**
	 * Specify if antennae with unspecified connectivity and the same structure
	 * should be grouped together (default = true)
	 */
	public boolean COLLAPSE_MULTIPLE_ANTENNAE = true;
	/**
	 * Specify if the mass information should be displayed in the drawing canvas
	 * (default = true)
	 */
	public boolean SHOW_MASSES_CANVAS = true;
	/**
	 * Specify if the reducing-end marker should be displayed in the drawing
	 * canvas (default = true)
	 */
	public boolean SHOW_REDEND_CANVAS = true;

	// exporting options
	/**
	 * Specify if the mass information should be displayed when exporting
	 * (default = false)
	 */
	public boolean SHOW_MASSES = false;
	/**
	 * Specify if the reducing-end marker should be displayed when exporting
	 * (default = false)
	 */
	public boolean SHOW_REDEND = false;

	// size options
	/** Specify if the linkage information should be displayed (default = true) */
	public boolean SHOW_INFO = true;

	/** Margin to the left of the drawing */
	public int MARGIN_LEFT = MARGIN_LEFT_DEFAULT;
	/** Margin at the top of the drawing */
	public int MARGIN_TOP = MARGIN_TOP_DEFAULT;
	/** Margin to the right of the drawing */
	public int MARGIN_RIGHT = MARGIN_RIGHT_DEFAULT;
	/** Margin at the bottom of the drawing */
	public int MARGIN_BOTTOM = MARGIN_BOTTOM_DEFAULT;

	/** Size of the bounding box of a residue. */
	public int NODE_SIZE = NODE_SIZE_DEFAULT;
	/** Size of the font used to display text on a residue */
	public int NODE_FONT_SIZE = NODE_FONT_SIZE_DEFAULT;
	/** Type of the font used to display text on a residue */
	public String NODE_FONT_FACE = NODE_FONT_FACE_DEFAULT;
	/** Size of the font used to display a glycan composition */
	public int COMPOSITION_FONT_SIZE = COMPOSITION_FONT_SIZE_DEFAULT;
	/** Type of the font used to display a glycan composition */
	public String COMPOSITION_FONT_FACE = COMPOSITION_FONT_FACE_DEFAULT;
	/** Size of the font used to display the linkage information */
	public int LINKAGE_INFO_SIZE = LINKAGE_INFO_SIZE_DEFAULT;
	/** Type of the font used to display the linkage information */
	public String LINKAGE_INFO_FONT_FACE = LINKAGE_INFO_FONT_FACE_DEFAULT;

	/** Space between the residues */
	public int NODE_SPACE = NODE_SPACE_DEFAULT;
	/** Space between the residue and the border residues */
	public int NODE_SUB_SPACE = NODE_SUB_SPACE_DEFAULT;
	/** Space between the structures */
	public int STRUCTURES_SPACE = STRUCTURES_SPACE_DEFAULT;
	/** Space between the structure and the mass information */
	public int MASS_TEXT_SPACE = MASS_TEXT_SPACE_DEFAULT;
	/** Size of the font used to display the mass information */
	public int MASS_TEXT_SIZE = MASS_TEXT_SIZE_DEFAULT;
	/** Type of the font used to display the mass information */
	public String MASS_TEXT_FONT_FACE = MASS_TEXT_FONT_FACE_DEFAULT;

	/**
	 * Scaling factor applied to the structure. To set the value of this
	 * variable the setScale() method must be used
	 */
	public double SCALE = 1.;

	// size customs
	/** User-defined value for the {@link #MARGIN_LEFT} variable */
	public int MARGIN_LEFT_CUSTOM = MARGIN_LEFT_DEFAULT;
	/** User-defined value for the {@link #MARGIN_TOP} variable */
	public int MARGIN_TOP_CUSTOM = MARGIN_TOP_DEFAULT;
	/** User-defined value for the {@link #MARGIN_RIGHT} variable */
	public int MARGIN_RIGHT_CUSTOM = MARGIN_RIGHT_DEFAULT;
	/** User-defined value for the {@link #MARGIN_BOTTOM} variable */
	public int MARGIN_BOTTOM_CUSTOM = MARGIN_BOTTOM_DEFAULT;

	/** User-defined value for the {@link #SHOW_INFO} variable */
	public boolean SHOW_INFO_CUSTOM = true;

	/** User-defined value for the {@link #NODE_SIZE} variable */
	public int NODE_SIZE_CUSTOM = NODE_SIZE_DEFAULT;
	/** User-defined value for the {@link #NODE_FONT_SIZE} variable */
	public int NODE_FONT_SIZE_CUSTOM = NODE_FONT_SIZE_DEFAULT;
	/** User-defined value for the {@link #NODE_FONT_FACE} variable */
	public String NODE_FONT_FACE_CUSTOM = NODE_FONT_FACE_DEFAULT;

	/** User-defined value for the {@link #COMPOSITION_FONT_SIZE} variable */
	public int COMPOSITION_FONT_SIZE_CUSTOM = COMPOSITION_FONT_SIZE_DEFAULT;
	/** User-defined value for the {@link #COMPOSITION_FONT_FACE} variable */
	public String COMPOSITION_FONT_FACE_CUSTOM = COMPOSITION_FONT_FACE_DEFAULT;

	/** User-defined value for the {@link #LINKAGE_INFO_SIZE} variable */
	public int LINKAGE_INFO_SIZE_CUSTOM = LINKAGE_INFO_SIZE_DEFAULT;
	/** User-defined value for the {@link #LINKAGE_INFO_FONT_FACE} variable */
	public String LINKAGE_INFO_FONT_FACE_CUSTOM = LINKAGE_INFO_FONT_FACE_DEFAULT;

	/** User-defined value for the {@link #NODE_SPACE} variable */
	public int NODE_SPACE_CUSTOM = NODE_SPACE_DEFAULT;
	/** User-defined value for the {@link #NODE_SUB_SPACE} variable */
	public int NODE_SUB_SPACE_CUSTOM = NODE_SUB_SPACE_DEFAULT;

	/** User-defined value for the {@link #STRUCTURES_SPACE} variable */
	public int STRUCTURES_SPACE_CUSTOM = STRUCTURES_SPACE_DEFAULT;

	/** User-defined value for the {@link #MASS_TEXT_SPACE} variable */
	public int MASS_TEXT_SPACE_CUSTOM = MASS_TEXT_SPACE_DEFAULT;
	/** User-defined value for the {@link #MASS_TEXT_SIZE} variable */
	public int MASS_TEXT_SIZE_CUSTOM = MASS_TEXT_SIZE_DEFAULT;
	/** User-defined value for the {@link #MASS_TEXT_FONT_FACE} variable */
	public String MASS_TEXT_FONT_FACE_CUSTOM = MASS_TEXT_FONT_FACE_DEFAULT;

	// size constants
	private static final int MARGIN_LEFT_DEFAULT = 30;
	private static final int MARGIN_TOP_DEFAULT = 30;
	private static final int MARGIN_RIGHT_DEFAULT = 30;
	private static final int MARGIN_BOTTOM_DEFAULT = 30;

	private static final int NODE_SIZE_DEFAULT = 22;
	private static final int NODE_FONT_SIZE_DEFAULT = 14;
	private static final String NODE_FONT_FACE_DEFAULT = "SansSerif.plain";
	private static final int COMPOSITION_FONT_SIZE_DEFAULT = 18;
	private static final String COMPOSITION_FONT_FACE_DEFAULT = "SansSerif.plain";
	private static final int LINKAGE_INFO_SIZE_DEFAULT = 12;
	private static final String LINKAGE_INFO_FONT_FACE_DEFAULT = "Serif.plain";

	private static final int NODE_SPACE_DEFAULT = 30;
	private static final int NODE_SUB_SPACE_DEFAULT = 1;

	private static final int MASS_TEXT_SPACE_DEFAULT = 15;
	private static final int MASS_TEXT_SIZE_DEFAULT = 14;
	private static final String MASS_TEXT_FONT_FACE_DEFAULT = "SansSerif.plain";

	private static final int STRUCTURES_SPACE_DEFAULT = 40;

	public static final int ICON_SIZE_MEDIUM = 26;
	public static final int ICON_SIZE_SMALL = 26;

	 
	private static final boolean SAVE_SPECTRA_DEFAULT = false;
	private boolean SAVE_SPECTRA=SAVE_SPECTRA_DEFAULT;
	public boolean SAVE_SPECTRA_CUSTOM = SAVE_SPECTRA_DEFAULT;

	// methods

	/**
	 * Default constructor.
	 */
	public GraphicOptions() {
	}

	/**
	 * Copy constructor.
	 */
	public GraphicOptions(GraphicOptions other) {
		if (other != null)
			this.copy(other);
	}

	/**
	 * Default constructor with display option specified
	 */
	public GraphicOptions(String display) {
		SCALE = 1.;
		setDisplay(display);
	}

	/**
	 * Convenience method that return all the font types available in the
	 * system.
	 */
	static public java.util.Vector<String> getAllFontFaces() {
		java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment
				.getLocalGraphicsEnvironment();

		java.util.Vector<String> ret = new java.util.Vector<String>();
		java.awt.Font[] all_fonts = ge.getAllFonts();
		for (int i = 0; i < all_fonts.length; i++)
			ret.add(all_fonts[i].getFontName());

		return ret;
	}

	/**
	 * Init the user-defined graphic options to the values corresponding to a
	 * specific preset.
	 */
	public void initCustomDisplay(String display) {
		MARGIN_LEFT_CUSTOM = MARGIN_LEFT_DEFAULT;
		MARGIN_TOP_CUSTOM = MARGIN_TOP_DEFAULT;
		MARGIN_RIGHT_CUSTOM = MARGIN_RIGHT_DEFAULT;
		MARGIN_BOTTOM_CUSTOM = MARGIN_BOTTOM_DEFAULT;

		NODE_SIZE_CUSTOM = NODE_SIZE_DEFAULT;

		NODE_FONT_SIZE_CUSTOM = NODE_FONT_SIZE_DEFAULT;
		NODE_FONT_FACE_CUSTOM = NODE_FONT_FACE_DEFAULT;

		COMPOSITION_FONT_SIZE_CUSTOM = COMPOSITION_FONT_SIZE_DEFAULT;
		COMPOSITION_FONT_FACE_CUSTOM = COMPOSITION_FONT_FACE_DEFAULT;

		LINKAGE_INFO_SIZE_CUSTOM = LINKAGE_INFO_SIZE_DEFAULT;
		LINKAGE_INFO_FONT_FACE_CUSTOM = LINKAGE_INFO_FONT_FACE_DEFAULT;

		NODE_SUB_SPACE_CUSTOM = NODE_SUB_SPACE_DEFAULT;

		STRUCTURES_SPACE_CUSTOM = STRUCTURES_SPACE_DEFAULT;

		MASS_TEXT_SPACE_CUSTOM = MASS_TEXT_SPACE_DEFAULT;
		MASS_TEXT_SIZE_CUSTOM = MASS_TEXT_SIZE_DEFAULT;
		MASS_TEXT_FONT_FACE_CUSTOM = MASS_TEXT_FONT_FACE_DEFAULT;

		if (display.equals(DISPLAY_COMPACT)) {
			NODE_SPACE_CUSTOM = (int) (NODE_SPACE_DEFAULT * SCALE / 2.);
			SHOW_INFO_CUSTOM = false;
		} else if (display.equals(DISPLAY_NORMAL)) {
			NODE_SPACE_CUSTOM = (int) (NODE_SPACE_DEFAULT * SCALE);
			SHOW_INFO_CUSTOM = false;
		} else {
			NODE_SPACE_CUSTOM = (int) (NODE_SPACE_DEFAULT * SCALE);
			SHOW_INFO_CUSTOM = (SCALE >= 1.);
		}
		
		SAVE_SPECTRA_CUSTOM=SAVE_SPECTRA_DEFAULT;
	}

	/**
	 * Set the display variable and init all the graphic options accordingly.
	 */
	public void setDisplay(String display) {

		if (!display.equals(DISPLAY_CUSTOM)) {
			// default settings

			MARGIN_LEFT = (int) (MARGIN_LEFT_DEFAULT * SCALE);
			MARGIN_TOP = (int) (MARGIN_TOP_DEFAULT * SCALE);
			MARGIN_RIGHT = (int) (MARGIN_RIGHT_DEFAULT * SCALE);
			MARGIN_BOTTOM = (int) (MARGIN_BOTTOM_DEFAULT * SCALE);

			NODE_SIZE = (int) (NODE_SIZE_DEFAULT * SCALE);

			NODE_FONT_SIZE = Math.max(10,
					(int) (NODE_FONT_SIZE_DEFAULT * SCALE));
			NODE_FONT_FACE = NODE_FONT_FACE_DEFAULT;

			COMPOSITION_FONT_SIZE = Math.max(14,
					(int) (COMPOSITION_FONT_SIZE_DEFAULT * SCALE));
			COMPOSITION_FONT_FACE = COMPOSITION_FONT_FACE_DEFAULT;

			LINKAGE_INFO_SIZE = Math.max(8,
					(int) (LINKAGE_INFO_SIZE_DEFAULT * SCALE));
			LINKAGE_INFO_FONT_FACE = LINKAGE_INFO_FONT_FACE_DEFAULT;

			NODE_SUB_SPACE = (int) (NODE_SUB_SPACE_DEFAULT * SCALE);

			STRUCTURES_SPACE = (int) (STRUCTURES_SPACE_DEFAULT * SCALE);

			MASS_TEXT_SPACE = (int) (MASS_TEXT_SPACE_DEFAULT * SCALE);
			MASS_TEXT_SIZE = Math.max(10,
					(int) (MASS_TEXT_SIZE_DEFAULT * SCALE));
			MASS_TEXT_FONT_FACE = MASS_TEXT_FONT_FACE_DEFAULT;

			if (display.equals(DISPLAY_COMPACT)) {
				NODE_SPACE = (int) (NODE_SPACE_DEFAULT * SCALE / 2.);
				SHOW_INFO = false;
				DISPLAY = DISPLAY_COMPACT;
			} else if (display.equals(DISPLAY_NORMAL)) {
				NODE_SPACE = (int) (NODE_SPACE_DEFAULT * SCALE);
				SHOW_INFO = false;
				DISPLAY = DISPLAY_NORMAL;
			} else {
				NODE_SPACE = (int) (NODE_SPACE_DEFAULT * SCALE);
				SHOW_INFO = (SCALE >= 1.);
				DISPLAY = DISPLAY_NORMALINFO;
			}
			
			SAVE_SPECTRA=SAVE_SPECTRA_DEFAULT;
		} else {
			// custom settings

			MARGIN_LEFT = (int) (MARGIN_LEFT_CUSTOM * SCALE);
			MARGIN_TOP = (int) (MARGIN_TOP_CUSTOM * SCALE);
			MARGIN_RIGHT = (int) (MARGIN_RIGHT_CUSTOM * SCALE);
			MARGIN_BOTTOM = (int) (MARGIN_BOTTOM_CUSTOM * SCALE);

			SHOW_INFO = SHOW_INFO_CUSTOM && (SCALE >= 1.);

			NODE_SIZE = (int) (NODE_SIZE_CUSTOM * SCALE);

			NODE_FONT_SIZE = Math
					.max(10, (int) (NODE_FONT_SIZE_CUSTOM * SCALE));
			NODE_FONT_FACE = NODE_FONT_FACE_CUSTOM;

			COMPOSITION_FONT_SIZE = Math.max(14,
					(int) (COMPOSITION_FONT_SIZE_CUSTOM * SCALE));
			COMPOSITION_FONT_FACE = COMPOSITION_FONT_FACE_CUSTOM;

			LINKAGE_INFO_SIZE = Math.max(8,
					(int) (LINKAGE_INFO_SIZE_CUSTOM * SCALE));
			LINKAGE_INFO_FONT_FACE = LINKAGE_INFO_FONT_FACE_CUSTOM;

			NODE_SPACE = (int) (NODE_SPACE_CUSTOM * SCALE);
			NODE_SUB_SPACE = (int) (NODE_SUB_SPACE_CUSTOM * SCALE);

			STRUCTURES_SPACE = (int) (STRUCTURES_SPACE_CUSTOM * SCALE);

			MASS_TEXT_SPACE = (int) (MASS_TEXT_SPACE_CUSTOM * SCALE);
			MASS_TEXT_SIZE = Math
					.max(10, (int) (MASS_TEXT_SIZE_CUSTOM * SCALE));
			MASS_TEXT_FONT_FACE = MASS_TEXT_FONT_FACE_CUSTOM;

			DISPLAY = DISPLAY_CUSTOM;
			
			SAVE_SPECTRA=SAVE_SPECTRA_CUSTOM;
		}
	}

	/**
	 * Return the orientation as a {@link ResAngle} object
	 */
	public ResAngle getOrientationAngle() {
		return getOrientationAngle(ORIENTATION);
	}

	/**
	 * Return an orientation as a {@link ResAngle} object
	 */
	public static ResAngle getOrientationAngle(int orientation) {
		if (orientation == LR)
			return new ResAngle(0);
		if (orientation == RL)
			return new ResAngle(180);
		if (orientation == BT)
			return new ResAngle(-90);
		if (orientation == TB)
			return new ResAngle(90);
		return new ResAngle(0);
	}

	/**
	 * Set the scaling factor to a specific value.
	 */
	public double setScale(double scale) {
		// compute nearest feasible scale
		// solve problem of drawing triangles and diamonds
		if (!DISPLAY.equals(DISPLAY_CUSTOM)) {
			if ((((int) (NODE_SIZE_DEFAULT * scale)) % 2) == 1)
				SCALE = (double) ((int) (NODE_SIZE_DEFAULT * scale) + 1)
						/ (double) NODE_SIZE_DEFAULT;
			else
				SCALE = scale;
		} else {
			if ((((int) (NODE_SIZE_CUSTOM * scale)) % 2) == 1)
				SCALE = (double) ((int) (NODE_SIZE_CUSTOM * scale) + 1)
						/ (double) NODE_SIZE_CUSTOM;
			else
				SCALE = scale;
		}

		// set options
		setDisplay(DISPLAY);

		// return effective scale
		return SCALE;
	}

	/**
	 * Store the options in the configuration
	 */
	public void store(Configuration config) {
		config.put("GraphicOptions", "notation", NOTATION);
		config.put("GraphicOptions", "display", DISPLAY);
		config.put("GraphicOptions", "orientation", ORIENTATION);

		config.put("GraphicOptions", "show_info", SHOW_INFO);
		config.put("GraphicOptions", "scale_canvas", SCALE_CANVAS);
		config.put("GraphicOptions", "collapse_multiple_antennae",
				COLLAPSE_MULTIPLE_ANTENNAE);
		config.put("GraphicOptions", "show_masses_canvas", SHOW_MASSES_CANVAS);
		config.put("GraphicOptions", "show_masses", SHOW_MASSES);
		config.put("GraphicOptions", "show_redend", SHOW_REDEND);

		config.put("GraphicOptions", "show_info_custom", SHOW_INFO_CUSTOM);

		config.put("GraphicOptions", "node_size_custom", NODE_SIZE_CUSTOM);
		config.put("GraphicOptions", "node_font_size_custom",
				NODE_FONT_SIZE_CUSTOM);
		config.put("GraphicOptions", "node_font_face_custom",
				NODE_FONT_FACE_CUSTOM);

		config.put("GraphicOptions", "composition_font_size_custom",
				COMPOSITION_FONT_SIZE_CUSTOM);
		config.put("GraphicOptions", "composition_font_face_custom",
				COMPOSITION_FONT_FACE_CUSTOM);

		config.put("GraphicOptions", "linkage_info_size_custom",
				LINKAGE_INFO_SIZE_CUSTOM);
		config.put("GraphicOptions", "linkage_info_font_face_custom",
				LINKAGE_INFO_FONT_FACE_CUSTOM);

		config.put("GraphicOptions", "node_space_custom", NODE_SPACE_CUSTOM);
		config.put("GraphicOptions", "node_sub_space_custom",
				NODE_SUB_SPACE_CUSTOM);

		config.put("GraphicOptions", "structures_space_custom",
				STRUCTURES_SPACE_CUSTOM);

		config.put("GraphicOptions", "mass_text_space_custom",
				MASS_TEXT_SPACE_CUSTOM);
		config.put("GraphicOptions", "mass_text_size_custom",
				MASS_TEXT_SIZE_CUSTOM);
		config.put("GraphicOptions", "mass_text_font_face_custom",
				MASS_TEXT_FONT_FACE_CUSTOM);

		config.put("GraphicOptions", "theme", THEME);
		config.put("GraphicOptions", "save_spectra", SAVE_SPECTRA_CUSTOM);
	}

	/**
	 * Retrieve the options from the configuration
	 */
	public void retrieve(Configuration config) {
		NOTATION = config.get("GraphicOptions", "notation", NOTATION);
		DISPLAY = config.get("GraphicOptions", "display", DISPLAY);
		ORIENTATION = config.get("GraphicOptions", "orientation", ORIENTATION);

		SHOW_INFO = config.get("GraphicOptions", "show_info", SHOW_INFO);
		SCALE_CANVAS = config.get("GraphicOptions", "scale_canvas",
				SCALE_CANVAS);
		COLLAPSE_MULTIPLE_ANTENNAE = config.get("GraphicOptions",
				"collapse_multiple_antennae", COLLAPSE_MULTIPLE_ANTENNAE);
		SHOW_MASSES_CANVAS = config.get("GraphicOptions", "show_masses_canvas",
				SHOW_MASSES_CANVAS);
		SHOW_MASSES = config.get("GraphicOptions", "show_masses", SHOW_MASSES);
		SHOW_REDEND = config.get("GraphicOptions", "show_redend", SHOW_REDEND);

		SHOW_INFO_CUSTOM = config.get("GraphicOptions", "show_info_custom",
				SHOW_INFO_CUSTOM);

		NODE_SIZE_CUSTOM = config.get("GraphicOptions", "node_size_custom",
				NODE_SIZE_CUSTOM);
		NODE_FONT_SIZE_CUSTOM = config.get("GraphicOptions",
				"node_font_size_custom", NODE_FONT_SIZE_CUSTOM);
		NODE_FONT_FACE_CUSTOM = config.get("GraphicOptions",
				"node_font_face_custom", NODE_FONT_FACE_CUSTOM);

		COMPOSITION_FONT_SIZE_CUSTOM = config.get("GraphicOptions",
				"composition_font_size_custom", COMPOSITION_FONT_SIZE_CUSTOM);
		COMPOSITION_FONT_FACE_CUSTOM = config.get("GraphicOptions",
				"composition_font_face_custom", COMPOSITION_FONT_FACE_CUSTOM);

		LINKAGE_INFO_SIZE_CUSTOM = config.get("GraphicOptions",
				"linkage_info_size_custom", LINKAGE_INFO_SIZE_CUSTOM);
		LINKAGE_INFO_FONT_FACE_CUSTOM = config.get("GraphicOptions",
				"linkage_info_font_face_custom", LINKAGE_INFO_FONT_FACE_CUSTOM);

		NODE_SPACE_CUSTOM = config.get("GraphicOptions", "node_space_custom",
				NODE_SPACE_CUSTOM);
		NODE_SUB_SPACE_CUSTOM = config.get("GraphicOptions",
				"node_sub_space_custom", NODE_SUB_SPACE_CUSTOM);

		STRUCTURES_SPACE_CUSTOM = config.get("GraphicOptions",
				"structures_space_custom", STRUCTURES_SPACE_CUSTOM);

		MASS_TEXT_SPACE_CUSTOM = config.get("GraphicOptions",
				"mass_text_space_custom", MASS_TEXT_SPACE_CUSTOM);
		MASS_TEXT_SIZE_CUSTOM = config.get("GraphicOptions",
				"mass_text_size_custom", MASS_TEXT_SIZE_CUSTOM);
		MASS_TEXT_FONT_FACE_CUSTOM = config.get("GraphicOptions",
				"mass_text_font_face_custom", MASS_TEXT_FONT_FACE_CUSTOM);
		THEME = config.get("GraphicOptions", "theme", THEME);
		SAVE_SPECTRA_CUSTOM=config.get("GraphicOptions", "save_spectra", true);
	}

	/**
	 * Create a copy of the object.
	 */
	public GraphicOptions clone() {
		GraphicOptions ret = new GraphicOptions();
		ret.copy(this);
		return ret;
	}

	/**
	 * Copy the values from another GraphicOptions object.
	 */
	public void copy(GraphicOptions other) {

		this.NOTATION = other.NOTATION;
		this.DISPLAY = other.DISPLAY;
		this.ORIENTATION = other.ORIENTATION;

		this.SCALE_CANVAS = other.SCALE_CANVAS;
		this.COLLAPSE_MULTIPLE_ANTENNAE = other.COLLAPSE_MULTIPLE_ANTENNAE;
		this.SHOW_MASSES_CANVAS = other.SHOW_MASSES_CANVAS;

		this.SHOW_MASSES = other.SHOW_MASSES;
		this.SHOW_REDEND = other.SHOW_REDEND;
		this.SHOW_INFO = other.SHOW_INFO;

		this.MARGIN_LEFT = other.MARGIN_LEFT;
		this.MARGIN_TOP = other.MARGIN_TOP;
		this.MARGIN_RIGHT = other.MARGIN_RIGHT;
		this.MARGIN_BOTTOM = other.MARGIN_BOTTOM;

		this.NODE_SIZE = other.NODE_SIZE;
		this.NODE_FONT_SIZE = other.NODE_FONT_SIZE;
		this.NODE_FONT_FACE = other.NODE_FONT_FACE;
		this.COMPOSITION_FONT_SIZE = other.COMPOSITION_FONT_SIZE;
		this.COMPOSITION_FONT_FACE = other.COMPOSITION_FONT_FACE;
		this.LINKAGE_INFO_SIZE = other.LINKAGE_INFO_SIZE;
		this.LINKAGE_INFO_FONT_FACE = other.LINKAGE_INFO_FONT_FACE;

		this.NODE_SPACE = other.NODE_SPACE;
		this.NODE_SUB_SPACE = other.NODE_SUB_SPACE;

		this.STRUCTURES_SPACE = other.STRUCTURES_SPACE;

		this.MASS_TEXT_SPACE = other.MASS_TEXT_SPACE;
		this.MASS_TEXT_SIZE = other.MASS_TEXT_SIZE;
		this.MASS_TEXT_FONT_FACE = other.MASS_TEXT_FONT_FACE;

		this.SHOW_INFO_CUSTOM = other.SHOW_INFO_CUSTOM;

		this.NODE_SIZE_CUSTOM = other.NODE_SIZE_CUSTOM;
		this.NODE_FONT_SIZE_CUSTOM = other.NODE_FONT_SIZE_CUSTOM;
		this.NODE_FONT_FACE_CUSTOM = other.NODE_FONT_FACE_CUSTOM;

		this.COMPOSITION_FONT_SIZE_CUSTOM = other.COMPOSITION_FONT_SIZE_CUSTOM;
		this.COMPOSITION_FONT_FACE_CUSTOM = other.COMPOSITION_FONT_FACE_CUSTOM;

		this.LINKAGE_INFO_SIZE_CUSTOM = other.LINKAGE_INFO_SIZE_CUSTOM;
		this.LINKAGE_INFO_FONT_FACE_CUSTOM = other.LINKAGE_INFO_FONT_FACE_CUSTOM;

		this.NODE_SPACE_CUSTOM = other.NODE_SPACE_CUSTOM;
		this.NODE_SUB_SPACE_CUSTOM = other.NODE_SUB_SPACE_CUSTOM;

		this.STRUCTURES_SPACE_CUSTOM = other.STRUCTURES_SPACE_CUSTOM;

		this.MASS_TEXT_SPACE_CUSTOM = other.MASS_TEXT_SPACE_CUSTOM;
		this.MASS_TEXT_SIZE_CUSTOM = other.MASS_TEXT_SIZE_CUSTOM;
		this.MASS_TEXT_FONT_FACE_CUSTOM = other.MASS_TEXT_FONT_FACE_CUSTOM;

		this.SCALE = other.SCALE;
		this.THEME = other.THEME;
		
		this.SAVE_SPECTRA_CUSTOM=other.SAVE_SPECTRA_CUSTOM;
	}

}
