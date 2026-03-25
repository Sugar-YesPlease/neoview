package com.neoview.server.mapper;

import com.neoview.server.model.MeetingMember;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议成员表 Mapper
 * 提供会议成员数据的数据库访问操作
 * 
 * @author 椰子皮
 */
@Mapper
public interface MeetingMemberMapper extends BaseMapper<MeetingMember> {
}
