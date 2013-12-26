package com.github.shimonxin.lms.proto;

/**
 *
 * @author andrea
 */
public class UnsubAckMessage extends MessageIDMessage {
    
    public UnsubAckMessage() {
        m_messageType = AbstractMessage.UNSUBACK;
    }
}

