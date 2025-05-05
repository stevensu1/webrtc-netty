# 基于Netty和WebRTC的实时通讯系统

这是一个使用Java Netty框架和WebRTC技术构建的实时通讯系统，支持多人视频通话和即时消息传递。

## 功能特点

- 基于Netty的高性能WebSocket信令服务器
- 支持WebRTC音视频实时通讯
- 房间管理机制，支持多房间、多用户
- 完整的信令交换流程（加入/离开房间、SDP交换、ICE候选交换）
- 响应式Web客户端界面

## 技术栈

- **后端**：Java 11+, Netty 4.1.x
- **前端**：HTML5, CSS3, JavaScript (原生WebRTC API)
- **构建工具**：Maven
- **日志**：SLF4J + Logback

## 系统架构

系统分为三个主要模块：

1. **信令服务**：基于Netty的WebSocket服务器，处理客户端连接和信令消息转发
2. **媒体处理**：基于WebRTC的点对点媒体流传输
3. **客户端通信**：Web客户端实现，包括用户界面和WebRTC连接管理

## 快速开始

### 环境要求

- JDK 11或更高版本
- Maven 3.6或更高版本
- 现代浏览器（支持WebRTC，如Chrome、Firefox、Edge等）

### 构建和运行

1. 克隆项目

```bash
git clone [项目URL]
cd webrtc-netty
```

2. 使用Maven构建项目

```bash
mvn clean package
```

3. 运行服务器

```bash
java -jar target/webrtc-netty-1.0-SNAPSHOT-jar-with-dependencies.jar [端口号]
```

如果不指定端口号，默认使用8080端口。

4. 访问Web客户端

打开浏览器，访问 `http://localhost:8080`

### 使用说明

1. 在打开的网页中，输入您的用户ID和房间ID
2. 点击"加入房间"按钮
3. 允许浏览器访问您的摄像头和麦克风
4. 当其他用户加入同一房间时，将自动建立连接并显示其视频
5. 使用界面上的按钮控制麦克风和摄像头
6. 点击"离开房间"按钮结束通话

## 项目结构

```
webrtc-netty/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── webrtc/
│   │   │               ├── handler/           # Netty处理器
│   │   │               ├── model/             # 数据模型
│   │   │               ├── service/           # 业务逻辑
│   │   │               └── WebRTCServer.java  # 主服务器类
│   │   └── resources/
│   │       ├── static/                        # 静态资源
│   │       │   └── index.html                 # Web客户端
│   │       └── logback.xml                    # 日志配置
│   └── test/                                  # 测试代码
└── pom.xml                                    # Maven配置
```

## 扩展和定制

### 添加更多功能

- 文本聊天：在SignalingMessage中添加文本消息类型
- 屏幕共享：使用WebRTC的getDisplayMedia API
- 录制功能：使用MediaRecorder API
- 用户认证：在信令服务中添加认证机制

### 性能优化

- 使用TURN服务器处理NAT穿透问题
- 实现媒体服务器（如Janus或Kurento）进行中继
- 优化Netty配置以支持更多并发连接

## 注意事项

- 在生产环境中，建议使用HTTPS和WSS协议以确保安全性
- 对于公网部署，需要配置STUN/TURN服务器以处理NAT穿透
- 大规模部署时，考虑使用集群架构和负载均衡

## 许可证

[MIT License](LICENSE)