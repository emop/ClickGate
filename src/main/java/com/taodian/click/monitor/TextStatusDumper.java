package com.taodian.click.monitor;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.taodian.click.ShopAccount;
import com.taodian.click.ShortUrlModel;
import com.taodian.emop.Version;

public class TextStatusDumper {
	private static DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static DateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	
	public static void output(PrintWriter writer, StatusMonitor status){
		writer.println(Version.getName() + " " + Version.getVersion());
		writer.println("Server started at:" + sdf.format(status.uptime));
		
		postOutput(writer, status);
		
		getOutput(writer, status);
		notFoundOutput(writer, status);
		
		cpcStatus(writer, status);

		httpOutput(writer, com.taodian.api.monitor.StatusMonitor.monitor);		
	}

	public static void postOutput(PrintWriter writer, StatusMonitor status){
		writer.println("");
		writer.println("==========到淘宝跳转(过滤爬虫)==========");
		writer.println("总调用次数:" + status.post.requestCount);
		
		writer.println("平均花费时间:" + status.post.averageElapsed + " ms, " +
				"最长时间:" + status.post.maxElapsed + " ms, " +
				"最短时间:" + status.post.minElapsed + " ms");
		writer.println("");
		writer.println("----------最近跳转列表-----------");
		for(Benchmark item : status.post.lastList.list()){
			clickUrlOutput(writer, item);
		}

		writer.println("----------最慢跳转列表-----------");
		for(Benchmark item : status.post.slowList.list()){
			clickUrlOutput(writer, item);
		}
		
	}
	
	public static void clickUrlOutput(PrintWriter writer, Benchmark item){
		if(item == null) return;
		if(item.obj == null || !(item.obj instanceof ShortUrlModel)) return;
		ShortUrlModel s = (ShortUrlModel)item.obj;
				
		String msg = String.format("%6d(ms) %s s:%s, uid:%s, ip:%s, mobile:%s, refer:%s", item.elapsed, sdf.format(new Date(item.start)),
				s.uri != null ? s.uri.toString() : s.shortKey, s.uid, s.ip, s.isMobile, s.refer
				);
		writer.println(msg);
	}
	
	public static void getOutput(PrintWriter writer, StatusMonitor status){
		writer.println("");
		writer.println("==========短网址访问(包含爬虫)==========");
		writer.println("总调用次数:" + status.get.requestCount);
		
		writer.println("平均花费时间:" + status.get.averageElapsed + " ms, " +
				"最长时间:" + status.get.maxElapsed + " ms, " +
				"最短时间:" + status.get.minElapsed + " ms");
		writer.println("");
		writer.println("----------最近跳转列表-----------");
		for(Benchmark item : status.get.lastList.list()){
			clickUrlOutput(writer, item);
		}
		
		writer.println("----------最慢跳转列表-----------");
		for(Benchmark item : status.get.slowList.list()){
			clickUrlOutput(writer, item);
		}
	}

	public static void notFoundOutput(PrintWriter writer, StatusMonitor status){
		writer.println("");
		writer.println("==========访问错误==========");
		writer.println("总错误次数:" + status.notfound.requestCount);		
		writer.println("");
		writer.println("----------最近错误列表-----------");
		for(Benchmark item : status.notfound.lastList.list()){
			clickUrlOutput(writer, item);
		}
	}
	
	public static void httpOutput(PrintWriter writer, com.taodian.api.monitor.StatusMonitor status){
		writer.println("");
		writer.println("==========HTTP请求统计==========");
		writer.println("总调用次数:" + status.http.requestCount);
		
		writer.println("平均花费时间:" + status.http.averageElapsed + " ms, " +
				"最长时间:" + status.http.maxElapsed + " ms, " +
				"最短时间:" + status.http.minElapsed + " ms");

		writer.println("");
		writer.println("----------最慢请求列表-----------");
		for(com.taodian.api.monitor.Benchmark item : status.http.slowList.list()){
			if(item == null) continue;
			String tmp = item.obj + "";
			tmp = tmp.replace("\n", "\\n");
			tmp = tmp.substring(0, Math.min(120, tmp.length()));
			
			String msg = String.format("%6d(ms) %s url:%s", item.elapsed, sdf.format(new Date(item.start)),
					tmp
					);
			writer.println(msg);		
		}
	}	
	
	public static void cpcStatus(PrintWriter writer, StatusMonitor status){
		writer.println("");
		writer.println("==========CPC点击统计统计==========");
		writer.println("总调用次数:" + status.cpcOk.requestCount);
		
		writer.println("");
		writer.println("----------正常店铺状态列表-----------");
		for(ShopAccount item : status.cpcOk.uniqueList.list()){
			cpcShopOutput(writer, item);
		}

		writer.println("");
		writer.println("----------异常店铺状态列表-----------");
		for(ShopAccount item : status.cpcErr.uniqueList.list()){
			cpcShopOutput(writer, item);
		}
	}

	private static void cpcShopOutput(PrintWriter writer, ShopAccount item){
		if(item == null) return;
		//if(item.obj == null || !(item.obj instanceof ShopAccount)) return;
		//ShopAccount s = (ShopAccount)item.obj;
				
		String msg = String.format("%s shop id:%s, balance:%3$1.2f, status:%4s", sdf.format(new Date(item.created)),
				item.shopId, item.banlance, item.status);
		writer.println(msg);
	}	
}
