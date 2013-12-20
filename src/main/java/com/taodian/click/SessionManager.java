package com.taodian.click;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 根据HTTP 请求Session，生成推广者ID。来识别唯一的一个客户端。
 * 
 * @author deonwu
 *
 */
public interface SessionManager {
	
	/**
	 * 根据HTTP 请求，生成访问者ID(uid)，来识别唯一的一个访问用户。
	 * 
	 * @param req -- ServletRequest 对象，封装了HTTP的请求头。
	 * @param response -- 响应对象，如果需要写Cookie什么的需要这个对象。
	 * @return 生成的用户识别ID。
	 */
	public String getSessionUserId(HttpServletRequest req, HttpServletResponse response);

	/**
	 * 生成客户端识别码，客户端在首次发现新的识别码时，会存储到本地。为了防止作弊，应该尽量避免客户端
	 * 识别码可以枚举或由第三方软件生成。
	 * 
	 * 用户识别码(uid)和终端识别码(cid)的关系：
	 * uid 是系统内部存储，统计，跟踪用到的一个整型数字。可以使用cid来生成uid，也可能是系统直接分配的id。
	 * cid 是识别用户终端的ID，只在ClickGate端使用。
	 * 
	 * @param req -- ServletRequest 对象，封装了HTTP的请求头。
	 * @return 生成的客户端识别码。
	 */	
	public String getSessionId(HttpServletRequest req);

}
