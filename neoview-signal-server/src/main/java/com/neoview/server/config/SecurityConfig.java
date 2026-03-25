package com.neoview.server.config;

import com.neoview.server.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * 安全配置：启用 JWT，无状态登录
 * 配置Spring Security安全策略，包括JWT认证、CSRF保护、请求授权等
 * 
 * @author 椰子皮
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
	/**
	 * 配置安全过滤器链
	 * 设置JWT认证、CSRF禁用、无状态会话、请求授权规则
	 * 
	 * @param http HttpSecurity配置对象
	 * @param jwtAuthFilter JWT认证过滤器
	 * @param corsConfigurationSource CORS配置源
	 * @return SecurityFilterChain 安全过滤器链
	 * @throws Exception 配置异常
	 * @author 椰子皮
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter, CorsConfigurationSource corsConfigurationSource) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/auth/**").permitAll()
				.requestMatchers("/api/health").permitAll()
				.requestMatchers("/ws/signaling/**").authenticated()
				.requestMatchers("/api/meetings/**").authenticated()
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * 创建密码编码器
	 * 使用BCrypt算法进行密码加密
	 * 
	 * @return PasswordEncoder 密码编码器实例
	 * @author 椰子皮
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * 创建认证管理器
	 * 用于处理用户认证请求
	 * 
	 * @param configuration 认证配置对象
	 * @return AuthenticationManager 认证管理器实例
	 * @throws Exception 获取认证管理器异常
	 * @author 椰子皮
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}
