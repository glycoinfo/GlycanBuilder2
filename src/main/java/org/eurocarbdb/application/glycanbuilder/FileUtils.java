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

import java.io.*;
import java.nio.channels.FileChannel;

import javax.swing.ImageIcon;

import java.awt.Point;
import java.awt.Cursor;
import java.awt.Toolkit;

/**
 * Utility class containing functions to facilitate the access to files, both
 * generic and application specific.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class FileUtils {

	protected static ThemeManager themeManager;

	public static ThemeManager getThemeManager() {
		return themeManager;
	}

	public static void setThemeManager(ThemeManager _themeManager) {
		themeManager = _themeManager;
	}

	private FileUtils() {
	}

	/**
	 * Convert the escape codes in a URI-like string into the original unicode
	 * character. Return the result.
	 */
	static public String removeEscapes(String src) {
		char[] in = src.toCharArray();
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < in.length; i++) {
			if (in[i] == '%' && i < (in.length - 2)) {
				if (in[i + 1] == '%') {
					out.append('%');
					i++;
				} else {
					String num = "" + in[i + 1] + in[i + 2];
					out.append((char) Integer.valueOf(num, 16).intValue());
					i += 2;
				}
			} else
				out.append(in[i]);
		}
		return out.toString();
	}

	/**
	 * Return the root directory of the current application
	 */
	static public String getRootDir() {
		String s = FileUtils.class.getResource("FileUtils.class").getFile();
		s = removeEscapes(s);
		if (s.indexOf("!") == -1)
			s = FileUtils.class.getResource("/").getFile(); // not a jar
		else
			s = s.substring(5, s.indexOf("!")); // path into a jar file

		return s.substring(0, s.lastIndexOf("/"));
	}	
	
	public static ThemeManager defaultThemeManager;

	/**
	 * Load a cursor from the application Jar file
	 * 
	 * @param id
	 *            the identifier of the cursor (without the extension)
	 */

	static public Cursor createCursor(String id) {
		String path = "/cursors/" + id + ".png";
		java.net.URL imgURL = FileUtils.class.getResource(path);
		if (imgURL != null) {
			ImageIcon img = new ImageIcon(imgURL);
			return Toolkit.getDefaultToolkit().createCustomCursor(
					img.getImage(), new Point(0, 0), id);
		}
		return null;
	}

	/**
	 * Make sure that a path to a file contain the specified extension. If not
	 * the extension is added and the resulting path is returned. If a different
	 * extension is present, it is first removed before adding the new one.
	 * 
	 * @param filename
	 *            the original path
	 * @param extension
	 *            the desired file extension
	 */
	static public String enforceExtension(String filename, String extension) {
		String ext = "";
		int i = filename.lastIndexOf('.');
		if (i > 0 && i < filename.length() - 1)
			ext = filename.substring(i + 1).toLowerCase();
		if (ext.equals(extension))
			return filename;
		return filename + "." + extension;
	}

	/**
	 * Open a stream to a desired resource irregarding if it's a regular file or
	 * if it's contained in a Jar file.
	 * 
	 * @param owner
	 *            the class requesting the resource, used to find the
	 *            correspding Jar file
	 * @param filename
	 *            the identifier of the desired resource
	 * @throws Exception
	 *             if the resource is not found
	 */
	static public InputStream open(Class owner, String filename)
			throws Exception {

		// try from jar
		java.net.URL file_url = owner.getResource(filename);
		if (file_url == null) {
			// try from file
			File file = new File(filename);
			if (!file.exists())
				throw new FileNotFoundException(filename);
			return new FileInputStream(file);
		} else
			return file_url.openStream();
	}

	/**
	 * Open a stream to a desired resource irregarding if it's a regular file or
	 * if it's contained in a Jar file.
	 * 
	 * @param owner
	 *            the class requesting the resource, used to find the
	 *            correspding Jar file
	 * @param filename
	 *            the identifier of the desired resource
	 * @param in_jar
	 *            <code>true</code> if the resource should be looked in the jar
	 *            file only, <code>false</code> if it should be looked in the
	 *            file system only
	 * @throws Exception
	 *             if the resource is not found
	 */
	static public InputStream open(Class owner, String filename, boolean in_jar)
			throws Exception {

		if (!in_jar) {
			// open from disk
			File file = new File(filename);
			if (!file.exists())
				throw new FileNotFoundException(filename);
			return new FileInputStream(file);
		}

		// open from jar
		java.net.URL file_url = owner.getResource(filename);
		if (file_url == null)
			throw new FileNotFoundException(filename);
		return file_url.openStream();
	}

	/**
	 * Return <code>true</code> if the file exists in the file systemq
	 */

	static public boolean exists(String filename) {
		return new File(filename).exists();
	}

	/**
	 * Return the text content of a file as a string
	 * 
	 * @param filename
	 *            the path to the file
	 * @throws Exception
	 *             of IO error
	 */
	static public String content(String filename) throws Exception {
		return content(new FileInputStream(filename));
	}

	/**
	 * Return the text content of a file as a string
	 * 
	 * @param file
	 *            the path to the file
	 * @throws Exception
	 *             of IO error
	 */
	static public String content(File file) throws Exception {
		return content(new FileInputStream(file));
	}

	/**
	 * Return the text content of a stream as a string
	 * 
	 * @param is
	 *            the input stream to be read
	 * @throws Exception
	 *             of IO error
	 */
	static public String content(InputStream is) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		// consume
		StringBuilder ret = new StringBuilder();

		int ch;
		while ((ch = br.read()) != -1)
			ret.appendCodePoint(ch);

		return ret.toString();
	}

	/**
	 * Return the content of a file as an array of bytes
	 * 
	 * @param filename
	 *            the path to the file
	 * @throws Exception
	 *             of IO error
	 */
	static public byte[] binaryContent(String filename) throws Exception {
		return binaryContent(new FileInputStream(filename));
	}

	/**
	 * Return the content of a file as an array of bytes
	 * 
	 * @param file
	 *            the path to the file
	 * @throws Exception
	 *             of IO error
	 */
	static public byte[] binaryContent(File file) throws Exception {
		return binaryContent(new FileInputStream(file));
	}

	/**
	 * Return the content of a file as an array of bytes
	 * 
	 * @param is
	 *            the input stream to be read
	 * @throws Exception
	 *             of IO error
	 */
	static public byte[] binaryContent(InputStream is) throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		// consume
		int b;
		while ((b = is.read()) != -1)
			os.write(b);

		return os.toByteArray();
	}

	/**
	 * Copy a file to a different destination
	 * 
	 * @param src
	 *            the original file
	 * @param dst
	 *            the destination path
	 */
	static public void copy(File src, File dst) throws Exception {
		if (src == null)
			throw new Exception("Invalid source file");
		if (dst == null)
			throw new Exception("Invalid destination file");

		FileChannel inChannel = new FileInputStream(src).getChannel();
		FileChannel outChannel = new FileOutputStream(dst).getChannel();
		try {
			long position = 0;
			long size = inChannel.size();
			while (position < size)
				position += inChannel.transferTo(position, 32000, outChannel);
		} catch (IOException e) {
			throw e;
		} finally {
			if (inChannel != null) inChannel.close();
			if (outChannel != null) outChannel.close();
		}
	}

	/**
	 * Copy the content of a stream to a destination file
	 * 
	 * @param src
	 *            the original stream
	 * @param dst
	 *            the destination path
	 */
	static public void copy(InputStream src, File dst) throws Exception {
		if (src == null)
			throw new Exception("Invalid source stream");
		if (dst == null)
			throw new Exception("Invalid destination file");

		BufferedInputStream bis = new BufferedInputStream(src);
		BufferedOutputStream bos = new BufferedOutputStream(
				new FileOutputStream(dst));
		int ch = -1;
		while ((ch = bis.read()) != -1) {
			bos.write(ch);
		}

		bos.close();
	}

}
