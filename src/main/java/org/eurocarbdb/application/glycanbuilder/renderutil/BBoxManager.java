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
import java.awt.Rectangle;

import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.*;

import java.awt.Dimension;
import java.awt.Point;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.linkage.Union;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;

/**
 * Objects of this class are used to compute and store the bounding box of a
 * residue. The bounding boxes are used to identify the position of a residue in
 * the display area.
 * 
 * @see GlycanRendererAWT
 * @see ResidueRendererAWT
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class BBoxManager {

	public GraphicOptions theGraphicOptions;

	public HashMap<Residue, Rectangle> parent_bboxes;
	public HashMap<Residue, Rectangle> current_bboxes;
	public HashMap<Residue, Rectangle> border_bboxes;
	public HashMap<Residue, Rectangle> complete_bboxes;
	public HashMap<Residue, Rectangle> support_bboxes;

	public HashMap<Residue, ArrayList<Residue>> linked_residues;

	//

	/**
	 * Default constructor.
	 */

	public BBoxManager() {
		parent_bboxes = new HashMap<Residue, Rectangle>();
		current_bboxes = new HashMap<Residue, Rectangle>();
		border_bboxes = new HashMap<Residue, Rectangle>();
		complete_bboxes = new HashMap<Residue, Rectangle>();
		support_bboxes = new HashMap<Residue, Rectangle>();

		linked_residues = new HashMap<Residue, ArrayList<Residue>>();
	}

	/**
	 * Return the graphic options used to determine the bounding box size.
	 */
	public GraphicOptions getGraphicOptions() {
		return theGraphicOptions;
	}

	/**
	 * Set the graphic options used to determine the bounding box size.
	 */
	public void setGraphicOptions(GraphicOptions opt) {
		theGraphicOptions = opt;
	}

	// ----------------
	// member access

	/**
	 * Return an iterator over the set of bounding boxes.
	 */
	public Iterator<Map.Entry<Residue, Rectangle>> iterator() {
		return current_bboxes.entrySet().iterator();
	}

	/**
	 * Clear all fields.
	 */
	public void reset() {
		parent_bboxes.clear();
		current_bboxes.clear();
		border_bboxes.clear();
		complete_bboxes.clear();
		support_bboxes.clear();

		linked_residues.clear();
	}

	/**
	 * Return the set of residues that are shown at the same position. Used to
	 * represent multiple residues with uncertain connectivity.
	 */
	public ArrayList<Residue> getLinkedResidues(Residue node) {
		ArrayList<Residue> ret = linked_residues.get(node);
		return ret == null ? new ArrayList<Residue>() : ret;
	}

	/**
	 * Link all the residues in the subtree rooted at <code>root_other</code>
	 * with <code>dest</code>
	 * 
	 * @see #getLinkedResidues
	 */
	public void linkSubtree(Residue dest, Residue root_other) {
		if (dest != root_other) {
			ArrayList<Residue> lst_red = new ArrayList();
			if (linked_residues.get(dest) == null) {
				//linked_residues.put(root_dest, new Union<Residue>(root_other));
				lst_red.add(root_other);
				linked_residues.put(dest, lst_red);
			}else
				linked_residues.get(dest).add(root_other);
		}

		for (Linkage l : root_other.getChildrenLinkages())
			linkSubtree(dest, l.getChildResidue());
	}

	/**
	 * Link all the residues in the subtree rooted at <code>root_other</code>
	 * with the corresponding residues in the subtree rooted at
	 * <code>root_dest</code>. The two subtrees must have the same topology.
	 * 
	 * @see #getLinkedResidues
	 */
	public void linkSubtrees(Residue root_dest, Residue root_other) throws Exception {
		if (root_dest == null)
			return;

		if (!root_dest.subtreeEquals(root_other))
			throw new Exception("Subtrees do not match");

		linkSubtreesPVT(root_dest, root_other);
	}

	private void linkSubtreesPVT(Residue root_dest, Residue root_other) {
		ArrayList<Residue> lst_red = new ArrayList();
		if (linked_residues.get(root_dest) == null) {
			lst_red.add(root_other);
			linked_residues.put(root_dest, lst_red);
			//linked_residues.put(root_dest, new Union<Residue>(root_other));
		}else
			linked_residues.get(root_dest).add(root_other);

		for (int i = 0; i < root_dest.getNoChildren(); i++)
			linkSubtreesPVT(root_dest.getLinkageAt(i).getChildResidue(),
					root_other.getLinkageAt(i).getChildResidue());
	}

	/**
	 * Return the bounding box occupied by a structure.
	 */
	public Rectangle getBBox(Glycan structure, boolean show_redend) {
		if (structure == null)
			return null;

		return union(getComplete(structure.getRoot(show_redend)),
				getComplete(structure.getBracket()));
	}

	/**
	 * Initialize all the bounding boxes corresponding to a residue.
	 */
	public void setAllBBoxes(Residue node, Rectangle bbox) {
		setParent(node, new Rectangle(bbox));
		setCurrent(node, new Rectangle(bbox));
		setBorder(node, new Rectangle(bbox));
		setComplete(node, new Rectangle(bbox));
		setSupport(node, new Rectangle(bbox));
	}

	/**
	 * Set the bounding box of the parent residue.
	 */
	public void setParent(Residue node, Rectangle bbox) {
		if (node != null && bbox != null)
			parent_bboxes.put(node, new Rectangle(bbox));
	}

	/**
	 * Get the bounding box of the parent residue.
	 */
	public Rectangle getParent(Residue node) {
		if (node == null)
			return null;
		return parent_bboxes.get(node);
	}

	/**
	 * Set the bounding box of the residue.
	 */
	public void setCurrent(Residue node, Rectangle bbox) {
		if (node != null && bbox != null)
			current_bboxes.put(node, new Rectangle(bbox));
	}

	/**
	 * Get the bounding box of the residue.
	 */
	public Rectangle getCurrent(Residue node) {
		if (node == null)
			return null;
		return current_bboxes.get(node);
	}

	/**
	 * Set the bounding box of the residue including the children position on
	 * its border.
	 */
	public void setBorder(Residue node, Rectangle bbox) {
		if (node != null && bbox != null)
			border_bboxes.put(node, new Rectangle(bbox));
	}

	/**
	 * Get the bounding box of the residue including the children position on
	 * its border.
	 */
	public Rectangle getBorder(Residue node) {
		if (node == null)
			return null;
		return border_bboxes.get(node);
	}

	/**
	 * Set the bounding box of the subtree rooted at the residue.
	 */
	public void setComplete(Residue node, Rectangle bbox) {
		if (node != null && bbox != null)
			complete_bboxes.put(node, new Rectangle(bbox));
	}

	/**
	 * Get the bounding box of the subtree rooted at the residue.
	 */
	public Rectangle getComplete(Residue node) {
		if (node == null)
			return null;
		return complete_bboxes.get(node);
	}

	/**
	 * Set the bounding box used to compute the spatial orientation of the
	 * residue.
	 */
	public void setSupport(Residue node, Rectangle bbox) {
		if (node != null && bbox != null)
			support_bboxes.put(node, new Rectangle(bbox));
	}

	/**
	 * Get the bounding box used to compute the spatial orientation of the
	 * residue.
	 */
	public Rectangle getSupport(Residue node) {
		if (node == null)
			return null;
		return support_bboxes.get(node);
	}

	// ----------------
	// functions

	/**
	 * Return the residue whose bounding box contains the point.
	 */
	public Residue getNodeAtPoint(Point p) {
		if (p == null)
			return null;

		for (Iterator<Map.Entry<Residue, Rectangle>> i = current_bboxes
				.entrySet().iterator(); i.hasNext();) {
			Map.Entry<Residue, Rectangle> e = i.next();
			if (e.getValue().contains(p))
				return e.getKey();
		}
		return null;
	}

	/**
	 * Return the residues whose bounding boxes intersect the given rectangle.
	 */
	public LinkedList<Residue> getNodesInside(Rectangle r) {
		if (r == null)
			return null;

		LinkedList<Residue> nodes = new LinkedList<Residue>();
		for (Iterator<Map.Entry<Residue, Rectangle>> i = current_bboxes
				.entrySet().iterator(); i.hasNext();) {
			Map.Entry<Residue, Rectangle> e = i.next();
			if (r.intersects(e.getValue()))
				nodes.add(e.getKey());
		}

		return nodes;
	}

	/**
	 * Return the union of all the bounding boxes of the subtrees rooted at the
	 * given residues.
	 */
	public Rectangle getComplete(List<Residue> nodes) {
		Rectangle bbox = null;
		for (Residue r : nodes)
			bbox = Geometry.union(getComplete(r), bbox);
		return bbox;
	}

	/**
	 * Return the bounding box of the central residue in a list of children. If
	 * the number of residues is even two bounding boxes are combined.
	 */
	public Rectangle getCurrent(List<Residue> nodes) {
		int n = nodes.size();
		if (n == 0)
			return null;
		if ((n % 2) == 1)
			return getCurrent(nodes.get(n / 2));
		return Geometry.union(getCurrent(nodes.get(n / 2 - 1)),
				getCurrent(nodes.get(n / 2)));
	}

	private Rectangle getCurrentComplete(List<Residue> nodes) {
		Rectangle bbox = null;
		if (nodes != null) {
			for (Residue r : nodes)
				bbox = Geometry.union(bbox, getCurrent(r));
		}
		return bbox;
	}

	private LinkedList<Rectangle> getAll(List<Residue> v) throws Exception {
		LinkedList<Rectangle> ret = new LinkedList<Rectangle>();
		if (v != null) {
			for (Residue r : v)
				getAll(r, ret);
		}
		return ret;
	}

	private LinkedList<Rectangle> getAll(Residue r) throws Exception {
		LinkedList<Rectangle> ret = new LinkedList<Rectangle>();
		getAll(r, ret);
		return ret;
	}

	private void getAll(Residue r, List<Rectangle> buffer) throws Exception {
		if (r == null)
			throw new Exception("Empty node");

		Rectangle cur = getCurrent(r);
		if (cur == null)
			throw new Exception("Empty bbox");

		buffer.add(cur);
		for (Linkage l : r.getChildrenLinkages())
			getAll(l.getChildResidue(), buffer);
	}

	/**
	 * Return the difference in radiants between two angles going clockwise.
	 */
	static public double distanceCW(double ref_angle, double other_angle) {
		if (other_angle > ref_angle)
			return (other_angle - ref_angle);
		return (2. * Math.PI + other_angle - ref_angle);
	}

	/**
	 * Return the difference in radiants between two angles going
	 * counter-clockwise.
	 */
	static public double distanceCCW(double ref_angle, double other_angle) {
		if (other_angle < ref_angle)
			return (ref_angle - other_angle);
		return (2. * Math.PI + ref_angle - other_angle);
	}

	/**
	 * Return the angle of the LinkedList joining the centers of the bounding boxes
	 * of the two residues.
	 */
	public double getScreenAngle(Residue node, Residue parent) {
		Rectangle node_bbox = getCurrent(node);
		Rectangle par_bbox = getCurrent(parent);
		if (node_bbox == null || par_bbox == null)
			return 0.;
		return angle(center(node_bbox), center(par_bbox));
	}

	public Residue getNearestCW(Residue node, Collection<Residue> brothers) {
		if (node == null)
			return null;

		Residue parent = node.getParent();
		if (parent == null)
			return null;

		double best_dist = 0.;
		Residue best_residue = null;
		double node_angle = getScreenAngle(node, parent);
		for (Residue other : brothers) {
			double other_angle = getScreenAngle(other, parent);
			double dist = distanceCW(node_angle, other_angle);
			if (dist < Math.PI / 2) {
				if (best_residue == null || dist < best_dist) {
					best_residue = other;
					best_dist = dist;
				}
			}
		}
		return best_residue;
	}

	public Residue getNearestCCW(Residue node, Collection<Residue> brothers) {
		if (node == null)
			return null;

		Residue parent = node.getParent();
		if (parent == null)
			return null;

		double best_dist = 0.;
		Residue best_residue = null;
		double node_angle = getScreenAngle(node, parent);
		for (Residue other : brothers) {
			double other_angle = getScreenAngle(other, parent);
			double dist = distanceCCW(node_angle, other_angle);
			if (dist < Math.PI / 2) {
				if (best_residue == null || dist < best_dist) {
					best_residue = other;
					best_dist = dist;
				}
			}
		}
		return best_residue;
	}

	public Residue getNearestUp(Residue node) {
		return getNearestUp(node, current_bboxes.keySet().iterator());
	}

	public Residue getNearestUp(Residue node, Collection<Residue> brothers) {
		return getNearestUp(node, brothers.iterator());
	}

	public Residue getNearestUp(Residue node, Iterator<Residue> first) {
		if (node == null || first == null)
			return null;

		Rectangle cur_rect = getCurrent(node);
		Residue best_node = null;
		Rectangle best_rect = null;
		double best_dist = 0.;
		for (Iterator<Residue> i = first; i.hasNext();) {
			Residue nav_node = i.next();
			Rectangle nav_rect = getCurrent(nav_node);
			if (isUp(nav_rect, cur_rect)) {
				double nav_dist = distance(nav_rect, cur_rect);
				if (best_node == null || nav_dist < best_dist
						|| (nav_dist == best_dist && best_rect.x > nav_rect.x)) {
					best_node = nav_node;
					best_rect = nav_rect;
					best_dist = nav_dist;
				}
			}
		}
		return best_node;
	}

	public Residue getNearestDown(Residue node) {
		return getNearestDown(node, current_bboxes.keySet().iterator());
	}

	public Residue getNearestDown(Residue node, Collection<Residue> brothers) {
		return getNearestDown(node, brothers.iterator());
	}

	public Residue getNearestDown(Residue node, Iterator<Residue> first) {
		if (node == null || first == null)
			return null;

		Rectangle cur_rect = getCurrent(node);
		Residue best_node = null;
		Rectangle best_rect = null;
		double best_dist = 0.;
		for (Iterator<Residue> i = first; i.hasNext();) {
			Residue nav_node = i.next();
			Rectangle nav_rect = getCurrent(nav_node);
			if (isDown(nav_rect, cur_rect)) {
				double nav_dist = distance(nav_rect, cur_rect);
				if (best_node == null || nav_dist < best_dist
						|| (nav_dist == best_dist && best_rect.x > nav_rect.x)) {
					best_node = nav_node;
					best_rect = nav_rect;
					best_dist = nav_dist;
				}
			}
		}
		return best_node;
	}

	public Residue getNearestLeft(Residue node) {
		return getNearestLeft(node, current_bboxes.keySet().iterator());
	}

	public Residue getNearestLeft(Residue node, Collection<Residue> brothers) {
		return getNearestLeft(node, brothers.iterator());
	}

	public Residue getNearestLeft(Residue node, Iterator<Residue> first) {
		if (node == null || first == null)
			return null;

		Rectangle cur_rect = getCurrent(node);
		Residue best_node = null;
		Rectangle best_rect = null;
		double best_dist = 0.;
		for (Iterator<Residue> i = first; i.hasNext();) {
			Residue nav_node = i.next();
			Rectangle nav_rect = getCurrent(nav_node);
			if (isLeft(nav_rect, cur_rect)) {
				double nav_dist = distance(nav_rect, cur_rect);
				if (best_node == null || nav_dist < best_dist
						|| (nav_dist == best_dist && best_rect.y > nav_rect.y)) {
					best_node = nav_node;
					best_rect = nav_rect;
					best_dist = nav_dist;
				}
			}
		}
		return best_node;
	}

	public Residue getNearestRight(Residue node) {
		return getNearestRight(node, current_bboxes.keySet().iterator());
	}

	public Residue getNearestRight(Residue node, Collection<Residue> brothers) {
		return getNearestRight(node, brothers.iterator());
	}

	public Residue getNearestRight(Residue node, Iterator<Residue> first) {
		if (node == null || first == null)
			return null;

		Rectangle cur_rect = getCurrent(node);
		Residue best_node = null;
		Rectangle best_rect = null;
		double best_dist = 0.;
		for (Iterator<Residue> i = first; i.hasNext();) {
			Residue nav_node = i.next();
			Rectangle nav_rect = getCurrent(nav_node);
			if (isRight(nav_rect, cur_rect)) {
				double nav_dist = distance(nav_rect, cur_rect);
				if (best_node == null || nav_dist < best_dist
						|| (nav_dist == best_dist && best_rect.y > nav_rect.y)) {
					best_node = nav_node;
					best_rect = nav_rect;
					best_dist = nav_dist;
				}
			}
		}
		return best_node;
	}

	// ----------------
	// traslation

	/**
	 * Translate the bounding boxes of the given residues by a specified
	 * displacement.
	 */
	public void translate(int sx, int sy, List<Residue> nodes) {
		if (nodes != null) {
			for (Iterator<Residue> i = nodes.iterator(); i.hasNext();)
				translate(sx, sy, i.next());
		}
	}

	/**
	 * Translate the bounding boxes of the given residues by a specified
	 * displacement.
	 */
	public void translate(Dimension s, List<Residue> nodes) {
		if (nodes != null) {
			for (Iterator<Residue> i = nodes.iterator(); i.hasNext();)
				translate(s, i.next());
		}
	}

	/**
	 * Translate the bounding boxes of all the residues of the given structures
	 * by a specified displacement.
	 */
	public void translate(Dimension s, Collection<Glycan> c) {
		translate(s.width, s.height, c);
	}

	/**
	 * Translate the bounding boxes of all the residues of the given structures
	 * by a specified displacement.
	 */
	public void translate(int sx, int sy, Collection<Glycan> c) {
		for (Glycan g : c)
			translate(sx, sy, g);
	}

	/**
	 * Translate the bounding boxes of all the residues of the given structure
	 * by a specified displacement.
	 */
	public void translate(Dimension s, Glycan g) {
		if (s != null)
			translate(s.width, s.height, g);
	}

	/**
	 * Translate the bounding boxes of all the residues of the given structure
	 * by a specified displacement.
	 */
	public void translate(int sx, int sy, Glycan g) {
		if (g != null) {
			translate(sx, sy, g.getRoot());
			translate(sx, sy, g.getBracket());
		}
	}

	/**
	 * Translate the bounding box of the residue by a specified displacement.
	 */
	public void translate(Dimension s, Residue node) {
		if (s != null)
			translate(s.width, s.height, node);
	}

	/**
	 * Translate the bounding box of the residue by a specified displacement.
	 */
	public void translate(int sx, int sy, Residue node) {
		if (node != null) {
			translate(sx, sy, parent_bboxes.get(node));
			translate(sx, sy, current_bboxes.get(node));
			translate(sx, sy, border_bboxes.get(node));
			translate(sx, sy, complete_bboxes.get(node));
			translate(sx, sy, support_bboxes.get(node));

			for (Iterator<Linkage> i = node.iterator(); i.hasNext();)
				translate(sx, sy, i.next().getChildResidue());
		}
	}

	/**
	 * Translate the coordinate of a rectangle by a specified displacement.
	 */
	static public void translate(int sx, int sy, Rectangle r) {
		if (r != null)
			r.translate(sx, sy);
	}

	// -------------
	// alignment

	private LinkedList<Rectangle> singleton(Rectangle r) {
		LinkedList<Rectangle> ret = new LinkedList<Rectangle>();
		if (r != null)
			ret.add(r);
		return ret;
	}

	private LinkedList<Residue> singleton(Residue r) {
		LinkedList<Residue> ret = new LinkedList<Residue>();
		if (r != null)
			ret.add(r);
		return ret;
	}

	static private void assertAlignment(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		if (ref_all_bboxes == null || ref_all_bboxes.size() == 0)
			throw new Exception("Empty reference bbox set");
		if (ref_sup_bbox == null)
			throw new Exception("Empty reference bbox");
		if (cur_all_bboxes == null || cur_all_bboxes.size() == 0)
			throw new Exception("Empty current bbox set");
		if (cur_sup_bbox == null)
			throw new Exception("Empty current bbox");
		if (space < 0)
			throw new Exception("Negative distance");
	}

	// --- VERTICAL

	public void alignLeftsOnTop(List<Residue> ref_nodes, List<Residue> nodes,
			int space) throws Exception {
		alignLeftsOnTop(getAll(ref_nodes), getCurrent(ref_nodes), nodes, space);
	}

	public void alignLeftsOnTop(Residue ref_node, List<Residue> nodes, int space)
			throws Exception {
		alignLeftsOnTop(getAll(ref_node), getCurrent(ref_node), nodes, space);
	}

	public void alignLeftsOnTop(Rectangle ref_bbox, List<Residue> nodes,
			int space) throws Exception {
		alignLeftsOnTop(singleton(ref_bbox), ref_bbox, nodes, space);
	}

	public void alignLeftsOnTop(Residue ref_node, Residue node, int space)
			throws Exception {
		alignLeftsOnTop(getAll(ref_node), getCurrent(ref_node),
				singleton(node), space);
	}

	public void alignLeftsOnTop(Rectangle ref_bbox, Residue node, int space)
			throws Exception {
		alignLeftsOnTop(singleton(ref_bbox), ref_bbox, singleton(node), space);
	}

	public void alignLeftsOnTop(Rectangle ref_bbox, List<Residue> ref_nodes,
			List<Residue> sup_nodes, List<Residue> nodes, int space)
			throws Exception {
		alignLeftsOnTop(new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrent(sup_nodes), nodes, space);
	}

	public void alignLeftsOnTop(List<Residue> ref_sup_nodes,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignLeftsOnTop(
				new Union<Rectangle>(getAll(ref_sup_nodes))
						.and(getAll(ref_nodes)),
				getCurrent(ref_sup_nodes), getAll(nodes),
				getCurrent(sup_nodes), nodes, space);
	}

	private void alignLeftsOnTop(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		alignLeftsOnTop(ref_all_bboxes, ref_sup_bbox, getAll(nodes),
				getCurrent(nodes), nodes, space);
	}

	private void alignLeftsOnTop(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, space);
			translate(left(ref_sup_bbox) - left(cur_sup_bbox), 0, nodes); // align
																			// left
			translate(
					0,
					shiftToTop(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), nodes); // put on top
		}
	}

	public void alignLeftsOnBottom(List<Residue> ref_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignLeftsOnBottom(getAll(ref_nodes), getCurrent(ref_nodes), nodes,
				space);
	}

	public void alignLeftsOnBottom(Residue ref_node, List<Residue> nodes,
			int space) throws Exception {
		alignLeftsOnBottom(getAll(ref_node), getCurrent(ref_node), nodes, space);
	}

	public void alignLeftsOnBottom(Rectangle ref_bbox, List<Residue> nodes,
			int space) throws Exception {
		alignLeftsOnBottom(singleton(ref_bbox), ref_bbox, nodes, space);
	}

	public void alignLeftsOnBottom(Residue ref_node, Residue node, int space)
			throws Exception {
		alignLeftsOnBottom(getAll(ref_node), getCurrent(ref_node),
				singleton(node), space);
	}

	public void alignLeftsOnBottom(Rectangle ref_bbox, Residue node, int space)
			throws Exception {
		alignLeftsOnBottom(singleton(ref_bbox), ref_bbox, singleton(node),
				space);
	}

	public void alignLeftsOnBottom(Rectangle ref_bbox, List<Residue> ref_nodes,
			List<Residue> sup_nodes, List<Residue> nodes, int space)
			throws Exception {
		alignLeftsOnBottom(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrent(sup_nodes), nodes, space);
	}

	public void alignLeftsOnBottom(List<Residue> ref_sup_nodes,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignLeftsOnBottom(
				new Union<Rectangle>(getAll(ref_sup_nodes))
						.and(getAll(ref_nodes)),
				getCurrent(ref_sup_nodes), getAll(nodes),
				getCurrent(sup_nodes), nodes, space);
	}

	private void alignLeftsOnBottom(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		alignLeftsOnBottom(ref_all_bboxes, ref_sup_bbox, getAll(nodes),
				getCurrent(nodes), nodes, space);
	}

	private void alignLeftsOnBottom(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, space);
			translate(left(ref_sup_bbox) - left(cur_sup_bbox), 0, nodes); // align
																			// left
			translate(
					0,
					shiftToBottom(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), nodes); // put on bottom
		}
	}

	public void alignRightsOnTop(List<Residue> ref_nodes, List<Residue> nodes,
			int space) throws Exception {
		alignRightsOnTop(getAll(ref_nodes), getCurrent(ref_nodes), nodes, space);
	}

	public void alignRightsOnTop(Residue ref_node, List<Residue> nodes,
			int space) throws Exception {
		alignRightsOnTop(getAll(ref_node), getCurrent(ref_node), nodes, space);
	}

	public void alignRightsOnTop(Rectangle ref_bbox, List<Residue> nodes,
			int space) throws Exception {
		alignRightsOnTop(singleton(ref_bbox), ref_bbox, nodes, space);
	}

	public void alignRightsOnTop(Residue ref_node, Residue node, int space)
			throws Exception {
		alignRightsOnTop(getAll(ref_node), getCurrent(ref_node),
				singleton(node), space);
	}

	public void alignRightsOnTop(Rectangle ref_bbox, Residue node, int space)
			throws Exception {
		alignRightsOnTop(singleton(ref_bbox), ref_bbox, singleton(node), space);
	}

	public void alignRightsOnTop(Rectangle ref_bbox, List<Residue> ref_nodes,
			List<Residue> sup_nodes, List<Residue> nodes, int space)
			throws Exception {
		alignRightsOnTop(new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrent(sup_nodes), nodes, space);
	}

	public void alignRightsOnTop(List<Residue> ref_sup_nodes,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignRightsOnTop(
				new Union<Rectangle>(getAll(ref_sup_nodes))
						.and(getAll(ref_nodes)),
				getCurrent(ref_sup_nodes), getAll(nodes),
				getCurrent(sup_nodes), nodes, space);
	}

	private void alignRightsOnTop(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		alignRightsOnTop(ref_all_bboxes, ref_sup_bbox, getAll(nodes),
				getCurrent(nodes), nodes, space);
	}

	private void alignRightsOnTop(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, space);
			translate(right(ref_sup_bbox) - right(cur_sup_bbox), 0, nodes); // align
																			// right
			translate(
					0,
					shiftToTop(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), nodes); // put on top
		}
	}

	public void alignRightsOnBottom(List<Residue> ref_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignRightsOnBottom(getAll(ref_nodes), getCurrent(ref_nodes), nodes,
				space);
	}

	public void alignRightsOnBottom(Residue ref_node, List<Residue> nodes,
			int space) throws Exception {
		alignRightsOnBottom(getAll(ref_node), getCurrent(ref_node), nodes,
				space);
	}

	public void alignRightsOnBottom(Rectangle ref_bbox, List<Residue> nodes,
			int space) throws Exception {
		alignRightsOnBottom(singleton(ref_bbox), ref_bbox, nodes, space);
	}

	public void alignRightsOnBottom(Residue ref_node, Residue node, int space)
			throws Exception {
		alignRightsOnBottom(getAll(ref_node), getCurrent(ref_node),
				singleton(node), space);
	}

	public void alignRightsOnBottom(Rectangle ref_bbox, Residue node, int space)
			throws Exception {
		alignRightsOnBottom(singleton(ref_bbox), ref_bbox, singleton(node),
				space);
	}

	public void alignRightsOnBottom(Rectangle ref_bbox,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignRightsOnBottom(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrent(sup_nodes), nodes, space);
	}

	public void alignRightsOnBottom(List<Residue> ref_sup_nodes,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignRightsOnBottom(
				new Union<Rectangle>(getAll(ref_sup_nodes))
						.and(getAll(ref_nodes)),
				getCurrent(ref_sup_nodes), getAll(nodes),
				getCurrent(sup_nodes), nodes, space);
	}

	private void alignRightsOnBottom(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		alignRightsOnBottom(ref_all_bboxes, ref_sup_bbox, getAll(nodes),
				getCurrent(nodes), nodes, space);
	}

	private void alignRightsOnBottom(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, space);
			translate(right(ref_sup_bbox) - right(cur_sup_bbox), 0, nodes); // align
																			// right
			translate(
					0,
					shiftToBottom(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), nodes); // put on bottom
		}
	}

	public void alignCentersOnTop(List<Residue> ref_nodes, List<Residue> nodes,
			int space) throws Exception {
		alignCentersOnTop(getAll(ref_nodes), getCurrent(ref_nodes), nodes,
				space);
	}

	public void alignCentersOnTop(Residue ref_node, List<Residue> nodes,
			int space) throws Exception {
		alignCentersOnTop(getAll(ref_node), getCurrent(ref_node), nodes, space);
	}

	public void alignCentersOnTop(Rectangle ref_bbox, List<Residue> nodes,
			int space) throws Exception {
		alignCentersOnTop(singleton(ref_bbox), ref_bbox, nodes, space);
	}

	public void alignCentersOnTop(Residue ref_node, Residue node, int space)
			throws Exception {
		alignCentersOnTop(getAll(ref_node), getCurrent(ref_node),
				singleton(node), space);
	}

	public void alignCentersOnTop(Rectangle ref_bbox, Residue node, int space)
			throws Exception {
		alignCentersOnTop(singleton(ref_bbox), ref_bbox, singleton(node), space);
	}

	public void alignCentersOnTop(Rectangle ref_bbox, List<Residue> ref_nodes,
			List<Residue> sup_nodes, List<Residue> nodes, int space)
			throws Exception {
		alignCentersOnTop(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrent(sup_nodes), nodes, space);
	}

	public void alignCentersOnTop(List<Residue> ref_sup_nodes,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignCentersOnTop(
				new Union<Rectangle>(getAll(ref_sup_nodes))
						.and(getAll(ref_nodes)),
				getCurrent(ref_sup_nodes), getAll(nodes),
				getCurrent(sup_nodes), nodes, space);
	}

	private void alignCentersOnTop(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		alignCentersOnTop(ref_all_bboxes, ref_sup_bbox, getAll(nodes),
				getCurrent(nodes), nodes, space);
	}

	private void alignCentersOnTop(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, space);
			translate(midx(ref_sup_bbox) - midx(cur_sup_bbox), 0, nodes); // align
																			// center
			translate(
					0,
					shiftToTop(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), nodes); // put on top
		}
	}

	public void alignCentersOnBottom(List<Residue> ref_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignCentersOnBottom(getAll(ref_nodes), getCurrent(ref_nodes), nodes,
				space);
	}

	public void alignCentersOnBottom(Residue ref_node, List<Residue> nodes,
			int space) throws Exception {
		alignCentersOnBottom(getAll(ref_node), getCurrent(ref_node), nodes,
				space);
	}

	public void alignCentersOnBottom(Rectangle ref_bbox, List<Residue> nodes,
			int space) throws Exception {
		alignCentersOnBottom(singleton(ref_bbox), ref_bbox, nodes, space);
	}

	public void alignCentersOnBottom(Residue ref_node, Residue node, int space)
			throws Exception {
		alignCentersOnBottom(getAll(ref_node), getCurrent(ref_node),
				singleton(node), space);
	}

	public void alignCentersOnBottom(Rectangle ref_bbox, Residue node, int space)
			throws Exception {
		alignCentersOnBottom(singleton(ref_bbox), ref_bbox, singleton(node),
				space);
	}

	public void alignCentersOnBottom(Rectangle ref_bbox,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignCentersOnBottom(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrent(sup_nodes), nodes, space);
	}

	public void alignCentersOnBottom(List<Residue> ref_sup_nodes,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignCentersOnBottom(
				new Union<Rectangle>(getAll(ref_sup_nodes))
						.and(getAll(ref_nodes)),
				getCurrent(ref_sup_nodes), getAll(nodes),
				getCurrent(sup_nodes), nodes, space);
	}

	private void alignCentersOnBottom(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		alignCentersOnBottom(ref_all_bboxes, ref_sup_bbox, getAll(nodes),
				getCurrent(nodes), nodes, space);
	}

	private void alignCentersOnBottom(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, space);
			translate(midx(ref_sup_bbox) - midx(cur_sup_bbox), 0, nodes); // align
																			// center
			translate(
					0,
					shiftToBottom(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), nodes); // put on bottom
		}
	}

	public void alignCornersOnTopAtLeft(Rectangle ref_bbox,
			List<Residue> ref_nodes, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		alignCornersOnTopAtLeft(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrentComplete(nodes), nodes,
				xspace, yspace);
	}

	private void alignCornersOnTopAtLeft(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, xspace);
			translate(left(ref_sup_bbox) - right(cur_sup_bbox) - xspace, 0,
					nodes); // align corner
			translate(
					0,
					shiftToTop(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, yspace), nodes); // put on top
		}
	}

	public void alignCornersOnTopAtRight(Rectangle ref_bbox,
			List<Residue> ref_nodes, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		alignCornersOnTopAtRight(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrentComplete(nodes), nodes,
				xspace, yspace);
	}

	private void alignCornersOnTopAtRight(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, xspace);
			translate(right(ref_sup_bbox) - left(cur_sup_bbox) + xspace, 0,
					nodes); // align center
			translate(
					0,
					shiftToTop(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, yspace), nodes); // put on top
		}
	}

	public void alignCornersOnBottomAtLeft(Rectangle ref_bbox,
			List<Residue> ref_nodes, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		alignCornersOnBottomAtLeft(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrentComplete(nodes), nodes,
				xspace, yspace);
	}

	private void alignCornersOnBottomAtLeft(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, xspace);
			translate(left(ref_sup_bbox) - right(cur_sup_bbox) - xspace, 0,
					nodes); // align center
			translate(
					0,
					shiftToBottom(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, yspace), nodes); // put on bottom
		}
	}

	public void alignCornersOnBottomAtRight(Rectangle ref_bbox,
			List<Residue> ref_nodes, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		alignCornersOnBottomAtRight(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrentComplete(nodes), nodes,
				xspace, yspace);
	}

	private void alignCornersOnBottomAtRight(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, xspace);
			translate(right(ref_sup_bbox) - left(cur_sup_bbox) + xspace, 0,
					nodes); // align center
			translate(
					0,
					shiftToBottom(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, yspace), nodes); // put on bottom
		}
	}

	private int shiftToTop(List<Rectangle> ref_bboxes, Rectangle ref_bbox,
			List<Rectangle> cur_bboxes, Rectangle cur_bbox, int space)
			throws Exception {
		return shiftToTop(ref_bboxes, ref_bbox, cur_bboxes, cur_bbox, space, 0);
	}

	private int shiftToTop(List<Rectangle> ref_bboxes, Rectangle ref_bbox,
			List<Rectangle> cur_bboxes, Rectangle cur_bbox, int space,
			int min_ref_space) throws Exception {
		int toll = theGraphicOptions.NODE_SPACE / 2 - 1;
		int shift = top(ref_bbox) - Math.max(space, min_ref_space)
				- bottom(cur_bbox);
		for (Rectangle cur : cur_bboxes)
			for (Rectangle ref : ref_bboxes)
				if (overlapx(cur, ref, toll))
					shift = Math.min(shift, top(ref) - space - bottom(cur));
		return shift;
	}

	private int shiftToBottom(List<Rectangle> ref_bboxes, Rectangle ref_bbox,
			List<Rectangle> cur_bboxes, Rectangle cur_bbox, int space)
			throws Exception {
		return shiftToBottom(ref_bboxes, ref_bbox, cur_bboxes, cur_bbox, space,
				0);
	}

	private int shiftToBottom(List<Rectangle> ref_bboxes, Rectangle ref_bbox,
			List<Rectangle> cur_bboxes, Rectangle cur_bbox, int space,
			int min_ref_space) throws Exception {
		int toll = theGraphicOptions.NODE_SPACE / 2 - 1;
		int shift = bottom(ref_bbox) + Math.max(space, min_ref_space)
				- top(cur_bbox);
		for (Rectangle cur : cur_bboxes)
			for (Rectangle ref : ref_bboxes)
				if (overlapx(cur, ref, toll))
					shift = Math.max(shift, bottom(ref) + space - top(cur));
		return shift;
	}

	// --- HORIZONTAL

	public void alignTopsOnLeft(List<Residue> ref_nodes, List<Residue> nodes,
			int space) throws Exception {
		alignTopsOnLeft(getAll(ref_nodes), getCurrent(ref_nodes), nodes, space);
	}

	public void alignTopsOnLeft(Residue ref_node, List<Residue> nodes, int space)
			throws Exception {
		alignTopsOnLeft(getAll(ref_node), getCurrent(ref_node), nodes, space);
	}

	public void alignTopsOnLeft(Rectangle ref_bbox, List<Residue> nodes,
			int space) throws Exception {
		alignTopsOnLeft(singleton(ref_bbox), ref_bbox, nodes, space);
	}

	public void alignTopsOnLeft(Residue ref_node, Residue node, int space)
			throws Exception {
		alignTopsOnLeft(getAll(ref_node), getCurrent(ref_node),
				singleton(node), space);
	}

	public void alignTopsOnLeft(Rectangle ref_bbox, Residue node, int space)
			throws Exception {
		alignTopsOnLeft(singleton(ref_bbox), ref_bbox, singleton(node), space);
	}

	public void alignTopsOnLeft(Rectangle ref_bbox, List<Residue> ref_nodes,
			List<Residue> sup_nodes, List<Residue> nodes, int space)
			throws Exception {
		alignTopsOnLeft(new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrent(sup_nodes), nodes, space);
	}

	public void alignTopsOnLeft(List<Residue> ref_sup_nodes,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignTopsOnLeft(
				new Union<Rectangle>(getAll(ref_sup_nodes))
						.and(getAll(ref_nodes)),
				getCurrent(ref_sup_nodes), getAll(nodes),
				getCurrent(sup_nodes), nodes, space);
	}

	private void alignTopsOnLeft(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		alignTopsOnLeft(ref_all_bboxes, ref_sup_bbox, getAll(nodes),
				getCurrent(nodes), nodes, space);
	}

	private void alignTopsOnLeft(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, space);
			translate(0, top(ref_sup_bbox) - top(cur_sup_bbox), nodes); // align
																		// top
			translate(
					shiftToLeft(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), 0, nodes); // put on left
		}
	}

	public void alignTopsOnRight(List<Residue> ref_nodes, List<Residue> nodes,
			int space) throws Exception {
		alignTopsOnRight(getAll(ref_nodes), getCurrent(ref_nodes), nodes, space);
	}

	public void alignTopsOnRight(Residue ref_node, List<Residue> nodes,
			int space) throws Exception {
		alignTopsOnRight(getAll(ref_node), getCurrent(ref_node), nodes, space);
	}

	public void alignTopsOnRight(Rectangle ref_bbox, List<Residue> nodes,
			int space) throws Exception {
		alignTopsOnRight(singleton(ref_bbox), ref_bbox, nodes, space);
	}

	public void alignTopsOnRight(Residue ref_node, Residue node, int space)
			throws Exception {
		alignTopsOnRight(getAll(ref_node), getCurrent(ref_node),
				singleton(node), space);
	}

	public void alignTopsOnRight(Rectangle ref_bbox, Residue node, int space)
			throws Exception {
		alignTopsOnRight(singleton(ref_bbox), ref_bbox, singleton(node), space);
	}

	public void alignTopsOnRight(Rectangle ref_bbox, List<Residue> ref_nodes,
			List<Residue> sup_nodes, List<Residue> nodes, int space)
			throws Exception {
		alignTopsOnRight(new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrent(sup_nodes), nodes, space);
	}

	public void alignTopsOnRight(List<Residue> ref_sup_nodes,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignTopsOnRight(
				new Union<Rectangle>(getAll(ref_sup_nodes))
						.and(getAll(ref_nodes)),
				getCurrent(ref_sup_nodes), getAll(nodes),
				getCurrent(sup_nodes), nodes, space);
	}

	private void alignTopsOnRight(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		alignTopsOnRight(ref_all_bboxes, ref_sup_bbox, getAll(nodes),
				getCurrent(nodes), nodes, space);
	}

	private void alignTopsOnRight(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, space);
			translate(0, top(ref_sup_bbox) - top(cur_sup_bbox), nodes); // align
																		// top
			translate(
					shiftToRight(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), 0, nodes); // put on right
		}
	}

	public void alignBottomsOnLeft(List<Residue> ref_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignBottomsOnLeft(getAll(ref_nodes), getCurrent(ref_nodes), nodes,
				space);
	}

	public void alignBottomsOnLeft(Residue ref_node, List<Residue> nodes,
			int space) throws Exception {
		alignBottomsOnLeft(getAll(ref_node), getCurrent(ref_node), nodes, space);
	}

	public void alignBottomsOnLeft(Rectangle ref_bbox, List<Residue> nodes,
			int space) throws Exception {
		alignBottomsOnLeft(singleton(ref_bbox), ref_bbox, nodes, space);
	}

	public void alignBottomsOnLeft(Residue ref_node, Residue node, int space)
			throws Exception {
		alignBottomsOnLeft(getAll(ref_node), getCurrent(ref_node),
				singleton(node), space);
	}

	public void alignBottomsOnLeft(Rectangle ref_bbox, Residue node, int space)
			throws Exception {
		alignBottomsOnLeft(singleton(ref_bbox), ref_bbox, singleton(node),
				space);
	}

	public void alignBottomsOnLeft(Rectangle ref_bbox, List<Residue> ref_nodes,
			List<Residue> sup_nodes, List<Residue> nodes, int space)
			throws Exception {
		alignBottomsOnLeft(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrent(sup_nodes), nodes, space);
	}

	public void alignBottomsOnLeft(List<Residue> ref_sup_nodes,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignBottomsOnLeft(
				new Union<Rectangle>(getAll(ref_sup_nodes))
						.and(getAll(ref_nodes)),
				getCurrent(ref_sup_nodes), getAll(nodes),
				getCurrent(sup_nodes), nodes, space);
	}

	private void alignBottomsOnLeft(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		alignBottomsOnLeft(ref_all_bboxes, ref_sup_bbox, getAll(nodes),
				getCurrent(nodes), nodes, space);
	}

	private void alignBottomsOnLeft(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, space);
			translate(0, bottom(ref_sup_bbox) - bottom(cur_sup_bbox), nodes); // align
																				// bottom
			translate(
					shiftToLeft(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), 0, nodes); // put on left
		}
	}

	public void alignBottomsOnRight(List<Residue> ref_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignBottomsOnRight(getAll(ref_nodes), getCurrent(ref_nodes), nodes,
				space);
	}

	public void alignBottomsOnRight(Residue ref_node, List<Residue> nodes,
			int space) throws Exception {
		alignBottomsOnRight(getAll(ref_node), getCurrent(ref_node), nodes,
				space);
	}

	public void alignBottomsOnRight(Rectangle ref_bbox, List<Residue> nodes,
			int space) throws Exception {
		alignBottomsOnRight(singleton(ref_bbox), ref_bbox, nodes, space);
	}

	public void alignBottomsOnRight(Residue ref_node, Residue node, int space)
			throws Exception {
		alignBottomsOnRight(getAll(ref_node), getCurrent(ref_node),
				singleton(node), space);
	}

	public void alignBottomsOnRight(Rectangle ref_bbox, Residue node, int space)
			throws Exception {
		alignBottomsOnRight(singleton(ref_bbox), ref_bbox, singleton(node),
				space);
	}

	public void alignBottomsOnRight(Rectangle ref_bbox,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignBottomsOnRight(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrent(sup_nodes), nodes, space);
	}

	public void alignBottomsOnRight(List<Residue> ref_sup_nodes,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignBottomsOnRight(
				new Union<Rectangle>(getAll(ref_sup_nodes))
						.and(getAll(ref_nodes)),
				getCurrent(ref_sup_nodes), getAll(nodes),
				getCurrent(sup_nodes), nodes, space);
	}

	private void alignBottomsOnRight(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		alignBottomsOnRight(ref_all_bboxes, ref_sup_bbox, getAll(nodes),
				getCurrent(nodes), nodes, space);
	}

	private void alignBottomsOnRight(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, space);
			translate(0, bottom(ref_sup_bbox) - bottom(cur_sup_bbox), nodes); // align
																				// bottom
			translate(
					shiftToRight(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), 0, nodes); // put on right
		}
	}

	public void alignCentersOnLeft(List<Residue> ref_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignCentersOnLeft(getAll(ref_nodes), getCurrent(ref_nodes), nodes,
				space);
	}

	public void alignCentersOnLeft(Residue ref_node, List<Residue> nodes,
			int space) throws Exception {
		alignCentersOnLeft(getAll(ref_node), getCurrent(ref_node), nodes, space);
	}

	public void alignCentersOnLeft(Rectangle ref_bbox, List<Residue> nodes,
			int space) throws Exception {
		alignCentersOnLeft(singleton(ref_bbox), ref_bbox, nodes, space);
	}

	public void alignCentersOnLeft(Residue ref_node, Residue node, int space)
			throws Exception {
		alignCentersOnLeft(getAll(ref_node), getCurrent(ref_node),
				singleton(node), space);
	}

	public void alignCentersOnLeft(Rectangle ref_bbox, Residue node, int space)
			throws Exception {
		alignCentersOnLeft(singleton(ref_bbox), ref_bbox, singleton(node),
				space);
	}

	public void alignCentersOnLeft(Rectangle ref_bbox, List<Residue> ref_nodes,
			List<Residue> sup_nodes, List<Residue> nodes, int space)
			throws Exception {
		alignCentersOnLeft(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrent(sup_nodes), nodes, space);
	}

	public void alignCentersOnLeft(List<Residue> ref_sup_nodes,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignCentersOnLeft(
				new Union<Rectangle>(getAll(ref_sup_nodes))
						.and(getAll(ref_nodes)),
				getCurrent(ref_sup_nodes), getAll(nodes),
				getCurrent(sup_nodes), nodes, space);
	}

	private void alignCentersOnLeft(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		alignCentersOnLeft(ref_all_bboxes, ref_sup_bbox, getAll(nodes),
				getCurrent(nodes), nodes, space);
	}

	private void alignCentersOnLeft(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, space);
			translate(0, midy(ref_sup_bbox) - midy(cur_sup_bbox), nodes); // align
																			// center
			translate(
					shiftToLeft(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), 0, nodes); // put on left
		}
	}

	public void alignCentersOnRight(List<Residue> ref_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignCentersOnRight(getAll(ref_nodes), getCurrent(ref_nodes), nodes,
				space);
	}

	public void alignCentersOnRight(Residue ref_node, List<Residue> nodes,
			int space) throws Exception {
		alignCentersOnRight(getAll(ref_node), getCurrent(ref_node), nodes,
				space);
	}

	public void alignCentersOnRight(Rectangle ref_bbox, List<Residue> nodes,
			int space) throws Exception {
		alignCentersOnRight(singleton(ref_bbox), ref_bbox, nodes, space);
	}

	public void alignCentersOnRight(Residue ref_node, Residue node, int space)
			throws Exception {
		alignCentersOnRight(getAll(ref_node), getCurrent(ref_node),
				singleton(node), space);
	}

	public void alignCentersOnRight(Rectangle ref_bbox, Residue node, int space)
			throws Exception {
		alignCentersOnRight(singleton(ref_bbox), ref_bbox, singleton(node),
				space);
	}

	public void alignCentersOnRight(Rectangle ref_bbox,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignCentersOnRight(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrent(sup_nodes), nodes, space);
	}

	public void alignCentersOnRight(List<Residue> ref_sup_nodes,
			List<Residue> ref_nodes, List<Residue> sup_nodes,
			List<Residue> nodes, int space) throws Exception {
		alignCentersOnRight(
				new Union<Rectangle>(getAll(ref_sup_nodes))
						.and(getAll(ref_nodes)),
				getCurrent(ref_sup_nodes), getAll(nodes),
				getCurrent(sup_nodes), nodes, space);
	}

	private void alignCentersOnRight(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		alignCentersOnRight(ref_all_bboxes, ref_sup_bbox, getAll(nodes),
				getCurrent(nodes), nodes, space);
	}

	private void alignCentersOnRight(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int space)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, space);
			translate(0, midy(ref_sup_bbox) - midy(cur_sup_bbox), nodes); // align
																			// center
			translate(
					shiftToRight(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), 0, nodes); // put on right
		}
	}

	public void alignCornersOnLeftAtTop(Rectangle ref_bbox,
			List<Residue> ref_nodes, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		alignCornersOnLeftAtTop(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrentComplete(nodes), nodes,
				xspace, yspace);
	}

	private void alignCornersOnLeftAtTop(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, xspace);
			translate(0, top(ref_sup_bbox) - bottom(cur_sup_bbox) - yspace,
					nodes); // align corner
			translate(
					shiftToLeft(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, xspace), 0, nodes); // put on left
		}
	}

	public void alignCornersOnLeftAtBottom(Rectangle ref_bbox,
			List<Residue> ref_nodes, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		alignCornersOnLeftAtBottom(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrentComplete(nodes), nodes,
				xspace, yspace);
	}

	private void alignCornersOnLeftAtBottom(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, xspace);
			translate(0, bottom(ref_sup_bbox) - top(cur_sup_bbox) + yspace,
					nodes); // align corner
			translate(
					shiftToLeft(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, xspace), 0, nodes); // put on left
		}
	}

	public void alignCornersOnRightAtTop(Rectangle ref_bbox,
			List<Residue> ref_nodes, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		alignCornersOnRightAtTop(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrentComplete(nodes), nodes,
				xspace, yspace);
	}

	private void alignCornersOnRightAtTop(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, xspace);
			translate(0, top(ref_sup_bbox) - bottom(cur_sup_bbox) - yspace,
					nodes); // align corner
			translate(
					shiftToRight(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, xspace), 0, nodes); // put on right
		}
	}

	public void alignCornersOnRightAtBottom(Rectangle ref_bbox,
			List<Residue> ref_nodes, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		alignCornersOnRightAtBottom(
				new Union<Rectangle>(ref_bbox).and(getAll(ref_nodes)),
				ref_bbox, getAll(nodes), getCurrentComplete(nodes), nodes,
				xspace, yspace);
	}

	private void alignCornersOnRightAtBottom(List<Rectangle> ref_all_bboxes,
			Rectangle ref_sup_bbox, List<Rectangle> cur_all_bboxes,
			Rectangle cur_sup_bbox, List<Residue> nodes, int xspace, int yspace)
			throws Exception {
		if (nodes != null && nodes.size() > 0) {
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
					cur_sup_bbox, nodes, xspace);
			translate(0, bottom(ref_sup_bbox) - top(cur_sup_bbox) + yspace,
					nodes); // align corner
			translate(
					shiftToRight(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, xspace), 0, nodes); // put on right
		}
	}

	private int shiftToLeft(List<Rectangle> ref_bboxes, Rectangle ref_bbox,
			List<Rectangle> cur_bboxes, Rectangle cur_bbox, int space)
			throws Exception {
		return shiftToLeft(ref_bboxes, ref_bbox, cur_bboxes, cur_bbox, space, 0);
	}

	private int shiftToLeft(List<Rectangle> ref_bboxes, Rectangle ref_bbox,
			List<Rectangle> cur_bboxes, Rectangle cur_bbox, int space,
			int min_ref_space) throws Exception {
		int toll = theGraphicOptions.NODE_SPACE / 2 - 1;
		int shift = left(ref_bbox) - Math.max(space, min_ref_space)
				- right(cur_bbox);
		for (Rectangle cur : cur_bboxes)
			for (Rectangle ref : ref_bboxes)
				if (overlapy(cur, ref, toll))
					shift = Math.min(shift, left(ref) - space - right(cur));
		return shift;
	}

	private int shiftToRight(List<Rectangle> ref_bboxes, Rectangle ref_bbox,
			List<Rectangle> cur_bboxes, Rectangle cur_bbox, int space)
			throws Exception {
		return shiftToRight(ref_bboxes, ref_bbox, cur_bboxes, cur_bbox, space,
				0);
	}

	private int shiftToRight(List<Rectangle> ref_bboxes, Rectangle ref_bbox,
			List<Rectangle> cur_bboxes, Rectangle cur_bbox, int space,
			int min_ref_space) throws Exception {
		int toll = theGraphicOptions.NODE_SPACE / 2 - 1;
		int shift = right(ref_bbox) + Math.max(space, min_ref_space)
				- left(cur_bbox);
		for (Rectangle cur : cur_bboxes)
			for (Rectangle ref : ref_bboxes)
				if (overlapy(cur, ref, toll))
					shift = Math.max(shift, right(ref) + space - left(cur));
		return shift;
	}

	// SYMMETRIC

	public void alignSymmetricOnLeft(Rectangle ref_sup_bbox,
			List<Residue> ref_nodes, List<Residue> nodes_t,
			List<Residue> nodes_b, int space, int min_ref_space)
			throws Exception {
		if (nodes_t != null && nodes_t.size() > 0 && nodes_b != null
				&& nodes_b.size() > 0) {
			// get bboxes
			List<Rectangle> ref_all_bboxes = new Union<Rectangle>(ref_sup_bbox)
					.and(getAll(ref_nodes));
			Rectangle cur_sup_bbox_t = getCurrentComplete(nodes_t);
			List<Rectangle> cur_all_bboxes_t = getAll(nodes_t);
			Rectangle cur_sup_bbox_b = getCurrentComplete(nodes_b);
			List<Rectangle> cur_all_bboxes_b = getAll(nodes_b);

			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes_t,
					cur_sup_bbox_t, nodes_t, space);
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes_b,
					cur_sup_bbox_b, nodes_b, space);

			// align cur_t with cur_b
			translate(right(cur_sup_bbox_t) - right(cur_sup_bbox_b), 0, nodes_b); // align
																					// rights
			translate(
					0,
					shiftToBottom(cur_all_bboxes_t, cur_sup_bbox_t,
							cur_all_bboxes_b, cur_sup_bbox_b, space,
							min_ref_space), nodes_b); // put on bottom

			// reset bboxes
			Union<Residue> nodes = new Union<Residue>(nodes_t).and(nodes_b);
			cur_sup_bbox_t = getCurrentComplete(nodes_t);
			cur_sup_bbox_b = getCurrentComplete(nodes_b);
			Rectangle cur_sup_bbox = union(cur_sup_bbox_t, cur_sup_bbox_b);
			List<Rectangle> cur_all_bboxes = getAll(nodes);

			// align cur_t and cur_b with ref
			translate(0, midy(ref_sup_bbox)
					- (bottom(cur_sup_bbox_t) + top(cur_sup_bbox_b)) / 2, nodes); // align
																					// symmetric
			translate(
					shiftToLeft(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), 0, nodes); // put on left
		}
	}

	public void alignSymmetricOnRight(Rectangle ref_sup_bbox,
			List<Residue> ref_nodes, List<Residue> nodes_t,
			List<Residue> nodes_b, int space, int min_ref_space)
			throws Exception {
		if (nodes_t != null && nodes_t.size() > 0 && nodes_b != null
				&& nodes_b.size() > 0) {
			// get bboxes
			List<Rectangle> ref_all_bboxes = new Union<Rectangle>(ref_sup_bbox)
					.and(getAll(ref_nodes));
			Rectangle cur_sup_bbox_t = getCurrentComplete(nodes_t);
			List<Rectangle> cur_all_bboxes_t = getAll(nodes_t);
			Rectangle cur_sup_bbox_b = getCurrentComplete(nodes_b);
			List<Rectangle> cur_all_bboxes_b = getAll(nodes_b);

			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes_t,
					cur_sup_bbox_t, nodes_t, space);
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes_b,
					cur_sup_bbox_b, nodes_b, space);

			// align cur_t with cur_b
			translate(left(cur_sup_bbox_t) - left(cur_sup_bbox_b), 0, nodes_b); // align
																				// lefts
			translate(
					0,
					shiftToBottom(cur_all_bboxes_t, cur_sup_bbox_t,
							cur_all_bboxes_b, cur_sup_bbox_b, space,
							min_ref_space), nodes_b); // put on bottom

			// reset bboxes
			Union<Residue> nodes = new Union<Residue>(nodes_t).and(nodes_b);
			cur_sup_bbox_t = getCurrentComplete(nodes_t);
			cur_sup_bbox_b = getCurrentComplete(nodes_b);
			Rectangle cur_sup_bbox = union(cur_sup_bbox_t, cur_sup_bbox_b);
			List<Rectangle> cur_all_bboxes = getAll(nodes);

			// align cur_t and cur_b with ref
			translate(0, midy(ref_sup_bbox)
					- (bottom(cur_sup_bbox_t) + top(cur_sup_bbox_b)) / 2, nodes); // align
																					// symmetric
			translate(
					shiftToRight(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), 0, nodes); // put on right
		}
	}

	public void alignSymmetricOnBottom(Rectangle ref_sup_bbox,
			List<Residue> ref_nodes, List<Residue> nodes_l,
			List<Residue> nodes_r, int space, int min_ref_space)
			throws Exception {
		if (nodes_l != null && nodes_l.size() > 0 && nodes_r != null
				&& nodes_r.size() > 0) {
			// get bboxes
			List<Rectangle> ref_all_bboxes = new Union<Rectangle>(ref_sup_bbox)
					.and(getAll(ref_nodes));
			Rectangle cur_sup_bbox_l = getCurrentComplete(nodes_l);
			List<Rectangle> cur_all_bboxes_l = getAll(nodes_l);
			Rectangle cur_sup_bbox_r = getCurrentComplete(nodes_r);
			List<Rectangle> cur_all_bboxes_r = getAll(nodes_r);

			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes_l,
					cur_sup_bbox_l, nodes_l, space);
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes_r,
					cur_sup_bbox_r, nodes_r, space);

			// align cur_l with cur_r
			translate(top(cur_sup_bbox_l) - top(cur_sup_bbox_r), 0, nodes_r); // align
																				// tops
			translate(
					shiftToRight(cur_all_bboxes_l, cur_sup_bbox_l,
							cur_all_bboxes_r, cur_sup_bbox_r, space,
							min_ref_space), 0, nodes_r); // put on right

			// reset bboxes
			Union<Residue> nodes = new Union<Residue>(nodes_l).and(nodes_r);
			cur_sup_bbox_l = getCurrentComplete(nodes_l);
			cur_sup_bbox_r = getCurrentComplete(nodes_r);
			Rectangle cur_sup_bbox = union(cur_sup_bbox_l, cur_sup_bbox_r);
			List<Rectangle> cur_all_bboxes = getAll(nodes);

			// align cur_l and cur_r with ref
			translate(midx(ref_sup_bbox)
					- (right(cur_sup_bbox_l) + left(cur_sup_bbox_r)) / 2, 0,
					nodes); // align symmetric
			translate(
					0,
					shiftToBottom(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), nodes); // put on bottom
		}
	}

	public void alignSymmetricOnTop(Rectangle ref_sup_bbox,
			List<Residue> ref_nodes, List<Residue> nodes_l,
			List<Residue> nodes_r, int space, int min_ref_space)
			throws Exception {
		if (nodes_l != null && nodes_l.size() > 0 && nodes_r != null
				&& nodes_r.size() > 0) {
			// get bboxes
			List<Rectangle> ref_all_bboxes = new Union<Rectangle>(ref_sup_bbox)
					.and(getAll(ref_nodes));
			Rectangle cur_sup_bbox_l = getCurrentComplete(nodes_l);
			List<Rectangle> cur_all_bboxes_l = getAll(nodes_l);
			Rectangle cur_sup_bbox_r = getCurrentComplete(nodes_r);
			List<Rectangle> cur_all_bboxes_r = getAll(nodes_r);

			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes_l,
					cur_sup_bbox_l, nodes_l, space);
			assertAlignment(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes_r,
					cur_sup_bbox_r, nodes_r, space);

			// align cur_l with cur_r
			translate(bottom(cur_sup_bbox_l) - bottom(cur_sup_bbox_r), 0,
					nodes_r); // align bottoms
			translate(
					shiftToRight(cur_all_bboxes_l, cur_sup_bbox_l,
							cur_all_bboxes_r, cur_sup_bbox_r, space,
							min_ref_space), 0, nodes_r); // put on right

			// reset bboxes
			Union<Residue> nodes = new Union<Residue>(nodes_l).and(nodes_r);
			cur_sup_bbox_l = getCurrentComplete(nodes_l);
			cur_sup_bbox_r = getCurrentComplete(nodes_r);
			Rectangle cur_sup_bbox = union(cur_sup_bbox_l, cur_sup_bbox_r);
			List<Rectangle> cur_all_bboxes = getAll(nodes);

			// align cur_l and cur_r with ref
			translate(midx(ref_sup_bbox)
					- (right(cur_sup_bbox_l) + left(cur_sup_bbox_r)) / 2, 0,
					nodes); // align symmetric
			translate(
					0,
					shiftToTop(ref_all_bboxes, ref_sup_bbox, cur_all_bboxes,
							cur_sup_bbox, space), nodes); // put on top
		}
	}

}
