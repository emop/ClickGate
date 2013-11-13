package com.taodian.click;

import com.taodian.api.TaodianApi;
import com.taodian.emop.utils.CacheApi;
import com.taodian.emop.utils.SimpleCacheApi;

/**
 * 短网址的服务类。
 * 
 * @author deonwu
 */
public class ShortUrlService {

	private TaodianApi api = null;
	private CacheApi cache = new SimpleCacheApi();
	private static ShortUrlService ins = null;
	
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
		
	}
	
	public ShortUrlModel getShortUrlInfo(String shortKey){
		return null;
	}
	

}
