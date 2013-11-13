package com.taodian.click.monitor.marks;

import com.taodian.click.monitor.Benchmark;
import com.taodian.click.monitor.StatusMark;


public class PendingRequest extends StatusMark{	
	public int timeoutCount = 0;
	
	public void markTimeOut(Benchmark mark){
		timeoutCount++;
		slowList.add(mark);
	}
}
