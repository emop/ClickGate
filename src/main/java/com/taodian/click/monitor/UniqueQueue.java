package com.taodian.click.monitor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 一个Benchmark 队列，里面的数据根据。统计时间排序，只保留消耗时间最多的记录。
 */
public class UniqueQueue<T> {
	protected int size = 0;
	protected ArrayBlockingQueue<T> queue = null;
	
	public UniqueQueue(int size){
		this.size = size > 0 ? size : 10;
		queue = new ArrayBlockingQueue<T>((int)(size * 1.5));
	}
	
	public void add(T mark){
		if(mark == null)return;
		if(!queue.contains(mark)){
			queue.add(mark);
			if(queue.size() > size){
				queue.poll();
			}
		}else {
			queue.remove(mark);
			queue.add(mark);
		}
	}
	
	public List<T> list(){
		ArrayList<T> data = new ArrayList<T>();
		data.addAll(queue);
		return data;
	}
}
