/**
 * RetainedMessageStoreMapDB.java created at 2013-12-27 下午2:06:44 by ShimonXin
 */
package com.github.shimonxin.lms.spi.store.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.shimonxin.lms.spi.events.PublishEvent;
import com.github.shimonxin.lms.spi.store.RetainedMessageStore;
import com.github.shimonxin.lms.spi.store.StoredMessage;
import com.github.shimonxin.lms.spi.subscriptions.MatchingCondition;

/**
 * RetainedMessageStore MapDB
 * 
 * @author ShimonXin
 * @created 2013-12-27
 * 
 */
public class RetainedMessageStoreMapDB implements RetainedMessageStore {
	private static final Logger LOG = LoggerFactory.getLogger(RetainedMessageStoreMapDB.class);
	private String storeFile;
	private DB db;
	// retained messages
	private ConcurrentNavigableMap<String, StoredMessage> m_retainedStore;

	/**
	 * @see com.github.shimonxin.lms.spi.store.RetainedMessageStore#init()
	 */
	@Override
	public void init() {
		db = DBMaker.newFileDB(new File(storeFile)).closeOnJvmShutdown().encryptionEnable("password").make();
		m_retainedStore = db.getTreeMap("retained");
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.RetainedMessageStore#stop()
	 */
	@Override
	public void stop() {
		if (db != null) {
			db.close();
		}
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.RetainedMessageStore#storeRetained(com.github.shimonxin.lms.spi.events.PublishEvent)
	 */
	@Override
	public void storeRetained(PublishEvent evt) {
		if (evt.getMessage().remaining() == 0) {
			// clean the message from topic
			m_retainedStore.remove(evt.getTopic());
		} else {
			// store the message to the topic
			m_retainedStore.put(evt.getTopic(), new StoredMessage(evt.getMessage(), evt.getQos(), evt.getTopic()));
		}
	}

	/**
	 * 
	 * @see com.github.shimonxin.lms.spi.store.RetainedMessageStore#searchMatching(com.github.shimonxin.lms.spi.subscriptions.MatchingCondition)
	 */
	@Override
	public Collection<StoredMessage> searchMatching(MatchingCondition condition) {
		LOG.debug("searchMatching scanning all retained messages, presents are " + m_retainedStore.size());
		List<StoredMessage> results = new ArrayList<StoredMessage>();
		for (String key : m_retainedStore.keySet()) {
			StoredMessage storedMsg = m_retainedStore.get(key);
			if (condition.match(key)) {
				results.add(storedMsg);
			}
		}
		return results;
	}

	public void setStoreFile(String storeFile) {
		this.storeFile = storeFile;
	}

}
