package com.github.shimonxin.lms.spi.messaging.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.shimonxin.lms.proto.AbstractMessage;
import com.github.shimonxin.lms.proto.ConnectMessage;
import com.github.shimonxin.lms.proto.DisconnectMessage;
import com.github.shimonxin.lms.proto.PubAckMessage;
import com.github.shimonxin.lms.proto.PubCompMessage;
import com.github.shimonxin.lms.proto.PubRecMessage;
import com.github.shimonxin.lms.proto.PubRelMessage;
import com.github.shimonxin.lms.proto.PublishMessage;
import com.github.shimonxin.lms.proto.QoS;
import com.github.shimonxin.lms.proto.SubscribeMessage;
import com.github.shimonxin.lms.proto.UnsubscribeMessage;
import com.github.shimonxin.lms.spi.events.DisconnectEvent;
import com.github.shimonxin.lms.spi.events.InitEvent;
import com.github.shimonxin.lms.spi.events.LostConnectionEvent;
import com.github.shimonxin.lms.spi.events.MessagingEvent;
import com.github.shimonxin.lms.spi.events.ProtocolEvent;
import com.github.shimonxin.lms.spi.events.PublishEvent;
import com.github.shimonxin.lms.spi.events.StopEvent;
import com.github.shimonxin.lms.spi.messaging.Messaging;
import com.github.shimonxin.lms.spi.messaging.ProtocolProcessor;
import com.github.shimonxin.lms.spi.session.ServerChannel;
import com.github.shimonxin.lms.spi.session.SessionConstants;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;

/**
 * 
 * LMAX queue messaging
 * 
 * @author ShimonXin
 * @created 2013-12-26
 * 
 */
public class LmaxQueueMessaging implements Messaging, EventHandler<ValueEvent> {
	private static final Logger LOG = LoggerFactory.getLogger(LmaxQueueMessaging.class);
	private BatchEventProcessor<ValueEvent> m_eventProcessor;
	private RingBuffer<ValueEvent> m_ringBuffer;
	private ExecutorService m_executor;
	private ProtocolProcessor m_processor;

	public void setProtocolProcessor(ProtocolProcessor processer) {
		m_processor = processer;
	}

	private void disruptorPublish(MessagingEvent msgEvent) {
		LOG.debug("disruptorPublish publishing event " + msgEvent);
		long sequence = m_ringBuffer.next();
		ValueEvent event = m_ringBuffer.get(sequence);
		event.setEvent(msgEvent);
		m_ringBuffer.publish(sequence);
	}

	@Override
	public void disconnect(ServerChannel session) {
		disruptorPublish(new DisconnectEvent(session));
	}

	@Override
	public void lostConnection(ServerChannel session) {
		disruptorPublish(new LostConnectionEvent(session));
	}

	@Override
	public void handleProtocolMessage(ServerChannel session, AbstractMessage msg) {
		disruptorPublish(new ProtocolEvent(session, msg));
	}

	@Override
	public void init() {
		LOG.debug("processInit invoked");
		m_executor = Executors.newFixedThreadPool(1);

		m_ringBuffer = new RingBuffer<ValueEvent>(ValueEvent.EVENT_FACTORY, 1024 * 32);

		SequenceBarrier barrier = m_ringBuffer.newBarrier();
		m_eventProcessor = new BatchEventProcessor<ValueEvent>(m_ringBuffer, barrier, this);
		// TODO in a presentation is said to don't do the followinf line!!
		m_ringBuffer.setGatingSequences(m_eventProcessor.getSequence());
		m_executor.submit(m_eventProcessor);
		disruptorPublish(new InitEvent());
	}

	@Override
	public void stop() {
		disruptorPublish(new StopEvent());
	}

	@Override
	public void onEvent(ValueEvent t, long l, boolean bln) throws Exception {
		MessagingEvent evt = t.getEvent();
		LOG.info("onEvent processing messaging event " + evt);
		if (evt instanceof PublishEvent) {
			m_processor.processPublish((PublishEvent) evt);
		} else if (evt instanceof StopEvent) {
			processStop();
		} else if (evt instanceof DisconnectEvent) {
			DisconnectEvent disEvt = (DisconnectEvent) evt;
			m_processor.processDisconnect(disEvt.getSession());
		} else if (evt instanceof ProtocolEvent) {
			ServerChannel session = ((ProtocolEvent) evt).getSession();
			AbstractMessage message = ((ProtocolEvent) evt).getMessage();
			if (message instanceof ConnectMessage) {
				m_processor.processConnect(session, (ConnectMessage) message);
			} else if (message instanceof PublishMessage) {
				PublishMessage pubMsg = (PublishMessage) message;
				PublishEvent pubEvt;
				String clientID = (String) session.getAttribute(SessionConstants.ATTR_CLIENTID);
				if (message.getQos() == QoS.MOST_ONE) {
					pubEvt = new PublishEvent(pubMsg.getTopicName(), pubMsg.getQos(), pubMsg.getPayload(), pubMsg.isRetainFlag(), clientID, session);
				} else {
					pubEvt = new PublishEvent(pubMsg.getTopicName(), pubMsg.getQos(), pubMsg.getPayload(), pubMsg.isRetainFlag(), clientID,
							pubMsg.getMessageID(), session);
				}
				m_processor.processPublish(pubEvt);
			} else if (message instanceof DisconnectMessage) {
				m_processor.processDisconnect(session);
			} else if (message instanceof UnsubscribeMessage) {
				UnsubscribeMessage unsubMsg = (UnsubscribeMessage) message;
				m_processor.processUnsubscribe(session, unsubMsg);
			} else if (message instanceof SubscribeMessage) {
				m_processor.processSubscribe(session, (SubscribeMessage) message);
			} else if (message instanceof PubRelMessage) {
				int messageID = ((PubRelMessage) message).getMessageID();
				m_processor.processPubRel(session, messageID);
			} else if (message instanceof PubRecMessage) {
				int messageID = ((PubRecMessage) message).getMessageID();
				m_processor.processPubRec(session, messageID);
			} else if (message instanceof PubCompMessage) {
				int messageID = ((PubCompMessage) message).getMessageID();
				m_processor.processPubComp(session, messageID);
			} else if (message instanceof PubAckMessage) {
				String clientID = (String) session.getAttribute(SessionConstants.ATTR_CLIENTID);
				int messageID = ((PubAckMessage) message).getMessageID();
				m_processor.processPubAck(clientID, messageID);
			} else {
				throw new RuntimeException("Illegal message received " + message);
			}

		} else if (evt instanceof InitEvent) {
			processInit();
		} else if (evt instanceof LostConnectionEvent) {
			LostConnectionEvent lostEvt = (LostConnectionEvent) evt;
			m_processor.proccessConnectionLost(lostEvt.getSession());
		}
	}

	private void processInit() {

		m_processor.processInit();
	}

	private void processStop() {
		LOG.debug("processStop invoked");
		m_processor.processStop();
		m_executor.shutdown();
	}

}
