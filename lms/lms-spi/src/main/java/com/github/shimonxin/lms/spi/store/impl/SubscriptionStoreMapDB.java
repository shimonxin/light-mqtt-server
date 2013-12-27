/**
 * SubscriptionStoreMapDB.java created at 2013-12-27 下午2:04:58 by ShimonXin
 */
package com.github.shimonxin.lms.spi.store.impl;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.shimonxin.lms.spi.store.SubscriptionStore;
import com.github.shimonxin.lms.spi.subscriptions.Subscription;
import com.github.shimonxin.lms.spi.subscriptions.Token;
import com.github.shimonxin.lms.spi.subscriptions.TreeNode;

/**
 * SubscriptionStore MapDB implementation
 * 
 * @author ShimonXin
 * @created 2013-12-27
 * 
 */
public class SubscriptionStoreMapDB implements SubscriptionStore {
	private static final Logger LOG = LoggerFactory.getLogger(SubscriptionStoreMapDB.class);
	private String storeFile;
	private DB db;
	private TreeNode subscriptions = new TreeNode(null);
	// persistent Map of clientID, set of Subscriptions
	private NavigableSet<Fun.Tuple2<String, Subscription>> m_persistentSubscriptions;
	
	/**
	 * @see com.github.shimonxin.lms.spi.store.SubscriptionStore#init()
	 */
	@Override
	public void init() {
		db = DBMaker.newFileDB(new File(storeFile)).closeOnJvmShutdown().encryptionEnable("password").make();
		m_persistentSubscriptions = db.getTreeSet("subscriptions");
		// reload any subscriptions persisted
		LOG.debug("Reloading all stored subscriptions...");
		for (Tuple2<String, Subscription> t : m_persistentSubscriptions.descendingSet()) {
			LOG.debug("Re-subscribing " + t.b.getClientId() + " to topic " + t.b.getTopic());
			addDirect(t.b);
		}
		LOG.debug("Finished loading");
	}

	private void addDirect(Subscription newSubscription) {
		TreeNode current = findMatchingNode(newSubscription.getTopic());
		current.addSubscription(newSubscription);
	}

	private TreeNode findMatchingNode(String topic) {
		List<Token> tokens = new ArrayList<Token>();
		try {
			tokens = splitTopic(topic);
		} catch (ParseException ex) {
			// TODO handle the parse exception
			LOG.error(null, ex);
			// return;
		}

		TreeNode current = subscriptions;
		for (Token token : tokens) {
			TreeNode matchingChildren;

			// check if a children with the same token already exists
			if ((matchingChildren = current.childWithToken(token)) != null) {
				current = matchingChildren;
			} else {
				// create a new node for the newly inserted token
				matchingChildren = new TreeNode(current);
				matchingChildren.setToken(token);
				current.addChild(matchingChildren);
				current = matchingChildren;
			}
		}
		return current;
	}

	/**
	 * 
	 * @see com.github.shimonxin.lms.spi.store.SubscriptionStore#stop()
	 */
	@Override
	public void stop() {
		if (db != null)
			db.close();
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.SubscriptionStore#activate(java.lang.String)
	 */
	@Override
	public void activate(String clientID) {
		LOG.debug("activate re-activating subscriptions for clientID " + clientID);
		subscriptions.activate(clientID);
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.SubscriptionStore#searchTopicSubscriptions(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Subscription> searchTopicSubscriptions(String topic) {
		List<Token> tokens = new ArrayList<Token>();
		try {
			tokens = splitTopic(topic);
		} catch (ParseException ex) {
			// TODO handle the parse exception
			LOG.error(null, ex);
			return Collections.EMPTY_LIST;
		}

		Queue<Token> tokenQueue = new LinkedBlockingDeque<Token>(tokens);
		List<Subscription> matchingSubs = new ArrayList<Subscription>();
		subscriptions.matches(tokenQueue, matchingSubs);
		return matchingSubs;
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.SubscriptionStore#removeForClient(java.lang.String)
	 */
	@Override
	public void removeForClient(String clientID) {
		subscriptions.removeClientSubscriptions(clientID);
		// remove from log all subscriptions
		m_persistentSubscriptions.remove(clientID);
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.SubscriptionStore#deactivate(java.lang.String)
	 */
	@Override
	public void deactivate(String clientID) {
		subscriptions.deactivate(clientID);
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.SubscriptionStore#removeSubscription(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public void removeSubscription(String topic, String clientID) {
		TreeNode matchNode = findMatchingNode(topic);
		// search fr the subscription to remove
		Subscription toBeRemoved = null;
		for (Subscription sub : matchNode.subscriptions()) {
			if (sub.getTopic().equals(topic)) {
				toBeRemoved = sub;
				break;
			}
		}
		if (toBeRemoved != null) {
			matchNode.subscriptions().remove(toBeRemoved);
			m_persistentSubscriptions.remove(toBeRemoved);
			db.commit();
		}

	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.SubscriptionStore#add(com.github.shimonxin.lms.spi.subscriptions.Subscription)
	 */
	@Override
	public void add(Subscription newSubscription) {
		addDirect(newSubscription);
		// log the subscription
		m_persistentSubscriptions.add(Fun.t2(newSubscription.getClientId(), newSubscription));
		db.commit();
	}

	/**
	 * Verify if the 2 topics matching respecting the rules of MQTT Appendix A
	 * 
	 * @see com.github.shimonxin.lms.spi.store.SubscriptionStore#matchTopics(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public boolean matchTopics(String msgTopic, String subscriptionTopic) {
		// TODO reimplement with iterators or with queues
		try {
			List<Token> msgTokens = splitTopic(msgTopic);
			List<Token> subscriptionTokens = splitTopic(subscriptionTopic);
			int i = 0;
			Token subToken = null;
			for (; i < subscriptionTokens.size(); i++) {
				subToken = subscriptionTokens.get(i);
				if (subToken != Token.MULTI && subToken != Token.SINGLE) {
					if (i >= msgTokens.size()) {
						return false;
					}
					Token msgToken = msgTokens.get(i);
					if (!msgToken.equals(subToken)) {
						return false;
					}
				} else {
					if (subToken == Token.MULTI) {
						return true;
					}
					if (subToken == Token.SINGLE) {
						// skip a step forward
					}
				}
			}
			// if last token was a SINGLE then treat it as an empty
			if (subToken == Token.SINGLE && (i - msgTokens.size() == 1)) {
				i--;
			}
			return i == msgTokens.size();
		} catch (ParseException ex) {
			LOG.error(null, ex);
			throw new RuntimeException(ex);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected static List<Token> splitTopic(String topic) throws ParseException {
		List res = new ArrayList<Token>();
		String[] splitted = topic.split("/");

		if (splitted.length == 0) {
			res.add(Token.EMPTY);
		}

		for (int i = 0; i < splitted.length; i++) {
			String s = splitted[i];
			if (s.isEmpty()) {
				// if (i != 0) {
				// throw new
				// ParseException("Bad format of topic, expetec topic name between separators",
				// i);
				// }
				res.add(Token.EMPTY);
			} else if (s.equals("#")) {
				// check that multi is the last symbol
				if (i != splitted.length - 1) {
					throw new ParseException("Bad format of topic, the multi symbol (#) has to be the last one after a separator", i);
				}
				res.add(Token.MULTI);
			} else if (s.contains("#")) {
				throw new ParseException("Bad format of topic, invalid subtopic name: " + s, i);
			} else if (s.equals("+")) {
				res.add(Token.SINGLE);
			} else if (s.contains("+")) {
				throw new ParseException("Bad format of topic, invalid subtopic name: " + s, i);
			} else {
				res.add(new Token(s));
			}
		}

		return res;
	}

	public String getStoreFile() {
		return storeFile;
	}

	public void setStoreFile(String storeFile) {
		this.storeFile = storeFile;
	}

}
