package com.taodian.click;

import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxl.Workbook;
import jxl.format.Colour;
import jxl.format.UnderlineStyle;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;

import com.taodian.api.TaodianApi;
import com.taodian.emop.http.HTTPResult;

/**
 * 把数据导出为Excel的辅助类。
 * 
 * @author deonwu
 *
 */
public class DataExporter {
	private static Log log = LogFactory.getLog("click.exporter");

	
	public static void export(String field, String value, String start, String end,
			TaodianApi api, OutputStream os){
		try {
			WritableWorkbook wbook = Workbook.createWorkbook(os);
			WritableSheet wsheet = wbook.createSheet("访问详情", 0);
			

			WritableFont wfont = new WritableFont(WritableFont.ARIAL, 16,WritableFont.BOLD, 
			                       false,UnderlineStyle.NO_UNDERLINE,Colour.BLACK);   
			WritableCellFormat wcfFC = new WritableCellFormat(wfont); 
			wcfFC.setBackground(Colour.AQUA); 
			//wsheet.addCell(new Label(1, 0, tmptitle, wcfFC));   
			wfont = new jxl.write.WritableFont(WritableFont.ARIAL, 14,WritableFont.BOLD, 
			                   false, UnderlineStyle.NO_UNDERLINE,Colour.BLACK);   
			wcfFC = new WritableCellFormat(wfont);
			
			int row = 0;

			wsheet.addCell(new Label(0, row, "短网址")); 
			wsheet.setColumnView(0, 10);

			wsheet.addCell(new Label(1, row, "商家"));  
			wsheet.setColumnView(1, 12);

			wsheet.addCell(new Label(2, row, "商品"));  
			wsheet.setColumnView(2, 12);

			wsheet.addCell(new Label(3, row, "推广者"));  
			wsheet.setColumnView(3, 6);

			wsheet.addCell(new Label(4, row, "点击单价"));  
			wsheet.setColumnView(4, 7);
			
			wsheet.addCell(new Label(5, row, "访问IP"));  
			wsheet.setColumnView(5, 14);

			wsheet.addCell(new Label(6, row, "访问设备"));  
			wsheet.setColumnView(6, 8);

			wsheet.addCell(new Label(7, row, "浏览器")); 
			wsheet.setColumnView(7, 8);

			wsheet.addCell(new Label(8, row, "访问时间"));  
			wsheet.setColumnView(8, 20);

			wsheet.addCell(new Label(9, row, "访问者ID"));  
			wsheet.setColumnView(9, 12);
			
			wsheet.addCell(new Label(10, row, "访问来源"));  
			wsheet.setColumnView(10, 80);

			wsheet.addCell(new Label(11, row, "Agent"));  
			wsheet.setColumnView(11, 80);

			DataIterator data = new DataIterator(field, value, start, end, api);
			
			row++;
			while(data.hasNext()){
				writeRow(wsheet, data.next(), row++);
			}
			
			wbook.write(); // 写入文件   
			wbook.close();  

		} catch (Exception e) {
			log.error(e, e);
		} 		
	}
	
	public static void writeRow(WritableSheet wsheet, Map<String, Object> rowData, int rowNum) throws RowsExceededException, WriteException{
		wsheet.addCell(new Label(0, rowNum, rowData.get("short_key") + ""));   
		wsheet.addCell(new Label(1, rowNum, rowData.get("shop_id") + ""));  
		wsheet.addCell(new Label(2, rowNum, rowData.get("num_iid") + ""));  
		wsheet.addCell(new Label(3, rowNum, rowData.get("user_id") + ""));
		String m = String.format("%10.2f", rowData.get("money"));
		
		wsheet.addCell(new Label(4, rowNum, m));  
		wsheet.addCell(new Label(5, rowNum, rowData.get("ip") + ""));  
		
		String d = rowData.get("device_type") + "";
		if(d.equals("1")){
			d = "iPad";
		}else if(d.equals("2")){
			d = "iPhone";
		}else if(d.equals("3")){
			d = "Android";
		}else if(d.equals("4")){
			d = "其他手机";
		}else {
			d = "电脑";
		}
		
		String browser = "";
		String userAgent = rowData.get("user_agent") + "";
		
		//log.info("xx:" + userAgent + ", l:" + userAgent.length());
		if(userAgent.length() > 0){
			String[] names = new String[]{
			"firefox", "msie", "opera", "chrome", "safari",
            "mozilla", "seamonkey",    "konqueror", "netscape",
            "gecko", "navigator", "mosaic", "lynx", "amaya",
            "omniweb", "avant", "camino", "flock", "aol"};
			
			//#($browser)[/ ]?([0-9.]*)#
			for(String n : names) {
				String reg = "(" + n +"[/ ]?([0-9.]*)?)";
				Pattern pa = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);
				Matcher ma = pa.matcher(userAgent);
				if(ma.find()){
					browser = ma.group(1);
					break;
				}
			}
		}
		
		wsheet.addCell(new Label(6, rowNum, d));  
		wsheet.addCell(new Label(7, rowNum,  browser));  
		wsheet.addCell(new Label(8, rowNum, rowData.get("click_time") + ""));  
		wsheet.addCell(new Label(9, rowNum, rowData.get("uid") + ""));  	
		wsheet.addCell(new Label(10, rowNum, rowData.get("refer") + ""));  	
		wsheet.addCell(new Label(11, rowNum, userAgent));  			
	}
			
	
	static class DataIterator implements Iterator<JSONObject>{
		private TaodianApi api = null;
		private String sql = "";
		private int page = 0;
		private int pageSize = 500;
		private ArrayBlockingQueue<JSONObject> queue = new ArrayBlockingQueue<JSONObject>(pageSize + 1);
		//private String field = "";
		//private String value = "";
		
		public DataIterator(String field, String value, String start, String end,
				TaodianApi api){
			this.api = api;
			DateFormat dayFormate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			long startTime = 0, endTime = 0;
			startTime = (System.currentTimeMillis() - System.currentTimeMillis() % 24 * 60 * 60 * 1000) /  1000;
			endTime = startTime + 24 * 60 * 60;
			if(start != null && start.length() > 0){
				try {
					Date startDate = dayFormate.parse(start.trim() + " 00:00:00");
					startTime = startDate.getTime() / 1000;
				} catch (ParseException e) {
					log.warn("export start time error:" + start);
				}
			}
			if(end != null && end.length() > 0){
				try {
					Date endDate = dayFormate.parse(end.trim() + " 23:59:59");
					endTime = endDate.getTime() / 1000;
				} catch (ParseException e) {
					log.warn("export start time error:" + start);
				}
			}
			field = field.replace("'", "").replace("\"", "");
			value = value.replace("'", "").replace("\"", "");			
			
			sql = "select l.click_id, l.click_time, l.short_key, l.ip, CAST(l.device_type as char(1)) as device_type, " +
			"l.user_agent, l.refer," +
			"d.user_id, d.shop_id, d.num_iid, d.money, d.uid " +
			 "from click_log l join rpt_activity_click_detail d using(click_id) where d." + field + "='" + value + "' " +
			 " and d.click_time > " + startTime + " and d.click_time < " + endTime + 
			 " and topic_id=1000 order by d.click_time asc " +
			"limit %s, %s";
			
			log.info("export data with sql:" + sql);
			loadData();
		}

		@Override
		public boolean hasNext() {
			return queue.size() > 0;
		}

		@Override
		public JSONObject next() {
			if(queue.size() <= 1){
				loadData();
			}
			return queue.poll();
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub
			
		}
		
		private void loadData(){
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("db_name", "click_report");
			
			String tmpSql = String.format(sql, page * pageSize, pageSize);
			param.put("sql", tmpSql);
			
			HTTPResult result = api.call("data_get_data", param);
			if(result.isOK){
				Collection<JSONObject> dataList = (Collection<JSONObject>)result.json.get("data");
				queue.addAll(dataList);
			}
			page++;
		}
	}
	
}
