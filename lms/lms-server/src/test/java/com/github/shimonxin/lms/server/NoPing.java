package com.github.shimonxin.lms.server;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

public class NoPing {

	/** 
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		MQTT mqtt=new MQTT();
		mqtt.setHost("localhost", 1883);
		mqtt.setKeepAlive((short)0);
		mqtt.setUserName("test");
		mqtt.setPassword("test");
		mqtt.setCleanSession(false);
		mqtt.setClientId("comsumer");
		BlockingConnection con=mqtt.blockingConnection();
		con.connect();
		con.subscribe(new Topic[]{new Topic("noping",QoS.AT_LEAST_ONCE)});
		con.receive();
	}

}
