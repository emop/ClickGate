package com.taodian.click;

import java.io.PrintWriter;

import org.mortbay.util.ajax.Continuation;

public class ClickVisitorChannel {
	public static final long TIME_OUT = 30 * 60 * 1000;
	public Continuation continuation = null;
	public PrintWriter writer = null;
	public long created = System.currentTimeMillis();
	
	public ClickVisitorChannel(Continuation c, PrintWriter p){
		this.continuation = c;
		this.writer = p;
	}
	
	public boolean isTimouted(){
		return System.currentTimeMillis() - created > TIME_OUT;
	}
}
