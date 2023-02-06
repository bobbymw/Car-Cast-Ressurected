package com.weinmann.ccr.core;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;

import android.content.ContextWrapper;
import android.util.Log;
import android.widget.Toast;

import com.weinmann.ccr.services.EnclosureHandler;

public class Util {

	// shared with SubscriptionEdit
	public static void findAvailablePodcasts(String url, EnclosureHandler enclosureHandler) throws Exception {
		Log.i("CarCastResurrected", "Processing URL: " + url);
		SAXParser sp = saxParserFactory.newSAXParser();
		URLConnection connection = openConnection(url);
		String charset = getCharset(connection.getContentType());

		// We want to get the encoding of the xml document and take a peek so we can properly decode the entire stream
		// especially important for non-UTF8 feeds
		PushbackInputStream pis = new PushbackInputStream(connection.getInputStream(), 1024);
		StringBuilder xmlHeader = new StringBuilder();
		byte[] bytes = new byte[1023];
		int i = 0;
		for (; i < bytes.length; i++) {
			int b = pis.read();
			bytes[i] = (byte) b;
			xmlHeader.append((char) b);
			if (b == '>') {
				break;
			}
		}
		pis.unread(bytes, 0, i + 1);
		Log.i("CarCastResurrected/Util", "xml start:" + xmlHeader);
		if (xmlHeader.toString().toLowerCase().contains("windows-1252")) {
			charset = "ISO-8859-1";
		}
		if (xmlHeader.toString().toLowerCase().contains("iso-8859-1")) {
			charset = "ISO-8859-1";
		}

		InputSource is = new InputSource(pis);
		Log.i("CarCastResurrected/Util", "parsing with encoding: " + charset);
		is.setEncoding(charset);

		try {
			sp.parse(is, enclosureHandler);
		} catch(Exception pf){
			if (charset.equals("UTF-8"))
				throw pf;
			Log.i("CarCastResurrected/Util", "parse failed, trying UTF-8");
			sp = saxParserFactory.newSAXParser();
			connection = openConnection(url);
			is = new InputSource(connection.getInputStream());
			is.setEncoding("UTF-8");
			sp.parse(is, enclosureHandler);
		}
	}

	public static boolean isValidURL(String url) {
		try {
			new URL(url);
			return true;

		} catch (MalformedURLException ex) {
			return false;
		}
	}

	public static void toast(ContextWrapper contextWrapper, String string) {
		Toast.makeText(contextWrapper.getApplicationContext(), string, Toast.LENGTH_LONG).show();
	}

	private static final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

	private static final String CHARSET = "charset=";

	public static String getCharset(String contentType) {
		int dex = -1;
		if (contentType != null && (dex = contentType.indexOf(CHARSET)) != -1) {
			String charset = contentType.substring(dex + CHARSET.length());
			if (charset.length()!=0)
				return charset;
		}
		return "UTF-8";
	}

	public static String getTimeString(int ms) {
		int min = ms / (1000 * 60);
		int sec = (ms - (min * 60 * 1000)) / 1000;
		String result = String.format("%02d:%02d", min, sec);
		return result;
	}

	private static HttpURLConnection openConnection(String url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestProperty("User-Agent", "carcastresurrected");
		connection.setConnectTimeout(30 * 1000);
		connection.setReadTimeout(20 * 1000);
		return connection;
	}
}
