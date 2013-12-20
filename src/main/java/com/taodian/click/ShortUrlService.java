package com.taodian.click;

import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taodian.api.TaodianApi;
import com.taodian.click.monitor.Benchmark;
import com.taodian.click.monitor.StatusMonitor;
import com.taodian.click.session.HTML5RedisSessionManager;
import com.taodian.emop.Settings;
import com.taodian.emop.http.HTTPClient;
import com.taodian.emop.http.HTTPResult;
import com.taodian.emop.utils.CacheApi;
import com.taodian.emop.utils.SimpleCacheApi;
import com.taodian.route.TargetURL;
import com.taodian.route.Router;

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
	
	protected TaobaoPool taobao = null;
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
	public VisitorManager vm = null;
	public Router router = null;
	public SessionManager sm = null;
	
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
		
		vm = new VisitorManager(workerPool);
		
		sm = new HTML5RedisSessionManager(this);
		/*
		if(inSAE){
			cache = new SAECacheWrapper();
		}
		*/
		taobao = new TaobaoPool();
		taobao.start(shortUrlPool, api,
				new TaodianApi(appKey, appSecret, "http://fmei.sinaapp.com/api/route",
						inSAE ? "simple" : "apache"));
		
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
	
	public TargetURL cpcServiceCheck(ShortUrlModel model, TargetURL next){
		/**
		 * 如果没有商家信息，就不知道向谁收钱。不能跳转。
		 */
		if(model.shopId > 0){
			if(hitShopClick(model.shopId, model.numIid)){
				next.isOK = true;
				next.url = model.longUrl;
				
				/**
				 * 早期生成的店铺首页。如果是单品链接，做强制转换重新生成。
				 */
				if(model.shopId == model.numIid && next.url.startsWith("http://item")){
					next.url = "http://shop" + model.shopId + ".taobao.com/";	
				}
			}else {
				next.isOK = false;
				next.url = this.getUserClickNoEnableUrl(model.userId, model.outId, model.platform);
				Map<String, String> param = new HashMap<String, String>();
				param.put("shop_id", model.shopId + "");
				param.put("user_id", model.userId + "");
				param.put("num_iid", model.numIid + "");
				param.put("short_key", model.shortKey);
				next.url = resolveVariable(next.url, param);
				log.debug("hit cpc error, to default url:" + next.url + ", user id:" + model.userId);
			}
		}else {
			log.debug("Not found shop id for CPC short url:" + model.shortKey);
			next.isOK = false;
			next.url = this.getUserClickNoEnableUrl(model.userId, model.outId, model.platform);
		}
		return next;
	}
	
	private String resolveVariable(String str, Map<String, String> param){
    	for(Entry<String, String> entry: param.entrySet()){
    		str = str.replaceAll("\\$\\{" + entry.getKey() + "\\}", entry.getValue());
    	}
    	
    	return str;
	}
	
	private void writeClickLogWithApi(ShortUrlModel model){
		DateFormat timeFormate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");		
		String t = timeFormate.format(new Date(System.currentTimeMillis()));
		String msg = String.format("%s %s %s %s [%s] %s", t, model.shortKey, model.uid, model.ip,
				model.agent, model.refer);
		//vm.write(msg);
		if(accesslog != null){
			accesslog.debug(msg);
		}else {
			log.debug(msg);
		}
		
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("short_key", model.shortKey);
		param.put("uid", model.uid);
		param.put("ip_addr", model.ip);
		param.put("agent", model.agent);
		param.put("refer", model.refer);
		
		HTTPResult r = api.call("tool_short_url_click", param);
		if(!r.isOK){
			log.warn("click log write error, short:" + model.shortKey + ", msg:" + r.errorMsg);
		}

	}
	
	/**
	 * 
	 * @param msg
	 */
	public void writeLog(String msg){
		vm.write(msg);
		if(accesslog != null){
			accesslog.debug(msg);
		}else {
			log.debug(msg);
		}
	}
	
	public long newUserId(){
		nextUID = (nextUID + 1) % MODE;
		
		return (System.currentTimeMillis() % (MODE * 1000)) * MODE + nextUID; 
	}
	
	public Map<String, Object> cacheStat(){
		return cache.stat();
	}
	
	public void doAction(String action, ShortUrlModel model, HttpServletRequest req, HttpServletResponse response){
		
	}
	
	/**
	 * 点击商家商品，如果余额不足或者没有找到商家信息。返回false。 表示点击失效，转到淘客
	 * 默认的连接去。
	 * @param shopId
	 * @param numIid
	 * @return
	 */
	private boolean hitShopClick(long shopId, long numIid){
		ShopAccount account = taobao.getShopAccount(shopId);
		ShopItem item = taobao.getShopItem(shopId, numIid);
		if(account != null && item != null){
			if(account.banlance >= item.price){
				float old = account.banlance;
				account.banlance -= item.price;
				log.info(String.format("shop click, shop:%1s old:%2$1.2f, new:%3$1.2f, price:%4$1.2f", account.shopId, old, account.banlance, item.price));
			}
		}
		
		boolean isOk = account != null && account.banlance > 0;
		Benchmark m = Benchmark.start(Benchmark.CPC_CLICK_OK);
		m.attachObject(account);
		if(isOk && item.isOnSale){
			m.done(Benchmark.CPC_CLICK_OK, 0);
		}else if(!item.isOnSale){	//商品下架
			isOk = false;
			m = Benchmark.start(Benchmark.CPC_ITEM_ERROR);
			m.attachObject(item);
			m.done();
		}else {					//店铺未找到，或余额不足。
			m.done(Benchmark.CPC_CLICK_FAILED, 0);
		}
		
		return isOk;
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
										if(f != null && f.startsWith("http:")){
											cpcCache.set(ac, f, 5 * 60);
										}else {
											cpcCache.set(ac, "/", 5 * 60);											
										}
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
							ac.wait(1000 * 2);
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
	
	public void export(String field, String value, String start, String end, OutputStream os){
		DataExporter.export(field, value, start, end, api, os);
	}
	
	public boolean checkSecretParam(String field, String value, String secret, String time){
		long curTime = System.currentTimeMillis();
		long nTime = 0;
		try{
			nTime = Long.parseLong(time);
		}catch(Exception e){}		
		long diff = Math.abs(curTime - nTime * 1000);
		
		if(diff > 1000 * 60 * 10){
			log.debug("check export param, timestamp diff so far:" + diff);
			return false;
		}		
		String appKey = Settings.getString(Settings.TAODIAN_APPID, null); // System.getProperty("");
		String appSecret = Settings.getString(Settings.TAODIAN_APPSECRET, null);
		String key = field + value +  time + appKey + appSecret;
		
		String pSecret = TaodianApi.MD5(key);
		if(secret != null && pSecret != null && pSecret.equals(secret)){
			return true;
		}

		log.debug("check export param, secret error, key:" + key + ", param secret:" + secret + ", p:" + pSecret);
		
		return false;
	}
	
}
