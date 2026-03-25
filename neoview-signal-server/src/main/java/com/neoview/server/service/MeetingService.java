package com.neoview.server.service;

import com.neoview.server.mapper.MeetingMapper;
import com.neoview.server.mapper.MeetingMemberMapper;
import com.neoview.server.mapper.MeetingRecordMapper;
import com.neoview.server.model.Meeting;
import com.neoview.server.model.MeetingMember;
import com.neoview.server.model.MeetingRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import static com.mybatisflex.core.query.QueryWrapper.create;


/**
 * 会议服务：创建、加入、离开、历史记录
 * 提供会议相关的业务逻辑处理
 * 
 * @author 椰子皮
 */
@Service
public class MeetingService {

	private final MeetingMapper meetingMapper;
	private final MeetingMemberMapper meetingMemberMapper;
	private final MeetingRecordMapper meetingRecordMapper;

	/**
	 * 构造函数
	 * 注入会议相关的Mapper
	 * 
	 * @param meetingMapper 会议数据访问层
	 * @param meetingMemberMapper 会议成员数据访问层
	 * @param meetingRecordMapper 会议记录数据访问层
	 * @author 椰子皮
	 */
	public MeetingService(MeetingMapper meetingMapper,
							MeetingMemberMapper meetingMemberMapper,
							MeetingRecordMapper meetingRecordMapper) {
		this.meetingMapper = meetingMapper;
		this.meetingMemberMapper = meetingMemberMapper;
		this.meetingRecordMapper = meetingRecordMapper;
	}

	/**
	 * 创建会议
	 * 创建新的会议房间并记录创建日志
	 * 
	 * @param hostUserId 主持人用户ID
	 * @param title 会议标题
	 * @return Meeting 创建的会议对象
	 * @author 椰子皮
	 */
	public Meeting createMeeting(String hostUserId, String title) {
		Meeting meeting = new Meeting();
		meeting.setId(UUID.randomUUID().toString());
		meeting.setTitle(title);
		meeting.setRoomId("room-" + UUID.randomUUID());
		meeting.setHostUserId(hostUserId);
		meeting.setStatus(1);
		meeting.setStartTime(LocalDateTime.now());
		meeting.setCreatedAt(LocalDateTime.now());
		meeting.setUpdatedAt(LocalDateTime.now());

		meetingMapper.insert(meeting);

		record(meeting.getId(), hostUserId, "CREATE", "创建会议");
		return meeting;
	}

	/**
	 * 根据房间ID查询会议
	 * 
	 * @param roomId 房间ID
	 * @return Meeting 查询到的会议对象，不存在则返回null
	 * @author 椰子皮
	 */
	public Meeting findByRoomId(String roomId) {
		QueryWrapper query = create()
			.select()
			.from(Meeting.class)
			.where(Meeting::getRoomId).eq(roomId);

		return meetingMapper.selectOneByQuery(query);
	}

	/**
	 * 加入会议
	 * 将用户加入指定会议并记录加入日志
	 * 
	 * @param meeting 会议对象
	 * @param userId 用户ID
	 * @param peerId WebRTC对等连接ID
	 * @return MeetingMember 创建的会议成员对象
	 * @author 椰子皮
	 */
	public MeetingMember joinMeeting(Meeting meeting, UUID userId, String peerId) {
		MeetingMember member = new MeetingMember();
		member.setId(UUID.randomUUID().toString());
		member.setMeetingId(meeting.getId());
		member.setUserId(userId.toString());
		member.setPeerId(peerId);
		member.setRole("member");
		member.setJoinedAt(LocalDateTime.now());
		member.setStatus(1);

		meetingMemberMapper.insert(member);
		record(meeting.getId(), userId.toString(), "JOIN", "加入会议");
		return member;
	}

	/**
	 * 离开会议
	 * 更新用户在会议中的状态为已离开，并记录离开日志
	 * 
	 * @param meeting 会议对象
	 * @param userId 用户ID
	 * @author 椰子皮
	 */
	public void leaveMeeting(Meeting meeting, UUID userId) {
		QueryWrapper query = create()
			.select()
			.from(MeetingMember.class)
			.where(MeetingMember::getMeetingId).eq(meeting.getId())
			.and(MeetingMember::getUserId).eq(userId)
			.and(MeetingMember::getStatus).eq(1);

		MeetingMember member = meetingMemberMapper.selectOneByQuery(query);
		if (member != null) {
			member.setLeftAt(LocalDateTime.now());
			member.setStatus(2);
			meetingMemberMapper.update(member);
			record(meeting.getId(), userId.toString(), "LEAVE", "离开会议");
		}
	}

	/**
	 * 查询指定用户主持的会议列表
	 * 
	 * @param hostUserId 主持人用户ID
	 * @return List<Meeting> 会议列表，按创建时间倒序排列
	 * @author 椰子皮
	 */
	public List<Meeting> listByHost(UUID hostUserId) {
		QueryWrapper query = create()
			.select()
			.from(Meeting.class)
			.where(Meeting::getHostUserId).eq(hostUserId)
			.orderBy(Meeting::getCreatedAt).desc();

		return meetingMapper.selectListByQuery(query);
	}

	/**
	 * 记录会议操作日志
	 * 创建会议操作记录并持久化
	 * 
	 * @param meetingId 会议ID
	 * @param userId 用户ID
	 * @param action 操作类型
	 * @param detail 操作详情
	 * @author 椰子皮
	 */
	private void record(String meetingId, String userId, String action, String detail) {
		MeetingRecord record = new MeetingRecord();
		record.setId(UUID.randomUUID().toString());
		record.setMeetingId(meetingId);
		record.setUserId(userId);
		record.setAction(action);
		record.setActionTime(LocalDateTime.now());
		record.setDetail(detail);

		meetingRecordMapper.insert(record);
	}
}
