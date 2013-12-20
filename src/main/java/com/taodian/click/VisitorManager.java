package com.taodian.click;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VisitorManager {
	private Log log = LogFactory.getLog("click.visitors");

	public Map<String, ClickVisitor> clients = new ConcurrentHashMap<String, ClickVisitor>();
	/**
	 * 通过Queue来确保日志的读写顺序。
	 */
	public BlockingQueue<String> buffer = new ArrayBlockingQueue<String>(100);

	protected Lock writeLock = new ReentrantLock();
	
	protected ThreadPoolExecutor executor = null;
	public VisitorManager(ThreadPoolExecutor pool){
		this.executor = pool;	
	}
	
	public synchronized void register(String name, ClickVisitorChannel channel){
		
		ClickVisitor v = clients.get(name);
		if(v == null){
			v =	new ClickVisitor(name);
			clients.put(name, v);
		}
		v.addChannel(channel);
	}
	
	public void write(String msg){
		if(buffer.remainingCapacity() > 5){
			buffer.add(msg);
			if(writeLock.tryLock()){
				writeLock.unlock();
				executor.execute(new Runnable(){
					public void run(){
						flushMessage();
					}
				});
			}
		}else {
			log.warn("The visitor log writer buffer is full。");
		}
	}
	
	protected void flushMessage(){
		if(writeLock.tryLock()){
			try{
				for(String msg = buffer.poll(); msg != null; msg = buffer.poll(1, TimeUnit.SECONDS)){
					for(ClickVisitor c : clients.values()){
						if(c.isClosed()){
							clients.remove(c.name);
							continue;
						}
						try {
							c.write(msg);
						} catch (IOException e) {
							log.error(e.toString(), e);
							clients.remove(c.name);
						}
					}
				}
			} catch (InterruptedException e) {
				log.debug("the buffer read waiting is intterupated.");
			}finally{
				writeLock.unlock();
			}
		}
	}
}
