package com.github.shimonxin.lms.spi.store;

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
	 * @param publishKey
	 */
	void cleanInFlightOutbound(String publishKey);
}
