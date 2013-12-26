/**
 * MqttV3ProtocalProcessor.java created at 2013-12-26 下午2:55:07 by ShimonXin
 */
package com.github.shimonxin.lms.spi.messaging.impl;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.shimonxin.lms.parser.Utils;
import com.github.shimonxin.lms.proto.ConnAckMessage;
import com.github.shimonxin.lms.proto.ConnectMessage;
import com.github.shimonxin.lms.proto.MessageIDMessage;
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
import com.github.shimonxin.lms.spi.Constants;
import com.github.shimonxin.lms.spi.MatchingCondition;
import com.github.shimonxin.lms.spi.ServerChannel;
import com.github.shimonxin.lms.spi.SessionDescriptor;
import com.github.shimonxin.lms.spi.SessionManger;
import com.github.shimonxin.lms.spi.events.PublishEvent;
import com.github.shimonxin.lms.spi.messaging.ProtocolProcessor;
import com.github.shimonxin.lms.spi.store.InflightMessageStore;
import com.github.shimonxin.lms.spi.store.PersistMessageStore;
import com.github.shimonxin.lms.spi.store.RetainedMessageStore;
import com.github.shimonxin.lms.spi.store.StoredMessage;
import com.github.shimonxin.lms.spi.store.SubscriptionStore;
import com.github.shimonxin.lms.spi.subscriptions.Subscription;

/**
 * 
 * @author ShimonXin
 * @created 2013-12-26
 * 
 */
public class MqttV3ProtocalProcessor implements ProtocolProcessor {
	private static final Logger LOG = LoggerFactory
			.getLogger(MqttV3ProtocalProcessor.class);
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
		// TODO persist -> outbound inflight
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
			ServerChannel oldSession = sessionManger.get(msg.getClientID())
					.getSession();
			boolean cleanSession = (Boolean) oldSession
					.getAttribute(Constants.CLEAN_SESSION);
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
		SessionDescriptor connDescr = new SessionDescriptor(msg.getUsername(),
				msg.getClientID(), session, msg.isCleanSession());
		sessionManger.put(msg.getClientID(), connDescr);

		int keepAlive = msg.getKeepAlive();
		LOG.debug(String.format("Connect with keepAlive %d s", keepAlive));
		session.setAttribute(Constants.KEEP_ALIVE, keepAlive);
		session.setAttribute(Constants.CLEAN_SESSION, msg.isCleanSession());
		// used to track the client in the subscription and publishing phases.
		session.setAttribute(Constants.ATTR_CLIENTID, msg.getClientID());

		session.setIdleTime(Math.round(keepAlive * 1.5f));
		// Handle will flag
		if (msg.isWillFlag()) {
			QoS willQos = QoS.values()[msg.getWillQos()];
			PublishEvent pubEvt = new PublishEvent(msg.getWillTopic(), willQos,
					msg.getWillMessage().getBytes(), msg.isWillRetain(),
					msg.getClientID(), session);
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
		List<PublishEvent> publishedEvents = persistMessageStore
				.retrivePersistedPublishes(clientID);
		if (publishedEvents == null) {
			LOG.debug("republishStored, no stored publish events");
			return;
		}
		for (PublishEvent pubEvt : publishedEvents) {
			sendPublish(pubEvt.getClientID(), pubEvt.getTopic(),
					pubEvt.getQos(), pubEvt.getMessage(), false,
					pubEvt.getMessageID());
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
		final String topic = evt.getTopic();
		final byte[] message = evt.getMessage();
		boolean retain = evt.isRetain();
		if (qos == QoS.MOST_ONE) {
			publish2Subscribers(topic, qos, message, retain, evt.getMessageID());
		} else if (qos == QoS.LEAST_ONE) {
			processPublishQoS1(evt);
		} else if (qos == QoS.EXACTLY_ONCE) {
			processPublishQoS2(evt);
		} else {
			LOG.error("not support QoS reserved");
		}

		if (retain) {
			retainedMessageStore.storeRetained(evt);
		}
	}

	/**
	 * Flood the subscribers with the message to notify. MessageID is optional
	 * and should only used for QoS 1 and 2
	 * */
	private void publish2Subscribers(String topic, QoS qos, byte[] message,
			boolean retain, Integer messageID) {
		LOG.debug("publish2Subscribers republishing to existing subscribers that matches the topic "
				+ topic);
		for (final Subscription sub : subscriptionStore
				.searchTopicSubscriptions(topic)) {
			LOG.debug("found matching subscription on topic " + sub.getTopic());
			if (sessionManger.containsClient(sub.getClientId())) {
				// online
				if (qos == QoS.MOST_ONE) {
					// QoS 0
					sendPublish(sub.getClientId(), topic, qos, message, false);
				} else {
					// QoS 1 or 2
					// not clean session = false and connected = false => store
					// it
					PublishEvent newPublishEvt = new PublishEvent(topic, qos,
							message, retain, sub.getClientId(), messageID, null);
					if (!sub.isCleanSession() && !sub.isActive()) {
						// clone the event with matching clientID
						persistMessageStore
								.persistedPublishForFuture(newPublishEvt);
					} else {
						inflightMessageStore.addInFlightOutbound(newPublishEvt);
						// publish
						sendPublish(sub.getClientId(), topic, qos, message,
								false);
					}
				}
			} else {
				// off line
				if (qos != QoS.MOST_ONE) {
					PublishEvent newPublishEvt = new PublishEvent(topic, qos,
							message, retain, sub.getClientId(), messageID, null);
					persistMessageStore
							.persistedPublishForFuture(newPublishEvt);
				}
			}
		}
	}

	private void sendPublish(String clientId, String topic, QoS qos,
			byte[] message, boolean retained) {
		sendPublish(clientId, topic, qos, message, retained, 0);
	}

	private void sendPublish(String clientId, String topic, QoS qos,
			byte[] message, boolean retained, int messageID) {
		LOG.debug("notify invoked with event ");
		PublishMessage pubMessage = new PublishMessage();
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
		LOG.debug(String.format("send publish message to client %s : %d %s",
				clientID, pubMessage.getMessageID(), pubMessage.getQos().name()));
		try {
			if (sessionManger.isEmpty()) {
				throw new RuntimeException(
						"Internal bad error, found m_clientIDs to null while it should be initialized, somewhere it's overwritten!!");
			}
			SessionDescriptor sessionDescr = sessionManger.get(clientID);
			if (sessionDescr == null) {
				throw new RuntimeException(String.format(
						"Can't find a SessionDescriptor for client %s ",
						clientID));
			}
			sessionDescr.getSession().write(pubMessage);
		} catch (Throwable t) {
			LOG.error("send publish message to client error", t);

		}
	}

	/**
	 * 
	 * send id message to client
	 * 
	 * @param clientID
	 * @param msg
	 */
	private void sendMessageToClient(String clientID, MessageIDMessage msg) {
		LOG.debug(String.format("send id message to client %s : %s %d",
				clientID, Utils.msgType2String(msg.getMessageType()),
				msg.getMessageID()));
		try {
			if (sessionManger.isEmpty()) {
				return;
			}
			SessionDescriptor sessionDescr = sessionManger.get(clientID);
			if (sessionDescr == null) {
				return;
			}
			sessionDescr.getSession().write(msg);
		} catch (Throwable t) {
			LOG.error(String.format("send id message ERROR! client %s : %s %d",
					clientID, Utils.msgType2String(msg.getMessageType()),
					msg.getMessageID()), t);
		}
	}

	@Override
	public void processPublishQoS1(PublishEvent evt) {
		final QoS qos = evt.getQos();
		final String topic = evt.getTopic();
		final byte[] message = evt.getMessage();
		boolean retain = evt.isRetain();
		String publishKey = String.format("%s%d", evt.getClientID(),
				evt.getMessageID());
		// store the temporary message
		inflightMessageStore.addInFlightInbound(evt);
		publish2Subscribers(topic, qos, message, retain, evt.getMessageID());
		inflightMessageStore.cleanInFlightInbound(publishKey);
		sendPubAck(evt.getClientID(), evt.getMessageID());
		LOG.debug("replying with PubAck to MSG ID " + evt.getMessageID());
	}

	@Override
	public void sendPubAck(String clientID, int messageID) {
		LOG.debug("sendPubAck invoked");
		PubAckMessage pubAckMessage = new PubAckMessage();
		pubAckMessage.setMessageID(messageID);
		sendMessageToClient(clientID, pubAckMessage);
	}

	@Override
	public void processPubAck(String clientID, int messageID) {
		inflightMessageStore.cleanInFlightOutbound(String.format("%s%d",
				clientID, messageID));
	}

	@Override
	public void processPublishQoS2(PublishEvent evt) {
		// store the message in temp store
		inflightMessageStore.addInFlightOutbound(evt);
		sendPubRec(evt.getClientID(), evt.getMessageID());
	}

	@Override
	public void sendPubRec(String clientID, int messageID) {
		LOG.debug(String.format(
				"sendPubRec invoked for clientID %s and messageID %d",
				clientID, messageID));
		PubRecMessage pubRecMessage = new PubRecMessage();
		pubRecMessage.setMessageID(messageID);
		sendMessageToClient(clientID, pubRecMessage);
	}

	/**
	 * Second phase of a publish QoS2 protocol, sent by publisher to the broker.
	 * Search the stored message and publish to all interested subscribers.
	 * 
	 * @see com.github.shimonxin.lms.spi.messaging.ProtocolProcessor#processPubRel(java.lang.String,
	 *      int)
	 */
	@Override
	public void processPubRel(String clientID, int messageID) {
		String publishKey = String.format("%s%d", clientID, messageID);
		inflightMessageStore.cleanInFlightInbound(publishKey);
		sendPubComp(clientID, messageID);
	}

	@Override
	public void sendPubComp(String clientID, int messageID) {
		LOG.debug(String.format(
				"sendPubComp invoked for clientID %s ad messageID %d",
				clientID, messageID));
		PubCompMessage pubCompMessage = new PubCompMessage();
		pubCompMessage.setMessageID(messageID);
		sendMessageToClient(clientID, pubCompMessage);
	}

	@Override
	public void processPubRec(String clientID, int messageID) {
		// once received a PUBREC reply with a PUBREL(messageID)
		LOG.debug(String.format(
				"processPubRec invoked for clientID %s ad messageID %d",
				clientID, messageID));
		PubRelMessage pubRelMessage = new PubRelMessage();
		pubRelMessage.setMessageID(messageID);
		sendMessageToClient(clientID, pubRelMessage);
	}

	@Override
	public void processPubComp(String clientID, int messageID) {
		// once received the PUBCOMP then remove the message from the temp
		// memory
		String publishKey = String.format("%s%d", clientID, messageID);
		inflightMessageStore.cleanInFlightOutbound(publishKey);
	}

	@Override
	public void processDisconnect(ServerChannel session) {
		boolean cleanSession = (Boolean) session
				.getAttribute(Constants.CLEAN_SESSION);
		String clientID = (String) session
				.getAttribute(Constants.ATTR_CLIENTID);
		if (cleanSession) {
			// cleanup topic subscriptions
			processRemoveAllSubscriptions(clientID);
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
			subscriptionStore.removeSubscription(topic,
					(String) session.getAttribute(Constants.ATTR_CLIENTID));
		}
		// ack the client
		UnsubAckMessage ackMessage = new UnsubAckMessage();
		ackMessage.setMessageID(msg.getMessageID());
		LOG.info(String.format("replying with UnsubAck to MSG ID %s",
				msg.getMessageID()));
		session.write(ackMessage);
	}

	@Override
	public void processSubscribe(ServerChannel session, SubscribeMessage msg) {
		boolean cleanSession = (Boolean) session
				.getAttribute(Constants.CLEAN_SESSION);
		String clientID = (String) session
				.getAttribute(Constants.ATTR_CLIENTID);
		LOG.info(String.format(
				"processSubscribe invoked from client %s with msgID %d",
				clientID, msg.getMessageID()));

		for (SubscribeMessage.Couple req : msg.subscriptions()) {
			QoS qos = QoS.values()[req.getQos()];
			Subscription newSubscription = new Subscription(clientID,
					req.getTopic(), qos, cleanSession);
			subscribeSingleTopic(newSubscription, req.getTopic());
		}

		// ack the client
		SubAckMessage ackMessage = new SubAckMessage();
		ackMessage.setMessageID(msg.getMessageID());

		for (int i = 0; i < msg.subscriptions().size(); i++) {
			ackMessage
					.addType(QoS.values()[msg.subscriptions().get(i).getQos()]);
		}
		LOG.info("replying with SubAck to MSG ID " + msg.getMessageID());
		session.write(ackMessage);
	}

	private void subscribeSingleTopic(Subscription newSubscription,
			final String topic) {
		subscriptionStore.add(newSubscription);
		// scans retained messages to be published to the new subscription
		Collection<StoredMessage> messages = retainedMessageStore
				.searchMatching(new MatchingCondition() {
					public boolean match(String key) {
						return subscriptionStore.matchTopics(key, topic);
					}
				});
		for (StoredMessage storedMsg : messages) {
			// fire the as retained the message
			LOG.debug("send publish message for topic " + topic);
			sendPublish(newSubscription.getClientId(), storedMsg.getTopic(),
					storedMsg.getQos(), storedMsg.getPayload(), true);
		}
	}

	public void setInflightMessageStore(
			InflightMessageStore inflightMessageStore) {
		this.inflightMessageStore = inflightMessageStore;
	}

	public void setRetainedMessageStore(
			RetainedMessageStore retainedMessageStore) {
		this.retainedMessageStore = retainedMessageStore;
	}

	public void setSubscriptionStore(SubscriptionStore subscriptionStore) {
		this.subscriptionStore = subscriptionStore;
	}

	@Override
	public void processStop() {
		// TODO outbound inflight -> persist
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

	@Override
	public void processPing(String clientID) {
		// TODO Auto-generated method stub

	}

}
