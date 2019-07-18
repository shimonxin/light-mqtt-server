A lightweight MQTT server
=========================

A lightweight MQTT server that will keep running for the duration of your Android application. 

Use LMAX for message queue.

Use MapDB to store in flight messages.

Use Netty for IO (based on moquette-mqtt writen by andrea).

Feature
----
* QoS 0,1,2 supported
* Custom authentication
* Custom subscription store
* Custom off line message store
* Custom in flight store
* Broker bridge

Simple Example
----
```java
final Server server = new Server();
server.startServer();
// Bind a shutdown hook
Runtime.getRuntime().addShutdownHook(new Thread() {
	@Override
	public void run() {
		server.stopServer();
	}
});
```

MQTT Server started at 0.0.0.0:1883

Custom Example
----
```java
// custom Inflight Message Store
InflightMessageStoreMapDB inflightMessageStore = new InflightMessageStoreMapDB();
inflightMessageStore.setStoreFile("/mqtt_inflight.db");

// custom Persist Message Store
PersistMessageStoreMapDB persistMessageStore = new PersistMessageStoreMapDB();
persistMessageStore.setStoreFile("/mqtt_persist.db");

// custom your Persist Message Store
RetainedMessageStoreMapDB retainedMessageStore = new RetainedMessageStoreMapDB();
retainedMessageStore.setStoreFile("/mqtt_retained.db");

// custom your Subscription Store
SubscriptionStoreMapDB subscriptionStore = new SubscriptionStoreMapDB();
subscriptionStore.setStoreFile("/mqtt_subscription.db");

// custom your Authenticator
Authenticator authenticator = new Authenticator() {
	@Override
	public boolean auth(String clientId, String username, String password) {
		// allways return true;
		return true;
	}

};

// custom your Session Manger
SessionManger sessionManger = new SessionManagerMemory();

// custom your Protocal Processor
MqttV3ProtocalProcessor processor = new MqttV3ProtocalProcessor();
processor.setAuthenticator(authenticator);
// force user login?
processor.setForceLogin(false);
processor.setInflightMessageStore(inflightMessageStore);
processor.setRetainedMessageStore(retainedMessageStore);
processor.setPersistMessageStore(persistMessageStore);
processor.setSessionManger(sessionManger);
processor.setSubscriptionStore(subscriptionStore);

// custom Messaging
LmaxQueueMessaging messaging = new LmaxQueueMessaging();
messaging.setProtocolProcessor(processor);	

// custom Server Acceptor
ServerAcceptor acceptor = new NettyAcceptor();	
final Server server = new Server();
server.setAcceptor(acceptor);
server.setMessaging(messaging);
// server configuration
server.setHost("example.org");
server.setPort(1883);
server.setDefaultTimeout(10);
// start
server.startServer();
// Bind a shutdown hook
Runtime.getRuntime().addShutdownHook(new Thread() {
	@Override
	public void run() {
		server.stopServer();
	}
});
```

License
-------

    Copyright 2013,2019 Shimon Xin
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
