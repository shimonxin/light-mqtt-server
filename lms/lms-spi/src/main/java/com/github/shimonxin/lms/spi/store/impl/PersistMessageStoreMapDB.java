/**
 * PersistMessageStoreMapDB.java created at 2013-12-27 下午2:05:56 by ShimonXin
 */
package com.github.shimonxin.lms.spi.store.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

import org.mapdb.Bind;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.shimonxin.lms.spi.events.PublishEvent;
import com.github.shimonxin.lms.spi.store.PersistMessageStore;

/**
 * PersistMessageStore MapDB
 * 
 * @author ShimonXin
 * @created 2013-12-27
 * 
 */
public class PersistMessageStoreMapDB implements PersistMessageStore {
	private static final Logger LOG = LoggerFactory.getLogger(PersistMessageStoreMapDB.class);
	private String storeFile;
	private DB db;
	private NavigableSet<Fun.Tuple2<String, PublishEvent>> m_persistentMessageStore;

	@Override
	public void init() {
		db = DBMaker.newFileDB(new File(storeFile)).closeOnJvmShutdown().encryptionEnable("password").make();
		m_persistentMessageStore = db.getTreeSet("persistedMessages");
	}

	@Override
	public void stop() {
		if (db != null) {
			db.close();
		}
	}

	@Override
	public void persistedPublishForFuture(PublishEvent newPublishEvt) {
		m_persistentMessageStore.add(Fun.t2(newPublishEvt.getClientID(), newPublishEvt));
	}

	@Override
	public List<PublishEvent> retrivePersistedPublishes(String clientId) {
		List<PublishEvent> publishs = new ArrayList<PublishEvent>();
		for (PublishEvent evt : Bind.findVals2(m_persistentMessageStore, clientId)) {
			publishs.add(evt);
		}
		return publishs;
	}

	@Override
	public void cleanPersistedPublishes(String clientID) {
		Set<Tuple2<String, PublishEvent>> publishs = new HashSet<Tuple2<String, PublishEvent>>();
		for (PublishEvent evt : Bind.findVals2(m_persistentMessageStore, clientID)) {
			publishs.add(Fun.t2(clientID, evt));
		}
		m_persistentMessageStore.removeAll(publishs);
		db.commit();
	}

	@Override
	public void removePersistedPublish(String clientID, int messageID) {
		PublishEvent eventToRemove = null;
		for (PublishEvent evt : Bind.findVals2(m_persistentMessageStore, clientID)) {
			if (evt.getMessageID() == messageID) {
				eventToRemove = evt;
				break;
			}
		}
		if (eventToRemove != null) {
			m_persistentMessageStore.remove(Fun.t2(clientID, eventToRemove));
		}
	}

	@Override
	public void persistedPublishsForFuture(List<PublishEvent> newPublishEvts) {
		LOG.debug(String.format("persistedPublishsForFuture invoke, size:%d", newPublishEvts.size()));
		for (PublishEvent evt : newPublishEvts) {
			persistedPublishForFuture(evt);
		}
	}

	public void setStoreFile(String storeFile) {
		this.storeFile = storeFile;
	}

}
