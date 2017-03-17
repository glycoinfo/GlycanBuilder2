package org.eurocarbdb.application.glycanbuilder;

import java.awt.Graphics2D;

import org.eurocarbdb.application.glycanbuilder.renderutil.Paintable;

public class DefaultPaintable implements Paintable {
	Graphics2D graphics2D;
	public DefaultPaintable(Graphics2D graphics2D){
		this.graphics2D=graphics2D;
	}
	
	@Override
	public Graphics2D getGraphics2D() {
		return this.graphics2D;
	}

	@Override
	public Object getObject() {
		return graphics2D;
	}

	@Override
	public void clear(){
		
	}
}



