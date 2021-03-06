package com.taodian.click.session;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.taodian.api.TaodianApi;
import com.taodian.click.SessionManager;
import com.taodian.click.ShortUrlService;
import com.taodian.emop.Settings;
import com.taodian.emop.utils.CacheApi;
import com.taodian.emop.utils.SimpleCacheApi;

public class HTML5RedisSessionManager implements SessionManager {
	protected Log log = LogFactory.getLog("click.session");

	public static final int DS_COMMON_DATA = 1;

	protected static final String EMOP_COOKIES = "emop_click_uid";
	protected CacheApi newUser = new SimpleCacheApi();
	protected ShortUrlService service = null;
	
	public HTML5RedisSessionManager(ShortUrlService s){
		this.service = s;
	}
	
	public boolean checkPingRedies(){
		Jedis j = this.getJedis();
		if(j != null){
			try{
				return j.ping().equals("PONG");
			}finally{
				releaseConn(j);
			}
		}
		return false;
	}
	
	@Override
	public String getSessionUserId(HttpServletRequest req,
			HttpServletResponse response) {
		
		String uid = null;
		String cid = req.getParameter("cid");
		if(cid != null && cid.length() > 10){
			uid = convertUIDFromCid(cid);
			log.debug(String.format("cid(%s) -> uid(%s)", cid, uid));
			if(uid == null){
				log.warn("not found cid data in redis, cid:" + cid);
			}
		}
		
		if(uid == null){
			uid = req.getParameter("user_id");
			if(uid != null){
				saveNewUid(req, cid, uid);
			}
		}
		
		if(uid == null){
			if(req.getCookies() != null){
				for(Cookie c: req.getCookies()){
					if(c.getName().equals(EMOP_COOKIES)){
						uid = c.getValue();
					}
				}
			}
		}else {
			Cookie c = new Cookie(EMOP_COOKIES, uid);
			c.setPath("/");
			c.setMaxAge(10 * 365 * 24 * 60 * 60);
			response.addCookie(c);
		}
		
		if(uid != null && req.getMethod().equals("POST")){
			Object o = newUser.get(uid);
			if(o != null){
				service.writeLog(o + "");
				newUser.remove(uid);
			}
		}
		
		if(uid == null){
			uid = service.newUserId() + "";
		}
		
		return uid;
	}
	
	private String convertUIDFromCid(String cid){
		String ck = "cid_" + cid;
		String uid = null;
		
		Object o = newUser.get(ck, true);
		if(o == null){
			Jedis j = getJedis();
			if(j != null){
				try{
					o = j.lindex(ck, 0);
				}finally{
					releaseConn(j);
				}
			}
			if(o != null){
				uid = o.toString();
				newUser.set(ck, o, 60);
			}
		}else {
			uid = o + "";
		}
		
		return uid;
	}
	
	private void saveNewUid(HttpServletRequest req, String cid, String uid){
		String ck = "cid_" + cid;
		
		newUser.set(ck, uid, 60 * 5);
		Jedis j = getJedis();
		if(j != null){
			try{
				j.rpush(ck, uid, "false", getRealIP(req), req.getRemoteHost(), 
						req.getHeader("User-Agent"), "old");
				j.expire(ck, 60 * 60 * 24 * 30);
			}finally{
				releaseConn(j);
			}
		}
	}

	public Jedis getJedis(){
		return service.getJedis();
	}
	
	public void releaseConn(Jedis jedis){
		service.returnJedis(jedis);
	}	
	
	@Override
	public String getSessionId(HttpServletRequest req) {
		String ip = getRealIP(req) + "";
		String agent = req.getHeader("User-Agent") + "";
		String cid =  TaodianApi.MD5(ip + agent); // "";
		
		String ck = "cid_" + cid;

		if(newUser.get(ck, true) == null){
			Jedis j = getJedis();
			try{
				if(j == null || !j.exists(ck)){
					DateFormat timeFormate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String time = timeFormate.format(new Date(System.currentTimeMillis()));
					
					/**
					 * 如果设置之前由cookies的uid，直接绑定到老的uid上面。
					 */
					String uid = service.newUserId() + "";
					if(req.getCookies() != null){
						for(Cookie c: req.getCookies()){
							if(c.getName().equals(EMOP_COOKIES)){
								uid = c.getValue() + "";
							}
						}
					}
					String host = req.getServerName() + "";
					String isMobile = isMobile(req) + "";			
					
					String newUser = "new_uid:%s,mobile:%s,ip:%s,host:%s,agent:[%s],created:%s";
					newUser = String.format(newUser, uid, isMobile, ip, host, agent, time);					
					log.debug("new cid:" + ck + ", info:" + newUser);
					if(j != null){
						j.rpush(ck, uid, isMobile, ip, host, agent, time);
						j.expire(ck, 60 * 60 * 24 * 30);
					}
					this.newUser.set(ck, uid, 60);
				}
			}catch(Exception e){
				log.warn("Redis create session error:" + e.toString(), e);
			}finally{
				releaseConn(j);
			}
		}
		
		return cid;
	}
	
	public String getRealIP(HttpServletRequest req){
		String ip = req.getHeader("HTTP_X_REAL_IP");;
		if(ip == null){
			ip = req.getHeader("HTTP_X_FORWARDED_FOR");
			if(ip != null && ip.indexOf(',') > 0){
				ip = ip.split(",")[0];
			}
		}
		if(ip == null){
			ip = req.getHeader("REMOTE_ADDR");
		}
		if(ip == null){
			ip = req.getHeader("HTTP_CLIENT_IP");			
		}
		if(ip == null){
			ip = req.getRemoteHost();
		}
		return ip;
	}

	protected boolean isMobile(HttpServletRequest req){
		String agent = req.getHeader("User-Agent");
		
		if(agent == null) return false;
		
        Pattern pa = Pattern.compile("(android|ios|ipad|iphone)", Pattern.CASE_INSENSITIVE);
        Matcher ma = pa.matcher(agent);
        if(ma.find()){
        	return true;
        }
        String q = req.getParameter("mobile");
        if(q != null && q.equals("y")){
        	return true;
        }
        
		return false;
	}	


}
