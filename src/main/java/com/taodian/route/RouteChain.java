package com.taodian.route;

import java.util.List;

/**
 * 路由链，是路由的一个子集，路由表里面包含的是路由链。路由链才包含具体的匹配规则。
 * 
 * @author deonwu
 */
public interface RouteChain {
	//public NextURL route(NextURL next, ShortUrlModel model, HttpServletRequest req);
	
	/**
	 * 根据输入规则，匹配一个需要执行的一个规则。
	 * @param rule
	 * @return 返回相匹配的规则，如果没有匹配任何规则, 返回null。
	 */
	public Rule match(Rule rule);
	
	/**
	 * 添加一条路由规则。
	 * @param expr
	 * @return
	 */
	public boolean addRoute(Rule rule);
	public boolean addRoute(int index, Rule rule);
	
	/**
	 * 删除一条路由规则。
	 * @param expr
	 * @return
	 */
	public boolean delRoute(Rule rule);
	
	public List<Rule> rules();
}
