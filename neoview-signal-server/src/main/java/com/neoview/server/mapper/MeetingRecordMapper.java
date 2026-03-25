package com.neoview.server.mapper;

import com.neoview.server.model.MeetingRecord;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议记录表 Mapper
 * 提供会议记录数据的数据库访问操作
 * 
 * @author 椰子皮
 */
@Mapper
public interface MeetingRecordMapper extends BaseMapper<MeetingRecord> {
}
