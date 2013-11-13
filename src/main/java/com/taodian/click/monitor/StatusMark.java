package com.taodian.click.monitor;

public class StatusMark {
	public int requestCount = 0;
	public long maxElapsed = 0;
	public long averageElapsed = 0;
	public long minElapsed = 0;

	public SortedQueue slowList = new SortedQueue(10);
	public Benchmark last = null;
	
	public void markRequest(Benchmark mark){
		requestCount++;
		slowList.add(mark);	
		if(averageElapsed > 0){
			averageElapsed = (averageElapsed + mark.elapsed)  / 2;
		}else {
			averageElapsed = mark.elapsed;
		}
		last = mark;
		if(minElapsed == 0){
			minElapsed = mark.elapsed;
		}
		
		if(maxElapsed < mark.elapsed){
			maxElapsed = mark.elapsed;
		}else if(minElapsed > mark.elapsed){
			minElapsed = mark.elapsed;
		}
	}
}
