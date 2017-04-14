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

import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.*;

import java.awt.*;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.linkage.LinkageStyle;
import org.eurocarbdb.application.glycanbuilder.linkage.LinkageStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
   Objects of this class are used to create a graphical representation
   of a {@link Linkage} object given the current graphic options
   ({@link GraphicOptions}. The rules to draw the linkage in the
   different notations are stored in the {@link
   LinkageStyleDictionary}.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class LinkageRendererAWT extends AbstractLinkageRenderer {
	public LinkageRendererAWT(){
		super();
	}
	
	public LinkageRendererAWT(GlycanRenderer src){
		super(src);
	}

	@Override
	public void paintEdge(Paintable paintable, Linkage link, boolean selected, Rectangle parent_bbox, Rectangle parent_border_bbox, Rectangle child_bbox, Rectangle child_border_bbox) {
    	if( link==null )
    		return;
    
    	Graphics2D g2d=paintable.getGraphics2D();

    	Stroke edge_stroke = createStroke(link,selected);
    	Shape edge_shape   = createShape(link,parent_bbox,child_bbox);

    	if(edge_shape!=null){
    		g2d.setStroke(edge_stroke);
    		g2d.setColor(Color.black);
    		g2d.draw(edge_shape);    
    		g2d.setStroke(new BasicStroke(1));
    	}    
    }

    @Override
	public void paintInfo(Paintable paintable, Linkage link, Rectangle parent_bbox, Rectangle parent_border_bbox, Rectangle child_bbox, Rectangle child_border_bbox) {
    	if( link==null || !theGraphicOptions.SHOW_INFO )
    		return;
    
    	Graphics2D g2d=paintable.getGraphics2D();

    	LinkageStyle style = theLinkageStyleDictionary.getStyle(link);

    	Font old_font = g2d.getFont();
    	Font new_font = new Font(theGraphicOptions.LINKAGE_INFO_FONT_FACE,Font.PLAIN,theGraphicOptions.LINKAGE_INFO_SIZE);
    	g2d.setFont(new_font);

    	Residue child = link.getChildResidue();
    	
    	if(child.isAlternative()) return;
    	
    	if( style.showParentLinkage(link) && checkEdgeConditionParentLinkage(link) )
    		paintInfo(paintable,createParentPosiitonProbability(link),parent_bbox,parent_border_bbox,child_bbox,child_border_bbox,true,false,link.hasMultipleBonds());
    	if( style.showAnomericCarbon(link) ) 
    		paintInfo(paintable,link.getChildPositionsString(),parent_bbox,parent_border_bbox,child_bbox,child_border_bbox,false,true,link.hasMultipleBonds());
    	if( style.showAnomericState(link,child.getAnomericState()) && checkAnomericPosition(link)) 
    		paintInfo(paintable,TextUtils.toGreek(child.getAnomericState()),parent_bbox,parent_border_bbox,child_bbox,child_border_bbox,false,false,link.hasMultipleBonds());        

    	g2d.setFont(old_font);
    }
    
    @Override
    protected void paintInfo(Paintable paintable, String text, Rectangle p, Rectangle pb, Rectangle c, Rectangle cb, boolean toparent, boolean above, boolean multiple) {
    	Graphics2D g2d=paintable.getGraphics2D();
    	Dimension tb = textBounds(text,theGraphicOptions.LINKAGE_INFO_FONT_FACE,theGraphicOptions.LINKAGE_INFO_SIZE);
    	Point pos = computePosition(tb,p,pb,c,cb,toparent,above,multiple);

    	g2d.clearRect(pos.x,(int)(pos.y-tb.getHeight()),(int)tb.getWidth(),(int)tb.getHeight());
    	g2d.drawString(text,pos.x,pos.y);
    }
    
    protected Stroke createStroke(Linkage link, boolean selected) {
    	LinkageStyle style = theLinkageStyleDictionary.getStyle(link);

    	if( style.isDashed() ) {
    		float[] dashes = {5.f,5.f};
    		return new BasicStroke((selected) ?2.f :1.f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,1.f,dashes,0.f);
    	}

    	return new BasicStroke((selected) ?2.f :1.f);
    }
    
    private boolean checkEdgeConditionParentLinkage(Linkage link) {
		boolean a_bIsShow = false;
		
		if(!link.getChildResidue().isComposition() && 
			!link.getParentResidue().isStartRepetition() && 
			(!link.getParentResidue().isReducingEnd() && !link.getChildResidue().isStartRepetition()) &&
			!link.getChildResidue().isStartCyclic()) a_bIsShow = true;
		if(link.getParentResidue().isSaccharide() && link.getChildResidue().isStartRepetition()) a_bIsShow = true;
		
		return a_bIsShow;
	}
    
    private boolean checkAnomericPosition(Linkage link) {
    	if(link.getChildResidue().getAnomericState() == 'o') return false;
    	if(link.getParentResidue().isReducingEnd() && link.getChildResidue().isStartRepetition()) return false;
    	
    	return true;
    }
    
    private String createParentPosiitonProbability (Linkage a_oLinkage) {
    	StringBuilder a_sProbability = new StringBuilder("");
    	int a_iHigh = a_oLinkage.getBonds().get(0).getProbabilityHigh();
    	int a_iLow = a_oLinkage.getBonds().get(0).getProbabilityLow();
 
    	if((a_iHigh != 100 && a_iLow != 100) && (a_iHigh == a_iLow)) {
    		a_sProbability.append("(" + ((a_iHigh == -100) ? "?" : a_iHigh) + "%" + ")");
    		a_sProbability.append(a_oLinkage.getParentPositionsString());
    		return a_sProbability.toString();
    	}
    	
    	if(a_iLow != 100) {
    		a_sProbability.append("(");
    		a_sProbability.append((a_iLow == -100) ? "?" : a_iLow);
    	}
    	if(a_iHigh != 100 || a_iLow < 100) {
    		if(a_sProbability.length() != 0) {
    			a_sProbability.append(",");
    			a_sProbability.append((a_iHigh == -100) ? "?" : a_iHigh);
    		}else {
    			a_sProbability.append("(");
    			a_sProbability.append((a_iHigh == -100) ? "?" : a_iHigh);
    		}
    	}
    	if(a_sProbability.length() > 0) a_sProbability.append("%)");
    	
    	a_sProbability.append(a_oLinkage.getParentPositionsString());
    	
    	return a_sProbability.toString();
    }
}