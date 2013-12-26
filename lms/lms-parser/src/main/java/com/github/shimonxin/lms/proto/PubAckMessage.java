package com.github.shimonxin.lms.proto;

/**
 * Placeholder for PUBACK message.
 * 
 * @author andrea
 */
public class PubAckMessage extends MessageIDMessage {
    
    public PubAckMessage() {
        m_messageType = AbstractMessage.PUBACK;
    }
}
