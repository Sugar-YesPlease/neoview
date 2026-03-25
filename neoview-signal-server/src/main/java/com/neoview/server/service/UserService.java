package com.neoview.server.service;

import com.neoview.server.mapper.UserMapper;
import com.neoview.server.model.User;
import com.mybatisflex.core.query.QueryWrapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.mybatisflex.core.query.QueryWrapper.create;

/**
 * 用户服务：注册、查询、密码校验
 * 提供用户相关的业务逻辑处理
 * 
 * @author 椰子皮
 */
@Service
public class UserService {
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 构造函数
	 * 注入用户Mapper和密码编码器
	 * 
	 * @param userMapper 用户数据访问层
	 * @param passwordEncoder 密码编码器
	 * @author 椰子皮
	 */
	public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 用户注册
	 * 创建新用户账户，对密码进行加密存储
	 * 
	 * @param username 用户名
	 * @param password 原始密码
	 * @param displayName 显示名称
	 * @return User 创建的用户对象
	 * @throws IllegalArgumentException 当用户名已存在时抛出
	 * @author 椰子皮
	 */
	public User register(String username, String password, String displayName) {
		User exist = findByUsername(username);
		if (exist != null) {
			throw new IllegalArgumentException("用户名已存在");
		}

		User user = new User();
		user.setId(UUID.randomUUID().toString());
		user.setUsername(username);
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setDisplayName(displayName);
		user.setStatus(1);
		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());

		userMapper.insert(user);
		return user;
	}

	/**
	 * 根据用户名查询用户
	 * 
	 * @param username 用户名
	 * @return User 查询到的用户对象，不存在则返回null
	 * @author 椰子皮
	 */
	public User findByUsername(String username) {
		QueryWrapper query = create()
			.select()
			.from(User.class)
			.where(User::getUsername).eq(username);

		return userMapper.selectOneByQuery(query);
	}

	/**
	 * 根据用户ID查询用户
	 * 
	 * @param userId 用户ID
	 * @return User 查询到的用户对象，不存在则返回null
	 * @author 椰子皮
	 */
	public User findById(String userId) {
		return userMapper.selectOneById(userId);
	}

	/**
	 * 验证用户密码
	 * 比对原始密码与存储的密码哈希是否匹配
	 * 
	 * @param user 用户对象
	 * @param rawPassword 原始密码
	 * @return boolean 密码匹配返回true，否则返回false
	 * @author 椰子皮
	 */
	public boolean verifyPassword(User user, String rawPassword) {
		return passwordEncoder.matches(rawPassword, user.getPasswordHash());
	}
}
