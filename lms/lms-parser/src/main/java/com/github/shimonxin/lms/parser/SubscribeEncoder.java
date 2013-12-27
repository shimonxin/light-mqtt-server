package com.github.shimonxin.lms.parser;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import com.github.shimonxin.lms.proto.AbstractMessage;
import com.github.shimonxin.lms.proto.QoS;
import com.github.shimonxin.lms.proto.SubscribeMessage;

/**
 *
 * @author andrea
 */
class SubscribeEncoder extends DemuxEncoder<SubscribeMessage> {

    @Override
    protected void encode(ChannelHandlerContext chc, SubscribeMessage message, ByteBuf out) {
         if (message.subscriptions().isEmpty()) {
            throw new IllegalArgumentException("Found a subscribe message with empty topics");
        }

        if (message.getQos() != QoS.LEAST_ONE) {
            throw new IllegalArgumentException("Expected a message with QOS 1, found " + message.getQos());
        }
        
        ByteBuf variableHeaderBuff = chc.alloc().buffer(4);
        variableHeaderBuff.writeShort(message.getMessageID());
        for (SubscribeMessage.Couple c : message.subscriptions()) {
            variableHeaderBuff.writeBytes(Utils.encodeString(c.getTopic()));
            variableHeaderBuff.writeByte(c.getQos());
        }
        
        int variableHeaderSize = variableHeaderBuff.readableBytes();
        byte flags = Utils.encodeFlags(message);
        ByteBuf buff = chc.alloc().buffer(2 + variableHeaderSize);

        buff.writeByte(AbstractMessage.SUBSCRIBE << 4 | flags);
        buff.writeBytes(Utils.encodeRemainingLength(variableHeaderSize));
        buff.writeBytes(variableHeaderBuff);

        out.writeBytes(buff);
    }
    
}
