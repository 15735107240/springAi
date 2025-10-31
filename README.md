# Spring AI 智能聊天助手

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-blue.svg)](https://spring.io/projects/spring-ai)
[![Spring AI Alibaba](https://img.shields.io/badge/Spring%20AI%20Alibaba-1.0.0.2-orange.svg)](https://github.com/alibaba/spring-ai-alibaba)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Redis](https://img.shields.io/badge/Redis-5.0+-red.svg)](https://redis.io/)

基于 Spring AI、Spring AI Alibaba 和阿里云通义千问的智能聊天助手，集成高德地图服务，支持对话记忆、知识库检索、智能工具调用等功能。

> 🚀 **[快速测试指南](./快速测试指南.md)** | 📋 **[更新日志](./CHANGELOG.md)**

## ✨ 核心特性

### 🤖 智能对话
- **分布式会话记忆**: 基于 Redisson 的 Redis 持久化对话历史，支持分布式部署
- **多轮对话**: 完整的上下文记忆，自动提取实体和参数，支持跨轮次对话
- **知识库检索**: 集成 DashScope RAG，支持"闫文杰的个人信息"知识库检索
- **流式响应**: 实时流式输出，提升用户体验
- **智能决策**: 根据对话上下文自动选择工具/知识库/大模型（三层决策机制）
- **自动日期注入**: 系统自动注入当前日期，确保时间相关的回答准确
- **工具自动发现**: 基于 `@Tool` 注解自动发现和注册工具，无需手动配置

### 💾 会话管理
- **持久化存储**: 基于 Redisson 的 Redis 存储，服务重启数据不丢失
- **自动过期**: 可配置的会话过期时间（默认7天）
- **分页查询**: 支持按页查询历史消息，适用于移动端和Web端
- **批量操作**: 支持批量查询、清除、刷新会话
- **多用户隔离**: 完全隔离的用户会话数据
- **高性能序列化**: 使用 Kryo5 进行高性能二进制序列化

### 🗺️ 高德地图集成
系统集成5个高德地图工具，支持完整的LBS服务：
- **地理编码** (`geoFunction`): 地址 ⇄ 坐标转换，支持多结果返回
- **逆地理编码** (`regeoFunction`): 坐标转详细地址
- **路径规划** (`directionFunction`): 驾车路线规划，自动处理地址/坐标输入
- **周边搜索** (`aroundSearchFunction`): POI 搜索，支持地址自动转换
- **天气查询** (`amapWeatherFunction`): 实时天气信息（温度、风力、湿度等）

### 🛠️ 业务工具
系统集成3个业务工具：
- **订单查询** (`getOrderFunction`): 根据用户编号和订单编号查询订单信息
- **租房信息** (`getLiveRoomFunction`): 根据租房类型、预算范围和地址匹配房源
- **天气服务** (`weatherFunction1`): 本地天气查询（Mock服务）

### 📊 其他功能
- **日志管理**: 按日期滚动的日志文件，保留 30 天
- **健康检查**: 内置健康检查端点
- **中文日志**: 所有日志信息均为中文，便于调试和维护

## 🏗️ 技术架构

### 技术栈
- **框架**: Spring Boot 3.4.5
- **AI 框架**: 
  - Spring AI 1.0.0 - Spring 官方 AI 框架
  - Spring AI Alibaba 1.0.0.2 - 阿里云 Spring AI 集成包
- **大模型**: 阿里云通义千问 (DashScope)
- **知识库**: DashScope RAG (Retrieval Augmented Generation)
- **缓存/存储**: Redis + Redisson 3.27.2
- **序列化**: Kryo5 高性能二进制序列化
- **地图服务**: 高德地图 API
- **构建工具**: Maven 3.6+

### 项目结构
```
springAI/
├── src/main/java/com/springai/chat/
│   ├── ChatApplication.java              # 启动类
│   ├── controller/
│   │   ├── ChatController.java          # 聊天控制器
│   │   └── ChatHistoryController.java   # 会话历史管理控制器
│   ├── service/
│   │   ├── ChatService.java             # 聊天服务接口
│   │   ├── ChatHistoryService.java     # 会话历史服务接口
│   │   └── impl/
│   │       ├── ChatServiceImpl.java    # 聊天服务实现
│   │       └── ChatHistoryServiceImpl.java  # 会话历史服务实现
│   ├── memory/
│   │   ├── RedissonChatMemory.java     # Redis会话存储实现
│   │   └── InMemoryChatMemory.java     # 内存会话存储（降级方案）
│   ├── exception/
│   │   └── ChatMemoryException.java    # 会话异常类
│   ├── tools/
│   │   ├── AmapMapsService.java        # 高德地图工具服务
│   │   ├── MockLiveService.java        # 租房信息服务
│   │   ├── MockOrderService.java       # 订单查询服务
│   │   └── MockWeatherService.java     # 天气服务（Mock）
│   └── config/
│       ├── BailianAutoconfiguration.java  # DashScope自动配置
│       └── RedissonConfig.java            # Redisson配置
├── src/main/resources/
│   ├── application.yml                  # 应用配置
│   └── logback-spring.xml               # 日志配置
└── pom.xml                              # Maven 配置
```

## 🚀 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- Redis 5.0+
- 阿里云 DashScope API Key
- 高德地图 API Key

### 配置说明

#### 1. 克隆项目
```bash
git clone <repository-url>
cd springAI
```

#### 2. 启动 Redis
```bash
# macOS (使用 Homebrew)
brew services start redis

# 或直接运行
redis-server

# 验证 Redis 是否运行
redis-cli ping
# 应该返回: PONG
```

#### 3. 配置 API Keys
编辑 `src/main/resources/application.yml`:

```yaml
spring:
  ai:
    dashscope:
      api-key: your_dashscope_api_key  # 阿里云通义千问 API Key
  amap:
    api-key: your_amap_api_key         # 高德地图 API Key
  data:
    redis:
      host: localhost                  # Redis 地址
      port: 6379                       # Redis 端口
      password:                        # Redis 密码（如有）

chat:
  memory:
    redis:
      ttl: 604800                      # 会话过期时间（秒），默认7天
  admin:
    key: admin                         # 管理员密钥（生产环境请修改）
```

**获取 API Keys**:
- 阿里云 DashScope: [https://dashscope.console.aliyun.com/](https://dashscope.console.aliyun.com/)
- 高德地图: [https://lbs.amap.com/](https://lbs.amap.com/)

#### 4. 启动应用
```bash
# 使用 Maven
mvn spring-boot:run

# 或打包后运行
mvn clean package
java -jar target/chat-1.0-SNAPSHOT.jar
```

应用将在 `http://localhost:10010` 启动。

## 📖 使用指南

### API 接口

#### 1. 智能对话

##### 流式对话（推荐）
```http
GET /api/simple/chat?query=你好&conversantId=user123
Accept: text/event-stream
```

##### 普通对话
```http
GET /api/simple/chat?query=你好&conversantId=user123
```

##### POST 方式
```http
POST /api/simple/chat
Content-Type: application/json

{
  "query": "你好",
  "conversantId": "user123"
}
```

**参数说明**:
- `query`: 用户输入的问题（必需）
- `conversantId`: 会话 ID，用于维护对话上下文（必需）

**响应**: 流式 `ChatResponse` 响应

**示例对话**:
```bash
# 天气查询
curl "http://localhost:10010/api/simple/chat?query=今天北京天气怎么样&conversantId=user1"

# 路径规划
curl "http://localhost:10010/api/simple/chat?query=从人民广场去天安门怎么走&conversantId=user1"

# 周边搜索
curl "http://localhost:10010/api/simple/chat?query=上海人民广场附近有什么餐厅&conversantId=user1"

# 地址查询
curl "http://localhost:10010/api/simple/chat?query=121.47,31.23这是哪里&conversantId=user1"

# 知识库检索
curl "http://localhost:10010/api/simple/chat?query=闫文杰的项目经历有哪些&conversantId=user1"

# 租房查询
curl "http://localhost:10010/api/simple/chat?query=我要租房，合租，预算3000-4000，地址在徐汇区&conversantId=user1"
```

### 会话历史管理

#### 1. 查询会话历史
```bash
# 查询最近10条消息（按时间倒序，最新的在前）
curl "http://localhost:10010/api/history/user123?lastN=10"

# 查询所有消息（按时间倒序，最新的在前）
curl "http://localhost:10010/api/history/user123"
```

> 📌 **注意**：所有查询结果按时间倒序返回，最新的消息在前

#### 2. 分页查询历史
```bash
# 第一页（默认每页10条，最新的消息）
curl "http://localhost:10010/api/history/user123/page"

# 第二页，每页20条（较旧的消息）
curl "http://localhost:10010/api/history/user123/page?page=2&size=20"
```

> 📌 **说明**：
> - 第1页：最新的消息（最近的10条）
> - 第2页：较旧的消息（第11-20条）
> - 页码从1开始

**响应示例**:
```json
{
  "conversationId": "user123",
  "page": 1,
  "size": 10,
  "totalMessages": 45,
  "totalPages": 5,
  "hasNext": true,
  "hasPrevious": false,
  "messages": [...]
}
```

#### 3. 查询会话信息
```bash
curl "http://localhost:10010/api/history/user123/info"
```

#### 4. 清除会话历史
```bash
curl -X DELETE "http://localhost:10010/api/history/user123"
```

#### 5. 刷新会话过期时间
```bash
curl -X POST "http://localhost:10010/api/history/user123/refresh"
```

#### 6. 批量检查会话
```bash
curl -X POST "http://localhost:10010/api/history/batch/check" \
  -H "Content-Type: application/json" \
  -d '["user123", "user456"]'
```

#### 7. 管理员：查询所有会话 🔐
```bash
# 需要管理员密钥
curl "http://localhost:10010/api/history/admin/conversations?adminKey=admin"
```

**响应示例**:
```json
{
  "total": 3,
  "conversations": [
    {
      "conversationId": "user123",
      "messageCount": 45,
      "remainingTtl": 604800
    },
    ...
  ]
}
```

> ⚠️ **安全提示**: 这是管理员接口，需要提供正确的 `adminKey` 才能访问

## 🎯 功能详解

### 智能决策机制

系统根据对话上下文自动选择最合适的处理方式：

#### 1. 工具优先
当问题需要实时信息、外部系统数据或可执行操作时，优先调用相应工具：
- **天气查询** → `amapWeatherFunction`
- **地址转坐标** → `geoFunction`
- **坐标转地址** → `regeoFunction`
- **路径规划** → `directionFunction`（支持地址/坐标输入，自动转换）
- **周边搜索** → `aroundSearchFunction`（支持地址/坐标输入，自动转换）
- **订单查询** → `getOrderFunction`
- **租房信息** → `getLiveRoomFunction`

#### 2. 知识库优先
当问题涉及内部知识、事实密集型问答时，优先进行知识库检索：
- 关于"闫文杰"的个人信息、项目经历、工作经验、技能特长
- 公司制度、产品信息、技术文档等内部知识
- 系统会自动从"闫文杰的个人信息"知识库中检索相关内容

#### 3. 大模型直答
闲聊、观点建议、常识性解释、无需实时数据的泛化问题时直接回答：
- 无需实时数据的泛化问题
- 直接由通义千问模型回答

#### 4. 不确定时
默认先进行知识库检索，再补充大模型组织答案。

### 上下文记忆

- **自动实体提取**: 从对话历史中抽取缺省实体（城市、地点、经纬度、人名、订单号、用户编号等）
- **智能参数补全**: 参数缺失且上下文可补全时自动补全；无法补全时先向用户澄清
- **参数一致性**: 同一会话中优先沿用最近确认过的实体与单位（如温度单位、地点城市等）
- **历史记忆**: 每次对话从Redis加载完整的对话历史，确保多轮对话的连贯性

### 地址智能处理

- **自动识别**: 自动识别输入是地址还是经纬度坐标
- **自动转换**: 工具需要经纬度但用户给出地址时，自动调用 `geoFunction` 获取坐标
- **多候选处理**: 地址转换有多个候选时，优先选择第一个匹配结果
- **混合输入支持**: 路径规划和周边搜索同时支持地址和坐标输入，必要时自动前置地理编码

### 工具描述优化

所有工具的描述都已优化，包含：
- **使用场景**: 明确说明何时使用该工具
- **触发关键词**: 列出常见的触发词汇
- **参数说明**: 详细的参数格式要求和示例
- **返回信息**: 说明返回结果的格式

## 🔧 配置参数

### 服务器配置
```yaml
server:
  port: 10010              # 服务端口
  servlet:
    context-path: /        # 上下文路径
```

### Spring AI 配置
```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}      # 通义千问 API Key（通过 Spring AI Alibaba 集成）
  amap:
    api-key: ${AMAP_API_KEY}             # 高德地图 API Key
```

> 📌 **说明**: 本项目使用 Spring AI Alibaba 1.0.0.2 来集成阿里云 DashScope API，提供了 `DashScopeApi`、`DashScopeDocumentRetriever` 和 `DocumentRetrievalAdvisor` 等组件，支持知识库检索（RAG）功能。

### Redis 配置
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: 
      database: 0
      timeout: 3000ms

redisson:
  single-server-config:
    address: redis://localhost:6379
    connection-pool-size: 64
    connection-minimum-idle-size: 10
```

### 会话记忆配置
```yaml
chat:
  memory:
    redis:
      key-prefix: "chat:memory:"    # Redis键前缀
      ttl: 604800                   # 会话过期时间（秒），默认7天
  admin:
    key: admin                       # 管理员密钥（生产环境请修改）
```

### 日志配置
```yaml
logging:
  level:
    com.springai: INFO                    # 应用日志级别
    org.springframework.web: INFO         # Spring Web 日志级别
  file:
    path: ../log                          # 日志文件路径
```

## 🛡️ 错误处理

所有工具调用都包含完整的错误处理：
- API 调用失败时返回明确错误信息
- 地址转换失败时提示用户
- 参数缺失时自动澄清或补全
- Redis 连接失败时提供降级方案

## 📊 监控端点

```bash
# 健康检查
curl http://localhost:10010/actuator/health

# 应用信息
curl http://localhost:10010/actuator/info

# 指标信息
curl http://localhost:10010/actuator/metrics
```

## 🔍 调试技巧

### 1. 查看详细日志
在 `application.yml` 中临时开启DEBUG日志：
```yaml
logging:
  level:
    com.springai.chat: DEBUG
    org.redisson: DEBUG
```

### 2. 使用 Redis Monitor
```bash
redis-cli MONITOR
```

### 3. 查看会话数据
```bash
# 查看所有会话键
redis-cli KEYS chat:memory:*

# 查看会话消息数量
redis-cli LLEN chat:memory:user123

# 查看会话过期时间
redis-cli TTL chat:memory:user123
```

## 📝 日志管理

日志文件位置: `../log/spring-ai-yyyy-MM-dd.log`

**配置特性**:
- 按日期自动滚动
- 单文件最大 50MB
- 保留最近 30 天
- 总容量限制 5GB
- 自动压缩归档
- 所有日志均为中文

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发流程
1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 👨‍💻 作者

**闫文杰**

## 🙏 致谢

- [Spring AI](https://spring.io/projects/spring-ai) - Spring 官方 AI 框架
- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba) - 阿里云 Spring AI 集成包，提供 DashScope API 支持
- [阿里云通义千问](https://dashscope.aliyun.com/) - 大语言模型服务
- [高德地图开放平台](https://lbs.amap.com/) - 地图服务提供商
- [Redisson](https://redisson.org/) - Redis Java客户端

## 📚 扩展阅读

- **[快速测试指南](./快速测试指南.md)** - 完整的测试场景和故障排查
- **[CHANGELOG](./CHANGELOG.md)** - 版本更新日志

---

⭐ 如果这个项目对你有帮助，请给一个 Star！

**最后更新**: 2025-01-XX | **版本**: v1.0.3
