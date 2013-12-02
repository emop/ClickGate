package com.taodian.click;

import java.io.Serializable;

public class ShopItemPrice implements Serializable{
	public ShopAccount shop = null;
	public long numIid;
	public float price;

	
	public int hashCode(){
		return (int)numIid;
	}
	
	public boolean equals(Object o){
		if(o instanceof ShopItemPrice){
			return o.hashCode() == this.hashCode();
		}else {
			return false;
		}
	}	
}
