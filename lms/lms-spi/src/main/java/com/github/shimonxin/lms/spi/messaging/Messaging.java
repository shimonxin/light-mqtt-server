package com.github.shimonxin.lms.spi.messaging;

import com.github.shimonxin.lms.proto.AbstractMessage;
import com.github.shimonxin.lms.spi.session.ServerChannel;

/**
 * 
 * messaging service
 * 
 * @author ShimonXin
 * @created 2013-12-26
 * 
 */
public interface Messaging {
	/**
	 * @param processer
	 */
	void setProtocolProcessor(ProtocolProcessor processer);

	/**
	 * init
	 */
	void init();

	/**
	 * 
	 * handle protocol message
	 * 
	 * @param session
	 * @param msg
	 */
	void handleProtocolMessage(ServerChannel session, AbstractMessage msg);

	/**
	 * 
	 * disconnect
	 * 
	 * @param session
	 */
	void disconnect(ServerChannel session);

	/**
	 * lost connection
	 * 
	 * @param clientID
	 */
	void lostConnection(String clientID);

	/**
	 * stop
	 */
	void stop();
}
