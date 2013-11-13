package com.taodian.click.monitor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * 一个Benchmark 队列，里面的数据根据。统计时间排序，只保留消耗时间最多的记录。
 */
public class SortedQueue {
	protected int size = 0;
	protected TreeSet<Benchmark> queue = new TreeSet<Benchmark>(new Comparator<Benchmark>(){
		@Override
		public int compare(Benchmark b1, Benchmark b2) {
			//让数据倒序排列
			return b1.elapsed > b2.elapsed ? -1 : 1;
		}
		
	});
	
	public SortedQueue(int size){
		this.size = size > 0 ? size : 10;
	}
	
	public void add(Benchmark mark){
		queue.add(mark);		
		if(queue.size() > size){
			queue.pollLast();
		}
	}
	
	public List<Benchmark> list(){
		ArrayList<Benchmark> data = new ArrayList<Benchmark>();
		data.addAll(queue);
		return data;
	}
}
