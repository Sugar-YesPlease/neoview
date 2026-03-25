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
 * 会议表实体：对应 tbl_meeting
 * 
 * @author 椰子皮
 */
@Table("tbl_meeting")
@Data
public class Meeting {
	/** 主键 */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.uuid)
	private String id;

	/** 会议标题 */
	private String title;

	/** 房间号（前端 join 时使用） */
	@Column("room_id")
	private String roomId;

	/** 主持人用户 ID */
	@Column("host_user_id")
	private String hostUserId;

	/** 状态：1 进行中，2 已结束 */
	private Integer status;

	/** 开始时间 */
	@Column("start_time")
	private LocalDateTime startTime;

	/** 结束时间 */
	@Column("end_time")
	private LocalDateTime endTime;

	/** 创建时间 */
	@Column("created_at")
	private LocalDateTime createdAt;

	/** 更新时间 */
	@Column("updated_at")
	private LocalDateTime updatedAt;

}
