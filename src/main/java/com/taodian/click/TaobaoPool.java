package com.taodian.click;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;

import com.taodian.api.TaodianApi;
import com.taodian.emop.Settings;
import com.taodian.emop.http.HTTPResult;
import com.taodian.emop.utils.CacheApi;
import com.taodian.emop.utils.SimpleCacheApi;

/**
 * 淘宝商品库，主要用来检查是否已经下架。
 * 
 * @author deonwu
 */
public class TaobaoPool {
	private Log log = LogFactory.getLog("click.taobaopool");
	private TaodianApi api = null;
	
	/**
	 * 老版本的API：http://fmei.sinaapp.com/api/route
	 */
	private TaodianApi emopApi = null;

	/**
	 * CPC 点击统计，商家计费缓存。
	 */
	private CacheApi cpcCache = new SimpleCacheApi();
	private ThreadPoolExecutor threadPool = null;
	public CopyOnWriteArraySet<String> pendingTask = new CopyOnWriteArraySet<String>();
	public BlockingQueue<Runnable> taskQueue = null;
	
	public void start(ThreadPoolExecutor pool, TaodianApi api, TaodianApi emopApi){
		this.threadPool = pool;
		taskQueue = pool.getQueue();
		this.api = api;
		this.emopApi = emopApi;
	}
	
	public ShopItem getShopItem(final long shopId, final long numIid){ 
		final String ac = "item_" + shopId + "_" + numIid;
		Object tmp = cpcCache.get(ac, true);
		ShopItem item = null;
		if(tmp == null){
			tmp = getDefaultItem(shopId, numIid);
			cpcCache.set(ac, tmp, 5 * 60);
		}
		item = (ShopItem)tmp;
		
		//如果超过3分钟没有刷新，等待刷新商品状态。
		if(System.currentTimeMillis() - item.lastRefreshTime > 1000 * 60 * 3){
			if(taskQueue.remainingCapacity() > 1 && !pendingTask.contains(ac)){
				pendingTask.add(ac);
				final ShopItem e = item;
				threadPool.execute(new Runnable(){
					public void run(){
						try{
							refreshShopItem(e);
							log.debug("refresh item status ok, price:" + e.price + ", onsale:" + e.isOnSale);
							e.lastRefreshTime = System.currentTimeMillis();
						}finally{
							pendingTask.remove(ac);
							synchronized(e){
								e.notifyAll();
							}
						}
					}
				});
			}
			
			//如果商品在状态刷新中，等待2秒。
			if(pendingTask.contains(ac)){
				synchronized(item){
					try {
						item.wait(1000 * 2);
					} catch (InterruptedException e) {
					}
				}	
			}
		}
		
		return item;
	}
	
	protected ShopItem getDefaultItem(long shopId, long numIid){
		ShopItem item = new ShopItem();
		item.isOnSale = true;
		item.shopId = shopId;
		item.numIid = numIid;
		int itemPrice = Settings.getInt("default_cpc_click_price", 10);
		item.price = itemPrice / 100.0f;
		
		return item;
	}
	
	private void refreshShopItem(ShopItem item){
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("shop_id", item.shopId);
		param.put("num_iid", item.numIid);

		HTTPResult r = api.call("credit_get_cpc_item_price", param);
		
		if(r.isOK){
			String f = r.getString("data.price");
			String s = r.getString("data.status");
			if(s != null && s.equals("0")){
				item.price = Float.parseFloat(f);
			}
		}
		
		param.put("num_iid", item.numIid);
		param.put("fields", "sale_status,num_iid");
		param.put("refresh_expired", 5);
		param.put("dict_list", "y");		

		r = emopApi.call("taobao_items_list_get", param);
		
		if(r.isOK){
			JSONObject obj = r.getJSONObject("data." + item.numIid);
			Object st = null;
			if(obj != null){
				st = obj.get("sale_status");
				item.saleStatus = st + "";
				item.isOnSale = st != null && st.equals("onsale");
			}
			log.debug("Shop item in emop api, item:" + item.numIid + ", sale_status:" + st);
		}
	}	
	

}
