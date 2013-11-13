package com.taodian.emop;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;

import com.taodian.emop.servlet.ShortUrlServlet;


/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
    	Log log = LogFactory.getLog("click.gate");
    	String httpPort = System.getProperty("http_port", "8082");
    	int port = Integer.parseInt(httpPort);
    	
        Server server = new Server(port);
        
        log.info("Starting click gate http server at port:" + port);
        
        ServletHandler context = new ServletHandler();
        server.setHandler(context);
 
        context.addServletWithMapping(ShortUrlServlet.class, "/c/*");
 
        server.start();
        server.join();
    }
}
