package com.example.webrtc.handler;

import com.example.webrtc.model.SignalingMessage;
import com.example.webrtc.service.SignalingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket帧处理器
 * 处理WebRTC信令消息，包括：
 * - 会话创建和管理
 * - ICE候选交换
 * - SDP提议和应答交换
 */
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketFrameHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SignalingService signalingService;
    
    public WebSocketFrameHandler(SignalingService signalingService) {
        this.signalingService = signalingService;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
        String message = frame.text();
        logger.debug("收到WebSocket消息: {}", message);
        
        try {
            // 解析信令消息
            SignalingMessage signalingMessage = objectMapper.readValue(message, SignalingMessage.class);
            
            // 处理不同类型的信令消息
            switch (signalingMessage.getType()) {
                case "join":
                    signalingService.handleJoinRoom(ctx.channel(), signalingMessage);
                    break;
                case "leave":
                    signalingService.handleLeaveRoom(ctx.channel(), signalingMessage);
                    break;
                case "offer":
                    signalingService.handleOffer(ctx.channel(), signalingMessage);
                    break;
                case "answer":
                    signalingService.handleAnswer(ctx.channel(), signalingMessage);
                    break;
                case "ice_candidate":
                    signalingService.handleIceCandidate(ctx.channel(), signalingMessage);
                    break;
                default:
                    logger.warn("未知的信令消息类型: {}", signalingMessage.getType());
            }
        } catch (Exception e) {
            logger.error("处理WebSocket消息异常", e);
            // 发送错误消息给客户端
            SignalingMessage errorMessage = new SignalingMessage();
            errorMessage.setType("error");
            errorMessage.setData("消息处理错误: " + e.getMessage());
            ctx.channel().writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(errorMessage)));
        }
    }
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        logger.info("客户端连接: {}", ctx.channel().remoteAddress());
        signalingService.addClient(ctx.channel());
    }
    
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        logger.info("客户端断开连接: {}", ctx.channel().remoteAddress());
        signalingService.removeClient(ctx.channel());
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("WebSocket处理器异常", cause);
        ctx.close();
    }
}