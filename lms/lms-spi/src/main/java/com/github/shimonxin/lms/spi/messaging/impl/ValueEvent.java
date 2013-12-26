package com.github.shimonxin.lms.spi.messaging.impl;

import com.github.shimonxin.lms.spi.events.MessagingEvent;
import com.lmax.disruptor.EventFactory;

/**
 * Carrier value object for the RingBuffer.
 * 
 * @author andrea
 */
public final class ValueEvent {

	private MessagingEvent m_event;

	public MessagingEvent getEvent() {
		return m_event;
	}

	public void setEvent(MessagingEvent event) {
		m_event = event;
	}

	public final static EventFactory<ValueEvent> EVENT_FACTORY = new EventFactory<ValueEvent>() {

		public ValueEvent newInstance() {
			return new ValueEvent();
		}
	};
}
