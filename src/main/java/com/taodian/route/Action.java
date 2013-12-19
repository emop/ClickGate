package com.taodian.route;

import java.util.HashMap;
import java.util.Map;

public class Action {
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
