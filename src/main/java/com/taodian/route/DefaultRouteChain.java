package com.taodian.route;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultRouteChain implements RouteChain {
	protected ArrayList<Rule> rules = new ArrayList<Rule>();
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();
    private RuleMatcher matcher = null;
    
    public DefaultRouteChain(RuleMatcher matcher){
    	this.matcher = matcher;
    }
    
	@Override
	public boolean addRoute(Rule rule) {		
		return addRoute(-1, rule);
	}
	
	@Override
	public boolean addRoute(int index, Rule rule) {
		if(match(rule, true, false) == null){
			w.lock();
			try{
				if(index >= 0 && index < rules.size()){
					rules.add(index, rule);
				}else {
					rules.add(rule);
				}	
			}finally{
				w.unlock();
			}
			
			return true;
		}
		return false;
	}	

	@Override
	public boolean delRoute(Rule rule) {
		if (rule == null) return false;
		
		boolean ret = false;
		w.lock();
		try{
			for(Iterator<Rule> iter = rules.iterator(); iter.hasNext();){
				if(matcher.isMatch(rule, rule, true)){
					iter.remove();
					ret = true;
					break;
				}
			}
		}finally{
			w.unlock();
		}
		
		return ret;
	}

	@Override
	public List<Rule> rules() {
		List<Rule> ret = new ArrayList<Rule>();
		r.lock();
		try{
			ret.addAll(rules);
		}finally{
			r.unlock();
		}
		return ret;
	}

	@Override
	public Rule match(Rule rule) {
		return match(rule, false, true);
	}
	
	protected Rule match(Rule rule, boolean strict, boolean retDef) {
		ArrayList<Rule> removed = null;
		r.lock();
		
		Rule matched = null;
		try{
			long cur = System.currentTimeMillis();
			for(Iterator<Rule> iter = rules.iterator(); iter.hasNext();){
				Rule r = iter.next();
				if(r.expired > 0 && r.expired < cur){
					if(removed == null){
						removed = new ArrayList<Rule>();
					}
					removed.add(r);
					continue;
				} 
				if(matcher.isMatch(r, rule, strict)){
					matched = r;
					break;
				}
			}
		}finally{
			r.unlock();
		}
		
		if(removed != null){
			w.lock();
			try{
				rules.removeAll(removed);
			}finally{
				w.unlock();
			}
		}

		return matched;
	}

}
