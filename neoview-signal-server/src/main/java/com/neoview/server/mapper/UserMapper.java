package com.neoview.server.mapper;

import com.neoview.server.model.User;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper
 * 提供用户数据的数据库访问操作
 * 
 * @author 椰子皮
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
