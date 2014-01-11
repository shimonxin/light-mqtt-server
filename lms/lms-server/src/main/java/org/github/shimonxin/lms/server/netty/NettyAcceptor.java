package org.github.shimonxin.lms.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;

import io.netty.handler.timeout.IdleStateHandler;
import org.github.shimonxin.lms.server.ServerAcceptor;
import org.github.shimonxin.lms.server.netty.metrics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.shimonxin.lms.parser.MQTTDecoder;
import com.github.shimonxin.lms.parser.MQTTEncoder;
import com.github.shimonxin.lms.spi.messaging.Messaging;

/**
 * 
 * @author andrea
 */
public class NettyAcceptor implements ServerAcceptor {

	private static final Logger LOG = LoggerFactory.getLogger(NettyAcceptor.class);

	EventLoopGroup m_bossGroup;
	EventLoopGroup m_workerGroup;
	// BytesMetricsCollector m_metricsCollector = new BytesMetricsCollector();
	MessageMetricsCollector m_metricsCollector = new MessageMetricsCollector();

	public void initialize(Messaging messaging, String host, int port, final int defaultTimeout) throws IOException {
		m_bossGroup = new NioEventLoopGroup();
		m_workerGroup = new NioEventLoopGroup();

		final NettyMQTTHandler handler = new NettyMQTTHandler();
		handler.setMessaging(messaging);

		ServerBootstrap b = new ServerBootstrap();
		b.group(m_bossGroup, m_workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addFirst("idleStateHandler", new IdleStateHandler(0, 0, defaultTimeout));
				pipeline.addAfter("idleStateHandler", "idleEventHandler", new MqttIdleTimoutHandler(null,null));
				pipeline.addLast("decoder", new MQTTDecoder());
				pipeline.addLast("encoder", new MQTTEncoder());
				pipeline.addLast("metrics", new MessageMetricsHandler(m_metricsCollector));
				pipeline.addLast("handler", handler);
			}
		}).option(ChannelOption.SO_BACKLOG, 128).option(ChannelOption.SO_REUSEADDR, true).childOption(ChannelOption.SO_KEEPALIVE, true);
		try {
			// Bind and start to accept incoming connections.
			ChannelFuture f = b.bind(host, port);
			LOG.info("Server binded");
			f.sync();
		} catch (InterruptedException ex) {
			LOG.error(null, ex);
		}
	}

	public void close() {
		if (m_workerGroup == null) {
			throw new IllegalStateException("Invoked close on an Acceptor that wasn't initialized");
		}
		if (m_bossGroup == null) {
			throw new IllegalStateException("Invoked close on an Acceptor that wasn't initialized");
		}
		MessageMetrics metrics = m_metricsCollector.computeMetrics();
		m_workerGroup.shutdownGracefully();
		m_bossGroup.shutdownGracefully();
		// LOG.info(String.format("Bytes read: %d, bytes wrote: %d", metrics.readBytes(), metrics.wroteBytes()));
		LOG.info("Msg read: {}, msg wrote: {}", metrics.messagesRead(), metrics.messagesWrote());
	}

}
