package com.github.shimonxin.lms.parser;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import com.github.shimonxin.lms.proto.AbstractMessage;
import com.github.shimonxin.lms.proto.PingRespMessage;

/**
 *
 * @author andrea
 */
class PingRespEncoder extends DemuxEncoder<PingRespMessage> {

    @Override
    protected void encode(ChannelHandlerContext chc, PingRespMessage msg, ByteBuf out) {
        out.writeByte(AbstractMessage.PINGRESP << 4).writeByte(0);
    }
}
