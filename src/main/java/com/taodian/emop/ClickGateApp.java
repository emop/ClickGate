package com.taodian.emop;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;

import com.taodian.click.ShortUrlService;
import com.taodian.emop.servlet.ApiRouterServlet;
import com.taodian.emop.servlet.DumpLogServlet;
import com.taodian.emop.servlet.ExporterServlet;
import com.taodian.emop.servlet.IndexServlet;
import com.taodian.emop.servlet.ShortUrlServlet;
import com.taodian.emop.servlet.StatusServlet;


/**
 * Hello world!
 *
 */
public class ClickGateApp 
{
    public static void main( String[] args ) throws Exception
    {
    	System.setProperty("user.timezone","Asia/Shanghai");
    	Settings.loadSettings();
    	//updateLog4jLevel("short_url");
    	startCleanLog("short_url");
    	
    	ShortUrlService.getInstance();
    	
    	Log log = LogFactory.getLog("click.gate");    	
    	
    	int port = Settings.getInt(Settings.HTTP_PORT, 8082);
        Server server = new Server(port);
        
        log.info("Starting click gate http server at port:" + port);
        
        ServletHandler context = new ServletHandler();
        server.setHandler(context);
 
        context.addServletWithMapping(ShortUrlServlet.class, "/c/*");
        context.addServletWithMapping(ShortUrlServlet.class, "/t/*");
        //数据导出。
        context.addServletWithMapping(ExporterServlet.class, "/export/*");
        
        context.addServletWithMapping(StatusServlet.class, "/status");
        
        //实时日志
        context.addServletWithMapping(DumpLogServlet.class, "/log");
        context.addServletWithMapping(ApiRouterServlet.class, "/api");

        context.addServletWithMapping(IndexServlet.class, "/*");
 
        server.start();
        server.join();
    }
    
	private static void startCleanLog( final String name){
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
				try{
					updateLog4jLevel(name);
				}catch(Throwable e){
					root.info(e.toString());
				}
			}
		}, 100, 1000 * 3600 * 12);
	}	
	
	private static void updateLog4jLevel(String name){
        org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
        String level = Settings.getString(Settings.LOG_LEVEL, "debug").toLowerCase().trim();
        if(level.equals("trace")){
                root.setLevel(org.apache.log4j.Level.TRACE);
        }else if(level.equals("debug")){
                root.setLevel(org.apache.log4j.Level.DEBUG);
        }else if(level.equals("info")){
                root.setLevel(org.apache.log4j.Level.INFO);
        }else if(level.equals("warn")){
                root.setLevel(org.apache.log4j.Level.WARN);
        }
        File r = new File("logs");
        
        int max_log_days = Settings.getInt(Settings.MAX_LOG_DAYS, 10);                
        Date d = new Date(System.currentTimeMillis() - 1000 * 3600 * 24 * max_log_days);                
        DateFormat format= new SimpleDateFormat("yy-MM-dd");            
        //root.debug("Remove log before " + format.format(d));
        for(File log : r.listFiles()){
            //if(!log.getName().startsWith(name))continue;
            String[] p = log.getName().split("\\.");
            String logDate = p[p.length -1];
            if(logDate.indexOf("-") > 0){
                try {
                    if(format.parse(logDate).getTime() < d.getTime()){
                            root.info("remove old log file:" + log.getName());
                            log.delete();
                    }
                } catch (Exception e) {
                        root.info(e.toString());
                }
            }
        }
	}    
}
