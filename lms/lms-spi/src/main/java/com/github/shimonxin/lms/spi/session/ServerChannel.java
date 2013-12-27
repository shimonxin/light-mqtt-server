/**
 * ServerChannel.java created at 2013-12-26 上午11:55:25 by ShimonXin
 */
package com.github.shimonxin.lms.spi.session;

/**
 * ServerChannel
 * @author ShimonXin
 * @created 2013-12-26
 * 
 */
public interface ServerChannel {

    Object getAttribute(Object key);
    
    void setAttribute(Object key, Object value);
    
    void setIdleTime(int idleTime);
    
    void close();
    
    void write(Object value);
}
