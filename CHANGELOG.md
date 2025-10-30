# 更新日志

## [1.0.2] - 2025-10-30

### 新增功能 ✨

#### 分页查询API
- 新增分页查询历史会话接口 `GET /api/history/{conversationId}/page`
- 支持自定义页码和每页大小（1-100条）
- **页码从1开始**，更符合用户习惯
- 返回完整的分页信息（总页数、是否有上下页等）
- 适用于移动端滚动加载和Web端分页浏览

#### 管理员接口 🔐
- 新增管理员接口 `GET /api/history/admin/conversations`
- 支持查询Redis中所有会话的用户ID列表
- 返回每个会话的详细信息（消息数量、剩余过期时间）
- 通过 `adminKey` 参数进行权限验证
- 方便管理员监控和管理会话数据

### 改进 🔧

- 优化了 `RedissonChatMemory` 的消息检索性能
- 添加了参数验证（页码、每页大小）
- 完善了错误提示信息
- 页码改为从1开始（`page=1`表示第一页）
- 添加了权限验证机制（403 Forbidden）

### 文档 📚

更新文档：
- **README.md** - 添加分页查询接口说明和使用示例
- **快速测试指南.md** - 添加分页查询测试场景
- 精简文档结构，保留核心文档（README、快速测试指南、CHANGELOG）

---

## [1.0.1] - 2025-10-30

### 新增功能 ✨

#### 1. Redisson分布式会话记忆系统
- 实现了基于Redis的分布式会话存储
- 支持多轮对话历史持久化
- 会话数据在应用重启后自动恢复
- 支持自动过期管理（默认7天）

#### 2. 会话历史管理API
新增了完整的会话管理RESTful接口：
- `GET /api/history/{conversationId}` - 查询会话历史
- `GET /api/history/{conversationId}/info` - 查询会话信息
- `DELETE /api/history/{conversationId}` - 清除会话历史
- `POST /api/history/{conversationId}/refresh` - 刷新过期时间
- `POST /api/history/batch/check` - 批量检查会话存在性

#### 3. 新增核心类

**配置类**：
- `RedissonConfig.java` - Redisson客户端配置

**服务类**：
- `RedissonChatMemory.java` - Redis会话记忆实现
- `ChatHistoryService.java` - 会话历史服务接口
- `ChatHistoryServiceImpl.java` - 会话历史服务实现

**控制器**：
- `ChatHistoryController.java` - 会话历史管理控制器

**异常类**：
- `ChatMemoryException.java` - 会话记忆专用异常

### 改进 🔧

#### 1. 依赖更新
- 新增 `redisson-spring-boot-starter:3.27.2`
- 新增 `kryo:5.5.0` - 用于高性能序列化
- 新增 `jackson-databind` 和 `jackson-datatype-jsr310`

#### 2. 配置增强
在 `application.yml` 中新增：
- Redis连接配置
- Redisson客户端配置
- 会话记忆配置（key前缀、TTL）

#### 3. 服务改进
- `SpringAiServiceImpl` 改为通过依赖注入使用 `ChatMemory`
- 移除了硬编码的 `InMemoryChatMemory`
- 支持可插拔的会话存储后端

### 文档 📚

新增以下文档：
1. **Redisson会话记忆使用说明.md**
   - 完整的功能说明
   - 详细的配置指南
   - API使用示例
   - 最佳实践

2. **快速测试指南.md**
   - 6个测试场景
   - 详细的测试步骤
   - 预期结果验证
   - 故障模拟

3. **项目架构说明.md**
   - 项目结构说明
   - 核心功能模块
   - 数据流程图
   - 设计模式说明

4. **CHANGELOG.md** (本文件)
   - 版本更新记录

### 技术细节 🔍

#### 数据存储结构
- **键格式**: `chat:memory:{conversationId}`
- **数据类型**: Redis List
- **序列化**: Kryo5 (高性能二进制序列化)
- **过期策略**: TTL自动过期

#### 序列化方案
- 使用Kryo5Codec替代JsonJacksonCodec
- 解决Spring AI Message对象无默认构造函数的问题
- 性能更优，存储更紧凑

#### 主要特性
- ✅ 分布式部署支持
- ✅ 会话自动过期
- ✅ 多用户隔离
- ✅ 持久化存储
- ✅ 高性能访问
- ✅ 异常容错
- ✅ 完整日志记录

### 性能指标 📊

- **连接池大小**: 64
- **最小空闲连接**: 10
- **默认过期时间**: 7天
- **支持并发**: 高并发场景
- **消息检索**: O(n) 时间复杂度

### 兼容性 🔄

- ✅ 向后兼容原有对话API
- ✅ 不影响现有功能
- ✅ 可选择性启用Redis
- ✅ 支持降级到内存存储

### 配置示例

```yaml
# 最小配置
spring:
  data:
    redis:
      host: localhost
      port: 6379

# 完整配置
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-wait: -1ms
          max-idle: 8
          min-idle: 0

redisson:
  single-server-config:
    address: redis://localhost:6379
    password: your_password
    database: 0
    connection-pool-size: 64
    connection-minimum-idle-size: 10

chat:
  memory:
    redis:
      key-prefix: "chat:memory:"
      ttl: 604800
```

### 使用示例

```bash
# 1. 进行对话（自动保存到Redis）
curl "http://localhost:10010/api/simple/chat?query=你好&conversantId=user123"

# 2. 查询历史
curl "http://localhost:10010/api/history/user123?lastN=10"

# 3. 清除历史
curl -X DELETE "http://localhost:10010/api/history/user123"
```

### 已知问题 ⚠️

无

### 迁移指南 🔄

从 1.0.0 升级到 1.0.1：

1. **更新依赖**
   ```bash
   mvn clean install
   ```

2. **配置Redis**
   在 `application.yml` 中添加Redis配置（见上文）

3. **启动Redis服务**
   ```bash
   redis-server
   ```

4. **重启应用**
   ```bash
   mvn spring-boot:run
   ```

5. **验证功能**
   参考《快速测试指南.md》进行测试

### 注意事项 ⚠️

1. **Redis必须可用**
   - 应用启动前确保Redis服务已启动
   - 如果Redis不可用，应用启动会失败

2. **数据迁移**
   - 如果之前使用 `InMemoryChatMemory`，历史数据不会自动迁移
   - 升级后旧的内存会话将丢失

3. **生产环境**
   - 必须设置Redis密码
   - 建议开启Redis持久化
   - 根据业务量调整TTL和连接池大小

### 后续计划 🚀

- [ ] 支持Redis集群模式
- [ ] 添加会话统计分析
- [ ] 支持会话数据导出
- [ ] 优化大量历史消息的检索性能
- [ ] 添加会话归档功能

### 贡献者 👥

- yanwenjie - 初始实现

---

## [1.0.0] - 2025-10-28

### 初始版本
- Spring AI基础功能
- 通义千问集成
- 工具调用支持
- 知识库检索
- 高德地图集成
- 内存会话存储

---

更多信息请查看项目文档。

