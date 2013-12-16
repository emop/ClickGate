package com.taodian.click;

import java.io.PrintWriter;

import org.mortbay.util.ajax.Continuation;

public class ClickVisitorChannel {
	public Continuation continuation = null;
	public PrintWriter writer = null;
	
	public ClickVisitorChannel(Continuation c, PrintWriter p){
		this.continuation = c;
		this.writer = p;		
	}
}
