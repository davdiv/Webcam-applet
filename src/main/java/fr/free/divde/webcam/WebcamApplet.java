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

package fr.free.divde.webcam;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import lombok.Delegate;
import lombok.Getter;
import lombok.Setter;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import org.apache.commons.io.IOUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import fr.free.divde.webcam.barcode.BarCodeListener;
import fr.free.divde.webcam.barcode.BarCodeReader;
import fr.free.divde.webcam.barcode.MatrixToImageWriter;
import fr.free.divde.webcam.image.DataImageUrl;
import fr.free.divde.webcam.image.ImageCapture;
import fr.free.divde.webcam.image.ImageListener;
import fr.free.divde.webcam.image.Webcam;
import fr.free.divde.webcam.image.WebcamView;

public class WebcamApplet extends JApplet {
	private static final long serialVersionUID = 3213530161234307400L;

	@Delegate
	private Webcam webcam;

	private WebcamView webcamView = new WebcamView();
	private BarCodeReader barcodeReader = new BarCodeReader();
	private BarCodeListener barcodeListener = initBarCodeListener();
	private ImageCapture imageCapture = new ImageCapture();

	private volatile JSObject window;
	private volatile JSObject getKeys;

	@Getter
	@Setter
	private volatile long sameContentDelay = 500;

	@Getter
	@Setter
	private volatile JSObject barCodeCallback;

	@Getter
	private volatile String lastCodeBar = "";
	@Getter
	private volatile long lastDetectionTime = 0;

	@Override
	public void init() {
		try {
			System.setSecurityManager(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		add(webcamView);
		webcam = new Webcam(this.getWidth(), this.getHeight());
		webcam.addCaptureListener(webcamView.getCaptureListener());
		barcodeReader.setBarCodeListener(barcodeListener);
		webcam.addImageListener(barcodeReader.getImageListener());
		webcam.addImageListener(imageCapture.getImageListener());

		String initEval = getParameter("initEval");
		synchronized (JSObject.class) {
			try {
				window = (JSObject) JSObject.getWindow(this);
				if (initEval != null) {
					window.eval(initEval);
				}
			} catch (Exception e) {
				e.printStackTrace();
				if (window == null) {
					webcam.startCapture();
				}
			}
		}
	}

	@Override
	public void destroy() {
		webcam.stopCapture();
		String destroyEval = getParameter("destroyEval");
		if (window != null && destroyEval != null) {
			synchronized (JSObject.class) {
				try {
					window.eval(destroyEval);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private BarCodeListener initBarCodeListener() {
		return new BarCodeListener() {
			@Override
			public void barCodeDetected(String content) {
				boolean sameContent = lastCodeBar.equals(content);
				if (!sameContent) {
					lastCodeBar = content;
				}
				long now = System.currentTimeMillis();
				long timeDifference = now - lastDetectionTime;
				lastDetectionTime = now;
				if (barCodeCallback != null
						&& (!sameContent || timeDifference > sameContentDelay)) {
					callJSLater(barCodeCallback, content, now);
				}

			}
		};
	}

	private void callJS(JSObject callbackObject, Object... args) {
		synchronized (JSObject.class) {
			try {
				JSObject function = (JSObject) getJSProperty(callbackObject,
						"fn", null);
				JSObject scope = (JSObject) getJSProperty(callbackObject,
						"scope", window);
				JSObject lastArg = (JSObject) getJSProperty(callbackObject,
						"arg", null);
				Object[] callArgs = new Object[args.length + 2];
				callArgs[0] = scope;
				System.arraycopy(args, 0, callArgs, 1, args.length);
				callArgs[callArgs.length - 1] = lastArg;
				function.call("call", callArgs);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	private static Object getJSProperty(JSObject object, String propertyName,
			Object defaultValue) {
		try {
			Object res = object.getMember(propertyName);
			if (res == null) {
				return defaultValue;
			}
			return res;
		} catch (JSException e) {
			return defaultValue;
		}
	}

	private Map<String, Object> convertToMap(JSObject jsObject) {
		if (getKeys == null) {
			getKeys = (JSObject) window
					.eval("(function(){var res=[];for(var key in this){if(this.hasOwnProperty(key)){res.push(key)}}return res;})");
		}
		JSObject keys = (JSObject) getKeys.call("call",
				new Object[] { jsObject });
		int size = ((Number) keys.getMember("length")).intValue();
		HashMap<String, Object> res = new HashMap<String, Object>(size);
		for (int i = 0; i < size; i++) {
			String name = (String) keys.getSlot(i);
			res.put(name, jsObject.getMember(name));
		}
		return res;
	}

	private Map<String, Object> getJSMapProperty(JSObject object,
			String propertyName) {
		JSObject value = (JSObject) getJSProperty(object, propertyName, null);
		if (value != null) {
			return convertToMap(value);
		}
		return new HashMap<String, Object>();
	}

	private static void setHeaders(URLConnection urlConnection,
			Map<String, Object> headers) {
		for (Entry<String, Object> entry : headers.entrySet()) {
			urlConnection.setRequestProperty(entry.getKey().toLowerCase(),
					entry.getValue().toString());
		}
	}

	private void callJSLater(final JSObject callbackObject,
			final Object... args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				callJS(callbackObject, args);
			}
		});
	}

	public void sendImage(JSObject parameters) {
		final String url = (String) parameters.getMember("url");
		final String format = (String) getJSProperty(parameters, "format",
				"png");
		final Map<String, Object> headers = getJSMapProperty(parameters,
				"headers");
		final JSObject callback = (JSObject) getJSProperty(parameters,
				"callback", null);
		imageCapture.captureImage(new ImageListener() {
			@Override
			public void nextFrame(BufferedImage image) {
				try {
					URL urlObject = new URL(getDocumentBase(), url);
					URLConnection connection = urlObject.openConnection();
					connection.setDoOutput(true);
					setHeaders(connection, headers);
					if (connection.getRequestProperty("content-type") == null) {
						// default content type
						connection.setRequestProperty("content-type", "image/"
								+ format);
					}
					OutputStream out = connection.getOutputStream();
					ImageIO.write(image, format, out);
					out.flush();
					out.close();
					if (callback != null) {
						StringWriter response = new StringWriter();
						IOUtils.copy(connection.getInputStream(), response,
								"UTF-8");
						callJS(callback, response.toString());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void createImageUrl(JSObject parameters) {
		final String mimeType = (String) getJSProperty(parameters, "mimeType",
				"image/png");
		final String format = (String) getJSProperty(parameters, "format",
				"png");
		final JSObject callback = (JSObject) getJSProperty(parameters,
				"callback", null);
		imageCapture.captureImage(new ImageListener() {
			@Override
			public void nextFrame(BufferedImage image) {
				try {
					String res = DataImageUrl.imageToDataURL(image, format,
							mimeType);
					callJS(callback, res);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public String createBarCodeUrl(JSObject parameters) throws IOException,
			WriterException {
		final String content = (String) parameters.getMember("content");
		final int width = (Integer) getJSProperty(parameters, "width", 230);
		final int height = (Integer) getJSProperty(parameters, "height", 230);
		final String mimeType = (String) getJSProperty(parameters, "mimeType",
				"image/jpeg");
		final String format = (String) getJSProperty(parameters, "format",
				"jpg");
		QRCodeWriter writer = new QRCodeWriter();
		BitMatrix result = writer.encode(content, BarcodeFormat.QR_CODE, width,
				height);
		BufferedImage image = MatrixToImageWriter.toBufferedImage(result);
		return DataImageUrl.imageToDataURL(image, format, mimeType);
	}

}
