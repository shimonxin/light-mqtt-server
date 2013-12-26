package com.github.shimonxin.lms.parser;

import com.github.shimonxin.lms.proto.MessageIDMessage;
import com.github.shimonxin.lms.proto.PubCompMessage;


/**
 *
 * @author andrea
 */
class PubCompDecoder extends MessageIDDecoder {

    @Override
    protected MessageIDMessage createMessage() {
        return new PubCompMessage();
    }
}
