package com.github.shimonxin.lms.parser;

import com.github.shimonxin.lms.proto.MessageIDMessage;
import com.github.shimonxin.lms.proto.PubRelMessage;

/**
 *
 * @author andrea
 */
class PubRelDecoder extends MessageIDDecoder {

    @Override
    protected MessageIDMessage createMessage() {
        return new PubRelMessage();
    }

}

