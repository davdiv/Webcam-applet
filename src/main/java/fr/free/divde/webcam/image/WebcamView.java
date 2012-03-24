/*
 * Webcam applet: gives access to the webcam from a web application.
 * Copyright (C) 2012 divde (http://divde.free.fr)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package fr.free.divde.webcam.image;

import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import lombok.Getter;

public class WebcamView extends JLabel {
	private static final long serialVersionUID = -5173502206466167696L;

	public WebcamView() {
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (capturing) {
					computeSizes();
					repaint();
				}
			}
		});
	}

	@Getter
	private CaptureListener captureListener = initCaptureListener();
	private Runnable repainter = initRepainter();
	private boolean capturing = false;
	private volatile BufferedImage image;
	private int srcWidth, srcHeight, destX1, destY1, destX2, destY2,
			imageWidth, imageHeight;

	private CaptureListener initCaptureListener() {
		return new CaptureListener() {

			@Override
			public void startCapture(final int width, final int height) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							capturing = true;
							srcWidth = width;
							srcHeight = height;
							computeSizes();
						}
					});
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void nextFrame(BufferedImage newImage) {
				assert !SwingUtilities.isEventDispatchThread();
				image = newImage;
				try {
					SwingUtilities.invokeAndWait(repainter);
				} catch (InterruptedException ex) {
				} catch (InvocationTargetException ex) {
					ex.printStackTrace();
				}
				image = null;
			}

			@Override
			public void stopCapture() {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							capturing = false;
							repaint();
						}
					});
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		};
	}

	private Runnable initRepainter() {
		return new Runnable() {

			@Override
			public void run() {
				paintImmediately(destX1, destY1, imageWidth, imageHeight);
			}
		};
	}

	private void computeSizes() {
		int destWidth = this.getWidth();
		int destHeight = this.getHeight();
		// first try to match height and see if width fits:
		imageWidth = srcWidth * destHeight / srcHeight;
		if (imageWidth > destWidth) {
			imageHeight = srcHeight * destWidth / srcWidth;
			imageWidth = destWidth;
		} else {
			imageHeight = destHeight;
		}
		// then compute coordinates:
		destX1 = (destWidth - imageWidth) / 2;
		destY1 = (destHeight - imageHeight) / 2;
		destX2 = destX1 + imageWidth;
		destY2 = destY1 + imageHeight;
	}

	@Override
	public void update(Graphics g) {
		paint(g);
	}

	@Override
	public void paintComponent(Graphics g) {
		if (capturing) {
			if (image != null) {
				g.drawImage(image, destX1, destY1, destX2, destY2, 0, 0,
						srcWidth, srcHeight, null);
			}
			// in case of horizontal strips at the top and bottom:
			g.clearRect(0, 0, this.getWidth(), destY1);
			g.clearRect(0, destY2, this.getWidth(), this.getHeight() - destY2);

			// in case of vertical strips at the left and right:
			g.clearRect(0, 0, destX1, this.getHeight());
			g.clearRect(destX2, 0, this.getWidth() - destX2, this.getHeight());
		} else {
			g.clearRect(0, 0, this.getWidth(), this.getHeight());
		}
	}

}
