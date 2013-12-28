package com.github.shimonxin.lms.spi.events;

import com.github.shimonxin.lms.proto.AbstractMessage;
import com.github.shimonxin.lms.spi.session.ServerChannel;


/**
 *
 * @author andrea
 */
public class OutputMessagingEvent extends MessagingEvent {
    private ServerChannel m_channel;
    private AbstractMessage m_message;

    public OutputMessagingEvent(ServerChannel channel, AbstractMessage message) {
        m_channel = channel;
        m_message = message;
    }

    public ServerChannel getChannel() {
        return m_channel;
    }

    public AbstractMessage getMessage() {
        return m_message;
    }
    
    
}
