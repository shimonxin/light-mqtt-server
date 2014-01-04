package com.github.shimonxin.lms.spi.events;

import com.github.shimonxin.lms.spi.session.ServerChannel;

/**
 * Used to model the connection lost event
 * 
 * @author andrea
 */
public class LostConnectionEvent extends MessagingEvent{
    private ServerChannel m_session;

    public LostConnectionEvent(ServerChannel session) {
    	m_session = session;
    }

    public ServerChannel getSession() {
        return m_session;
    }
    
}
