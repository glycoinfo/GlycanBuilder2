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

package org.eurocarbdb.application.glycanbuilder.converterLINUCS;

import java.util.*;
import java.util.regex.*;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParser;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
   Read glycan structures from strings in LINUCS format. Writing is
   not supported.
   
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class LinucsParser implements GlycanParser {

    static Pattern linucs_type_pattern;
    static Pattern linucs_link_pattern;
    
    static {
    linucs_type_pattern = Pattern.compile("^(?:([abo\\?])-)?(?:([dl\\?])-)?((?:[1-9](?:\\,[1-9])*-?deoxy-)?(?:[1-9](?:\\,[1-9])*-?en-)?(?:[1-9](?:\\,[1-9])*-?anhydro-)?[a-z]{3}(?:hex)?)([fp])?([an]?)((?:[1-9n][a-z]{1,3})?)((?:[1-9n][a-z]{1,3})?)((?:[1-9n][a-z]{1,3})?)(-ol)?$");
    linucs_link_pattern = Pattern.compile("^\\((.+)\\+(.+)\\)$");
    }
   
    public void setTolerateUnknown(boolean f) {
    }

    public String writeGlycan(Glycan structure) {
    return null;
    }

    public Glycan readGlycan(String str, MassOptions default_mass_options) throws Exception {
    return new Glycan(readSubtree(str),true,default_mass_options);    
    }

         
    static private Residue readSubtree(String str) throws Exception {  
     // parse node    
    int endlink = TextUtils.findEnclosed(str,0,'[',']');
    if( endlink==-1 )
        throw new Exception("Invalid input: " + str);

    int endtype = TextUtils.findEnclosed(str,endlink+1,'[',']');
    if( endtype==-1 )
        throw new Exception("Invalid input: " + str);

    // save node info
    String link = str.substring(1,endlink); 
    String type = str.substring(endlink+2,endtype).toUpperCase(); 

    // parse children    
    int endchild = TextUtils.findEnclosed(str,endtype+1,'{','}');
    if( endchild==-1 || endchild!=str.length()-1 )
        throw new Exception("Invalid input: " + str);
    str = str.substring(endtype+2,endchild); 

    Vector<Residue> parsed_children = new Vector<Residue>();
    while( str.length()>0 ) {
        
        endlink = TextUtils.findEnclosed(str,0,'[',']');
        if( endlink==-1 )
        throw new Exception("Invalid child string: " + str);
        endtype = TextUtils.findEnclosed(str,endlink+1,'[',']');
        if( endtype==-1 )
        throw new Exception("Invalid child string: " + str);
        endchild = TextUtils.findEnclosed(str,endtype+1,'{','}');
        if( endchild==-1 )
        throw new Exception("Invalid child string: " + str);
       
        parsed_children.add(readSubtree(str.substring(0,endchild+1)));
        
        str = str.substring(endchild+1);        
    }

    // create residue
    Residue parent = null;
    if( parsed_children.size()==1 ) {
    	
        parent = createResidueFromLINUCS(link,type,true);      
        if( parent==null  ) 
        return parsed_children.elementAt(0);        
    }
    else
        parent = createResidueFromLINUCS(link,type,false);      

    if( !parent.canHaveChildren() && parsed_children.size()>0)
        throw new Exception("Linking to non parentable");
    
    // add children
    for( Residue child : parsed_children ) {
        if( child.getParentLinkage()!=null )
        parent.addChild(child,child.getParentLinkage().getBonds());
        else
        parent.addChild(child);
    }
    return parent;    
    }
    
    
    static private Residue createResidueFromLINUCS(String _link, String _type, boolean skip_unknown) throws Exception {
    _type = TextUtils.squeezeAll(_type.toLowerCase(), ' ').replace(' ','_');

    // parse type
    Matcher mt = linucs_type_pattern.matcher(_type);
    if( !mt.matches() ) {
        if( _type.equals("p") )
        return ResidueDictionary.newResidue("P");    
        if( _type.equals("sulfate") ) 
        return ResidueDictionary.newResidue("S");    
        if( _type.equals("methyl") )
        return ResidueDictionary.newResidue("Me");
        if( _type.equals("2-aminopyridine") )
        return ResidueDictionary.newResidue("2AP");

        if( skip_unknown )
        return null;
        throw new Exception("Invalid type: " + _type);       
    }
    
    // create residue
    Residue ret = new Residue();

    ret.setAnomericState(getProperty(mt.group(1),"ab"));
    ret.setChirality(getProperty(mt.group(2), "DL"));
    String residue_type = mt.group(3) + mt.group(5);
           ret.setRingSize(getProperty(mt.group(4),"pf"));

    // parse modifications
    boolean alditol = false;
    int start_mod = 6;      
    int end_mod = mt.groupCount();
    if( mt.group(end_mod)!=null && mt.group(end_mod).equals("-ol") ) {
        alditol = true;
        end_mod--;
    }
    
    Vector<String> modifications = new Vector<String>();
    for(int i=start_mod; i<=end_mod; i++ ) {
        if( mt.group(i)!=null && mt.group(i).length()>0 ) {
        if( mt.group(i).equals("nac") && (residue_type.equals("gal") || residue_type.equals("glc") || residue_type.equals("man")) )
            residue_type = residue_type + mt.group(i);                        
        else if( residue_type.equals("neu") && (mt.group(i).equals("5ac") || mt.group(i).equals("5gc")) ) 
            residue_type = residue_type + mt.group(i).substring(1); // remove 5 position
        else
            modifications.add(mt.group(i));        
        }
    }        

    // set residue type
    if( skip_unknown && !ResidueDictionary.hasResidueType(residue_type) ) 
        return null;                
    ret.setType(ResidueDictionary.getResidueType(residue_type));

    // add modifications
    for( String sub : modifications ) {        
        ret.addChild( ResidueDictionary.newResidue(sub.substring(1)), Character.toUpperCase(sub.charAt(0)) );    
    }

    // parse link
    if( _link.length()>0 ) {        
        Matcher ml = linucs_link_pattern.matcher(_link);
        if( !ml.matches() ) 
        throw new Exception("Invalid link: " + _link);           
        
        if( ml.group(1)!=null && ml.group(1).length()==1 && Character.isDigit(ml.group(1).charAt(0)) ) {
        if( ml.group(1).charAt(0)=='0' ) 
            ret.setParentLinkage(new Linkage(null,ret));
        else
        	ret.setParentLinkage(new Linkage(null,ret, ml.group(1).charAt(0)));
        }
        if( ml.group(2)!=null && ml.group(2).length()==1 && Character.isDigit(ml.group(2).charAt(0)) && ml.group(2).charAt(0)!='0' )        
        	ret.setAnomericCarbon(ml.group(2).charAt(0));
    }
    
    return ret;
    }

   
    static private char getProperty(String value, String domain) {
    if( value==null || value.length()>1 ) 
        return '?';

    char v = value.toLowerCase().charAt(0);
    int ind = domain.toLowerCase().indexOf(v);
    if( ind==-1 )
        return '?';
    return domain.charAt(ind);
    }

	@Override
	public String writeGlycan(Glycan structure, BBoxManager bboxManager) {
		throw new UnsupportedOperationException();
	}
}