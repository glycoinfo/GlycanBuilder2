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

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;

/**
   Objects of this class are used to compute and store the position of
   a residue around its parent during the rendering process.

   @see GlycanRendererAWT
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class PositionManager {

	protected static final ResAngle[] static_positions = new ResAngle[5];
	static {
		try {
			static_positions[0] = new ResAngle(-90);
			static_positions[1] = new ResAngle(-45);
			static_positions[2] = new ResAngle(0);
			static_positions[3] = new ResAngle(45);
			static_positions[4] = new ResAngle(90);
			//static_positions[5] = new ResAngle(-135);
			//static_positions[6] = new ResAngle(135);
		}
		catch(Exception e) {
			LogUtils.report(e);
		}
	}

	protected HashMap<Residue,ResAngle> orientations;
	protected HashMap<Residue,ResAngle> rotations;
	protected HashMap<Residue,ResAngle> relative_positions;
	protected HashMap<Residue,ResAngle> absolute_positions;
	protected HashMap<Residue,Boolean>  onborder_flags;
	protected HashMap<Residue,Boolean>  sticky_flags;

	/**
       Default constructor.
	 */
	public PositionManager() {
		orientations = new HashMap<Residue,ResAngle>();
		rotations    = new HashMap<Residue,ResAngle>();
		relative_positions = new HashMap<Residue,ResAngle>();
		absolute_positions = new HashMap<Residue,ResAngle>();
		onborder_flags     = new HashMap<Residue,Boolean>();
		sticky_flags     = new HashMap<Residue,Boolean>();
	}

	/**
       Clear all fields.
	 */
	public void reset() {
		orientations.clear();
		rotations.clear();
		relative_positions.clear();
		absolute_positions.clear();
		onborder_flags.clear();
		sticky_flags.clear();
	}


	/**
       Store the information about the position of a residue around
       its parent.
       @param node the residue
       @param parent_orientation the absolute orientation of the parent
       @param relative_position the position of the residue relative
       to its parent
       @param on_border <code>true</code> if the residue is rendered
       on the border of the parent
       @param sticky <code>true</code> if all the children of the
       current residue should be placed in position 0
	 */

	public void add(Residue node, ResAngle parent_orientation, ResAngle relative_position, boolean on_border, boolean sticky) {
		if( node!=null ) {
			ResAngle absolute_position = parent_orientation.combine(relative_position);
			ResAngle rotation = (sticky) ?relative_position : new ResAngle();
			ResAngle orientation = parent_orientation;
			if( sticky && !relative_position.equals(-45) && !relative_position.equals(45) && !on_border)
				//if( !relative_position.equals(-45) && !relative_position.equals(45) )
				orientation = absolute_position;

			orientations.put(node,orientation);
			rotations.put(node,rotation);
			relative_positions.put(node,relative_position);
			absolute_positions.put(node,absolute_position);
			onborder_flags.put(node,on_border);
			sticky_flags.put(node,sticky);
		}
	}


	/**
       Return the absolute orientation of the parent residue
	 */
	public ResAngle getOrientation(Residue node) {
		return orientations.get(node);
	}

	public ResAngle getRotation(Residue node) {
		return rotations.get(node);
	}

	/**
       Return the orientation of the residue relative to its parent
	 */
	public ResAngle getRelativePosition(Residue node) {
		return relative_positions.get(node);
	}

	/**
       Return the absolute orientation of the residue
	 */
	public ResAngle getAbsolutePosition(Residue node) {
		return absolute_positions.get(node);
	}

	/**
       Return <code>true</code> if the residue is rendered on the
       border of its parent.
	 */
	public boolean isOnBorder(Residue node) {
		Boolean b = onborder_flags.get(node);
		if( b==null )
			return false;
		return b;
	}

	/**
       Return <code>true</code> if the children of the residue should
       be placed all in position 0.
	 */
	public boolean isSticky(Residue node) {
		Boolean b = sticky_flags.get(node);
		if( b==null )
			return false;
		return b;
	}

	/**
       Return all the possible positions of a residue around its parent.
	 */
	static public ResAngle[] getPossiblePositions() {
		return static_positions;
	}

	/**
       Return the possible positions of the residue around its parent
       given the current orientation.
	 */
	public ResAngle[] getAvailablePositions(Residue current, ResAngle orientation) {    
		ResAngle[] positions = getPossiblePositions();
		if( current==null )
			return positions;

		Residue parent = current.getParent();
		if( parent==null )
			return positions;

		ResAngle cur_abs_pos = getAbsolutePosition(current);
		if( cur_abs_pos==null )
			return positions;

		// check for conflicting position (e.g: +90/-90)
		Vector<ResAngle> vec_available_positions = new Vector<ResAngle>();
		for( int i=0; i<positions.length; i++ ) {
			ResAngle pos = positions[i];
			ResAngle abs_pos = orientation.combine(pos);
			if( !cur_abs_pos.isOpposite(abs_pos) )
				vec_available_positions.add(pos);
		}

		// transform into array
		ResAngle[] available_positions = new ResAngle[vec_available_positions.size()];
		vec_available_positions.copyInto(available_positions);

		return available_positions;       
	}

	/**
       Return the children of the residue that have been placed in a
       specific position.
	 */
	public LinkedList<Residue> getChildrenAtPosition(Residue parent, ResAngle position) {
		LinkedList<Residue> nodes = new LinkedList<Residue>();
		for(Iterator<Linkage> i=parent.iterator(); i.hasNext(); ) {
			Residue child = i.next().getChildResidue();
			if( getRelativePosition(child).equals(position) )
				nodes.add(child);
		}
		return nodes;
	}

	/**
       Return the children of the residue that have been placed in a
       specific position.
       @param on_border if <code>true</code> look at the children
       residues on the border of the parent
	 */
	public LinkedList<Residue> getChildrenAtPosition(Residue parent, ResAngle position, boolean on_border) {
		LinkedList<Residue> nodes = new LinkedList<Residue>();
		for(Iterator<Linkage> i=parent.iterator(); i.hasNext(); ) {
			Residue child = i.next().getChildResidue();
			if( getRelativePosition(child).equals(position) && isOnBorder(child)==on_border )
				nodes.add(child);
		}
		return nodes;
	}

	/*
    public Vector<Residue> getChildrenCompatibleWith(Residue parent, Linkage link, Residue child) {
    Vector<Residue> nodes = new Vector<Residue>();

    ResAngle[] child_positions = ResiduePlacementDictionary.getPlacement(parent,link,child).getPositions();
    for( int i=0; i<child_positions.length; i++ )
        nodes.addAll(getChildrenAtPosition(parent,child_positions[i]));

    return nodes;
    }*/
}
