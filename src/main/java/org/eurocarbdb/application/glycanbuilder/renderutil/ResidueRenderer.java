package org.eurocarbdb.application.glycanbuilder.renderutil;

import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.Icon;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;

public interface ResidueRenderer {

	/**
	   Return the graphic options used by this object.
	 */
	public abstract GraphicOptions getGraphicOptions();

	/**
	   Set the graphic options used by this object.
	 */
	public abstract void setGraphicOptions(GraphicOptions opt);

	/**
	   Return the residue style dictionary used by this object.
	 */
	public abstract ResidueStyleDictionary getResidueStyleDictionary();

	/**
	   Set the residue style dictionary used by this object.
	 */
	public abstract void setResidueStyleDictionary(
			ResidueStyleDictionary residueStyleDictionary);

	/**
	   Return a graphical representation of a residue type as an icon
	   of <code>max_y_size</code> height.
	 */
	public abstract Icon getIcon(ResidueType type, int max_y_size);

	public abstract Image getImage(ResidueType type, int max_y_size);

	/**
	   Return the text to be written in the residue representation
	   given the residue style in the current notation.
	 */
	public abstract String getText(Residue node);

	/**
	   Return the text to be written in the residue representation
	   given the residue style in the current notation.
	   @param on_border <code>true</code> if the residue is displayed
	   on the border of its parent, used for substitutions and
	   modifications
	 */
	public abstract String getText(Residue node, boolean on_border);

	/**
	   Draw a residue on a graphic context using the specified
	   bounding box.
	   @param paintable the graphic context
	   @param node the residue to be drawn
	   @param selected <code>true</code> if the residue should be
	   shown as selected
	   @param on_border <code>true</code> if the residue should be
	   drawn on the border of its parent
	   @param par_bbox the bounding box of the parent residue
	   @param cur_bbox the bounding box of the current residue
	   @param sup_bbox the bounding box used to decide the spatial
	   orientation of the residue
	   @param orientation the orientation of the residue
	 */
	public abstract void paint(Paintable paintable, Residue node, boolean selected,
			boolean on_border, Rectangle par_bbox, Rectangle cur_bbox,
			Rectangle sup_bbox, ResAngle orientation);

	/**
	   Draw a residue on a graphic context using the specified
	   bounding box.
	   @param paintable the graphic context
	   @param node the residue to be drawn
	   @param selected <code>true</code> if the residue should be
	   shown as selected
	   @param active <code>true</code> if the residue should be
	   shown as active
	   @param on_border <code>true</code> if the residue should be
	   drawn on the border of its parent
	   @param par_bbox the bounding box of the parent residue
	   @param cur_bbox the bounding box of the current residue
	   @param sup_bbox the bounding box used to decide the spatial
	   orientation of the residue
	   @param orientation the orientation of the residue
	 */
	public abstract void paint(Paintable paintable, Residue node, boolean selected,
			boolean active, boolean on_border, Rectangle par_bbox,
			Rectangle cur_bbox, Rectangle sup_bbox, ResAngle orientation);

	public abstract Rectangle computeBoundingBox(Residue node,
			boolean onBorder, int i, int j, ResAngle orientation,
			int nODE_SIZE, int maxValue);

}