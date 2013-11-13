package com.taodian.click.monitor;

import java.util.Date;

/**
 * 服务监控的统计信息。
 * @author deonwu
 *
 */
public class StatusModel {
	public Date startTime = null;
	
	public int timeoutCount = 0;
	public int requestCount = 0;

	
	public static void benchmark(){
		
	}
	
}
