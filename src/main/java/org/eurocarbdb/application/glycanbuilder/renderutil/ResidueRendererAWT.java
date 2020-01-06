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
import java.awt.geom.*;
import java.awt.image.*;
import java.util.ArrayList;
import java.awt.font.*;

import javax.swing.*;

import org.eurocarbdb.application.glycanbuilder.DefaultPaintable;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueStyle;
import org.eurocarbdb.application.glycanbuilder.ResidueStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.eurocarbdb.application.glycanbuilder.util.GraphicUtils;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;
import org.glycoinfo.application.glycanbuilder.util.GlycanUtils;

/**
   Objects of this class are used to create a graphical representation
   of a {@link Residue} object given the current graphic options
   ({@link GraphicOptions}. The rules to draw the residue in the
   different notations are stored in the {@link
   ResidueStyleDictionary}.   

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class ResidueRendererAWT extends AbstractResidueRenderer {

	boolean showAnom = true;
	
	public ResidueRendererAWT() {

    }

    public ResidueRendererAWT(GlycanRenderer src) {
    	super(src);
    } 
	
    @Override
	public Icon getIcon(ResidueType type, int max_y_size) {
    	int orientation = theGraphicOptions.ORIENTATION;
    	theGraphicOptions.ORIENTATION = GraphicOptions.RL;
    
    	//compute bounding box
    	Residue node = new Residue(type);
    	Rectangle bbox = computeBoundingBox(node,false,4,4,new ResAngle(),max_y_size-8,max_y_size-8);

    	//Create an image that supports transparent pixels
    	BufferedImage img = GraphicUtils.createCompatibleImage(bbox.width+8, bbox.height+8, false);

    	//create a graphic context
    	Graphics2D g2d = img.createGraphics();
    	g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);    
    	g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);    
    	g2d.setBackground(new Color(255,255,255,0));

    	//paint the residue
    	paint(new DefaultPaintable(g2d),node,false,false,null,bbox,null,new ResAngle());

    	theGraphicOptions.ORIENTATION = orientation;
    	return new ImageIcon(img);
    }
	
	public BufferedImage getBufferedImage(ResidueType type, int max_y_size, boolean a_bIsShowAnom) {
    	int orientation = theGraphicOptions.ORIENTATION;
        theGraphicOptions.ORIENTATION = GraphicOptions.RL;
        
        this.showAnom = a_bIsShowAnom;
        
        //compute bounding box
        Residue node = new Residue(type);
        Rectangle bbox = computeBoundingBox(node,false,4,4,new ResAngle(),max_y_size-8,max_y_size-8);
        
        //Create an image that supports transparent pixels
        BufferedImage img = GraphicUtils.createCompatibleImage(bbox.width+8, bbox.height+8, false);

        //create a graphic context
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);    
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);    
        g2d.setBackground(new Color(255,255,255,0));

        //paint the residue
        paint(new DefaultPaintable(g2d),node,false,false,null,bbox,null,new ResAngle());

        theGraphicOptions.ORIENTATION = orientation;        
   
        this.showAnom = true;
        
        return img;
    }
	
    @Override
	public Image getImage(ResidueType type, int max_y_size) {     
    	return Toolkit.getDefaultToolkit().createImage(getBufferedImage(type,max_y_size, false).getSource());
    }
    
    @Override
	public void paint(Paintable paintable, Residue node, boolean selected, boolean active, boolean on_border, Rectangle par_bbox, Rectangle cur_bbox, Rectangle sup_bbox, ResAngle orientation) {
    	if( node==null ) return;
    	
    	Graphics2D g2d=paintable.getGraphics2D();
    	ResidueStyle style = theResidueStyleDictionary.getStyle(node);
    	
    	//check ring size for SNFG
    	boolean isSNFG = (!this.checkComposiiton(node).isEmpty());
    	
    	//draw shape
    	Point pp = ( par_bbox!=null ) ?center(par_bbox) :center(cur_bbox);
    	Point ps = ( sup_bbox!=null ) ?center(sup_bbox) :center(cur_bbox);    	
    	Shape shape = createShape(node,par_bbox,cur_bbox,sup_bbox,orientation,style);
    	Shape text_shape = createTextShape(node,par_bbox,cur_bbox,sup_bbox,orientation,style);
    	Shape fill_shape  = createFillShape(node,cur_bbox,style,orientation, angle(pp,ps));
    	
    	Color shape_color  = style.getShapeColor();
    	Color fill_color   = style.getFillColor();
    	Color text_color = style.getTextColor();
    	if( selected )
    		fill_color = new Color(sig(fill_color.getRed()),sig(fill_color.getGreen()),sig(fill_color.getBlue()));
    	if( !active ) {
    		shape_color = new Color(sig(shape_color.getRed()),sig(shape_color.getGreen()),sig(shape_color.getBlue()));
    		fill_color = new Color(sig(fill_color.getRed()),sig(fill_color.getGreen()),sig(fill_color.getBlue()));
    		text_color = new Color(sig(text_color.getRed()),sig(text_color.getGreen()),sig(text_color.getBlue()));
    	}

    	if( shape!=null && !on_border ) {      
    		if( fill_shape!=null ) {
    			Object old_hint = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);    
    			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);

    			Shape old_clip = g2d.getClip();
    			g2d.clip(shape);

    			g2d.setColor((style.isFillNegative()) ?fill_color :Color.white);
    			g2d.fill(shape);

    			g2d.setColor((style.isFillNegative()) ?Color.white :fill_color);
    			g2d.fill(fill_shape);

    			if( old_hint!=null )
    			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,old_hint);

    			g2d.setColor(shape_color);        
    			g2d.draw(fill_shape);

    			g2d.setClip(old_clip);
    		}

    		//draw contour
    		g2d.setStroke( (selected) ?new BasicStroke(2) :new BasicStroke(1));
    		g2d.setColor(shape_color);
    		g2d.draw(shape);
    		g2d.setStroke(new BasicStroke(1));
    	}
    	else if( selected ) {
    		//draw selected contour for empty shape
    		float[] dashes = {5.f,5.f};
    		g2d.setStroke(new BasicStroke(2.f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,1.f,dashes,0.f));
    		g2d.setColor(shape_color);        
    		g2d.draw(cur_bbox);
    		g2d.setStroke(new BasicStroke(1));
    	}

    	//add text shape
    	if( text_shape!=null ) {
    		g2d.setColor(shape_color);
    		g2d.fill(text_shape);
    	}

    	//draw text
    	if( shape==null || on_border || style.getText()!=null || isSNFG) {
    		if( shape==null || on_border )
    			orientation = theGraphicOptions.getOrientationAngle();
    		else if( style.getText()!=null )
    			orientation = new ResAngle(0);

    		String text = getText(node,on_border);
    		if(isSNFG) {
    			ArrayList<String> a_aConfs = this.checkComposiiton(node);
    			if(shape != null) {
    				if(a_aConfs.size() == 1) text = a_aConfs.get(0);
    				if(a_aConfs.size() == 2) text = a_aConfs.get(0) + a_aConfs.get(1);
    				if(a_aConfs.size() == 3) text = a_aConfs.get(0) + a_aConfs.get(1) + a_aConfs.get(2);
    			}else {
    				for(String a_sConf : a_aConfs) {
    					if(a_sConf.equals("p") || a_sConf.equals("f")) text = text + a_sConf;
    					if(a_sConf.equals("D") || a_sConf.equals("L")) text = a_sConf + text;
    				}
    			}
    		}
    		
    		//set font
    		//int font_size = sat(9*cur_bbox.width/text.length()/10,theGraphicOptions.NODE_FONT_SIZE);
    		
    		int font_size = (isSNFG && text.length() < 3) ? 10 : theGraphicOptions.NODE_FONT_SIZE;
    		int x_size    = textBounds(text,theGraphicOptions.NODE_FONT_FACE,font_size).width;
    		if( shape!=null ) 
    			font_size = sat(8 * font_size * cur_bbox.width / x_size / 10,font_size);

    		Font new_font = null;
    		if(isSNFG && (node.isAlditol() && text.equals("o")) || (node.isAldehyde() && text.equals("a"))) {
    			new_font = new Font(theGraphicOptions.NODE_FONT_FACE, Font.ITALIC, font_size);
    		}else
	    		new_font = new Font(theGraphicOptions.NODE_FONT_FACE,Font.PLAIN,font_size);
    		
    		Font old_font = g2d.getFont();
    		g2d.setFont(new_font);

    		//compute bounding rect
    		Rectangle2D.Double text_bound = new Rectangle2D.Double();
    		text_bound.setRect(new TextLayout(text,new_font,g2d.getFontRenderContext()).getBounds());

    		//draw text
    		g2d.setColor(text_color);    
    		if( orientation.equals(0) || orientation.equals(180) ) {
    			Rectangle2D.Double text_rect = new Rectangle2D.Double(midx(cur_bbox)-text_bound.width/2,midy(cur_bbox)-text_bound.height/2,text_bound.width,text_bound.height);
    			if( shape==null || fill_shape==null ) 
    				g2d.clearRect((int)text_rect.x,(int)text_rect.y,(int)text_rect.width,(int)text_rect.height);
    			g2d.drawString(text,(int)text_rect.x,(int)(text_rect.y+text_rect.height));
    		}
    		if(orientation.equals(-90) || orientation.equals(90)) {
    			if(isSNFG) {
    				Rectangle2D.Double text_rect = new Rectangle2D.Double(midx(cur_bbox)-text_bound.width/2,midy(cur_bbox)-text_bound.height/2,text_bound.width,text_bound.height);
        			if( shape==null || fill_shape==null ) 
        				g2d.clearRect((int)text_rect.x,(int)text_rect.y,(int)text_rect.width,(int)text_rect.height);
        			g2d.drawString(text,(int)text_rect.x,(int)(text_rect.y+text_rect.height));
    			} else {
    				Rectangle2D.Double text_rect = new Rectangle2D.Double(midx(cur_bbox)-text_bound.height/2,midy(cur_bbox)-text_bound.width/2,text_bound.height,text_bound.width);
    				if( shape==null || fill_shape==null ) 
    					g2d.clearRect((int)text_rect.x,(int)text_rect.y,(int)text_rect.width,(int)text_rect.height);        
    				
    				g2d.rotate(-Math.PI/2.0); 
    				g2d.drawString(text,-(int)(text_rect.y+text_rect.height),(int)(text_rect.x+text_rect.width));
    				g2d.rotate(+Math.PI/2.0);     				
    			}
    		}

    		g2d.setFont(old_font);
    	}
    	
    	boolean a_bIsShow = false;
    	if(node.isSaccharide()) a_bIsShow = GlycanUtils.isFacingAnom(node);
    	if(!theGraphicOptions.SHOW_REDEND_CANVAS && node.isSaccharide()) {
    		a_bIsShow = (node.getTreeRoot().firstChild().equals(node) && !node.isAlditol() && !node.getTreeRoot().isBracket());
    	}
    	
    	// draw anomeric state
    	if(shape != null && a_bIsShow == true) 
    		showAnomericState(g2d, node, orientation, cur_bbox);
    	
    	g2d.setColor(Color.black);
    	//g2d.drawString(""+node.id,left(cur_bbox),bottom(cur_bbox));
    }
    
    private Graphics2D showAnomericState(Graphics2D g2d, Residue node, ResAngle orientation, Rectangle cur_bbox) {
    	String anomString = TextUtils.toGreek(node.getAnomericState());
		Font oldFont = g2d.getFont();
		Font newFont = new Font(theGraphicOptions.LINKAGE_INFO_FONT_FACE,Font.PLAIN,theGraphicOptions.LINKAGE_INFO_SIZE);
		Rectangle2D.Double text_bound = new Rectangle2D.Double();

		g2d.setFont(newFont);
		text_bound.setRect(new TextLayout(TextUtils.toGreek(node.getAnomericState()),newFont,g2d.getFontRenderContext()).getBounds());

		Rectangle2D.Double txtRect = null;
		int margine = 0;
		if(orientation.equals(0) || orientation.equals(180)) {
			margine = (orientation.equals(0)) ? -15 : 15;
			txtRect = new Rectangle2D.Double(midx(cur_bbox)-text_bound.width/2,midy(cur_bbox)-text_bound.height/2,text_bound.width,text_bound.height);
			g2d.drawString(anomString, (int)txtRect.x + margine, (int)(txtRect.y + txtRect.height));
		} else {
			margine = (orientation.equals(90)) ? -15 : 15;
			txtRect = new Rectangle2D.Double(midx(cur_bbox)-text_bound.height/2,midy(cur_bbox)-text_bound.width/2,text_bound.height,text_bound.width);

			g2d.rotate(-Math.PI/2.0); 
			g2d.drawString(anomString,-(int)(txtRect.y+txtRect.height + margine),(int)(txtRect.x+txtRect.width));
			g2d.rotate(+Math.PI/2.0); 
		}
		
		g2d.setFont(oldFont);
		
		return g2d;
    }
}