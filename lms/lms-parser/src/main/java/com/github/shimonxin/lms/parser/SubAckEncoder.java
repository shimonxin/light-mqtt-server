package com.github.shimonxin.lms.parser;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import com.github.shimonxin.lms.proto.AbstractMessage;
import com.github.shimonxin.lms.proto.QoS;
import com.github.shimonxin.lms.proto.SubAckMessage;

/**
 *
 * @author andrea
 */
class SubAckEncoder extends DemuxEncoder<SubAckMessage> {

    @Override
    protected void encode(ChannelHandlerContext chc, SubAckMessage message, ByteBuf out) {
        if (message.types().isEmpty()) {
            throw new IllegalArgumentException("Found a suback message with empty topics");
        }

        ByteBuf variableHeaderBuff = chc.alloc().buffer(4);
        variableHeaderBuff.writeShort(message.getMessageID());
        for (QoS  c : message.types()) {
            variableHeaderBuff.writeByte(c.ordinal());
        }

        int variableHeaderSize = variableHeaderBuff.readableBytes();
        ByteBuf buff = chc.alloc().buffer(2 + variableHeaderSize);

        buff.writeByte(AbstractMessage.SUBACK << 4 );
        buff.writeBytes(Utils.encodeRemainingLength(variableHeaderSize));
        buff.writeBytes(variableHeaderBuff);

        out.writeBytes(buff);
    }
    
}
