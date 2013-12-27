package com.github.shimonxin.lms.spi.events;

import com.github.shimonxin.lms.spi.session.ServerChannel;


/**
 *
 * @author andrea
 */
public class DisconnectEvent extends MessagingEvent {
    
    ServerChannel m_session;
    
    public DisconnectEvent(ServerChannel session) {
        m_session = session;
    }

    public ServerChannel getSession() {
        return m_session;
    }
    
    
}
