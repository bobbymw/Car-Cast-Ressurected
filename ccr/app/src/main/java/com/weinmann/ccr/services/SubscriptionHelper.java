package com.weinmann.ccr.services;

import java.util.List;

import com.weinmann.ccr.core.Subscription;

public interface SubscriptionHelper {
    public boolean addSubscription(Subscription toAdd);
    public void deleteAllSubscriptions();
    public boolean editSubscription(Subscription original, Subscription updated);
    public List<Subscription> getSubscriptions();
    public boolean removeSubscription(Subscription toRemove);
    public List<Subscription> resetToDemoSubscriptions();
	boolean toggleSubscription(Subscription toToggle);
}
