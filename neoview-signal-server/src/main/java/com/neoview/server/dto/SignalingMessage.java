package com.neoview.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 信令消息结构：前端 <-> Spring Boot <-> Node
 * 用于WebSocket通信的消息载体
 * 
 * @author 椰子皮
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignalingMessage {
	/** 消息类型 */
	public String type;
	
	/** 消息ID，用于请求-响应匹配 */
	public String id;
	
	/** 方法名 */
	public String method;
	
	/** 消息数据 */
	public Object data;
	
	/** 响应状态，true表示成功 */
	public Boolean ok;
	
	/** 错误码 */
	public String errorCode;
	
	/** 错误原因 */
	public String errorReason;
}
