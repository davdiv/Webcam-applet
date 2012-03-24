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

package fr.free.divde.webcam.barcode;

import java.awt.image.BufferedImage;

import lombok.Getter;
import lombok.Setter;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import fr.free.divde.webcam.image.ImageListener;

public class BarCodeReader {

	private Reader reader = new QRCodeReader();
	@Getter
	@Setter
	private BarCodeListener barCodeListener;
	@Getter
	private ImageListener imageListener = initImageListener();

	private ImageListener initImageListener() {
		return new ImageListener() {

			@Override
			public void nextFrame(BufferedImage image) {
				processImage(image);
			}
		};
	}

	private void processImage(BufferedImage image) {
		if (barCodeListener == null) {
			return;
		}
		LuminanceSource source = new BufferedImageLuminanceSource(image);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			Result result = reader.decode(bitmap);
			String stringResult = result.getText();
			barCodeListener.barCodeDetected(stringResult);
			reader.reset();
		} catch (NotFoundException e) {
		} catch (ChecksumException e) {
		} catch (FormatException e) {
		}
	}

}