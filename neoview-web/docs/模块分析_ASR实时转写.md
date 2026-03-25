# ASR实时转写 模块分析

## 1. 功能概述 (Functional Overview)
该模块负责会议语音的实时转写：从本地麦克风采集音频、降采样编码、分片上行到信令通道、接收识别结果并更新右侧转写面板。

## 2. 页面跳转流程 (Page Transition Flow)
```mermaid
stateDiagram-v2
    state "未启动ASR" as Idle
    state "已启动ASR" as Started
    state "音频发送中" as Streaming
    state "结果回填中" as Rendering
    state "已停止ASR" as Stopped

    Idle --> Started : "满足入会与麦克风条件"
    Started --> Streaming : "发送asrStart与asrAudio"
    Streaming --> Rendering : "收到asrResult"
    Rendering --> Streaming : "继续分片上传"
    Streaming --> Stopped : "停止ASR或离会"
```

## 3. 接口清单 (API List)
| Interface Description | URI | Method | Parameter Description | Code Reference |
| :--- | :--- | :--- | :--- | :--- |
| 启动识别会话 | `type: asrStart` | WS Send | `roomId, userId, peerId, displayName` | `src/services/asrStreamer.js` + `src/services/mediasoupSession.js` |
| 上行音频分片 | `type: asrAudio` | WS Send | `roomId, seq, audioBase64` | `src/services/asrStreamer.js` + `src/services/mediasoupSession.js` |
| 停止识别会话 | `type: asrStop` | WS Send | `roomId, userId, peerId` | `src/services/asrStreamer.js` + `src/services/mediasoupSession.js` |
| 接收识别结果 | `type: asrResult` | WS Receive | `displayName, text, isFinal, timestamp` | `src/services/mediasoupSession.js` + `src/App.vue` |

## 4. 业务逻辑时序图 (All Business Logic)
### 4.1 启动ASR
```mermaid
sequenceDiagram
    participant A as "App.vue"
    participant ASR as "asrStreamer.js"
    participant MS as "mediasoupSession.js"

    A->>ASR: "start({stream,roomId,userId,peerId,displayName})"
    ASR->>MS: "sendAsrStart(meta)"
    MS-->>ASR: "发送成功"
```

### 4.2 音频分片上传
```mermaid
sequenceDiagram
    participant ASR as "asrStreamer.js"
    participant MS as "mediasoupSession.js"
    participant SFU as "服务端"

    ASR->>ASR: "AudioContext采样并降采样16k"
    ASR->>ASR: "PCM编码为base64"
    ASR->>MS: "sendAsrAudio({roomId,seq,audioBase64})"
    MS->>SFU: "发送asrAudio消息"
```

### 4.3 识别结果回填
```mermaid
sequenceDiagram
    participant SFU as "服务端"
    participant MS as "mediasoupSession.js"
    participant A as "App.vue"
    participant UI as "MeetingRoom.vue"

    SFU->>MS: "推送asrResult"
    MS-->>A: "onState(type=asrResult,payload)"
    A->>A: "updateTranscriptionLines(payload)"
    A-->>UI: "更新transcriptionLines"
```

### 4.4 清空转写
```mermaid
sequenceDiagram
    participant U as "用户"
    participant UI as "MeetingRoom.vue"
    participant A as "App.vue"

    U->>UI: "点击清空转写"
    UI->>A: "emit(clear-transcription)"
    A->>A: "clearTranscriptionLines()"
    A-->>UI: "转写列表置空"
```

### 4.5 停止ASR
```mermaid
sequenceDiagram
    participant A as "App.vue"
    participant ASR as "asrStreamer.js"
    participant MS as "mediasoupSession.js"

    A->>ASR: "stop()"
    ASR->>MS: "sendAsrStop(meta)"
    ASR->>ASR: "断开processor与source并关闭audioContext"
    ASR-->>A: "清理完成"
```

## 5. 数据模型 (ER Diagram)
```mermaid
erDiagram
    ASR_SESSION {
        string roomId
        string userId
        string peerId
        string displayName
        boolean running
    }
    ASR_AUDIO_CHUNK {
        int seq
        string audioBase64
        long timestamp
    }
    ASR_RESULT_LINE {
        string displayName
        string text
        boolean isFinal
        long timestamp
    }

    ASR_SESSION ||--o{ ASR_AUDIO_CHUNK : "产生分片"
    ASR_SESSION ||--o{ ASR_RESULT_LINE : "接收结果"
```
