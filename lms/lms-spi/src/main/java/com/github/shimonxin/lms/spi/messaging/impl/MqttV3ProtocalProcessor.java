/**
 * MqttV3ProtocalProcessor.java created at 2013-12-26 下午2:55:07 by ShimonXin
 */
package com.github.shimonxin.lms.spi.messaging.impl;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.shimonxin.lms.proto.ConnAckMessage;
import com.github.shimonxin.lms.proto.ConnectMessage;
import com.github.shimonxin.lms.proto.PubAckMessage;
import com.github.shimonxin.lms.proto.PubCompMessage;
import com.github.shimonxin.lms.proto.PubRecMessage;
import com.github.shimonxin.lms.proto.PubRelMessage;
import com.github.shimonxin.lms.proto.PublishMessage;
import com.github.shimonxin.lms.proto.QoS;
import com.github.shimonxin.lms.proto.SubAckMessage;
import com.github.shimonxin.lms.proto.SubscribeMessage;
import com.github.shimonxin.lms.proto.UnsubAckMessage;
import com.github.shimonxin.lms.proto.UnsubscribeMessage;
import com.github.shimonxin.lms.spi.Authenticator;
import com.github.shimonxin.lms.spi.events.PublishEvent;
import com.github.shimonxin.lms.spi.messaging.ProtocolProcessor;
import com.github.shimonxin.lms.spi.session.ServerChannel;
import com.github.shimonxin.lms.spi.session.SessionConstants;
import com.github.shimonxin.lms.spi.session.SessionDescriptor;
import com.github.shimonxin.lms.spi.session.SessionManger;
import com.github.shimonxin.lms.spi.store.InflightMessageStore;
import com.github.shimonxin.lms.spi.store.PersistMessageStore;
import com.github.shimonxin.lms.spi.store.RetainedMessageStore;
import com.github.shimonxin.lms.spi.store.StoredMessage;
import com.github.shimonxin.lms.spi.store.SubscriptionStore;
import com.github.shimonxin.lms.spi.subscriptions.MatchingCondition;
import com.github.shimonxin.lms.spi.subscriptions.Subscription;

/**
 * 
 * @author ShimonXin
 * @created 2013-12-26
 * 
 */
public class MqttV3ProtocalProcessor implements ProtocolProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(MqttV3ProtocalProcessor.class);
	InflightMessageStore inflightMessageStore;
	PersistMessageStore persistMessageStore;
	RetainedMessageStore retainedMessageStore;
	SubscriptionStore subscriptionStore;
	Authenticator authenticator;
	SessionManger sessionManger;
	boolean forceLogin;

	@Override
	public void processInit() {
		inflightMessageStore.init();
		persistMessageStore.init();
		retainedMessageStore.init();
		subscriptionStore.init();
	}

	@Override
	public void processConnect(ServerChannel session, ConnectMessage msg) {
		LOG.info("processConnect for client " + msg.getClientID());
		if (msg.getProcotolVersion() != 0x03) {
			ConnAckMessage badProto = new ConnAckMessage();
			badProto.setReturnCode(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION);
			LOG.info("processConnect sent bad proto ConnAck");
			session.write(badProto);
			session.close();
			return;
		}
		if (forceLogin && !msg.isUserFlag()) {
			// user must be authenticated
			ConnAckMessage okResp = new ConnAckMessage();
			okResp.setReturnCode(ConnAckMessage.NOT_AUTHORIZED);
			session.write(okResp);
			return;
		}

		// client ID can not greater than 64 characters
		if (msg.getClientID() == null || msg.getClientID().length() > 23) {
			ConnAckMessage okResp = new ConnAckMessage();
			okResp.setReturnCode(ConnAckMessage.IDENTIFIER_REJECTED);
			session.write(okResp);
			return;
		}

		// if an old client with the same ID already exists close its session.
		if (sessionManger.containsClient(msg.getClientID())) {
			// clean the subscriptions if the old used a cleanSession = true
			ServerChannel oldSession = sessionManger.get(msg.getClientID()).getSession();
			boolean cleanSession = (Boolean) oldSession.getAttribute(SessionConstants.CLEAN_SESSION);
			if (cleanSession) {
				// cleanup topic subscriptions
				processRemoveAllSubscriptions(msg.getClientID());
			}
			oldSession.close();
		}

		// handle user authentication
		if (msg.isUserFlag()) {
			String pwd = null;
			if (msg.isPasswordFlag()) {
				pwd = msg.getPassword();
			}
			if (!authenticator.auth(msg.getClientID(), msg.getUsername(), pwd)) {
				ConnAckMessage okResp = new ConnAckMessage();
				okResp.setReturnCode(ConnAckMessage.BAD_USERNAME_OR_PASSWORD);
				session.write(okResp);
				return;
			}
		}
		int keepAlive = msg.getKeepAlive();
		SessionDescriptor connDescr = new SessionDescriptor(msg.getUsername(), msg.getClientID(), session, msg.isCleanSession(), keepAlive);
		sessionManger.put(msg.getClientID(), connDescr);
		session.setAttribute(SessionConstants.KEEP_ALIVE, keepAlive);
		session.setAttribute(SessionConstants.CLEAN_SESSION, msg.isCleanSession());
		// used to track the client in the subscription and publishing phases.
		session.setAttribute(SessionConstants.ATTR_CLIENTID, msg.getClientID());
		session.setIdleTime(Math.round(keepAlive * 1.5f));
		LOG.debug(String.format("Connect as user[%s] with clientID[%s] keepAlive[%ds]", msg.getUsername(), msg.getClientID(), keepAlive));
		// Handle will flag
		if (msg.isWillFlag()) {
			QoS willQos = QoS.values()[msg.getWillQos()];
			PublishEvent pubEvt = new PublishEvent(msg.getWillTopic(), willQos, msg.getWillMessage().getBytes(), msg.isWillRetain(), msg.getClientID(), session);
			processPublish(pubEvt);
		}

		subscriptionStore.activate(msg.getClientID());

		// handle clean session flag
		if (msg.isCleanSession()) {
			// remove all prev subscriptions
			// cleanup topic subscriptions
			processRemoveAllSubscriptions(msg.getClientID());
		}

		ConnAckMessage okResp = new ConnAckMessage();
		okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
		LOG.info("processConnect sent OK ConnAck");
		session.write(okResp);

		if (!msg.isCleanSession()) {
			// force the republish of stored QoS1 and QoS2
			republishOfflineMessages(msg.getClientID());
		}
	}

	/**
	 * 
	 * republish offline messages
	 * 
	 * @param clientID
	 */
	private void republishOfflineMessages(String clientID) {
		LOG.debug("republishStored invoked");
		List<PublishEvent> publishedEvents = persistMessageStore.retrivePersistedPublishes(clientID);
		if (publishedEvents == null || publishedEvents.isEmpty()) {
			LOG.debug("republishStored, no stored publish events");
			return;
		}
		for (PublishEvent pubEvt : publishedEvents) {
			sendPublish(pubEvt.getClientID(), pubEvt.getTopic(), pubEvt.getQos(), pubEvt.getMessage(), false, pubEvt.getMessageID(), true);
		}
	}

	/**
	 * 
	 * cleanup topic subscriptions
	 * 
	 * @param clientID
	 */
	private void processRemoveAllSubscriptions(String clientID) {
		LOG.debug("processRemoveAllSubscriptions invoked");
		subscriptionStore.removeForClient(clientID);

		// remove also the messages stored of type QoS1/2
		persistMessageStore.cleanPersistedPublishes(clientID);
	}

	@Override
	public void processPublish(PublishEvent evt) {
		LOG.debug("processPublish invoked with " + evt);
		final QoS qos = evt.getQos();
		if (qos == QoS.RESERVED) {
			LOG.error("not support QoS reserved");
			return;
		}
		ServerChannel session = evt.getSession();
		String clientID = evt.getClientID();
		Integer messageID = evt.getMessageID();
		boolean retain = evt.isRetain();
		final String topic = evt.getTopic();
		final byte[] message = evt.getMessage();
		String publishKey = String.format("%s%d", clientID, messageID);
		if (qos == QoS.MOST_ONE) {
			publish2Subscribers(topic, qos, message, retain, messageID);
		} else if (qos == QoS.LEAST_ONE) {
			// store the temporary message
			inflightMessageStore.addInFlightInbound(evt);
			publish2Subscribers(topic, qos, message, retain, messageID);
			inflightMessageStore.cleanInFlightInbound(publishKey);
			PubAckMessage pubAckMessage = new PubAckMessage();
			pubAckMessage.setMessageID(messageID);
			session.write(pubAckMessage);
		} else if (qos == QoS.EXACTLY_ONCE) {
			// store the temporary message
			inflightMessageStore.addInFlightInbound(evt);
			PubRecMessage pubRecMessage = new PubRecMessage();
			pubRecMessage.setMessageID(messageID);
			session.write(pubRecMessage);
		}
		if (retain) {
			retainedMessageStore.storeRetained(evt);
		}
	}

	/**
	 * Flood the subscribers with the message to notify. MessageID is optional and should only used for QoS 1 and 2
	 * */
	private void publish2Subscribers(String topic, QoS qos, byte[] message, boolean retain, Integer messageID) {
		LOG.debug("publish2Subscribers republishing to existing subscribers that matches the topic " + topic);
		for (final Subscription sub : subscriptionStore.searchTopicSubscriptions(topic)) {
			LOG.debug("found matching subscription on topic " + sub.getTopic());
			if (sessionManger.containsClient(sub.getClientId())) {
				// online
				if (qos == QoS.MOST_ONE) {
					// QoS 0
					sendPublish(sub.getClientId(), topic, qos, message, false);
				} else {
					// QoS 1 or 2 not clean session = false and connected = false => store it
					PublishEvent newPublishEvt = new PublishEvent(topic, qos, message, retain, sub.getClientId(), messageID, null);
					if (!sub.isCleanSession() && !sub.isActive()) {
						// clone the event with matching clientID
						persistMessageStore.persistedPublishForFuture(newPublishEvt);
					} else {
						inflightMessageStore.addInFlightOutbound(newPublishEvt);
						// publish
						sendPublish(sub.getClientId(), topic, qos, message, false);
					}
				}
			} else {
				// off line
				if (qos != QoS.MOST_ONE) {
					PublishEvent newPublishEvt = new PublishEvent(topic, qos, message, retain, sub.getClientId(), messageID, null);
					persistMessageStore.persistedPublishForFuture(newPublishEvt);
				}
			}
		}
	}

	private void sendPublish(String clientId, String topic, QoS qos, byte[] message, boolean retained) {
		sendPublish(clientId, topic, qos, message, retained, 0, false);
	}

	private void sendPublish(String clientId, String topic, QoS qos, byte[] message, boolean retained, int messageID, boolean dupFlag) {
		LOG.debug("notify invoked with event ");
		PublishMessage pubMessage = new PublishMessage();
		pubMessage.setDupFlag(dupFlag);
		pubMessage.setRetainFlag(retained);
		pubMessage.setTopicName(topic);
		pubMessage.setQos(qos);
		pubMessage.setPayload(message);
		if (pubMessage.getQos() != QoS.MOST_ONE) {
			pubMessage.setMessageID(messageID);
		}
		sendMessageToClient(clientId, pubMessage);
	}

	/**
	 * 
	 * send publish message to client
	 * 
	 * @param clientID
	 * @param msg
	 */
	private void sendMessageToClient(String clientID, PublishMessage pubMessage) {
		LOG.debug(String.format("send publish message to client %s : %d %s", clientID, pubMessage.getMessageID(), pubMessage.getQos().name()));
		try {
			if (sessionManger.isEmpty()) {
				throw new RuntimeException("Internal bad error, found m_clientIDs to null while it should be initialized, somewhere it's overwritten!!");
			}
			SessionDescriptor sessionDescr = sessionManger.get(clientID);
			if (sessionDescr == null) {
				throw new RuntimeException(String.format("Can't find a SessionDescriptor for client %s ", clientID));
			}
			sessionDescr.getSession().write(pubMessage);
		} catch (Throwable t) {
			LOG.error("send publish message to client error", t);
			// TODO QoS 1 2 Persist ?
		}
	}

	@Override
	public void processPubAck(String clientID, int messageID) {
		inflightMessageStore.cleanInFlightOutbound(clientID, messageID);
		persistMessageStore.removePersistedPublish(clientID, messageID);
	}

	/**
	 * Second phase of a publish QoS2 protocol, sent by publisher to the broker. Search the stored message and publish to all interested subscribers.
	 * 
	 * @see com.github.shimonxin.lms.spi.messaging.ProtocolProcessor#processPubRel(java.lang.String, int)
	 */
	@Override
	public void processPubRel(ServerChannel session, int messageID) {
		String clientID = (String) session.getAttribute(SessionConstants.ATTR_CLIENTID);
		String publishKey = String.format("%s%d", clientID, messageID);
		PublishEvent evt = inflightMessageStore.retriveInFlightInbound(publishKey);
		if (evt != null) {
			publish2Subscribers(evt.getTopic(), evt.getQos(), evt.getMessage(), false, evt.getMessageID());
			inflightMessageStore.cleanInFlightInbound(publishKey);
		}
		PubCompMessage pubCompMessage = new PubCompMessage();
		pubCompMessage.setMessageID(messageID);
		session.write(pubCompMessage);
	}

	@Override
	public void processPubRec(ServerChannel session, int messageID) {
		// once received a PUBREC reply with a PUBREL(messageID)
		String clientID = (String) session.getAttribute(SessionConstants.ATTR_CLIENTID);
		LOG.debug(String.format("processPubRec invoked for clientID %s ad messageID %d", clientID, messageID));
		PubRelMessage pubRelMessage = new PubRelMessage();
		pubRelMessage.setMessageID(messageID);
		session.write(pubRelMessage);
	}

	@Override
	public void processPubComp(ServerChannel session, int messageID) {
		// once received the PUBCOMP then remove the message from the temp memory
		String clientID = (String) session.getAttribute(SessionConstants.ATTR_CLIENTID);
		inflightMessageStore.cleanInFlightOutbound(clientID, messageID);
		persistMessageStore.removePersistedPublish(clientID, messageID);
	}

	@Override
	public void processDisconnect(ServerChannel session) {
		boolean cleanSession = (Boolean) session.getAttribute(SessionConstants.CLEAN_SESSION);
		String clientID = (String) session.getAttribute(SessionConstants.ATTR_CLIENTID);
		if (cleanSession) {
			// cleanup topic subscriptions
			processRemoveAllSubscriptions(clientID);
		} else {
			// save inflight messages to persist
			persistMessageStore.persistedPublishsForFuture(inflightMessageStore.retriveOutboundPublishes(clientID));
		}
		sessionManger.remove(clientID);
		session.close();
		// de-activate the subscriptions for this ClientID
		subscriptionStore.deactivate(clientID);
	}

	@Override
	public void processUnsubscribe(ServerChannel session, UnsubscribeMessage msg) {
		LOG.debug("processSubscribe invoked");
		for (String topic : msg.topics()) {
			subscriptionStore.removeSubscription(topic, (String) session.getAttribute(SessionConstants.ATTR_CLIENTID));
		}
		// ack the client
		UnsubAckMessage ackMessage = new UnsubAckMessage();
		ackMessage.setMessageID(msg.getMessageID());
		LOG.info(String.format("replying with UnsubAck to MSG ID %s", msg.getMessageID()));
		session.write(ackMessage);
	}

	@Override
	public void processSubscribe(ServerChannel session, SubscribeMessage msg) {
		boolean cleanSession = (Boolean) session.getAttribute(SessionConstants.CLEAN_SESSION);
		String clientID = (String) session.getAttribute(SessionConstants.ATTR_CLIENTID);
		LOG.info(String.format("processSubscribe invoked from client %s with msgID %d", clientID, msg.getMessageID()));
		for (SubscribeMessage.Couple req : msg.subscriptions()) {
			QoS qos = QoS.values()[req.getQos()];
			Subscription newSubscription = new Subscription(clientID, req.getTopic(), qos, cleanSession);
			subscribeSingleTopic(newSubscription, req.getTopic());
		}
		// ack the client
		SubAckMessage ackMessage = new SubAckMessage();
		ackMessage.setMessageID(msg.getMessageID());
		for (int i = 0; i < msg.subscriptions().size(); i++) {
			ackMessage.addType(QoS.values()[msg.subscriptions().get(i).getQos()]);
		}
		LOG.info("replying with SubAck to MSG ID " + msg.getMessageID());
		session.write(ackMessage);
	}

	private void subscribeSingleTopic(Subscription newSubscription, final String topic) {
		subscriptionStore.add(newSubscription);
		// scans retained messages to be published to the new subscription
		Collection<StoredMessage> messages = retainedMessageStore.searchMatching(new MatchingCondition() {
			public boolean match(String key) {
				return subscriptionStore.matchTopics(key, topic);
			}
		});
		for (StoredMessage storedMsg : messages) {
			// fire the as retained the message
			LOG.debug("send publish message for topic " + topic);
			sendPublish(newSubscription.getClientId(), storedMsg.getTopic(), storedMsg.getQos(), storedMsg.getPayload(), true);
		}
	}

	public void setInflightMessageStore(InflightMessageStore inflightMessageStore) {
		this.inflightMessageStore = inflightMessageStore;
	}

	public void setRetainedMessageStore(RetainedMessageStore retainedMessageStore) {
		this.retainedMessageStore = retainedMessageStore;
	}

	public void setSubscriptionStore(SubscriptionStore subscriptionStore) {
		this.subscriptionStore = subscriptionStore;
	}

	@Override
	public void processPing(ServerChannel session) {
		String clientID = (String) session.getAttribute(SessionConstants.ATTR_CLIENTID);
		int keepAlive = (Integer) session.getAttribute(SessionConstants.KEEP_ALIVE);
		republishDelayedMessages(clientID, keepAlive);
	}

	/**
	 * 
	 * republish delayed messages
	 * 
	 * @param clientID
	 */
	private void republishDelayedMessages(String clientID, int keepAlive) {
		LOG.debug(String.format("republishDelayedMessages invoked for client[%s] keepAlive[%ds]", clientID, keepAlive));
		List<PublishEvent> publishedEvents = inflightMessageStore.retriveDelayedPublishes(clientID, keepAlive);
		if (publishedEvents == null || publishedEvents.isEmpty()) {
			LOG.debug("republishStored, no stored publish events");
			return;
		}
		for (PublishEvent pubEvt : publishedEvents) {
			sendPublish(pubEvt.getClientID(), pubEvt.getTopic(), pubEvt.getQos(), pubEvt.getMessage(), false, pubEvt.getMessageID(), true);
		}
	}

	@Override
	public void processStop() {
		// outbound inflight -> persist
		persistMessageStore.persistedPublishsForFuture(inflightMessageStore.retriveDelayedPublishes());
		inflightMessageStore.stop();
		persistMessageStore.stop();
		retainedMessageStore.stop();
		subscriptionStore.stop();
	}

	public void setAuthenticator(Authenticator authenticator) {
		this.authenticator = authenticator;
	}

	public void setSessionManger(SessionManger sessionManger) {
		this.sessionManger = sessionManger;
	}

	public void setForceLogin(boolean forceLogin) {
		this.forceLogin = forceLogin;
	}

}
