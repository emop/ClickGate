package com.taodian.click;

import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.taodian.api.TaodianApi;
import com.taodian.emop.http.HTTPClient;
import com.taodian.emop.http.HTTPResult;

/**
 * 把数据导出为Excel的辅助类。
 * 
 * @author deonwu
 *
 */
public class MemberExporter {
	private static Log log = LogFactory.getLog("click.exporter");
	private static HTTPClient http = null;
	private static TaodianApi tdApi = null;
	
	public static void export(String url,
			TaodianApi api, OutputStream os){
		http = HTTPClient.create("apache");
		tdApi = api;
		try {
			WritableWorkbook wbook = Workbook.createWorkbook(os);
			WritableSheet wsheet = wbook.createSheet("会员列表", 0);
			

			WritableFont wfont = new WritableFont(WritableFont.ARIAL, 16,WritableFont.BOLD, 
			                       false,UnderlineStyle.NO_UNDERLINE,Colour.BLACK);   
			WritableCellFormat wcfFC = new WritableCellFormat(wfont); 
			wcfFC.setBackground(Colour.AQUA); 
			//wsheet.addCell(new Label(1, 0, tmptitle, wcfFC));   
			wfont = new jxl.write.WritableFont(WritableFont.ARIAL, 14,WritableFont.BOLD, 
			                   false, UnderlineStyle.NO_UNDERLINE,Colour.BLACK);   
			wcfFC = new WritableCellFormat(wfont);
			
			int row = 0;



			DataIterator data = new DataIterator(url, api);
			
			writeHeadRow(wsheet, data.next(), row);
			
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

	public static void writeHeadRow(WritableSheet wsheet, List<Object> rowData, int rowNum) throws RowsExceededException, WriteException{
		int row = rowNum;

		for(int i = 0; i < rowData.size(); i++){		
			wsheet.addCell(new Label(i, row, rowData.get(i) + "")); 
			wsheet.setColumnView(0, 10);		
		}

	}
	
	public static void writeRow(WritableSheet wsheet, List<Object> rowData, int rowNum) throws RowsExceededException, WriteException{
		
		for(int i = 0; i < rowData.size(); i++){	
			String o = rowData.get(i) + "";
			if(o.trim().startsWith("@")){
				o = convertCredit(o.substring(1));
			}			
			wsheet.addCell(new Label(i, rowNum, o)); 
			wsheet.setColumnView(0, 10);		
		}
	}
	
	public static String convertCredit(String account){
		HashMap p = new HashMap<String, Object>();
		p.put("account_id", account);
		HTTPResult r = tdApi.call("credit_get_credit_account", p);
		String o = r.getString("data.banlance");
		
		return o;
	}
			
	
	static class DataIterator implements Iterator<JSONArray>{
		private TaodianApi api = null;
		private int pageSize = 50000;
		private ArrayBlockingQueue<JSONArray> queue = new ArrayBlockingQueue<JSONArray>(pageSize + 1);
		//private String field = "";
		//private String value = "";
		private String datURL = "";
		private HashMap p = new HashMap<String, Object>();
		
		public DataIterator(String url,
				TaodianApi api){
			this.api = api;
			this.datURL = url;
			if(url.indexOf('?') > 0){
				String[] r = url.split("\\?", 2);
				datURL = r[0];
				if(r[1].length() > 0){
					for(String seg : r[1].split("&")){
						String[] p2 = seg.split("=", 2);
						p.put(p2[0], p2[1]);
						
						log.info("parse param:" + p2[0] + "->" + p2[1]);
					}
				}
			}
			
			log.info("export data with sql:" + datURL);
			loadData();
		}

		@Override
		public boolean hasNext() {
			return queue.size() > 0;
		}

		@Override
		public JSONArray next() {
			/*
			if(queue.size() <= 1){
				loadData();
			}*/
			return queue.poll();
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub
			
		}
		
		private void loadData(){			
			HTTPResult result = http.post(datURL, p);
			if(result.isOK){
				Collection<JSONArray> dataList = (Collection<JSONArray>)result.json.get("data");
				queue.addAll(dataList);
			}			
		}
	}
	
}
