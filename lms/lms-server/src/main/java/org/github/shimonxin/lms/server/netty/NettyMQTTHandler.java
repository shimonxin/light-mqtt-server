package org.github.shimonxin.lms.server.netty;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.shimonxin.lms.proto.AbstractMessage;
import com.github.shimonxin.lms.proto.PingRespMessage;

import static com.github.shimonxin.lms.proto.AbstractMessage.*;
import com.github.shimonxin.lms.proto.Utils;
import com.github.shimonxin.lms.spi.messaging.Messaging;
import com.github.shimonxin.lms.spi.session.SessionConstants;

/**
 *
 * @author andrea
 */
@Sharable
public class NettyMQTTHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger LOG = LoggerFactory.getLogger(NettyMQTTHandler.class);
    private Messaging m_messaging;
    private final Map<ChannelHandlerContext, NettyChannel> m_channelMapper = new HashMap<ChannelHandlerContext, NettyChannel>();
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        AbstractMessage msg = (AbstractMessage) message;
        LOG.info("Received a message of type {}", Utils.msgType2String(msg.getMessageType()));
        try {
            switch (msg.getMessageType()) {
                case CONNECT:
                case SUBSCRIBE:
                case UNSUBSCRIBE:
                case PUBLISH:
                case PUBREC:
                case PUBCOMP:
                case PUBREL:
                case DISCONNECT:
                case PUBACK:               
                    NettyChannel channel;
                    synchronized(m_channelMapper) {
                        if (!m_channelMapper.containsKey(ctx)) {
                            m_channelMapper.put(ctx, new NettyChannel(ctx));
                        }
                        channel = m_channelMapper.get(ctx);
                    }                    
                    m_messaging.handleProtocolMessage(channel, msg);
                    break;
                case PINGREQ:
                	PingRespMessage resp = new PingRespMessage();
                	ctx.writeAndFlush(resp);
                	break;
            }
        } catch (Exception ex) {
            LOG.error("Bad error in processing the message", ex);
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx)throws Exception {
        NettyChannel channel = m_channelMapper.get(ctx);
        String clientID = (String) channel.getAttribute(SessionConstants.ATTR_CLIENTID);
        m_messaging.lostConnection(channel);
        ctx.close(/*false*/);
        synchronized(m_channelMapper) {
            m_channelMapper.remove(ctx);
        }
    }
    
    public void setMessaging(Messaging messaging) {
        m_messaging = messaging;
    }
}
