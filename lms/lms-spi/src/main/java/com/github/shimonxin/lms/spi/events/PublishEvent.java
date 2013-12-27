package com.github.shimonxin.lms.spi.events;

import java.io.Serializable;

import com.github.shimonxin.lms.proto.QoS;
import com.github.shimonxin.lms.spi.session.ServerChannel;



/**
 *
 * @author andrea
 */
public class PublishEvent extends MessagingEvent implements Serializable {
    /**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -2813326819401898446L;
	String m_topic;
    QoS m_qos;
    byte[] m_message;
    boolean m_retain;
    String m_clientID;
    //Optional attribute, available only fo QoS 1 and 2
    int m_msgID;

    transient ServerChannel m_session;
    
    long timestamp;
    
    public PublishEvent(String topic, QoS qos, byte[] message, boolean retain,
            String clientID, ServerChannel session) {
        m_topic = topic;
        m_qos = qos;
        m_message = message;
        m_retain = retain;
        m_clientID = clientID;
        m_session = session;
    }

    public PublishEvent(String topic, QoS qos, byte[] message, boolean retain,
                        String clientID, int msgID, ServerChannel session) {
        this(topic, qos, message, retain, clientID, session);
        m_msgID = msgID;
    }
    
    public String getTopic() {
        return m_topic;
    }

    public QoS getQos() {
        return m_qos;
    }

    public byte[] getMessage() {
        return m_message;
    }

    public boolean isRetain() {
        return m_retain;
    }
    
    public String getClientID() {
        return m_clientID;
    }

    public int getMessageID() {
        return m_msgID;
    }

    public ServerChannel getSession() {
        return m_session;
    }

    @Override
    public String toString() {
        return "PublishEvent{" +
                "m_msgID=" + m_msgID +
                ", m_clientID='" + m_clientID + '\'' +
                ", m_retain=" + m_retain +
                ", m_qos=" + m_qos +
                ", m_topic='" + m_topic + '\'' +
                '}';
    }

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_clientID == null) ? 0 : m_clientID.hashCode());
		result = prime * result + m_msgID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof PublishEvent)) {
			return false;
		}
		PublishEvent other = (PublishEvent) obj;
		if (m_clientID == null) {
			if (other.m_clientID != null) {
				return false;
			}
		} else if (!m_clientID.equals(other.m_clientID)) {
			return false;
		}
		if (m_msgID != other.m_msgID) {
			return false;
		}
		return true;
	}
}
