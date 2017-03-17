package org.eurocarbdb.application.glycanbuilder.renderutil;

import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.angle;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.center;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.distance;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.getExclusionRadius;
import static org.eurocarbdb.application.glycanbuilder.renderutil.Geometry.translate;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;

import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.linkage.LinkageStyle;
import org.eurocarbdb.application.glycanbuilder.linkage.LinkageStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;

public abstract class AbstractLinkageRenderer implements LinkageRenderer{
	protected LinkageStyleDictionary  theLinkageStyleDictionary; 
    protected GraphicOptions theGraphicOptions;

    public AbstractLinkageRenderer() {
    	theLinkageStyleDictionary = new LinkageStyleDictionary();
    	theGraphicOptions = new GraphicOptions();
    }

    public AbstractLinkageRenderer(GlycanRenderer src) {
    	theLinkageStyleDictionary = src.getLinkageStyleDictionary();
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
	public LinkageStyleDictionary getLinkageStyleDictionary() {
    	return theLinkageStyleDictionary; 
    }
    
    @Override
	public void setLinkageStyleDictionary(LinkageStyleDictionary linkageStyleDictionary) {
    	theLinkageStyleDictionary = linkageStyleDictionary;
    }

    @Override
	abstract public void paintEdge(Paintable paintable, Linkage link, boolean selected, Rectangle parent_bbox, Rectangle parent_border_bbox, Rectangle child_bbox, Rectangle child_border_bbox);

    @Override
	abstract public void paintInfo(Paintable paintable, Linkage link, Rectangle parent_bbox, Rectangle parent_border_bbox, Rectangle child_bbox, Rectangle child_border_bbox);

    abstract protected void paintInfo(Paintable paintable, String text, Rectangle p, Rectangle pb, Rectangle c, Rectangle cb, boolean toparent, boolean above, boolean multiple);

    protected Point computePosition(Dimension tb, Rectangle p, Rectangle pb, Rectangle c, Rectangle cb, boolean toparent, boolean above, boolean multiple) {       
    	Point cp = center(p);
    	Point cc = center(c);

    	double r = 0.5 * theGraphicOptions.LINKAGE_INFO_SIZE;
    	double cx=0.,cy=0.,R=0.,angle=0.;
    	if( toparent ) {
    		cx = cp.x;
    		cy = cp.y;
    		angle = angle(cc,cp);
    		R = getExclusionRadius(cp,angle,pb)+2;
    	}
    	else {
    		cx = c.x+c.width/2;
    		cy = c.y+c.height/2;
    		angle = angle(cp,cc);
    		R = getExclusionRadius(cc,angle,cb)+2;
    	}
    	double space = (multiple) ?4. :2.;

    	boolean add = above;
    	if( toparent )
    		add = !add;

    	double tx=0.,ty=0.;
    	if( add ) {
    		tx = cx+(R+r)*Math.cos(angle)+(r+space)*Math.cos(angle-Math.PI/2.);
    		ty = cy+(R+r)*Math.sin(angle)+(r+space)*Math.sin(angle-Math.PI/2.);
    	}
    	else {
    		tx = cx+(R+r)*Math.cos(angle)+(r+space)*Math.cos(angle+Math.PI/2.);
    		ty = cy+(R+r)*Math.sin(angle)+(r+space)*Math.sin(angle+Math.PI/2.);
    	}    

    	tx -= tb.getWidth()/2;
    	ty += tb.getHeight()/2;

    	return new Point((int)tx,(int)ty);
    }

    static private Shape createLine(Point p1, Point p2) {    
    	Polygon l = new Polygon();
    	l.addPoint(p1.x,p1.y);
    	l.addPoint(p2.x,p2.y);
    	return l;
    }

    protected Shape createCurve(Point p1, Point p2) {
    	double cx = (p1.x+p2.x)/2.;
    	double cy = (p1.y+p2.y)/2.;
    	double r = distance(p1,p2)/2.;
    	double angle = angle(p1,p2);

    	// start point
    	double x1 = cx+r*Math.cos(angle);
    	double y1 = cy+r*Math.sin(angle);

    	// end point
    	double x2 = cx+r*Math.cos(angle+Math.PI);
    	double y2 = cy+r*Math.sin(angle+Math.PI);

    	// ctrl point 1
    	double cx1 = cx+0.1*r*Math.cos(angle);
    	double cy1 = cy+0.1*r*Math.sin(angle);
    	double tx1 = cx1+r*Math.cos(angle+Math.PI/2.);
    	double ty1 = cy1+r*Math.sin(angle+Math.PI/2.);

    	// ctrl point 2
    	double cx2 = cx+0.1*r*Math.cos(angle+Math.PI);
    	double cy2 = cy+0.1*r*Math.sin(angle+Math.PI);
    	double tx2 = cx2+r*Math.cos(angle-Math.PI/2.);
    	double ty2 = cy2+r*Math.sin(angle-Math.PI/2.);  

    	return new CubicCurve2D.Double(x1,y1,tx1,ty1,tx2,ty2,x2,y2);
    }
    
    private Shape createCurve(Point p1, Point p2, boolean multiple) {
    	if( multiple ) {
    		GeneralPath gp = new GeneralPath();
    		double a = angle(p1,p2);

    		Shape curve1 = createCurve(translate(p1,2.*Math.cos(a+Math.PI/2),2.*Math.sin(a+Math.PI/2)),
    				translate(p2,2.*Math.cos(a+Math.PI/2),2.*Math.sin(a+Math.PI/2)),
    				false);
    		gp.append(curve1,false);

    		Shape curve2 = createCurve(translate(p1,2.*Math.cos(a-Math.PI/2),2.*Math.sin(a-Math.PI/2)),
    				translate(p2,2.*Math.cos(a-Math.PI/2),2.*Math.sin(a-Math.PI/2)),
    				false);
    		gp.append(curve2,false);

    		return gp;
    	}
    	return createCurve(p1,p2);
    }

    protected Shape createShape(Linkage link, Rectangle parent_bbox, Rectangle child_bbox) {
    	LinkageStyle style = theLinkageStyleDictionary.getStyle(link);
    	String edge_style  = style.getShape(); 
    
    	Point parent_center = center(parent_bbox);
    	Point child_center  = center(child_bbox);

    	if(link.getChildResidue().isAlternative())
    		return null;
    	if(edge_style.equals("none"))
    		return null;
    	if(edge_style.equals("empty"))
    		return null;
    	if(edge_style.equals("line"))
    		return createLine(parent_center,child_center,link.hasMultipleBonds());
    	if(edge_style.equals("curve"))
    		return createCurve(parent_center,child_center,link.hasMultipleBonds());
    
    	return createLine(parent_center,child_center);
    }
    
    private Shape createLine(Point p1, Point p2, boolean multiple) {
    	if( multiple ) {
    		GeneralPath gp = new GeneralPath();
    		double a = angle(p1,p2);

    		Shape line1 = createLine(translate(p1,2.*Math.cos(a+Math.PI/2),2.*Math.sin(a+Math.PI/2)),
    				translate(p2,2.*Math.cos(a+Math.PI/2),2.*Math.sin(a+Math.PI/2)),
    				false);
    		gp.append(line1,false);

    		Shape line2 = createLine(translate(p1,2.*Math.cos(a-Math.PI/2),2.*Math.sin(a-Math.PI/2)),
    				translate(p2,2.*Math.cos(a-Math.PI/2),2.*Math.sin(a-Math.PI/2)),
    				false);
    		gp.append(line2,false);

    		return gp;
    	}
    	return createLine(p1,p2);
    }
}
