package com.neoview.server.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 全局 CORS 配置：允许本地 UI 访问 REST 接口
 * 
 * @author 椰子皮
 */
@Configuration
public class CorsConfig {
	/**
	 * 创建CORS配置源
	 * 配置跨域访问策略，允许前端应用从不同域名访问后端接口
	 * 
	 * @return CorsConfigurationSource CORS配置源实例
	 * @author 椰子皮
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		if (true) {
			// 开发环境：允许所有本地域名（注意：生产环境不要启用）
			configuration.addAllowedOriginPattern("*");
		} else {
			// 生产环境：明确指定允许的域名
			configuration.setAllowedOriginPatterns(Arrays.asList(
				"http://localhost:*",
				"http://127.0.0.1:*",
				"http://localhost:5173",
                    "http://localhost:5174",
				"http://localhost:3000",
				"http://localhost:8080",
				"http://localhost:8081",
				"http://localhost:8082"
			));
		}
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
		configuration.setAllowCredentials(false); // 通配符模式下不能设置credentials
		configuration.setMaxAge(3600L); // 预检请求缓存时间

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
