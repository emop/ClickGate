package com.taodian.emop;

import com.taodian.route.DefaultRouter;
import com.taodian.route.RouteException;

public class MocRouter extends DefaultRouter {
	
	public CLI parseCommandLineTest(String s) throws RouteException{
		return this.parseCommandLine(s);
	}

}
