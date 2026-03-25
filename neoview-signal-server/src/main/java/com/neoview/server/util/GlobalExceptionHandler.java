package com.neoview.server.util;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.context.request.WebRequest;

/**
 * 全局异常处理：把服务端异常转换为可读信息
 * 统一处理各类异常并返回标准化的错误响应
 * 
 * @author 椰子皮
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
	/**
	 * 处理非法参数异常
	 * 返回400错误状态
	 * 
	 * @param ex 非法参数异常
	 * @return ResponseEntity<ApiError> 错误响应
	 * @author 椰子皮
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(new ApiError(ex.getMessage()));
	}
	
	/**
	 * 处理认证异常
	 * 返回401未授权状态
	 * 
	 * @param ex 认证异常
	 * @return ResponseEntity<ApiError> 错误响应
	 * @author 椰子皮
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
		return ResponseEntity.status(401).body(new ApiError("认证失败: " + ex.getMessage()));
	}
	
	/**
	 * 处理访问拒绝异常
	 * 返回403禁止访问状态
	 * 
	 * @param ex 访问拒绝异常
	 * @return ResponseEntity<ApiError> 错误响应
	 * @author 椰子皮
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
		return ResponseEntity.status(403).body(new ApiError("访问被拒绝: " + ex.getMessage()));
	}
	
	/**
	 * 处理通用异常
	 * 返回500服务器内部错误状态
	 * 
	 * @param ex 异常对象
	 * @param request Web请求对象
	 * @return ResponseEntity<ApiError> 错误响应
	 * @author 椰子皮
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGeneric(Exception ex, WebRequest request) {
		// 打印详细错误日志
		ex.printStackTrace();
		return ResponseEntity.status(500).body(new ApiError("服务器内部错误: " + ex.getMessage()));
	}
}
