package org.github.shimonxin.lms.server.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;

import org.apache.log4j.Logger;

import com.github.shimonxin.lms.spi.messaging.ProtocolProcessor;
import com.github.shimonxin.lms.spi.session.ServerChannel;

class MqttIdleTimoutHandler extends ChannelDuplexHandler {
	private static Logger logger = Logger.getLogger(MqttIdleTimoutHandler.class);
	private ProtocolProcessor processor;
	private ServerChannel session;

	public MqttIdleTimoutHandler(ProtocolProcessor processor, ServerChannel session) {
		this.processor = processor;
		this.session = session;
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleState) {
			IdleState e = (IdleState) evt;
			if (e == IdleState.ALL_IDLE) {
				if (processor == null || session == null) {
					logger.debug("time out,simply close connection");
					ctx.close();
				} else {
					logger.debug("time out,process disconnect");
					processor.processDisconnect(session);
				}
			}
		}
	}
}
