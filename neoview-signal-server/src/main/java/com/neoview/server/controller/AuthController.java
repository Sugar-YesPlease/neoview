package com.neoview.server.controller;

import com.neoview.server.dto.AuthRequest;
import com.neoview.server.dto.AuthResponse;
import com.neoview.server.model.User;
import com.neoview.server.security.JwtService;
import com.neoview.server.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 登录/注册接口
 * 处理用户认证相关的HTTP请求
 * 
 * @author 椰子皮
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final UserService userService;
	private final JwtService jwtService;

	/**
	 * 构造函数
	 * 注入用户服务和JWT服务
	 * 
	 * @param userService 用户服务
	 * @param jwtService JWT服务
	 * @author 椰子皮
	 */
	public AuthController(UserService userService, JwtService jwtService) {
		this.userService = userService;
		this.jwtService = jwtService;
	}

	/**
	 * 用户注册接口
	 * 注册新用户并返回认证信息（当前已禁用）
	 * 
	 * @param request 注册请求体，包含用户名、密码和显示名称
	 * @return ResponseEntity<AuthResponse> 注册成功返回认证响应，当前返回401未授权
	 * @author 椰子皮
	 */
	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
		User user = userService.register(request.username, request.password, request.displayName);
		String token = jwtService.generateToken(user.getId(), user.getUsername());
        return ResponseEntity.status(401).build();
	}

	/**
	 * 用户登录接口
	 * 验证用户凭证并返回JWT令牌
	 * 
	 * @param request 登录请求体，包含用户名和密码
	 * @return ResponseEntity<AuthResponse> 登录成功返回认证响应（含令牌），失败返回401
	 * @author 椰子皮
	 */
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
		User user = userService.findByUsername(request.username);
		if (user == null || !userService.verifyPassword(user, request.password)) {
			return ResponseEntity.status(401).build();
		}
		String token = jwtService.generateToken(user.getId(), user.getUsername());
		return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getDisplayName()));
	}
}
