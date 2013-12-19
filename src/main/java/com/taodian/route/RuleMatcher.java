package com.taodian.route;

/**
 * 比较俩个规则是否相同。
 * 
 * 需要根据不同的链路，使用不同的比较方式。例如CPC的，可能优先比较UserID。如果是普通的可以优先比较
 * IP。
 * @author deonwu
 *
 */
public interface RuleMatcher {
	public boolean isMatch(Rule r, Rule r2, boolean isStrict);
}
