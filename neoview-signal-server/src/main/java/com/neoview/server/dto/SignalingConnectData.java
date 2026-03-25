package com.neoview.server.dto;

/**
 * 前端发起 connect 时的 data 结构
 * 用于WebSocket连接时传递房间信息
 * 
 * @author 椰子皮
 */
public class SignalingConnectData {
	/** 房间ID */
	public String roomId;
}
