/**
 * MatchingCondition.java created at 2013-12-26 下午5:41:16 by ShimonXin
 */
package com.github.shimonxin.lms.spi.subscriptions;

/**
 * Matching Condition
 * @author ShimonXin
 * @created 2013-12-26
 * 
 */
public interface MatchingCondition {
	 boolean match(String key);
}
