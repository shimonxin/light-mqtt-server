package com.github.shimonxin.lms.proto;

import java.nio.ByteBuffer;

/**
 *
 * @author andrea
 */
public class PublishMessage extends MessageIDMessage {

    private String m_topicName;
    private ByteBuffer m_payload;
    
    public PublishMessage() {
        m_messageType = AbstractMessage.PUBLISH;
    }

    public String getTopicName() {
        return m_topicName;
    }

    public void setTopicName(String topicName) {
        this.m_topicName = topicName;
    }

    public ByteBuffer getPayload() {
        return m_payload;
    }

    public void setPayload(ByteBuffer payload) {
        this.m_payload = payload;
    }
}
