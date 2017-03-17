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

import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.dataset.GWSParser;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
   This class contains the information about a terminal structure. A
   terminal structure is a common motif found at the non-reducing ends
   of glycan structures.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class TerminalType {

    //

    protected String  name;
    protected String  superclass;
    protected String  structure;
    protected String  description;

    //--

    /**
       Create a new terminal type from an initialization string.
       @throws Exception if the string is in the wrong format
    */
    public TerminalType(String init) throws Exception {
    LinkedList<String> tokens = TextUtils.tokenize(init,"\t");
    if( tokens.size()!=4 ) 
        throw new Exception("Invalid string format: " + init);

    name            = tokens.get(0);
    superclass      = tokens.get(1);
    structure       = tokens.get(2);
    description     = tokens.get(3);
    }

    /**
       Return the identifier of the terminal type.
     */
    public String getName() {
    return name;
    }

    /**
       Return the class of this terminal type.
     */
    public String getSuperclass() {
    return superclass;
    }

    /**
       Return the string representation of the structure associated
       with this core type.
     */
    public String getStructure() {
    return "#attach--?" + structure;
    }

    /**
       Return the string representation of the structure associated
       with this core type. Specify the linkage position of this
       terminal on its parent. 
     */
    public String getStructure(char linkage) {
    return "#attach--" + linkage + structure;
    }

    /**
       Return a description of this terminal type.
     */
    public String getDescription() {
    return description;
    }

    /**
       Create a new structure from this terminal type.
     */
    public Residue newTerminal() throws Exception {
    return GWSParser.readSubtree(getStructure(),false);
    }

    /**
       Create a new structure from this terminal type. Specify the
       linkage position of this terminal on its parent.
     */
    public Residue newTerminal(char linkage) throws Exception {
    return GWSParser.readSubtree(getStructure(linkage),false);
    }
}