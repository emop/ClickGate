package com.taodian.route;

import java.util.HashMap;
import java.util.Map;

public class Action {
	/**
	 * 做下步正常跳转动作。所有日志，计费等都需要正常进行。
	 */
	public static final String FORWARD = "forward";
	
	/**
	 * 做拒绝跳转操作，跳转到错误错误页面。
	 */
	public static final String REJECT = "reject";
	
	/**
	 * 忽略日志记录，主要是CPC不用计费。但是正常跳转。
	 */
	public static final String IGNORE = "ignore";
	
	/**
	 * 重定向到其他地址。
	 */
	public static final String REDIRECT = "redirect";
	

	public String name = "";
	public String url = "";
	public boolean isLast = false;
	
	private static Map<String, Action> s = new HashMap<String, Action>();
	static{
		
	}
	
	public static Action get(String n){		
		return s.get(n);
	}
}
