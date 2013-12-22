package com.taodian.click.api;

import java.util.Map;

import com.taodian.click.ShortUrlService;
import com.taodian.route.RouteException;

/**
 * Click Gate 相关的 API 功能。通过HTTP JSON方式调用。
 * 
 * @author deonwu
 *
 */
public class GateApi {
	
	/**
	 * 更新路由规则配置。 在Map 中加入， route 命令，完成配置功能。
	 * @param param
	 * @return
	 */
	public Result updateGateRoute(Map<String, Object> param){
		Result r = Result.emptyOk();
		ShortUrlService s = ShortUrlService.getInstance();
		Object cmd = param.get("route");
		if(s.router != null && cmd != null){
			try {
				if(!s.router.updateRouteTable(cmd + "")){
					r.error("ret", "route command return false");
				}
			} catch (RouteException e) {
				r.error("route_err", e.toString());
			}
		}else if(cmd == null) {
			r.error("no_found_route_cmd", "Not found route command");
		}else if(s.router == null){
			r.error("route_disabled", "The click gate isn't enbaled route feature.");			
		}
		
		return r;
	}

}
