package org.eurocarbdb.application.glycanbuilder.renderutil;

import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.bottom;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.left;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.midx;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.midy;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.right;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.textBounds;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.top;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.union;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.width;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.BookingManager;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Pair;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResiduePlacement;
import org.eurocarbdb.application.glycanbuilder.ResidueStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.converterGWS.GWSParser;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.ResiduePlacementDictionary;
import org.eurocarbdb.application.glycanbuilder.fileutil.FileConstants;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.linkage.LinkageStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Union;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;

public abstract class AbstractGlycanRenderer implements GlycanRenderer{

	//private static final Log logger = LogFactory.getLog(AbstractGlycanRenderer.class);
	protected ResidueRenderer theResidueRenderer;
	protected LinkageRenderer theLinkageRenderer;

	protected ResiduePlacementDictionary theResiduePlacementDictionary;
	protected ResidueStyleDictionary theResidueStyleDictionary;
	protected LinkageStyleDictionary theLinkageStyleDictionary;

	protected GraphicOptions theGraphicOptions;

	protected GlycanRendererMode theRendererMode=GlycanRendererMode.DRAWING;

	/**
	 * Empty constructor.
	 */
	public AbstractGlycanRenderer() {
		theResiduePlacementDictionary = new ResiduePlacementDictionary();
		theResidueStyleDictionary = new ResidueStyleDictionary();
		theLinkageStyleDictionary = new LinkageStyleDictionary();

		theGraphicOptions = new GraphicOptions();

		theResidueStyleDictionary.loadStyles(FileConstants.RESIDUE_STYLES_FILE_SNFG);
		theResiduePlacementDictionary.loadPlacements(FileConstants.RESIDUE_PLACEMENTS_FILE_SNFG);
		theLinkageStyleDictionary.loadStyles(FileConstants.LINKAGE_STYLES_FILE_SNFG);
		//theResidueStyleDictionary.loadStyles(FileConstants.RESIDUE_STYLES_FILE_CFGLINK);
		//theResiduePlacementDictionary.loadPlacements(FileConstants.RESIDUE_PLACEMENTS_FILE_CFGLINK);
		//theLinkageStyleDictionary.loadStyles(FileConstants.LINKAGE_STYLES_FILE_CFGLINK);

		initialiseRenderers();
	}

	/**
	 * Copy constructor. All dictionaries and options are copied from the
	 * <code>src</code> object.
	 */
	public AbstractGlycanRenderer(AbstractGlycanRenderer src) {
		if (src == null) {
			theResiduePlacementDictionary = new ResiduePlacementDictionary();
			theResidueStyleDictionary = new ResidueStyleDictionary();
			theLinkageStyleDictionary = new LinkageStyleDictionary();

			theGraphicOptions = new GraphicOptions();
		} else {
			theResiduePlacementDictionary = src.theResiduePlacementDictionary;
			theResidueStyleDictionary = src.theResidueStyleDictionary;
			theLinkageStyleDictionary = src.theLinkageStyleDictionary;

			theGraphicOptions = src.theGraphicOptions.clone();
		}

		initialiseRenderers();
	}

	abstract protected void initialiseRenderers();

	@Override
	public void setRenderMode(GlycanRendererMode mode){
		theRendererMode=mode;
	}

	@Override
	public GlycanRendererMode getRenderMode(){
		return theRendererMode;
	}

	// ---

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#getResidueRenderer()
	 */
	@Override
	public ResidueRenderer getResidueRenderer() {
		return theResidueRenderer;
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#setResidueRenderer(org.eurocarbdb.application.glycanbuilder.ResidueRenderer)
	 */
	@Override
	public void setResidueRenderer(ResidueRenderer r) {
		theResidueRenderer = r;
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#getLinkageRenderer()
	 */
	@Override
	public LinkageRenderer getLinkageRenderer() {
		return theLinkageRenderer;
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#setLinkageRenderer(org.eurocarbdb.application.glycanbuilder.LinkageRenderer)
	 */
	@Override
	public void setLinkageRenderer(LinkageRenderer r) {
		theLinkageRenderer = r;
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#getGraphicOptions()
	 */
	@Override
	public GraphicOptions getGraphicOptions() {
		return theGraphicOptions;
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#setGraphicOptions(org.eurocarbdb.application.glycanbuilder.GraphicOptions)
	 */
	@Override
	public void setGraphicOptions(GraphicOptions opt) {
		theGraphicOptions = opt;
		theResidueRenderer.setGraphicOptions(theGraphicOptions);
		theLinkageRenderer.setGraphicOptions(theGraphicOptions);
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#getResiduePlacementDictionary()
	 */
	@Override
	public ResiduePlacementDictionary getResiduePlacementDictionary() {
		return theResiduePlacementDictionary;
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#setResiduePlacementDictionary(org.eurocarbdb.application.glycanbuilder.ResiduePlacementDictionary)
	 */
	@Override
	public void setResiduePlacementDictionary(
			ResiduePlacementDictionary residuePlacementDictionary) {
		theResiduePlacementDictionary = residuePlacementDictionary;
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#getResidueStyleDictionary()
	 */
	@Override
	public ResidueStyleDictionary getResidueStyleDictionary() {
		return theResidueStyleDictionary;
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#setResidueStyleDictionary(org.eurocarbdb.application.glycanbuilder.ResidueStyleDictionary)
	 */
	@Override
	public void setResidueStyleDictionary(
			ResidueStyleDictionary residueStyleDictionary) {
		theResidueStyleDictionary = residueStyleDictionary;
		theResidueRenderer.setResidueStyleDictionary(theResidueStyleDictionary);
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#getLinkageStyleDictionary()
	 */
	@Override
	public LinkageStyleDictionary getLinkageStyleDictionary() {
		return theLinkageStyleDictionary;
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#setLinkageStyleDictionary(org.eurocarbdb.application.glycanbuilder.LinkageStyleDictionary)
	 */
	@Override
	public void setLinkageStyleDictionary(
			LinkageStyleDictionary linkageStyleDictionary) {
		theLinkageStyleDictionary = linkageStyleDictionary;
		theLinkageRenderer.setLinkageStyleDictionary(theLinkageStyleDictionary);
	}

	// -----------------
	// Painting

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#paint(java.awt.Graphics2D, org.eurocarbdb.application.glycanbuilder.Glycan, java.util.HashSet, java.util.HashSet, boolean, boolean, org.eurocarbdb.application.glycanbuilder.PositionManager, org.eurocarbdb.application.glycanbuilder.BBoxManager)
	 */
	@Override
	public void paint(Paintable paintable, Glycan structure,
			HashSet<Residue> selected_residues,
			HashSet<Linkage> selected_linkages, boolean show_mass,
			boolean show_redend, PositionManager posManager,
			BBoxManager bboxManager) {
		paint(paintable, structure, selected_residues, selected_linkages, null,
				show_mass, show_redend, posManager, bboxManager);
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#paint(java.awt.Graphics2D, org.eurocarbdb.application.glycanbuilder.Glycan, java.util.HashSet, java.util.HashSet, java.util.Collection, boolean, boolean, org.eurocarbdb.application.glycanbuilder.PositionManager, org.eurocarbdb.application.glycanbuilder.BBoxManager)
	 */
	@Override
	public void paint(Paintable paintable, Glycan structure,
			HashSet<Residue> selected_residues,
			HashSet<Linkage> selected_linkages,
			Collection<Residue> active_residues, boolean show_mass,
			boolean show_redend, PositionManager posManager,
			BBoxManager bboxManager) {

		if (structure == null || structure.isEmpty())
			return;

		//this.assignID(structure);
		
		selected_residues = (selected_residues != null) ? selected_residues : new HashSet<Residue>();
		selected_linkages = (selected_linkages != null) ? selected_linkages : new HashSet<Linkage>();

		if (structure.isComposition())
			paintBracket(paintable, structure, selected_residues,
					selected_linkages, active_residues, posManager, bboxManager);
			//paintComposition(paintable, structure.getRoot(), structure.getBracket(),
			//	selected_residues, posManager, bboxManager);
		else {
			paintResidue(paintable, structure.getRoot(show_redend),
					selected_residues, selected_linkages, active_residues,
					posManager, bboxManager);
			paintBracket(paintable, structure, selected_residues,
					selected_linkages, active_residues, posManager, bboxManager);
		}
		
		//if(this.theGraphicOptions.NOTATION.equals(GraphicOptions.NOTATION_SNFG))
		//	displayLegend(paintable, structure, show_redend, bboxManager);
		if (show_mass)
			displayMass(paintable, structure, show_redend, bboxManager);
	}

	abstract protected void displayMass(Paintable paintable, Glycan structure,boolean show_redend, BBoxManager bboxManager);

	abstract protected void displayLegend(Paintable paintable, Glycan structure, boolean show_redend, BBoxManager bboxManager);
	
	abstract protected void assignID (Glycan structure);
	
	protected String getMassText(Glycan structure) {
		StringBuilder sb = new StringBuilder();
		DecimalFormat df = new DecimalFormat("0.0000");
		double mz = structure.computeMZ();

		sb.append("m/z: ");
		if (mz < 0.) sb.append("???");
		else sb.append(df.format(mz));
		sb.append(" [");
		sb.append(structure.getMassOptions().toString());
		sb.append("]");

		return sb.toString();
	}

	abstract protected void paintComposition(Paintable paintable, Residue root,Residue bracket, HashSet<Residue> selected_residues,
			PositionManager posManager, BBoxManager bboxManager);

	abstract protected void paintQuantity(Paintable paintable, Residue antennae, int quantity,BBoxManager bboxManager);

	protected void paintResidue(Paintable paintable, Residue node,
			HashSet<Residue> selected_residues,
			HashSet<Linkage> selected_linkages,
			Collection<Residue> active_residues, PositionManager posManager,
			BBoxManager bboxManager) {
		if (node == null) return;

		Rectangle parent_bbox = bboxManager.getParent(node);
		Rectangle node_bbox = bboxManager.getCurrent(node);
		Rectangle border_bbox = bboxManager.getBorder(node);
		Rectangle support_bbox = bboxManager.getSupport(node);

		// not shown 
		if (node_bbox == null) return;

		// paint edges
		for (Linkage link : node.getChildrenLinkages()) {

			Residue child = link.getChildResidue();
			Rectangle child_bbox = bboxManager.getCurrent(child);
			Rectangle child_border_bbox = bboxManager.getBorder(child);

			if (child_bbox != null && !posManager.isOnBorder(child)) {
				boolean selected = (selected_residues.contains(node) && selected_residues
						.contains(child)) || selected_linkages.contains(link);
				boolean active = (active_residues == null || (active_residues
						.contains(node) && active_residues.contains(child)));
				theLinkageRenderer.paintEdge(paintable, link, selected, node_bbox,
						border_bbox, child_bbox, child_border_bbox);
			}
		}

		// paint node
		boolean selected = selected_residues.contains(node);
		boolean active = (active_residues == null || active_residues.contains(node));
		theResidueRenderer.paint(paintable, node, selected, active,
				posManager.isOnBorder(node), parent_bbox, node_bbox,
				support_bbox, posManager.getOrientation(node));

		// paint children
		for (Linkage link : node.getChildrenLinkages())
			paintResidue(paintable, link.getChildResidue(), selected_residues,
					selected_linkages, active_residues, posManager, bboxManager);

		// paint info
		for (Linkage link : node.getChildrenLinkages()) {

			Residue child = link.getChildResidue();
			Rectangle child_bbox = bboxManager.getCurrent(child);
			Rectangle child_border_bbox = bboxManager.getBorder(child);

			if (child_bbox != null && !posManager.isOnBorder(child))
				theLinkageRenderer.paintInfo(paintable, link, node_bbox, border_bbox, child_bbox, child_border_bbox);
		}
	}

	// -----------------
	// Positioning

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#computeSize(java.awt.Rectangle)
	 */
	@Override
	public Dimension computeSize(Rectangle all_bbox) {
		if (all_bbox == null || all_bbox.width == 0 || all_bbox.height == 0)
			return new Dimension(1, 1);
		// return new
		// Dimension(theGraphicOptions.MARGIN_LEFT+theGraphicOptions.MARGIN_RIGHT,theGraphicOptions.MARGIN_TOP+theGraphicOptions.MARGIN_BOTTOM);
		return new Dimension(theGraphicOptions.MARGIN_LEFT + all_bbox.width
				+ theGraphicOptions.MARGIN_RIGHT, theGraphicOptions.MARGIN_TOP
				+ all_bbox.height + theGraphicOptions.MARGIN_BOTTOM);
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#computeBoundingBoxes(java.util.Collection, boolean, boolean, org.eurocarbdb.application.glycanbuilder.PositionManager, org.eurocarbdb.application.glycanbuilder.BBoxManager)
	 */
	@Override
	public Rectangle computeBoundingBoxes(Collection<Glycan> structures,
			boolean show_masses, boolean show_redend,
			PositionManager posManager, BBoxManager bboxManager) {
		return computeBoundingBoxes(structures, show_masses, show_redend,
				posManager, bboxManager, true);
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#computeBoundingBoxes(java.util.Collection, boolean, boolean, org.eurocarbdb.application.glycanbuilder.PositionManager, org.eurocarbdb.application.glycanbuilder.BBoxManager, boolean)
	 */
	@Override
	public Rectangle computeBoundingBoxes(Collection<Glycan> structures,
			boolean show_masses, boolean show_redend,
			PositionManager posManager, BBoxManager bboxManager, boolean reset) {

		if (reset) {
			// init bboxes
			posManager.reset();
			bboxManager.reset();
		}

		// compute bounding boxes;
		Rectangle all_bbox = new Rectangle(theGraphicOptions.MARGIN_TOP,theGraphicOptions.MARGIN_LEFT, 0, 0);
		int cur_top = theGraphicOptions.MARGIN_TOP;
		int cur_left=theGraphicOptions.MARGIN_LEFT;
		for (Iterator<Glycan> i = structures.iterator(); i.hasNext();) {
			// compute glycan bbox
			Rectangle glycan_bbox = computeBoundingBoxes(i.next(), cur_left, cur_top, show_masses,
					show_redend, posManager, bboxManager);

			all_bbox = Geometry.union(all_bbox, glycan_bbox);

			if(theRendererMode==GlycanRendererMode.DRAWING){
				cur_top=Geometry.bottom(all_bbox) + theGraphicOptions.STRUCTURES_SPACE;
			}else if(theRendererMode==GlycanRendererMode.TOOLBAR){
				cur_left=Geometry.right(all_bbox) + theGraphicOptions.STRUCTURES_SPACE;
			}
		}

		return all_bbox;
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#computeBoundingBoxes(org.eurocarbdb.application.glycanbuilder.Glycan, int, int, boolean, boolean, org.eurocarbdb.application.glycanbuilder.PositionManager, org.eurocarbdb.application.glycanbuilder.BBoxManager)
	 */
	@Override
	public Rectangle computeBoundingBoxes(Glycan structure, int cur_left,
			int cur_top, boolean show_mass, boolean show_redend,
			PositionManager posManager, BBoxManager bboxManager) {
		if (structure == null)
			return new Rectangle(cur_left, cur_top, 0, 0);

		try {
			bboxManager.setGraphicOptions(theGraphicOptions);

			if (!structure.isEmpty()) {

				Residue root;// = structure.getRoot();
				Residue bracket;// = structure.getBracket();
				ResAngle orientation = theGraphicOptions.getOrientationAngle();

				/*if (structure.isComposition()) {
					root = structure.getRoot();
					bracket = structure.getBracket();

					// assign positions
					assignPositionComposition(root, posManager);
					assignPositionComposition(bracket, posManager);

					// compute bounding boxes
					computeBoundingBoxesComposition(root, bracket, posManager, bboxManager);
				} else {
					*/root = structure.getRoot(show_redend);
					bracket = structure.getBracket();

					// assign positions
					posManager.add(root, new ResAngle(), orientation, false, true);
					assignPosition(root, false, orientation, root, posManager);

					posManager.add(bracket, new ResAngle(), orientation, false, true);
					assignPosition(bracket, false, orientation, bracket, posManager);

					// compute bounding boxes
					computeBoundingBoxes(root, posManager, bboxManager);
					computeBoundingBoxesBracket(bracket, root, theGraphicOptions.COLLAPSE_MULTIPLE_ANTENNAE,
							posManager, bboxManager);

					// add bracket bbox
					Rectangle bbox = union(bboxManager.getComplete(root), bboxManager.getComplete(bracket));
					bboxManager.setComplete(root, bbox);

					// translate if necessary
					bboxManager.translate(cur_left - bbox.x, cur_top - bbox.y, root);
					bboxManager.translate(cur_left - bbox.x, cur_top - bbox.y, bracket);
					bbox.translate(cur_left - bbox.x, cur_top - bbox.y);

					// add masses
					if (show_mass) {
						Dimension d = textBounds(getMassText(structure),
								theGraphicOptions.MASS_TEXT_FONT_FACE,
								theGraphicOptions.MASS_TEXT_SIZE);
						Rectangle text_bbox = new Rectangle(cur_left, bottom(bbox)
								+ theGraphicOptions.MASS_TEXT_SPACE, d.width,
								d.height);
						bbox = union(bbox, text_bbox);
					}

					return bbox;
				//}
			}
			// cannot properly handle error cases such as multi-threading problems because of this.
		} catch (Exception e) {
			LogUtils.report(e);
		}

		//logger.debug("    return new Rectangle(" + cur_left + "," + cur_top + ", 0, 0);");
		// instead it defaults to a tiny rectangle.

		return new Rectangle(cur_left, cur_top, 0, 0);
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#assignPositions(org.eurocarbdb.application.glycanbuilder.Glycan, org.eurocarbdb.application.glycanbuilder.PositionManager)
	 */

	@Override
	public void assignPositions(Glycan structure, PositionManager posManager) {
		if (structure == null)
			return;

		try {
			ResAngle orientation = theGraphicOptions.getOrientationAngle();
			Residue root = structure.getRoot(true);
			Residue bracket = structure.getBracket();

			posManager.add(root, new ResAngle(), orientation, false, true);
			assignPosition(root, false, orientation, root, posManager);

			posManager.add(bracket, new ResAngle(), orientation, false, true);
			assignPosition(bracket, false, orientation, bracket, posManager);
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	private void assignPositionComposition(Residue current,
			PositionManager posManager) throws Exception {
		if (current == null)
			return;

		posManager.add(current, theGraphicOptions.getOrientationAngle(),
				new ResAngle(), false, false);
		for (Linkage l : current.getChildrenLinkages())
			assignPositionComposition(l.getChildResidue(), posManager);
	}

	private void assignPosition(Residue current, boolean sticky,
			ResAngle orientation, Residue turning_point,
			PositionManager posManager) throws Exception {
		if (current == null)
			return;

		// init positions
		BookingManager bookManager = new BookingManager(posManager.getAvailablePositions(current, orientation));

		// add children to the booking manager
		for (Iterator<Linkage> i = current.iterator(); i.hasNext();) {
			Linkage link = i.next();
			Residue child = link.getChildResidue();
			Residue matching_child = (child.getCleavedResidue() != null) ? child.getCleavedResidue() : child;

			// get placement
			ResiduePlacement placement = matching_child.getPreferredPlacement();
			if (placement == null
					|| (!current.isSaccharide() && !current.isBracket())
					|| !bookManager.isAvailable(placement))
				placement = theResiduePlacementDictionary.getPlacement(current, link, matching_child, sticky);

			// set placement
			bookManager.add(child, placement);
		}

		// place children
		bookManager.place();

		// store positions
		for (Iterator<Linkage> i = current.iterator(); i.hasNext();) {
			Residue child = i.next().getChildResidue();

			ResiduePlacement child_placement = bookManager.getPlacement(child);
			ResAngle child_pos = bookManager.getPosition(child);
			posManager.add(child, orientation, child_pos, child_placement.isOnBorder(), child_placement.isSticky());

			ResAngle child_orientation = posManager.getOrientation(child);
			Residue child_turning_point = (child_orientation.equals(orientation)) ? turning_point : child;
			assignPosition(child, child_placement.isSticky(), child_orientation, child_turning_point, posManager);
		}
	}

	// ----------------
	// Bounding boxe

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#makeCompositionText(org.eurocarbdb.application.glycanbuilder.Glycan, boolean)
	 */
	@Override
	public String makeCompositionText(Glycan g, boolean styled) {
		return makeCompositionText(g, theGraphicOptions.getOrientationAngle(theGraphicOptions.RL), styled);
	}

	/**
	 * Return a string representation of the composition of a glycan structure
	 */
	static public String makeCompositionTextPlain(Glycan g) {
		if (!g.isComposition())
			g = g.getComposition();
		return makeCompositionText(g.getRoot(), g.getBracket(), new ResAngle(0), false);
	}

	/**
	 * Return a string representation of the composition of a glycan structure
	 * 
	 * @param orientation
	 *            the orientation at which the text will be displayed
	 * @param styled
	 *            <code>true</code> if the returned text is displayed in a
	 *            {@link StyledTextCellRenderer}
	 */
	static public String makeCompositionText(Glycan g, ResAngle orientation, boolean styled) {
		if (!g.isComposition())
			g = g.getComposition();
		return makeCompositionText(g.getRoot(), g.getBracket(), orientation, styled);
	}

	protected static String makeCompositionText(Residue root, Residue bracket,
			ResAngle orientation, boolean styled) {

		LinkedList<String> cleavages = new LinkedList<String>();
		TreeMap<String, Integer> residues = new TreeMap<String, Integer>();

		// get components
		for (Linkage l : bracket.getChildrenLinkages()) {
			Residue r = l.getChildResidue();
			if (r.isCleavage()) {
				if (!r.getType().isLCleavage())
					cleavages.add(r.getCleavageType());
			} else {
				String type = r.getResidueName();
				Integer old_num = residues.get(type);
				if (old_num == null)
					residues.put(type, 1);
				else
					residues.put(type, old_num + 1);
			}
		}

		// build name
		StringBuilder text = new StringBuilder();

		// write left/top part
		if (orientation.equals(180) || orientation.equals(90)) {
			if (cleavages.size() > 0) {
				for (String s : cleavages)
					text.append(s);
				text.append('-');
			}
		} else {
			if (!root.getTypeName().equals("freeEnd")) {
				if (root.isCleavage())
					text.append(root.getCleavageType());
				else
					text.append(root.getResidueName());
				text.append('-');
			}
		}

		// write middle
		for (Map.Entry<String, Integer> e : residues.entrySet()) {
			text.append("(" + e.getKey() + ")");
			if (styled) {
				text.append("_{");
				text.append(e.getValue());
				text.append('}');
			} else
				text.append(e.getValue());
		}

		// write right/bottom part
		if (orientation.equals(180) || orientation.equals(90)) {
			if (!root.getTypeName().equals("freeEnd")) {
				text.append('-');
				if (root.isCleavage())
					text.append(root.getCleavageType());
				else
					text.append(root.getResidueName());
			}
		} else {
			if (cleavages.size() > 0) {
				text.append('-');
				for (String s : cleavages)
					text.append(s);
			}
		}

		return text.toString();
	}

	private void computeBoundingBoxesComposition(Residue root, Residue bracket,
			PositionManager posManager, BBoxManager bboxManager) {
		ResAngle orientation = posManager.getOrientation(root);

		String text = makeCompositionText(root, bracket, orientation, true);
		Font font = new Font(theGraphicOptions.COMPOSITION_FONT_FACE, Font.PLAIN, theGraphicOptions.COMPOSITION_FONT_SIZE);

		StyledTextCellRenderer stcr = new StyledTextCellRenderer(false);
		stcr.getRendererComponent(font, Color.black, Color.white, text);

		Dimension d = stcr.getPreferredSize();
		if (orientation.equals(0) || orientation.equals(180))
			bboxManager.setAllBBoxes(bracket, new Rectangle(0, 0, d.width, d.height));
		else
			bboxManager.setAllBBoxes(bracket, new Rectangle(0, 0, d.height, d.width));

		bboxManager.linkSubtree(bracket, root);
		bboxManager.linkSubtree(bracket, bracket);
	}

	private void computeBoundingBoxes(Residue node, PositionManager posManager,
			BBoxManager bboxManager) throws Exception {
		if (node == null) return;
	
		// compute all bboxes
		ResAngle orientation = posManager.getOrientation(node);
		if (orientation.equals(0))
			computeBoundingBoxesLR(node, posManager, bboxManager);
		else if (orientation.equals(180))
			computeBoundingBoxesRL(node, posManager, bboxManager);
		else if (orientation.equals(90))
			computeBoundingBoxesTB(node, posManager, bboxManager);
		else if (orientation.equals(-90))
			computeBoundingBoxesBT(node, posManager, bboxManager);
		else
			throw new Exception("Invalid orientation " + orientation
					+ " at node " + node.id);
	}

	private void computeBoundingBoxesLR(Residue node,
			PositionManager posManager, BBoxManager bboxManager)
					throws Exception {
		if (node == null) return;

		Rectangle node_bbox = theResidueRenderer.computeBoundingBox(node,
				posManager.isOnBorder(node), 0, 0,
				posManager.getOrientation(node), theGraphicOptions.NODE_SIZE,
				Integer.MAX_VALUE);
		bboxManager.setAllBBoxes(node, node_bbox);

		// -----------------
		// place substituents

		LinkedList<Residue> region_m90b = posManager.getChildrenAtPosition(node, new ResAngle(-90), true);
		for (int i = 0; i < region_m90b.size(); i++) {
			computeBoundingBoxes(region_m90b.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignBottomsOnRight(region_m90b.subList(0, i), region_m90b.subList(i, i + 1),
						theGraphicOptions.NODE_SUB_SPACE);
		}

		LinkedList<Residue> region_p90b = posManager.getChildrenAtPosition(node, new ResAngle(90), true);
		for (int i = 0; i < region_p90b.size(); i++) {
			computeBoundingBoxes(region_p90b.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignTopsOnLeft(region_p90b.subList(0, i), region_p90b.subList(i, i + 1),
						theGraphicOptions.NODE_SUB_SPACE);
		}

		// ----------------
		// place positions

		// position -90 (top)
		LinkedList<Residue> region_m90 = posManager.getChildrenAtPosition(node, new ResAngle(-90), false);
		for (int i = 0; i < region_m90.size(); i++) {
			computeBoundingBoxes(region_m90.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignBottomsOnRight(region_m90.subList(0, i), region_m90.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position +90 (bottom)
		LinkedList<Residue> region_p90 = posManager.getChildrenAtPosition(node, new ResAngle(90), false);
		for (int i = 0; i < region_p90.size(); i++) {
			computeBoundingBoxes(region_p90.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignTopsOnLeft(region_p90.subList(0, i), region_p90.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position 0 (right)
		LinkedList<Residue> region_0 = posManager.getChildrenAtPosition(node, new ResAngle(0));
		for (int i = 0; i < region_0.size(); i++) {
			computeBoundingBoxes(region_0.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignLeftsOnBottom(region_0.subList(0, i), region_0.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position -45 (top right)
		LinkedList<Residue> region_m45 = posManager.getChildrenAtPosition(node, new ResAngle(-45));
		for (int i = 0; i < region_m45.size(); i++) {
			computeBoundingBoxes(region_m45.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignLeftsOnBottom(region_m45.subList(0, i), region_m45.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position 45 (bottom right)
		LinkedList<Residue> region_p45 = posManager.getChildrenAtPosition(node, new ResAngle(45));
		for (int i = 0; i < region_p45.size(); i++) {
			computeBoundingBoxes(region_p45.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignLeftsOnBottom(region_p45.subList(0, i), region_p45.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// ----------------
		// align substituents with node
		bboxManager.alignCentersOnTop(node_bbox, region_m90b, theGraphicOptions.NODE_SUB_SPACE);
		bboxManager.alignCentersOnBottom(node_bbox, region_p90b, theGraphicOptions.NODE_SUB_SPACE);

		Union<Residue> border_nodes = new Union<Residue>(region_m90b).and(region_p90b);
		Rectangle border_bbox = bboxManager.getComplete(border_nodes.and(node));

		// align position 0,45,-45 with node to not clash with 90b,-90b
		if (region_0.size() > 0) {
			bboxManager.alignLeftsOnTop(region_0, region_m45, theGraphicOptions.NODE_SPACE);
			bboxManager.alignLeftsOnBottom(region_0, new Union<Residue>(region_0).and(region_m45), region_p45, region_p45,
					theGraphicOptions.NODE_SPACE);
			bboxManager.alignCentersOnRight(node_bbox, border_nodes, region_0, new Union<Residue>(region_0).and(region_m45)
					.and(region_p45), theGraphicOptions.NODE_SPACE);
		} else if (region_m45.size() == 0)
			bboxManager.alignCornersOnRightAtBottom(node_bbox, border_nodes, region_p45, theGraphicOptions.NODE_SPACE,
					theGraphicOptions.NODE_SPACE);
		else if (region_p45.size() == 0)
			bboxManager.alignCornersOnRightAtTop(node_bbox, border_nodes, region_m45, theGraphicOptions.NODE_SPACE,
					theGraphicOptions.NODE_SPACE);
		else
			bboxManager.alignSymmetricOnRight(node_bbox, border_nodes,
					region_m45, region_p45, theGraphicOptions.NODE_SPACE, 2
					* theGraphicOptions.NODE_SPACE
					+ theGraphicOptions.NODE_SIZE);

		// align positions -90 and 90 with node to not clash with the rest
		Union<Residue> ex_nodes = new Union<Residue>(region_0).and(region_m45).and(region_p45).and(border_nodes);
		bboxManager.alignCentersOnTop(node_bbox, ex_nodes, region_m90, region_m90, theGraphicOptions.NODE_SPACE);
		bboxManager.alignCentersOnBottom(node_bbox, ex_nodes, region_p90, region_p90, theGraphicOptions.NODE_SPACE);

		// ----------------
		// set rectangles
		Union<Residue> all_nodes = ex_nodes.and(region_m90).and(region_p90).and(node);

		bboxManager.setCurrent(node, node_bbox);
		bboxManager.setBorder(node, border_bbox);
		bboxManager.setComplete(node, bboxManager.getComplete(all_nodes));
		if (node.hasChildren())
			bboxManager.setSupport(node, new Rectangle(midx(node_bbox)
					+ theGraphicOptions.NODE_SPACE
					+ theGraphicOptions.NODE_SIZE, midy(node_bbox), 0, 0));
		for (Iterator<Linkage> i = node.iterator(); i.hasNext();)
			bboxManager.setParent(i.next().getChildResidue(), node_bbox);
	}

	private void computeBoundingBoxesRL(Residue node,
			PositionManager posManager, BBoxManager bboxManager) throws Exception {
		if (node == null) return;

		Rectangle node_bbox = theResidueRenderer.computeBoundingBox(node,
				posManager.isOnBorder(node), 0, 0,
				posManager.getOrientation(node), theGraphicOptions.NODE_SIZE, Integer.MAX_VALUE);
		bboxManager.setAllBBoxes(node, node_bbox);

		// -----------------
		// place substituents

		// -90 border (bottom)
		LinkedList<Residue> region_m90b = posManager.getChildrenAtPosition(node, new ResAngle(-90), true);
		for (int i = 0; i < region_m90b.size(); i++) {
			computeBoundingBoxes(region_m90b.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignTopsOnLeft(region_m90b.subList(0, i), region_m90b.subList(i, i + 1),
						theGraphicOptions.NODE_SUB_SPACE);
		}

		// +90 border (top)
		LinkedList<Residue> region_p90b = posManager.getChildrenAtPosition(node, new ResAngle(90), true);
		for (int i = 0; i < region_p90b.size(); i++) {
			computeBoundingBoxes(region_p90b.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignBottomsOnRight(region_p90b.subList(0, i), region_p90b.subList(i, i + 1),
						theGraphicOptions.NODE_SUB_SPACE);
		}

		// ----------------
		// place positions

		// position -90 (bottom)
		LinkedList<Residue> region_m90 = posManager.getChildrenAtPosition(node, new ResAngle(-90), false);
		for (int i = 0; i < region_m90.size(); i++) {
			computeBoundingBoxes(region_m90.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignTopsOnLeft(region_m90.subList(0, i), region_m90.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position +90 (top)
		LinkedList<Residue> region_p90 = posManager.getChildrenAtPosition(node, new ResAngle(90), false);
		for (int i = 0; i < region_p90.size(); i++) {
			computeBoundingBoxes(region_p90.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignBottomsOnRight(region_p90.subList(0, i), region_p90.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position 0 (left)
		LinkedList<Residue> region_0 = posManager.getChildrenAtPosition(node, new ResAngle(0), false);
		for (int i = 0; i < region_0.size(); i++) {
			computeBoundingBoxes(region_0.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignRightsOnTop(region_0.subList(0, i), region_0.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position -45 (bottom left)
		LinkedList<Residue> region_m45 = posManager.getChildrenAtPosition(node, new ResAngle(-45), false);
		for (int i = 0; i < region_m45.size(); i++) {
			computeBoundingBoxes(region_m45.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignRightsOnTop(region_m45.subList(0, i), region_m45.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position 45 (top left)
		LinkedList<Residue> region_p45 = posManager.getChildrenAtPosition(node, new ResAngle(45), false);
		for (int i = 0; i < region_p45.size(); i++) {
			computeBoundingBoxes(region_p45.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignRightsOnTop(region_p45.subList(0, i), region_p45.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// ----------------
		// align substituents with node
		bboxManager.alignCentersOnBottom(node_bbox, region_m90b, theGraphicOptions.NODE_SUB_SPACE);
		bboxManager.alignCentersOnTop(node_bbox, region_p90b, theGraphicOptions.NODE_SUB_SPACE);

		Union<Residue> border_nodes = new Union<Residue>(region_m90b).and(region_p90b);
		Rectangle border_bbox = bboxManager.getComplete(border_nodes.and(node));

		// align position 0,45,-45 with node to not clash with 90b,-90b
		if (region_0.size() > 0) {
			bboxManager.alignRightsOnBottom(region_0, region_m45, theGraphicOptions.NODE_SPACE);
			bboxManager.alignRightsOnTop(region_0,
					new Union<Residue>(region_0).and(region_m45), region_p45,
					region_p45, theGraphicOptions.NODE_SPACE);
			bboxManager.alignCentersOnLeft(node_bbox, border_nodes, region_0,
					new Union<Residue>(region_0).and(region_m45)
					.and(region_p45), theGraphicOptions.NODE_SPACE);
		} else if (region_m45.size() == 0)
			bboxManager.alignCornersOnLeftAtTop(node_bbox, border_nodes,
					region_p45, theGraphicOptions.NODE_SPACE,
					theGraphicOptions.NODE_SPACE);
		else if (region_p45.size() == 0)
			bboxManager.alignCornersOnLeftAtBottom(node_bbox, border_nodes,
					region_m45, theGraphicOptions.NODE_SPACE,
					theGraphicOptions.NODE_SPACE);
		else
			bboxManager.alignSymmetricOnLeft(node_bbox, border_nodes,
					region_p45, region_m45, theGraphicOptions.NODE_SPACE, 2
					* theGraphicOptions.NODE_SPACE
					+ theGraphicOptions.NODE_SIZE);

		// align positions -90 and 90 with node to not clash with the rest
		Union<Residue> ex_nodes = new Union<Residue>(region_0).and(region_m45).and(region_p45).and(border_nodes);
		bboxManager.alignCentersOnBottom(node_bbox, ex_nodes, region_m90, region_m90, theGraphicOptions.NODE_SPACE);
		bboxManager.alignCentersOnTop(node_bbox, ex_nodes, region_p90, region_p90, theGraphicOptions.NODE_SPACE);

		// ----------------
		// set rectangles
		Union<Residue> all_nodes = ex_nodes.and(region_m90).and(region_p90).and(node);

		bboxManager.setCurrent(node, node_bbox);
		bboxManager.setBorder(node, border_bbox);
		bboxManager.setComplete(node, bboxManager.getComplete(all_nodes));
		if (node.hasChildren())
			bboxManager.setSupport(node, new Rectangle(midx(node_bbox)
					- theGraphicOptions.NODE_SPACE
					- theGraphicOptions.NODE_SIZE, midy(node_bbox), 0, 0));
		for (Iterator<Linkage> i = node.iterator(); i.hasNext();)
			bboxManager.setParent(i.next().getChildResidue(), node_bbox);
	}

	private void computeBoundingBoxesTB(Residue node,
			PositionManager posManager, BBoxManager bboxManager) throws Exception {
		if (node == null)
			return;

		Rectangle node_bbox = theResidueRenderer.computeBoundingBox(node,
				posManager.isOnBorder(node), 0, 0,
				posManager.getOrientation(node), theGraphicOptions.NODE_SIZE, Integer.MAX_VALUE);
		bboxManager.setAllBBoxes(node, node_bbox);

		// -----------------
		// place substituents

		// -90 border (right)
		LinkedList<Residue> region_m90b = posManager.getChildrenAtPosition(node, new ResAngle(-90), true);
		for (int i = 0; i < region_m90b.size(); i++) {
			computeBoundingBoxes(region_m90b.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignLeftsOnBottom(region_m90b.subList(0, i), region_m90b.subList(i, i + 1),
						theGraphicOptions.NODE_SUB_SPACE);
		}

		// +90 border (left)
		LinkedList<Residue> region_p90b = posManager.getChildrenAtPosition(node, new ResAngle(90), true);
		for (int i = 0; i < region_p90b.size(); i++) {
			computeBoundingBoxes(region_p90b.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignRightsOnTop(region_p90b.subList(0, i),
						region_p90b.subList(i, i + 1),
						theGraphicOptions.NODE_SUB_SPACE);
		}
		// ----------------
		// place positions

		// position -90 (right)
		LinkedList<Residue> region_m90 = posManager.getChildrenAtPosition(node, new ResAngle(-90), false);
		for (int i = 0; i < region_m90.size(); i++) {
			computeBoundingBoxes(region_m90.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignLeftsOnBottom(region_m90.subList(0, i), region_m90.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position +90 (left)
		LinkedList<Residue> region_p90 = posManager.getChildrenAtPosition(node, new ResAngle(90), false);
		for (int i = 0; i < region_p90.size(); i++) {
			computeBoundingBoxes(region_p90.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignRightsOnTop(region_p90.subList(0, i), region_p90.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position 0 (bottom)
		LinkedList<Residue> region_0 = posManager.getChildrenAtPosition(node, new ResAngle(0));
		for (int i = 0; i < region_0.size(); i++) {
			computeBoundingBoxes(region_0.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignTopsOnLeft(region_0.subList(0, i), region_0.subList(i, i + 1), theGraphicOptions.NODE_SPACE);
		}

		// position -45 (bottom right)
		LinkedList<Residue> region_m45 = posManager.getChildrenAtPosition(node, new ResAngle(-45));
		for (int i = 0; i < region_m45.size(); i++) {
			computeBoundingBoxes(region_m45.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignTopsOnLeft(region_m45.subList(0, i), region_m45.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position 45 (bottom left)
		LinkedList<Residue> region_p45 = posManager.getChildrenAtPosition(node, new ResAngle(45));
		for (int i = 0; i < region_p45.size(); i++) {
			computeBoundingBoxes(region_p45.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignTopsOnLeft(region_p45.subList(i, i + 1),
						region_p45.subList(0, i), theGraphicOptions.NODE_SPACE);
		}

		// ----------------
		// align substituents with node
		bboxManager.alignCentersOnRight(node_bbox, region_m90b, theGraphicOptions.NODE_SUB_SPACE);
		bboxManager.alignCentersOnLeft(node_bbox, region_p90b, theGraphicOptions.NODE_SUB_SPACE);

		Union<Residue> border_nodes = new Union<Residue>(region_m90b).and(region_p90b);
		Rectangle border_bbox = bboxManager.getComplete(border_nodes.and(node));

		// align position 0,45,-45 with node to not clash with 90b,-90b
		if (region_0.size() > 0) {
			bboxManager.alignTopsOnRight(region_0, region_m45, theGraphicOptions.NODE_SPACE);
			bboxManager.alignTopsOnLeft(region_0,
					new Union<Residue>(region_0).and(region_m45), region_p45,
					region_p45, theGraphicOptions.NODE_SPACE);
			bboxManager.alignCentersOnBottom(node_bbox, border_nodes, region_0,
					new Union<Residue>(region_0).and(region_m45).and(region_p45), theGraphicOptions.NODE_SPACE);
		} else if (region_m45.size() == 0)
			bboxManager.alignCornersOnBottomAtLeft(node_bbox, border_nodes,
					region_p45, theGraphicOptions.NODE_SPACE,
					theGraphicOptions.NODE_SPACE);
		else if (region_p45.size() == 0)
			bboxManager.alignCornersOnBottomAtRight(node_bbox, border_nodes,
					region_m45, theGraphicOptions.NODE_SPACE,
					theGraphicOptions.NODE_SPACE);
		else
			bboxManager.alignSymmetricOnBottom(node_bbox, border_nodes,
					region_p45, region_m45, 2 * theGraphicOptions.NODE_SPACE, 2
					* theGraphicOptions.NODE_SPACE
					+ theGraphicOptions.NODE_SIZE);

		// align positions -90 and 90 with node to not clash with the rest
		Union<Residue> ex_nodes = new Union<Residue>(region_0).and(region_m45).and(region_p45).and(border_nodes);
		bboxManager.alignCentersOnRight(node_bbox, ex_nodes, region_m90, region_m90, theGraphicOptions.NODE_SPACE);
		bboxManager.alignCentersOnLeft(node_bbox, ex_nodes, region_p90, region_p90, theGraphicOptions.NODE_SPACE);

		// ----------------
		// set rectangles
		Union<Residue> all_nodes = ex_nodes.and(region_m90).and(region_p90).and(node);

		bboxManager.setCurrent(node, node_bbox);
		bboxManager.setBorder(node, border_bbox);
		bboxManager.setComplete(node, bboxManager.getComplete(all_nodes));
		if (node.hasChildren())
			bboxManager.setSupport(node, new Rectangle(midx(node_bbox), midy(node_bbox) + theGraphicOptions.NODE_SPACE
					+ theGraphicOptions.NODE_SIZE, 0, 0));
		for (Iterator<Linkage> i = node.iterator(); i.hasNext();)
			bboxManager.setParent(i.next().getChildResidue(), node_bbox);
	}

	private void computeBoundingBoxesBT(Residue node,
			PositionManager posManager, BBoxManager bboxManager) throws Exception {
		if (node == null)
			return;

		Rectangle node_bbox = theResidueRenderer.computeBoundingBox(node,
				posManager.isOnBorder(node), 0, 0,
				posManager.getOrientation(node), theGraphicOptions.NODE_SIZE, Integer.MAX_VALUE);
		bboxManager.setAllBBoxes(node, node_bbox);

		// -----------------
		// place substituents

		// -90 border (left)
		LinkedList<Residue> region_m90b = posManager.getChildrenAtPosition(node, new ResAngle(-90), true);
		for (int i = 0; i < region_m90b.size(); i++) {
			computeBoundingBoxes(region_m90b.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignRightsOnTop(region_m90b.subList(0, i), region_m90b.subList(i, i + 1),
						theGraphicOptions.NODE_SUB_SPACE);
		}

		// +90 border (right)
		LinkedList<Residue> region_p90b = posManager.getChildrenAtPosition(node, new ResAngle(90), true);
		for (int i = 0; i < region_p90b.size(); i++) {
			computeBoundingBoxes(region_p90b.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignLeftsOnBottom(region_p90b.subList(0, i), region_p90b.subList(i, i + 1),
						theGraphicOptions.NODE_SUB_SPACE);
		}
		// ----------------
		// place positions

		// position -90 (left)
		LinkedList<Residue> region_m90 = posManager.getChildrenAtPosition(node, new ResAngle(-90), false);
		for (int i = 0; i < region_m90.size(); i++) {
			computeBoundingBoxes(region_m90.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignRightsOnTop(region_m90.subList(0, i), region_m90.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position +90 (right)
		LinkedList<Residue> region_p90 = posManager.getChildrenAtPosition(node, new ResAngle(90), false);
		for (int i = 0; i < region_p90.size(); i++) {
			computeBoundingBoxes(region_p90.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignLeftsOnBottom(region_p90.subList(0, i), region_p90.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position 0 (top)
		LinkedList<Residue> region_0 = posManager.getChildrenAtPosition(node, new ResAngle(0));
		for (int i = 0; i < region_0.size(); i++) {
			computeBoundingBoxes(region_0.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignBottomsOnRight(region_0.subList(0, i), region_0.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position -45 (top left)
		LinkedList<Residue> region_m45 = posManager.getChildrenAtPosition(node, new ResAngle(-45));
		for (int i = 0; i < region_m45.size(); i++) {
			computeBoundingBoxes(region_m45.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignBottomsOnRight(region_m45.subList(0, i), region_m45.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// position 45 (top right)
		LinkedList<Residue> region_p45 = posManager.getChildrenAtPosition(node, new ResAngle(45));
		for (int i = 0; i < region_p45.size(); i++) {
			computeBoundingBoxes(region_p45.get(i), posManager, bboxManager);
			if (i > 0)
				bboxManager.alignBottomsOnRight(region_p45.subList(0, i), region_p45.subList(i, i + 1),
						theGraphicOptions.NODE_SPACE);
		}

		// ----------------
		// align substituents with node
		bboxManager.alignCentersOnLeft(node_bbox, region_m90b, theGraphicOptions.NODE_SUB_SPACE);
		bboxManager.alignCentersOnRight(node_bbox, region_p90b, theGraphicOptions.NODE_SUB_SPACE);

		Union<Residue> border_nodes = new Union<Residue>(region_m90b).and(region_p90b);
		Rectangle border_bbox = bboxManager.getComplete(border_nodes.and(node));

		// align position 0,45,-45 with node to not clash with 90b,-90b
		if (region_0.size() > 0) {
			bboxManager.alignBottomsOnLeft(region_0, region_m45, theGraphicOptions.NODE_SPACE);
			bboxManager.alignBottomsOnRight(region_0, new Union<Residue>(
					region_0).and(region_m45), region_p45, region_p45,
					theGraphicOptions.NODE_SPACE);
			bboxManager.alignCentersOnTop(node_bbox, border_nodes, region_0,
					new Union<Residue>(region_0).and(region_m45)
					.and(region_p45), theGraphicOptions.NODE_SPACE);
		} else if (region_m45.size() == 0)
			bboxManager.alignCornersOnTopAtRight(node_bbox, border_nodes,
					region_p45, theGraphicOptions.NODE_SPACE,
					theGraphicOptions.NODE_SPACE);
		else if (region_p45.size() == 0)
			bboxManager.alignCornersOnTopAtLeft(node_bbox, border_nodes,
					region_m45, theGraphicOptions.NODE_SPACE,
					theGraphicOptions.NODE_SPACE);
		else
			bboxManager.alignSymmetricOnTop(node_bbox, border_nodes,
					region_m45, region_p45, theGraphicOptions.NODE_SPACE, 2
					* theGraphicOptions.NODE_SPACE
					+ theGraphicOptions.NODE_SIZE);

		// align positions -90 and 90 with node to not clash with the rest
		Union<Residue> ex_nodes = new Union<Residue>(region_0).and(region_m45).and(region_p45).and(border_nodes);
		bboxManager.alignCentersOnLeft(node_bbox, ex_nodes, region_m90, region_m90, theGraphicOptions.NODE_SPACE);
		bboxManager.alignCentersOnRight(node_bbox, ex_nodes, region_p90, region_p90, theGraphicOptions.NODE_SPACE);

		// ----------------
		// set rectangles
		Union<Residue> all_nodes = ex_nodes.and(region_m90).and(region_p90).and(node);

		bboxManager.setCurrent(node, node_bbox);
		bboxManager.setBorder(node, border_bbox);
		bboxManager.setComplete(node, bboxManager.getComplete(all_nodes));
		if (node.hasChildren())
			bboxManager.setSupport(node, new Rectangle(midx(node_bbox),
					midy(node_bbox) - theGraphicOptions.NODE_SPACE
					- theGraphicOptions.NODE_SIZE, 0, 0));
		for (Iterator<Linkage> i = node.iterator(); i.hasNext();)
			bboxManager.setParent(i.next().getChildResidue(), node_bbox);
	}

	// ----------------
	// Bounding boxes bracket

	private void computeBoundingBoxesBracket(Residue bracket, Residue root,
			boolean COLLAPSE_MULTIPLE_ANTENNAE, PositionManager posManager,
			BBoxManager bboxManager) throws Exception {
		if (bracket == null || root == null)
			return;

		// compute all bboxes
		ResAngle orientation = posManager.getOrientation(bracket);

		if (orientation.equals(0))
			computeBoundingBoxesBracketLR(bracket, root,
					COLLAPSE_MULTIPLE_ANTENNAE, posManager, bboxManager);
		else if (orientation.equals(180))
			computeBoundingBoxesBracketRL(bracket, root,
					COLLAPSE_MULTIPLE_ANTENNAE, posManager, bboxManager);
		else if (orientation.equals(90))
			computeBoundingBoxesBracketTB(bracket, root,
					COLLAPSE_MULTIPLE_ANTENNAE, posManager, bboxManager);
		else if (orientation.equals(-90))
			computeBoundingBoxesBracketBT(bracket, root,
					COLLAPSE_MULTIPLE_ANTENNAE, posManager, bboxManager);
		else
			throw new Exception("Invalid orientation " + orientation);
	}

	private void computeBoundingBoxesBracketLR(Residue bracket, Residue root,
			boolean COLLAPSE_MULTIPLE_ANTENNAE, PositionManager posManager,
			BBoxManager bboxManager) throws Exception {
		if (bracket == null || root == null)
			return;

		ResAngle orientation = posManager.getOrientation(bracket);

		// compute children bounding boxes
		int id = 0;
		int max_quantity = 1;
		LinkedList<Residue> antennaee = new LinkedList<Residue>();
		TreeMap<String, Pair<Residue, Integer>> unique_antennae = new TreeMap<String, Pair<Residue, Integer>>();
		for (int i = 0; i < bracket.getNoChildren(); i++) {
			Residue child = bracket.getChildAt(i); // avoid concurrent
			// modification of
			// iterator!!
			String child_str = (COLLAPSE_MULTIPLE_ANTENNAE) ? GWSParser.writeSubtree(child, false) : ("" + (id++));

			//append by e15d5605, 20191224
			if (COLLAPSE_MULTIPLE_ANTENNAE) {
				child_str = child.getParentLinkage().getParentPositionsString() + child_str;
			}

			Pair<Residue, Integer> value = unique_antennae.get(child_str);
			if (value == null) {
				Residue antennae = child;

				// create fake attachment point for non-border residues
				if (!posManager.isOnBorder(antennae)) {
					antennae = ResidueDictionary.newResidue("#attach");
					child.insertParent(antennae);
					posManager.add(antennae, orientation, new ResAngle(), false, true);
				}

				// set child bbox
				computeBoundingBoxesLR(antennae, posManager, bboxManager);
				if (antennaee.size() > 0)
					bboxManager.alignLeftsOnBottom(antennaee.getLast(), antennae, theGraphicOptions.NODE_SPACE);

				// add antennae to the list
				antennaee.add(antennae);
				unique_antennae.put(child_str, new Pair<Residue, Integer>(child, 1));
			} else {
				// link to other antennae
				bboxManager.linkSubtrees(value.getFirst(), child);

				// update quantity for repeated antennae
				int new_quantity = value.getSecond() + 1;
				unique_antennae.put(child_str, new Pair<Residue, Integer>(value.getFirst(), new_quantity));
				max_quantity = Math.max(max_quantity, new_quantity);
			}
		}

		// compute bracket bbox
		Rectangle structure_bbox = new Rectangle(bboxManager.getComplete(root));
		Rectangle bracket_bbox = new Rectangle(right(structure_bbox), top(structure_bbox), theGraphicOptions.NODE_SIZE,
				structure_bbox.height);

		// align antennaee
		if (antennaee.size() > 0)
			bboxManager.alignCentersOnRight(bracket_bbox, antennaee, 0);
		Rectangle antennaee_bbox = (antennaee.size() > 0) ? new Rectangle(
				bboxManager.getComplete(antennaee)) : null;
				Rectangle all_bbox = union(bracket_bbox, antennaee_bbox);

				// compute bbox for quantities
				if (max_quantity > 1) {
					Dimension quantity_text_dim = textBounds(max_quantity + "x",
							theGraphicOptions.NODE_FONT_FACE,
							theGraphicOptions.NODE_FONT_SIZE);
					all_bbox.width += quantity_text_dim.width + 2;
				}

				// restore linkages
				for (Residue antennae : antennaee) {
					if (!posManager.isOnBorder(antennae))
						bracket.removeChild(antennae);
				}

				// set bboxes
				bboxManager.setParent(bracket, structure_bbox);
				bboxManager.setCurrent(bracket, bracket_bbox);
				bboxManager.setBorder(bracket, bracket_bbox);
				bboxManager.setComplete(bracket, all_bbox);
				bboxManager.setSupport(bracket, bracket_bbox);
	}

	private void computeBoundingBoxesBracketRL(Residue bracket, Residue root,
			boolean COLLAPSE_MULTIPLE_ANTENNAE, PositionManager posManager,
			BBoxManager bboxManager) throws Exception {
		if (bracket == null || root == null)
			return;

		ResAngle orientation = posManager.getOrientation(bracket);

		// compute children bounding boxes
		int id = 0;
		int max_quantity = 1;
		LinkedList<Residue> antennaee = new LinkedList<Residue>();
		TreeMap<String, Pair<Residue, Integer>> unique_antennae = new TreeMap<String, Pair<Residue, Integer>>();
		for (int i = 0; i < bracket.getNoChildren(); i++) {
			Residue child = bracket.getChildAt(i); // avoid concurrent
			// modification of
			// iterator!!
			String child_str = (COLLAPSE_MULTIPLE_ANTENNAE) ? GWSParser.writeSubtree(child, false) : ("" + (id++));

			//append by e15d5605, 20191224
			if (COLLAPSE_MULTIPLE_ANTENNAE) {
				child_str = child.getParentLinkage().getParentPositionsString() + child_str;
			}

			Pair<Residue, Integer> value = unique_antennae.get(child_str);
			if (value == null) {
				Residue antennae = child;

				// create fake attachment point for non-border residues
				if (!posManager.isOnBorder(antennae)) {
					antennae = ResidueDictionary.newResidue("#attach");
					child.insertParent(antennae);
					posManager.add(antennae, orientation, new ResAngle(), false, true);
				}

				// set child bbox
				computeBoundingBoxesRL(antennae, posManager, bboxManager);
				if (antennaee.size() > 0)
					bboxManager.alignRightsOnTop(antennaee.getLast(), antennae, theGraphicOptions.NODE_SPACE);

				// add antennae to the list
				antennaee.add(antennae);
				unique_antennae.put(child_str, new Pair<Residue, Integer>(child, 1));
			} else {
				// link to other antennae
				bboxManager.linkSubtrees(value.getFirst(), child);

				// update quantity for repeated antennae
				int new_quantity = value.getSecond() + 1;
				unique_antennae.put(child_str, new Pair<Residue, Integer>(value.getFirst(), new_quantity));
				max_quantity = Math.max(max_quantity, new_quantity);
			}
		}

		// compute bracket bbox
		Rectangle structure_bbox = new Rectangle(bboxManager.getComplete(root));
		Rectangle bracket_bbox = new Rectangle(left(structure_bbox) - theGraphicOptions.NODE_SIZE, top(structure_bbox),
				theGraphicOptions.NODE_SIZE, structure_bbox.height);

		// align antennaee
		if (antennaee.size() > 0)
			bboxManager.alignCentersOnLeft(bracket_bbox, antennaee, 0);
		Rectangle antennaee_bbox = (antennaee.size() > 0) ? new Rectangle(
				bboxManager.getComplete(antennaee)) : null;
				Rectangle all_bbox = union(bracket_bbox, antennaee_bbox);

				// compute bbox for quantities
				if (max_quantity > 1) {
					Dimension quantity_text_dim = textBounds(max_quantity + "x",
							theGraphicOptions.NODE_FONT_FACE,
							theGraphicOptions.NODE_FONT_SIZE);
					all_bbox.x -= quantity_text_dim.width + 2;
					all_bbox.width += quantity_text_dim.width + 2;
				}

				// restore linkages
				for (Residue antennae : antennaee) {
					if (!posManager.isOnBorder(antennae))
						bracket.removeChild(antennae);
				}

				// set bboxes
				bboxManager.setParent(bracket, structure_bbox);
				bboxManager.setCurrent(bracket, bracket_bbox);
				bboxManager.setBorder(bracket, bracket_bbox);
				bboxManager.setComplete(bracket, all_bbox);
				bboxManager.setSupport(bracket, bracket_bbox);
	}

	private void computeBoundingBoxesBracketTB(Residue bracket, Residue root,
			boolean COLLAPSE_MULTIPLE_ANTENNAE, PositionManager posManager,
			BBoxManager bboxManager) throws Exception {
		if (bracket == null || root == null)
			return;

		ResAngle orientation = posManager.getOrientation(bracket);

		// compute children bounding boxes
		int id = 0;
		int max_quantity = 1;
		LinkedList<Residue> antennaee = new LinkedList<Residue>();
		TreeMap<String, Pair<Residue, Integer>> unique_antennae = new TreeMap<String, Pair<Residue, Integer>>();
		for (int i = 0; i < bracket.getNoChildren(); i++) {
			Residue child = bracket.getChildAt(i); // avoid concurrent
			// modification of
			// iterator!!
			String child_str = (COLLAPSE_MULTIPLE_ANTENNAE) ? GWSParser.writeSubtree(child, false) : ("" + (id++));

			//append by e15d5605, 20191224
			if (COLLAPSE_MULTIPLE_ANTENNAE) {
				child_str = child.getParentLinkage().getParentPositionsString() + child_str;
			}

			Pair<Residue, Integer> value = unique_antennae.get(child_str);
			if (value == null) {
				Residue antennae = child;

				// create fake attachment point for non-border residues
				if (!posManager.isOnBorder(antennae)) {
					antennae = ResidueDictionary.newResidue("#attach");
					child.insertParent(antennae);
					posManager.add(antennae, orientation, new ResAngle(), false, true);
				}

				// set child bbox
				computeBoundingBoxesTB(antennae, posManager, bboxManager);
				if (antennaee.size() > 0)
					bboxManager.alignBottomsOnLeft(antennaee.getLast(), antennae, theGraphicOptions.NODE_SPACE);

				// add antennae to the list
				antennaee.add(antennae);
				unique_antennae.put(child_str, new Pair<Residue, Integer>(child, 1));
			} else {
				// link to other antennae
				bboxManager.linkSubtrees(value.getFirst(), child);

				// update quantity for repeated antennae
				int new_quantity = value.getSecond() + 1;
				unique_antennae.put(child_str, new Pair<Residue, Integer>(value.getFirst(), new_quantity));
				max_quantity = Math.max(max_quantity, new_quantity);
			}
		}

		// compute bracket bbox
		Rectangle structure_bbox = new Rectangle(bboxManager.getComplete(root));
		Rectangle bracket_bbox = new Rectangle(left(structure_bbox),
				bottom(structure_bbox), width(structure_bbox),
				theGraphicOptions.NODE_SIZE);

		// align antennaee
		if (antennaee.size() > 0)
			bboxManager.alignCentersOnBottom(bracket_bbox, antennaee, 0);
		Rectangle antennaee_bbox = (antennaee.size() > 0) ? new Rectangle(
				bboxManager.getComplete(antennaee)) : null;
				Rectangle all_bbox = union(bracket_bbox, antennaee_bbox);

				// compute bbox for quantities
				if (max_quantity > 1) {
					Dimension quantity_text_dim = textBounds(max_quantity + "x",
							theGraphicOptions.NODE_FONT_FACE,
							theGraphicOptions.NODE_FONT_SIZE);
					all_bbox.height += quantity_text_dim.width + 2; // the string is
					// rotated
				}

				// restore linkages
				for (Residue antennae : antennaee) {
					if (!posManager.isOnBorder(antennae))
						bracket.removeChild(antennae);
				}

				// set bboxes
				bboxManager.setParent(bracket, structure_bbox);
				bboxManager.setCurrent(bracket, bracket_bbox);
				bboxManager.setBorder(bracket, bracket_bbox);
				bboxManager.setComplete(bracket, all_bbox);
				bboxManager.setSupport(bracket, bracket_bbox);
	}

	private void computeBoundingBoxesBracketBT(Residue bracket, Residue root,
			boolean COLLAPSE_MULTIPLE_ANTENNAE, PositionManager posManager,
			BBoxManager bboxManager) throws Exception {
		if (bracket == null || root == null)
			return;

		ResAngle orientation = posManager.getOrientation(bracket);

		// compute children bounding boxes
		int id = 0;
		int max_quantity = 1;
		LinkedList<Residue> antennaee = new LinkedList<Residue>();
		TreeMap<String, Pair<Residue, Integer>> unique_antennae = new TreeMap<String, Pair<Residue, Integer>>();
		for (int i = 0; i < bracket.getNoChildren(); i++) {
			Residue child = bracket.getChildAt(i); // avoid concurrent
			// modification of
			// iterator!!
			String child_str = (COLLAPSE_MULTIPLE_ANTENNAE) ? GWSParser.writeSubtree(child, false) : ("" + (id++));

			//append by e15d5605, 20191224
			if (COLLAPSE_MULTIPLE_ANTENNAE) {
				child_str = child.getParentLinkage().getParentPositionsString() + child_str;
			}

			Pair<Residue, Integer> value = unique_antennae.get(child_str);
			if (value == null) {
				Residue antennae = child;

				// create fake attachment point for non-border residues
				if (!posManager.isOnBorder(antennae)) {
					antennae = ResidueDictionary.newResidue("#attach");
					child.insertParent(antennae);
					posManager.add(antennae, orientation, new ResAngle(), false, true);
				}

				// set child bbox
				computeBoundingBoxesBT(antennae, posManager, bboxManager);
				if (antennaee.size() > 0)
					bboxManager.alignTopsOnRight(antennaee.getLast(), antennae, theGraphicOptions.NODE_SPACE);

				// add antennae to the list
				antennaee.add(antennae);
				unique_antennae.put(child_str, new Pair<Residue, Integer>(child, 1));
			} else {
				// link to other antennae
				bboxManager.linkSubtrees(value.getFirst(), child);

				// update quantity for repeated antennae
				int new_quantity = value.getSecond() + 1;
				unique_antennae.put(child_str, new Pair<Residue, Integer>(value.getFirst(), new_quantity));
				max_quantity = Math.max(max_quantity, new_quantity);
			}
		}

		// compute bracket bbox
		Rectangle structure_bbox = new Rectangle(bboxManager.getComplete(root));
		Rectangle bracket_bbox = new Rectangle(left(structure_bbox),
				top(structure_bbox) - theGraphicOptions.NODE_SIZE,
				width(structure_bbox), theGraphicOptions.NODE_SIZE);
		// align antennaee
		if (antennaee.size() > 0)
			bboxManager.alignCentersOnTop(bracket_bbox, antennaee, 0);
		Rectangle antennaee_bbox = (antennaee.size() > 0) ? new Rectangle(bboxManager.getComplete(antennaee)) : null;
		Rectangle all_bbox = union(bracket_bbox, antennaee_bbox);

		// compute bbox for quantities
		if (max_quantity > 1) {
			Dimension quantity_text_dim = textBounds(max_quantity + "x",
					theGraphicOptions.NODE_FONT_FACE,
					theGraphicOptions.NODE_FONT_SIZE);
			all_bbox.y -= quantity_text_dim.width + 2; // the string is rotated
			all_bbox.height += quantity_text_dim.width + 2;
		}

		// restore linkages
		for (Residue antennae : antennaee) {
			if (!posManager.isOnBorder(antennae))
				bracket.removeChild(antennae);
		}

		// set bboxes
		bboxManager.setParent(bracket, structure_bbox);
		bboxManager.setCurrent(bracket, bracket_bbox);
		bboxManager.setBorder(bracket, bracket_bbox);
		bboxManager.setComplete(bracket, all_bbox);
		bboxManager.setSupport(bracket, bracket_bbox);
	}

	protected void paintBracket(Paintable paintable, Glycan _glycan,
			HashSet<Residue> selected_residues,
			HashSet<Linkage> selected_linkages,
			Collection<Residue> active_residues, PositionManager posManager,
			BBoxManager bboxManager) {

		if(_glycan == null || _glycan.getBracket() == null) return;
		
		Residue bracket = _glycan.getBracket();

		Rectangle parent_bbox = bboxManager.getParent(bracket);
		Rectangle bracket_bbox = bboxManager.getCurrent(bracket);
		Rectangle support_bbox = bboxManager.getSupport(bracket);

		// paint bracket
		boolean selected = selected_residues.contains(bracket);
		boolean active = (active_residues == null || active_residues.contains(bracket));

		if(!_glycan.isComposition())
			theResidueRenderer.paint(paintable, bracket, selected, active, false,
					parent_bbox, bracket_bbox, support_bbox,
					posManager.getOrientation(bracket));

		// paint antennaee
		for (Linkage link : bracket.getChildrenLinkages()) {
			Residue child = link.getChildResidue();
			int quantity = bboxManager.getLinkedResidues(child).size() + 1;

			Rectangle node_bbox = bboxManager.getParent(child);
			Rectangle child_bbox = bboxManager.getCurrent(child);
			Rectangle child_border_bbox = bboxManager.getBorder(child);

			if (child_bbox != null) {
				// paint edge
				if (!posManager.isOnBorder(child)) {
					selected = (selected_residues.contains(bracket) && selected_residues.contains(child)) || selected_linkages.contains(link);
					active = (active_residues == null || (active_residues.contains(bracket) && active_residues.contains(child)));

					if(!_glycan.isComposition())
						theLinkageRenderer.paintEdge(paintable, link, selected, node_bbox, node_bbox, child_bbox, child_border_bbox);
				}

				// paint child
				paintResidue(paintable, child, selected_residues, selected_linkages, active_residues, posManager, bboxManager);

				// paint info
				if (!posManager.isOnBorder(child))
					theLinkageRenderer.paintInfo(paintable, link, node_bbox, node_bbox, child_bbox, child_border_bbox);

				// paint quantity
				if (quantity > 1)
					paintQuantity(paintable, child, quantity, bboxManager);
			}
		}
	}

	// -------------------------
	// Export graphics

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#getImage(org.eurocarbdb.application.glycanbuilder.Glycan, boolean, boolean, boolean)
	 */
	@Override
	public BufferedImage getImage(Glycan structure, boolean opaque,
			boolean show_masses, boolean show_redend) {
		LinkedList<Glycan> structures = new LinkedList<Glycan>();
		if (structure != null)
			structures.add(structure);
		return getImage(structures, opaque, show_masses, show_redend, 1.);
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#getImage(org.eurocarbdb.application.glycanbuilder.Glycan, boolean, boolean, boolean, double)
	 */
	@Override
	public synchronized BufferedImage getImage(Glycan structure, boolean opaque,
			boolean show_masses, boolean show_redend, double scale) {
		LinkedList<Glycan> structures = new LinkedList<Glycan>();
		if (structure != null)
			structures.add(structure);
		return getImage(structures, opaque, show_masses, show_redend, scale);
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#getImage(java.util.Collection, boolean, boolean, boolean)
	 */
	@Override
	public BufferedImage getImage(Collection<Glycan> structures,
			boolean opaque, boolean show_masses, boolean show_redend) {
		return getImage(structures, opaque, show_masses, show_redend, 1.);
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#getImage(java.util.Collection, boolean, boolean, boolean, double)
	 */
	@Override
	public BufferedImage getImage(Collection<Glycan> structures,
			boolean opaque, boolean show_masses, boolean show_redend,
			double scale) {
		return getImage(structures, opaque, show_masses, show_redend, scale, new PositionManager(), new BBoxManager());
	}

	/* (non-Javadoc)
	 * @see org.eurocarbdb.application.glycanbuilder.GlycanRenderer#getImage(java.util.Collection, boolean, boolean, boolean, double, org.eurocarbdb.application.glycanbuilder.PositionManager, org.eurocarbdb.application.glycanbuilder.BBoxManager)
	 */
	@Override
	abstract public BufferedImage getImage(Collection<Glycan> structures,boolean opaque, boolean show_masses, boolean show_redend,double scale,PositionManager posManager,BBoxManager bboxManager);	
}
