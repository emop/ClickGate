package com.taodian.click.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.taodian.click.SessionManager;
import com.taodian.click.ShortUrlService;

public class SimpleCookieSessionManager implements SessionManager  {

	@Override
	public String getSessionUserId(HttpServletRequest req,
			HttpServletResponse response) {
		return ShortUrlService.getInstance().newUserId() + "";
	}

	@Override
	public String getSessionId(HttpServletRequest req) {
		// TODO Auto-generated method stub
		return "na";
	}


}
