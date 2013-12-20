package com.taodian.route;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import com.taodian.click.ShortUrlModel;

/**
 * 一个规则路由器，根据输入条件匹配下一个处理的Action。
 * 
 * @author deonwu
 */
public interface Router {
	public TargetURL route(TargetURL next, ShortUrlModel model, HttpServletRequest req);
	
	/**
	 * 添加一条路由规则。
	 * @param expr
	 * @return
	 */
	public boolean addRoute(String expr);	
	/**
	 * 删除一条路由规则。
	 * @param expr
	 * @return
	 */
	public boolean delRoute(String expr);
	
	/**
	 * 保存所有路由到文件。
	 * @param path
	 */
	public void save(String path);
	public void load(String path);
	
	/**
	 * 输出整个路由表。
	 * @param writer
	 */
	public void dump(PrintWriter writer);
}
