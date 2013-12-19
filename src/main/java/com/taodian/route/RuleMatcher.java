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
	
	/**
	 * 检查两个规则是否匹配。把路由规则，和查询条件都转换为相同的形式进行比较。支持严格匹配，和模糊匹配
	 * 两种方式， 严格匹配只两个规则的所有字段都需要一样。
	 * 
	 * 模糊匹配，只检查不为空的关键字段。
	 * 例如：
	 * r1.user_id = 74, uid = 0, short_key = '001'
	 * r2.user_id = 74, uid = 1234, short_key = '001'
	 * 
	 * 在模糊匹配的情况下，两个规则匹配成功。因为r1 的uid 为空，表示不检查。在严格匹配的方式下，失败。
	 * 因为uid 不一样。
	 *  
	 * @param r
	 * @param r2
	 * @param isStrict 是否进行严格的匹配检查。
	 * @return true 如果两个规则匹配。
	 */
	public boolean isMatch(Rule r, Rule r2, boolean isStrict);
}
