package com.neoview.server.config;

import com.neoview.server.websocket.SignalingWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket 配置：前端通过 /ws/signaling 连接信令服务
 * 
 * @author 椰子皮
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
	private final SignalingWebSocketHandler signalingWebSocketHandler;

	/**
	 * 构造函数
	 * 注入信令WebSocket处理器
	 * 
	 * @param signalingWebSocketHandler 信令WebSocket处理器
	 * @author 椰子皮
	 */
	public WebSocketConfig(SignalingWebSocketHandler signalingWebSocketHandler) {
		this.signalingWebSocketHandler = signalingWebSocketHandler;
	}

	/**
	 * 注册WebSocket处理器
	 * 配置WebSocket端点和跨域策略
	 * 
	 * @param registry WebSocket处理器注册中心
	 * @author 椰子皮
	 */
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(signalingWebSocketHandler, "/ws/signaling")
			.setAllowedOrigins("*").setAllowedOriginPatterns("*");
	}

	/**
	 * 创建WebSocket容器配置
	 * 配置WebSocket消息缓冲区大小
	 * 
	 * @return ServletServerContainerFactoryBean WebSocket容器配置Bean
	 * @author 椰子皮
	 */
	@Bean
	public ServletServerContainerFactoryBean createWebSocketContainer() {
		ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
		container.setMaxTextMessageBufferSize(2 * 1024 * 1024);
		container.setMaxBinaryMessageBufferSize(2 * 1024 * 1024);
		return container;
	}
}
