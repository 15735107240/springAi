# 更新日志

## [1.0.3] - 2025-01-XX

### 优化 ⚙️

#### 工具描述优化
- **AmapMapsService**: 优化所有5个工具的 `@Tool` 描述，包含详细的使用场景、触发关键词、参数格式说明和示例
  - `geoFunction`: 地理编码工具描述优化
  - `regeoFunction`: 逆地理编码工具描述优化
  - `directionFunction`: 路径规划工具描述优化
  - `aroundSearchFunction`: 周边搜索工具描述优化
  - `amapWeatherFunction`: 天气查询工具描述优化

- **业务工具**: 优化3个业务工具的描述
  - `getLiveRoomFunction`: 租房信息查询描述优化
  - `getOrderFunction`: 订单查询描述优化
  - `weatherFunction1`: Mock天气服务描述优化

#### 系统提示词优化
- 重构 `SYSTEM_PROMPT`，采用 Markdown 格式，结构更清晰
- 详细说明每个工具的使用场景和触发关键词
- 完善参数提取优先级和格式验证规则
- 添加典型使用示例，帮助AI更准确地理解工具用途

### 技术改进 🔧

- 优化 `ChatServiceImpl`，将工具配置从 `prompt().tools()` 移到 `defaultTools()`，提升性能
- 完善消息内容提取逻辑，确保 Redis 中存储的是纯文本而非对象描述
- 改进错误处理和日志记录

### 文档 📚

- 重新生成 README.md，更新技术栈和功能说明
- 更新快速测试指南，简化测试步骤
- 更新 CHANGELOG，记录所有优化内容

---

## [1.0.2] - 2025-10-30

### 新增功能 ✨

#### 分页查询API
- 新增分页查询历史会话接口 `GET /api/history/{conversationId}/page`
- 支持自定义页码和每页大小（1-100条）
- 页码从1开始，更符合用户习惯
- 返回完整的分页信息

#### 管理员接口 🔐
- 新增管理员接口 `GET /api/history/admin/conversations`
- 支持查询Redis中所有会话
- 通过 `adminKey` 参数进行权限验证

### 改进 🔧

- 优化 `RedissonChatMemory` 的消息检索性能
- 消息查询按时间倒序返回（最新的消息在前）
- 添加了参数验证和错误提示

---

## [1.0.1] - 2025-10-30

### 新增功能 ✨

#### Redisson分布式会话记忆系统
- 实现了基于Redis的分布式会话存储
- 支持多轮对话历史持久化
- 会话数据在应用重启后自动恢复
- 支持自动过期管理（默认7天）

#### 会话历史管理API
新增了完整的会话管理RESTful接口：
- `GET /api/history/{conversationId}` - 查询会话历史
- `GET /api/history/{conversationId}/info` - 查询会话信息
- `DELETE /api/history/{conversationId}` - 清除会话历史
- `POST /api/history/{conversationId}/refresh` - 刷新过期时间
- `POST /api/history/batch/check` - 批量检查会话存在性

### 技术细节 🔍

- **序列化**: 使用Kryo5进行高性能二进制序列化
- **存储结构**: Redis List，键格式 `chat:memory:{conversationId}`
- **过期策略**: TTL自动过期，默认7天

---

## [1.0.0] - 2025-10-28

### 初始版本
- Spring AI 基础功能
- 阿里云通义千问集成
- 工具调用支持（8个工具）
- 知识库检索（DashScope RAG）
- 高德地图集成
- 内存会话存储
- 流式响应

---

更多详细信息请查看项目 [README.md](./README.md)
