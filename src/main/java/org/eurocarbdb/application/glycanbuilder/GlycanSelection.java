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

	/**
	 * The structures themselves, for a paste within this application (#370).
	 *
	 * <p>Copy and paste went through the GWS text, and GWS does not carry everything a structure
	 * holds: stereo read as partly or relatively known, the candidate parents of an antenna, a
	 * probability, a cycle. Pasted, they came back in their general form, or not at all. A paste in
	 * the application that made the copy takes the structures as they were copied; anything else
	 * still gets the text and the images.
	 */
	static public final DataFlavor structuresFlavor;
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

		structuresFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.List",
				"GlycanBuilder2 structures");

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
		theStructures = asCopied(_structures);
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
		theStructures = asCopied(_structures);
	}

	/**
	 * The structures as they are now. The selection is what was copied, and what is in the document
	 * can change after it: copying the document's structures themselves, the text and the pictures
	 * were made only when asked for, from whatever the structures had become by then.
	 */
	private static Collection<Glycan> asCopied(Collection<Glycan> _structures) {
		if (_structures == null) return null;
		java.util.List<Glycan> copied = new java.util.ArrayList<Glycan>();
		for (Glycan structure : _structures)
			copied.add(structure == null ? null : structure.clone());
		return copied;
	}

	/**
	 * The structures a transfer holds as structures, each a new copy so that pasting twice gives two
	 * structures, or null when it holds none - text from another application, or a picture.
	 *
	 * @param _transfer What the clipboard holds.
	 * @return Returns new copies of the structures, or null.
	 */
	@SuppressWarnings("unchecked")
	public static java.util.List<Glycan> structuresIn(Transferable _transfer) {
		if (_transfer == null || structuresFlavor == null || !_transfer.isDataFlavorSupported(structuresFlavor))
			return null;
		try {
			return (java.util.List<Glycan>) _transfer.getTransferData(structuresFlavor);
		} catch (Exception notThere) {
			return null;
		}
	}

	/** New copies of the structures copied, for a paste. */
	private java.util.List<Glycan> pastedCopies() {
		java.util.List<Glycan> copies = new java.util.ArrayList<Glycan>();
		for (Glycan structure : theStructures)
			if (structure != null) copies.add(structure.clone());
		return copies;
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

		if (theStructures != null && structuresFlavor != null)
			supportedFlavors.add(structuresFlavor);

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

		if (theStructures != null && parFlavor.equals(structuresFlavor))
			return true;

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

		if (theStructures != null && parFlavor.equals(structuresFlavor))
			return pastedCopies();

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
		// whether to show the masses, and then whether to show the reducing end: it was given the first
		// twice, so the SVG on the clipboard showed the reducing end only when the masses were shown,
		// unlike every bitmap it puts there (#231)
		return SVGUtils.getVectorGraphics((GlycanRendererAWT) theGlycanRenderer, theStructures,
				theGlycanRenderer.getGraphicOptions().SHOW_MASSES,
				theGlycanRenderer.getGraphicOptions().SHOW_REDEND);
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
