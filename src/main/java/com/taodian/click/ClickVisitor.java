package com.taodian.click;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.mortbay.log.Log;

public class ClickVisitor {
	public String name = "";
	
	public BlockingQueue<String> buffer = new ArrayBlockingQueue<String>(200);
	public ArrayList<ClickVisitorChannel> channels = new ArrayList<ClickVisitorChannel>();
	
	//private 
	private long lastWriteTime = 0;
	
	public ClickVisitor(String name){
		this.name = name;
	}
	
	public void addChannel(ClickVisitorChannel c){
		this.channels.add(c);
		if(!buffer.isEmpty()){
			flushMessage(c);
		}
	}
	
	public void write(String msg) throws IOException{
		if(channels.isEmpty()){
			if(buffer.remainingCapacity() > 0){
				buffer.add(msg);
			}else {
				Log.debug(String.format("'%s' log write buffer is full。", name));
			}
		}else {
			boolean isWrote = false;
			for(Iterator<ClickVisitorChannel> iter = channels.iterator(); iter.hasNext();){
				ClickVisitorChannel ch = iter.next();
				if(!ch.continuation.isPending()){
					iter.remove();
					continue;
				}
				try{
					ch.writer.println(msg);
					ch.writer.flush();
					isWrote = true;
				}catch(Exception e){
					iter.remove();
				}
			}
			if(!isWrote){
				buffer.add(msg);
			}else {
				lastWriteTime = System.currentTimeMillis();
			}
		}
	}
	
	/**
	 * 写日志通道为空，并且超过5分钟没有任何写操作。确定客户端关闭。
	 * @return
	 */
	public boolean isClosed(){
		return channels.isEmpty() && System.currentTimeMillis() - lastWriteTime > 5 * 60 * 1000;
	}
	
	private void flushMessage(ClickVisitorChannel c){
		for(String m = buffer.poll(); m != null; m = buffer.poll()){
			c.writer.println(m);
		}
		c.writer.flush();
	}
}
