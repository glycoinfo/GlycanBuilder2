package org.eurocarbdb.application.glycanbuilder;

/*
 * Copyright (c) 2005-2010 Flamingo Kirill Grouchnikov. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *     
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *     
 *  o Neither the name of Flamingo Kirill Grouchnikov nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.EventListenerList;

import org.pushingpixels.flamingo.api.common.AsynchronousLoadListener;
import org.pushingpixels.flamingo.api.common.AsynchronousLoading;
import org.pushingpixels.flamingo.api.common.icon.ResizableIcon;
import org.pushingpixels.flamingo.internal.utils.FlamingoUtilities;

public class ImageResizableIconReducedMem implements Icon, AsynchronousLoading,
		ResizableIcon {
	/**
	 * The original image.
	 */
	protected BufferedImage originalImage;

	/**
	 * The input stream of the original image.
	 */
	protected InputStream imageInputStream;

	/**
	 * The input stream of the original image.
	 */
	protected Image image;

	/**
	 * Contains all precomputed images.
	 */
	protected Map<String, BufferedImage> cachedImages;

	/**
	 * The width of the current image.
	 */
	protected int width;

	/**
	 * The height of the current image.
	 */
	protected int height;

	protected int minScale = -1;

	public void minScale(int _minScale) {
		minScale = _minScale;
	}

	/**
	 * The listeners.
	 */
	protected EventListenerList listenerList = new EventListenerList();
	
	static protected HashMap<String,ImageResizableIconReducedMem> imageCache=new HashMap<String,ImageResizableIconReducedMem>();
	

	/**
	 * Returns the icon for the specified URL.
	 * 
	 * @param location
	 *            Icon URL.
	 * @param initialDim
	 *            Initial dimension of the icon.
	 * @return Icon instance.
	 */
	public static ImageResizableIconReducedMem getIcon(URL location,
			Dimension initialDim) {
		String key=location.toString()+"-"+initialDim.getWidth()+"-"+initialDim.getHeight();
		if(imageCache.containsKey(key)){
			if(location.toString().contains("database_annotate"))
				System.err.println("Hit cache! "+location.toString()+"-"+initialDim.getWidth()+"-"+initialDim.getHeight());
			ImageResizableIconReducedMem test=imageCache.get(key);
			ImageResizableIconReducedMem test1=null;
			try {
				test1 = new ImageResizableIconReducedMem(location.openStream(),
						(int) initialDim.getWidth(), (int) initialDim.getHeight(),false);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			test1.cachedImages=test.cachedImages;
			return test1;
		}else{
			try {
				ImageResizableIconReducedMem test=new ImageResizableIconReducedMem(location.openStream(),
						(int) initialDim.getWidth(), (int) initialDim.getHeight(),true);
				imageCache.put(key, test);
				return test;
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return null;
			}
		}
	}

	/**
	 * Create a new image-wrapper icon.
	 * 
	 * @param inputStream
	 *            The input stream to read the image from.
	 * @param w
	 *            The width of the icon.
	 * @param h
	 *            The height of the icon.
	 */
	public ImageResizableIconReducedMem(InputStream inputStream, int w, int h,boolean preSize) {
		this.imageInputStream = inputStream;
		this.width = w;
		this.height = h;
		this.listenerList = new EventListenerList();
		this.cachedImages = new LinkedHashMap<String, BufferedImage>() {
			@Override
			protected boolean removeEldestEntry(
					Map.Entry<String, BufferedImage> eldest) {
				return size() > 5;
			};
		};
		
		if(preSize){
			this.presize(this.width, this.height);
		}
	}

	/**
	 * Create a new image-wrapper icon.
	 * 
	 * @param image
	 *            The original image.
	 * @param w
	 *            The width of the icon.
	 * @param h
	 *            The height of the icon.
	 */
	public ImageResizableIconReducedMem(Image image, int w, int h) {
		this.imageInputStream = null;
		this.image = image;
		this.width = w;
		this.height = h;
		this.listenerList = new EventListenerList();
		this.cachedImages = new LinkedHashMap<String, BufferedImage>() {
			@Override
			protected boolean removeEldestEntry(
					Map.Entry<String, BufferedImage> eldest) {
				return size() > 5;
			};
		};
		this.presize(this.width, this.height);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jvnet.flamingo.common.AsynchronousLoading#addAsynchronousLoadListener
	 * (org.jvnet.flamingo.common.AsynchronousLoadListener)
	 */
	public void addAsynchronousLoadListener(AsynchronousLoadListener l) {
		this.listenerList.add(AsynchronousLoadListener.class, l);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jvnet.flamingo.common.AsynchronousLoading#removeAsynchronousLoadListener
	 * (org.jvnet.flamingo.common.AsynchronousLoadListener)
	 */
	public void removeAsynchronousLoadListener(AsynchronousLoadListener l) {
		this.listenerList.remove(AsynchronousLoadListener.class, l);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.Icon#getIconWidth()
	 */
	public int getIconWidth() {
		return width;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.Icon#getIconHeight()
	 */
	public int getIconHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics,
	 * int, int)
	 */
	public void paintIcon(Component c, Graphics g, int x, int y) {
		BufferedImage image = this.cachedImages.get(this.getIconWidth() + ":"
				+ this.getIconHeight());
		if (image != null) {
			int dx = (this.width - image.getWidth()) / 2;
			int dy = (this.height - image.getHeight()) / 2;
			g.drawImage(image, x + dx, y + dy, null);
		}
	}

	/**
	 * Sets the preferred size for <code>this</code> icon. The rendering is
	 * scheduled automatically.
	 * 
	 * @param dim
	 *            Preferred size.
	 */
	public synchronized void setPreferredSize(Dimension dim) {
		if ((dim.width == this.width) && (dim.height == this.height))
			return;
		this.width = dim.width;
		this.height = dim.height;

		this.renderImage(this.width, this.height);
	}

	/**
	 * Renders the image.
	 * 
	 * @param renderWidth
	 *            Requested rendering width.
	 * @param renderHeight
	 *            Requested rendering height.
	 */
	protected synchronized void renderImage(final int renderWidth,
			final int renderHeight) {
		String key = renderWidth + ":" + renderHeight;
		if (this.cachedImages.containsKey(key)) {
			fireAsyncCompleted(true);
			return;
		}

		SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
			@Override
			protected BufferedImage doInBackground() throws Exception {
				//System.err.println("running in background!");
				if (imageInputStream != null) {
					synchronized (imageInputStream) {
						if (originalImage == null) {
							// read original image
							originalImage = ImageIO.read(imageInputStream);
						}
					}
				} else {
					GraphicsEnvironment e = GraphicsEnvironment
							.getLocalGraphicsEnvironment();
					GraphicsDevice d = e.getDefaultScreenDevice();
					GraphicsConfiguration c = d.getDefaultConfiguration();
					originalImage = c.createCompatibleImage(
							image.getWidth(null), image.getHeight(null),
							Transparency.TRANSLUCENT);
					Graphics g = originalImage.getGraphics();
					g.drawImage(image, 0, 0, null);
					g.dispose();
				}

				BufferedImage result = originalImage;
				float scaleX = (float) originalImage.getWidth()
						/ (float) renderWidth;
				float scaleY = (float) originalImage.getHeight()
						/ (float) height;

				float scale = Math.max(scaleX, scaleY);
				if (minScale > -1 && minScale < scale)
					return originalImage;
				// System.err.println("Scale: "+scale);
				if (scale > 1.0f) {
					int finalWidth = (int) (originalImage.getWidth() / scale);
					result = FlamingoUtilities.createThumbnail(originalImage,
							finalWidth);
				}

				return result;
				// return null;
			}

			@Override
			protected void done() {
				try {
					BufferedImage bufferedImage = get();
					cachedImages.put(renderWidth + ":" + renderHeight,
							bufferedImage);
					fireAsyncCompleted(true);
				} catch (Exception exc) {
					fireAsyncCompleted(false);
				}
			}
		};
		worker.execute();
	}

	/**
	 * Fires the asynchronous load event.
	 * 
	 * @param event
	 *            Event object.
	 */
	protected void fireAsyncCompleted(Boolean event) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == AsynchronousLoadListener.class) {
				((AsynchronousLoadListener) listeners[i + 1]).completed(event);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jvnet.flamingo.common.AsynchronousLoading#isLoading()
	 */
	@Override
	public synchronized boolean isLoading() {
		BufferedImage image = this.cachedImages.get(this.getIconWidth() + ":"
				+ this.getIconHeight());
		return (image == null);
	}

	@Override
	public void setDimension(Dimension dim) {
		// TODO Auto-generated method stub
		this.setPreferredSize(dim);
	}

	protected void presize(final int renderWidth, final int renderHeight) {
		GraphicsEnvironment e = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice d = e.getDefaultScreenDevice();
		GraphicsConfiguration c = d.getDefaultConfiguration();

		if (imageInputStream != null) {
			synchronized (imageInputStream) {
				if (originalImage == null) {
					try {
						originalImage = ImageIO.read(imageInputStream);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		} else {
			originalImage = c.createCompatibleImage(image.getWidth(null),
					image.getHeight(null), Transparency.TRANSLUCENT);
		}

		Graphics g = originalImage.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();

		BufferedImage result = originalImage;
		float scaleX = (float) originalImage.getWidth() / (float) renderWidth;
		float scaleY = (float) originalImage.getHeight() / (float) height;

		float scale = Math.max(scaleX, scaleY);
		if (scale > 1.0f) {
			int finalWidth = (int) (originalImage.getWidth() / scale);
			result = FlamingoUtilities.createThumbnail(originalImage,
					finalWidth);
		}

		cachedImages.put(renderWidth + ":" + renderHeight, result);

		image = result;

		// System.err.println("Clearing...");
	}
}
