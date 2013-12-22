package com.taodian.click.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 开放接口，统一返回的结构。在HTTP 接口里面，才能统一的生成返回结果。主要包含接口的返回值，接口调用状态，
 * 错误码。
 * 
 * @author deonwu
 *
 */
public class Result implements Serializable{
	private static final long serialVersionUID = 1L;
	
	/**
	 * 开放接口，返回状态。成功统一为:ok, 其他状态没有明确定义，可以根据各个接口自行定义。 
	 */
	public String status = null;
	/**
	 * 开放接口，出错时对应的错误码。有每个接口自行定义。
	 */
	public String code = null;
	/**
	 * 开放接口，出错时对应的错误信息。有每个接口自行定义。
	 */
	public String msg = null;
	
	/**
	 * 接口的返回结果，需要一个可以JSON序列化的对象。主要是Map或List两种结构之一。
	 */
	public Object result = null;	
	
	public boolean isOk(){
		return status != null && status.trim().equals("ok");
	}
	
	/**
	 * 创建一个空的返回Result对象，把status 设置为ok.
	 * 
	 * @return
	 */
	public static Result emptyOk(){
		Result r = new Result();
		r.status = "ok";
		r.code = "";
		r.msg = "";
		r.result = new HashMap<String, Object>();
		
		return r;
	}
	
	/**
	 * 设置接口的错误码。
	 * @param code
	 */
	public void error(String code){
		error(code, "");
	}
	
	/**
	 * 设置接口的错误码和错误消息。 并且把status 设置为"err".
	 * @param code
	 */
	public void error(String code, String msg){
		this.code = code;
		this.status = "err";
		this.msg = msg;
	}
	
	/**
	 * 设置一个返回结果。
	 * @param key
	 * @param obj
	 */
	public void putValue(String key, Object obj){
		if(result instanceof Map){
			((Map<String, Object>)result).put(key, obj);
		}
	}
}
