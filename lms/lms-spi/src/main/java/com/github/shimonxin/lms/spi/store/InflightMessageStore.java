package com.github.shimonxin.lms.spi.store;

import java.util.List;

import com.github.shimonxin.lms.spi.events.PublishEvent;

public interface InflightMessageStore {
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
	 * add inbound message to inflight store
	 * 
	 * @param evt
	 */
	void addInFlightInbound(PublishEvent evt);

	/**
	 * 
	 * retrive inbound message
	 * 
	 * @param publishKey
	 * @return
	 */
	PublishEvent retriveInFlightInbound(String publishKey);

	/**
	 * 
	 * clean inbound message form inflight store
	 * 
	 * @param publishKey
	 */
	void cleanInFlightInbound(String publishKey);

	/**
	 * 
	 * add outbound message to inflight store
	 * 
	 * @param evt
	 */
	void addInFlightOutbound(PublishEvent evt);

	/**
	 * 
	 * clean outbound message from inflight store
	 * 
	 * @param clientID
	 * @param messageID
	 */
	void cleanInFlightOutbound(String clientID,int messageID);

	/**
	 * 
	 * read delayed publish (QoS 1 2) for client
	 * 
	 * @param clientID
	 * @param keepAlive
	 * @return
	 */
	List<PublishEvent> retriveDelayedPublishes(String clientID, int keepAlive);
	/**
	 * 
	 * read outbound publish (QoS 1 2) for client
	 * @param clientID
	 * @return
	 */
	List<PublishEvent> retriveOutboundPublishes(String clientID);
	/**
	 * 
	 * read all delayed publish (QoS 1 2)
	 * 
	 * @return
	 */
	List<PublishEvent> retriveDelayedPublishes();
	/**
	 * 
	 * read all delayed publish (QoS 1 2)
	 * 
	 * @return
	 */
	List<PublishEvent> retriveDelayedPublishes(int timeout);
}
