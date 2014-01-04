package com.github.shimonxin.lms.spi.events;

/**
 * Used to re-fire stored QoS1 QoS2 events once a client reconnects.
 */
public class RepublishEvent extends MessagingEvent {
    private String m_clientID;

    public RepublishEvent(String clientID) {
        this.m_clientID = clientID;
    }

    public String getClientID() {
        return m_clientID;
    }
}
