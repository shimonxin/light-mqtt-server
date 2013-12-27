package com.github.shimonxin.lms.parser;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import com.github.shimonxin.lms.proto.AbstractMessage;
import com.github.shimonxin.lms.proto.QoS;
import com.github.shimonxin.lms.proto.UnsubscribeMessage;


/**
 *
 * @author andrea
 */
class UnsubscribeEncoder extends DemuxEncoder<UnsubscribeMessage> {

    @Override
    protected void encode(ChannelHandlerContext chc, UnsubscribeMessage message, ByteBuf out) {
        if (message.topics().isEmpty()) {
            throw new IllegalArgumentException("Found an unsubscribe message with empty topics");
        }

        if (message.getQos() != QoS.LEAST_ONE) {
            throw new IllegalArgumentException("Expected a message with QOS 1, found " + message.getQos());
        }
        
        ByteBuf variableHeaderBuff = chc.alloc().buffer(4);
        variableHeaderBuff.writeShort(message.getMessageID());
        for (String topic : message.topics()) {
            variableHeaderBuff.writeBytes(Utils.encodeString(topic));
        }
        
        int variableHeaderSize = variableHeaderBuff.readableBytes();
        byte flags = Utils.encodeFlags(message);
        ByteBuf buff = chc.alloc().buffer(2 + variableHeaderSize);

        buff.writeByte(AbstractMessage.UNSUBSCRIBE << 4 | flags);
        buff.writeBytes(Utils.encodeRemainingLength(variableHeaderSize));
        buff.writeBytes(variableHeaderBuff);

        out.writeBytes(buff);
    }
    
}
