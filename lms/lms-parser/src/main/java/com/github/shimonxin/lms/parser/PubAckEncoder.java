package com.github.shimonxin.lms.parser;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import com.github.shimonxin.lms.proto.AbstractMessage;
import com.github.shimonxin.lms.proto.PubAckMessage;

/**
 *
 * @author andrea
 */
class PubAckEncoder extends DemuxEncoder<PubAckMessage> {

    @Override
    protected void encode(ChannelHandlerContext chc, PubAckMessage msg, ByteBuf out) {
        ByteBuf buff = chc.alloc().buffer(4);
        buff.writeByte(AbstractMessage.PUBACK << 4);
        buff.writeBytes(Utils.encodeRemainingLength(2));
        buff.writeShort(msg.getMessageID());
        out.writeBytes(buff);
    }
    
}
