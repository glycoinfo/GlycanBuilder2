package org.eurocarbdb.application.glycanbuilder.renderutil;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.ResiduePlacementDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.linkage.LinkageStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.WURCSFramework.util.residuecontainer.LinkageBlock;
import org.glycoinfo.WURCSFramework.util.residuecontainer.ResidueContainer;

public interface GlycanRenderer {
	public abstract void setRenderMode(GlycanRendererMode mode);
	
	public abstract GlycanRendererMode getRenderMode();

	/**
	 * Return the residue renderer used by this object.
	 */
	public abstract ResidueRenderer getResidueRenderer();

	/**
	 * Set the residue renderer used by this object.
	 */
	public abstract void setResidueRenderer(ResidueRenderer r);

	/**
	 * Return the linkage renderer used by this object.
	 */
	public abstract LinkageRenderer getLinkageRenderer();

	/**
	 * Set the linkage renderer used by this object.
	 */
	public abstract void setLinkageRenderer(LinkageRenderer r);

	/**
	 * Return the graphic options used by this object.
	 */
	public abstract GraphicOptions getGraphicOptions();

	/**
	 * Set the graphic options used by this object.
	 */
	public abstract void setGraphicOptions(GraphicOptions opt);

	/**
	 * Return the residue placement dictionary used by this object.
	 */
	public abstract ResiduePlacementDictionary getResiduePlacementDictionary();

	/**
	 * Set the residue placement dictionary used by this object.
	 */
	public abstract void setResiduePlacementDictionary(
			ResiduePlacementDictionary residuePlacementDictionary);

	/**
	 * Return the residue style dictionary used by this object.
	 */
	public abstract ResidueStyleDictionary getResidueStyleDictionary();

	/**
	 * Set the residue style dictionary used by this object.
	 */
	public abstract void setResidueStyleDictionary(
			ResidueStyleDictionary residueStyleDictionary);

	/**
	 * Return the linkage style dictionary used by this object.
	 */
	public abstract LinkageStyleDictionary getLinkageStyleDictionary();

	/**
	 * Set the linkage style dictionary used by this object.
	 */
	public abstract void setLinkageStyleDictionary(
			LinkageStyleDictionary linkageStyleDictionary);

	/**
	 * Draw a glycan structure on a graphics context using the calculated
	 * bounding boxes.
	 * 
	 * @param paintable
	 *            the graphic context
	 * @param structure
	 *            the glycan structure to be drawn
	 * @param selected_residues
	 *            the set of residues that must be shown as selected
	 * @param selected_linkages
	 *            the set of linkages that must be shown as selected
	 * @param show_mass
	 *            <code>true</code> if the mass information about the structure
	 *            should be displayed
	 * @param show_redend
	 *            <code>true</code> if the reducing end marker should be
	 *            displayed
	 * @param posManager
	 *            the object used to spatially arrange the residues
	 * @param bboxManager
	 *            the object used to store the residue bounding boxes
	 */
	public abstract void paint(Paintable paintable, Glycan structure,
			HashSet<Residue> selected_residues,
			HashSet<Linkage> selected_linkages, boolean show_mass,
			boolean show_redend, PositionManager posManager,
			BBoxManager bboxManager);

	/**
	 * Draw a glycan structure on a graphics context using the calculated
	 * bounding boxes.
	 * 
	 * @param paintable
	 *            the graphic context
	 * @param structure
	 *            the glycan structure to be drawn
	 * @param selected_residues
	 *            the set of residues that must be shown as selected
	 * @param selected_linkages
	 *            the set of linkages that must be shown as selected
	 * @param active_residues
	 *            the set of residues that are active, all the others will be
	 *            displayed with less bright colors
	 * @param show_mass
	 *            <code>true</code> if the mass information about the structure
	 *            should be displayed
	 * @param show_redend
	 *            <code>true</code> if the reducing end marker should be
	 *            displayed
	 * @param posManager
	 *            the object used to spatially arrange the residues
	 * @param bboxManager
	 *            the object used to store the residue bounding boxes
	 */
	public abstract void paint(Paintable paintable, Glycan structure,
			HashSet<Residue> selected_residues,
			HashSet<Linkage> selected_linkages,
			Collection<Residue> active_residues, boolean show_mass,
			boolean show_redend, PositionManager posManager,
			BBoxManager bboxManager);

	/**
	 * Add the margins to a structure bounding box.
	 */
	public abstract Dimension computeSize(Rectangle all_bbox);

	/**
	 * Compute the residue bounding boxes for a set of structures.
	 * 
	 * @param structures
	 *            the list of structures to be displayed
	 * @param show_masses
	 *            <code>true</code> if the mass information about the structure
	 *            should be displayed
	 * @param show_redend
	 *            <code>true</code> if the reducing end marker should be
	 *            displayed
	 * @param posManager
	 *            the object used to spatially arrange the residues
	 * @param bboxManager
	 *            the object used to store the residue bounding boxes
	 */
	public abstract Rectangle computeBoundingBoxes(
			Collection<Glycan> structures, boolean show_masses,
			boolean show_redend, PositionManager posManager,
			BBoxManager bboxManager);

	/**
	 * Compute the residue bounding boxes for a set of structures.
	 * 
	 * @param structures
	 *            the list of structures to be displayed
	 * @param show_masses
	 *            <code>true</code> if the mass information about the structure
	 *            should be displayed
	 * @param show_redend
	 *            <code>true</code> if the reducing end marker should be
	 *            displayed
	 * @param posManager
	 *            the object used to spatially arrange the residues
	 * @param bboxManager
	 *            the object used to store the residue bounding boxes
	 * @param reset
	 *            <code>true</code> if the bounding boxes manager should be
	 *            re-initialized
	 */
	public abstract Rectangle computeBoundingBoxes(
			Collection<Glycan> structures, boolean show_masses,
			boolean show_redend, PositionManager posManager,
			BBoxManager bboxManager, boolean reset);

	/**
	 * Compute the residue bounding boxes for a single structures.
	 * 
	 * @param structure
	 *            the structure to be displayed
	 * @param cur_left
	 *            the left position where to display the structure
	 * @param cur_top
	 *            the top position where to display the structure
	 * @param show_mass
	 *            <code>true</code> if the mass information about the structure
	 *            should be displayed
	 * @param show_redend
	 *            <code>true</code> if the reducing end marker should be
	 *            displayed
	 * @param posManager
	 *            the object used to spatially arrange the residues
	 * @param bboxManager
	 *            the object used to store the residue bounding boxes
	 */
	public abstract Rectangle computeBoundingBoxes(Glycan structure,
			int cur_left, int cur_top, boolean show_mass, boolean show_redend,
			PositionManager posManager, BBoxManager bboxManager);

	/**
	 * Compute the positions of each residue and store them in the position
	 * manager.
	 */

	public abstract void assignPositions(Glycan structure,
			PositionManager posManager);

	/**
	 * Return a string representation of the composition of a glycan structure
	 * 
	 * @param styled
	 *            <code>true</code> if the returned text is displayed in a
	 *            {@link StyledTextCellRenderer}
	 */
	public abstract String makeCompositionText(Glycan g, boolean styled);

	/**
	 * Return a graphical representation of a structure as an image object.
	 * 
	 * @param structure
	 *            the structure to be displayed
	 * @param opaque
	 *            <code>true</code> if a non transparent background should be
	 *            used
	 * @param show_masses
	 *            <code>true</code> if the mass information about the structure
	 *            should be displayed
	 * @param show_redend
	 *            <code>true</code> if the reducing end marker should be
	 *            displayed
	 */
	public abstract BufferedImage getImage(Glycan structure, boolean opaque,
			boolean show_masses, boolean show_redend);

	/**
	 * Return a graphical representation of a structure as an image object.
	 * 
	 * @param structure
	 *            the structure to be displayed
	 * @param opaque
	 *            <code>true</code> if a non transparent background should be
	 *            used
	 * @param show_masses
	 *            <code>true</code> if the mass information about the structure
	 *            should be displayed
	 * @param show_redend
	 *            <code>true</code> if the end marker should be displayed
	 * @param scale
	 *            the scale factor that should be applied to all dimensions
	 */
	public abstract BufferedImage getImage(Glycan structure, boolean opaque,
			boolean show_masses, boolean show_redend, double scale);

	/**
	 * Return a graphical representation of a set of structures as an image
	 * object.
	 * 
	 * @param structures
	 *            the set of structures to be displayed
	 * @param opaque
	 *            <code>true</code> if a non transparent background should be
	 *            used
	 * @param show_masses
	 *            <code>true</code> if the mass information about the structure
	 *            should be displayed
	 * @param show_redend
	 *            <code>true</code> if the end marker should be displayed
	 */
	public abstract BufferedImage getImage(Collection<Glycan> structures,
			boolean opaque, boolean show_masses, boolean show_redend);

	public abstract BufferedImage getImage(Collection<Glycan> structures,
			boolean opaque, boolean show_masses, boolean show_redend,
			double scale);

	/**
	 * Return a graphical representation of a set of structures as an image
	 * object.
	 * 
	 * @param structures
	 *            the set of structures to be displayed
	 * @param opaque
	 *            <code>true</code> if a non transparent background should be
	 *            used
	 * @param show_masses
	 *            <code>true</code> if the mass information about the structure
	 *            should be displayed
	 * @param show_redend
	 *            <code>true</code> if the end marker should be displayed
	 * @param scale
	 *            the scale factor that should be applied to all dimensions
	 */
	public abstract BufferedImage getImage(Collection<Glycan> structures,
			boolean opaque, boolean show_masses, boolean show_redend,
			double scale, PositionManager posManager, BBoxManager bboxManager);
}