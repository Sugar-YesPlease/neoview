# neoview-web

NeoView 前端应用，基于 Vue 3 + Vite 构建，提供视频会议的用户界面和媒体处理能力。

## 功能说明

- 用户登录/注册界面
- 会议大厅（创建/加入会议）
- 会议室界面（视频/音频/屏幕共享）
- 实时语音识别结果展示
- AI 降噪功能

## 环境要求

- Node.js 18+
- npm 或 yarn

## 配置

创建 `.env.local` 文件配置环境变量：

```bash
# API 地址
VITE_API_BASE=https://your-domain.com

# WebSocket 信令地址
VITE_WS_URL=wss://your-domain.com/ws/signaling
```

## 安装运行

```bash
# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

## 项目结构

```
neoview-web/web/
├── src/
│   ├── main.js              # 应用入口
│   ├── App.vue              # 根组件
│   ├── config.js            # 配置
│   ├── components/          # 组件
│   │   └── MeetingRoom.vue  # 会议室组件
│   └── services/            # 服务
│       ├── api.js           # HTTP API
│       ├── signaling.js     # 信令 WebSocket
│       ├── mediasoupSession.js  # mediasoup 会话
│       └── asrStreamer.js   # ASR 音频流
├── public/                  # 静态资源
├── index.html               # 入口 HTML
├── vite.config.js           # Vite 配置
└── package.json             # 依赖配置
```

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 构建工具
- **mediasoup-client** - WebRTC 客户端
- **RNNoise** - AI 降噪

## 相关文档

- [主项目文档](../README.md)
- [Vue 3 文档](https://vuejs.org/)
- [mediasoup-client 文档](https://mediasoup.org/documentation/v3/mediasoup-client/)