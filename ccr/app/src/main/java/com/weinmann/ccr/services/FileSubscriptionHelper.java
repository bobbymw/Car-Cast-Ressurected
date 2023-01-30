package com.weinmann.ccr.services;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import android.util.Log;

import com.weinmann.ccr.core.Config;
import com.weinmann.ccr.core.OrderingPreference;
import com.weinmann.ccr.core.Subscription;
import com.weinmann.ccr.core.Util;
import com.weinmann.ccr.util.ExportOpml;

public class FileSubscriptionHelper {

    private static final String CONCAT_DIVIDER = "\\;";
    private static final String REGEX_DIVIDER = "\\\\;";
    private final Config mConfig;

    public FileSubscriptionHelper(Config config) {
        mConfig = config;
    }

    public boolean addSubscription(Subscription toAdd) {
        List<Subscription> subs = getSubscriptions();

        if (containsSubscriptionURL(subs, toAdd.url)) {
            // we already have the URL stored:
            return false;
        } // endif

        // test the url:
        if (Util.isValidURL(toAdd.url)) {
            // passed, put it in and save:
            subs.add(toAdd);
            saveSubscriptions(subs);

            return true;

        } else {
            Log.e("CarCastResurrected", "addSubscription: bad url: " + toAdd.url);
            return false;
        }
    }

    /**
     * Scan the list for a subscription by its URL
     * 
     * @param subs the list to scan
     * @param url the URL to look for
     * @return <code>true</code> if found in the list, <code>false</code>
     *         otherwise.
     */
    private boolean containsSubscriptionURL(List<Subscription> subs, String url) {
        return indexOfSubscriptionURL(subs, url) != -1;
    }

    /**
     * Insure that all properties are keyed properly, by URL. If a key is not a
     * url, but the value is, then the pair is removed, then placed back in
     * reverse.
     * 
     * @param props the properties to scan
     */
    List<Subscription> convertProperties(Properties props) {
        List<Subscription> subscriptions = new ArrayList<>();

        Set<Object> keys = props.keySet();
        for (Object key : keys) {
            String url = (String) key;
            String nameAndMore = props.getProperty(url, "");
            Subscription sub = convertProperty(url, nameAndMore);
            if (sub != null) {
                subscriptions.add(sub);
            } // endif
        } // endforeach

        return subscriptions;
    }

    private Subscription convertProperty(String url, String nameAndMore) {
        String[] split = nameAndMore.split(REGEX_DIVIDER);
 
        if (split.length == 5) {
	        // best case, we should have all properties:
	        try {
	            String name = split[0];
	            int maxCount = Integer.parseInt(split[1]);
	            OrderingPreference pref = OrderingPreference.valueOf(split[2]);
	            boolean enabled = Boolean.parseBoolean(split[3]);
	            boolean priority = Boolean.parseBoolean(split[4]);
	            return new Subscription(name, url, maxCount, pref, enabled, priority);
	
	        } catch (Exception ex) {
	            Log.w("CarCastResurrected", "couldn't read subscription " + url + "=" + nameAndMore);
	        } // endtry
        } else if (split.length == 4) {
	        // next best case, we should have everything except priority (default to false)
	        try {
	            String name = split[0];
	            int maxCount = Integer.parseInt(split[1]);
	            OrderingPreference pref = OrderingPreference.valueOf(split[2]);
	            boolean enabled = Boolean.parseBoolean(split[3]);
	            return new Subscription(name, url, maxCount, pref, enabled, false);
	
	        } catch (Exception ex) {
	            Log.w("CarCastResurrected", "couldn't read subscription " + url + "=" + nameAndMore);
	        } // endtry
        } else if (split.length == 3) {
	        // third best case, we have all properties except enabled:
	        try {
	            String name = split[0];
	            int maxCount = Integer.parseInt(split[1]);
	            OrderingPreference pref = OrderingPreference.valueOf(split[2]);
	            return new Subscription(name, url, maxCount, pref);
	
	        } catch (Exception ex) {
	            Log.w("CarCastResurrected", "couldn't read subscription " + url + "=" + nameAndMore);
	        } // endtry
        } else if (split.length == 1) {
            String name = split[0];
            // oops, missing extra properties:
            return new Subscription(name, url);

        } else {
            Log.w("CarCastResurrected", "couldn't read subscription " + url + "=" + nameAndMore);
        } // endif
        
        return null;
    }


    public void deleteAllSubscriptions() {
        List<Subscription> emptyList = Collections.emptyList();
        saveSubscriptions(emptyList);
    }


    public boolean editSubscription(Subscription original, Subscription updated) {
        List<Subscription> subs = getSubscriptions();
        int idx = indexOfSubscriptionURL(subs, original.url);
        if (idx != -1) {
            subs.remove(idx);
            subs.add(updated);
            saveSubscriptions(subs);
            return true;
        } // endif

        return false;
    }


    public List<Subscription> getSubscriptions() {
        File subscriptionFile = getSubscriptionFile();

        if (!subscriptionFile.exists()) {
            subscriptionFile.getParentFile().mkdirs();
            return resetToDemoSubscriptions();
        }

        try {
            InputStream dis = new BufferedInputStream(new FileInputStream(subscriptionFile));
            Properties props = new Properties();
            props.load(dis);

            return convertProperties(props);

        } catch (Exception e1) {
            return Collections.emptyList();
        }
    }

    /**
     * Scan the list for a subscription by its URL
     * 
     * @param subs the list to scan
     * @param url the URL to look for
     * @return the index in the list, or -1 if not found
     */
    private int indexOfSubscriptionURL(List<Subscription> subs, String url) {
        for (int i = 0; i < subs.size(); i++) {
            Subscription sub = subs.get(i);
            if (sub.url.equals(url)) {
                return i;
            } // endif
        } // endfor

        // not found:
        return -1;
    }

    List<Subscription> readLegacySites(InputStream input) throws IOException {
        List<Subscription> sites = new ArrayList<>();
        DataInputStream dis = new DataInputStream(input);
        String line = null;
        while ((line = dis.readLine()) != null) {
            int eq = line.indexOf('=');
            if (eq != -1) {
                String name = line.substring(0, eq);
                String url = line.substring(eq + 1);
                if (Util.isValidURL(url)) {
                    sites.add(new Subscription(name, url));
                } // endif

            }
        }

        return sites;
    }


    public boolean removeSubscription(Subscription toRemove) {
        List<Subscription> subs = getSubscriptions();
        int idx = indexOfSubscriptionURL(subs, toRemove.url);
        if (idx != -1) {
            subs.remove(idx);
            saveSubscriptions(subs);
            return true;
        } // endif

        return false;
    }


    public boolean toggleSubscription(Subscription toToggle) {
        List<Subscription> subs = getSubscriptions();
        int idx = indexOfSubscriptionURL(subs, toToggle.url);
        if (idx != -1) {
        	Subscription sub = subs.get(idx);
            sub.enabled = !sub.enabled;
            saveSubscriptions(subs);
            return true;
        } // endif

        return false;
    }
    

    public List<Subscription> resetToDemoSubscriptions() {
        List<Subscription> subs = new ArrayList<>();
        subs.add(new Subscription("Clark Howard", "https://feeds.megaphone.fm/clarkhoward"));
        saveSubscriptions(subs);
        return subs;
    }

    public void exportOPML(FileOutputStream fileOutputStream) {
        ExportOpml.export(getSubscriptions(), fileOutputStream);
    }
    
    private boolean saveSubscriptions(List<Subscription> subscriptions) {
        try {
            File subscriptionFile = getSubscriptionFile();
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(subscriptionFile));
            Properties outSubs = new Properties();
            
            for (Subscription sub : subscriptions) {
                String valueStr = sub.name + CONCAT_DIVIDER + sub.maxDownloads + CONCAT_DIVIDER + sub.orderingPreference.name() + CONCAT_DIVIDER + sub.enabled + CONCAT_DIVIDER + sub.priority;
                outSubs.put(sub.url, valueStr);
            } // endforeach
            
            outSubs.store(bos, "CarCastResurrected Subscription File v3");
            bos.close();

            // success:
            return true;

        } catch (IOException e) {
            // failure:
            return false;
        }
    }

    private File getSubscriptionFile() {
        return mConfig.getCarCastPath("podcasts.properties");
    }
}
