package com.taodian.click;

import java.io.Serializable;

/**
 * 店铺商品信息。
 * 
 * @author deonwu
 */
public class ShopItem implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public ShopAccount shop = null;
	public long shopId;
	public long numIid;
	public long libId;
	public long planId;
	public int version;
	public float price;
	public boolean isOnSale = false;
	
	public String saleStatus = "";
	public long lastRefreshTime = 0;
	public long created = System.currentTimeMillis();
	
	public int hashCode(){
		return (int)numIid;
	}
	
	public boolean equals(Object o){
		if(o instanceof ShopItem){
			return o.hashCode() == this.hashCode();
		}else {
			return false;
		}
	}	
}
