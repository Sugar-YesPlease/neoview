package com.neoview.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 阿里云NLS语音服务配置属性
 * 用于配置阿里云智能语音服务的访问凭证和连接参数
 * 
 * @author 椰子皮
 */
@ConfigurationProperties(prefix = "aliyun.nls")
@Data
public class AliyunNlsProperties {
	/** 阿里云AccessKey ID */
	private String accessKeyId;
	
	/** 阿里云AccessKey Secret */
	private String accessKeySecret;
	
	/** 阿里云NLS应用Key */
	private String appKey;
	
	/** NLS服务端点地址 */
	private String endpoint = "https://nls-meta.cn-shanghai.aliyuncs.com";
	
	/** NLS WebSocket连接地址 */
	private String wsUrl = "wss://nls-gateway-cn-shanghai.aliyuncs.com/ws/v1";
}
