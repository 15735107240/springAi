# Spring AI 智能聊天助手

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

基于 Spring AI 和阿里云通义千问的智能聊天助手，集成高德地图 MCP 服务，支持对话记忆、知识库检索、工具调用等功能。

## ✨ 核心特性

### 🤖 智能对话
- **上下文记忆**: 基于会话 ID 的对话历史管理
- **知识库检索**: 集成 DashScope RAG，支持内部知识问答
- **流式响应**: 实时流式输出，提升用户体验
- **智能决策**: 根据对话上下文自动选择工具/知识库/大模型

### 🗺️ 高德地图集成
- **地理编码**: 地址 ⇄ 坐标转换，支持多结果返回
- **路径规划**: 驾车路线规划，自动处理地址/坐标输入
- **周边搜索**: POI 搜索，支持地址自动转换
- **天气查询**: 实时天气信息（温度、风力、湿度等）
- **逆地理编码**: 坐标转详细地址

### 🛠️ 工具调用
- **订单查询**: 根据用户编号和订单编号查询订单信息
- **租房信息**: 查询用户租房信息
- **时间查询**: 获取指定时区的时间
- **天气服务**: 本地天气查询（Mock）

### 📊 其他功能
- **图像生成**: 基于文本描述生成图像
- **日志管理**: 按日期滚动的日志文件，保留 30 天
- **健康检查**: 内置健康检查端点

## 🏗️ 技术架构

### 技术栈
- **框架**: Spring Boot 3.2.5
- **AI 引擎**: Spring AI Alibaba 1.0.0.3
- **大模型**: 阿里云通义千问 (DashScope)
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
│   │   └── AmapTestController.java    # 高德地图测试控制器
│   ├── service/
│   │   ├── SpringAiService.java       # 服务接口
│   │   └── impl/
│   │       └── SpringAiServiceImpl.java # 服务实现
│   ├── tools/
│   │   ├── AmapMapsService.java       # 高德地图工具
│   │   ├── MockWeatherService.java    # 天气服务
│   │   ├── MockOrderService.java      # 订单服务
│   │   ├── MockLiveService.java       # 租房服务
│   │   └── TimeTools.java             # 时间工具
│   └── config/
│       └── Config.java                # 工具函数配置
├── src/main/resources/
│   ├── application.yml                # 应用配置
│   └── logback-spring.xml            # 日志配置
└── pom.xml                           # Maven 配置
```

## 🚀 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- 阿里云 DashScope API Key
- 高德地图 API Key

### 配置说明

#### 1. 克隆项目
```bash
git clone <repository-url>
cd springAI
```

#### 2. 配置 API Keys
编辑 `src/main/resources/application.yml`:

```yaml
spring:
  ai:
    dashscope:
      api-key: your_dashscope_api_key  # 阿里云通义千问 API Key
  amap:
    api-key: your_amap_api_key         # 高德地图 API Key
```

**获取 API Keys**:
- 阿里云 DashScope: [https://dashscope.console.aliyun.com/](https://dashscope.console.aliyun.com/)
- 高德地图: [https://lbs.amap.com/](https://lbs.amap.com/)

#### 3. 启动应用
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

1. **工具优先**: 实时信息、外部系统数据
   - 天气查询 → `amapWeatherFunction`
   - 地址转换 → `geoFunction` / `regeoFunction`
   - 路径规划 → `directionFunction`
   - 周边搜索 → `aroundSearchFunction`
   - 订单查询 → `getOrderFunction`

2. **知识库优先**: 内部知识、事实密集型问答
   - 公司制度、产品信息
   - 技术文档、个人信息

3. **大模型直答**: 闲聊、观点建议、常识解释

### 上下文记忆

- 自动从对话历史中提取实体（城市、地点、坐标等）
- 参数缺失时自动补全或向用户澄清
- 保持参数一致性（温度单位、地点城市等）

### 地址智能处理

- 自动识别输入是地址还是经纬度
- 地址自动转换为坐标（用于路径规划和周边搜索）
- 支持混合输入（地址 + 坐标）

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

## 📮 联系方式

如有问题或建议，欢迎通过以下方式联系：
- 提交 Issue
- 发送邮件

---

⭐ 如果这个项目对你有帮助，请给一个 Star！

