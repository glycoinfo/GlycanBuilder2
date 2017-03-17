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

package org.eurocarbdb.application.glycanbuilder.converter;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;


/**
   Generic interface for an object that can read and write a glycan
   structure from its string representation in a specific format.
   
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public interface GlycanParser {

    /**
       Set the flag to <code>true</code> if the parser should tolerate
       unknown residues on reading and return a structure with nodes
       of unidentified type.
     */
    public void setTolerateUnknown(boolean f);

    /**
       Convert a glycan structure to its string representation.
     */
    public String writeGlycan(Glycan structure);

    /**
       Create a glycan structure from its string representation.
       @param default_mass_options the mass options to use for the new
       structure if they are not specified in the string
       representation
       @throws Exception if the string cannot be parsed
     */
    public Glycan readGlycan(String buffer, MassOptions default_mass_options) throws Exception;

	public String writeGlycan(Glycan structure, BBoxManager bboxManager);
}