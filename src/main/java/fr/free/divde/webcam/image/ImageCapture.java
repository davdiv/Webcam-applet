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
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;

public class ImageCapture {

	private AtomicReference<ImageListener> subImageListener = new AtomicReference<ImageListener>();
	@Getter
	private ImageListener imageListener = new ImageListener() {
		@Override
		public void nextFrame(final BufferedImage image) {
			final ImageListener listener = subImageListener.getAndSet(null);
			if (listener != null) {
				Thread captureThread = new Thread(new Runnable() {
					@Override
					public void run() {
						listener.nextFrame(image);
					}
				});
				captureThread.start();
			}
		}
	};

	public void captureImage(ImageListener capture) {
		subImageListener.set(capture);
	}

}
