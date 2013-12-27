/**
 * SessionManger.java created at 2013-12-26 下午3:30:05 by ShimonXin
 */
package com.github.shimonxin.lms.spi.session;

/**
 * Session Manger
 * 
 * @author ShimonXin
 * @created 2013-12-26
 * 
 */
public interface SessionManger {
	/**
	 * 
	 * contains client
	 * 
	 * @param clientID
	 * @return
	 */
	boolean containsClient(String clientID);

	/**
	 * 
	 * get session descriptor
	 * 
	 * @param clientID
	 * @return
	 */
	SessionDescriptor get(String clientID);

	/**
	 * 
	 * put session descriptor
	 * 
	 * @param clientID
	 * @param sessionDescr
	 */
	void put(String clientID, SessionDescriptor sessionDescr);

	/**
	 * 
	 * no sessions
	 * 
	 * @return
	 */
	boolean isEmpty();

	/**
	 * 
	 * remove client
	 * 
	 * @param clientID
	 */
	void remove(String clientID);
}
