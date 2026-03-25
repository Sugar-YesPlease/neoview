# neoview-signal-bridge

mediasoup 信令桥接服务，实现 Spring Boot 与 mediasoup-demo server 之间的协议转换。

## 功能说明

```
Spring Boot <--(WebSocket JSON)--> Node Bridge <--(protoo WebSocket)--> mediasoup
```

本服务作为中间层，负责：
- 翻译 protoo 协议消息
- 建立 Spring Boot 与 mediasoup 之间的 WebSocket 通信
- 处理 mediasoup 的 request/notification 消息

## 环境要求

- Node.js 18+
- npm 或 yarn

## 配置

通过环境变量配置：

```bash
# 监听端口（默认 7000）
export LISTEN_PORT=7000

# mediasoup 服务地址
export PROTOO_HOST=your_mediasoup_host
export PROTOO_PORT=4443
export PROTOO_PROTOCOL=wss

# 开发环境可跳过证书校验
export PROTOO_INSECURE=false
```

## 安装运行

```bash
# 安装依赖
npm install

# 启动服务
npm start
```

## 项目结构

```
neoview-signal-bridge/
├── server.js        # 主服务文件
├── package.json     # 依赖配置
└── .gitignore
```

## 相关文档

- [主项目文档](../README.md)
- [mediasoup 官方文档](https://mediasoup.org/)
