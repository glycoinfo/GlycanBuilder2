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

import java.io.*;
import java.awt.image.*;
import java.awt.datatransfer.*;
import java.awt.Toolkit;

import org.eurocarbdb.application.glycanbuilder.GlycanSelection;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;

/**
 * Utility class with methods to access the system clipboard.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */
public class ClipUtils {

	protected static final Clipboard local_clipboard;
	static {
		local_clipboard = new Clipboard("GWB");
		try {
			Toolkit.getDefaultToolkit().getSystemClipboard()
					.setContents(new GlycanSelection(null, null), null);
		} catch (SecurityException se) {

		}
	}

	private ClipUtils() {
		
	}

	/**
	 * Set the contents of the system clipboard from a transferable object. If
	 * the system clipboard is not available use an application wide clipboard.
	 */
	static public void setContents(Transferable t) {
		try {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
		} catch (SecurityException se) {
			local_clipboard.setContents(t, null);
		}
	}

	/**
	 * Set the contents of the system clipboard from an image transferable
	 * object. If the system clipboard is not available use an application wide
	 * clipboard.
	 */
	static public void setContents(BufferedImage img) {
		try {
			Toolkit.getDefaultToolkit().getSystemClipboard()
					.setContents(new ImageTransferable(img), null);
		} catch (SecurityException se) {
			local_clipboard.setContents(new ImageTransferable(img), null);
		}
	}

	/**
	 * Return the contents of the system clipboard. If the system clipboard is
	 * not available use an application wide clipboard.
	 */
	static public Transferable getContents() {
		try {
			return Toolkit.getDefaultToolkit().getSystemClipboard()
					.getContents(null);
		} catch (SecurityException se) {
			return local_clipboard.getContents(null);
		}
	}

	/**
	 * Print the flavours available for a transferable object.
	 */
	static public void showFlavors(Transferable t) {
		DataFlavor[] flavors = t.getTransferDataFlavors();
		for (int i = 0; i < flavors.length; i++) {
			System.out.println(flavors[i].getMimeType() + ": "
					+ flavors[i].getHumanPresentableName());
		}
	}
}

class ImageTransferable implements Transferable {

	static private DataFlavor[] imageFlavors;

	static public DataFlavor imageFlavor;
	static public DataFlavor bmpFlavor;
	static public DataFlavor pngFlavor;
	static public DataFlavor jpegFlavor;

	static {
		imageFlavors = new DataFlavor[4];
		imageFlavors[0] = imageFlavor = DataFlavor.imageFlavor;
		imageFlavors[1] = bmpFlavor = new DataFlavor("image/bmp", "image/bmp");
		imageFlavors[2] = pngFlavor = new DataFlavor("image/png", "image/png");
		imageFlavors[3] = jpegFlavor = new DataFlavor("image/jpeg",
				"image/jpeg");
	}

	private BufferedImage theImage = null;

	public ImageTransferable(BufferedImage img) {
		theImage = img;
	}

	public DataFlavor[] getTransferDataFlavors() {
		return imageFlavors;
	}

	public boolean isDataFlavorSupported(DataFlavor parFlavor) {
		if (parFlavor == null)
			throw new NullPointerException();

		for (int i = 0; i < imageFlavors.length; i++) {
			if (parFlavor.equals(imageFlavors[i]))
				return true;
		}
		return false;
	}

	public synchronized Object getTransferData(DataFlavor parFlavor)
			throws UnsupportedFlavorException, IOException {
		if (parFlavor == null)
			throw new NullPointerException();

		if (theImage != null) {
			if (parFlavor.equals(imageFlavor))
				return getImage();
			if (parFlavor.equals(bmpFlavor))
				return getImageStream("bmp");
			if (parFlavor.equals(pngFlavor))
				return getImageStream("png");
			if (parFlavor.equals(jpegFlavor))
				return getImageStream("jpeg");
		}

		throw new UnsupportedFlavorException(parFlavor);
	}

	public BufferedImage getImage() {
		return theImage;
	}

	public InputStream getImageStream(String format) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			javax.imageio.ImageIO.write(getImage(), format, bos);
			return new ByteArrayInputStream(bos.toByteArray());
		} catch (Exception e) {
			LogUtils.report(e);
			return null;
		}
	}
}