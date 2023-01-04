package com.weinmann.ccr.services;

import java.io.Serializable;

@SuppressWarnings("serial")
public class HistoryEntry implements Serializable {

	final String subscription;
	final String podcastURL;

	public HistoryEntry(String subscription, String podcastURL) {
		super();
		this.subscription = subscription;
		this.podcastURL = podcastURL;
	}
}
