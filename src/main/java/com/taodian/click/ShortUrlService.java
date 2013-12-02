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
import com.taodian.click.monitor.Benchmark;
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
	
	/**
	 * 短网址缓存。
	 */
	private CacheApi cache = new SimpleCacheApi();
	
	/**
	 * CPC 点击统计，商家计费缓存。
	 */
	private CacheApi cpcCache = new SimpleCacheApi();

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
				m.shortKeySource = r.getString("data.create_source");
				m.platform = r.getString("data.plat_form");
				m.outId = r.getString("data.out_id");
				
				String tmp = r.getString("data.user_id");
				if(tmp != null && tmp.length() > 0){
					m.userId = Integer.parseInt(tmp);
				}
				tmp = r.getString("data.num_iid");
				if(tmp != null && tmp.length() > 0){
					m.numIid = Long.parseLong(tmp);
				}
				tmp = r.getString("data.shop_id");
				if(tmp != null && tmp.length() > 0){
					m.shopId = Long.parseLong(tmp);
				}
				
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
	
	public NextURL cpcServiceCheck(ShortUrlModel model, NextURL next){
		/**
		 * 如果没有商家信息，就不知道向谁收钱。不能跳转。
		 */
		if(model.shopId > 0){
			if(hitShopClick(model.shopId, model.numIid)){
				next.isOK = true;
				next.url = model.longUrl;
			}else {
				next.url = this.getUserClickNoEnableUrl(model.userId, model.outId, model.platform);
				log.debug("hit cpc error, to default url:" + next.url + ", user id:" + model.userId);
			}
		}else {
			log.debug("Not found shop id for CPC short url:" + model.shortKey);
			next.isOK = false;
			next.url = this.getUserClickNoEnableUrl(model.userId, model.outId, model.platform);
		}
		return next;
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
	
	/**
	 * 点击商家商品，如果余额不足或者没有找到商家信息。返回false。 表示点击失效，转到淘客
	 * 默认的连接去。
	 * @param shopId
	 * @param numIid
	 * @return
	 */
	private boolean hitShopClick(long shopId, long numIid){
		ShopAccount account = this.getShopAccount(shopId);
		ShopItemPrice price = this.getShopItemPrice(shopId, numIid);
		if(account != null && price != null){
			if(account.banlance > price.price){
				account.banlance -= price.price;
			}
		}
		
		boolean isOk = account != null && account.banlance > 0;
		Benchmark m = Benchmark.start(Benchmark.CPC_CLICK_OK);
		m.attachObject(account);
		if(isOk){
			m.done(Benchmark.CPC_CLICK_OK, 0);
		}else {
			m.done(Benchmark.CPC_CLICK_FAILED, 0);
		}
		
		return isOk;
	}
	
	private ShopAccount getShopAccount(final long shopId){
		final String ac = "shop_" + shopId;
		ShopAccount account = null;
		Object tmp = null;
		for(int i = 0; i < 2 && tmp == null; i++){
			tmp = cpcCache.get(ac);
			if(tmp == null){
				if(pendingShortQueue.remainingCapacity() > 1){
					if(!pendingShortKey.contains(ac)){
						pendingShortKey.add(ac);
						shortUrlPool.execute(new Runnable(){
							public void run(){
								try{
									getRemoteAccount(ac, shopId);
								}finally{
									pendingShortKey.remove(ac);
									synchronized(ac){
										ac.notifyAll();
									}
								}
							}
						});
					}else {
						log.warn("shop id in pending:" + ac);
					}
					synchronized(ac){
						try {
							ac.wait(1000 * 4);
							tmp = cpcCache.get(ac, true);
						} catch (InterruptedException e) {
						}
					}
				}else {
					log.error("Have too many pending shop account, queue size:" + pendingShortQueue.size());
				}	
			}			
		}
		
		if(tmp != null && (tmp instanceof ShopAccount)){
			account = (ShopAccount)tmp;
		}else {
			account = new ShopAccount();
			account.shopId = shopId;
			account.banlance = 0;
			account.status = "not_found";
			cpcCache.set(ac, account, 60);
		}
		return account;
	}
	
	private ShopItemPrice getShopItemPrice(final long shopId, final long numIid){
		final String ac = "item_" + shopId + "_" + numIid;
		ShopItemPrice account = null;
		Object tmp = null;
		for(int i = 0; i < 2 && tmp == null; i++){
			tmp = cpcCache.get(ac);
			if(tmp == null){
				if(pendingShortQueue.remainingCapacity() > 1){
					if(!pendingShortKey.contains(ac)){
						pendingShortKey.add(ac);
						shortUrlPool.execute(new Runnable(){
							public void run(){
								try{
									getRemoteShopItemPrice(ac, shopId, numIid);
								}finally{
									pendingShortKey.remove(ac);
									synchronized(ac){
										ac.notifyAll();
									}
								}
							}
						});
					}else {
						log.warn("item price in pending:" + ac);
					}
					synchronized(ac){
						try {
							ac.wait(1000 * 4);
							tmp = cpcCache.get(ac, true);
						} catch (InterruptedException e) {
						}
					}
				}else {
					log.error("Have too many pending item price, queue size:" + pendingShortQueue.size());
				}	
			}			
		}
		
		if(tmp != null && (tmp instanceof ShopItemPrice)){
			account = (ShopItemPrice)tmp;
		}else {
			account = new ShopItemPrice();
			account.numIid = numIid;
			account.price = 0;
			int price = Settings.getInt("default_cpc_click_price", 10);
			account.price = price / 100.0f;
			cpcCache.set(ac, account, 60);
		}
		
		return account;
	}	
	
	private ShopAccount getRemoteAccount(String ck, long shopId){
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("shop_id", shopId);
		param.put("user_id", shopId);
		param.put("account_group", "cpc");

		HTTPResult r = api.call("credit_get_credit_account", param);
		
		ShopAccount ac = new ShopAccount();
		ac.shopId = shopId;
		if(r.isOK){
			String f = r.getString("data.banlance");
			String s = r.getString("data.status");
			ac.status = s;
			if(s != null && s.equals("0")){
				ac.banlance = Float.parseFloat(f);
			}
			cpcCache.set(ck, ac, 5 * 60);
		}else {
			ac.status = r.errorCode;
		}
		
		return ac;
	}
	
	private ShopItemPrice getRemoteShopItemPrice(String ck, long shopId, long numIid){
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("shop_id", shopId);
		param.put("num_iid", numIid);

		HTTPResult r = api.call("credit_get_cpc_item_price", param);
		
		ShopItemPrice ac = new ShopItemPrice();
		ac.numIid = numIid;
		int price = Settings.getInt("default_cpc_click_price", 10);
		ac.price = price / 100.0f;				

		if(r.isOK){
			String f = r.getString("data.price");
			String s = r.getString("data.status");
			if(s != null && s.equals("0")){
				ac.price = Float.parseFloat(f);
			}
			
			cpcCache.set(ck, ac, 5 * 60);
		}
		return ac;
	}	
	

	/**
	 * 查询淘客的，点击失效的默认连接。
	 * @param url
	 * @return
	 */
	private String getUserClickNoEnableUrl(final int userId, final String outId, final String platform){
		final String ac = "c_" + userId + "_" + outId + "_" + platform;
		
		Object tmp = null;
		for(int i = 0; i < 2 && tmp == null; i++){
			tmp = cpcCache.get(ac);
			if(tmp == null){
				if(pendingShortQueue.remainingCapacity() > 1){
					if(!pendingShortKey.contains(ac)){
						pendingShortKey.add(ac);
						shortUrlPool.execute(new Runnable(){
							public void run(){
								try{
									Map<String, Object> param = new HashMap<String, Object>();
									param.put("user_id", userId);
									param.put("out_id", outId);
									param.put("platform", platform);

									HTTPResult r = api.call("credit_get_user_not_found_shop_url", param);									
									if(r.isOK){
										String f = r.getString("data.url");
										cpcCache.set(ac, f, 5 * 60);
									}
								}finally{
									pendingShortKey.remove(ac);
									synchronized(ac){
										ac.notifyAll();
									}
								}
							}
						});
					}else {
						log.warn("default click in pending:" + ac);
					}
					synchronized(ac){
						try {
							ac.wait(1000 * 4);
							tmp = cpcCache.get(ac, true);
						} catch (InterruptedException e) {
						}
					}
				}else {
					log.error("Have too many pending click url, queue size:" + pendingShortQueue.size());
				}
			}
		}
		
		return tmp == null ? "/" : tmp.toString();
	}
	
}
