package com.taodian.click;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taodian.api.TaodianApi;
import com.taodian.emop.Settings;
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

	private TaodianApi api = null;
	private CacheApi cache = new SimpleCacheApi();
	private static ShortUrlService ins = null;
	
	private long nextUID = 0;
	private int MODE = 10000;
	
	public static synchronized ShortUrlService getInstance(){
		if(ins == null){
			ins = new ShortUrlService();
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
		
		if(appKey != null && appSecret != null){
			api = new TaodianApi(appKey, appSecret, appRoute);		
		}else {
			log.info("The taodian.api_id and taodian.api_secret Java properties are required.");
			System.exit(255);
		}
	}
	
	public ShortUrlModel getShortUrlInfo(String shortKey, boolean noCache){
		Object tmp = cache.get(shortKey, true);
		
		if(tmp == null || noCache){
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("short_key", shortKey);
			param.put("auto_mobile", "y");
			
			HTTPResult r = api.call("tool_convert_long_url", param);
			
			ShortUrlModel m = new ShortUrlModel();
			if(r.isOK){
				m.shortKey = shortKey;
				m.longUrl = r.getString("data.long_url");
				m.mobileLongUrl = r.getString("data.mobile_long_url");
				cache.set(shortKey, m, 5 * 60);
				
				tmp = m;
			}
		}
		
		if(tmp != null && tmp instanceof ShortUrlModel){
			return (ShortUrlModel)tmp;
		}
		return null;
	}
	
	public void writeClickLog(ShortUrlModel model){
		
	}
	
	public long newUserId(){
		nextUID = (nextUID + 1) % MODE;
		
		return (System.currentTimeMillis() % (MODE * 1000)) * MODE + nextUID; 
	}
	

}
