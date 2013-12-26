package com.github.shimonxin.lms.proto;

/**
 * Basic abstract message for all MQTT protocol messages.
 * 
 * @author andrea
 */
public abstract class AbstractMessage {

    public static final byte CONNECT = 1; // Client request to connect to Server
    public static final byte CONNACK = 2; // Connect Acknowledgment
    public static final byte PUBLISH = 3; // Publish message
    public static final byte PUBACK = 4; // Publish Acknowledgment
    public static final byte PUBREC = 5; //Publish Received (assured delivery part 1)
    public static final byte PUBREL = 6; // Publish Release (assured delivery part 2)
    public static final byte PUBCOMP = 7; //Publish Complete (assured delivery part 3)
    public static final byte SUBSCRIBE = 8; //Client Subscribe request
    public static final byte SUBACK = 9; // Subscribe Acknowledgment
    public static final byte UNSUBSCRIBE = 10; //Client Unsubscribe request
    public static final byte UNSUBACK = 11; // Unsubscribe Acknowledgment
    public static final byte PINGREQ = 12; //PING Request
    public static final byte PINGRESP = 13; //PING Response
    public static final byte DISCONNECT = 14; //Client is Disconnecting

    //type
    protected boolean m_dupFlag;
    protected QoS m_qos;
    protected boolean m_retainFlag;
    protected int m_remainingLength;
    protected byte m_messageType;

    public byte getMessageType() {
        return m_messageType;
    }

    public void setMessageType(byte messageType) {
        this.m_messageType = messageType;
    }

    public boolean isDupFlag() {
        return m_dupFlag;
    }

    public void setDupFlag(boolean dupFlag) {
        this.m_dupFlag = dupFlag;
    }

    public QoS getQos() {
        return m_qos;
    }

    public void setQos(QoS qos) {
        this.m_qos = qos;
    }

    public boolean isRetainFlag() {
        return m_retainFlag;
    }

    public void setRetainFlag(boolean retainFlag) {
        this.m_retainFlag = retainFlag;
    }

    /**
     * TOBE used only internally
     */
    public int getRemainingLength() {
        return m_remainingLength;
    }
    
    /**
     * TOBE used only internally
     */
    public void setRemainingLength(int remainingLength) {
        this.m_remainingLength = remainingLength;
    }
}
