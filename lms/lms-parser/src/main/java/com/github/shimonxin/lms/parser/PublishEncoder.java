package com.github.shimonxin.lms.parser;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import com.github.shimonxin.lms.proto.AbstractMessage;
import com.github.shimonxin.lms.proto.PublishMessage;
import com.github.shimonxin.lms.proto.QoS;

/**
 *
 * @author andrea
 */
class PublishEncoder extends DemuxEncoder<PublishMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, PublishMessage message, ByteBuf out) {
        if (message.getQos() == QoS.RESERVED) {
            throw new IllegalArgumentException("Found a message with RESERVED Qos");
        }
        if (message.getTopicName() == null || message.getTopicName().isEmpty()) {
            throw new IllegalArgumentException("Found a message with empty or null topic name");
        }
        
        ByteBuf variableHeaderBuff = ctx.alloc().buffer(2);
        variableHeaderBuff.writeBytes(Utils.encodeString(message.getTopicName()));
        if (message.getQos() == QoS.LEAST_ONE || 
            message.getQos() == QoS.EXACTLY_ONCE ) {
            if (message.getMessageID() == null) {
                throw new IllegalArgumentException("Found a message with QOS 1 or 2 and not MessageID setted");
            }
            variableHeaderBuff.writeShort(message.getMessageID());
        }
        variableHeaderBuff.writeBytes(message.getPayload());
        int variableHeaderSize = variableHeaderBuff.readableBytes();
        
        byte flags = Utils.encodeFlags(message);
        
        ByteBuf buff = ctx.alloc().buffer(2 + variableHeaderSize);
        buff.writeByte(AbstractMessage.PUBLISH << 4 | flags);
        buff.writeBytes(Utils.encodeRemainingLength(variableHeaderSize));
        buff.writeBytes(variableHeaderBuff);

        out.writeBytes(buff);
    }
    
}
