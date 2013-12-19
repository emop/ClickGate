package com.taodian.route;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import com.taodian.click.NextURL;
import com.taodian.click.ShortUrlModel;

public class DefaultRouter implements Router {
	/**
	 * 处理通用的路由规则，例如根据来源IP过滤等。
	 */
	protected RouteChain input = null;
	
	/**
	 * CPC 专用过滤规则。
	 */
	protected RouteChain cpcForword = null;

	@Override
	public NextURL route(NextURL next, ShortUrlModel model,
			HttpServletRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean addRoute(String expr) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delRoute(String expr) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void save(String path) {
		// TODO Auto-generated method stub

	}

	@Override
	public void load(String path) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dump(PrintWriter writer) {
		// TODO Auto-generated method stub

	}

}
