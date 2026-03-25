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
 * 会议记录实体：对应 tbl_meeting_record
 * 
 * @author 椰子皮
 */
@Table("tbl_meeting_record")
@Data
public class MeetingRecord {
	/** 主键 */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.uuid)
	private String id;

	/** 会议 ID */
	@Column("meeting_id")
	private String meetingId;

	/** 用户 ID */
	@Column("user_id")
	private String userId;

	/** 操作类型：JOIN/LEAVE/END 等 */
	private String action;

	/** 操作时间 */
	@Column("action_time")
	private LocalDateTime actionTime;

    /** 额外备注 */
    private String detail;
}
