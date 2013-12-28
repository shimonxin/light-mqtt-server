/**
 * SessionManagerMemory.java created at 2013-12-28 下午2:17:33 by ShimonXin
 */
package com.github.shimonxin.lms.spi.session.impl;

import java.util.HashMap;
import java.util.Map;

import com.github.shimonxin.lms.spi.session.SessionDescriptor;
import com.github.shimonxin.lms.spi.session.SessionManger;

/**
 * SessionManager Memory
 * 
 * @author ShimonXin
 * @created 2013-12-28
 * 
 */
public class SessionManagerMemory implements SessionManger {
	private Map<String, SessionDescriptor> m_clientIDs = new HashMap<String, SessionDescriptor>();

	/**
	 * @see com.github.shimonxin.lms.spi.session.SessionManger#containsClient(java.lang.String)
	 */
	@Override
	public boolean containsClient(String clientID) {
		return m_clientIDs.containsKey(clientID);
	}

	/**
	 * @see com.github.shimonxin.lms.spi.session.SessionManger#get(java.lang.String)
	 */
	@Override
	public SessionDescriptor get(String clientID) {
		return m_clientIDs.get(clientID);
	}

	/**
	 * @see com.github.shimonxin.lms.spi.session.SessionManger#put(java.lang.String, com.github.shimonxin.lms.spi.session.SessionDescriptor)
	 */
	@Override
	public void put(String clientID, SessionDescriptor sessionDescr) {
		m_clientIDs.put(clientID, sessionDescr);
	}

	/**
	 * @see com.github.shimonxin.lms.spi.session.SessionManger#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return m_clientIDs.isEmpty();
	}

	/**
	 * @see com.github.shimonxin.lms.spi.session.SessionManger#remove(java.lang.String)
	 */
	@Override
	public SessionDescriptor remove(String clientID) {
		SessionDescriptor exist = get(clientID);
		if (exist != null)
			m_clientIDs.remove(clientID);
		return exist;
	}

}
