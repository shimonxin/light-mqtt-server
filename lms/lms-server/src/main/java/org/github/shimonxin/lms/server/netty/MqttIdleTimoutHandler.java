package org.github.shimonxin.lms.server.netty;

import com.github.shimonxin.lms.spi.messaging.ProtocolProcessor;
import com.github.shimonxin.lms.spi.session.ServerChannel;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;

class MqttIdleTimoutHandler extends ChannelDuplexHandler {
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
					ctx.close();
				} else {
					processor.processDisconnect(session);
				}
			}
		}
	}
}
