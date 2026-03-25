package com.neoview.server.mapper;

import com.neoview.server.model.Meeting;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议表 Mapper
 * 提供会议数据的数据库访问操作
 * 
 * @author 椰子皮
 */
@Mapper
public interface MeetingMapper extends BaseMapper<Meeting> {
}
