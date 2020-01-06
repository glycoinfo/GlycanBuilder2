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
package org.eurocarbdb.application.glycanbuilder.renderutil;

import java.util.*;

import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.*;

import java.awt.*;
import java.awt.image.*;

import org.eurocarbdb.application.glycanbuilder.DefaultPaintable;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.ResiduePlacementDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.linkage.LinkageStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.application.glycanbuilder.util.GlycanUtils;

/**
 * Objects of this class are used to create a graphical representation of a
 * {@link Glycan} object given the current graphic options (
 * {@link GraphicOptions}). The rules to draw the structures in the different
 * notations are stored in the style dictionaries:
 * {@link ResidueStyleDictionary}, {@link LinkageStyleDictionary} and
 * {@link ResiduePlacementDictionary}. The classes {@link ResidueRendererAWT} and
 * {@link LinkageRendererAWT} are used to draw the different parts of the
 * structure. The graphical representation is created in three steps: first the
 * position of each residue around the parent is computed using the rules on
 * residue placements from the {@link ResiduePlacementDictionary} and stored in
 * the {@link PositionManager}; second the bounding box of each residue in the
 * structure is computed from its position and the parent's bounding box, and
 * the values are stored in a {@link BBoxManager}; third the residues are drawn
 * inside their bounding boxes and the linkages are drawn by connecting the
 * centers of the bounding boxes. The output can be directed to a
 * {@link Graphics2D} object or to an image.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class GlycanRendererAWT extends AbstractGlycanRenderer {

	public GlycanRendererAWT() {
		super();
	}
	
	public GlycanRendererAWT(AbstractGlycanRenderer src) {
		super(src);
	}

	public GlycanRendererAWT(GlycanRendererAWT src) {
		super(src);
	}
	
	protected void initialiseRenderers(){
		theResidueRenderer = new ResidueRendererAWT(this);
		theLinkageRenderer = new LinkageRendererAWT(this);
	}

	@Override
	public void paint(Paintable paintable, Glycan structure,
			HashSet<Residue> selected_residues,
			HashSet<Linkage> selected_linkages,
			Collection<Residue> active_residues, boolean show_mass,
			boolean show_redend, PositionManager posManager,
			BBoxManager bboxManager) {

		if (structure == null || structure.isEmpty())
			return;

		boolean a_bIsAlditol = show_redend;
		if(!structure.isComposition())
			a_bIsAlditol = GlycanUtils.isShowRedEnd(structure, theGraphicOptions, show_redend);
		
		this.assignID(structure);
			
		selected_residues = (selected_residues != null) ? selected_residues
				: new HashSet<Residue>();
		selected_linkages = (selected_linkages != null) ? selected_linkages
				: new HashSet<Linkage>();

		if (structure.isComposition()) {
			paintBracket(paintable, structure, selected_residues,
					selected_linkages, active_residues, posManager, bboxManager);
			//paintComposition(paintable, structure.getRoot(), structure.getBracket(),
			//		selected_residues, posManager, bboxManager);
		}else {
			paintResidue(paintable, structure.getRoot(a_bIsAlditol),
					selected_residues, selected_linkages, active_residues,
					posManager, bboxManager);
			paintBracket(paintable, structure, selected_residues,
					selected_linkages, active_residues, posManager, bboxManager);
		}
		if(theGraphicOptions.NOTATION.equals(GraphicOptions.NOTATION_SNFG))
			displayLegend(paintable, structure, show_redend, bboxManager);
		if (show_mass)
			displayMass(paintable, structure, show_redend, bboxManager);
	}

	protected void assignID (Glycan structure) {
		HashMap<String, Integer> a_mIndex = new HashMap<String, Integer>();
		int a_iID = 1;
		for(Residue a_oRES : structure.getAllResidues()) {
			if(!a_oRES.isSaccharide() || a_oRES.getType().getSuperclass().equals("Bridge")) continue;
			if(theGraphicOptions.NOTATION.equals(GraphicOptions.NOTATION_SNFG)) {
				if(!theResidueStyleDictionary.containsResidue(a_oRES)) {
					if(!a_mIndex.containsKey(a_oRES.getType().getDescription())) {
						a_mIndex.put(a_oRES.getType().getDescription(), a_iID);
						a_iID++;
					}
					if(a_mIndex.containsKey(a_oRES.getType().getDescription())) {
						a_oRES.setID(a_mIndex.get(a_oRES.getType().getDescription()));
					}
				}				
			} else {
				if(a_oRES.getID() != 0) a_oRES.setID(0);
			}
		}
		
		return;
	}
	
	protected void displayLegend(Paintable paintable, Glycan structure, boolean show_redend, BBoxManager bboxManager) {
		Graphics2D g2d = paintable.getGraphics2D();
		Rectangle structure_all_bbox = bboxManager.getComplete(structure.getRoot(show_redend));
		
		g2d.setColor(Color.black);
		g2d.setFont(new Font(theGraphicOptions.MASS_TEXT_FONT_FACE, Font.PLAIN, 10));
		
		// create legend of unsupported monosaccharides
		TreeMap<Integer, String> a_mIndex = new TreeMap<Integer, String>();
		int a_iID = 1;
		StringBuilder a_sbLegend = new StringBuilder();
		for(Residue a_oRES : structure.getAllResidues()) {
			if(!a_oRES.isSaccharide() || a_oRES.getType().getSuperclass().equals("Bridge")) continue;
			if(!theResidueStyleDictionary.containsResidue(a_oRES) && !a_mIndex.containsValue(a_oRES.getType().getDescription())) {
				a_mIndex.put(a_iID, a_oRES.getType().getDescription());
				a_iID++;
			}
 		}
		
		for(Integer a_iKey : a_mIndex.keySet()) {
			a_sbLegend.append(a_iKey + "=" + a_mIndex.get(a_iKey) + " \n");
		}
	
		g2d.drawString(a_sbLegend.toString(), 
				Geometry.left(structure_all_bbox), 
				Geometry.bottom(structure_all_bbox) + theGraphicOptions.MASS_TEXT_SPACE/2 + 8);
	}
	
	@Override
	protected void displayMass(Paintable paintable, Glycan structure,
			boolean show_redend, BBoxManager bboxManager) {
		Graphics2D g2d=paintable.getGraphics2D();
		Rectangle structure_all_bbox = bboxManager.getComplete(structure
				.getRoot(show_redend));

		g2d.setColor(Color.black);
		g2d.setFont(new Font(theGraphicOptions.MASS_TEXT_FONT_FACE, Font.PLAIN,
				theGraphicOptions.MASS_TEXT_SIZE));

		String text = getMassText(structure);
		g2d.drawString(text, Geometry.left(structure_all_bbox),
				Geometry.bottom(structure_all_bbox)
						+ theGraphicOptions.MASS_TEXT_SPACE
						+ theGraphicOptions.MASS_TEXT_SIZE);
	}

	@Override
	protected void paintComposition(Paintable paintable, Residue root,
			Residue bracket, HashSet<Residue> selected_residues,
			PositionManager posManager, BBoxManager bboxManager) {
		Graphics2D g2d=paintable.getGraphics2D();
		ResAngle orientation = posManager.getOrientation(root);

		String text = makeCompositionText(root, bracket, orientation, false);
		Rectangle text_rect = bboxManager.getCurrent(bracket);

		// draw selected contour
		if (selected_residues.contains(bracket)) {
			float[] dashes = { 5.f, 5.f };
			g2d.setStroke(new BasicStroke(2.f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_ROUND, 1.f, dashes, 0.f));
			g2d.setColor(Color.black);
			g2d.draw(text_rect);
			g2d.setStroke(new BasicStroke(1));
		}

		Font font = new Font(theGraphicOptions.COMPOSITION_FONT_FACE,
				Font.PLAIN, theGraphicOptions.COMPOSITION_FONT_SIZE);

		StyledTextCellRenderer stcr = new StyledTextCellRenderer(false);
		stcr.getRendererComponent(font, Color.black, Color.white, text);
		BufferedImage img = SVGUtils.getImage(stcr, false);

		if (orientation.equals(0) || orientation.equals(180))
			g2d.drawImage(img, null, text_rect.x, text_rect.y);
		else {
			g2d.rotate(-Math.PI / 2.0);
			g2d.drawImage(img, null, -text_rect.y - text_rect.height,
					text_rect.x);
			g2d.rotate(Math.PI / 2.0);
		}
	}

	@Override
	protected void paintQuantity(Paintable paintable, Residue antenna, int quantity,
			BBoxManager bboxManager) {
		ResAngle orientation = theGraphicOptions.getOrientationAngle();
		Graphics2D g2d=paintable.getGraphics2D();
		// get dimensions
		String text;
		if (orientation.equals(0) || orientation.equals(-90))
			text = "x" + quantity;
		else
			text = quantity + "x";

		Dimension text_dim = textBounds(text, theGraphicOptions.NODE_FONT_FACE,
				theGraphicOptions.NODE_FONT_SIZE);

		// retrieve bounding box
		Rectangle text_rect = null;
		Rectangle antenna_bbox = bboxManager.getComplete(antenna);

		if (orientation.equals(0))
			text_rect = new Rectangle(right(antenna_bbox) + 3,
					midy(antenna_bbox) - 1 - text_dim.height / 2,
					text_dim.width, text_dim.height); // left to right
		else if (orientation.equals(180))
			text_rect = new Rectangle(left(antenna_bbox) - 3 - text_dim.width,
					midy(antenna_bbox) - 1 - text_dim.height / 2,
					text_dim.width, text_dim.height); // right to left
		else if (orientation.equals(90))
			text_rect = new Rectangle(midx(antenna_bbox) - text_dim.height / 2,
					bottom(antenna_bbox) + 3, text_dim.height, text_dim.width); // top
																				// to
																				// bottom
		else
			text_rect = new Rectangle(midx(antenna_bbox) - text_dim.height / 2,
					top(antenna_bbox) - 3 - text_dim.width, text_dim.height,
					text_dim.width); // bottom to top

		// paint text
		g2d.setColor(Color.black);
		g2d.setFont(new Font(theGraphicOptions.NODE_FONT_FACE, Font.PLAIN,
				theGraphicOptions.NODE_FONT_SIZE));

		if (orientation.equals(0) || orientation.equals(180))
			g2d.drawString(text, left(text_rect), bottom(text_rect));
		else {
			g2d.rotate(-Math.PI / 2.0);
			g2d.drawString(text, -bottom(text_rect), right(text_rect));
			// g2d.drawString(text,-(int)(text_rect.y+text_rect.height),(int)(text_rect.x+text_rect.width));
			g2d.rotate(+Math.PI / 2.0);

		}
	}

	@Override
	public BufferedImage getImage(Collection<Glycan> structures,boolean opaque, boolean show_masses, boolean show_redend,double scale,
			PositionManager posManager,BBoxManager bboxManager) {
		if (structures == null)
			structures = new Vector<Glycan>();

		// set scale
		GraphicOptions view_opt = theGraphicOptions;
		boolean old_flag = view_opt.SHOW_INFO;
		view_opt.SHOW_INFO = (old_flag && scale == 1.);
		view_opt.setScale(scale * view_opt.SCALE_CANVAS);

		Rectangle all_bbox = computeBoundingBoxes(structures, show_masses,
				show_redend, posManager, bboxManager);

		// Create an image that supports transparent pixels
		Dimension d = computeSize(all_bbox);
		BufferedImage img = createCompatibleImage(d.width,
				d.height, opaque);

		// prepare graphics context
		Graphics2D g2d = img.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		if (opaque) {
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

			// clear background
			g2d.setBackground(Color.white);
			g2d.clearRect(0, 0, d.width, d.height);
			// g2d.setColor(Color.white);
			// g2d.fillRect(0, 0, d.width, d.height);
		} else {
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			g2d.setBackground(new Color(255, 255, 255, 0));
		}

		// paint structures
		for (Glycan s : structures)
			paint(new DefaultPaintable(g2d), s, null, null, show_masses, show_redend, posManager,
					bboxManager);

		// reset scale
		view_opt.setScale(1.);
		view_opt.SHOW_INFO = old_flag;

		img.flush();

		return img;
	}
	
	  public BufferedImage createCompatibleImage(int width, int height, boolean opaque) {   
	    // retrieve graphic environment

	    // no display
	    if( GraphicsEnvironment.isHeadless() ) {
	      if( opaque )
	        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	      return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	    }

	    // compatible to display
	    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice gs = ge.getDefaultScreenDevice();
	    GraphicsConfiguration gc = gs.getDefaultConfiguration();
	    if( opaque )         
	      return gc.createCompatibleImage(width, height);
	    return gc.createCompatibleImage(width, height, Transparency.BITMASK);
	  }
}
