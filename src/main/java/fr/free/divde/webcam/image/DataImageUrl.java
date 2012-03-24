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
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;

public class DataImageUrl {
	public static String imageToDataURL(BufferedImage image, String format,
			String mimeType) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, format, out);
		byte[] imageBytes = out.toByteArray();
		String base64 = Base64.encodeBase64String(imageBytes);
		StringBuilder res = new StringBuilder("data:");
		res.append(mimeType);
		res.append(";base64,");
		res.append(base64);
		return res.toString();
	}
}
