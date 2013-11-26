package com.taodian.click;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taodian.api.TaodianApi;
import com.taodian.click.monitor.StatusMonitor;
import com.taodian.emop.Settings;
import com.taodian.emop.http.HTTPClient;
import com.taodian.emop.http.HTTPResult;
import com.taodian.emop.utils.CacheApi;
import com.taodian.emop.utils.SimpleCacheApi;

/**
 * 短网址的服务类。
 * 
 * @author deonwu
 */
public class ShortUrlService {
	private Log log = LogFactory.getLog("click.shorturl.service");
	private Log accesslog = null;

	private TaodianApi api = null;
	
	/**
	 * 兼容，老得冒泡点击统计。
	 */
	private HTTPClient http = null;
	
	private CacheApi cache = new SimpleCacheApi();
	private static ShortUrlService ins = null;
	
	private long nextUID = 0;
	private int urlCacheTime = 0;
	private int MODE = 10000;
	/**
	 * 写点击统计日志的线程池。
	 */
	protected ThreadPoolExecutor workerPool = null;
	/**
	 * 同步老得冒泡统计的线程池。这个响应速度有点慢。
	 */
	protected ThreadPoolExecutor syncPool = null;
	/**
	 * 查询短网址的线程池，把从API查询长连接，放到独立的线程去做。避免HTTP 线程被阻塞导致错误。
	 */
	protected ThreadPoolExecutor shortUrlPool = null;
	
	/**
	 * 这两个对队列，本身是应该为protected类型的。只是为了方便status里面显示，设置为public。
	 */
	public LinkedBlockingDeque<Runnable> pendingShortQueue = new LinkedBlockingDeque<Runnable>(150);
	public LinkedBlockingDeque<Runnable> writeLogQueue = null; //new LinkedBlockingDeque<Runnable>(150);

	public CopyOnWriteArraySet<String> pendingShortKey = new CopyOnWriteArraySet<String>();
	
	
	public static synchronized ShortUrlService getInstance(){
		if(ins == null){
			ins = new ShortUrlService();
			Settings.loadSettings();
	    	StatusMonitor.startMonitor();
	    	com.taodian.api.monitor.StatusMonitor.startMonitor();

			ins.start();
		}
		return ins;
	}
	
	/**
	 * 开始短网址服务，主要是初始化淘客API的连接信息。
	 */
	protected void start(){
		String appKey = Settings.getString(Settings.TAODIAN_APPID, null); // System.getProperty("");
		String appSecret = Settings.getString(Settings.TAODIAN_APPSECRET, null);
		String appRoute = Settings.getString(Settings.TAODIAN_APPROUTE, "http://api.zaol.cn/api/route");
		boolean inSAE = Settings.getString("in_sae", "n").equals("y");
		
		if(appKey != null && appSecret != null){
			api = new TaodianApi(appKey, appSecret, appRoute, inSAE ? "simple" : "apache");		
		}else {
			log.info("The taodian.api_id and taodian.api_secret Java properties are required.");
			System.exit(255);
		}
		
		int writeLogThread = Settings.getInt(Settings.WRITE_LOG_THREAD_COUNT, 10);
		int queueSize = Settings.getInt(Settings.WRITE_LOG_QUEUE_SIZE, 1024);
		int shortUrlThread = Settings.getInt(Settings.GET_SHORT_URL_THREAD_COUNT, 50);
		
		writeLogQueue = new LinkedBlockingDeque<Runnable>(queueSize);

		log.debug("start log write thread pool, core size:" + writeLogThread + ", queue size:");
		workerPool = new ThreadPoolExecutor(
				writeLogThread,
				writeLogThread * 2,
				10, 
				TimeUnit.SECONDS, 
				writeLogQueue
				);
		
		syncPool = new ThreadPoolExecutor(
				5,
				writeLogThread * 2,
				10, 
				TimeUnit.SECONDS, 
				new LinkedBlockingDeque<Runnable>(50)
				);

		shortUrlPool = new ThreadPoolExecutor(
				shortUrlThread,
				shortUrlThread * 2,
				10, 
				TimeUnit.SECONDS, 
				pendingShortQueue
				);

		if(Settings.getString(Settings.WRITE_ACCESS_LOG, "y").equals("y")){
			accesslog = LogFactory.getLog("click.accesslog");
		}
		
		urlCacheTime = Settings.getInt(Settings.CACHE_URL_TIMEOUT, 60);
		/*
		if(inSAE){
			cache = new SAECacheWrapper();
		}
		*/
		
		http = HTTPClient.create(inSAE ? "simple" : "apache");
	}
	
	

	public ShortUrlModel getTaobaokeUrlInfo(final URLInput req, boolean noCache){
		final String uri = req.getURI();
		Object tmp = null;
		
		for(int i = 0; i < 2 && tmp == null; i++){
			tmp = cache.get(uri, true);
			if(tmp == null || noCache){
				if(pendingShortQueue.remainingCapacity() > 1){
					if(!pendingShortKey.contains(uri)){
						pendingShortKey.add(uri);
						shortUrlPool.execute(new Runnable(){
							public void run(){
								try{
									convertTaokeLink(req);
								}finally{
									pendingShortKey.remove(uri);
									synchronized(uri){
										uri.notifyAll();
									}
								}
							}
						});
					}else {
						log.warn("short key in pending:" + uri);
					}
					synchronized(uri){
						try {
							uri.wait(1000 * 4);
							tmp = cache.get(uri, true);
						} catch (InterruptedException e) {
						}
					}
				}else {
					log.error("Have too many pending short url, queue size:" + pendingShortQueue.size());
				}	
			}			
		}
		
		if(tmp != null && tmp instanceof ShortUrlModel){
			/**
			 * 删除错误的转换结果。
			 */
			ShortUrlModel m = (ShortUrlModel)tmp;
			if(m.longUrl == null || m.longUrl.length() < 5){
				cache.remove(uri);
			}
			m.uri = req;
			return m;
		}
		return null;
	}

	protected void convertTaokeLink(URLInput key){
		if(key.type.equals(URLInput.INPUT_SHORT_URL)){
			getLongUrlFromRemote(key.getURI(), key.shortKey);
		}else {
			getTaokeFromRemote(key.getURI(), key.userName, key.info);
		}
	}

	protected void getTaokeFromRemote(String uri, String userName, String info){
	}
	
	protected void getLongUrlFromRemote(String uri, String shortKey){
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("short_key", shortKey);
		param.put("auto_mobile", "y");

		String errorMsg = "";
		for(int i = 0; i < 2; i++){
			HTTPResult r = api.call("tool_convert_long_url", param);
			
			ShortUrlModel m = new ShortUrlModel();
			if(r.isOK){
				if(i > 0){
					log.warn(String.format("The short url '%s' is get with retry %s times", shortKey, i));
				}
				m.shortKey = shortKey;
				m.longUrl = r.getString("data.long_url");
				m.mobileLongUrl = r.getString("data.mobile_long_url");
				cache.set(uri, m, urlCacheTime * 60);
				
				break;
			} //已经明确的返回错误了，就不用重试了。		
			else if(r.errorCode != null && r.errorCode.equals("not_found")){
				m.shortKey = shortKey;
				m.longUrl = "/";
				m.mobileLongUrl = "/";
				cache.set(uri, m, urlCacheTime * 60);
				
				break;
			}
			errorMsg = "code:" + r.errorCode + ", msg:" + r.errorMsg;
			log.error(String.format("The short url '%s' is not found, error:%s", shortKey, errorMsg));
			try{
				Thread.sleep(1000 * (i + 1));
			}catch(Exception e){}
		}
	}
	
	public void writeClickLog(final ShortUrlModel model){
		try{
			workerPool.execute(new Runnable(){
				public void run(){
					writeClickLogWithApi(model);
				}
			});
		}catch(Exception e){
			log.error("Log write thread pool is full", e);
		}
		
		if(Settings.getInt("old_emop_click", 0) == 1){
			if(syncPool.getQueue().remainingCapacity() > 1){
				syncPool.execute(new Runnable(){
					public void run(){
							String click = String.format("http://emop.sinaapp.com/UrlStat/stat/%s/%s/?uid=y", model.shortKey, model.uid);
							HTTPResult resp = http.post(click, new HashMap<String, Object>(), "text");	
							if(log.isDebugEnabled()){
								log.debug("old emop:" + click + ", resp:" + resp.text);
							}
						}
				});	
			}else {
				log.warn("Log old click sync pool is full");
			}
		}
	}
	
	private void writeClickLogWithApi(ShortUrlModel model){
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("short_key", model.shortKey);
		param.put("uid", model.uid);
		param.put("ip_addr", model.ip);
		param.put("agent", model.agent);
		param.put("refer", model.refer);
		
		HTTPResult r = api.call("tool_short_url_click", param);
		
		ShortUrlModel m = new ShortUrlModel();
		String msg = String.format("%s %s %s [%s] %s", model.shortKey, model.uid, model.ip,
				model.agent, model.refer);
		if(accesslog != null){
			accesslog.debug(msg);
		}else {
			log.debug(msg);
		}
		if(!r.isOK){
			log.warn("click log write error, short:" + model.shortKey + ", msg:" + r.errorMsg);
		}

	}
	
	public long newUserId(){
		nextUID = (nextUID + 1) % MODE;
		
		return (System.currentTimeMillis() % (MODE * 1000)) * MODE + nextUID; 
	}
	
	public Map<String, Object> cacheStat(){
		return cache.stat();
	}
	

}
