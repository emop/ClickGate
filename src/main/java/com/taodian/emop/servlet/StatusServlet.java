package com.taodian.emop.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.taodian.click.ShortUrlService;
import com.taodian.click.monitor.StatusMonitor;
import com.taodian.emop.Settings;



public class StatusServlet extends HttpServlet {
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
    	doPost(request, response);
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("utf8");
		response.setContentType("text/plain");
		
		StatusMonitor.output(response.getWriter());
		cacheStatus(response.getWriter());
		outputJVMStatus(response.getWriter());
		
		outputJVMSetting(response.getWriter());
    }
    
    private void cacheStatus(PrintWriter out){
    	out.println("\n==============缓存状态=================");
    	Map<String, Object> st = ShortUrlService.getInstance().cacheStat();

    	out.println("请求次数:" + st.get("get_count"));
    	out.println("命中次数:" + st.get("hit_count"));
    	out.println("命中失败次数:" + st.get("hit_failed_count"));
    	out.println("当前对象个数:" + st.get("item_count"));
    	out.println("删除对象个数:" + st.get("item_rmeove_count"));
    }
    
    private void outputJVMStatus(PrintWriter out){
    	out.println("\n==============JVM 状态=================");
    	Runtime rt = Runtime.getRuntime();
    	
    	int total = toM(rt.totalMemory());
    	int max = toM(rt.maxMemory());
    	int free = toM(rt.freeMemory());
    	
    	
    	out.println(String.format("系统内存，最大可用:%s Mb, 当前使用:%s Mb,  空闲:%s Mb",  max, total, free));
    	
    	out.println(String.format("当前线程数:%s", Thread.activeCount()));
    	//Thread.
    	//Thread.get
    	/*
    	Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
    	
    	out.println("\n---------线程列表---------");
    	for(Entry<Thread, StackTraceElement[]> entry: threads.entrySet()){
    		Thread t = entry.getKey();
    		StackTraceElement[] st = entry.getValue();
    		
    		out.println(String.format("[%s]%s, isActive:%s, trace:\n%s", 
    				t.getThreadGroup().getName(), t.getName(), t.isAlive(), 
    				formateStrackTrace(st)
    				));
    		
    		//out.println("\n");
    		//t.i
    		
    	}
    	*/
    }
    
    private void outputJVMSetting(PrintWriter out) throws IOException{
    	out.println("\n==============应用状态=================");
    	ShortUrlService s = ShortUrlService.getInstance();
    	int writeLog = s.writeLogQueue.size();
    	int getUrl = s.pendingShortKey.size();
    	
    	out.println(String.format("写日志队列:%s, 短网址等待数:%s",writeLog, getUrl));
    	
    	out.println("\n------------应用设置------------");
    	Settings.dumpSetting(out);
    }	
    private int toM(long m){
    	return (int)(m / 1024 / 1024);
    }
    
    private String formateStrackTrace(StackTraceElement[] st){
    	if(st == null) return "";
    	if(st.length < 2) return "";
    	StackTraceElement t = st[0];
    	String info = String.format("%s in %s[%s] -->\n",t.getMethodName(), t.getClassName(), t.getLineNumber());
    	
    	for(int i = 1; i < st.length; i++){
    		t = st[i];
    		String cname = t.getClassName();
    		if(cname.startsWith("com.taodian")){
    			info += String.format("%s in %s[%s] -->\n",t.getMethodName(), t.getClassName(), t.getLineNumber());
    		}else {
    			info += ".";
    		}
    	}
    	
    	return info;
    }
}
