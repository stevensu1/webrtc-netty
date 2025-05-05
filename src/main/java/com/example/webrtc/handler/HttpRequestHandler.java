package com.example.webrtc.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * HTTP请求处理器
 * 处理普通HTTP请求，提供静态资源服务
 * 对于WebSocket请求，转发到WebSocket处理器
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);
    
    private final String wsUri;
    private static final String INDEX_PAGE = "index.html";
    
    public HttpRequestHandler(String wsUri) {
        this.wsUri = wsUri;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 处理WebSocket握手请求
        if (wsUri.equalsIgnoreCase(request.uri())) {
            // 将请求传递给下一个处理器（WebSocketServerProtocolHandler）
            ctx.fireChannelRead(request.retain());
            return;
        }
        
        // 处理HTTP请求
        if (request.method() != HttpMethod.GET) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }
        
        String uri = request.uri();
        if ("/".equals(uri)) {
            uri = "/" + INDEX_PAGE;
        }
        
        // 尝试提供静态资源
        try {
            URL resourceUrl = getClass().getClassLoader().getResource("static" + uri);
            if (resourceUrl == null) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }
            
            File file = new File(resourceUrl.toURI());
            if (file.isHidden() || !file.exists()) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }
            
            if (!file.isFile()) {
                sendError(ctx, HttpResponseStatus.FORBIDDEN);
                return;
            }
            
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length();
            
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            HttpUtil.setContentLength(response, fileLength);
            
            // 设置Content-Type
            String contentType = "text/html";
            if (uri.endsWith(".css")) {
                contentType = "text/css";
            } else if (uri.endsWith(".js")) {
                contentType = "application/javascript";
            } else if (uri.endsWith(".json")) {
                contentType = "application/json";
            }
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.KEEP_ALIVE);
            response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            
            // 读取文件内容到ByteBuf
            byte[] buffer = new byte[(int) fileLength];
            raf.readFully(buffer);
            response.content().writeBytes(buffer);
            raf.close();
            
            // 写入响应并关闭连接
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.KEEP_ALIVE);
                ctx.writeAndFlush(response);
            } else {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        } catch (URISyntaxException e) {
            logger.error("资源URI语法错误", e);
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            logger.error("处理HTTP请求异常", e);
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status,
                io.netty.buffer.Unpooled.copiedBuffer("错误: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("HTTP处理器异常", cause);
        ctx.close();
    }
}