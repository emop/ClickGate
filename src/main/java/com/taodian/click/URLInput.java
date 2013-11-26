package com.taodian.click;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 短网址输入信息，根据这个输入转换对应的淘客链接。
 * 
 * @author deonwu
 */
public class URLInput  implements Serializable{
	public static final String INPUT_SHORT_URL = "short";
	public static final String INPUT_TAOKE_ONLINE = "taoke";
	
	public String type = "short";
	public String shortKey = "";
	public String userName = "";
	public String info = "";
	
	
	public String getURI(){
		if(type.equals(INPUT_SHORT_URL)){
			return "c/" + shortKey;
		}else if(type.equals(INPUT_TAOKE_ONLINE)){
			try {
				return String.format("t/%s/%s", userName, URLEncoder.encode(info, "UTF8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return String.format("t/%s/%s", userName, info);
			}
		}
		return "";
	}
	
	public String toString(){
		return getURI();
	}
}
