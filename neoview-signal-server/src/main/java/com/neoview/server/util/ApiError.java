package com.neoview.server.util;

/**
 * 统一错误返回
 * 用于封装API错误响应信息
 * 
 * @author 椰子皮
 */
public class ApiError {
	/** 错误信息 */
	public String message;
	
	/** 时间戳 */
	public String timestamp;
	
	/** 请求路径 */
	public String path;

	/**
	 * 构造函数
	 * 创建带有错误信息的错误响应
	 * 
	 * @param message 错误信息
	 * @author 椰子皮
	 */
	public ApiError(String message) {
		this.message = message;
		this.timestamp = java.time.LocalDateTime.now().toString();
	}
	
	/**
	 * 构造函数
	 * 创建带有错误信息和请求路径的错误响应
	 * 
	 * @param message 错误信息
	 * @param path 请求路径
	 * @author 椰子皮
	 */
	public ApiError(String message, String path) {
		this.message = message;
		this.timestamp = java.time.LocalDateTime.now().toString();
		this.path = path;
	}
}
