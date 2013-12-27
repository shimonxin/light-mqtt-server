package com.github.shimonxin.lms.spi.store;

import java.util.List;

import com.github.shimonxin.lms.spi.events.PublishEvent;

public interface PersistMessageStore {
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
	 * persisted Publish Event for future
	 * 
	 * @param newPublishEvt
	 */
	void persistedPublishForFuture(PublishEvent newPublishEvt);

	/**
	 * 
	 * retrive persisted publish events
	 * 
	 * @param clientId
	 * @return
	 */
	List<PublishEvent> retrivePersistedPublishes(String clientId);

	/**
	 * 
	 * clean all persisted publish for client
	 * 
	 * @param clientID
	 */
	void cleanPersistedPublishes(String clientID);

	/**
	 * 
	 * remove an offline message
	 * 
	 * @param clientID
	 * @param messageID
	 */
	void removePersistedPublish(String clientID, int messageID);
	/**
	 * 
	 * persisted Publish Events for future
	 * @param newPublishEvts
	 */
	void persistedPublishsForFuture(List<PublishEvent> newPublishEvts);
}
