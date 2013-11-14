package com.taodian.click.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 一个Benchmark 队列，里面的数据根据。统计时间排序，只保留消耗时间最多的记录。
 */
public class MaxSizeQueue {
	protected int size = 0;
	protected ArrayBlockingQueue<Benchmark> queue = null;
	
	public MaxSizeQueue(int size){
		this.size = size > 0 ? size : 10;
		
		queue = new ArrayBlockingQueue<Benchmark>((int)(size * 1.5));
	}
	
	public void add(Benchmark mark){
		queue.add(mark);		
		if(queue.size() > size){
			queue.poll();
		}
	}
	
	public List<Benchmark> list(){
		ArrayList<Benchmark> data = new ArrayList<Benchmark>();
		data.addAll(queue);
		Collections.reverse(data);
		return data;
	}
}
