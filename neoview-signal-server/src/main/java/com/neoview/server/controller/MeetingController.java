package com.neoview.server.controller;

import com.neoview.server.dto.MeetingCreateRequest;
import com.neoview.server.dto.MeetingJoinRequest;
import com.neoview.server.model.Meeting;
import com.neoview.server.model.MeetingMember;
import com.neoview.server.service.MeetingService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 会议管理接口
 * 处理会议创建、加入、离开等操作的HTTP请求
 * 
 * @author 椰子皮
 */
@RestController
@RequestMapping("/api/meetings")
public class MeetingController {
	private final MeetingService meetingService;

	/**
	 * 构造函数
	 * 注入会议服务
	 * 
	 * @param meetingService 会议服务
	 * @author 椰子皮
	 */
	public MeetingController(MeetingService meetingService) {
		this.meetingService = meetingService;
	}

	/**
	 * 创建会议接口
	 * 创建一个新的会议房间
	 * 
	 * @param request 创建会议请求体，包含会议标题
	 * @param authentication 认证信息，用于获取当前用户ID
	 * @return ResponseEntity<Meeting> 返回创建的会议信息
	 * @author 椰子皮
	 */
	@PostMapping
	public ResponseEntity<Meeting> create(@RequestBody MeetingCreateRequest request,
									Authentication authentication) {
		UUID userId = UUID.fromString(authentication.getName());
		Meeting meeting = meetingService.createMeeting(userId.toString(), request.title);
		return ResponseEntity.ok(meeting);
	}

	/**
	 * 查询会议列表接口
	 * 获取当前用户主持的所有会议
	 * 
	 * @param authentication 认证信息，用于获取当前用户ID
	 * @return ResponseEntity<List<Meeting>> 返回会议列表
	 * @author 椰子皮
	 */
	@GetMapping
	public ResponseEntity<List<Meeting>> list(Authentication authentication) {
		UUID userId = UUID.fromString(authentication.getName());
		return ResponseEntity.ok(meetingService.listByHost(userId));
	}

	/**
	 * 加入会议接口
	 * 用户加入指定的会议房间
	 * 
	 * @param request 加入会议请求体，包含房间ID
	 * @param authentication 认证信息，用于获取当前用户ID
	 * @return ResponseEntity<MeetingMember> 返回会议成员信息，会议不存在返回404
	 * @author 椰子皮
	 */
	@PostMapping("/join")
	public ResponseEntity<MeetingMember> join(@RequestBody MeetingJoinRequest request,
									Authentication authentication) {
		UUID userId = UUID.fromString(authentication.getName());
		Meeting meeting = meetingService.findByRoomId(request.roomId);
		if (meeting == null) {
			return ResponseEntity.notFound().build();
		}

		// peerId 用 UUID 生成，保证唯一
		String peerId = "peer-" + UUID.randomUUID();
		MeetingMember member = meetingService.joinMeeting(meeting, userId, peerId);
		return ResponseEntity.ok(member);
	}

	/**
	 * 离开会议接口
	 * 用户离开指定的会议房间
	 * 
	 * @param request 离开会议请求体，包含房间ID
	 * @param authentication 认证信息，用于获取当前用户ID
	 * @return ResponseEntity<Void> 成功返回200，会议不存在返回404
	 * @author 椰子皮
	 */
	@PostMapping("/leave")
	public ResponseEntity<Void> leave(@RequestBody MeetingJoinRequest request,
									Authentication authentication) {
		UUID userId = UUID.fromString(authentication.getName());
		Meeting meeting = meetingService.findByRoomId(request.roomId);
		if (meeting == null) {
			return ResponseEntity.notFound().build();
		}

		meetingService.leaveMeeting(meeting, userId);
		return ResponseEntity.ok().build();
	}
}
