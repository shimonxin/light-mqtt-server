/**
 * MessageStore.java created at 2013-12-26 下午12:36:01 by ShimonXin
 */
package com.github.shimonxin.lms.spi.store;

import java.util.Collection;

import com.github.shimonxin.lms.spi.events.PublishEvent;
import com.github.shimonxin.lms.spi.subscriptions.MatchingCondition;

/**
 * retained message store
 * @author ShimonXin
 * @created 2013-12-26
 * 
 */
public interface RetainedMessageStore {
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
	 * store retained
	 * @param evt
	 */
	void storeRetained(PublishEvent evt);
	/**
	 * 
	 * retrive retained messages
	 * @param matchingCondition
	 * @return
	 */
	Collection<StoredMessage> searchMatching(MatchingCondition matchingCondition);
}
