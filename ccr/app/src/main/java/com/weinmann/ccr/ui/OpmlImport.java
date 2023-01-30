package com.weinmann.ccr.ui;

import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;

import android.net.Uri;
import android.os.Bundle;
import android.util.Xml;
import android.widget.Button;

import com.weinmann.ccr.R;
import com.weinmann.ccr.core.Subscription;
import com.weinmann.ccr.core.Util;
import com.weinmann.ccr.services.FileSubscriptionHelper;

public class OpmlImport extends BaseActivity {

	private boolean replaceAllOnImport = true;
	private Uri feedFile;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.oiml_import);

		final Button replaceAll = findViewById(R.id.replaceAll);
		final Button addToButton = findViewById(R.id.addToSubscription);
		replaceAll.setOnClickListener(v -> importOpml());
		addToButton.setOnClickListener(v -> {
			replaceAllOnImport = false;
			importOpml();
		});

		feedFile = getIntent().getData();

	}

	void importOpml() {
		FileSubscriptionHelper subscriptionHelper = new FileSubscriptionHelper(getConfig());

		if(replaceAllOnImport){
			subscriptionHelper.deleteAllSubscriptions();
		}
		InputStream in = null;
		try {
			in = getContentResolver().openInputStream(feedFile);
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);

			int eventType;
			int count = 0;
			while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {	
				if(eventType == XmlPullParser.START_TAG) {
					if(parser.getName().equals("outline")){
						String title = parser.getAttributeValue(null, "title");
						String xmlUrl = parser.getAttributeValue(null, "xmlUrl");
						if(subscriptionHelper.addSubscription(new Subscription(title, xmlUrl)))
							count++;
					}
					
				}				
			}
			Util.toast(this, "Imported "+count+" subscriptions");
			finish();
		} catch (Throwable t) {
            Util.toast(this, "Yikes "+t.getMessage());
		} finally {
			try {
				in.close();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

}
