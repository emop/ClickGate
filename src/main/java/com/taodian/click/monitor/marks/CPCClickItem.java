package com.taodian.click.monitor.marks;

import com.taodian.click.ShopAccount;
import com.taodian.click.ShopItem;
import com.taodian.click.monitor.Benchmark;
import com.taodian.click.monitor.StatusMark;
import com.taodian.click.monitor.UniqueQueue;


public class CPCClickItem extends StatusMark{	
	public UniqueQueue<ShopItem> uniqueList = new UniqueQueue<ShopItem>(10);

	public void markRequest(Benchmark mark){
		super.markRequest(mark);
		if(mark.obj != null){
			uniqueList.add((ShopItem)mark.obj);
		}
	}
}
