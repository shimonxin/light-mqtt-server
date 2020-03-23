package org.github.shimonxin.lms.server.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.util.HashMap;
import java.util.Map;

import com.github.shimonxin.lms.spi.messaging.ProtocolProcessor;
import com.github.shimonxin.lms.spi.session.ServerChannel;
import com.github.shimonxin.lms.spi.session.SessionConstants;

/**
 * 
 * @author andrea
 */
public class NettyChannel implements ServerChannel {

	private ChannelHandlerContext m_channel;

	private Map<Object, AttributeKey<Object>> m_attributesKeys = new HashMap<Object, AttributeKey<Object>>();

	private static final AttributeKey<Object> ATTR_KEY_KEEPALIVE = AttributeKey.valueOf(SessionConstants.KEEP_ALIVE);
	private static final AttributeKey<Object> ATTR_KEY_CLEANSESSION = AttributeKey.valueOf(SessionConstants.CLEAN_SESSION);
	private static final AttributeKey<Object> ATTR_KEY_CLIENTID = AttributeKey.valueOf(SessionConstants.ATTR_CLIENTID);

	private long connectedTimestamp;

	NettyChannel(ChannelHandlerContext ctx) {
		m_channel = ctx;
		m_attributesKeys.put(SessionConstants.KEEP_ALIVE, ATTR_KEY_KEEPALIVE);
		m_attributesKeys.put(SessionConstants.CLEAN_SESSION, ATTR_KEY_CLEANSESSION);
		m_attributesKeys.put(SessionConstants.ATTR_CLIENTID, ATTR_KEY_CLIENTID);
		connectedTimestamp = System.currentTimeMillis();
	}

	public Object getAttribute(Object key) {
		Attribute<Object> attr = m_channel.attr(mapKey(key));
		return attr.get();
	}

	public void setAttribute(Object key, Object value) {
		Attribute<Object> attr = m_channel.attr(mapKey(key));
		attr.set(value);
	}

	private synchronized AttributeKey<Object> mapKey(Object key) {
		if (!m_attributesKeys.containsKey(key)) {
			throw new IllegalArgumentException("mapKey can't find a matching AttributeKey for " + key);
		}
		return m_attributesKeys.get(key);
	}

	public void setIdleTime(int idleTime,ProtocolProcessor processor) {
		if (m_channel.pipeline().names().contains("idleStateHandler")) {
			m_channel.pipeline().remove("idleStateHandler");
		}
		if (m_channel.pipeline().names().contains("idleEventHandler")) {
			m_channel.pipeline().remove("idleEventHandler");
		}
		m_channel.pipeline().addFirst("idleStateHandler", new IdleStateHandler(0, 0, idleTime));
		m_channel.pipeline().addAfter("idleStateHandler", "idleEventHandler", new MqttIdleTimoutHandler(processor,this));
	}

	public void close(boolean immediately) {
		m_channel.close();
	}

	public void write(Object value) {
		m_channel.writeAndFlush(value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_channel == null) ? 0 : m_channel.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof NettyChannel)) {
			return false;
		}
		NettyChannel other = (NettyChannel) obj;
		if (m_channel == null) {
			if (other.m_channel != null) {
				return false;
			}
		} else if (!m_channel.equals(other.m_channel)) {
			return false;
		}
		return true;
	}

	public long getConnectedTimestamp() {
		return connectedTimestamp;
	}

	public void setConnectedTimestamp(long connectedTimestamp) {
		this.connectedTimestamp = connectedTimestamp;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NettyChannel [");
		if (m_channel != null)
			builder.append("m_channel=").append(m_channel).append(", ");
		builder.append("connectedTimestamp=").append(connectedTimestamp).append("]");
		return builder.toString();
	}

}
