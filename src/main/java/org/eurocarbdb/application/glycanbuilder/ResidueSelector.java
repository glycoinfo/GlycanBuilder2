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
/**
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

package org.eurocarbdb.application.glycanbuilder;

import java.util.*;
import java.text.*;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.image.*;
import java.awt.print.*;

import javax.swing.*;

import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.linkage.Union;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.Geometry;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRenderer;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.PositionManager;
import org.eurocarbdb.application.glycanbuilder.util.MouseUtils;

class ResidueSelector extends JComponent implements  MouseListener, MouseMotionListener {

	// Classes

	public interface SelectionChangeListener {    
		public void selectionChanged(SelectionChangeEvent e);    
	}

	public static class SelectionChangeEvent {
		private ResidueSelector src;

		public SelectionChangeEvent(ResidueSelector _src) {
			src = _src;
		}

		public ResidueSelector getSource() {
			return src;
		}       
	}

	// -----------

	protected static final long serialVersionUID = 0L;    

	// singletons
	protected JScrollPane theScrollPane = null;

	// document
	protected Glycan theStructure = null;
	protected Collection<Residue> active_residues = null;

	// selection
	protected boolean allow_multiple_selection;
	protected Residue current_residue;
	protected HashSet<Residue> selected_residues; 

	// painting
	protected GlycanRenderer  theGlycanRenderer;
	protected Rectangle       structure_bbox;
	protected BBoxManager     theBBoxManager;
	protected PositionManager thePosManager;

	// events

	protected Point mouse_start_point = null;
	protected Point mouse_end_point = null; 

	// 
	protected LinkedList<SelectionChangeListener> listeners;

	//---------
	// construction    

	public ResidueSelector(Glycan structure, Collection<Residue> actives, boolean multiple_sel) {
		// init
		theStructure = structure;
		active_residues = actives;
		allow_multiple_selection = multiple_sel;

		//
		current_residue = null;
		selected_residues = new HashSet<Residue>();

		theGlycanRenderer = new GlycanRendererAWT();
		thePosManager  = new PositionManager();
		theBBoxManager = new BBoxManager();
		structure_bbox = null;

		listeners = new LinkedList<SelectionChangeListener>();

		// set the canvas 
		this.setOpaque(true);
		this.setBackground(Color.white);

		// add mouse events
		addMouseMotionListener( this );
		addMouseListener( this ); 
	}    

	public GlycanRenderer getGlycanRenderer() {
		return theGlycanRenderer;
	}

	public void setGlycanRenderer(GlycanRenderer r) {
		theGlycanRenderer = r;
	}

	public void setScrollPane(JScrollPane sp) {
		theScrollPane = sp;
	}



	//-------------------
	// JComponent


	public Dimension getPreferredSize() {
		if( structure_bbox==null )
			structure_bbox = theGlycanRenderer.computeBoundingBoxes(new Union<Glycan>(theStructure),false,true,thePosManager,theBBoxManager);    
		return theGlycanRenderer.computeSize(structure_bbox);
	}            

	public Dimension getMinimumSize() {
		return new Dimension(0,0);
	}

	//-------------------    
	// painting


	protected void paintComponent(Graphics g) {
		if (isOpaque()) { //paint background
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}

		// prepare graphic object
		Graphics2D g2d = (Graphics2D)g.create();        
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// set clipping area
		Rectangle clipRect = new Rectangle();
		g.getClipBounds(clipRect);

		if( mouse_start_point!=null && mouse_end_point!=null ) {
			g2d.setColor(Color.black);
			g2d.draw(Geometry.makeRectangle(mouse_start_point,mouse_end_point));
		}    

		// draw        
		structure_bbox = theGlycanRenderer.computeBoundingBoxes(new Union<Glycan>(theStructure),false,true,thePosManager,theBBoxManager);    
		theGlycanRenderer.paint(new DefaultPaintable(g2d),theStructure,selected_residues,null,active_residues,false,true,thePosManager,theBBoxManager);
		revalidate();

		// dispose graphic object
		g2d.dispose();    
	}   


	//---------------
	// selection

	public Residue getResidueAtPoint(Point p) {
		Residue ret = getResidueAtPoint(theStructure.getRoot(),p);
		if( ret!=null )
			return ret;

		ret = getResidueAtPoint(theStructure.getBracket(),p);
		if( ret!=null )
			return ret;    
		return null;
	}

	public Residue getResidueAtPoint(Residue r, Point p) {
		if( r==null )
			return null;

		Rectangle cur_bbox = theBBoxManager.getCurrent(r);
		if( cur_bbox==null )
			return null;

		if( cur_bbox.contains(p) )
			return r;

		for( Linkage l : r.getChildrenLinkages() ) {
			Residue ret = getResidueAtPoint(l.getChildResidue(),p);
			if( ret!=null )
				return ret;        
		}
		return null;
	}   

	public boolean hasSelection() {
		return (selected_residues.size()>0);
	}

	public void resetSelection() {    
		selected_residues.clear();
		current_residue = null;

		fireUpdatedSelection();
	}

	public boolean hasCurrentSelection() {
		return (current_residue!=null );
	}

	public Residue getCurrentSelection() {
		return current_residue;
	}     

	public boolean hasCurrentResidue() {
		return (current_residue!=null);
	}

	public Residue getCurrentResidue() {
		return current_residue;
	}

	private void setCurrentResidue(Residue node) {
		if( active_residues!=null && !active_residues.contains(node)) 
			node = null;

		if( node!=null )
			selected_residues.add(node);
		current_residue = node;

		fireUpdatedSelection();
	}

	public boolean isSelected(Residue node) {
		if( node==null )
			return false;
		return selected_residues.contains(node);
	}

	public boolean hasSelectedResidues() {
		return !selected_residues.isEmpty();
	}   

	public Collection<Residue> getSelectedResiduesList() {
		return selected_residues;
	}

	public Residue[] getSelectedResidues() {
		return (Residue[])selected_residues.toArray(new Residue[0]);
	}

	public void setSelection(Collection<Residue> nodes) {    
		if( active_residues!=null )
			nodes = new Union<Residue>(active_residues).intersect(nodes);

		if( nodes==null || nodes.isEmpty() ) {
			resetSelection();
		}
		else if( nodes.size()==1 ) {
			selected_residues.clear();

			current_residue = nodes.iterator().next();
			selected_residues.add(current_residue);

			fireUpdatedSelection();
		}
		else if( allow_multiple_selection ) {
			selected_residues.clear();
			current_residue = null;

			for(Iterator<Residue> i=nodes.iterator(); i.hasNext(); ) 
				selected_residues.add(i.next());

			fireUpdatedSelection();            
		}
	}

	public void setSelection(Residue node) {
		if( active_residues!=null && !active_residues.contains(node)) 
			node = null;

		if( node==null ) 
			resetSelection();    
		else {
			selected_residues.clear();
			selected_residues.add(node);
			current_residue = node;

			fireUpdatedSelection();        
		}
	}

	public void enforceSelection(Point p) {
		Residue r = getResidueAtPoint(p);
		if( r!=null ) 
			enforceSelection(r);
	}

	public void enforceSelection(Residue node) {
		if( active_residues!=null && !active_residues.contains(node)) 
			node = null;

		if( isSelected(node) ) {
			current_residue = node;
			fireUpdatedSelection();
		}
		else
			setSelection(node);
	}         

	public boolean enforceSelection() {
		setSelection(theStructure.getRoot());
		return true;
	}

	public void addSelection(Collection<Residue> nodes) {
		if( active_residues!=null )
			nodes = new Union<Residue>(active_residues).intersect(nodes);

		if( nodes!=null && nodes.size()>0 ) {
			if( allow_multiple_selection ) {
				for(Residue node : nodes) 
					selected_residues.add(node);
				current_residue = null;

				fireUpdatedSelection();
			}
			else if( nodes.size()==1 ) 
				setSelection(nodes.iterator().next());        
		}
	}

	public void addSelection(Residue node) {
		if( active_residues!=null && !active_residues.contains(node)) 
			node = null;

		if( node!=null ) {
			if( allow_multiple_selection ) {
				selected_residues.add(node);
				current_residue = node;

				fireUpdatedSelection();
			}
			else
				setSelection(node);
		}
	}

	public void addSelectionPathTo(Residue node) {
		if( active_residues!=null && !active_residues.contains(node)) 
			node = null;

		if( node!=null ) {
			if( allow_multiple_selection ) {
				if( current_residue==null ) 
					selected_residues.add(node);        
				else 
					selected_residues.addAll(Glycan.getPath(current_residue,node));           
				current_residue = node;

				fireUpdatedSelection();
			}
			else
				setSelection(node);
		}
	}   

	private Residue findNearest(Point p, Collection<Residue> nodes) {
		if( p==null )
			return null;

		Residue best_node = null;
		double  best_dist = 0.;
		for( Residue cur_node : nodes ) {
			Rectangle cur_rect = theBBoxManager.getCurrent(cur_node);
			double cur_dist = Geometry.distance(p,cur_rect);
			if( best_node==null || best_dist>cur_dist ) {
				best_node = cur_node;
				best_dist = cur_dist;
			}
		}

		return best_node;
	}


	//---------------
	// structure navigation

	public void goToStart() {       
		if( theScrollPane!=null ) {
			JViewport vp = theScrollPane.getViewport();
			vp.setViewPosition(new Point(0,0));        
			vp.setViewPosition(new Point(0,0)); 
		}
	}

	public void goToEnd() {       
		if( theScrollPane!=null ) {
			JViewport vp = theScrollPane.getViewport();
			Dimension all = getPreferredSize();
			Dimension view = vp.getExtentSize();

			vp.setViewPosition(new Point(0,all.height-view.height));        
			vp.setViewPosition(new Point(0,all.height-view.height));        
		}
	}

	public void onNavigateUp() {
		Residue current = getCurrentSelection();
		if( current==null ) setSelection(theStructure.getRoot());    
		else {
			Residue best_node = theBBoxManager.getNearestUp(current);
			if( best_node!=null ) setSelection(best_node);
		}
	}

	public void onNavigateDown() {
		Residue current = getCurrentSelection();
		if( current==null ) 
			setSelection(theStructure.getRoot());    
		else {
			Residue best_node = theBBoxManager.getNearestDown(current);        
			if( best_node!=null ) setSelection(best_node);       
		}    
	}

	public void onNavigateLeft() {
		Residue current = getCurrentSelection();
		if( current==null ) 
			setSelection(theStructure.getRoot());    
		else {
			Residue best_node = theBBoxManager.getNearestLeft(current);     
			if( best_node!=null ) setSelection(best_node);
		}
	}

	public void onNavigateRight() {    
		Residue current = getCurrentSelection();
		if( current==null ) 
			setSelection(theStructure.getRoot());    
		else {
			Residue best_node = theBBoxManager.getNearestRight(current);  
			if( best_node!=null ) setSelection(best_node);
		}
	}


	//---------------
	// events  

	public void addSelectionChangeListener(SelectionChangeListener l) {
		if( l!=null ) listeners.add(l);
	}

	public void removeSelectionChangeListener(SelectionChangeListener l) {
		if( l!=null ) listeners.remove(l);
	}

	public void fireUpdatedSelection() {
		for( Iterator<SelectionChangeListener> i=listeners.iterator(); i.hasNext(); ) 
			i.next().selectionChanged(new SelectionChangeEvent(this));
		repaint();
		showSelection();
	}

	public void showSelection() {

		if( theScrollPane==null ) 
			return;

		// update bounding boxes
		theGlycanRenderer.computeBoundingBoxes(new Union<Glycan>(theStructure),false,true,thePosManager,theBBoxManager);

		//

		Rectangle bbox = null;
		for( Residue r : selected_residues )
			bbox = Geometry.union(bbox,theBBoxManager.getCurrent(r));    

		if( bbox!=null ) {
			bbox = Geometry.expand(bbox,5);

			// show bbox in viewport
			Rectangle view = theScrollPane.getViewport().getViewRect();
			int new_x = Geometry.left(view);
			int new_y = Geometry.top(view);
			if( Geometry.left(view)>Geometry.left(bbox) ) 
				new_x = Geometry.left(bbox);
			else if( Geometry.right(view)<Geometry.right(bbox) ) {
				int min_move = Geometry.right(bbox)-Geometry.right(view);
				int max_move = Geometry.left(bbox)-Geometry.left(view);
				new_x += Math.min(min_move,max_move);
			}
			if( Geometry.top(view)>Geometry.top(bbox) ) 
				new_y = Geometry.top(bbox);
			else if( Geometry.bottom(view)<Geometry.bottom(bbox) ) {
				int min_move = Geometry.bottom(bbox)-Geometry.bottom(view);
				int max_move = Geometry.top(bbox)-Geometry.top(view);
				new_y += Math.min(min_move,max_move);
			}
			theScrollPane.getViewport().setViewPosition(new Point(new_x,new_y));        
			theScrollPane.getViewport().setViewPosition(new Point(new_x,new_y));    
		}
	}

	//---------------
	// mouse handling 

	public void mouseEntered(MouseEvent e) {    
	}

	public void mouseExited(MouseEvent e) {    
	}

	public void mousePressed(MouseEvent e) {
		if( MouseUtils.isPushTrigger(e) || MouseUtils.isCtrlPushTrigger(e) ) 
			mouse_start_point = e.getPoint();
	}        

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {

		if( mouse_start_point!=null ) {
			mouse_end_point = e.getPoint();                        
			repaint();            
			dragAndScroll(e);    
		}
		repaint();
	}        

	public void mouseReleased(MouseEvent e) {

		if( mouse_end_point!=null ) {
			Rectangle mouse_rect = Geometry.makeRectangle(mouse_start_point,mouse_end_point);
			if( MouseUtils.isNothingPressed(e) ) 
				setSelection(theBBoxManager.getNodesInside(mouse_rect));                
			else if( MouseUtils.isCtrlPressed(e) ) 
				addSelection(theBBoxManager.getNodesInside(mouse_rect));        
			setCurrentResidue(findNearest(e.getPoint(),selected_residues));
		}

		// reset
		repaint();

		mouse_start_point = null;
		mouse_end_point = null;
		repaint();
	}

	public void mouseClicked(MouseEvent e) {      
		Residue r = getResidueAtPoint(e.getPoint());
		if( r!=null ) {
			if( MouseUtils.isSelectTrigger(e) )  setSelection(r);
			else if( MouseUtils.isAddSelectTrigger(e) ) addSelection(r);
			else if( MouseUtils.isSelectAllTrigger(e) ) addSelectionPathTo(r);    
		}    
		else if( MouseUtils.isSelectTrigger(e) || MouseUtils.isAddSelectTrigger(e) || MouseUtils.isSelectAllTrigger(e) )
			resetSelection();
	}



	private void dragAndScroll(MouseEvent e) {
		if( theScrollPane==null ) return;

		// move view if near borders                
		Point point = e.getPoint();
		JViewport view = theScrollPane.getViewport();
		Rectangle inner = view.getViewRect();
		inner.grow(-10,-10);

		if( !inner.contains(point) ) {
			Point orig = view.getViewPosition(); 
			if( point.x<inner.x ) orig.x -= 10;
			else if( point.x>(inner.x+inner.width) ) orig.x += 10;
			if( point.y<inner.y ) orig.y -= 10;
			else if( point.y>(inner.y+inner.height) ) orig.y += 10;

			int maxx = getBounds().width-view.getViewRect().width;
			int maxy = getBounds().height-view.getViewRect().height;
			if( orig.x<0 ) orig.x = 0;
			if( orig.x>maxx ) orig.x = maxx;
			if( orig.y<0 ) orig.y = 0;
			if( orig.y>maxy ) orig.y = maxy;

			view.setViewPosition(orig);                    
		}    
	}

}


