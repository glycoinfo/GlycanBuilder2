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


package org.eurocarbdb.application.glycanbuilder.converterGlycoMinds;

import java.util.*;
import java.util.regex.*;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParser;
import org.eurocarbdb.application.glycanbuilder.converterGWS.GWSParser;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
   Read glycan structure from strings in GlycoMind format.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class GlycoMindsParser implements GlycanParser {

    private static final class SU {
    public String code;
    public char chirality;
    public char ring_size;

    public SU(String _code) {
        code = _code;
        chirality = '?';
        ring_size = '?';
    }

    public SU(String _code, char _chirality, char _ring_size) {
        code = _code;
        chirality = _chirality;
        ring_size = _ring_size;
    }
    }

    private static Pattern gmind_pattern;
    private static Pattern gmind_sub_pattern;
    
    private static HashMap<String,String> gmind_types;

    private static HashMap<String,SU> gmind_codes;

    static {
    gmind_pattern = Pattern.compile("([A-Z]+)([\\'\\^\\~]?)((?:\\[[\\?1-9a-zA-Z\\,]+\\])?)([abo\\?]?)([1-9\\?]?(?:/[1-9\\?])*)\\z");
    gmind_sub_pattern = Pattern.compile("([\\?1-9])([a-zA-Z]+)(?:\\,([\\?1-9])([a-zA-Z]+))*");

    gmind_types = new HashMap<String,String>();
    gmind_types.put("G", "?1D-Glc,p");
    gmind_types.put("A", "?1D-Gal,p");
    gmind_types.put("GN","?1D-GlcNAc,p");
    gmind_types.put("AN","?1D-GalNAc,p");
    gmind_types.put("M", "?1D-Man,p");
    gmind_types.put("N", "?2D-Neu,p");
    gmind_types.put("NN","?2D-NeuAc,p");
    gmind_types.put("NJ","?2D-NeuGc,p");
    gmind_types.put("K", "?2D-KDN,p");
    gmind_types.put("W", "?2D-KDO,p");
    gmind_types.put("L", "?1D-GalA,p");
    gmind_types.put("I", "?1D-IdoA,p");
    gmind_types.put("H", "?1L-Rha,p");
    gmind_types.put("F", "?1L-Fuc,p");
    gmind_types.put("X", "?1D-Xyl,p");
    gmind_types.put("B", "?1D-Rib,p"); 
    gmind_types.put("R", "?1L-Ara,f");
    gmind_types.put("U", "?1D-GlcA,p");
    gmind_types.put("O", "?1D-All,p"); 
    gmind_types.put("P", "?1D-Api,p");
    gmind_types.put("E", "?2D-Fru,f");

    gmind_types.put("T", "Ac");
    gmind_types.put("S", "S");
    gmind_types.put("P", "P");
    gmind_types.put("ME", "Me");

    gmind_codes = new HashMap<String,SU>();
    gmind_codes.put("Glc",new SU("G", 'D', 'p'));
    gmind_codes.put("Gal",new SU("A", 'D', 'p'));
    gmind_codes.put("GlcNAc",new SU("GN",'D','p'));
    gmind_codes.put("GalNAc",new SU("AN",'D','p'));
    gmind_codes.put("Man",new SU("M",'D','p'));
    gmind_codes.put("Neu",new SU("N",'D','p'));
    gmind_codes.put("NeuAc",new SU("NN",'D','p'));
    gmind_codes.put("NeuGc",new SU("NJ",'D','p'));
    gmind_codes.put("KDN",new SU("K",'D','p'));
    gmind_codes.put("KDO",new SU("W",'D','p'));
    gmind_codes.put("GalA",new SU("L",'D','p'));
    gmind_codes.put("IdoA",new SU("I",'D','p'));
    gmind_codes.put("Rha",new SU("H",'L','p'));
    gmind_codes.put("Fuc",new SU("F",'L','p'));
    gmind_codes.put("Xyl",new SU("X",'D','p'));
    gmind_codes.put("Rib",new SU("B",'D','p')); 
    gmind_codes.put("Ara",new SU("R",'L','f'));
    //Corrected Glc to GlcA
    gmind_codes.put("GlcA",new SU("U",'D','p'));
    gmind_codes.put("All",new SU("O",'D','p')); 
    gmind_codes.put("Api",new SU("P",'D','p'));
    gmind_codes.put("Fru",new SU("E",'D','f'));

    gmind_codes.put("Ac",new SU("T"));
    gmind_codes.put("S",new SU("S"));
    gmind_codes.put("P",new SU("P"));
    gmind_codes.put("Me",new SU("ME"));
    }

    public void setTolerateUnknown(boolean f) {
    }

    public String writeGlycan(Glycan structure) {
    if( structure.isFragment() )
        return "";
    
    // remove reducing end modification
    Residue root = structure.getRoot();
    if( root!=null && !root.isSaccharide() )
        root = root.firstChild();       

    // write structure
    if( structure.getBracket()==null )
        return writeSubtree(root,false);

    // write core
    StringBuilder sb = new StringBuilder();
    sb.append(writeSubtree(root,true));

    // write antennae
    for( Linkage l : structure.getBracket().getChildrenLinkages() ) {
        sb.append(',');
        sb.append(writeSubtree(l.getChildResidue(),false));
    }    
    
    return sb.toString();
    }

    public Glycan readGlycan(String str, MassOptions default_mass_options) throws Exception {
    str = TextUtils.trim(str);

    if( str.indexOf("//")!=-1 )
        throw new Exception("Unsupported structures with uncertain residues");
    if( str.indexOf("*")!=-1 )
        throw new Exception("Unsupported structures with unknown residues");
    if( str.indexOf("{")!=-1 )
        throw new Exception("Unsupported structures with repeating units");
    
    // remove aglyca
    int index = str.indexOf(";");
    if( index==-1 ) {
        index = str.indexOf(":");
        if( index==-1 )
        index = str.indexOf("#");
    }
    if( index!=-1 ) 
        str = str.substring(0,index);
    
    // remove variable specifications
    str = str.replaceAll("(\\([1-9]+\\%\\))|([1-9]+\\%)",""); 

    // detect core and antennae
    String[] tokens1 = str.split("\\|");
    String str_core = tokens1[tokens1.length-1];

    String[] tokens2 = new String[0];
    if( str_core.indexOf(",")!=-1 ) {
        // antennae specification in cartoonist
        tokens2 = str.split("\\,");
        str_core = tokens2[0];
    }

    // parse the core
    System.out.println("=> " + str_core);
    Glycan structure = new Glycan(readSubtree(str_core),true,default_mass_options);

    // parse antennae
    for( int i=tokens1.length-2; i>=0; i-- ) {
        String str_antenna = tokens1[i].substring(0,tokens1[i].length()-1);
        Residue antenna = readSubtree(str_antenna);           
        structure.addAntenna(antenna,antenna.getParentLinkage().getBonds());
    }
    for( int i=1; i<tokens2.length; i++ ) {
        String str_antenna = tokens2[i];
        Residue antenna = readSubtree(str_antenna);           
        structure.addAntenna(antenna,antenna.getParentLinkage().getBonds());
    }

    return structure;
    }


    private static Residue readSubtree(String str) throws Exception {       
    
    String in = "" + str;

    Matcher m = gmind_pattern.matcher(str);
    if( !m.find() ) 
        throw new Exception("Unrecognized format: " + str);

    // parse residue
    Residue ret = createFromGlycoMinds(m.group(1),m.group(2),m.group(3),m.group(4),m.group(5));
    str = str.substring(0,str.length()-m.group(0).length());

    // parse children
    Vector<Linkage> children  = new Vector<Linkage>();
    while( str.length()>0 ) {
        Residue child = null; 
        int par_ind = TextUtils.findEnclosedInvert(str,str.length()-1,'(',')');
        if( par_ind!=-1 ) {
        child = readSubtree(str.substring(par_ind+1,str.length()-1));
        str = str.substring(0,par_ind);
        }
        else {
        child = readSubtree(str);
        str = "";
        }
        children.add(child.getParentLinkage());
    }    
    
    // put children in glycomics order 
    if( children.size()>0 ) {        
        children.insertElementAt(children.lastElement(),0);
        children.remove(children.size()-1);
    }    
    fixBisectingGlcNAc(ret,children);

    // add children
    //Collections.sort(children,new Linkage.LinkageComparator());       
    for( Linkage l : children )
        ret.addChild(l.getChildResidue(),l.getBonds());
    return ret;
    }

    private static void fixBisectingGlcNAc(Residue parent, Vector<Linkage> children ) {
    if( !parent.getTypeName().equals("Man") || children.size()!=3 ) 
        return;

    int pos = 0;
    int glcnac_pos = -1;
    int no_glcnac = 0;
    int no_man = 0;
    for( int i=0; i<children.size(); i++ ) {
        Linkage l = children.get(i);
        if( l.getChildResidue().getTypeName().equals("Man") ) 
        no_man++; 
        else if( l.getChildResidue().getTypeName().equals("GlcNAc") ) {
        no_glcnac++;         
        glcnac_pos = i;
        }
        else
        return;
    }

    if( no_glcnac!=1 || no_man!=2 )
        return;

    if( glcnac_pos!=1 ) {
        // swap pos
        Linkage help = children.get(1);
        children.set(1,children.get(glcnac_pos));
        children.set(glcnac_pos,help);
    }
    }

    private static Residue createFromGlycoMinds(String type, String mod_stereo, String subs, String anom, String link) throws Exception {
    
    // get residue type
    String res_type = gmind_types.get(type);
    System.out.println("Residue type is "+res_type);
    if( res_type==null )
        throw new Exception("Unrecognized gmind type: " + type);
    Residue ret = GWSParser.readSubtree(res_type,false);

    // stereochemistry modifications
    if( mod_stereo!=null && mod_stereo.length()>0 ) {
        if( mod_stereo.equals("'") ) 
        ret.setChirality((ret.getChirality()=='D') ?'L' :'D');
        else if( mod_stereo.equals("^") ) 
        ret.setRingSize((ret.getRingSize()=='p') ?'f' :'p');        
        else if( mod_stereo.equals("~") ) {
        ret.setChirality((ret.getChirality()=='D') ?'L' :'D');
        ret.setRingSize((ret.getRingSize()=='p') ?'f' :'p');        
        }
    }

    // anomericity
    if( anom!=null && anom.length()>0 ) 
        ret.setAnomericState(anom.charAt(0));

    // substitutions
    if( subs!=null && subs.length()>1 ) {
        subs = subs.substring(1,subs.length()-1);
        Matcher m = gmind_sub_pattern.matcher(subs);
        if( !m.lookingAt() )  
        throw new Exception("Unrecognized format for substitution: " + subs);
        
        for( int i=0; i<m.groupCount(); i+=2 ) {
        String sub = m.group(i+2);
        if( sub!=null && sub.length()>0 ) {
            String sub_type = gmind_types.get(sub);
            if( sub_type==null )
            throw new Exception("Unrecognized gmind type: " + sub);
            Residue ret_sub = ResidueDictionary.newResidue(sub_type);
            ret.addChild(ret_sub,m.group(i+1).charAt(0));        
        }        
        }
    }

    // linkage position
    Linkage par_link = new Linkage(null,ret);
    if( link!=null && link.length()>0 ) 
        par_link.setLinkagePositions(parsePositions(link));
    ret.setParentLinkage(par_link);

    return ret;
    }

    static private char[] parsePositions(String str) {
    String[] fields = str.split("/");
    char[] ret = new char[fields.length];
    for( int i=0; i<fields.length; i++ )
        ret[i] = fields[i].charAt(0);
    return ret;
    }    

    private String writeSubtree(Residue r, boolean add_uncertain_leaf) {
    if( r==null )
        return "";
    if( !r.isSaccharide() )
        return "*";
        
    StringBuilder sb = new StringBuilder();

    // create SU
    if( gmind_codes.get(r.getTypeName())==null ) {
        // unsupported type
        sb.insert(0,'*'); 
    }
    else {
        // add linkage info
        if( r.getParentLinkage()!=null )  {
        char ppos = r.getParentLinkage().getParentPositionsSingle();
        if( ppos!='?' )
            sb.insert(0,ppos);
        }
        sb.insert(0,r.getAnomericState());
        
        // get childrens
        Vector<Linkage> modifications = new Vector<Linkage>();
        Vector<Linkage> children = new Vector<Linkage>();
        for( Linkage l : r.getChildrenLinkages() ) {
        if( l.getChildResidue().isSaccharide() )
            children.add(l);
        else
            modifications.add(l);
        }
        Collections.sort(modifications,new Linkage.LinkageComparator());
        
        // add modifications
        if( modifications.size()>0 ) {
        StringBuilder msb = new StringBuilder();
        msb.append('[');
        for( Linkage l : modifications ) {
            if( gmind_codes.get(l.getChildResidue().getTypeName())==null )
            sb.append('*');
            else {
            sb.append(l.getParentPositionsSingle());
            sb.append(gmind_codes.get(l.getChildResidue().getTypeName()).code);
            }
        }
        msb.append(']');

        sb.insert(0,msb.toString());
        }
        
        // add type
        SU su = gmind_codes.get(r.getTypeName());
        if( su.chirality!=r.getChirality() && su.ring_size!=r.getRingSize() )
        sb.insert(0,'~');
        else if( su.chirality!=r.getChirality() )
        sb.insert(0,'\'');
        else if( su.ring_size!=r.getRingSize() )
        sb.insert(0,'^');
        sb.insert(0,su.code);

        // add children
        if( children.size()>0 ) {
        for( int i=1; i<children.size(); i++ ) {
            sb.insert(0,')');
            sb.insert(0,writeSubtree(children.get(i).getChildResidue(),add_uncertain_leaf));
            sb.insert(0,'(');
        }
        sb.insert(0,writeSubtree(children.firstElement().getChildResidue(),add_uncertain_leaf));
        }
        else {
        if( add_uncertain_leaf )
            sb.insert(0,"1%");
        }
    }
    return sb.toString();
    }

	@Override
	public String writeGlycan(Glycan structure, BBoxManager bboxManager) {
		throw new UnsupportedOperationException();
	}
}