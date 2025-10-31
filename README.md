# Spring AI 智能聊天助手

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Redis](https://img.shields.io/badge/Redis-5.0+-red.svg)](https://redis.io/)

基于 Spring AI 和阿里云通义千问的智能聊天助手，集成高德地图 MCP 服务，支持对话记忆、知识库检索、工具调用等功能。

> 🚀 **[快速测试指南](./快速测试指南.md)** | 📋 **[更新日志](./CHANGELOG.md)**

## ✨ 核心特性

### 🤖 智能对话
- **分布式会话记忆**: 基于 Redis 的持久化对话历史，支持分布式部署 ⭐NEW
- **多轮对话**: 完整的上下文记忆，自动提取实体和参数（支持检索最近100条消息）
- **知识库检索**: 集成 DashScope RAG，支持"闫文杰的个人信息"知识库检索 ⭐
- **流式响应**: 实时流式输出，提升用户体验
- **智能决策**: 根据对话上下文自动选择工具/知识库/大模型（三层决策机制）
- **自动日期注入**: 系统自动注入当前日期，确保时间相关的回答准确

### 💾 会话管理
- **持久化存储**: 基于 Redisson 的 Redis 存储，服务重启数据不丢失 ⭐NEW
- **自动过期**: 可配置的会话过期时间（默认7天）
- **分页查询**: 支持按页查询历史消息，适用于移动端和Web端 ⭐NEW
- **批量操作**: 支持批量查询、清除、刷新会话
- **多用户隔离**: 完全隔离的用户会话数据

### 🗺️ 高德地图集成
- **地理编码**: 地址 ⇄ 坐标转换，支持多结果返回
- **路径规划**: 驾车路线规划，自动处理地址/坐标输入
- **周边搜索**: POI 搜索，支持地址自动转换
- **天气查询**: 实时天气信息（温度、风力、湿度等）
- **逆地理编码**: 坐标转详细地址

### 🛠️ 工具调用
系统集成了8个智能工具，可根据用户需求自动调用：

**高德地图工具**:
- **地理编码** (`geoFunction`): 将地址转换为经纬度坐标
- **逆地理编码** (`regeoFunction`): 将经纬度坐标转换为详细地址
- **路径规划** (`directionFunction`): 规划从起点到终点的驾车路线
- **周边搜索** (`aroundSearchFunction`): 搜索指定位置周边的兴趣点（餐厅、地铁站等）
- **天气查询** (`amapWeatherFunction`): 查询指定城市的实时天气信息

**业务工具**:
- **订单查询** (`getOrderFunction`): 根据用户编号和订单编号查询订单信息
- **租房信息** (`getLiveRoomFunction`): 查询用户租房信息
- **天气服务** (`weatherFunction1`): 本地天气查询（Mock）

### 📊 其他功能
- **图像生成**: 基于文本描述生成图像
- **日志管理**: 按日期滚动的日志文件，保留 30 天
- **健康检查**: 内置健康检查端点

## 🏗️ 技术架构

### 技术栈
- **框架**: Spring Boot 3.2.5
- **AI 引擎**: Spring AI Alibaba 1.0.0-M5.1
- **大模型**: 阿里云通义千问 (DashScope)
- **缓存/存储**: Redis + Redisson 3.27.2 ⭐NEW
- **序列化**: Kryo5 高性能二进制序列化 ⭐NEW
- **地图服务**: 高德地图 API
- **模板引擎**: Thymeleaf
- **构建工具**: Maven

### 项目结构
```
springAI/
├── src/main/java/com/springai/chat/
│   ├── HelloApplication.java          # 启动类
│   ├── controller/
│   │   ├── SpringAiController.java    # 主控制器
│   │   ├── AmapTestController.java    # 高德地图测试控制器
│   │   └── ChatHistoryController.java # 会话历史管理 ⭐NEW
│   ├── service/
│   │   ├── SpringAiService.java       # 服务接口
│   │   ├── ChatHistoryService.java    # 会话历史服务 ⭐NEW
│   │   └── impl/
│   │       ├── SpringAiServiceImpl.java     # 服务实现
│   │       └── ChatHistoryServiceImpl.java  # 会话历史实现 ⭐NEW
│   ├── memory/
│   │   └── RedissonChatMemory.java    # Redis会话存储 ⭐NEW
│   ├── exception/
│   │   └── ChatMemoryException.java   # 会话异常 ⭐NEW
│   ├── tools/
│   │   ├── AmapMapsService.java       # 高德地图工具
│   │   ├── MockWeatherService.java    # 天气服务
│   │   ├── MockOrderService.java      # 订单服务
│   │   └── MockLiveService.java       # 租房服务
│   └── config/
│       ├── Config.java                # 工具函数配置
│       └── RedissonConfig.java        # Redisson配置 ⭐NEW
├── src/main/resources/
│   ├── application.yml                # 应用配置（含Redis）
│   └── logback-spring.xml            # 日志配置
└── pom.xml                           # Maven 配置
```

## 🚀 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- Redis 5.0+ ⭐NEW
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
```http
GET /api/simple/chat?query=你好&conversantId=user123
```

**参数说明**:
- `query`: 用户输入的问题
- `conversantId`: 会话 ID，用于维护对话上下文

**响应**: 流式文本响应

**示例对话**:
```bash
# 天气查询
curl "http://localhost:10010/api/simple/chat?query=今天北京天气怎么样&conversantId=user1"

# 路径规划
curl "http://localhost:10010/api/simple/chat?query=从上海到北京怎么走&conversantId=user1"

# 周边搜索
curl "http://localhost:10010/api/simple/chat?query=上海人民广场附近有什么餐厅&conversantId=user1"

# 地址查询
curl "http://localhost:10010/api/simple/chat?query=北京天安门在哪里&conversantId=user1"
```

#### 2. 图像生成
```http
GET /api/simple/image?query=一只可爱的猫咪
```

**响应**: 重定向到生成的图像 URL

### 会话历史管理 ⭐NEW

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
> - 第3页：更旧的消息（第21-30条）

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
  "conversationIds": ["user123", "user456", "user789"],
  "conversations": [
    {
      "conversationId": "user123",
      "messageCount": 45,
      "remainingTtl": 604800
    },
    {
      "conversationId": "user456",
      "messageCount": 20,
      "remainingTtl": 500000
    },
    {
      "conversationId": "user789",
      "messageCount": 10,
      "remainingTtl": 600000
    }
  ]
}
```

> ⚠️ **安全提示**: 这是管理员接口，需要提供正确的 `adminKey` 才能访问

### 高德地图工具测试接口

#### 地理编码
```bash
curl "http://localhost:10010/api/amap/geo?address=北京市天安门&city=北京"
```

#### 逆地理编码
```bash
curl "http://localhost:10010/api/amap/regeo?location=116.407387,39.904179"
```

#### 路径规划
```bash
curl "http://localhost:10010/api/amap/direction?origin=上海人民广场&destination=北京天安门"
```

#### 周边搜索
```bash
curl "http://localhost:10010/api/amap/around?location=上海人民广场&keywords=餐厅&radius=1000"
```

#### 天气查询
```bash
curl "http://localhost:10010/api/amap/weather?city=北京"
```

## 🎯 功能详解

### 智能决策机制

系统根据对话上下文自动选择最合适的处理方式：

1. **工具优先**: 实时信息、外部系统数据或可执行操作
   - 天气查询 → `amapWeatherFunction` / `weatherFunction1`
   - 地址转坐标 → `geoFunction`
   - 坐标转地址 → `regeoFunction`
   - 路径规划 → `directionFunction`（支持地址/坐标输入）
   - 周边搜索 → `aroundSearchFunction`（支持地址/坐标输入）
   - 订单查询 → `getOrderFunction`
   - 租房信息 → `getLiveRoomFunction`

2. **知识库优先**: 内部知识、事实密集型问答
   - 公司制度、产品信息、技术文档
   - 个人信息、关于"闫文杰"的相关信息
   - 使用 DashScope RAG 进行智能检索

3. **大模型直答**: 闲聊、观点建议、常识性解释
   - 无需实时数据的泛化问题
   - 直接由通义千问模型回答

4. **不确定时**: 默认先进行知识库检索，再补充大模型组织答案

### 上下文记忆

- **自动实体提取**: 从对话历史中抽取缺省实体（城市、地点、经纬度、人名、订单号等）
- **智能参数补全**: 参数缺失且上下文可补全时自动补全；无法补全时先向用户澄清
- **参数一致性**: 同一会话中优先沿用最近确认过的实体与单位（如温度单位、地点城市等）
- **历史记忆大小**: 每次对话可检索最近100条历史消息用于上下文理解

### 地址智能处理

- **自动识别**: 自动识别输入是地址还是经纬度坐标
- **自动转换**: 工具需要经纬度但用户给出地址时，自动调用 `geoFunction` 获取坐标
- **多候选处理**: 地址转换有多个候选时，优先选择 `geocodes[0]`（第一个结果）
- **混合输入支持**: 路径规划和周边搜索同时支持地址和坐标输入，必要时自动前置地理编码

## 📝 日志管理

日志文件位置: `../log/spring-ai-yyyy-MM-dd.log`

**配置特性**:
- 按日期自动滚动
- 单文件最大 50MB
- 保留最近 30 天
- 总容量限制 5GB
- 自动压缩归档

## 🔧 配置参数

### 服务器配置
```yaml
server:
  port: 10010              # 服务端口
  servlet:
    context-path: /        # 上下文路径
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

### Spring AI 配置
```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}      # 通义千问 API Key
  amap:
    api-key: ${AMAP_API_KEY}             # 高德地图 API Key
```

## 🛡️ 错误处理

所有工具调用都包含完整的错误处理：
- API 调用失败时返回明确错误信息
- 地址转换失败时提示用户
- 参数缺失时自动澄清或补全

## 📊 监控端点

```bash
# 健康检查
curl http://localhost:10010/actuator/health

# 应用信息
curl http://localhost:10010/actuator/info

# 指标信息
curl http://localhost:10010/actuator/metrics
```

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
- [阿里云通义千问](https://dashscope.aliyun.com/) - 大语言模型服务
- [高德地图开放平台](https://lbs.amap.com/) - 地图服务提供商

## 📚 扩展阅读

- **[快速测试指南](./快速测试指南.md)** - 完整的测试场景和故障排查
- **[CHANGELOG](./CHANGELOG.md)** - 版本更新日志

## 🔧 高级配置

### Redisson 配置详解

```yaml
redisson:
  single-server-config:
    address: redis://localhost:6379
    password:                         # Redis密码
    database: 0                       # 数据库索引
    connection-pool-size: 64          # 连接池大小
    connection-minimum-idle-size: 10  # 最小空闲连接
    idle-connection-timeout: 10000    # 空闲连接超时
    timeout: 3000                     # 操作超时时间
  threads: 16                         # 线程池大小
  netty-threads: 32                   # Netty线程池大小

chat:
  admin:
    key: your_secure_admin_key        # ⚠️ 生产环境务必修改此密钥
```

> ⚠️ **安全建议**：生产环境中务必修改默认的管理员密钥，建议使用随机生成的复杂字符串

### 会话存储机制

- **序列化方式**: Kryo5（高性能二进制序列化）
- **存储结构**: Redis List
- **键格式**: `chat:memory:{conversationId}`
- **自动过期**: 默认7天（604800秒）

### 分页查询最佳实践

**页码说明**：页码从 **1** 开始（第1页、第2页、第3页...）

**排序说明**：所有消息按时间倒序返回，最新的消息在前

```javascript
// 前端示例：滚动加载（最新消息在前）
async function loadMoreMessages(page = 1, size = 20) {
  const response = await fetch(
    `/api/history/${conversationId}/page?page=${page}&size=${size}`
  );
  const data = await response.json();
  return {
    messages: data.messages,  // 已经按时间倒序排列（最新的在前）
    hasMore: data.hasNext,
    currentPage: data.page,
    totalPages: data.totalPages
  };
}

// 使用示例
loadMoreMessages(1);  // 第1页：最新的20条消息
loadMoreMessages(2);  // 第2页：较旧的20条消息
```

**消息顺序示例**：
```
Redis存储顺序（从旧到新）：
[消息1, 消息2, 消息3, ..., 消息100]

API返回顺序（从新到旧）：
第1页：[消息100, 消息99, 消息98, ...]
第2页：[消息80, 消息79, 消息78, ...]
```

### 常见问题排查

#### 问题1: Redis连接失败
```bash
# 检查Redis是否运行
redis-cli ping

# 清除旧数据（序列化格式不兼容时）
redis-cli DEL chat:memory:{conversationId}
```

#### 问题2: 会话数据丢失
- 检查TTL配置是否过短
- 验证Redis持久化是否开启
- 确认Redis内存淘汰策略

## 📮 联系方式

如有问题或建议，欢迎通过以下方式联系：
- 提交 Issue
- 发送邮件

---

⭐ 如果这个项目对你有帮助，请给一个 Star！

**最后更新**: 2025-10-31 | **版本**: v1.0.2

