package org.github.shimonxin.lms.server;

import java.io.File;
import java.io.IOException;

import org.github.shimonxin.lms.server.netty.NettyAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.shimonxin.lms.spi.Authenticator;
import com.github.shimonxin.lms.spi.messaging.Messaging;
import com.github.shimonxin.lms.spi.messaging.impl.LmaxQueueMessaging;
import com.github.shimonxin.lms.spi.messaging.impl.MqttV3ProtocalProcessor;
import com.github.shimonxin.lms.spi.session.SessionManger;
import com.github.shimonxin.lms.spi.session.impl.SessionManagerMemory;
import com.github.shimonxin.lms.spi.store.impl.InflightMessageStoreMapDB;
import com.github.shimonxin.lms.spi.store.impl.PersistMessageStoreMapDB;
import com.github.shimonxin.lms.spi.store.impl.RetainedMessageStoreMapDB;
import com.github.shimonxin.lms.spi.store.impl.SubscriptionStoreMapDB;

/**
 * Launch a configured version of the server.
 * 
 * @author andrea
 */
public class Server {

	private static final Logger LOG = LoggerFactory.getLogger(Server.class);

	public static final String STORAGE_FILE_PATH = System.getProperty("user.home") + File.separator + "moquette_store.hawtdb";

	private ServerAcceptor m_acceptor;
	private String host = "0.0.0.0";
	private int port = 1883;
	private int defaultTimeout = 10;
	Messaging messaging;

	public static void main(String[] args) throws IOException {

		final Server server = new Server();
		server.startServer();
		// Bind a shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				server.stopServer();
			}
		});

	}

	public void startServer() throws IOException {
		if (messaging == null) {
			InflightMessageStoreMapDB inflightMessageStore = new InflightMessageStoreMapDB();
			inflightMessageStore.setStoreFile("/mqtt_inflight.db");
			PersistMessageStoreMapDB persistMessageStore = new PersistMessageStoreMapDB();
			persistMessageStore.setStoreFile("/mqtt_persist.db");
			RetainedMessageStoreMapDB retainedMessageStore = new RetainedMessageStoreMapDB();
			retainedMessageStore.setStoreFile("/mqtt_retained.db");
			SubscriptionStoreMapDB subscriptionStore = new SubscriptionStoreMapDB();
			subscriptionStore.setStoreFile("/mqtt_subscription.db");
			Authenticator authenticator = new Authenticator() {
				@Override
				public boolean auth(String clientId, String username, String password) {
					// allways return true;
					return true;
				}

			};
			SessionManger sessionManger = new SessionManagerMemory();
			MqttV3ProtocalProcessor processor = new MqttV3ProtocalProcessor();
			processor.setAuthenticator(authenticator);
			processor.setForceLogin(true);
			processor.setInflightMessageStore(inflightMessageStore);
			processor.setRetainedMessageStore(retainedMessageStore);
			processor.setPersistMessageStore(persistMessageStore);
			processor.setSessionManger(sessionManger);
			processor.setSubscriptionStore(subscriptionStore);
			messaging = new LmaxQueueMessaging();
			messaging.setProtocolProcessor(processor);
		}
		messaging.init();

		m_acceptor = new NettyAcceptor();
		m_acceptor.initialize(messaging, host, port, defaultTimeout);
	}

	public void stopServer() {
		System.out.println("Server stopping...");
		messaging.stop();
		m_acceptor.close();
		System.out.println("Server stopped");
		try {
			// sleep one second to give the disruptor
			Thread.sleep(1000);
		} catch (InterruptedException ex) {
			LOG.error(null, ex);
		}
	}
}
