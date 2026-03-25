package com.neoview.server.config;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置：用于缓存用户状态或临时信令数据
 * 
 * @author 椰子皮
 */
@Configuration
public class RedisConfig {
	/**
	 * 创建Redis模板实例
	 * 配置Redis的序列化策略，使用FastJSON替代JDK默认序列化
	 * 
	 * @param factory Redis连接工厂，由Spring自动注入
	 * @return RedisTemplate<String, Object> 配置好的Redis模板实例
	 * @author 椰子皮
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);

        // 1. Key和HashKey使用String序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);

        // 2. Value和HashValue使用FastJSON序列化（核心：替代JDK序列化）
        GenericFastJsonRedisSerializer fastJsonSerializer = new GenericFastJsonRedisSerializer();
        redisTemplate.setValueSerializer(fastJsonSerializer);
        redisTemplate.setHashValueSerializer(fastJsonSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
	}

}
