package com.taodian.emop.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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

import com.taodian.click.ShortUrlModel;
import com.taodian.click.ShortUrlService;
import com.taodian.click.monitor.Benchmark;

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
		String key = this.getUrlKey(req);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("utf8");
		response.setHeader("Cache-Control", "no-cache");
		
		if(key != null && key.length() > 4){
			Benchmark mark = Benchmark.start(Benchmark.SHORT_KEY_GET);
			String nocache = req.getParameter("no_cache");

			ShortUrlModel model = service.getShortUrlInfo(key, nocache != null && nocache.trim().toLowerCase().equals("y"));
			
			/**
			 * 做一个clone，因为记录点击状态的时候。需要加入本次的点击信息。多个点击之间会出现冲突。
			 */
			if(model != null){
				model = model.copy();
				trackClickInfo(model, req);

				Map<String, String> param = createSubmitForm(model, req);
				outputSubmitForm(param, response);
				
				mark.attachObject(model);
				mark.done();
			}else {
				model = new ShortUrlModel();
				model.shortKey = key;
				trackClickInfo(model, req);
				
				mark.attachObject(key);
				mark.done(Benchmark.SHORT_KEY_NOT_FOUND, 0);
				
				response.setContentType("text/plain");
				response.getWriter().println("短网址转换失败:" + key);				
			}
		}else {
			response.setContentType("text/plain");
			response.getWriter().println("欢迎使用冒泡短网址系统。");
		}
	}
	
	protected void doPost(HttpServletRequest req, HttpServletResponse response) throws IOException{
		String key = this.getUrlKey(req);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("utf8");
		
		if(key != null && key.length() > 4){
			Benchmark mark = Benchmark.start(Benchmark.SHORT_KEY_POST);
			ShortUrlModel model = service.getShortUrlInfo(key, false);
			if(model != null){
				mark.attachObject(model);
				NextURL n = postShortCheck(model, req);
				if(n.isOK){
					mark.done();
				}else {
					mark.done(Benchmark.SHORT_KEY_POST_CHECK_ERROR, 0);
				}
				
				//设置302 响应头。
				response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

				String url = response.encodeRedirectURL(n.url);
				response.sendRedirect(url);	
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
		//response.encodeRedirectUrl(arg0)
	}
	
	//protected void 

	protected void trackClickInfo(ShortUrlModel m, HttpServletRequest req){
		
	}
	
	protected NextURL postShortCheck(ShortUrlModel model, HttpServletRequest req){
		return null;
	}
	
	/**
	 */
	protected Map<String, String> createSubmitForm(ShortUrlModel model, HttpServletRequest req){
		Map<String, String> p = new HashMap<String, String>();
		
		p.put("debug", req.getParameter("debug"));
		
		return p;
	}
	
	/**
	 * 输出二次提交用到的表单。
	 * @throws IOException 
	 */
	protected void outputSubmitForm(Map<String, String> param, HttpServletResponse response) throws IOException{
		String t = templates;
    	
    	for(Entry<String, String> entry: param.entrySet()){
    		t  = t .replaceAll("\\$\\{" + entry.getKey() + "\\}", entry.getValue());
    	}
		
    	String d = param.get("debug");
    	if(d != null && d.equals("true")){
    		response.setContentType("text/plain");
    	}else {
    		response.setContentType("text/html");    		
    	}
    	response.getWriter().print(t);
	}
	
	protected boolean isMobile(){
		return false;
	}
	
	protected void writeClickLog(){
		
	}
	
	private String getUrlKey(HttpServletRequest req){
        Pattern pa = Pattern.compile("(c|t|)/([^\\.]+)");
        Matcher ma = pa.matcher(req.getRequestURI());
        if(ma.find()){
        	String key = ma.group(2);
			try {
				key = URLDecoder.decode(key, "UTF8");
			} catch (UnsupportedEncodingException e) {
			}
        	return key;
        }
		
		return null;
	}
	
	class NextURL{
		public String url = null;
		public boolean isOK = false;
	}
	
}
