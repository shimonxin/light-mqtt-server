package com.github.shimonxin.lms.spi.messaging;

import com.github.shimonxin.lms.proto.AbstractMessage;
import com.github.shimonxin.lms.spi.ServerChannel;

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
	 * 
	 * disconnect
	 * @param session
	 */
	void disconnect(ServerChannel session);
	/**
	 * 
	 * handle protocol message
	 * @param session
	 * @param msg
	 */
	void handleProtocolMessage(ServerChannel session, AbstractMessage msg);
	/**
	 * 
	 * init
	 */
	void init();
	
	/**
	 * stop
	 */
	void stop();
}
