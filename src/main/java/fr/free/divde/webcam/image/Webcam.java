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

import java.awt.image.BufferedImage;

import javax.swing.event.EventListenerList;

import lombok.Getter;
import lombok.Setter;

import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.video.capture.VideoCapture;

public final class Webcam {
	private static final int STOP_WAIT_DELAY = 1000;

	private EventListenerList listenerList = new EventListenerList();
	@Getter
	@Setter
	private int requestedWidth;
	@Getter
	@Setter
	private int requestedHeight;
	private volatile boolean requestedStop;

	private Thread thread;

	public Webcam(int requestedWidth, int requestedHeight) {
		this.requestedWidth = requestedWidth;
		this.requestedHeight = requestedHeight;
	}

	private Runnable captureRunnable = new Runnable() {
		@Override
		public void run() {
			captureThread();
		}
	};

	public synchronized void startCapture() {
		if (thread != null) {
			return;
		}
		thread = new Thread(captureRunnable);
		thread.setName("Webcam capture");
		thread.setDaemon(true);
		requestedStop = false;
		thread.start();
	}

	public synchronized void stopCapture() {
		if (thread == null) {
			return;
		}
		requestedStop = true;
		thread.interrupt();
		try {
			thread.join(STOP_WAIT_DELAY);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		thread = null;
	}

	public synchronized void restartCapture() {
		stopCapture();
		startCapture();
	}

	private void captureThread() {
		VideoCapture capture = null;
		try {
			capture = new VideoCapture(requestedWidth, requestedHeight);
			fireStartCapture(capture.getWidth(), capture.getHeight());
			while (!requestedStop) {
				MBFImage image = capture.getNextFrame();
				BufferedImage bufferedImage = ImageUtilities
						.createBufferedImage(image);
				fireNextFrame(bufferedImage);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (capture != null) {
				capture.stopCapture();
			}
			fireStopCapture();
			thread = null;
		}
	}

	private void fireNextFrame(BufferedImage bufferedImage) {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ImageListener.class
					|| listeners[i] == CaptureListener.class) {
				((ImageListener) listeners[i + 1]).nextFrame(bufferedImage);
			}
		}
	}

	private void fireStartCapture(int width, int height) {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == CaptureListener.class) {
				((CaptureListener) listeners[i + 1])
						.startCapture(width, height);
			}
		}
	}

	private void fireStopCapture() {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == CaptureListener.class) {
				((CaptureListener) listeners[i + 1]).stopCapture();
			}
		}
	}

	public synchronized boolean isCapturing() {
		return thread != null;
	}

	public void addImageListener(ImageListener listener) {
		listenerList.add(ImageListener.class, listener);
	}

	public void removeImageListener(ImageListener listener) {
		listenerList.remove(ImageListener.class, listener);
	}

	public void addCaptureListener(CaptureListener listener) {
		listenerList.add(CaptureListener.class, listener);
	}

	public void removeCaptureListener(CaptureListener listener) {
		listenerList.remove(CaptureListener.class, listener);
	}

}
