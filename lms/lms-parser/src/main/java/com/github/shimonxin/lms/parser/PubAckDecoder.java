package com.github.shimonxin.lms.parser;

import com.github.shimonxin.lms.proto.MessageIDMessage;
import com.github.shimonxin.lms.proto.PubAckMessage;

/**
 *
 * @author andrea
 */
class PubAckDecoder extends MessageIDDecoder {

    @Override
    protected MessageIDMessage createMessage() {
        return new PubAckMessage();
    }
    
}
