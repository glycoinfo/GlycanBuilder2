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

package org.eurocarbdb.application.glycanbuilder;

import java.util.*;
import java.io.*;
import java.awt.image.*;
import java.awt.datatransfer.*;

import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRenderer;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.SVGUtils;

/**
 * Object used to store glycan data into the cliboard
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class GlycanSelection implements Transferable {

	static private final DataFlavor[] dataFlavors;
	static private final DataFlavor[] imageFlavors;

	/** Clipboard format for text objects */
	static public final DataFlavor stringFlavor;
	/** Clipboard format for generic glycan objects */
	static public final DataFlavor glycoFlavor;
	/** Clipboard format for java image objects */
	static public final DataFlavor imageFlavor;
	/** Clipboard format for images in BMP format */
	static public final DataFlavor bmpFlavor;
	/** Clipboard format for images in PNG format */
	static public final DataFlavor pngFlavor;
	/** Clipboard format for images in JPG format */
	static public final DataFlavor jpegFlavor;
	/** Clipboard format for images in SVG format */
	static public final DataFlavor svgFlavor;
	/*
	 * static public final DataFlavor pdfFlavor; static public final DataFlavor
	 * epsFlavor;
	 */
	/*
	 * static public final DataFlavor mswmfFlavor; static public final
	 * DataFlavor oowmfFlavor; static public final DataFlavor imgwmfFlavor;
	 */

	static {
		imageFlavors = new DataFlavor[6];
		imageFlavors[0] = glycoFlavor = new DataFlavor(
				"application/x-glycoworkbench", "application/x-glycoworkbench");
		imageFlavors[1] = imageFlavor = DataFlavor.imageFlavor;
		imageFlavors[2] = bmpFlavor = new DataFlavor("image/bmp", "image/bmp");
		imageFlavors[3] = pngFlavor = new DataFlavor("image/png", "image/png");
		imageFlavors[4] = jpegFlavor = new DataFlavor("image/jpeg",
				"image/jpeg");
		imageFlavors[5] = svgFlavor = new DataFlavor("image/svg+xml",
				"image/svg+xml");

		dataFlavors = new DataFlavor[1];
		dataFlavors[0] = stringFlavor = DataFlavor.stringFlavor;

		/*
		 * imageFlavors[5] = mswmfFlavor = new
		 * DataFlavor("application/x-msmetafile","application/x-msmetafile");
		 * imageFlavors[6] = oowmfFlavor = new
		 * DataFlavor("application/x-openoffice-wmf"
		 * ,"application/x-openoffice-wmf"); imageFlavors[7] = imgwmfFlavor =
		 * new DataFlavor("image/wmf","image/wmf");
		 */
		/*
		 * imageFlavors[6] = pdfFlavor = new
		 * DataFlavor("application/pdf","application/pdf"); imageFlavors[7] =
		 * epsFlavor = new
		 * DataFlavor("application/postscript","application/postscript");
		 */
	}

	// data in this selection
	private GlycanRenderer theGlycanRenderer = null;
	private Collection<Glycan> theStructures = null;
	private Data theData = null;

	/**
	 * Construct a new object storing a table of data to be put in the clipboard
	 * 
	 * @param _data
	 *            the data to be put in the clipboard
	 */
	public GlycanSelection(Data _data) {
		theData = _data;
	}

	/**
	 * Construct a new object storing a list of structures to be put in the
	 * clipboard
	 * 
	 * @param _glycanRenderer
	 *            the renderer that will be used to generate images of the
	 *            structures
	 * @param _structures
	 *            the list of glycan structures to be put in the clipboard
	 */
	public GlycanSelection(GlycanRenderer _glycanRenderer,
			Collection<Glycan> _structures) {
		theGlycanRenderer = _glycanRenderer;
		theStructures = _structures;
	}

	/**
	 * Construct a new object storing a list of structures and a table of data
	 * to be put in the clipboard
	 * 
	 * @param _glycanRenderer
	 *            the renderer that will be used to generate images of the
	 *            structures
	 * @param _data
	 *            the data to be put in the clipboard
	 */
	public GlycanSelection(Data _data, GlycanRendererAWT _glycanRenderer,
			Collection<Glycan> _structures) {
		theData = _data;
		theGlycanRenderer = _glycanRenderer;
		theStructures = _structures;
	}

	/**
	 * Returns a list of DataFlavor objects indicating the flavors the data can
	 * be provided in.
	 */
	public ArrayList<DataFlavor> getTransferDataFlavorsVector() {

		ArrayList<DataFlavor> supportedFlavors = new ArrayList<DataFlavor>();

		if (theData != null) {
			for (int i = 0; i < dataFlavors.length; i++)
				supportedFlavors.add(dataFlavors[i]);
		}

		if (theStructures != null && theGlycanRenderer != null) {
			for (int i = 0; i < imageFlavors.length; i++)
				supportedFlavors.add(imageFlavors[i]);
		}

		return supportedFlavors;
	}

	public DataFlavor[] getTransferDataFlavors() {
		return getTransferDataFlavorsVector().toArray(new DataFlavor[0]);
	}

	public boolean isDataFlavorSupported(DataFlavor parFlavor) {
		if (parFlavor == null)
			throw new NullPointerException();

		if (theData != null) {
			for (int i = 0; i < dataFlavors.length; i++) {
				if (parFlavor.equals(dataFlavors[i]))
					return true;
			}
		}

		if (theStructures != null && theGlycanRenderer != null) {
			for (int i = 0; i < imageFlavors.length; i++) {
				if (parFlavor.equals(imageFlavors[i]))
					return true;
			}
		}

		return false;
	}

	public synchronized Object getTransferData(DataFlavor parFlavor)
			throws UnsupportedFlavorException, IOException {
		if (parFlavor == null)
			throw new NullPointerException();

		if (theData != null) {
			if (parFlavor.equals(stringFlavor))
				return theData.toString();
		}

		if (theStructures != null && theGlycanRenderer != null) {
			// if( parFlavor.equals(stringFlavor) )
			// return getText();
			if (parFlavor.equals(glycoFlavor))
				return getStream(getText().getBytes());
			if (parFlavor.equals(imageFlavor))
				return getImage();
			if (parFlavor.equals(bmpFlavor))
				return getImageStream("bmp");
			if (parFlavor.equals(pngFlavor))
				return getImageStream("png");
			if (parFlavor.equals(jpegFlavor))
				return getImageStream("jpeg");
			if (parFlavor.equals(svgFlavor))
				return getStream(getSVG().getBytes());
			/*
			 * if (parFlavor.equals(mswmfFlavor) ) return getStream(getWMF());
			 * if (parFlavor.equals(oowmfFlavor) ) return getStream(getWMF());
			 * if (parFlavor.equals(imgwmfFlavor) ) return getStream(getWMF());
			 */
			/*
			 * if (parFlavor.equals(pdfFlavor) ) return getStream(getPDF()); if
			 * (parFlavor.equals(epsFlavor) ) return getStream(getEPS());
			 */
		}

		throw new UnsupportedFlavorException(parFlavor);
	}

	/**
	 * Return the table of data stored in the object
	 */
	public Data getData() {
		return theData;
	}

	/**
	 * Return the list of structures stored in the object
	 */
	public Collection<Glycan> getStructures() {
		return theStructures;
	}

	/**
	 * Return a string encoding of the list of structures stored in the object.
	 * The internal format is used for the encodic
	 */
	public String getText() {
		return GlycanDocument.toString(theStructures);
	}

	/**
	 * Return an image on which the structures stored in the object are drawn
	 */
	public BufferedImage getImage() {
		return theGlycanRenderer.getImage(theStructures, true,
				theGlycanRenderer.getGraphicOptions().SHOW_MASSES,
				theGlycanRenderer.getGraphicOptions().SHOW_REDEND);
	}

	/**
	 * Return a stream to read the image on which the structures stored in the
	 * object are drawn
	 */
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

	/**
	 * Return a string in SVG format representing a rendering of the structures
	 * stored in the object
	 */
	public String getSVG() {
		System.out.println("getSVG");
		return SVGUtils.getVectorGraphics((GlycanRendererAWT) theGlycanRenderer, theStructures,
				theGlycanRenderer.getGraphicOptions().SHOW_MASSES,
				theGlycanRenderer.getGraphicOptions().SHOW_MASSES);
	}

	/*
	 * 
	 * public byte[] getWMF() { return
	 * GlycanCanvas.getWMFGraphics(theStructures); }
	 * 
	 * public byte[] getPDF() { return
	 * GlycanCanvas.getPDFGraphics(theStructures); }
	 * 
	 * public byte[] getEPS() { return
	 * GlycanCanvas.getEPSGraphics(theStructures); }
	 */

	protected InputStream getStream(byte[] buffer) {
		try {
			return new ByteArrayInputStream(buffer);
		} catch (Exception e) {
			LogUtils.report(e);
			return null;
		}
	}

	/*
	 * public void writeImage(String filename, String format) { try {
	 * FileOutputStream fos = new FileOutputStream(filename);
	 * javax.imageio.ImageIO.write(getImage(),format,fos); fos.close(); }
	 * catch(Exception e) { LogUtils.report(e); } }
	 */
}
