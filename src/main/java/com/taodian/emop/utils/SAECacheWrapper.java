package com.taodian.emop.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sina.sae.memcached.SaeMemcache;
import com.sina.sae.util.SaeUserInfo;

public class SAECacheWrapper implements CacheApi {
	private SaeMemcache mc = null;	
	private String cachePrefix = "";
	
	public 	SAECacheWrapper(){	
		cachePrefix = "v" + SaeUserInfo.getAppVersion();
		mc = new SaeMemcache("127.0.0.1", 11211);
		mc.init();
	}
	
	@Override
	public void set(String key, Object data, int expired) {
		mc.init();
		mc.set(cachePrefix + key, data, expired);
	}

	@Override
	public Object get(String key) {
		mc.init();			
		return mc.get(cachePrefix + key);
	}

	@Override
	public Object get(String key, boolean update) {
		return get(key);
	}

	@Override
	public boolean remove(String key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean cleanAll() {
		mc.init();
		return mc.flushAll();
	}

	@Override
	public Map<String, Object> stat() {
		return new HashMap<String, Object>();
	}

	@Override
	public List<String> keys() {
		return new ArrayList<String>();
	}

	@Override
	public boolean add(String key, Object data, int expired) {
		mc.init();
		return mc.add(cachePrefix + key, data, expired);
	}


}
