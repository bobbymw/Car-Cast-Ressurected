package com.weinmann.ccr.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.weinmann.ccr.R;
import com.weinmann.ccr.core.CarCastResurrectedApplication;
import com.weinmann.ccr.core.ExternalMediaStatus;
import com.weinmann.ccr.core.Subscription;
import com.weinmann.ccr.core.Util;
import com.weinmann.ccr.services.DownloadHistory;

/**
 * A good video about listview http://code.google.com/events/io/2010/sessions/world-of-listview-android.html
 * 
 * Although right now Subscriptions is using a very simple approach
 */

public class Subscriptions extends BaseActivity {

	private static final String DISABLE_SUBSCRIPTION = "Disable";
	private static final String ENABLE_SUBSCRIPTION = "Enable";
	private static final String DELETE_SUBSCRIPTION = "Delete";
	private static final String EDIT_SUBSCRIPTION = "Edit";
	private static final String ERASE_SUBSCRIPTIONS_S_HISTORY = "Erase History";

	SimpleAdapter listAdapter;
	ListView listView;
	private final List<Map<String, Object>> subscriptions = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.subscription_list);
		setTitle(CarCastResurrectedApplication.getAppTitle() + ": Subscriptions");
		listView = findViewById(R.id.siteList);
		registerForContextMenu(listView);

		ExternalMediaStatus status = ExternalMediaStatus.getExternalMediaStatus();
		if (status == ExternalMediaStatus.unavailable) {
			Util.toast(this, "Unable to read subscriptions from sdcard");
			return;
		}

		listAdapter = new SimpleAdapter(this, subscriptions, R.layout.main_item_two_line_row2, new String[] { "name", "enabled" },
				new int[] { R.id.text1, R.id.text2 });
		listView.setAdapter(listAdapter);
	}

	// Invoked when returning from a subscription edit
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		reloadSubscriptions();
	}

	// Invoked when the background service is bound (hooked up) to this Activity
	@Override
	protected void onPostContentServiceChanged() {
		reloadSubscriptions();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Map<?, ?> rowData = (Map<?, ?>) listView.getAdapter().getItem(info.position);

		Subscription sub = (Subscription) rowData.get("subscription");

		if (item.getTitle().equals(DISABLE_SUBSCRIPTION) || item.getTitle().equals(ENABLE_SUBSCRIPTION)) {
			contentService.toggleSubscription(sub);
			reloadSubscriptions();
			Subscriptions.this.listAdapter.notifyDataSetChanged();
			return true;

		} else if (item.getTitle().equals(DELETE_SUBSCRIPTION)) {
			contentService.deleteSubscription(sub);
			Subscriptions.this.subscriptions.remove(info.position);
			Subscriptions.this.listAdapter.notifyDataSetChanged();
			return true;

		} else if (item.getTitle().equals(EDIT_SUBSCRIPTION)) {
			Intent intent = new Intent(this, SubscriptionEdit.class);
			intent.putExtra("subscription", sub);
			startActivityForResult(intent, info.position);
		} else if (item.getTitle().equals(ERASE_SUBSCRIPTIONS_S_HISTORY)) {
			int erasedPodcasts = new DownloadHistory(getConfig()).eraseHistory(sub.name);
			Util.toast(this, "Removed " + erasedPodcasts + " podcasts from download history.");
		}
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Map<?, ?> rowData = (Map<?, ?>) listView.getAdapter().getItem(info.position);
		Subscription sub = (Subscription) rowData.get("subscription");
		menu.setHeaderTitle(sub.name);

		if (sub.enabled)
			menu.add(DISABLE_SUBSCRIPTION);
		else
			menu.add(ENABLE_SUBSCRIPTION);

		menu.add(EDIT_SUBSCRIPTION);
		menu.add(DELETE_SUBSCRIPTION);
		menu.add(ERASE_SUBSCRIPTIONS_S_HISTORY);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.edit_subscription_menu, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.addSubscription) {
			Intent addIntent = new Intent(this, SubscriptionEdit.class);
			addIntent.putExtra("focus", "This value is ignored; only the presence of the key matters.");
			startActivityForResult(addIntent, Integer.MAX_VALUE);
			return true;
		}
		if (item.getItemId() == R.id.deleteAllSubscriptions) {
			new AlertDialog.Builder(Subscriptions.this).setIcon(android.R.drawable.ic_dialog_alert).setMessage("Delete All Subscriptions?")
					.setPositiveButton("Delete", (dialog, which) -> {
						contentService.deleteAllSubscriptions();
						reloadSubscriptions();
					}).setNegativeButton("Cancel", null).show();
		}
		if (item.getItemId() == R.id.resetToDemoSubscriptions) {
			new AlertDialog.Builder(Subscriptions.this).setIcon(android.R.drawable.ic_dialog_alert)
					.setMessage("Reset to Demo Subscriptions (will delete all current subscriptions)?")
					.setPositiveButton("Reset to Demos", (dialog, which) -> {
						contentService.resetToDemoSubscriptions();
						reloadSubscriptions();
					}).setNegativeButton("Cancel", null).show();
			return true;
		}
		if (item.getItemId() == R.id.search) {
			startActivityForResult(new Intent(this, Search.class), Integer.MAX_VALUE);
			return true;
		}
		if (item.getItemId() == R.id.export) {
			exportOpml();
		}
		if (item.getItemId() == R.id.importx) {
            startActivityForResult(new Intent(this, OpmlLocator.class), 0);
        }
		return super.onMenuItemSelected(featureId, item);

	}

	protected void reloadSubscriptions() {
		subscriptions.clear();

		// If we have no content service... then game over... cant display anything
		List<Subscription> sites = new ArrayList<>();
		if (contentService != null) {
			sites = getSubscriptions();
		}
		// sort sites by name:
		Collections.sort(sites);

		for (Subscription sub : sites) {
			Map<String, Object> item = new HashMap<>();
			item.put("name", sub.name);

			if (sub.enabled)
				item.put("enabled", "");
			else
				item.put("enabled", "(Disabled)");

			item.put("subscription", sub);
			subscriptions.add(item);
		}

		listAdapter.notifyDataSetChanged();
	}

	public void exportOpml() {
		File tempFile = null;
		try {

            tempFile = new File(Environment.getExternalStorageDirectory(),"/CarCastResurrected.opml");
            FileOutputStream opmlFile = new FileOutputStream(tempFile);
			contentService.exportOPML(opmlFile);
            opmlFile.close();
		} catch (Exception ex) {
			Util.toast(this, "Problem creating temporary file\n"+ex.getMessage());
			return;
		}
		Log.i("CarCastResurrected", "temporary file is "+tempFile.length());

		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		// Mime type of the attachment (or) u can use sendIntent.setType("*/*")
		sendIntent.setType("text/xml");
		// Subject for the message or Email
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "CarCastResurrected OPML");
		// Full Path to the attachment
		sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile));
		// Use a chooser to decide whether email or mms
		startActivity(Intent.createChooser(sendIntent, "Export OPML..."));
	}
}
