package org.eurocarbdb.application.glycanbuilder.renderutil;

import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.angle;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.center;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.getTextShape;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.isDown;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.isLeft;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.isUp;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.textBounds;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueStyle;
import org.eurocarbdb.application.glycanbuilder.ResidueStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

public abstract class AbstractResidueRenderer implements ResidueRenderer{
	protected ResidueStyleDictionary theResidueStyleDictionary; 
    protected GraphicOptions theGraphicOptions;

    public AbstractResidueRenderer() {
    	theResidueStyleDictionary = new ResidueStyleDictionary();
    	theGraphicOptions = new GraphicOptions();
    }

    public AbstractResidueRenderer(GlycanRenderer src) {
    	theResidueStyleDictionary = src.getResidueStyleDictionary();
    	theGraphicOptions = src.getGraphicOptions();
    }

    @Override
	public GraphicOptions getGraphicOptions() {
    	return theGraphicOptions;
    }

    @Override
	public void setGraphicOptions(GraphicOptions opt) {
    	theGraphicOptions = opt;
    }   

    @Override
	public ResidueStyleDictionary getResidueStyleDictionary() {
    	return theResidueStyleDictionary; 
    }

    @Override
	public void setResidueStyleDictionary(ResidueStyleDictionary residueStyleDictionary) {
    	theResidueStyleDictionary = residueStyleDictionary;
    }

    @Override
	public String getText(Residue node) {
    	if( node==null ) 
    		return "";
    	
    	ResidueType  type  = node.getType();
    	ResidueStyle style = 
    			theResidueStyleDictionary.getStyle(node);
    	String text = style.getText();    
    	
    	return (text!=null) ?text :type.getResidueName();
    }

    @Override
	public String getText(Residue node, boolean on_border) {
    	// special cases
    	if( node==null )
    		return "";
    	if( on_border && node.isSpecial() && !node.isLCleavage() )
    		return "*";

    	// get text
    	String text = null;
    	if( on_border && node.isLCleavage() ) 
    		text = getText(node.getCleavedResidue());
    	else
    		text = getText(node);
    	
    	// add linkage 
    	if( on_border && /*!node.getParentLinkage().hasUncertainParentPositions() &&*/ theGraphicOptions.SHOW_INFO ) {
    		text =  node.getParentLinkage().getParentPositionsString() + text;
    	}

    	// add probability
    	if(node.hasParent()) {
    		text = this.createProbability(node.getParentLinkage()) + text;
    	}
    		
    	// add brackets for cleavages    
    	if( on_border && node.isLCleavage() ) 
    		text = "(" + text + ")";
	
    	return text;
    }

    public Rectangle computeBoundingBox(Residue node, boolean on_border, int x, int y, ResAngle orientation, int node_size, int max_y_size) {    
    	// get style
    	ResidueStyle style = theResidueStyleDictionary.getStyle(node);
    	String shape = style.getShape();

    	// compute dimensions
    	if( max_y_size<node_size )
    		node_size = max_y_size;
    	//if( shape==null )    
    	
    	Dimension dim;    
    	if( shape==null || on_border ) {
    		String text = getText(node,on_border);

    		int font_size = theGraphicOptions.NODE_FONT_SIZE;
    		int x_size = textBounds(text,theGraphicOptions.NODE_FONT_FACE,font_size).width;

    		if( x_size > node_size  )         
    			dim = new Dimension(x_size,node_size);        
    		else
    			dim = new Dimension(node_size,node_size);    

    		orientation = theGraphicOptions.getOrientationAngle();
    	}
    	else if( shape.equals("startrep") || shape.equals("endrep") ) {
    		int size = Math.min(node_size*2,max_y_size);
    		int font_size = theGraphicOptions.LINKAGE_INFO_SIZE;
    		dim = new Dimension(size/2,size+2*font_size);
    	}else if( shape.equals("point") )
    		dim = new Dimension(1,1);
    	else
    		dim = new Dimension(node_size,node_size);

    	// return bounding box
    	if( orientation.equals(0) || orientation.equals(180) )
    		return new Rectangle(x,y,dim.width,dim.height);
    	return new Rectangle(x,y,dim.height,dim.width);
    }

    protected static int sat(int v, int t) {
    	if( v>t )
    		return t;
    	return v;
    }

    protected static int sig(int v) {
    	return 128+v/2;
    }   

    @Override
	public void paint(Paintable paintable, Residue node, boolean selected, boolean on_border, Rectangle par_bbox, Rectangle cur_bbox, Rectangle sup_bbox, ResAngle orientation) {
    	paint(paintable,node,selected,true,on_border,par_bbox,cur_bbox,sup_bbox,orientation);
    }

    @Override
	abstract public void paint(Paintable paintable, Residue node, boolean selected, boolean active, boolean on_border, Rectangle par_bbox, Rectangle cur_bbox, Rectangle sup_bbox, ResAngle orientation);   

    static private Polygon createDiamond(double x, double y, double w, double h) {
    	if( (w%2)==1 )
    		w++;
    	if( (h%2)==1 )
    		h++;

    	Polygon p = new Polygon();
    	p.addPoint((int)(x+w/2), (int)(y));
    	p.addPoint((int)(x+w),   (int)(y+h/2));
    	p.addPoint((int)(x+w/2), (int)(y+h));
    	p.addPoint((int)(x),     (int)(y+h/2));
    	return p;
    }

    static private Shape createHatDiamond(double angle, double x, double y, double w, double h) {
    	GeneralPath f = new GeneralPath();

    	// append diamond
    	f.append(createDiamond(x,y,w,h),false);

    	// append hat
    	Polygon p = new Polygon();
    	p.addPoint((int)(x-2),(int)(y+h/2-2));
    	p.addPoint((int)(x+w/2-2),(int)(y-2));
    	f.append(p,false);

    	return f;
    }

    static private Shape createRHatDiamond(double angle, double x, double y, double w, double h) {
    	GeneralPath f = new GeneralPath();

    	// append diamond
    	f.append(createDiamond(x,y,w,h),false);

    	// append hat
    	Polygon p = new Polygon();
    	p.addPoint((int)(x+w+2),(int)(y+h/2-2));
    	p.addPoint((int)(x+w/2+2),(int)(y-2));
    	f.append(p,false);

    	return f;
    }

    static private Polygon createRhombus(double x, double y, double w, double h) {
    	Polygon p = new Polygon();
    	p.addPoint((int)(x+0.50*w), (int)(y));
    	p.addPoint((int)(x+0.85*w), (int)(y+0.50*h));
    	p.addPoint((int)(x+0.50*w), (int)(y+h));
    	p.addPoint((int)(x+0.15*w), (int)(y+0.50*h));
    	return p;
    }

    static private Polygon createTriangle(double angle, double x, double y, double w, double h) {
    	Polygon p = new Polygon();
    	
    	if( angle>=-Math.PI/4. && angle<=Math.PI/4. ) {
    		//pointing right
    		p.addPoint((int)(x+w), (int)(y+h/2));
    		p.addPoint((int)(x),   (int)(y+h));
    		p.addPoint((int)(x),   (int)(y));
    	}
    	else if( angle>=Math.PI/4. && angle<=3.*Math.PI/4. ) {
    		//pointing down
    		p.addPoint((int)(x+w/2), (int)(y+h));
    		p.addPoint((int)(x),     (int)(y));
    		p.addPoint((int)(x+w),   (int)(y));
    	}
    	else if( angle>=-3.*Math.PI/4. && angle<=-Math.PI/4. ) {
    		// pointing up
    		p.addPoint((int)(x+w/2), (int)(y));
    		p.addPoint((int)(x+w),   (int)(y+h));
    		p.addPoint((int)(x),     (int)(y+h));
    	}
    	else {
    		//pointing left
    		p.addPoint((int)(x),   (int)(y+h/2));
    		p.addPoint((int)(x+w), (int)(y+h));
    		p.addPoint((int)(x+w), (int)(y));

    	}

    	return p;
    }
    
    static private Polygon createTopTriangle(int angle, double x, double y, double w, double h) {
    	Polygon p = new Polygon();
    	
    	if(angle == 0) {
    		// pointing up
    		p.addPoint((int)(x+w/2), (int)(y));
    		p.addPoint((int)(x+w),   (int)(y+h));
    		p.addPoint((int)(x),     (int)(y+h));
    	}
    	if(angle == 1) {
    		//pointing right
    		p.addPoint((int)(x+w), (int)(y+h/2));
    		p.addPoint((int)(x),   (int)(y+h));
    		p.addPoint((int)(x),   (int)(y));
    	}
    	if(angle == 2) {
    		//pointing down
    		p.addPoint((int)(x+w/2), (int)(y+h));
    		p.addPoint((int)(x),     (int)(y));
    		p.addPoint((int)(x+w),   (int)(y));
    	}
    	if(angle == 3) {
    		//pointing left
    		p.addPoint((int)(x),   (int)(y+h/2));
    		p.addPoint((int)(x+w), (int)(y+h));
    		p.addPoint((int)(x+w), (int)(y));
    	}
    	
    	return p;
    }

    static private Polygon createStar(double x, double y, double w, double h, int points) {
    	double rx = w/2.;
    	double ry = h/2.;
    	double cx = x+w/2.;
    	double cy = y+h/2.;

    	double step = Math.PI/(double)points;         
    	double nstep = Math.PI/2.-2.*step;

    	double mrx = rx/(Math.cos(step)+Math.sin(step)/Math.tan(nstep));
    	double mry = ry/(Math.cos(step)+Math.sin(step)/Math.tan(nstep));

    	Polygon p = new Polygon();
    	for(int i=0; i<=2*points; i++ ) {
    		if( (i%2)==0 ) 
    			p.addPoint((int)(cx+rx*Math.cos(i*step-Math.PI/2.)),(int)(cy+ry*Math.sin(i*step-Math.PI/2.)));
    		else
    			p.addPoint((int)(cx+mrx*Math.cos(i*step-Math.PI/2.)),(int)(cy+mry*Math.sin(i*step-Math.PI/2.)));
    	}        
    	return p;
    }    

    static private Polygon createPentagon(double x, double y, double w, double h) {
    	double rx = w/2.;
    	double ry = h/2.;
    	double cx = x+w/2.;
    	double cy = y+h/2.;

    	double step = Math.PI/2.5;        
    	Polygon p = new Polygon();
    	for(int i=0; i<=5; i++ ) {
    		p.addPoint((int)(cx+rx*Math.cos(i*step-Math.PI/2.)),(int)(cy+ry*Math.sin(i*step-Math.PI/2.)));
    	}        
    	return p;
    }

    static private Polygon createHexagon(double x, double y, double w, double h) {
    	double rx = w/2.;
    	double ry = h/2.;
    	double cx = x+w/2.;
    	double cy = y+h/2.;

    	double step = Math.PI/3.;        
    	Polygon p = new Polygon();
    	for(int i=0; i<=6; i++ ) {
    		p.addPoint((int)(cx+rx*Math.cos(i*step)),(int)(cy+ry*Math.sin(i*step)));
    	}        
    	return p;
    }

    static private Polygon createFlatHexagon(double x, double y, double w, double h) {
     	double rx = w/2.;
    	double ry = h/2.5;
    	double cx = x+w/2.;
    	double cy = y+h/2.;

    	double step = Math.PI/3.;        
    	Polygon p = new Polygon();
    	for(int i=0; i<=6; i++ ) {
    		p.addPoint((int)(cx+rx*Math.cos(i*step)),(int)(cy+ry*Math.sin(i*step)));
    	}        
    	return p;
    }
    
    static private Polygon createHeptagon(double x, double y, double w, double h) {
    	double rx = w/2.;
    	double ry = h/2.;
    	double cx = x+w/2.;
    	double cy = y+h/2.;

    	double step = Math.PI/3.5;        
    	Polygon p = new Polygon();
    	for(int i=0; i<=7; i++ ) {
    		p.addPoint((int)(cx+rx*Math.cos(i*step-Math.PI/2.)),(int)(cy+ry*Math.sin(i*step-Math.PI/2.)));
    	}        
    	return p;
    }

    static private Shape createLine(double angle, double x, double y, double w, double h) {    
    	double rx = w/2.;
    	double ry = h/2.;
    	double cx = x+w/2.;
    	double cy = y+h/2.;

    	Polygon p = new Polygon();

    	double x1 = cx+rx*Math.cos(angle-Math.PI/2.);
    	double y1 = cy+ry*Math.sin(angle-Math.PI/2.);
    	p.addPoint((int)x1,(int)y1);

    	double x2 = cx+rx*Math.cos(angle+Math.PI/2.);
    	double y2 = cy+ry*Math.sin(angle+Math.PI/2.);
    	p.addPoint((int)x2,(int)y2);

    	return p;
    }

    static private Shape createCleavage(double angle, double x, double y, double w, double h, boolean has_oxygen) {
    	GeneralPath f = new GeneralPath();

    	double rx = w/2.;
    	double ry = h/2.;
    	double cx = x+w/2.;
    	double cy = y+h/2.;

    	// create cut 
    	double x1 = cx+rx*Math.cos(angle+Math.PI/2.);
    	double y1 = cy+ry*Math.sin(angle+Math.PI/2.);
    	double x2 = cx+rx*Math.cos(angle-Math.PI/2.);
    	double y2 = cy+ry*Math.sin(angle-Math.PI/2.);
    	double x3 = x2+rx*Math.cos(angle);
    	double y3 = y2+ry*Math.sin(angle);

    	Polygon p = new Polygon();
    	p.addPoint((int)x1,(int)y1);
    	p.addPoint((int)x2,(int)y2);
    	p.addPoint((int)x3,(int)y3);
    	p.addPoint((int)x2,(int)y2);
    	f.append(p,false);

    	if( has_oxygen ) {
    		// create oxygen
    		double ox = cx+rx*Math.cos(angle);
    		double oy = cy+ry*Math.sin(angle);
    		Shape o = new Ellipse2D.Double(ox-rx/3.,oy-ry/3.,rx/1.5,ry/1.5);
    		f.append(o,false);    
    	}

    	return f;
    }

    static private Shape createCrossRingCleavage(double angle, double x, double y, double w, double h, int first_pos, int last_pos) {
    	//return createArc(x,y,w,h,first_pos,last_pos);

    	GeneralPath c = new GeneralPath();
    	//c.append(createLine(0,x,y,w,h),false);

    	// add hexagon
    	c.append(createHexagon(x+1,y+1,w-2,h-2),false);
    	//return c;       

    	// add line    
    	double rx = w/2.;
    	double ry = h/2.;
    	double cx = x+w/2.;
    	double cy = y+h/2.;

    	Polygon p1 = new Polygon();
    	p1.addPoint((int)cx,(int)cy);
    	p1.addPoint((int)(cx+1.2*rx*Math.cos(angle+first_pos*Math.PI/3-Math.PI/6)),
    			(int)(cy+1.2*ry*Math.sin(angle+first_pos*Math.PI/3-Math.PI/6)));
    	c.append(p1,false);

    	Polygon p2 = new Polygon();
    	p2.addPoint((int)cx,(int)cy);
    	p2.addPoint((int)(cx+1.2*rx*Math.cos(angle+last_pos*Math.PI/3-Math.PI/6)),
    			(int)(cy+1.2*ry*Math.sin(angle+last_pos*Math.PI/3-Math.PI/6)));
    	c.append(p2,false);
    	return c;
    }

    static private Shape createEnd(double angle, double x, double y, double w, double h) {
    	double rx = w/2.;
    	double ry = h/2.;
    	double cx = x+w/2.;
    	double cy = y+h/2.;

    	// start point
    	double x1 = cx+rx*Math.cos(angle-Math.PI/2.);
    	double y1 = cy+ry*Math.sin(angle-Math.PI/2.);

    	// end point
    	double x2 = cx+rx*Math.cos(angle+Math.PI/2.);
    	double y2 = cy+ry*Math.sin(angle+Math.PI/2.);

    	// ctrl point 1
    	double cx1 = cx+0.5*rx*Math.cos(angle-Math.PI/2.);
    	double cy1 = cy+0.5*ry*Math.sin(angle-Math.PI/2.);
    	double tx1 = cx1+0.5*rx*Math.cos(angle-Math.PI);
    	double ty1 = cy1+0.5*ry*Math.sin(angle-Math.PI);

    	// ctrl point 2
    	double cx2 = cx+0.5*rx*Math.cos(angle+Math.PI/2.);
    	double cy2 = cy+0.5*ry*Math.sin(angle+Math.PI/2.);
    	double tx2 = cx2+0.5*rx*Math.cos(angle);
    	double ty2 = cy2+0.5*ry*Math.sin(angle);    

    	return new CubicCurve2D.Double(x1,y1,tx1,ty1,tx2,ty2,x2,y2);
    }
    
    static private Shape createBracket(double angle, double x, double y, double w, double h) {        
    	double rx = w/2.;
    	double ry = h/2.;
    	double cx = x+w/2.;
    	double cy = y+h/2.;

    	// first start point
    	double x11 = cx+rx*Math.cos(angle-Math.PI/2.)+0.2*rx*Math.cos(angle);
    	double y11 = cy+ry*Math.sin(angle-Math.PI/2.)+0.2*ry*Math.sin(angle);

    	// first ctrl point 1
    	double tx11 = cx+0.9*rx*Math.cos(angle-Math.PI/2.)+0.2*rx*Math.cos(angle-Math.PI);
    	double ty11 = cy+0.9*ry*Math.sin(angle-Math.PI/2.)+0.2*ry*Math.sin(angle-Math.PI);

    	// first ctrl point 2;
    	double tx21 = cx+0.1*rx*Math.cos(angle-Math.PI/2.)+0.2*rx*Math.cos(angle);
    	double ty21 = cy+0.1*ry*Math.sin(angle-Math.PI/2.)+0.2*ry*Math.sin(angle);

    	// first end point
    	double x21 = cx+0.2*rx*Math.cos(angle-Math.PI);
    	double y21 = cy+0.2*ry*Math.sin(angle-Math.PI);

    	// first shape
    	Shape s1 = new CubicCurve2D.Double(x11,y11,tx11,ty11,tx21,ty21,x21,y21);

    	// second start point
    	double x12 = cx+rx*Math.cos(angle+Math.PI/2.)+0.2*rx*Math.cos(angle);
    	double y12 = cy+ry*Math.sin(angle+Math.PI/2.)+0.2*ry*Math.sin(angle);

    	// second ctrl point 1
    	double tx12 = cx+0.9*rx*Math.cos(angle+Math.PI/2.)+0.2*rx*Math.cos(angle-Math.PI);
    	double ty12 = cy+0.9*ry*Math.sin(angle+Math.PI/2.)+0.2*ry*Math.sin(angle-Math.PI);

    	// second ctrl point 2;
    	double tx22 = cx+0.1*rx*Math.cos(angle+Math.PI/2.)+0.2*rx*Math.cos(angle);
    	double ty22 = cy+0.1*ry*Math.sin(angle+Math.PI/2.)+0.2*ry*Math.sin(angle);

    	// second end point
    	double x22 = cx+0.2*rx*Math.cos(angle-Math.PI);
    	double y22 = cy+0.2*ry*Math.sin(angle-Math.PI);

    	// second shape
    	Shape s2 = new CubicCurve2D.Double(x12,y12,tx12,ty12,tx22,ty22,x22,y22);

    	// generate bracket
    	GeneralPath b = new GeneralPath();
    	b.append(s1,false);    
    	b.append(s2,false);    
    	return b;
    }

    private Shape createRepetition(double angle, double x, double y, double w, double h) {    
    	double r = Math.min(w,h);
    	double cx = x+w/2.;
    	double cy = y+h/2.;

    	//-----
    	// create shape
    	Polygon p = new Polygon();

    	// first point
    	double x1 = cx+r*Math.cos(angle-Math.PI/2.)+r/4.*Math.cos(angle+Math.PI);
    	double y1 = cy+r*Math.sin(angle-Math.PI/2.)+r/4.*Math.sin(angle+Math.PI);
    	p.addPoint((int)x1,(int)y1);

    	// second point
    	double x2 = cx+r*Math.cos(angle-Math.PI/2.);
    	double y2 = cy+r*Math.sin(angle-Math.PI/2.);
    	p.addPoint((int)x2,(int)y2);

    	// third point
    	double x3 = cx+r*Math.cos(angle+Math.PI/2.);
    	double y3 = cy+r*Math.sin(angle+Math.PI/2.);
    	p.addPoint((int)x3,(int)y3);

    	// fourth point
    	double x4 = cx+r*Math.cos(angle+Math.PI/2.)+r/4.*Math.cos(angle+Math.PI);
    	double y4 = cy+r*Math.sin(angle+Math.PI/2.)+r/4.*Math.sin(angle+Math.PI);
    	p.addPoint((int)x4,(int)y4);

    	// close shape
    	p.addPoint((int)x3,(int)y3);
    	p.addPoint((int)x2,(int)y2);


    	return p;
    }
    
    protected Shape createCyclic(ResAngle a_oAngle, double x, double y, double w, double h) {
    	double a_dXpoint = x;
    	double a_dCtrlX = 0.0;
    	double a_dCtrlY = 0.0;
    	
    	Shape s1 = null;
    	
    	/** R2L 180*/
    	if(a_oAngle.getIntAngle() == 0) {
    		a_dCtrlX = a_dXpoint + w;
    		a_dCtrlY = (y-h*.5 + y+h*1.5)*.5;
    		s1 = new QuadCurve2D.Double(a_dXpoint, y-h*.5, a_dCtrlX, a_dCtrlY, a_dXpoint, y+h*1.5);
    	}
    	
    	if(a_oAngle.getIntAngle() == 180) {
    		a_dCtrlX = a_dXpoint;
    		a_dCtrlY = (y-h*.5 + y+h*1.5)*.5;
    		a_dXpoint = a_dXpoint + w;
    		s1 = new QuadCurve2D.Double(a_dXpoint, y-h*.5, a_dCtrlX, a_dCtrlY, a_dXpoint, y+h*1.5);    		
    	}
    	    	
    	if(a_oAngle.getIntAngle() == 90) {
    		a_dCtrlX = (a_dXpoint*2 + 23)*.5;
    		a_dCtrlY = y + h;
    		s1 = new QuadCurve2D.Double(a_dXpoint+34, y, a_dCtrlX, a_dCtrlY, a_dXpoint-11, y);
    	}
    	
    	if(a_oAngle.getIntAngle() == -90) {
    		a_dCtrlX = (a_dXpoint*2 + 23)*.5;
    		a_dCtrlY = y;
    		s1 = new QuadCurve2D.Double(a_dXpoint+34, y+h, a_dCtrlX, a_dCtrlY, a_dXpoint-11, y+h);
    	}
    
    	return s1;
    }
    
    protected Shape createShape(Residue node, Rectangle par_bbox, Rectangle cur_bbox, Rectangle sup_bbox, ResAngle orientation, ResidueStyle style) {
    	String shape = style.getShape();

    	if( shape==null || shape.equals("none") || shape.equals("-") )
    		return null;
    	
    	double x = (double)cur_bbox.getX();
    	double y = (double)cur_bbox.getY();
    	double w = (double)cur_bbox.getWidth();
    	double h = (double)cur_bbox.getHeight();
    	
    	// non-oriented shapes
    	if( shape.equals("point") )
    		return new Rectangle2D.Double(x+w/2.,y+h/2.,0,0);    
    	if( shape.equals("square") )
    		return new Rectangle2D.Double(x,y,w,h);    
    	if( shape.equals("circle") ) 
    		return new Ellipse2D.Double(x,y,w,h);    
    	if( shape.equals("diamond") ) 
    		return createDiamond(x,y,w,h);            
    	if( shape.equals("rhombus") ) 
    		return createRhombus(x,y,w,h);            
    	if( shape.equals("star") ) 
    		return createStar(x,y,w,h,5);    
    	if( shape.equals("sixstar") ) 
    		return createStar(x,y,w,h,6);    
    	if( shape.equals("sevenstar") ) 
    		return createStar(x,y,w,h,7);    
    	if( shape.equals("pentagon") ) 
    		return createPentagon(x,y,w,h);            
    	if( shape.equals("hexagon") ) 
    		return createHexagon(x,y,w,h);  
    	if( shape.equals("flathexagon"))
    		return createFlatHexagon(x,y,w,h);
    	if( shape.equals("heptagon") ) 
    		return createHeptagon(x,y,w,h);            
    	if( shape.equals("flatsquare"))
    		return new Rectangle2D.Double(x,y+h*.25,w,h*.5);
    	
    	Point pp = ( par_bbox!=null ) ?center(par_bbox) :center(cur_bbox);
    	Point pc = center(cur_bbox);
    	Point ps = ( sup_bbox!=null ) ?center(sup_bbox) :center(cur_bbox);
    	
    	// partially oriented shapes
    	if( shape.equals("triangle") ) {
    		if(this.theGraphicOptions.NOTATION.equals(GraphicOptions.NOTATION_SNFG)) {
    			if(node.getWasSticky()) {
    				if(orientation.getIntAngle() == 180) {
        				return createTopTriangle(theGraphicOptions.ORIENTATION,x,y,w,h);
    				}else
    					return createTriangle(angle(pp,ps),x,y,w,h);
    			}else {
    				return createTopTriangle(theGraphicOptions.ORIENTATION,x,y,w,h);
    			}
    		}else
    			return createTriangle(angle(pp,ps),x,y,w,h);
    	}
    	if( shape.equals("hatdiamond") ) 
    		return createHatDiamond(angle(pp,ps),x,y,w,h);            
    	if( shape.equals("rhatdiamond") ) 
    		return createRHatDiamond(angle(pp,ps),x,y,w,h);            
    
    	if( shape.equals("bracket") ) 
    		return createBracket(orientation.opposite().getAngle(),x,y,w,h);
    	if( shape.equals("startrep") ) 
    		return createRepetition(orientation.opposite().getAngle(),x,y,w,h);
    	if( shape.equals("endrep") ) 
    		return createRepetition(orientation.getAngle(),x,y,w,h);
    	if( shape.equals("startcyclic"))
    		return createCyclic(orientation, x, y, w, h);
    	if( shape.equals("endcyclic"))
    		return createCyclic(orientation.opposite(), x, y, w, h);
    	if( shape.equals("startalt"))
    		return createBracket(orientation.getAngle(),x,y,w,h);
    	if( shape.equals("endalt"))
    		return createBracket(orientation.opposite().getAngle(),x,y,w,h);    	
    		
    	// totally oriented shapes
    	if( shape.startsWith("acleavage") ) {
    		LinkedList<String> tokens = TextUtils.tokenize(shape,"_");
    		int first_pos = Integer.parseInt(tokens.get(1));
    		int last_pos  = Integer.parseInt(tokens.get(2));
    		return createCrossRingCleavage(angle(pc,ps),x,y,w,h,first_pos,last_pos);
    	}
    	if( shape.equals("bcleavage") ) 
    		return createCleavage(angle(ps,pc),x,y,w,h,false);
    	if( shape.equals("ccleavage") ) 
    		return createCleavage(angle(ps,pc),x,y,w,h,true);

    	if( shape.startsWith("xcleavage") ) {
    		LinkedList<String> tokens = TextUtils.tokenize(shape,"_");
    		int first_pos = Integer.parseInt(tokens.get(1));
    		int last_pos  = Integer.parseInt(tokens.get(2));
    		return createCrossRingCleavage(angle(pp,pc),x,y,w,h,first_pos,last_pos);
    	}
    	if( shape.equals("ycleavage") ) 
    		return createCleavage(angle(pp,pc),x,y,w,h,true);
    	if( shape.equals("zcleavage") ) 
    		return createCleavage(angle(pp,pc),x,y,w,h,false);

    	if( shape.equals("end") ) 
    		return createEnd(angle(pp,ps),x,y,w,h);

    	return cur_bbox;
    }


    private Shape createRepetitionText(double angle, double x, double y, double w, double h, int min, int max) {    
    	double r = Math.min(w,h);
    	double cx = x+w/2.;
    	double cy = y+h/2.;

    	double x2 = cx+r*Math.cos(angle-Math.PI/2.);
    	double y2 = cy+r*Math.sin(angle-Math.PI/2.);
    	double x3 = cx+r*Math.cos(angle+Math.PI/2.);
    	double y3 = cy+r*Math.sin(angle+Math.PI/2.);


    	GeneralPath ret = new GeneralPath();

    	if(min == max && (min != -1 && max != -1)) min = -1;
    	
    	//--------
    	// add min repetition
    	if( min>=0 || max>=0 || (min == -1 && max == -1)) {
    		String text = (min>=0) ?""+min :"n";
    		Dimension tb = textBounds(text,theGraphicOptions.LINKAGE_INFO_FONT_FACE,theGraphicOptions.LINKAGE_INFO_SIZE);

    		double dist = (isUp(angle) || isDown(angle)) ?tb.width/2+4 :tb.height/2+4;
    		double xmin,ymin;
    		if( isLeft(angle) || isUp(angle) ) {
    			xmin = x2+dist*Math.cos(angle-Math.PI/2.)-tb.width/2.;
    			ymin = y2+dist*Math.sin(angle-Math.PI/2.)+tb.height/2.;
    		}
    		else {
    			xmin = x3+dist*Math.cos(angle+Math.PI/2.)-tb.width/2.;
    			ymin = y3+dist*Math.sin(angle+Math.PI/2.)+tb.height/2.;
    		}

    		ret.append(getTextShape(xmin,ymin,text,theGraphicOptions.LINKAGE_INFO_FONT_FACE,theGraphicOptions.LINKAGE_INFO_SIZE),false);
    	}


    	//--------
    	// add max repetition    
    	if( min>=0 || max>=0 ) {
    		String text = (max>=0) ?""+max :"+inf";
    		Dimension tb = textBounds(text,theGraphicOptions.LINKAGE_INFO_FONT_FACE,theGraphicOptions.LINKAGE_INFO_SIZE);

    		double dist = (isUp(angle) || isDown(angle)) ?tb.width/2+4 :tb.height/2+4;
    		double xmax,ymax;
    		if( isLeft(angle) || isUp(angle) ) {
    			xmax = x3+dist*Math.cos(angle+Math.PI/2.)-tb.width/2.;
    			ymax = y3+dist*Math.sin(angle+Math.PI/2.)+tb.height/2.;
    		}
    		else {
    			xmax = x2+dist*Math.cos(angle-Math.PI/2.)-tb.width/2.;
    			ymax = y2+dist*Math.sin(angle-Math.PI/2.)+tb.height/2.;
    		}

    		ret.append(getTextShape(xmax,ymax,text,theGraphicOptions.LINKAGE_INFO_FONT_FACE,theGraphicOptions.LINKAGE_INFO_SIZE),false);
    	}

    	return ret;
    }

    protected Shape createTextShape(Residue node, Rectangle par_bbox, Rectangle cur_bbox, Rectangle sup_bbox, ResAngle orientation, ResidueStyle style) {
    	//ResidueStyle style = theResidueStyleDictionary.getStyle(node);
    	String shape = style.getShape();

    	if( shape==null || shape.equals("none") || shape.equals("-") )
    		return null;

    	double x = (double)cur_bbox.getX();
    	double y = (double)cur_bbox.getY();
    	double w = (double)cur_bbox.getWidth();
    	double h = (double)cur_bbox.getHeight();

    	if( shape.equals("endrep")) {
    		return createRepetitionText(orientation.getAngle(),x,y,w,h,node.getMinRepetitions(),node.getMaxRepetitions());    
    	}
    	
    	return null;
    }

    //--------------
    // Fill

    static private Shape createTriangle(double x1, double y1, double x2, double y2, double x3, double y3) {
    	Polygon p = new Polygon();
    	p.addPoint((int)x1,(int)y1);
    	p.addPoint((int)x2,(int)y2);
    	p.addPoint((int)x3,(int)y3);
    	return p;
    }
    
    static private Shape createHalf(double rx, double ry, double cx, double cy) {
    	double step = Math.PI/2.5;        
    	Polygon p = new Polygon();
    	p.addPoint((int)((cx-.5)+rx*Math.cos(5*step-Math.PI/2.)),(int)(cy+ry*Math.sin(5*step-Math.PI/2.)));
    	p.addPoint((int)((cx-.5)+rx*Math.cos(4*step-Math.PI/2.)),(int)(cy+ry*Math.sin(4*step-Math.PI/2.)));
    	p.addPoint((int)((cx-.5)+rx*Math.cos(3*step-Math.PI/2.)),(int)(cy+ry*Math.sin(3*step-Math.PI/2.)));
    	p.addPoint((int)((cx-.5)+rx*Math.cos(2.5*step-Math.PI/2.)),(int)(cy+ry*Math.sin(3*step-Math.PI/2.)));
    	return p;
    }
    
    static private Shape createCheckered(double x, double y, double w, double h) {
    	GeneralPath c = new GeneralPath();
    	c.append(new Rectangle2D.Double(x+w/2.,y,w/2.,h/2.),false);    
    	c.append(new Rectangle2D.Double(x,y+h/2.,w/2.,h/2.),false);    
    	return c;
    }

    static private Shape createArc(double x, double y, double w, double h, int start_pos, int end_pos) {    
    	return new Arc2D.Double(x-0.5*w,y-0.5*h,2*w,2*h,-end_pos*60.+30.,-((start_pos-end_pos+6)%6)*60.,Arc2D.PIE);
    }
    
    protected Shape createFillShape(Residue node, Rectangle cur_bbox, ResidueStyle style, ResAngle orientation, double a_dAngle) {    
    	//ResidueStyle style = theResidueStyleDictionary.getStyle(node);
    	String fillstyle   = style.getFillStyle();

    	double x = (double)cur_bbox.x;
    	double y = (double)cur_bbox.y;
    	double w = (double)cur_bbox.width;
    	double h = (double)cur_bbox.height;
    	
    	if(node.isStartCyclic() || node.isEndCyclic())
    		return new Rectangle2D.Double(x, y, 0, 0);
    	
    	if(style.getShape() != null && style.getShape().equals("triangle") && node.getTypeName().contains("NAc")) {
    		if(theGraphicOptions.ORIENTATION == 0) {
    			return new Rectangle2D.Double(x+w/2.,y,w/2.,h);
    		}
    		if(theGraphicOptions.ORIENTATION == 1) {
    			return new Rectangle2D.Double(y,x,w,h/2.);
    		}
    		if(theGraphicOptions.ORIENTATION == 2) {
    			return new Rectangle2D.Double(x,y,w/2.,h);
    		}
    		if(theGraphicOptions.ORIENTATION == 3) {
    			return new Rectangle2D.Double(x,y,w,h/2.);
    		}
    	}
    	
    	if( fillstyle.equals("empty") )
    		return null;
    	if( fillstyle.equals("full") )
    		return cur_bbox;
    	if( fillstyle.equals("half"))
    		return createHalf(w/2., h/2., x+w/2., y+h/2.);
    	if( fillstyle.equals("left") )
    		return new Rectangle2D.Double(x,y,w/2.,h);
    	if( fillstyle.equals("top") )
    		return new Rectangle2D.Double(x,y,w,h/2.);
    	if( fillstyle.equals("right") )
    		return new Rectangle2D.Double(x+w/2.,y,w/2.,h);
    	if( fillstyle.equals("bottom") )
    		return new Rectangle2D.Double(x,y+h/2.,w,h/2.);

    	if( fillstyle.equals("topleft") ) 
    		return createTriangle(x,y,x+w,y,x,y+h);
    	if( fillstyle.equals("topright") ) 
    		return createTriangle(x,y,x+w,y,x+w,y+h);
    	if( fillstyle.equals("bottomright") ) 
    		return createTriangle(x+w,y,x+w,y+h,x,y+h);
    	if( fillstyle.equals("bottomleft") ) 
    		return createTriangle(x,y,x+w,y+h,x,y+h);    	
    	
    	double cx = x+w/2.;
    	double cy = y+h/2.;
    	double rx = w/6.;
    	double ry = h/6.;
    	if( fillstyle.equals("circle") )
    		return new Ellipse2D.Double(cx-rx,cy-ry,2.*rx,2.*ry);
    	if( fillstyle.equals("checkered") )
    		return createCheckered(x,y,w,h);
    	if( fillstyle.startsWith("arc") ) {
    		LinkedList<String> tokens = TextUtils.tokenize(fillstyle,"_");
    		int first_pos = Integer.parseInt(tokens.get(1));
    		int last_pos  = Integer.parseInt(tokens.get(2));
    		return createArc(x,y,w,h,first_pos,last_pos);
    	}

    	return null;
    }
    
    private String createProbability (Linkage a_oLinkage) {
    	StringBuilder a_sProbability = new StringBuilder("");
    	int a_iHigh = a_oLinkage.getBonds().get(0).getProbabilityHigh();
    	int a_iLow = a_oLinkage.getBonds().get(0).getProbabilityLow();
 
    	if((a_iHigh != 100 && a_iLow != 100) && (a_iHigh == a_iLow)) {
    		a_sProbability.append("(" + ((a_iHigh == -100) ? "?" : a_iHigh) + "%" + ")");
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
    	
    	return a_sProbability.toString();
    }
    
    public ArrayList<String> checkComposiiton (Residue a_oRES) {
    	ArrayList<String> a_aConfs = new ArrayList<String>();
    	
    	if(!theGraphicOptions.NOTATION.equals(GraphicOptions.NOTATION_SNFG)) return a_aConfs;
    	if(!a_oRES.isSaccharide()) return a_aConfs;
 
    	ResidueType a_oRT = a_oRES.getType();

    	if(a_oRT.getName().equals(a_oRES.getTypeName())) {
    		if(a_oRES.getRingSize() != '?' && a_oRT.getRingSize() != '?') {
    			if(a_oRT.getRingSize() != a_oRES.getRingSize()) a_aConfs.add(String.valueOf(a_oRES.getRingSize()));
    		}
    		if(a_oRES.getChirality() != '?' && a_oRT.getChirality() != '?') {
    			if(a_oRT.getChirality() != a_oRES.getChirality()) a_aConfs.add(String.valueOf(a_oRES.getChirality()));
    		}
    		if(a_oRT.getRingSize() == '?') {
    			if(a_oRES.isAlditol()) a_aConfs.add(String.valueOf(a_oRES.getRingSize()));
    		}
    	}
    	
    	return a_aConfs;
    }
}
