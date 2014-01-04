package com.github.shimonxin.lms.spi.store;

import java.io.Serializable;
import java.nio.ByteBuffer;

import com.github.shimonxin.lms.proto.QoS;
import com.github.shimonxin.lms.spi.events.PublishEvent;

/**
 * Publish event serialized to the DB.
 * 
 * @author andrea
 */
public class StoredPublishEvent implements Serializable, Comparable<StoredPublishEvent> {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1838151253355044668L;
	String m_topic;
	QoS m_qos;
	byte[] m_message;
	boolean m_retain;
	String m_clientID;
	// Optional attribute, available only fo QoS 1 and 2
	int m_msgID;
	long timestamp;

	public StoredPublishEvent(PublishEvent wrapped) {
		m_topic = wrapped.getTopic();
		m_qos = wrapped.getQos();
		m_retain = wrapped.isRetain();
		m_clientID = wrapped.getClientID();
		m_msgID = wrapped.getMessageID();

		ByteBuffer buffer = wrapped.getMessage();
		m_message = new byte[buffer.remaining()];
		buffer.get(m_message);
		buffer.rewind();
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

	public PublishEvent convertFromStored() {
		ByteBuffer bbmessage = ByteBuffer.wrap(m_message);
		PublishEvent liveEvt = new PublishEvent(m_topic, m_qos, bbmessage, m_retain, m_clientID, m_msgID, null);
		return liveEvt;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public int compareTo(StoredPublishEvent other) {
		if (other == null) {
			return 1;
		}
		if (this == other) {
			return 0;
		}
		if (m_clientID == null) {
			if (other.m_clientID != null) {
				return -1;
			}
			return 0;
		} else if (!m_clientID.equals(other.m_clientID)) {
			return m_clientID.compareTo(other.m_clientID);
		} else {
			return m_msgID - other.m_msgID;
		}
	}
}
