package com.taodian.emop.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	
	public void init(ServletConfig config){
		service = ShortUrlService.getInstance();
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
			ShortUrlModel model = service.getShortUrlInfo(key);
			if(model != null){
				mark.attachObject(model);
				Map<String, String> param = createSubmitForm(model, req);
				outputSubmitForm(param, response);
				
				mark.done();
			}else {
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
			ShortUrlModel model = service.getShortUrlInfo(key);
			if(model != null){
				mark.attachObject(model);
				//String nextUrl = 
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

	protected NextURL postShortCheck(ShortUrlModel model, HttpServletRequest req){
		return null;
	}
	
	/**
	 */
	protected Map<String, String> createSubmitForm(ShortUrlModel model, HttpServletRequest response){
		return null;
	}
	
	/**
	 * 输出二次提交用到的表单。
	 */
	protected void outputSubmitForm(Map<String, String> param, HttpServletResponse response){
		
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
