package com.github.shimonxin.lms.spi.store;

import java.io.Serializable;
import java.nio.ByteBuffer;

import com.github.shimonxin.lms.proto.QoS;

public class StoredMessage implements Serializable {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 4667605603518529945L;
	QoS m_qos;
	transient ByteBuffer m_payload;
	byte[] paload_bytes;
	String m_topic;

	public StoredMessage(ByteBuffer message, QoS qos, String topic) {
		m_qos = qos;
		m_payload = message;
		paload_bytes = message.array();
		m_topic = topic;
	}

	public QoS getQos() {
		return m_qos;
	}

	public void setQos(QoS qos) {
		m_qos = qos;
	}

	public ByteBuffer getPayload() {
		if (m_payload == null && paload_bytes != null) {
			m_payload = ByteBuffer.wrap(paload_bytes);
		}
		return m_payload;
	}

	public byte[] getPayloadBytes() {
		if (paload_bytes == null && m_payload != null) {
			paload_bytes = m_payload.array();
		}
		return paload_bytes;
	}

	public void setPayloadBytes(byte[] paload_bytes) {
		this.paload_bytes = paload_bytes;
		m_payload = ByteBuffer.wrap(paload_bytes);

	}

	public String getTopic() {
		return m_topic;
	}

	public void setTopic(String topic) {
		m_topic = topic;
	}

}
