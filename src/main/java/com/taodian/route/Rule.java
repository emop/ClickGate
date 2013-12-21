package com.taodian.route;


public class Rule {
	public int sourceUserId = 0;
	public long sourceShopId = 0;
	public long sourceNumIid = 0;
	public boolean sourceIsNewUser = false;
	public boolean sourceIsMobile = false;
	
	public String sourceIP = null;
	public String sourceShortKey = null;
	
	public String targetAction = "";
	public String nextUrl = "";
	
	public long expired = 0;
}
