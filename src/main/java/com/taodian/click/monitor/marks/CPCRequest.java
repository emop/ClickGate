package com.taodian.click.monitor.marks;

import com.taodian.click.ShopAccount;
import com.taodian.click.monitor.Benchmark;
import com.taodian.click.monitor.StatusMark;
import com.taodian.click.monitor.UniqueQueue;


public class CPCRequest extends StatusMark{	
	public UniqueQueue<ShopAccount> uniqueList = new UniqueQueue<ShopAccount>(10);

	public void markRequest(Benchmark mark){
		super.markRequest(mark);
		if(mark.obj != null){
			uniqueList.add((ShopAccount)mark.obj);
		}
	}
}
