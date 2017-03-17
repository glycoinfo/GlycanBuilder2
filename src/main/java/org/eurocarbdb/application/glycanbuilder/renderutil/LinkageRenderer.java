package org.eurocarbdb.application.glycanbuilder.renderutil;

import java.awt.Rectangle;

import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.linkage.LinkageStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;

public interface LinkageRenderer {

	/**
	   Return the graphic options used by this object.
	 */
	public abstract GraphicOptions getGraphicOptions();

	/**
	   Set the graphic options used by this object.
	 */
	public abstract void setGraphicOptions(GraphicOptions opt);

	/**
	   Return the linkage style dictionary used by this object.
	 */
	public abstract LinkageStyleDictionary getLinkageStyleDictionary();

	/**
	   Set the linkage style dictionary used by this object.
	 */
	public abstract void setLinkageStyleDictionary(
			LinkageStyleDictionary linkageStyleDictionary);

	/**
	   Draw the line part of a linkage on a graphic context using the
	   specified bounding boxes.
	   @param paintable the graphic context
	   @param link the linkage to be drawn
	   @param selected <code>true</code> if the residue should be
	   shown as selected
	   @param parent_bbox the bounding box of the parent residue
	   @param parent_border_bbox the bounding box of the parent
	   residue including the residues on border
	   @param child_bbox the bounding box of the child residue
	   @param child_border_bbox the bounding box of the child residue
	   including the residues on border
	 */
	public abstract void paintEdge(Paintable paintable, Linkage link,
			boolean selected, Rectangle parent_bbox,
			Rectangle parent_border_bbox, Rectangle child_bbox,
			Rectangle child_border_bbox);

	/**
	   Draw the text part of a linkage on a graphic context using the
	   specified bounding boxes.
	   @param paintable the graphic context
	   @param link the linkage to be drawn
	   @param parent_bbox the bounding box of the parent residue
	   @param parent_border_bbox the bounding box of the parent
	   residue including the residues on border
	   @param child_bbox the bounding box of the child residue
	   @param child_border_bbox the bounding box of the child residue
	   including the residues on border
	 */
	public abstract void paintInfo(Paintable paintable, Linkage link,
			Rectangle parent_bbox, Rectangle parent_border_bbox,
			Rectangle child_bbox, Rectangle child_border_bbox);

}