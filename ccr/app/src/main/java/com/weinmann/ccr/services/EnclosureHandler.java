package com.weinmann.ccr.services;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

/**
 *  SAX Parsing kinda sucks, I probably should have gone with DOM or TRAX or something else....
 * 
 * @author bob
 *
 */

public class EnclosureHandler extends DefaultHandler {

	private static final int STOP = -2;
	public static final int UNLIMITED = -1;

	String feedName;
	private boolean grabTitle;
	private final DownloadHistory history;

	public int max = 2;

	public final List<MetaNet> metaNets = new ArrayList<>();

	private boolean needTitle = true;
	private boolean startTitle;
	public String title = "";
	private String lastTitle = "";
	
	private boolean startDescription = false;
	private String lastDescription = "";

	private boolean priority = false;

	public EnclosureHandler(DownloadHistory history, Boolean priority) {
		this.history = history;
		this.priority = priority;
	}

	public EnclosureHandler(DownloadHistory history) {
                this(history, false);
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		if (needTitle && startTitle) {
			title += new String(ch, start, length);
		}

		if (grabTitle) {
			lastTitle += new String(ch, start, length);
		}
		
		if(startDescription)
			lastDescription += new String(ch, start, length);
			
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		super.endElement(uri, localName, name);

		if (needTitle && startTitle) {
			// Log.i("title", title);
			needTitle = false;
		}
		grabTitle = false;
		startDescription = false;
	}

	public String getTitle() {
		return title;
	}
	
	

	private boolean isAudio(String url, String type) {
		// for http://feeds.feedburner.com/dailyaudiobible
		// which always has the same intro at the top.
		if (url.endsWith("/Intro_to_DAB.mp3")) {
			return false;
		}
		if (url.toLowerCase().endsWith(".mp3"))
			return true;
		if (url.toLowerCase().endsWith(".m4a"))
			return true;
		if (url.toLowerCase().endsWith(".ogg"))
			return true;
		if (url.contains(".mp3?"))
			return true;
		if (url.contains(".m4a?"))
			return true;
		if (url.contains(".ogg?"))
			return true;
		if ("audio/mp3".equals(type))
			return true;
		return "audio/ogg".equals(type);
	}
	
	public void setFeedName(String feedName) {
		this.feedName = feedName;
	}

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes attrs) {

		// This grabs the first title and uses it as the feed title
		if (needTitle && localName.equals("title")) {
			startTitle = true;
		}

		if (localName.equals("title")) {
			lastTitle = "";
			grabTitle = true;
		} else if (localName.equals("item")) {
			lastTitle = "";
			lastDescription = "";
		}

		if(localName.equals("description")){
			startDescription = true;
		}

		if (localName.equals("enclosure") && attrs.getValue("url") != null) {
			if (!isAudio(attrs.getValue("url"), attrs.getValue("type"))) {
				Log.i("content", "Not downloading, url doesn't end right type... " + attrs.getValue("url") + ", " + attrs.getValue("type"));
				return;
			}
			
			Log.i("CarCastResurrected", localName + " " + attrs.getValue("url") + "; priority=" + priority);
			try {
				if (max != STOP && (max == UNLIMITED || max > 0)) {
					if (max > 0)
						max--;
					if (feedName == null) {
						if (title != null) {
							feedName = title;
						} else {
							feedName = "No Title";
						}
					}
					int length = 0;
					if (attrs.getValue("length") != null && attrs.getValue("length").length() != 0) {
						try {
							length = Integer.parseInt(attrs.getValue("length").trim());
						} catch (NumberFormatException nfe) {
							// some feeds have bad lengths
						}
					}
				    MetaNet metaNet = new MetaNet(feedName, new URL(attrs.getValue("url")), length, getMimetype(attrs.getValue("url"), attrs.getValue("type")), priority);
					metaNet.setTitle(lastTitle);
					metaNet.setDescription(lastDescription);
					if (history.contains(metaNet)) {
						// stop getting podcasts after we find one in our
						// history.
						max = STOP;
					} else {		
						boolean found = false;
						for(MetaNet i: metaNets){
							if(i.getUrl().equals(metaNet.getUrl())){
								found = true;
							}
						}
						if(!found)
							metaNets.add(metaNet);
					}
				}
			} catch (MalformedURLException e) {
				Log.e("CarCastResurrected", this.getClass().getSimpleName(), e);
			}
		}
	}

	public void setMax(int max) {
		this.max=max;
		
	}
	

	private String getMimetype(String url, String type) {
		if (url.toLowerCase().endsWith(".mp3"))
			return "audio/mp3";
		if (url.toLowerCase().endsWith(".m4a"))
			return "audio/mp3";
		if (url.toLowerCase().endsWith(".ogg"))
			return "audio/ogg";
		if (url.contains(".mp3?"))
			return "audio/mp3";
		// best effort
		if( type != null && !"".equals(type) ) {
			return type;
		}
		return "application/octet-stream";
	}
}
