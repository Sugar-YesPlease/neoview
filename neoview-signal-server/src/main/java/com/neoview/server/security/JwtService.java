package com.neoview.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * JWT 相关工具：生成与校验 token
 * 提供JWT令牌的生成和解析功能
 * 
 * @author 椰子皮
 */
@Service
public class JwtService {
	private final SecretKey signingKey;
	private final long expireMinutes;

	/**
	 * 构造函数
	 * 初始化JWT签名密钥和过期时间配置
	 * 
	 * @param secret JWT签名密钥（Base64编码或原始字符串）
	 * @param expireMinutes 令牌过期时间（分钟）
	 * @author 椰子皮
	 */
	public JwtService(@Value("${app.jwt.secret}") String secret,
					 @Value("${app.jwt.expire-minutes}") long expireMinutes) {
		String encodedSecret = encodeIfNeeded(secret);
		byte[] keyBytes = Decoders.BASE64.decode(encodedSecret);
		
		// 确保密钥长度至少为256位（32字节）
		if (keyBytes.length < 32) {
			// 如果密钥太短，使用Keys.secretKeyFor生成安全密钥
			this.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
		} else {
			this.signingKey = Keys.hmacShaKeyFor(keyBytes);
		}
		this.expireMinutes = expireMinutes;
	}

	/**
	 * 生成JWT令牌
	 * 根据用户ID和用户名创建带过期时间的JWT令牌
	 * 
	 * @param userId 用户ID
	 * @param username 用户名
	 * @return String 生成的JWT令牌字符串
	 * @author 椰子皮
	 */
	public String generateToken(String userId, String username) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(expireMinutes * 60);

		return Jwts.builder()
			.setSubject(userId)
			.claim("username", username)
			.setIssuedAt(Date.from(now))
			.setExpiration(Date.from(exp))
			.signWith(signingKey, SignatureAlgorithm.HS512)
			.compact();
	}

	/**
	 * 解析JWT令牌
	 * 验证并提取JWT令牌中的声明信息
	 * 
	 * @param token JWT令牌字符串
	 * @return Claims 令牌中的声明信息
	 * @author 椰子皮
	 */
	public Claims parse(String token) {
		return Jwts.parserBuilder()
			.setSigningKey(signingKey)
			.build()
			.parseClaimsJws(token)
			.getBody();
	}

	/**
	 * 编码密钥（如需要）
	 * 将原始字符串转换为Base64编码格式
	 * 
	 * @param secret 原始密钥字符串
	 * @return String Base64编码后的密钥
	 * @author 椰子皮
	 */
	private String encodeIfNeeded(String secret) {
		// 如果用户直接给了普通字符串，就转成 Base64 形式
		// 注意：生产环境请直接配置一个足够长的 Base64 字符串
		if (secret.matches("^[A-Za-z0-9+/=]+$") && secret.length() >= 32) {
			return secret;
		}
		return java.util.Base64.getEncoder().encodeToString(secret.getBytes());
	}
}
