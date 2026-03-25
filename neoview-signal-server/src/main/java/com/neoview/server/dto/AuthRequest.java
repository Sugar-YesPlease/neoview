package com.neoview.server.dto;

/**
 * 登录/注册请求体
 * 用于接收用户认证请求的数据
 * 
 * @author 椰子皮
 */
public class AuthRequest {
	/** 用户名 */
	public String username;
	
	/** 密码 */
	public String password;
	
	/** 显示名称 */
	public String displayName;
}
