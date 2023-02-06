package com.weinmann.ccr.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Location {

	public static Location load(File stateFile) throws IOException {
		try {
			Properties prop = new Properties();
			FileInputStream fis = new FileInputStream(stateFile);
			prop.load(fis);
			fis.close();

			return new Location(prop.get("title").toString());
		} catch (Throwable t) {
			stateFile.delete();
			return null;
		}
	}

	public static Location save(File stateFile, String title) throws IOException {
		Properties prop = new Properties();
		prop.setProperty("title", title);
		// prop.setProperty("pos", Integer.toString(pos));
		FileOutputStream fos = new FileOutputStream(stateFile);
		prop.save(fos, "");
		fos.close();
		return new Location(title);
	}


	public final String title;

	public Location(String title) {
		super();
		this.title = title;
	}

}