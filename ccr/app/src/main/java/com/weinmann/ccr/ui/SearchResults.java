package com.weinmann.ccr.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.weinmann.ccr.R;
import com.weinmann.ccr.core.CarCastResurrectedApplication;
import com.weinmann.ccr.core.Subscription;
import com.weinmann.ccr.core.Util;

public class SearchResults extends BaseActivity {

	String lastResults;

	@SuppressWarnings("unchecked")
    private void add(int position) {
        ListView listView = findViewById(R.id.siteList);
        Map<String, String> rowData = (Map<String, String>) listView.getAdapter().getItem(position);

        String name = rowData.get("name");
	    String url = rowData.get("url");

		boolean b = contentService.addSubscription(new Subscription(name, url));
		if (b) {
			Util.toast(this, "Added subscription to " + name);
		} else {
			Util.toast(this, "Already subscribed to " + name);
		}
	}

	private List<Subscription> getResults() {
		List<Subscription> res = new ArrayList<>();
		try {
			lastResults = contentService.startSearch("-results-");
			String[] lines = lastResults.split("\\n");
			for (String line : lines) {
				if (!line.trim().equals("") && !line.startsWith("#")) {
				    int eq = line.indexOf('=');
			        if (eq != -1) {
			            String name = line.substring(0, eq);
			            String url = line.substring(eq + 1);
			            res.add(new Subscription(name, url));
			        }
				}
			}
		} catch (Exception e) {
		}
		return res;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			showResults();
		}
	}

	@Override
	protected void onPostContentServiceChanged() {
		showResults();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		if (item.getTitle().equals("Subscribe")) {
			add(info.position);
			return false;
		}
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.subscription_list);

		setTitle(CarCastResurrectedApplication.getAppTitle()+": subscription search results");

		ListView listView = findViewById(R.id.siteList);

		listView.setOnItemClickListener((arg0, arg1, position, arg3) -> add(position));
		registerForContextMenu(listView);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add("Subscribe");
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search_results_menu, menu);
		return true;
	}

	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.searchAgain) {
			finish();
			return true;
		}
		add(((AdapterContextMenuInfo) item.getMenuInfo()).position);
		return true;
	}

	protected void showResults() {

		try {
			ListView listView = findViewById(R.id.siteList);

			List<Subscription> sites = getResults();

			Util.toast(this, "Found " + sites.size() + " results");

			List<Map<String, String>> list = new ArrayList<>();

			for (Subscription sub: sites) {
				Map<String, String> item = new HashMap<>();
				item.put("name", sub.name);
				item.put("url", sub.url);
				list.add(item);

			}
			SimpleAdapter notes = new SimpleAdapter(this, list,
					R.layout.main_item_two_line_row, new String[] { "name",
							"url" }, new int[] { R.id.text1, R.id.text2 });
			listView.setAdapter(notes);
		} catch (Throwable t) {
			Util.toast(this, "Sorry, problem with search results.");
		}
	}

}
