package com.taodian.emop.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

import com.taodian.api.TaodianApi;
import com.taodian.click.ClickVisitorChannel;
import com.taodian.click.ShortUrlService;
import com.taodian.emop.Settings;



public class DumpLogServlet extends HttpServlet {
	protected Log log = LogFactory.getLog("click.gate.servlet");
	protected ShortUrlService service = null;
	
	public void init(ServletConfig config){
		service = ShortUrlService.getInstance();		
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
    	doPost(request, response);
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("utf8");
		response.setContentType("text/plain");
	
		String cid = request.getParameter("cid");
		String token = request.getParameter("token");
		if(cid == null || token == null){
			response.getWriter().println("需要cid和token参数查看日志。");
		}else {
			String appid = Settings.getString(Settings.TAODIAN_APPID, "0");
			String appkey = Settings.getString(Settings.TAODIAN_APPSECRET, "");
			String checkKey = appid + "," + cid + "," + appkey;
			String checkSign = TaodianApi.MD5(checkKey);
			response.setHeader("Transfer-Encoding", "chunked");
			if(checkSign.toLowerCase().equals(token.toLowerCase())){
				PrintWriter r = response.getWriter();
				r.println("CONNECTED");
				r.flush();

				Continuation c = ContinuationSupport.getContinuation(request, null);
				service.vm.register(cid, new ClickVisitorChannel(c, r));
				
				log.info("suspend '" + cid + "' connection, writer:" + r);
				//响应头等30分钟写日志。
				c.suspend(ClickVisitorChannel.TIME_OUT);
				//log.info("new client '" + cid + "' add ok !");
			}else {
				response.getWriter().println("token签名错误。");				
			}
		}
    }
    
}
