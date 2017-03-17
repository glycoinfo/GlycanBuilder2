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

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.font.*;

import org.eurocarbdb.application.glycanbuilder.util.GraphicUtils;

/**
   Utility class with different methods for computing coordinates and
   dimensions of geometrical shapes.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class Geometry {

	/**
       Return the dimensions of a text in a given font.
	 */
	static public Dimension textBounds(String text, String font_face, int font_size) {

		if( text.length()==0 )
			return new Dimension(0,font_size);

		// retrieve a graphics2d object
		BufferedImage img = GraphicUtils.createCompatibleImage(10,10,true);
		Graphics2D g2d = img.createGraphics();

		// compute text bounds
		FontRenderContext frc = g2d.getFontRenderContext();    
		Font font = new Font(font_face,Font.PLAIN,font_size);

		Rectangle text_bound = new Rectangle();
		text_bound.setRect(new TextLayout(text,font,frc).getBounds());
		return new Dimension(text_bound.width,text_bound.height);
	}


	/**
       Return the shape used to draw a text in a given font.
	 */
	static public Shape getTextShape(String text, String font_face, int font_size) {

		// retrieve a graphics2d object    
		BufferedImage img = GraphicUtils.createCompatibleImage(10,10,true);
		Graphics2D g2d = img.createGraphics();

		// compute text layout
		FontRenderContext frc = g2d.getFontRenderContext();    
		Font font = new Font(font_face,Font.PLAIN,font_size);    
		TextLayout tl = new TextLayout(text, font, frc);
		return tl.getOutline(null);
	}

	/**
       Return the shape used to draw a text in a given
       font. Initialize the shape at the given coordinates.
	 */
	static public Shape getTextShape(double x, double y, String text, String font_face, int font_size) {

		Shape s = getTextShape(text,font_face,font_size);

		AffineTransform t = new AffineTransform();
		t.setToTranslation(x,y);
		return t.createTransformedShape(s);
	}

	/**
       Create a rectangle given two corners.
	 */
	static public Rectangle makeRectangle(Point a, Point b) {
		if( a!=null && b!=null ) {
			int x = (a.x<b.x) ?a.x :b.x;
			int y = (a.y<b.y) ?a.y :b.y;
			int w = Math.abs(a.x-b.x);
			int h = Math.abs(a.y-b.y);
			return new Rectangle(x,y,w,h);
		}
		return null;
	}

	/**
       Return the coordinates of the center of a rectangle.
	 */
	static public Point center(Rectangle r) {
		return new Point(midx(r),midy(r));
	}

	/**
       Return the x coordinate of the center of a rectangle.
	 */
	static public int midx(Rectangle r) {
		return (r.x+(r.width/2));
	}

	/**
       Return the y coordinate of the center of a rectangle.
	 */
	static public int midy(Rectangle r) {
		return (r.y+(r.height/2));
	}

	/**
       Return the left coordinate of a rectangle.
	 */
	static public int left(Rectangle r) {
		return r.x;
	}

	/**
       Return the top coordinate of a rectangle.
	 */
	static public int top(Rectangle r) {
		return r.y;
	}

	/**
       Return the right coordinate of a rectangle.
	 */
	static public int right(Rectangle r) {
		return (r.x+r.width);
	}

	/**
       Return the bottom coordinate of a rectangle.
	 */
	static public int bottom(Rectangle r) {
		return (r.y+r.height);
	}

	/**
       Return the width of a rectangle.
	 */
	static public int width(Rectangle r) {
		return r.width;
	}

	/**
       Return the height of a rectangle.
	 */
	static public int height(Rectangle r) {
		return r.height;
	}

	/**
       Return the top-left coordinates of a rectangle.
	 */
	static public Point topleft(Rectangle r) {
		return new Point(left(r),top(r));
	}

	/**
       Return the top-right coordinates of a rectangle.
	 */
	static public Point topright(Rectangle r) {
		return new Point(right(r),top(r));
	}

	/**
       Return the bottom-left coordinates of a rectangle.
	 */
	static public Point bottomleft(Rectangle r) {
		return new Point(left(r),bottom(r));
	}

	/**
       Return the bottom-right coordinates of a rectangle.
	 */
	static public Point bottomright(Rectangle r) {
		return new Point(right(r),bottom(r));
	}

	/**
       Return the smallest rectangle containing the two rectangles.
	 */
	static public Rectangle union(Rectangle a, Rectangle b) {
		if( a==null && b==null ) return null;    
		if( a==null ) return b;
		if( b==null ) return a;
		return a.union(b);
	}          

	/**
       Expand both dimensions of a rectangle by the given size.
       @return the rectangle with the new dimension
	 */
	static public Rectangle expand(Rectangle r, int d) {
		return new Rectangle(r.x-d,r.y-d,r.width+2*d,r.height+2*d);
	}

	/**
       Return the distance between the centers of two rectangles.
	 */
	static public double distance(Rectangle a, Rectangle b) {
		return distance(center(a),center(b));
	}

	/**
       Return the distance between a point and the center of a
       rectangle.
	 */
	static public double distance(Point a, Rectangle b) {
		return distance(a,center(b));
	}

	/**
       Return the distance between two points.
	 */
	static public double distance(Point a, Point b) {
		return Math.sqrt((a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y));
	}

	/**
       Return the distance between the point <code>p</code> and the
       line passing by the points <code>s1</code> and <code>s2</code>.
	 */
	static public double distance(Point p, Point s1, Point s2) {

		double u = (double)((p.x-s1.x)*(s2.x-s1.x) + (p.y-s1.y)*(s2.y-s1.y))/(double)((s2.x-s1.x)*(s2.x-s1.x)+(s2.y-s1.y)*(s2.y-s1.y));
		if( u<0 ) return distance(p,s1);
		if( u>1 ) return distance(p,s2);
		return distance(p,new Point((int)(s1.x+u*(s2.x-s1.x)),(int)(s1.y+u*(s2.y-s1.y))));
	}


	/**
       Return the angle of the vector joining two points.
	 */
	static public double angle(Point p1, Point p2) {
		// from p2 to p1 

		if( p1.equals(p2) ) return -Math.PI/2.; // point up by default

		double x1 = p1.x;
		double x2 = p2.x;
		double y1 = p1.y;
		double y2 = p2.y;

		double d = Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
		double a = Math.acos((x1-x2)/d);    
		return (y1>=y2) ?a :-a;
	}

	/**
       Normalize an angle to stay between -PI and +PI.
	 */
	static public double normalize(double angle) {
		while(angle<=-Math.PI) angle+= 2.*Math.PI;
		while(angle>Math.PI) angle-= 2.*Math.PI;
		return angle;
	}

	static public boolean isDown(double angle) {
		angle = normalize(angle);
		return (angle>=-0.75*Math.PI && angle<-0.25*Math.PI);
	}

	static public boolean isRight(double angle) {
		angle = normalize(angle);
		return (angle>=-0.25*Math.PI && angle<0.25*Math.PI);
	}

	static public boolean isUp(double angle) {
		angle = normalize(angle);
		return (angle>=0.25*Math.PI && angle<0.75*Math.PI);
	}

	static public boolean isLeft(double angle) {
		angle = normalize(angle);
		return (angle>=0.75*Math.PI || angle<-0.75*Math.PI);
	}

	/**
       Return <code>true</code> if the first rectangle is above the
       second.
	 */
	static public boolean isUp(Rectangle f, Rectangle t) {
		int d_x = midx(f)-midx(t);
		int d_y = midy(f)-midy(t);
		return(d_y<0 && Math.abs(d_y)>Math.abs(d_x));
	}


	/**
       Return <code>true</code> if the first rectangle is below the
       second.
	 */
	static public boolean isDown(Rectangle f, Rectangle t) {
		int d_x = midx(f)-midx(t);
		int d_y = midy(f)-midy(t);
		return(d_y>0 && Math.abs(d_y)>Math.abs(d_x));
	}

	/**
       Return <code>true</code> if the first rectangle is on the left
       of the second.
	 */
	static public boolean isLeft(Rectangle f, Rectangle t) {
		int d_x = midx(f)-midx(t);
		int d_y = midy(f)-midy(t);
		return(d_x<0 && Math.abs(d_x)>Math.abs(d_y));
	}

	/**
       Return <code>true</code> if the first rectangle is on the right
       of the second.
	 */
	static public boolean isRight(Rectangle f, Rectangle t) {
		int d_x = midx(f)-midx(t);
		int d_y = midy(f)-midy(t);
		return(d_x>0 && Math.abs(d_x)>Math.abs(d_y));
	}

	/**
       Return the dimensions of the space between the two rectangles.
       Rectangle <code>in</code> must be contained in <code>out</code>.
	 */
	static public Insets getInsets(Rectangle in, Rectangle out) {
		return new Insets(top(in)-top(out),left(in)-left(out),
				bottom(out)-bottom(in),right(out)-right(in));    
	}

	static protected double getExclusionRadius(Point center, double angle, Rectangle bbox) {
		if( !bbox.contains(center) ) return 0.;

		double tla = angle(topleft(bbox),center);
		double tra = angle(topright(bbox),center);
		double bla = angle(bottomleft(bbox),center);
		double bra = angle(bottomright(bbox),center);

		double R = 0.;
		if( angle>=tra && angle<=bra )
			R = (right(bbox)-center.x)/Math.cos(angle);
		else if( angle>=bra && angle<=bla )
			R = (bottom(bbox)-center.y)/Math.cos(angle-0.5*Math.PI);
		else if( angle>=tla && angle<=tra )
			R = (center.y-top(bbox))/Math.cos(angle+0.5*Math.PI);
		else
			R = (center.x-left(bbox))/Math.cos(angle+Math.PI);

		return R;
	}

	static protected boolean overlapx(Rectangle a, Rectangle b, int toll) {
		if( a==null || b==null ) return false;

		int la = left(a) - toll;
		int ra = right(a) + toll;
		int lb = left(b) - toll;
		int rb = right(b) + toll;

		return (la<=lb && lb<=ra) || (la<=rb && rb<=ra) || (lb<=la && la<=rb) || (lb<=ra && ra<=rb);
	}

	static protected boolean overlapy(Rectangle a, Rectangle b, int toll) {
		if( a==null || b==null ) return false;

		int ta = top(a) - toll;
		int ba = bottom(a) + toll;
		int tb = top(b) - toll;
		int bb = bottom(b) + toll;

		return (ta<=tb && tb<=ba) || (ta<=bb && bb<=ba) || (tb<=ta && ta<=bb) || (tb<=ba && ba<=bb);
	}

	/**
       Translate a point by a specified displacement.
	 */
	 static public Point translate(Point p, double dx, double dy) {
		 return new Point((int)(p.x+dx),(int)(p.y+dy));
	 }

}