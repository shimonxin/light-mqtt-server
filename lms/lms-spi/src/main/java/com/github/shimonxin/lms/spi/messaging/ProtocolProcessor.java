/**
 * Processor.java created at 2013-12-26 下午1:50:08 by ShimonXin
 */
package com.github.shimonxin.lms.spi.messaging;

import com.github.shimonxin.lms.proto.ConnectMessage;
import com.github.shimonxin.lms.proto.SubscribeMessage;
import com.github.shimonxin.lms.proto.UnsubscribeMessage;
import com.github.shimonxin.lms.spi.ServerChannel;
import com.github.shimonxin.lms.spi.events.PublishEvent;

/**
 * Protocol Processor
 * 
 * @author ShimonXin
 * @created 2013-12-26
 * 
 */
public interface ProtocolProcessor {
	/**
	 * 
	 * start initialization
	 */
	void processInit();
	/**
	 * 
	 * start stop
	 */
	void processStop();
	
	/**
	 * 
	 * process connect
	 * 
	 * @param session
	 * @param msg
	 */
	void processConnect(ServerChannel session, ConnectMessage msg);

	/**
	 * 
	 * process publish
	 * 
	 * @param evt
	 */
	void processPublish(PublishEvent evt);

	// Receive a message of QoS 1
	/**
	 * 
	 * process publish QoS 1
	 * 
	 * @param evt
	 */
	void processPublishQoS1(PublishEvent evt);

	/**
	 * 
	 * send publish ack
	 * 
	 * @param evt
	 */
	void sendPubAck(String clientID, int messageID);

	// Send a message of QoS 1
	/**
	 * 
	 * process publish ack
	 * 
	 * @param clientID
	 * @param messageID
	 */
	void processPubAck(String clientID, int messageID);

	// Receive a message of QoS 2
	/**
	 * 
	 * process publish QoS 2
	 * 
	 * @param evt
	 */
	void processPublishQoS2(PublishEvent evt);

	/**
	 * 
	 * send publish rec
	 * 
	 * @param clientID
	 * @param messageID
	 */
	void sendPubRec(String clientID, int messageID);

	/**
	 * 
	 * process publish rel
	 * 
	 * @param clientID
	 * @param messageID
	 */
	void processPubRel(String clientID, int messageID);

	/**
	 * 
	 * send pub comp
	 * 
	 * @param clientID
	 * @param messageID
	 */
	void sendPubComp(String clientID, int messageID);

	// Send a message of QoS 2
	/**
	 * 
	 * process pub rec
	 * 
	 * @param clientID
	 * @param messageID
	 */
	void processPubRec(String clientID, int messageID);

	/**
	 * 
	 * process pub comp
	 * 
	 * @param clientID
	 * @param messageID
	 */
	void processPubComp(String clientID, int messageID);

	/**
	 * 
	 * process disconnect
	 * 
	 * @param session
	 */
	void processDisconnect(ServerChannel session);

	/**
	 * 
	 * process unsubscribe
	 * 
	 * @param session
	 * @param msg
	 */
	void processUnsubscribe(ServerChannel session, UnsubscribeMessage msg);

	/**
	 * 
	 * process subcribe
	 * 
	 * @param session
	 * @param msg
	 */
	void processSubscribe(ServerChannel session, SubscribeMessage msg);
	
	/**
	 * 
	 * process ping
	 * @param clientID
	 */
	void processPing(String clientID);
}
