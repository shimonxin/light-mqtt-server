/**
 * InflightMessageStoreMapDB.java created at 2013-12-27 下午2:05:38 by ShimonXin
 */
package com.github.shimonxin.lms.spi.store.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentNavigableMap;

import org.mapdb.BTreeKeySerializer;
import org.mapdb.Bind;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.shimonxin.lms.spi.events.PublishEvent;
import com.github.shimonxin.lms.spi.store.InflightMessageStore;
import com.github.shimonxin.lms.spi.store.StoredPublishEvent;

/**
 * InflightMessageStore MapDB
 * 
 * @author ShimonXin
 * @created 2013-12-27
 * 
 */
public class InflightMessageStoreMapDB implements InflightMessageStore {
	private static final Logger LOG = LoggerFactory.getLogger(InflightMessageStoreMapDB.class);
	private String storeFile;
	private DB db;
	private ConcurrentNavigableMap<String, StoredPublishEvent> m_inflightInboundStore;
	private NavigableSet<Fun.Tuple2<String, StoredPublishEvent>> m_inflightOutboundStore;

	/**
	 * @see com.github.shimonxin.lms.spi.store.InflightMessageStore#init()
	 */
	@Override
	public void init() {
		db = DBMaker.newFileDB(new File(storeFile)).closeOnJvmShutdown().encryptionEnable("password").make();
		m_inflightInboundStore = db.getTreeMap("inflightInbound");
		m_inflightOutboundStore = db.createTreeSet("inflightOutbound").serializer(BTreeKeySerializer.TUPLE2).makeOrGet();
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.InflightMessageStore#stop()
	 */
	@Override
	public void stop() {
		if (db != null) {
			db.close();
		}
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.InflightMessageStore#addInFlightInbound(com.github.shimonxin.lms.spi.events.PublishEvent)
	 */
	@Override
	public void addInFlightInbound(PublishEvent evt) {
		m_inflightInboundStore.put(String.format("%s%d", evt.getClientID(), evt.getMessageID()), new StoredPublishEvent(evt));
		db.commit();
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.InflightMessageStore#retriveInFlightInbound(java.lang.String)
	 */
	@Override
	public PublishEvent retriveInFlightInbound(String publishKey) {
		StoredPublishEvent se = m_inflightInboundStore.get(publishKey);
		if (se == null) {
			return null;
		}
		return se.convertFromStored();
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.InflightMessageStore#cleanInFlightInbound(java.lang.String)
	 */
	@Override
	public void cleanInFlightInbound(String publishKey) {
		m_inflightInboundStore.remove(publishKey);
		db.commit();
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.InflightMessageStore#addInFlightOutbound(com.github.shimonxin.lms.spi.events.PublishEvent)
	 */
	@Override
	public void addInFlightOutbound(PublishEvent evt) {
		StoredPublishEvent se = new StoredPublishEvent(evt);
		se.setTimestamp(System.currentTimeMillis());
		m_inflightOutboundStore.add(Fun.t2(evt.getClientID(), se));
		db.commit();
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.InflightMessageStore#cleanInFlightOutbound(java.lang.String)
	 */
	@Override
	public void cleanInFlightOutbound(String clientID, int messageID) {
		Tuple2<String, StoredPublishEvent> tobeRemoved = null;
		for (StoredPublishEvent evt : Bind.findVals2(m_inflightOutboundStore, clientID)) {
			if (evt.getMessageID() == messageID) {
				tobeRemoved = Fun.t2(clientID, evt);
			}
		}
		if (tobeRemoved != null) {
			m_inflightOutboundStore.remove(tobeRemoved);
			db.commit();
		}
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.InflightMessageStore#retriveDelayedPublishes(java.lang.String, int)
	 */
	@Override
	public List<PublishEvent> retriveDelayedPublishes(String clientID, int keepAlive) {
		LOG.debug(String.format("retriveDelayedPublishes client[%s] keep alive[%ds]", clientID, keepAlive));
		List<PublishEvent> publishs = new ArrayList<PublishEvent>();
		long now = System.currentTimeMillis();
		for (StoredPublishEvent evt : Bind.findVals2(m_inflightOutboundStore, clientID)) {
			if (keepAlive == 0 || (now - evt.getTimestamp()) > (keepAlive * 1000)) {
				publishs.add(evt.convertFromStored());
			}
		}
		return publishs;
	}

	/**
	 * @see com.github.shimonxin.lms.spi.store.InflightMessageStore#retriveOutboundPublishes(java.lang.String)
	 */
	@Override
	public List<PublishEvent> retriveOutboundPublishes(String clientID) {
		LOG.debug(String.format("retriveOutboundPublishes client[%s] ", clientID));
		List<PublishEvent> publishs = new ArrayList<PublishEvent>();
		for (StoredPublishEvent evt : Bind.findVals2(m_inflightOutboundStore, clientID)) {
			publishs.add(evt.convertFromStored());
		}
		return publishs;
	}
	public void cleanOutboundPublishes(String clientID) {
		LOG.debug(String.format("retriveOutboundPublishes client[%s] ", clientID));
		List<Fun.Tuple2<String, StoredPublishEvent>> publishs = new ArrayList<Fun.Tuple2<String, StoredPublishEvent>>();
		for (StoredPublishEvent evt : Bind.findVals2(m_inflightOutboundStore, clientID)) {
			publishs.add(Fun.t2(clientID, evt));
		}
		m_inflightOutboundStore.removeAll(publishs);
		db.commit();
	}
	/**
	 * @see com.github.shimonxin.lms.spi.store.InflightMessageStore#retriveDelayedPublishes()
	 */
	@Override
	public List<PublishEvent> retriveDelayedPublishes() {
		LOG.debug("retriveAllOutboundPublishes ");
		List<PublishEvent> publishs = new ArrayList<PublishEvent>();
		for (Tuple2<String, StoredPublishEvent> t : m_inflightOutboundStore) {
			publishs.add(t.b.convertFromStored());
		}
		return publishs;
	}

	public void setStoreFile(String storeFile) {
		this.storeFile = storeFile;
	}

	@Override
	public List<PublishEvent> retriveDelayedPublishes(int timeout) {
		LOG.debug(String.format("retriveDelayedPublishes time out [%ds]",  timeout));
		List<PublishEvent> publishs = new ArrayList<PublishEvent>();
		long now = System.currentTimeMillis();
		
		for (Fun.Tuple2<String, StoredPublishEvent> t : m_inflightOutboundStore.descendingSet()) {
			if (timeout == 0 || (now - t.b.getTimestamp()) > (timeout * 1000)) {
				publishs.add(t.b.convertFromStored());
			}
		}
		return publishs;
	}

}
