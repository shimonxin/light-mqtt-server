/**
 * SessionDescriptor.java created at 2013-12-26 下午3:37:59 by ShimonXin
 */
package com.github.shimonxin.lms.spi.session;

/**
 * session sescriptor
 * 
 * @author ShimonXin
 * @created 2013-12-26
 * 
 */
public class SessionDescriptor {
	private String m_clientID;
	private ServerChannel m_session;
	private boolean m_cleanSession;
	private String m_username;
	private int m_keepAlive;

	public SessionDescriptor(String username, String clientID, ServerChannel session, boolean cleanSession, int keepAlive) {
		this.m_username = username;
		this.m_clientID = clientID;
		this.m_session = session;
		this.m_cleanSession = cleanSession;
		this.m_keepAlive = keepAlive;
	}

	public boolean isCleanSession() {
		return m_cleanSession;
	}

	public String getClientID() {
		return m_clientID;
	}

	public ServerChannel getSession() {
		return m_session;
	}

	public String getUserID() {
		return m_username;
	}

	public int getKeepAlive() {
		return m_keepAlive;
	}
}
