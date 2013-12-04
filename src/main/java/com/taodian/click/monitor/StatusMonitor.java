package com.taodian.click.monitor;

import java.io.PrintWriter;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

import com.taodian.click.monitor.marks.CPCClickItem;
import com.taodian.click.monitor.marks.CPCRequest;
import com.taodian.click.monitor.marks.HTTPRequest;

public class StatusMonitor {
	public static StatusMonitor monitor = null;	
	protected ArrayBlockingQueue<Benchmark> marks = new ArrayBlockingQueue<Benchmark>(1024 * 5);
	protected Timer timer = new Timer();
	
	//public WeixinRequest weixin = new WeixinRequest();
	public HTTPRequest post = new HTTPRequest();
	public HTTPRequest get = new HTTPRequest();

	public HTTPRequest notfound = new HTTPRequest();

	public CPCRequest cpcOk = new CPCRequest();
	public CPCRequest cpcErr = new CPCRequest();
	public CPCClickItem itemErr = new CPCClickItem();
	
	public Date uptime = null;

	private StatusMonitor(){
		timer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				try{
					markAllItem();
				}catch(Throwable e){					
				}
			}
			
		}, 100, 100);
	}
	
	public static void startMonitor(){
		monitor = new StatusMonitor();
		monitor.uptime = new Date(System.currentTimeMillis());
	}
	
	public static void output(PrintWriter writer){
		if(monitor != null){
			TextStatusDumper.output(writer, monitor);
		}		
	}
	
	public static void hit(Benchmark mark){
		if(monitor != null){
			monitor.addStatus(mark);
		}
	}
	
	public void addStatus(Benchmark mark){
		if(marks.remainingCapacity() > 10){
			marks.add(mark);
		}
	}
	
	public void markAllItem(){
		if(marks.size() == 0) return;
		
		for(Benchmark m = marks.poll(); m != null; m = marks.poll()){
			if(m.type.equals(Benchmark.SHORT_KEY_GET)){
				get.markRequest(m);
			}else if(m.type.equals(Benchmark.SHORT_KEY_POST)){
				post.markRequest(m);
			}else if(m.type.equals(Benchmark.SHORT_KEY_NOT_FOUND)){
				notfound.markRequest(m);
			}else if(m.type.equals(Benchmark.CPC_CLICK_OK)){
				cpcOk.markRequest(m);
			}else if(m.type.equals(Benchmark.CPC_CLICK_FAILED)){
				cpcErr.markRequest(m);
			}else if(m.type.equals(Benchmark.CPC_ITEM_ERROR)){
				itemErr.markRequest(m);
			}
		}
	}
}
