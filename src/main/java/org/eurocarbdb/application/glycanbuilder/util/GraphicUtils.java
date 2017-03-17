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

package org.eurocarbdb.application.glycanbuilder.util;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

/**
   Utility class with functions to simplify the access to the graphic
   environment

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class GraphicUtils {

	private GraphicUtils() {}

	/**
       Create an image compatible with the graphic environment. If no
       graphic environment is present a simple image is created
       instead.
       @param width width of the image
       @param height height of the image
       @param opaque if <code>false</code> the background of the image
       will be set to transparant
	 */
	static public BufferedImage createCompatibleImage(int width, int height, boolean opaque) {    
		// retrieve graphic environment

		// no display
		if( GraphicsEnvironment.isHeadless() ) {
			if( opaque )
				return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		}

		// compatible to display
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gs = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gs.getDefaultConfiguration();
		if( opaque )         
			return gc.createCompatibleImage(width, height);
		return gc.createCompatibleImage(width, height, Transparency.BITMASK);
	}


	/**
       Create an image.
       @param width width of the image
       @param height height of the image
       @param opaque if <code>false</code> the background of the image
       will be set to transparant
	 */
	static public BufferedImage createImage(int width, int height, boolean opaque) {
		if( opaque )
			return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}

	/**
       Create an image.
       @param d size of the image
       @param opaque if <code>false</code> the background of the image
       will be set to transparant
	 */
	static public BufferedImage createImage(Dimension d, boolean opaque) {
		return createImage(d.width,d.height,opaque);
	}


}