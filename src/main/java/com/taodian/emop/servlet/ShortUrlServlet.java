package com.taodian.emop.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taodian.api.TaodianApi;
import com.taodian.click.ShortUrlModel;
import com.taodian.click.ShortUrlService;
import com.taodian.click.URLInput;
import com.taodian.click.monitor.Benchmark;
import com.taodian.emop.Settings;
import com.taodian.route.Action;
import com.taodian.route.TargetURL;

/**
 * 短网址跳转服务。利用TaodianAPI，把短地址转换为长连接。
 * 
 * 1. 支持防爬虫跟踪机制。
 * 2. 支持点击统计。
 * 
 * @author deonwu
 */
public class ShortUrlServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	protected ShortUrlService service = null;
	protected String templates = "";
	protected Log log = LogFactory.getLog("click.gate.servlet");
	
	protected String secret = "不要问这个用来干什么，这是一个秘密。";
	
	public void init(ServletConfig config){
		service = ShortUrlService.getInstance();
		
		byte[] buffer = new byte[64 * 1024];
		InputStream ins = this.getClass().getClassLoader().getResourceAsStream("short_url_form_template.html");		
		try{
			int len = ins.read(buffer);
			
			templates = new String(buffer, 0, len, "UTF-8");
		}catch(Exception e){
			log.error(e.toString(), e);
		}
	}
	
	/**
	 * 网址的GET请求处理，生成2次提交表单返回。
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException{
		URLInput key = this.getUrlKey(req);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("utf8");
		response.setHeader("Cache-Control", "no-cache");
		
		//log.debug("-------------------key:" + key);
		
		if(key != null){
			Benchmark mark = Benchmark.start(Benchmark.SHORT_KEY_GET);
			String nocache = req.getParameter("no_cache");

			ShortUrlModel model = service.getTaobaokeUrlInfo(key, nocache != null && nocache.trim().toLowerCase().equals("y"));
			
			/**
			 * 做一个clone，因为记录点击状态的时候。需要加入本次的点击信息。多个点击之间会出现冲突。
			 */
			if(model != null && model.longUrl != null && model.longUrl.length() > 5){
				model = model.copy();
				trackClickInfo(model, req, response);
				if(key.action == null || key.action.trim().length() == 0){
					Map<String, String> param = createSubmitForm(key, model, req);
					outputSubmitForm(param, response);
				}else {
					service.doAction(key.action, model, req, response);
				}
				
				mark.attachObject(model);
				mark.done();
			}else {
				model = new ShortUrlModel();
				model.shortKey = key.getURI();
				trackClickInfo(model, req, response);
				
				mark.attachObject(model);
				mark.done(Benchmark.SHORT_KEY_NOT_FOUND, 0);
				
				response.setContentType("text/plain");
				response.getWriter().println("淘客地址转换失败:" + key.getURI());				
			}
		}else {
			response.setContentType("text/plain");
			response.getWriter().println("欢迎使用冒泡短网址系统。");
		}
	}
	
	protected void doPost(HttpServletRequest req, HttpServletResponse response) throws IOException{
		URLInput key = this.getUrlKey(req);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("utf8");
		
		if(key != null){
			Benchmark mark = Benchmark.start(Benchmark.SHORT_KEY_POST);
			ShortUrlModel model = service.getTaobaokeUrlInfo(key, false);
			if(model != null){
				model = model.copy();
				trackClickInfo(model, req, response);
				
				mark.attachObject(model);
				
				if(key.action == null || key.action.trim().length() == 0){
					TargetURL n = postShortCheck(model, req);
					
					if(n.isOK && n.actionName.equals(Action.FORWARD)){
						service.writeClickLog(model);						
					}else {
						if(log.isDebugEnabled()){
							log.debug(String.format("ignore write click log, status:%s, action:%s, url:%s", n.isOK, n.actionName, n.url));
						}
					}
					
					if(n.actionName.equals(Action.REJECT)){
						mark.done(Benchmark.SHORT_KEY_POST_CHECK_ERROR, 0);

						response.setContentType("text/plain");
						response.getWriter().println("短网址拒绝访问:" + key);	
					}else {
						mark.done();					

						//设置302 响应头。
						response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

						String url = response.encodeRedirectURL(n.url);
						response.sendRedirect(url);						
					}
					
				}else {
					service.doAction(key.action, model, req, response);
				}				
			}else {
				mark.attachObject(key);
				mark.done(Benchmark.SHORT_KEY_POST_NOTFOUND, 0);
				response.setContentType("text/plain");
				response.getWriter().println("短网址转换失败:" + key);				
			}
		}else {
			response.setContentType("text/plain");
			response.getWriter().println("欢迎使用冒泡短网址系统。");
		}
	}
	
	/**
	 * 更新用户信息：
	 * 1. User_agent
	 * 2. Referer 
	 * 3. IP
	 * 4. UID
	 */
	protected void trackClickInfo(ShortUrlModel m, HttpServletRequest req, HttpServletResponse response){
		/*
		Enumeration e = req.getHeaderNames();
		for(;e.hasMoreElements(); ){
			String n = e.nextElement() + "";
			log.debug(n + " -->" + req.getHeader(n));
		}
		*/
		
		m.isMobile = isMobile(req);
		m.agent = req.getHeader("User-Agent");
		m.refer = req.getHeader("Referer");
		m.ip = req.getHeader("HTTP_X_REAL_IP");
		
		m.agent = m.agent == null ? "" : m.agent;
		m.refer = m.refer == null ? "" : m.refer;
		//m.uid = null;
		m.uid = service.sm.getSessionUserId(req, response);
		
		if(m.ip == null){
			m.ip = req.getHeader("HTTP_X_FORWARDED_FOR");
			if(m.ip != null && m.ip.indexOf(',') > 0){
				m.ip = m.ip.split(",")[0];
			}
		}
		if(m.ip == null){
			m.ip = req.getHeader("REMOTE_ADDR");
		}
		if(m.ip == null){
			m.ip = req.getHeader("HTTP_CLIENT_IP");			
		}
		if(m.ip == null){
			m.ip = req.getRemoteHost();
		}

	}
	
	protected TargetURL postShortCheck(ShortUrlModel model, HttpServletRequest req){
		TargetURL next = new TargetURL();
		next.isOK = false;
		next.actionName = Action.REJECT;
		
		long c = 0;
		String clickTime = req.getParameter("click_time") + "";
		if(clickTime != null){
			try{
				c = Long.parseLong(clickTime);
			}catch(Exception e){}
		}
		c = System.currentTimeMillis() - c;
		
		if(c > 0 && c < 120 * 1000){
			String ref = secret + model.shortKey + "," + model.agent + "," +  clickTime;
			String hash = TaodianApi.MD5(ref);
			
			String code = req.getParameter("check_code");
			
			if(code != null && hash != null && hash.equals(code)){
				next.isOK = true;
				next.actionName = Action.FORWARD;
				model.refer = req.getParameter("refer");
			}else {
				log.warn(String.format("hash:%s != %s, ref:%s, ip:%s, uid:%s, user id:%s", code, hash, ref, 
						model.ip, model.uid, model.userId));
			}
		}else {
			log.warn(String.format("the second submit time is delay so far, ms:" + c + ", ip:" + model.ip 
					+ " uid:" + model.uid + ", user id:" + model.userId));			
		}
		
		next.url = "/";
		model.isMobile = isMobile(req);
		
		if(next.isOK && service.router != null) {
			next = service.router.route(next, model, req);
		}
		
		if(next.isOK && (next.actionName.equals(Action.FORWARD) || next.actionName.equals(Action.IGNORE))){
			if(model.shortKeySource != null && model.shortKeySource.equals("cpc")) {
				next = service.cpcServiceCheck(model, next);
			}else {
				if(model.isMobile && model.mobileLongUrl != null && model.mobileLongUrl.startsWith("http://")){
					next.url = model.mobileLongUrl;
				}else if(model.longUrl != null){
					next.url = model.longUrl;
				}
			}
		
			//修复有些连接里面不是正确的HTTP。
			if(next.url == null || !next.url.startsWith("http:")){
				next.isOK = false;
				int fix = next.url.indexOf("http:");
				//log.info("url:" + next.url + ", fix:" + fix);
				if(fix > 0){
					next.url = next.url.substring(fix);
				}
			}
		}
		
		DateFormat timeFormate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");		
		String t = timeFormate.format(new Date(System.currentTimeMillis()));
		String msg = String.format("$%s$%s$%s$%s$%s$%s$%s", t, model.shortKey, next.actionName,
				model.uid, model.ip, model.agent, model.refer);
		service.vm.write(msg);
		
		return next;
	}
	
	/**
	 */
	protected Map<String, String> createSubmitForm(URLInput key, ShortUrlModel model, HttpServletRequest req){
		Map<String, String> p = new HashMap<String, String>();
		
		String clickTime = System.currentTimeMillis() + "";
		
		p.put("debug", req.getParameter("debug"));
		p.put("click_time", clickTime);
		p.put("user_id", model.uid);
		p.put("refer", model.refer);
		//p.put("short_key", model.shortKey);
		if(service.sm != null){
			p.put("cid", service.sm.getSessionId(req));
		}
		p.put("uri", key.getURI());
		p.put("auto_mobile", req.getParameter("auto_mobile"));	
		p.put("source_domain", Settings.getString(Settings.TAOKE_SOURCE_DOMAIN, "wap.emop.cn"));
		
		String ref = secret + model.shortKey + "," + model.agent + "," + clickTime;
		
		//log.debug(String.format("hash ref:%s", ref));

	 	String hash = TaodianApi.MD5(ref);

	 	int i = (int)(Math.random() * 32) % 32;
	 	String header = hash.substring(0, i);
	 	String tail = hash.substring(i);
	 	
		p.put("index_key", (i * 32 + i) + "");
		p.put("code", header + hash + tail);
	 	
		return p;
	}
	
	/**
	 * 输出二次提交用到的表单。
	 * @throws IOException 
	 */
	protected void outputSubmitForm(Map<String, String> param, HttpServletResponse response) throws IOException{
		String t = templates;
    	
    	for(Entry<String, String> entry: param.entrySet()){
    		if(log.isDebugEnabled()){
    		//	log.info("replace:" + entry.getKey() + "-->" + entry.getValue());
    		}
    		t  = t .replaceAll("\\$\\{" + entry.getKey() + "\\}", entry.getValue() + "");
    	}
		
    	String d = param.get("debug");
    	if(d != null && d.equals("true")){
    		response.setContentType("text/plain");
    	}else {
    		response.setContentType("text/html");    		
    	}
    	response.getWriter().print(t);
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
	
	private URLInput getUrlKey(HttpServletRequest req){
        Pattern pa = Pattern.compile("(c|t)/([a-zA-Z0-9]+)(/([%a-zA-Z0-9_]+))?");
        Matcher ma = pa.matcher(req.getRequestURI());
        
        URLInput url = null;
        if(ma.find()){
        	url = new URLInput();
        	String type = ma.group(1);
        	if(type.equals("c")){
        		url.type = URLInput.INPUT_SHORT_URL;
				url.shortKey = ma.group(2);
				url.action = ma.group(4);
        	}else if(type.equals("t") && ma.group(4) != null){
        		url.type = URLInput.INPUT_TAOKE_ONLINE;
				try {
					url.userName = ma.group(2);
					url.info = URLDecoder.decode(ma.group(4), "UTF8");
				} catch (UnsupportedEncodingException e) {
				}
        	}else {
        		return null;
        	}
        	return url;
        }
		
		return null;
	}
	

	
}
