# neoview-signal-server

NeoView 后端服务，基于 Spring Boot 3.2 构建，提供用户认证、会议管理等核心业务功能。

## 功能说明

- **用户认证**：基于 JWT 的用户登录/注册
- **会议管理**：创建、加入、离开会议
- **WebSocket 信令**：与前端和 Node 桥接服务通信
- **实时语音识别**：集成阿里云 NLS 语音转文字服务

## 环境要求

- JDK 21+
- PostgreSQL 14+
- Redis 6+
- Maven 3.8+

## 数据库初始化

```bash
psql -U postgres -f src/main/resources/db/schema.sql
```

## 配置

通过环境变量配置敏感信息：

```bash
# 数据库
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=neoview
export DB_USERNAME=postgres
export DB_PASSWORD=your_password

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379

# JWT
export JWT_SECRET=your_jwt_secret_key

# 阿里云 NLS (可选)
export ALIYUN_NLS_ACCESS_KEY_ID=your_key
export ALIYUN_NLS_ACCESS_KEY_SECRET=your_secret
export ALIYUN_NLS_APP_KEY=your_app_key

# Node 桥接服务
export PROTOO_HOST=your_mediasoup_host
export PROTOO_PORT=4443
```

## 运行

```bash
# 开发模式
./mvnw spring-boot:run

# 打包
./mvnw clean package

# 生产运行
java -jar target/neoview-signal-server-*.jar
```

## 项目结构

```
neoview-signal-server/
├── src/main/java/com/neoview/server/
│   ├── config/           # 配置类
│   ├── controller/       # REST 控制器
│   ├── security/         # 安全组件
│   ├── service/          # 业务服务
│   ├── websocket/        # WebSocket 处理
│   └── model/            # 数据模型
├── src/main/resources/
│   ├── application.yml   # 配置文件
│   └── db/schema.sql     # 数据库脚本
└── pom.xml
```

## 相关文档

- [主项目文档](../README.md)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
