package com.neoview.server.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 过滤器：从请求头读取 token 并注入 Spring Security 上下文
 * 负责在每个请求中解析JWT令牌并设置用户认证信息
 * 
 * @author 椰子皮
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
	private final JwtService jwtService;

	/**
	 * 构造函数
	 * 注入JWT服务
	 * 
	 * @param jwtService JWT服务
	 * @author 椰子皮
	 */
	public JwtAuthFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	/**
	 * 执行过滤器内部逻辑
	 * 从请求头或查询参数中提取JWT令牌，解析并设置认证信息
	 * 
	 * @param request HTTP请求对象
	 * @param response HTTP响应对象
	 * @param filterChain 过滤器链
	 * @throws ServletException Servlet异常
	 * @throws IOException IO异常
	 * @author 椰子皮
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request,
							HttpServletResponse response,
							FilterChain filterChain) throws ServletException, IOException {
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		String token = null;

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
		} else {
			// WebSocket 无法自定义 Header，这里支持 query 参数传 token
			token = request.getParameter("token");
		}

		if (token != null && !token.isBlank()) {
			try {
				Claims claims = jwtService.parse(token);
				String userId = claims.getSubject();
				String username = claims.get("username", String.class);

				UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(userId, username, Collections.emptyList());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (Exception ignored) {
				// token 无效直接放行，让后续鉴权拦截
			}
		}


		filterChain.doFilter(request, response);
	}
}
