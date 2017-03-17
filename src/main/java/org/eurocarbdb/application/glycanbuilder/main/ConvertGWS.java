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

package org.eurocarbdb.application.glycanbuilder.main;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;


/**
*   Converts GWS sequence format to other sequence formats.
*
*   @author aceroni
*/
public class ConvertGWS 
{

    /**
    *   Converts GWS format to the given format name. 
    *   See {@link GlycanDocument#toString} for valid format names.
    */
    public static final String toFormat( String format_name, String gws_sequence )
    throws Exception
    {
        BuilderWorkspace theWorkspace = new BuilderWorkspace( null, false,new GlycanRendererAWT() );
        GlycanDocument theDoc = theWorkspace.getStructures(); 
        
        theDoc.fromString( gws_sequence, false );
        return theDoc.toString( format_name );
    }
    
    
    static public void main(String[] args) 
    {
        if( args.length == 0 ) 
        {
            System.err.println("Usage: ConvertGWS <GWS string> [<output format>]");
            return;
        }
    
        String sequence = args[0];
        String outputType = ( args.length > 1 ) ? args[1] : "glycoct_xml";
                    
        try 
        {
            System.out.println( toFormat( outputType, sequence ) );
        }
        catch( Exception e ) 
        {
                System.err.println("Error: " + LogUtils.getError(e));
                System.err.println("Stack: " + LogUtils.getErrorStack(e));
        }           
    }
    
}