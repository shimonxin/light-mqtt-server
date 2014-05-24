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

        int variableHeaderSize = 2 + message.types().size();
        ByteBuf buff = chc.alloc().buffer(6 + variableHeaderSize);
        try {
            buff.writeByte(AbstractMessage.SUBACK << 4 );
            buff.writeBytes(Utils.encodeRemainingLength(variableHeaderSize));
            buff.writeShort(message.getMessageID());
            for (QoS c : message.types()) {
                buff.writeByte(c.ordinal());
            }

            out.writeBytes(buff);
        } finally {
            buff.release();
        }
    }
    
}
