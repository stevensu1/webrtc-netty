package com.example.webrtc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.webrtc.handler.HttpRequestHandler;
import com.example.webrtc.handler.WebSocketFrameHandler;
import com.example.webrtc.service.SignalingService;

/**
 * WebRTC信令服务器主类
 * 使用Netty实现WebSocket服务器，处理WebRTC信令
 */
public class WebRTCServer {
    private static final Logger logger = LoggerFactory.getLogger(WebRTCServer.class);
    
    private final int port;
    private final SignalingService signalingService;
    
    public WebRTCServer(int port) {
        this.port = port;
        this.signalingService = new SignalingService();
    }
    
    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                            new HttpServerCodec(),
                            new ChunkedWriteHandler(),
                            new HttpObjectAggregator(65536),
                            new HttpRequestHandler("/ws"),
                            new WebSocketServerProtocolHandler("/ws", null, true),
                            new WebSocketFrameHandler(signalingService)
                        );
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            ChannelFuture f = b.bind(port).sync();
            logger.info("WebRTC信令服务器启动，监听端口: " + port);
            
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
    
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new WebRTCServer(port).run();
    }
}