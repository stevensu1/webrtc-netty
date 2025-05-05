package com.example.webrtc.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * WebRTC信令消息模型
 * 用于在服务器和客户端之间传递WebRTC信令
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignalingMessage {
    // 消息类型: join, leave, offer, answer, ice_candidate, error等
    private String type;
    
    // 发送者用户ID
    private String userId;
    
    // 目标用户ID
    private String targetUserId;
    
    // 房间ID
    private String roomId;
    
    // 消息数据，根据消息类型不同而不同
    // - 对于offer/answer: SDP信息
    // - 对于ice_candidate: ICE候选信息
    // - 对于error: 错误信息
    private Object data;
    
    public SignalingMessage() {
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getTargetUserId() {
        return targetUserId;
    }
    
    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }
    
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return "SignalingMessage{" +
                "type='" + type + '\'' +
                ", userId='" + userId + '\'' +
                ", targetUserId='" + targetUserId + '\'' +
                ", roomId='" + roomId + '\'' +
                ", data=" + data +
                '}';
    }
}