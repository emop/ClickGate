package com.taodian.click;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ClickVisitor {
	private Log log = LogFactory.getLog("click.visitor");
	
	public String name = "";
	
	public BlockingQueue<String> buffer = new ArrayBlockingQueue<String>(200);
	public CopyOnWriteArrayList<ClickVisitorChannel> channels = new CopyOnWriteArrayList<ClickVisitorChannel>();
	
	//private 
	private long lastWriteTime = 0;
	
	public ClickVisitor(String name){
		this.name = name;
	}
	
	public void addChannel(ClickVisitorChannel c){
		synchronized(c.writer) {
			log.info(name + " add new channel:" + c.continuation + ",c:" + c.toString() + ", writer:" + c.writer);
			if(!buffer.isEmpty()){
				flushMessage(c);
			}
			
			for(Iterator<ClickVisitorChannel> iter = channels.iterator(); iter.hasNext();){
				ClickVisitorChannel ch = iter.next();
				if(ch.writer.equals(c.writer)){
					iter.remove();
					ch.continuation.resume();
				}
			}
			this.channels.add(c);
		}
	}
	
	public void write(String msg) throws IOException{
		if(channels.isEmpty()){
			if(buffer.remainingCapacity() > 0){
				buffer.add(msg);
			}else {
				log.debug(String.format("'%s' log write buffer is full。", name));
			}
		}else {
			boolean isWrote = false;
			for(Iterator<ClickVisitorChannel> iter = channels.iterator(); iter.hasNext();){
				ClickVisitorChannel ch = iter.next();
				synchronized(ch.writer) {
					if(!ch.continuation.isPending() || ch.isTimouted()){
						iter.remove();
	
						ch.continuation.resume();
						log.info(name + " remove resumed channel:" + ch.continuation + ",c:" + ch.toString() + ", writer:" + ch.writer);
						continue;
					}
					try{
						ch.writer.println(msg);
						ch.writer.flush();
						isWrote = true;
					}catch(Exception e){
						iter.remove();
						log.info(name + " remove exception channel:" + ch.continuation + ", exception:" + e.toString() + ",c:" + ch.toString() + ", writer:" + ch.writer);
					}
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
