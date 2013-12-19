package com.taodian.click;

/**
 * 短网址下一跳地址。
 * 
 * @author deonwu
 *
 */
public class NextURL {
	public static final String FORWARD = "forward";
	public static final String IGNORE = "ignore";
	//public static final String IGNORE = "ignore";
	
	public String url = null;
	public boolean isOK = false;
	
	public boolean writeLog = true;
	
	public String actionName = FORWARD;
	
	public boolean isLast = false;
}
