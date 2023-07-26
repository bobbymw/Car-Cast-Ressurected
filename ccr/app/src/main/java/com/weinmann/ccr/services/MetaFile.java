package com.weinmann.ccr.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import android.media.MediaPlayer;
import android.util.Log;

/**
 * Meta information about a podcast. From rss metadata (hopefully someday from id3tags as well.)
 */
public class MetaFile {

	private final File file;
	private Properties properties = new Properties();
	private static final String defaultBaseFilename = "0";

	public String getFilename(){
		return file.getName();
	}
	public File getFile() { return file; }

	MetaFile(File file) {
		this.file = file;

		File metaFile = getMetaPropertiesFile();
		if (metaFile.exists()) {
			try (FileInputStream fis = new FileInputStream(metaFile)) {
				properties.load(fis);
			} catch (Exception e) {
				Log.e("Meta", "Can't load properties");
			}
		} else {
			properties.setProperty("title", file.getName());
			properties.setProperty("feedName", "unknown feed");
			properties.setProperty("currentPos", "0");
			computeDuration();
			save();
		}
	}

        // It would be better to just store this in the metadata!
        //
        // Take a filename of either the form
        //    "...../XXXX:00:YYYY.mp3"
        //    "...../XXXX.mp3"
        // and return just "XXXX".
        //
        public String getBaseFilename()
        {
           String name = getFilename();
           if ( name == null ) return defaultBaseFilename;

           // Find start of base file name.
           int slashIndex = name.lastIndexOf('/');
           // slashIndex is -1 if no slash is present, which works perfectly below!

           // Find end of base file name.
           int i = slashIndex + 1;
           while ( i < name.length() && Character.isDigit(name.charAt(i)) )
              i += 1;
           if ( name.length()  <= i ) return defaultBaseFilename;
           if ( slashIndex + 1 == i ) return defaultBaseFilename;

           return name.substring(slashIndex + 1, i);
        }

	private void computeDuration() {
		// ask media player
		MediaPlayer mediaPlayer = new MediaPlayer();
		try {
			mediaPlayer.setDataSource(file.toString());
			mediaPlayer.prepare();
			setDurationMs(mediaPlayer.getDuration());
		} catch (Exception e) {
			setDurationMs(0);
		} finally {
			mediaPlayer.reset();
			mediaPlayer.release();
		}

	}

	public MetaFile(MetaNet metaNet, File castFile) {
		file = castFile;
		properties = metaNet.properties;
		computeDuration();
	}

	public void delete() {
		file.delete();
		getMetaPropertiesFile().delete();
	}

	public int getCurrentPosMs() {
		if (properties.getProperty("currentPos") == null)
			return 0;
		return Integer.parseInt(properties.getProperty("currentPos"));
	}

	public int getDurationMs() {
		Object durationObj = properties.get("duration");

		if (durationObj == null) {
			computeDuration();
			durationObj = properties.get("duration");
		}

		if (durationObj == null) {
			return -1;
		}

		return Integer.parseInt((String) durationObj);
	}

	public String getFeedName() {
		if (properties.get("feedName") == null)
			return "unknown";
		return properties.get("feedName").toString();
	}

	private File getMetaPropertiesFile() {
		// check for metadata
		String name = file.getName();
		int lastDot = name.lastIndexOf('.');
		if (lastDot != -1) {
			name = name.substring(0, lastDot);
		}
		name += ".meta";
		return new File(file.getParent(), name);
	}

	public String getTitle() {
		if (properties.get("title") == null) {
			String title = file.getName();
			int lastDot = title.lastIndexOf('.');
			return title.substring(0, lastDot);
		}
		return properties.get("title").toString();
	}

	public void save() {
		try (FileOutputStream fos = new FileOutputStream(getMetaPropertiesFile())) {
			properties.store(fos, "");
		} catch (Throwable e) {
			Log.e("MetaFile", "saving meta data", e);
		}
	}

	public void setCurrentPosMs(int msec) {
		properties.setProperty("currentPos", Integer.toString(msec));
		if (getDurationMs() == -1)
			return;
		if (msec > getDurationMs() * .9) {
			setListenedTo();
		}
	}

	public void setDurationMs(int msec) {
		properties.setProperty("duration", Integer.toString(msec));
	}

	public void setListenedTo() {
		properties.setProperty("listenedTo", "true");
	}

	public boolean isListenedTo() {
		return properties.getProperty("listenedTo" ) != null;
	}
	
	public String getDescription(){
		return properties.getProperty("description");
	}
}
