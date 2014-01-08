/**
 * ServerChannel.java created at 2013-12-26 上午11:55:25 by ShimonXin
 */
package com.github.shimonxin.lms.spi.session;

import com.github.shimonxin.lms.spi.messaging.ProtocolProcessor;

/**
 * ServerChannel
 * @author ShimonXin
 * @created 2013-12-26
 * 
 */
public interface ServerChannel {

    Object getAttribute(Object key);
    
    void setAttribute(Object key, Object value);
    
    void setIdleTime(int idleTime,ProtocolProcessor processor);
    
    void close(boolean immediately);
    
    void write(Object value);
}
