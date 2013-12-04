package com.taodian.click.monitor;

public class Benchmark {
	public static final String SHORT_KEY_GET = "short_key_get";
	public static final String SHORT_KEY_POST = "short_key_post";

	public static final String SHORT_KEY_POST_CHECK_ERROR = "short_key_post_error";
	
	public static final String SHORT_KEY_POST_NOTFOUND = "short_key_post_not_found";

	public static final String SHORT_KEY_NOT_FOUND = "short_key_not_found";

	public static final String SHORT_KEY_get_cache = "short_key_get_cache";

	public static final String CPC_CLICK_OK = "cpc_click_ok";
	public static final String CPC_CLICK_FAILED = "cpc_click_erro";
	public static final String CPC_ITEM_ERROR = "cpc_click_err";

	
	public long start = 0;
	public long end = 0;
	public long elapsed = 0;
	public String type = "";
	public Object obj = null;

	private Benchmark(String type){
		this.type = type;
	}
	
	public static Benchmark start(String type, Object obj){
		Benchmark mark = new Benchmark(type);
		mark.start = System.currentTimeMillis();
		mark.obj = obj;
		
		return mark;
	}

	public static Benchmark start(String type){
		return start(type, null);
	}
	
	
	public void attachObject(Object obj){
		this.obj = obj;
	}
	
	public void done(){
		end = System.currentTimeMillis();
		elapsed = end - start;
		StatusMonitor.hit(this);
	}
	
	public void done(long value){
		elapsed = value;
		StatusMonitor.hit(this);
	}
	
	public void done(String type, long value){
		this.type = type;
		if(value > 0){
			elapsed = value;
		}else {
			end = System.currentTimeMillis();
			elapsed = end - start;			
		}
		
		StatusMonitor.hit(this);
	}	
	
	public Benchmark copy(){
		Benchmark m = new Benchmark(type);
		m.type = type;
		m.start = start;
		m.end = end;
		m.obj = obj;
		
		return m;
	}
}
