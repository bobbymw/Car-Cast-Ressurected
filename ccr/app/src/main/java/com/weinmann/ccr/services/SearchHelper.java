package com.weinmann.ccr.services;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SearchHelper extends Thread {
	boolean done;
	String results = "";
	String search;

	public SearchHelper(String search) {
		this.search = search;
	}

	@Override
	public void run() {
		try {
			URL url = new URL("http://jadn.com/carcast/search?q="
					+ URLEncoder.encode(search));

			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.connect();
			if (con.getResponseCode() != 200) {
				done = true;
				return;
			}
			StringBuilder sb = new StringBuilder();
			InputStream is = con.getInputStream();
			byte[] buf = new byte[2048];
			int amt = 0;
			while ((amt = is.read(buf)) > 0) {
				sb.append(new String(buf, 0, amt));
			}
			is.close();
			results = sb.toString();
		} catch (Throwable e) {
		} finally {		
			done = true;
		}
	}

}
