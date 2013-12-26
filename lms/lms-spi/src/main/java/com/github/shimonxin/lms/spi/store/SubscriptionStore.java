package com.github.shimonxin.lms.spi.store;

import java.util.List;

import com.github.shimonxin.lms.spi.subscriptions.Subscription;

public interface SubscriptionStore {
	/**
	 * 
	 * init
	 */
	void init();

	/**
	 * stop
	 */
	void stop();

	/**
	 * 
	 * active client　subscription
	 * 
	 * @param clientID
	 */
	void activate(String clientID);

	/**
	 * 
	 * search topic subscriptions
	 * 
	 * @param topic
	 * @return
	 */
	List<Subscription> searchTopicSubscriptions(String topic);

	/**
	 * 
	 * remove all subscriptions for client
	 * 
	 * @param clientID
	 */
	void removeForClient(String clientID);

	/**
	 * 
	 * deactive client　subscription
	 * 
	 * @param clientID
	 */
	void deactivate(String clientID);

	/**
	 * 
	 * remove subscription for client
	 * 
	 * @param topic
	 * @param clientID
	 */
	void removeSubscription(String topic, String clientID);
	/**
	 * 
	 * add subscription
	 * @param newSubscription
	 */
	void add(Subscription newSubscription);
	/**
	 * 
	 * TODO SubscriptionStore.matchTopics comment
	 * @param key
	 * @param topic
	 * @return
	 */
	boolean isSubscribed(String key, String topic);

	boolean matchTopics(String key, String topic);
}
