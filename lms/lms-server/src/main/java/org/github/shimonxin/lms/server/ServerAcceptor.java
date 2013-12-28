package org.github.shimonxin.lms.server;

import java.io.IOException;

import com.github.shimonxin.lms.spi.messaging.Messaging;

/**
 *
 * @author andrea
 */
public interface ServerAcceptor {
    
    void initialize(Messaging messaging,String host,int port,int defaultTimeout) throws IOException;
    
    void close();
}
