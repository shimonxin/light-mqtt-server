package com.github.shimonxin.lms.spi.events;


import com.github.shimonxin.lms.parser.Utils;
import com.github.shimonxin.lms.proto.AbstractMessage;
import com.github.shimonxin.lms.spi.ServerChannel;



/**
 * Event used to carry ProtocolMessages from front handler to event processor
 */
public class ProtocolEvent extends MessagingEvent {
    ServerChannel m_session;
    AbstractMessage message;

    public ProtocolEvent(ServerChannel session, AbstractMessage message) {
        this.m_session = session;
        this.message = message;
    }

    public ServerChannel getSession() {
        return m_session;
    }

    public AbstractMessage getMessage() {
        return message;
    }
    
    @Override
    public String toString() {
        return "ProtocolEvent wrapping " + Utils.msgType2String(message.getMessageType());
    }
}
