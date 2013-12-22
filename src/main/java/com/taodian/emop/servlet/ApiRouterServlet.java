package com.taodian.emop.servlet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONValue;

import com.taodian.api.TaodianApi;
import com.taodian.click.ShortUrlService;
import com.taodian.click.api.GateApi;
import com.taodian.click.api.Result;
import com.taodian.emop.Settings;

public class ApiRouterServlet extends HttpServlet {
	protected ShortUrlService service = null;
	protected Log log = LogFactory.getLog("click.gate.api");
	protected GateApi api = null;

	public void init(ServletConfig config){
		service = ShortUrlService.getInstance();		
		api = new GateApi();
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
    	doPost(request, response);
    }
    
    protected void doPost(HttpServletRequest req, HttpServletResponse response)
	throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("utf8");
	
		String tmpId = req.getParameter("app_id");
		String name = req.getParameter("name");
		String params = req.getParameter("params");
		String sign = req.getParameter("sign");
		String time = req.getParameter("time");
		long start = System.currentTimeMillis();
		Map<String, Object> obj = new HashMap<String, Object>();
		Result result = null;
		
		response.setContentType("application/json");
		obj.put("status", "err");
		try{
			int appId = 0;
			if(tmpId != null && tmpId.length() > 0){
				appId = Integer.parseInt(tmpId);
			}
			if(appId > 0){
				result = invoke(appId, name, params, sign, time);
				if(result != null){
					if(result.status != null && result.status.length() > 0){
						obj.put("status", result.status);
					}
					if(result.msg != null && result.msg.length() > 0){
						obj.put("msg", result.msg);
					}
					if(result.code != null && result.code.length() > 0){
						obj.put("code", result.code);
					}
					if(result.result != null){
						obj.put("data", result.result);
					}
				}else {
					obj.put("msg", "Api error:result is null");					
				}
			}else {
				obj.put("msg", "app_id is required");
			}
		}catch(Throwable e){
			String msg = e.getMessage();
			if(msg != null && msg.length() > 100){
				msg = msg.substring(0, 100).split("\n")[0];
			}
			
			obj.put("msg", "Api exception, " + msg);
			log.error(e.toString(), e);
		}finally{
			long elapse = System.currentTimeMillis() - start;
			obj.put("elapse", elapse / 1000.0f);
		}
		
		try {
			JSONValue.writeJSONString(obj, response.getWriter());
		} catch (IOException e) {
			log.error(e.toString(), e);
		}
			
				
    }
    
    public Result invoke(int appId, String name, String param, String sign, String time){
    	Result r = Result.emptyOk();
    	
    	String key = appId + "," + time + "," + Settings.getString(Settings.TAODIAN_APPSECRET, "");
    	String curSign = TaodianApi.MD5(key);
    	if(curSign.equals(sign)){
    		Map<String, Object> params = null;
    		if(param != null && param.trim().startsWith("{")){
    			params = (Map<String, Object>)JSONValue.parse(param);
    		}else {
    			params = new HashMap<String, Object>();
    		}
    		try {
				Method m = api.getClass().getMethod(name, Map.class);
				Object o = m.invoke(api, new Object[]{params});
				if(o != null){
					r = (Result)o;
				}
			} catch (Exception e){
				r.error("api_err", e.toString());
			}
    	}else {
    		r.error("sign_err", "cur:" + curSign + "!=" + sign);
    	}
    	
    	return r;
    }

}
