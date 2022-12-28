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
package org.eurocarbdb.application.glycanbuilder;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eurocarbdb.application.glycanbuilder.renderutil.ResAngle;

/**
   The booking manager is used to assign display positions to the
   children of a residue. In case more children could be displayed in
   the same positions they are redistributed to minimize the number of
   residues in each position.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/


public class BookingManager {

	// 20221130 S.TSUCHIYA, investigating the error about java.io.FileNotFoundException
	//private static final Log logger = LogFactory.getLog(BookingManager.class);

	private HashMap<Integer,LinkedList<Residue> > available_positions;
	private HashMap<Residue,ResAngle>          assigned_positions;

	private HashMap<Residue,ResiduePlacement> residues;
	private LinkedList<Residue> single_position_residues;
	private LinkedList<Residue> on_border_residues;
	private LinkedList<Residue> other_residues;


	/**
       Constructor.
       @param avail_positions display positions available around the
       parent residue
	 */
	public BookingManager(ResAngle[] avail_positions) {

		// init residue list
		residues  = new HashMap<Residue,ResiduePlacement>();
		single_position_residues = new LinkedList<Residue>();
		on_border_residues = new LinkedList<Residue>();
		other_residues = new LinkedList<Residue>();

		// init positions
		available_positions = new HashMap<Integer,LinkedList<Residue> >();
		assigned_positions = new HashMap<Residue,ResAngle>();

		for( int i=0; i<avail_positions.length; i++ ) {
			available_positions.put(avail_positions[i].getIntAngle(), new LinkedList<Residue>());
		}
	}

	/**
       Collect the positions in which a residue can be displayed.
       @param r the child residue
       @param rp the requested positions
       @throws Exception when none of the requested positions are available
	 */
	public void add(Residue r, ResiduePlacement rp) throws Exception {
		if( r==null || rp==null )
			return;
		if( !isAvailable(rp) ) 
			throw new Exception("Cannot place residue " + r.getTypeName() + " in position(s) " + rp.getStringPositions());

		residues.put(r,rp);
		if( rp.getPositions().length==1 ) 
			single_position_residues.add(r);
		else if( rp.isOnBorder()==true )
			on_border_residues.add(r);
		else 
			other_residues.add(r);
	}   

	/**
       Place all the children in some display positions around the
       parent.
       @throws Exception if some residue cannot be assigned to any position
	 */
	public void place() throws Exception {
		// assign single position residues
		for( Residue r : single_position_residues) 
			assignPosition(r,getPossiblePositions(r)[0]);

		// assign on border residues 
		for( Residue r : on_border_residues) 
			assignPosition(r,findOnBorderPosition(r));            

		// assign other residues
		for( Residue r : other_residues) 
			assignPosition(r,findPosition(r));    
	}

	/**
       Return the final display position of the residue.
	 */
	public ResAngle getPosition(Residue r) {
		return assigned_positions.get(r);
	}

	/**
       Return the positions initial requested by the residue.
	 */    
	public ResiduePlacement getPlacement(Residue r) {
		return residues.get(r);
	}

	/**
       Return <code>true</code> if at least one of the requested
       positions is available.
	 */
	public boolean isAvailable(ResiduePlacement rp) {
		ResAngle[] pos = rp.getPositions();
		for( int i=0; i<pos.length; i++ ) 
			if( isAvailable(pos[i]) )
				return true;
		return false;
	}

	//    

	private boolean isAvailable(ResAngle p) {
	  // NPE found here: https://github.com/glytoucan/glytoucan.github.io/issues/91
		// 20221130 S.TSUCHIYA, investigating the error about java.io.FileNotFoundException
		//logger.debug("NPE was here.");
	  if (p == null)
	    return false;
		return ( available_positions.get(p.getIntAngle())!=null );
	}

	private LinkedList<Residue> getAssignedResidues(ResAngle p) {
		return available_positions.get(p.getIntAngle());
	}

	private void assignPosition(Residue r, ResAngle ra) throws Exception{
		LinkedList<Residue> ar = getAssignedResidues(ra);
		if( ar==null )
			throw new Exception("Cannot assign residue " + r.getTypeName() + " in position " + ra.getIntAngle() + ": position is not available");        

		ar.add(r);
		assigned_positions.put(r,ra);
	}

	private ResAngle[] getPossiblePositions(Residue r) {    
		return residues.get(r).getPositions();
	}

	private boolean isOnBorder(Residue r) {
		return residues.get(r).isOnBorder();
	}

	private int countOnBorderResidues(ResAngle p) {
		int count = 0;
		for( Iterator<Residue> l=getAssignedResidues(p).iterator(); l.hasNext(); ) {
			if( isOnBorder(l.next()) ) 
				count++;
		}
		return count;
	}

	private boolean hasEmptyPositions(ResAngle[] pos) {
		for( int i=0; i<pos.length; i++ ){
			if( available_positions.get(pos[i].getIntAngle())==null || available_positions.get(pos[i].getIntAngle()).size()==0 ){
				return true;
			}
		}
		return false;
	}

	private ResAngle findOnBorderPosition(Residue r) {
		ResAngle[] positions = getPossiblePositions(r);  
		if(other_residues.size()>0 || !hasEmptyPositions(positions) ) {
			for( int i=0; i<positions.length; i++ ) {
				if( countOnBorderResidues(positions[i])==1 )
					return positions[i];
			}
		}

		return findPosition(r);
	}

	private ResAngle findPosition(Residue r) {
		ResAngle[] positions = getPossiblePositions(r);

		ResAngle best_pos = null; 
		int      best_occ = 0;
		for( int i=0; i<positions.length; i++ ) {

			ResAngle pos = positions[i];
			if( isAvailable(pos) ) {        
				int occ = getAssignedResidues(pos).size();
				if( best_pos==null || occ<best_occ ) {
					best_pos = pos;
					best_occ = occ;
				}
			}
		}

		return best_pos;
	}
}