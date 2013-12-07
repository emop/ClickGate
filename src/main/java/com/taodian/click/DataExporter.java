package com.taodian.click;

import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

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

			wsheet.addCell(new Label(9, row, "访问来源"));  
			wsheet.setColumnView(9, 80);
			
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
		
		wsheet.addCell(new Label(6, rowNum, d));  
		wsheet.addCell(new Label(7, rowNum,  ""));  
		wsheet.addCell(new Label(8, rowNum, rowData.get("click_time") + ""));  
		wsheet.addCell(new Label(9, rowNum, rowData.get("refer") + ""));  		
	}
			
	
	static class DataIterator implements Iterator<JSONObject>{
		private TaodianApi api = null;
		private String sql = "";
		private int page = 0;
		private int pageSize = 500;
		private ArrayBlockingQueue<JSONObject> queue = new ArrayBlockingQueue<JSONObject>(pageSize + 1);
		private String field = "";
		private String value = "";
		
		public DataIterator(String field, String value, String start, String end,
				TaodianApi api){
			this.api = api;
			
			sql = "select l.click_id, l.click_time, l.short_key, l.ip, CAST(l.device_type as char(1)) as device_type, " +
			"l.user_agent, l.refer," +
			"d.user_id, d.shop_id, d.num_iid, d.money, d.uid " +
			 "from click_log l join rpt_activity_click_detail d using(click_id) where d." + field + "='" + value + "' " +
			 "and topic_id=1000 order by click_time asc " +
			"limit %s, %s";
			
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
