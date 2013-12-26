package com.github.shimonxin.lms.parser;

import com.github.shimonxin.lms.proto.MessageIDMessage;
import com.github.shimonxin.lms.proto.UnsubAckMessage;

/**
 *
 * @author andrea
 */
class UnsubAckDecoder extends MessageIDDecoder {

    @Override
    protected MessageIDMessage createMessage() {
        return new UnsubAckMessage();
    }
}

