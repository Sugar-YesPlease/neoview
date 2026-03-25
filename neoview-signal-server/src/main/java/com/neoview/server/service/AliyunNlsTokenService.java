package com.neoview.server.service;



import com.alibaba.nls.client.AccessToken;
import com.neoview.server.config.AliyunNlsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;



/**
 * 阿里云NLS令牌服务
 * 负责获取和缓存阿里云智能语音服务的访问令牌
 * 
 * @author 椰子皮
 */
@Service
public class AliyunNlsTokenService {
	private static final Logger logger = LoggerFactory.getLogger(AliyunNlsTokenService.class);
	private static final long TOKEN_EARLY_EXPIRE_MILLIS = 60_000;

	private final AliyunNlsProperties properties;
	private volatile String cachedToken;
	private volatile long cachedExpireTimeMillis;

	/**
	 * 构造函数
	 * 注入阿里云NLS配置属性
	 * 
	 * @param properties 阿里云NLS配置属性
	 * @author 椰子皮
	 */
	public AliyunNlsTokenService(AliyunNlsProperties properties) {
		this.properties = properties;
	}

	/**
	 * 获取阿里云智能语音服务的访问令牌
	 * 该方法会检查缓存中的令牌是否仍然有效，如果无效或即将过期则重新获取新的令牌。
	 * 使用同步机制确保多线程环境下令牌获取的安全性。
	 * 
	 * @return 阿里云NLS访问令牌字符串
	 * @throws IllegalStateException 当AccessKey未配置或令牌获取失败时抛出
	 * @author 椰子皮
	 */
	public synchronized String fetchToken() {
		String accessKeyId = resolveAccessKeyId();
		String accessKeySecret = resolveAccessKeySecret();

		if (isBlank(accessKeyId) || isBlank(accessKeySecret)) {
			throw new IllegalStateException("阿里云 NLS AccessKey 未配置");
		}
		if (isMasked(accessKeyId) || isMasked(accessKeySecret)) {
			throw new IllegalStateException("阿里云 NLS AccessKey 配置为脱敏值，请填写完整的 AK/SK");
		}

		if (cachedToken != null && !isExpiringSoon()) {
			return cachedToken;
		}

		try {
			logger.info("NLS AccessKey 来源: accessKeyId={}, secret={}", mask(accessKeyId), mask(accessKeySecret));
			AccessToken accessToken = new AccessToken(accessKeyId, accessKeySecret);
			accessToken.apply();
			cachedToken = accessToken.getToken();
			cachedExpireTimeMillis = accessToken.getExpireTime() * 1000L;
			if (isBlank(cachedToken)) {
				throw new IllegalStateException("阿里云 NLS Token 获取为空");
			}
			return cachedToken;
		} catch (Exception ex) {
			logger.error("获取阿里云 NLS Token 失败", ex);
			throw new IllegalStateException("阿里云 NLS Token 获取失败: " + ex.getMessage());
		}
	}

	/**
	 * 检查令牌是否即将过期
	 * 
	 * @return boolean 即将过期返回true，否则返回false
	 * @author 椰子皮
	 */
	private boolean isExpiringSoon() {
		long now = System.currentTimeMillis();
		return cachedExpireTimeMillis <= now + TOKEN_EARLY_EXPIRE_MILLIS;
	}

	/**
	 * 解析AccessKeyId
	 * 优先从配置文件获取，其次从环境变量获取
	 * 
	 * @return String AccessKeyId
	 * @author 椰子皮
	 */
	private String resolveAccessKeyId() {
		return firstNonBlank(
				properties.getAccessKeyId(),
				System.getenv("ALIYUN_NLS_ACCESS_KEY_ID"),
				System.getenv("ALIYUN_AK_ID")
		);
	}

	/**
	 * 解析AccessKeySecret
	 * 优先从配置文件获取，其次从环境变量获取
	 * 
	 * @return String AccessKeySecret
	 * @author 椰子皮
	 */
	private String resolveAccessKeySecret() {
		return firstNonBlank(
				properties.getAccessKeySecret(),
				System.getenv("ALIYUN_NLS_ACCESS_KEY_SECRET"),
				System.getenv("ALIYUN_AK_SECRET")
		);
	}

	/**
	 * 获取第一个非空字符串
	 * 
	 * @param values 字符串数组
	 * @return String 第一个非空字符串，全为空则返回null
	 * @author 椰子皮
	 */
	private String firstNonBlank(String... values) {
		for (String v : values) {
			if (!isBlank(v)) {
				return v;
			}
		}
		return null;
	}

	/**
	 * 检查字符串是否为空
	 * 
	 * @param s 待检查字符串
	 * @return boolean 为空或空白返回true，否则返回false
	 * @author 椰子皮
	 */
	private boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	/**
	 * 检查字符串是否为脱敏值
	 * 
	 * @param s 待检查字符串
	 * @return boolean 包含脱敏标记返回true，否则返回false
	 * @author 椰子皮
	 */
	private boolean isMasked(String s) {
		return s != null && s.contains("****");
	}

	/**
	 * 对字符串进行脱敏处理
	 * 保留前4位和后4位，中间用****替换
	 * 
	 * @param s 原始字符串
	 * @return String 脱敏后的字符串
	 * @author 椰子皮
	 */
	private String mask(String s) {
		if (s == null || s.length() <= 8) {
			return s;
		}
		return s.substring(0, 4) + "****" + s.substring(s.length() - 4);
	}
}
