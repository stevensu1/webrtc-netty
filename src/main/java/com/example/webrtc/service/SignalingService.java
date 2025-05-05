package com.example.webrtc.service;

import com.example.webrtc.model.SignalingMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC信令服务
 * 负责管理客户端连接、房间和信令消息转发
 */
public class SignalingService {
    private static final Logger logger = LoggerFactory.getLogger(SignalingService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 所有已连接的客户端
    private final ChannelGroup allClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    
    // 用户ID到Channel的映射
    private final Map<String, Channel> userChannels = new ConcurrentHashMap<>();
    
    // Channel到用户ID的映射
    private final Map<Channel, String> channelUsers = new ConcurrentHashMap<>();
    
    // 房间ID到房间成员的映射
    private final Map<String, ChannelGroup> rooms = new ConcurrentHashMap<>();
    
    // 用户所在的房间
    private final Map<String, String> userRooms = new ConcurrentHashMap<>();
    
    /**
     * 添加新客户端连接
     */
    public void addClient(Channel channel) {
        allClients.add(channel);
    }
    
    /**
     * 移除客户端连接
     */
    public void removeClient(Channel channel) {
        allClients.remove(channel);
        
        // 获取用户ID
        String userId = channelUsers.get(channel);
        if (userId != null) {
            // 从房间中移除用户
            String roomId = userRooms.get(userId);
            if (roomId != null) {
                leaveRoom(userId, roomId);
            }
            
            // 清理映射
            userChannels.remove(userId);
            channelUsers.remove(channel);
        }
    }
    
    /**
     * 处理加入房间请求
     */
    public void handleJoinRoom(Channel channel, SignalingMessage message) throws Exception {
        String userId = message.getUserId();
        String roomId = message.getRoomId();
        
        if (userId == null || roomId == null) {
            throw new IllegalArgumentException("用户ID和房间ID不能为空");
        }
        
        // 记录用户ID和Channel的映射
        userChannels.put(userId, channel);
        channelUsers.put(channel, userId);
        
        // 加入房间
        joinRoom(userId, roomId);
        
        // 通知用户成功加入房间
        SignalingMessage response = new SignalingMessage();
        response.setType("joined");
        response.setUserId(userId);
        response.setRoomId(roomId);
        
        // 获取房间中的其他用户
        ChannelGroup roomChannels = rooms.get(roomId);
        if (roomChannels != null) {
            response.setData(roomChannels.size());
        }
        
        channel.writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(response)));
        
        // 通知房间中的其他用户有新用户加入
        notifyRoomUserJoined(userId, roomId);
    }
    
    /**
     * 处理离开房间请求
     */
    public void handleLeaveRoom(Channel channel, SignalingMessage message) throws Exception {
        String userId = message.getUserId();
        String roomId = message.getRoomId();
        
        if (userId == null || roomId == null) {
            throw new IllegalArgumentException("用户ID和房间ID不能为空");
        }
        
        // 离开房间
        leaveRoom(userId, roomId);
        
        // 通知用户成功离开房间
        SignalingMessage response = new SignalingMessage();
        response.setType("left");
        response.setUserId(userId);
        response.setRoomId(roomId);
        channel.writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(response)));
    }
    
    /**
     * 处理SDP提议
     */
    public void handleOffer(Channel channel, SignalingMessage message) throws Exception {
        forwardMessageToUser(message.getTargetUserId(), message);
    }
    
    /**
     * 处理SDP应答
     */
    public void handleAnswer(Channel channel, SignalingMessage message) throws Exception {
        forwardMessageToUser(message.getTargetUserId(), message);
    }
    
    /**
     * 处理ICE候选
     */
    public void handleIceCandidate(Channel channel, SignalingMessage message) throws Exception {
        forwardMessageToUser(message.getTargetUserId(), message);
    }
    
    /**
     * 加入房间
     */
    private void joinRoom(String userId, String roomId) {
        // 获取用户的Channel
        Channel userChannel = userChannels.get(userId);
        if (userChannel == null) {
            return;
        }
        
        // 获取或创建房间
        ChannelGroup roomChannels = rooms.computeIfAbsent(roomId,
                k -> new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
        
        // 将用户添加到房间
        roomChannels.add(userChannel);
        
        // 记录用户所在的房间
        userRooms.put(userId, roomId);
        
        logger.info("用户 {} 加入房间 {}, 当前房间人数: {}", userId, roomId, roomChannels.size());
    }
    
    /**
     * 离开房间
     */
    private void leaveRoom(String userId, String roomId) {
        // 获取用户的Channel
        Channel userChannel = userChannels.get(userId);
        if (userChannel == null) {
            return;
        }
        
        // 获取房间
        ChannelGroup roomChannels = rooms.get(roomId);
        if (roomChannels == null) {
            return;
        }
        
        // 将用户从房间中移除
        roomChannels.remove(userChannel);
        
        // 如果房间为空，则删除房间
        if (roomChannels.isEmpty()) {
            rooms.remove(roomId);
            logger.info("房间 {} 已空，已删除", roomId);
        } else {
            // 通知房间中的其他用户有用户离开
            notifyRoomUserLeft(userId, roomId);
        }
        
        // 移除用户所在的房间记录
        userRooms.remove(userId);
        
        logger.info("用户 {} 离开房间 {}", userId, roomId);
    }
    
    /**
     * 通知房间中的其他用户有新用户加入
     */
    private void notifyRoomUserJoined(String userId, String roomId) {
        try {
            ChannelGroup roomChannels = rooms.get(roomId);
            if (roomChannels == null) {
                return;
            }
            
            SignalingMessage notification = new SignalingMessage();
            notification.setType("user_joined");
            notification.setUserId(userId);
            notification.setRoomId(roomId);
            
            String message = objectMapper.writeValueAsString(notification);
            
            // 向房间中除了新加入用户外的所有用户发送通知
            Channel userChannel = userChannels.get(userId);
            for (Channel channel : roomChannels) {
                if (channel != userChannel) {
                    channel.writeAndFlush(new TextWebSocketFrame(message));
                }
            }
        } catch (Exception e) {
            logger.error("通知用户加入房间失败", e);
        }
    }
    
    /**
     * 通知房间中的其他用户有用户离开
     */
    private void notifyRoomUserLeft(String userId, String roomId) {
        try {
            ChannelGroup roomChannels = rooms.get(roomId);
            if (roomChannels == null) {
                return;
            }
            
            SignalingMessage notification = new SignalingMessage();
            notification.setType("user_left");
            notification.setUserId(userId);
            notification.setRoomId(roomId);
            
            String message = objectMapper.writeValueAsString(notification);
            
            // 向房间中的所有用户发送通知
            roomChannels.writeAndFlush(new TextWebSocketFrame(message));
        } catch (Exception e) {
            logger.error("通知用户离开房间失败", e);
        }
    }
    
    /**
     * 转发消息给指定用户
     */
    private void forwardMessageToUser(String targetUserId, SignalingMessage message) throws Exception {
        Channel targetChannel = userChannels.get(targetUserId);
        if (targetChannel == null) {
            logger.warn("目标用户 {} 不存在或未连接", targetUserId);
            return;
        }
        
        targetChannel.writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(message)));
    }
}