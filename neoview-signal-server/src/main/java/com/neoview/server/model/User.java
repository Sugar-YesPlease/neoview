package com.neoview.server.model;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户表实体：对应 tbl_user
 * 
 * @author 椰子皮
 */
@Table("tbl_user")
@Data
public class User {
	/** 主键 */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.uuid)
	private String id;

	/** 登录用户名（唯一） */
	private String username;

	/** 密码哈希（不可明文存储） */
	@Column("password_hash")
	private String passwordHash;

	/** 显示昵称 */
	@Column("display_name")
	private String displayName;

	/** 状态：1 正常，0 禁用 */
	private Integer status;

	/** 创建时间 */
	@Column("created_at")
	private LocalDateTime createdAt;

	/** 更新时间 */
	@Column("updated_at")
	private LocalDateTime updatedAt;
}
