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
 * 会议成员实体：对应 tbl_meeting_member
 * 
 * @author 椰子皮
 */
@Table("tbl_meeting_member")
@Data
public class MeetingMember {
	/** 主键 */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.uuid)
	private String id;

	/** 会议 ID */
	@Column("meeting_id")
	private String meetingId;

	/** 用户 ID */
	@Column("user_id")
	private String userId;

	/** 当前用户在该会议中的 peerId（WebRTC 标识） */
	@Column("peer_id")
	private String peerId;

	/** 成员角色 */
	private String role;

	/** 加入时间 */
	@Column("joined_at")
	private LocalDateTime joinedAt;

	/** 离开时间 */
	@Column("left_at")
	private LocalDateTime leftAt;

	/** 状态：1 在会，2 已离开 */
	private Integer status;

}
