/**
 * Authenticator.java created at 2013-12-26 下午3:25:26 by ShimonXin
 */
package com.github.shimonxin.lms.spi;

/**
 * username and password checker
 * 
 * @author ShimonXin
 * @created 2013-12-26
 * 
 */
public interface Authenticator {
	/**
	 * 
	 * 认证
	 * 
	 * @param clientId
	 *            客户端ID
	 * @param username
	 *            用户名
	 * @param password
	 *            密码
	 * @return true 认证成功，false 认证失败
	 */
	boolean auth(String clientId, String username, String password);
}
