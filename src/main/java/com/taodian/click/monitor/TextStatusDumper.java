package com.taodian.click.monitor;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.taodian.emop.Version;

public class TextStatusDumper {
	private static DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static DateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	
	public static void output(PrintWriter writer, StatusMonitor status){
		writer.println(Version.getName() + " " + Version.getVersion() + " build time:" + Version.getBuildDate());
		writer.println("Server started at:" + sdf.format(status.uptime));
		
	//	weixinOutput(writer, status);
		httpOutput(writer, status);
		
		pendingRequestOutput(writer, status);

	}
	
	/*
	public static void weixinOutput(PrintWriter writer, StatusMonitor status){
		writer.println("");
		writer.println("==========微信接口转发状态==========");
		writer.println("总调用次数:" + status.weixin.requestCount);
		writer.println("响应超时次数:" + status.weixin.timeoutCount);
		
		writer.println("平均花费时间:" + status.weixin.averageElapsed + " ms, " +
				"最长时间:" + status.weixin.maxElapsed + " ms, " +
				"最短时间:" + status.weixin.minElapsed + " ms");
		writer.println("");
		writer.println("最近一次请求：");
		outWeixinRequest(writer, status.weixin.last);
		writer.println("");
		writer.println("----------时间最长记录-----------");
		for(Benchmark item : status.weixin.slowList.list()){
			outWeixinRequest(writer, item);
		}
	}
	*/
	
	/*
	public static void outWeixinRequest(PrintWriter writer, Benchmark item){
		if(item == null) return;
		RouteRequest req = (RouteRequest)item.obj;
		String tmp = req.msg != null ? req.msg.data.get(WeixinMessage.CONTENT) : "";
		tmp = tmp == null ? "" : tmp;
		tmp = tmp.replace("\n", "\\n");
		tmp = tmp.substring(0, Math.min(120, tmp.length()));
		
		String msg = String.format("%6d(ms) %s session:%-20s type:%-8s content:%s", item.elapsed, sdf.format(new Date(item.start)),
				req.session.toString(), 
				req.msg != null ? req.msg.msgType : "",
						tmp
				);
		writer.println(msg);
	}
	*/

	public static void httpOutput(PrintWriter writer, StatusMonitor status){
		writer.println("\n");
		writer.println("==========HTTP请求记录==========");
		writer.println("总调用次数:" + status.http.requestCount);
		
		writer.println("平均花费时间:" + status.http.averageElapsed + " ms, " +
				"最长时间:" + status.http.maxElapsed + " ms, " +
				"最短时间:" + status.http.minElapsed + " ms");
		writer.println("");
		writer.println("最近一次请求：");
		outHTTPRequest(writer, status.http.last);
		writer.println("");
		writer.println("----------时间最长记录-----------");
		for(Benchmark item : status.http.slowList.list()){
			outHTTPRequest(writer, item);
		}
	}
	
	public static void outHTTPRequest(PrintWriter writer, Benchmark item){
		if(item == null) return;
		String tmp = item.obj + "";
		tmp = tmp.replace("\n", "\\n");
		tmp = tmp.substring(0, Math.min(120, tmp.length()));
		
		String msg = String.format("%6d(ms) %s url:%s", item.elapsed, sdf.format(new Date(item.start)),
				tmp
				);
		writer.println(msg);
	}	
	
	public static void pendingRequestOutput(PrintWriter writer, StatusMonitor status){
		writer.println("\n");
		writer.println("==========同时在线用户记录==========");
		writer.println("检查次数:" + status.pending.requestCount);
		
		writer.println("平均在线人数:" + status.pending.averageElapsed + " 人, " +
				"最多在线:" + status.pending.maxElapsed + " 人, " +
				"最少在线:" + status.pending.minElapsed + " 人");
		writer.println("");
		writer.println("最近一次统计：");
		pendingRequestRequest(writer, status.pending.last);
		writer.println("");
		writer.println("----------同时在线时间点-----------");
		for(Benchmark item : status.pending.slowList.list()){
			pendingRequestRequest(writer, item);
		}
	}
	
	public static void pendingRequestRequest(PrintWriter writer, Benchmark item){
		if(item == null) return;
		
		String msg = String.format("%6d 人 %s", item.elapsed, sdf2.format(new Date(item.start)));
		writer.println(msg);
	}		
}
