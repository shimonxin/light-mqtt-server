package com.github.shimonxin.lms.parser;

import com.github.shimonxin.lms.proto.MessageIDMessage;
import com.github.shimonxin.lms.proto.PubRecMessage;

/**
 *
 * @author andrea
 */
class PubRecDecoder extends MessageIDDecoder {

    @Override
    protected MessageIDMessage createMessage() {
        return new PubRecMessage();
    }
}
