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

package org.eurocarbdb.application.glycanbuilder.renderutil;

//import com.pietjonas.wmfwriter2d.*;

import java.util.*;
import java.text.*;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.image.*;

import javax.swing.*;

import org.w3c.dom.*;
import org.apache.batik.svggen.*;
import org.apache.batik.ext.awt.g2d.GraphicContext;
import org.eurocarbdb.application.glycanbuilder.DefaultPaintable;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;


class SVGGlycanRenderer extends GlycanRendererAWT {


    public SVGGlycanRenderer(GlycanRendererAWT src) {
    theResidueRenderer = src.theResidueRenderer;
    theLinkageRenderer = src.theLinkageRenderer;
    theResiduePlacementDictionary = src.theResiduePlacementDictionary;
    theResidueStyleDictionary = src.theResidueStyleDictionary;
    theLinkageStyleDictionary = src.theLinkageStyleDictionary;
    theGraphicOptions = src.theGraphicOptions;
    }

    public void paint(GroupingSVGGraphics2D g2d, Glycan structure, HashSet<Residue> selected_residues, HashSet<Linkage> selected_linkages, boolean show_mass, boolean show_redend, PositionManager posManager, BBoxManager bboxManager) {    

    if( structure==null || structure.getRoot(show_redend)==null )
        return;
    
    selected_residues = (selected_residues!=null) ?selected_residues :new HashSet<Residue>();
    selected_linkages = (selected_linkages!=null) ?selected_linkages :new HashSet<Linkage>();

    paintResidue(g2d,structure,structure.getRoot(show_redend),selected_residues,selected_linkages,posManager,bboxManager);
    paintBracket(g2d,structure,structure.getBracket(),selected_residues,selected_linkages,posManager,bboxManager);
    if( show_mass ) {
        g2d.addGroup("m",structure,null);
        displayMass(new DefaultPaintable(g2d),structure,show_redend,bboxManager);
    }        
    }

    public void paintResidue(GroupingSVGGraphics2D g2d, Glycan structure,Residue node, HashSet<Residue> selected_residues, HashSet<Linkage> selected_linkages, PositionManager posManager, BBoxManager bboxManager) {    
    if( node==null )
        return;

    Rectangle parent_bbox  = bboxManager.getParent(node);
    Rectangle node_bbox    = bboxManager.getCurrent(node);
    Rectangle border_bbox  = bboxManager.getBorder(node);
    Rectangle support_bbox = bboxManager.getSupport(node);
    
    if( node_bbox==null ) // not shown
        return;

    // paint edges
    for(Linkage link : node.getChildrenLinkages() ) {
        Residue child = link.getChildResidue();        
        Rectangle child_bbox = bboxManager.getCurrent(child);
        Rectangle child_border_bbox = bboxManager.getBorder(child);

        if( child_bbox!=null && !posManager.isOnBorder(child) ) {
        g2d.addGroup("l",structure,node,child);
        boolean selected = (selected_residues.contains(node) && selected_residues.contains(child)) || selected_linkages.contains(link);
        theLinkageRenderer.paintEdge(new DefaultPaintable(g2d),link,selected,node_bbox,border_bbox,child_bbox,child_border_bbox);                
        }        
    }
    
    // paint node
    g2d.addGroup("r",structure,node);
    theResidueRenderer.paint(new DefaultPaintable(g2d),node,selected_residues.contains(node),posManager.isOnBorder(node),parent_bbox,node_bbox,support_bbox,posManager.getOrientation(node));
    
    // paint children
    for(Linkage link : node.getChildrenLinkages() ) 
        paintResidue(g2d,structure,link.getChildResidue(),selected_residues,selected_linkages,posManager,bboxManager);

    // paint info
    for(Linkage link : node.getChildrenLinkages() ) {
    
        Residue child = link.getChildResidue();        
        Rectangle child_bbox = bboxManager.getCurrent(child);
        Rectangle child_border_bbox = bboxManager.getBorder(child);

        if( child_bbox!=null && !posManager.isOnBorder(child) ) {
        g2d.addGroup("li",structure,node,child);
        theLinkageRenderer.paintInfo(new DefaultPaintable(g2d),link,node_bbox,border_bbox,child_bbox,child_border_bbox);                        
        }
    }
    }

    public void paintBracket(GroupingSVGGraphics2D g2d, Glycan structure, Residue bracket, HashSet<Residue> selected_residues, HashSet<Linkage> selected_linkages, PositionManager posManager, BBoxManager bboxManager) {    
    if( bracket==null )
        return;
    
    Rectangle parent_bbox  = bboxManager.getParent(bracket);
    Rectangle bracket_bbox = bboxManager.getCurrent(bracket);
    Rectangle support_bbox = bboxManager.getSupport(bracket);

    // paint bracket
    g2d.addGroup("b",structure,bracket);
    theResidueRenderer.paint(new DefaultPaintable(g2d),bracket,selected_residues.contains(bracket),false,parent_bbox,bracket_bbox,support_bbox,posManager.getOrientation(bracket));

    // paint antennae
    for( Linkage link : bracket.getChildrenLinkages() ) {
        Residue child = link.getChildResidue();       
        int quantity  = bboxManager.getLinkedResidues(child).size()+1;

        Rectangle node_bbox         = bboxManager.getParent(child);
        Rectangle child_bbox        = bboxManager.getCurrent(child);
        Rectangle child_border_bbox = bboxManager.getBorder(child);
        
        if( child_bbox!=null ) {        
        // paint edge
        if( !posManager.isOnBorder(child) ) {
            g2d.addGroup("l",structure,bracket,child);
            boolean selected = (selected_residues.contains(bracket) && selected_residues.contains(child)) || selected_linkages.contains(link);
            theLinkageRenderer.paintEdge(new DefaultPaintable(g2d),link,selected,node_bbox,node_bbox,child_bbox,child_border_bbox);
        }
        
        // paint child
        paintResidue(g2d,structure,child,selected_residues,selected_linkages,posManager,bboxManager);    

        // paint info
        if( !posManager.isOnBorder(child) ) {
            g2d.addGroup("li",structure,bracket,child);
            theLinkageRenderer.paintInfo(new DefaultPaintable(g2d),link,node_bbox,node_bbox,child_bbox,child_border_bbox);
        }

        if( quantity>1 ) 
            paintQuantity(new DefaultPaintable(g2d),child,quantity,bboxManager);        
        }                
    }    
    }

}
