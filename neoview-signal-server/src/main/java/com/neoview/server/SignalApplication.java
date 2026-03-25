package com.neoview.server;

import com.neoview.server.config.AliyunNlsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 应用入口：Spring Boot 主启动类
 * 
 * @author 椰子皮
 */
@SpringBootApplication
@EnableConfigurationProperties(AliyunNlsProperties.class)
public class SignalApplication {
	/**
	 * 应用程序主入口方法
	 * 启动Spring Boot应用程序，初始化所有组件和配置
	 * 
	 * @param args 命令行参数，可用于传递启动配置
	 * @author 椰子皮
	 */
	public static void main(String[] args) {
		SpringApplication.run(SignalApplication.class, args);
	}
}
