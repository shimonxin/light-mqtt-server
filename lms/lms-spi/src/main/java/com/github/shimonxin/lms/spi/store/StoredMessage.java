package com.github.shimonxin.lms.spi.store;

import java.io.Serializable;

import com.github.shimonxin.lms.proto.QoS;

public class StoredMessage implements Serializable {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 4667605603518529945L;
	QoS m_qos;
	byte[] m_payload;
	String m_topic;

	public StoredMessage(byte[] message, QoS qos, String topic) {
		m_qos = qos;
		m_payload = message;
		m_topic = topic;
	}

	public QoS getQos() {
		return m_qos;
	}

	public void setQos(QoS qos) {
		m_qos = qos;
	}

	public byte[] getPayload() {		
		return m_payload;
	}

	public String getTopic() {
		return m_topic;
	}

	public void setTopic(String topic) {
		m_topic = topic;
	}

}
