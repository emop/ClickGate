package com.taodian.route;

/**
 * 短网址下一跳地址。
 * 
 * @author deonwu
 *
 */
public class TargetURL {
	//public static final String IGNORE = "ignore";
	
	public String url = null;
	public boolean isOK = false;
	
	//public boolean writeLog = true;
	
	public String actionName = Action.FORWARD;
	
	public boolean isLast = false;
}
