package com.github.shimonxin.lms.proto;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author andrea
 */
public class SubAckMessage extends MessageIDMessage {

    List<QoS> m_types = new ArrayList<QoS>();
    
    public SubAckMessage() {
        m_messageType = AbstractMessage.SUBACK;
    }

    public List<QoS> types() {
        return m_types;
    }

    public void addType(QoS type) {
        m_types.add(type);
    }
}
