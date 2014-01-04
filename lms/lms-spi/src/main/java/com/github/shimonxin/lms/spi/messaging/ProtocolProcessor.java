/**
 * Processor.java created at 2013-12-26 下午1:50:08 by ShimonXin
 */
package com.github.shimonxin.lms.spi.messaging;

import com.github.shimonxin.lms.proto.ConnectMessage;
import com.github.shimonxin.lms.proto.SubscribeMessage;
import com.github.shimonxin.lms.proto.UnsubscribeMessage;
import com.github.shimonxin.lms.spi.events.PublishEvent;
import com.github.shimonxin.lms.spi.session.ServerChannel;

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
	 * force login ?
	 * @param forceLogin
	 */
	void setForceLogin(boolean forceLogin);
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
	 * process inbound publish
	 * 
	 * @param session
	 * @param evt
	 */
	void processPublish(PublishEvent evt);

	// Send a message of QoS 1
	/**
	 * 
	 * process publish ack
	 * 
	 * @param clientID
	 * @param messageID
	 */
	void processPubAck(String clientID, int messageID);

	/**
	 * 
	 * process publish rel
	 * 
	 * @param clientID
	 * @param messageID
	 */
	void processPubRel(ServerChannel session, int messageID);

	// Send a message of QoS 2
	/**
	 * 
	 * process pub rec
	 * 
	 * @param clientID
	 * @param messageID
	 */
	void processPubRec(ServerChannel session, int messageID);

	/**
	 * 
	 * process pub comp
	 * 
	 * @param clientID
	 * @param messageID
	 */
	void processPubComp(ServerChannel session, int messageID);

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
	 * connection lost
	 * @param clientID
	 */
	void proccessConnectionLost(String clientID);
}
