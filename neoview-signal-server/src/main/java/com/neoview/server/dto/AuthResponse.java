package com.neoview.server.dto;

/**
 * 登录响应体
 * 用于返回用户认证成功后的数据
 * 
 * @author 椰子皮
 */
public class AuthResponse {
	/** JWT令牌 */
	public String token;
	
	/** 用户ID */
	public String userId;
	
	/** 显示名称 */
	public String displayName;

	/**
	 * 构造函数
	 * 创建登录响应对象
	 * 
	 * @param token JWT令牌
	 * @param userId 用户ID
	 * @param displayName 显示名称
	 * @author 椰子皮
	 */
	public AuthResponse(String token, String userId, String displayName) {
		this.token = token;
		this.userId = userId;
		this.displayName = displayName;
	}
}
