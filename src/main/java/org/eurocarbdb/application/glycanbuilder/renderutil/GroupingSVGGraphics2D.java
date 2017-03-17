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
import org.w3c.dom.*;
import org.apache.batik.svggen.*;
import org.apache.batik.ext.awt.g2d.GraphicContext;

class GroupingSVGGraphics2D extends SVGGraphics2D {      

	private static class MyDOMTreeManager extends DOMTreeManager {
		public MyDOMTreeManager(GraphicContext gc,SVGGeneratorContext generatorContext,int maxGCOverrides){
			super(gc,generatorContext,maxGCOverrides);
		}

		public Document getDOMFactory() {
			return generatorContext.getDOMFactory();
		}

	}

	private static class MyDOMGroupManager extends DOMGroupManager {

		private MyDOMTreeManager tm;

		public MyDOMGroupManager(GraphicContext gc, MyDOMTreeManager domTreeManager) {
			super(gc,domTreeManager);
			tm = domTreeManager;
		}

		public void addGroup(String id) {
			currentGroup = tm.getDOMFactory().createElementNS(SVG_NAMESPACE_URI, SVG_G_TAG);
			currentGroup.setAttribute("ID",id);
		}

	}

	private MyDOMTreeManager tm;
	private MyDOMGroupManager gm;

	private HashMap<String,Integer> last_ids = new HashMap<String,Integer>();
	private HashMap<Object,Integer> ids = new HashMap<Object,Integer>();

	public GroupingSVGGraphics2D(Document d, boolean text_as_shapes) {
		super(d, new DefaultImageHandler(), new DefaultExtensionHandler(), text_as_shapes);
		setDOMTreeManager(tm = new MyDOMTreeManager(gc,generatorCtx,DEFAULT_MAX_GC_OVERRIDES));
		setDOMGroupManager(gm = new MyDOMGroupManager(gc,tm));
	}

	public void addGroup(String id_class, Object parent, Object rep) {
		int id_p = getID(parent);
		int id_r = getID(rep);

		gm.addGroup(id_class + "-" + id_p + ":" + id_r);
	}

	public void addGroup(String id_class, Object parent, Object rep1, Object rep2) {
		int id_p = getID(parent);
		int id_r1 = getID(rep1);
		int id_r2 = getID(rep2);

		gm.addGroup(id_class + "-" + id_p + ":" + id_r1 + "," + id_r2);
	}

	private int getID(Object o) {
		if( o==null ) return 0;
		if( ids.containsKey(o) ) return ids.get(o);

		int new_id = generateID(o.getClass().getName()); 
		ids.put(o,new_id);
		return new_id;
	}

	private int generateID(String class_name) {
		if( last_ids.containsKey(class_name) ) {
			int last_id = last_ids.get(class_name);
			last_ids.put(class_name, last_id+1);
			return last_id+1;
		}
		last_ids.put(class_name,1);
		return 1;
	}
}
