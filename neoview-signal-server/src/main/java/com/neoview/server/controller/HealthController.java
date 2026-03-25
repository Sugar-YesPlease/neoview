package com.neoview.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 * 提供服务健康状态检查接口
 * 
 * @author 椰子皮
 */
@RestController
public class HealthController {
	/**
	 * 健康检查接口
	 * 用于检测服务是否正常运行
	 * 
	 * @return String 返回"ok"表示服务健康
	 * @author 椰子皮
	 */
	@GetMapping("/api/health")
	public String health() {
		return "ok";
	}
}
