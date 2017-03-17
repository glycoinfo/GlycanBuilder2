/**
 * 
 */
package org.eurocarbdb.application.glycanbuilder.util;

import java.net.URL;

public class IconProperties {
	public URL imgURL;
	public boolean scale;
	public String id;

	public IconProperties(URL _imgURL, boolean _scale) {
		imgURL = _imgURL;
		scale = _scale;
	}
}