# 高德地图MCP集成说明

## 概述
本项目已成功集成高德地图MCP服务，使您的Spring AI应用能够通过simpleChat接口调用高德地图的各种功能。

## 功能特性

### 1. 地理编码 (geoFunction)
- **功能**: 将地址转换为经纬度坐标
- **使用场景**: 用户询问"北京天安门在哪里"时，可以获取精确坐标
- **API调用**: `geoFunction(address, city)`

### 2. 逆地理编码 (regeoFunction)  
- **功能**: 将经纬度坐标转换为地址信息
- **使用场景**: 用户提供坐标时，可以获取对应的地址描述
- **API调用**: `regeoFunction(location)`

### 3. 路径规划 (directionFunction)
- **功能**: 规划从起点到终点的驾车路线
- **使用场景**: 用户询问"从上海到北京怎么走"时，提供详细路线
- **API调用**: `directionFunction(origin, destination, type)`

### 4. 周边搜索 (aroundSearchFunction)
- **功能**: 搜索指定位置周边的兴趣点
- **使用场景**: 用户询问"附近有什么餐厅"时，搜索周边POI
- **API调用**: `aroundSearchFunction(location, keywords, radius)`

### 5. 天气查询 (amapWeatherFunction)
- **功能**: 查询指定城市的实时天气信息
- **使用场景**: 用户询问"今天天气怎么样"时，获取详细天气信息
- **API调用**: `amapWeatherFunction(city)`

## 配置说明

### 1. API Key配置
在 `application.yml` 中配置您的高德地图API Key：
```yaml
spring:
  amap:
    api-key: your_amap_api_key_here
```

### 2. 获取高德地图API Key
1. 访问 [高德开放平台](https://lbs.amap.com/)
2. 注册并登录账号
3. 创建应用获取API Key
4. 将API Key配置到application.yml中

## 使用方式

### 通过simpleChat接口
现在您可以通过 `/api/simple/chat` 接口与AI对话，AI会自动识别地图相关需求并调用相应的高德地图功能：

**示例对话**:
- "上海到北京的路线怎么走？" → 自动调用路径规划功能
- "北京天安门在哪里？" → 自动调用地理编码功能  
- "121.473667,31.230525这个坐标在哪里？" → 自动调用逆地理编码功能
- "上海人民广场附近有什么餐厅？" → 自动调用周边搜索功能
- "今天北京天气怎么样？" → 自动调用天气查询功能

### 直接API测试
您也可以通过测试接口直接调用高德地图功能：

```bash
# 地理编码测试
GET /api/amap/geo?address=北京市天安门&city=北京

# 逆地理编码测试  
GET /api/amap/regeo?location=116.407387,39.904179

# 路径规划测试
GET /api/amap/direction?origin=121.473667,31.230525&destination=116.407387,39.904179

# 周边搜索测试
GET /api/amap/around?location=121.473667,31.230525&keywords=餐厅&radius=1000

# 天气查询测试
GET /api/amap/weather?city=北京
```

## 技术实现

### 核心组件
1. **AmapMapsService**: 高德地图服务工具类
2. **Config.java**: Spring AI函数注册配置
3. **SpringAiServiceImpl**: 集成高德地图工具到ChatClient
4. **AmapTestController**: 测试控制器

### 集成流程
1. 创建高德地图工具类，实现各种地图功能
2. 在Config.java中注册为Spring AI函数
3. 在SpringAiServiceImpl中将函数添加到ChatClient
4. 更新系统提示词，让AI知道如何使用地图功能

## 注意事项

1. **API Key安全**: 请妥善保管您的高德地图API Key，不要提交到版本控制系统
2. **调用限制**: 注意高德地图API的调用频率限制
3. **错误处理**: 当前实现包含基本的错误处理，实际使用时建议完善异常处理逻辑
4. **响应解析**: 当前返回的是简化响应，实际使用时需要解析高德地图API的完整JSON响应

## 扩展功能

您可以基于现有框架轻松扩展更多高德地图功能：
- 公交路线规划
- 步行路线规划  
- 实时路况查询
- 行政区划查询
- IP定位查询
- 静态地图服务

只需要在AmapMapsService中添加新的方法，并在Config.java中注册即可。

## 天气查询功能详细说明

### 功能特性
- **实时天气**: 获取最新的天气信息
- **详细信息**: 包含温度、天气状况、风向、风力、湿度等
- **多城市支持**: 支持全国主要城市查询
- **adcode支持**: 支持使用城市编码查询

### 返回信息
- 城市名称
- 天气状况（晴、多云、雨等）
- 温度（摄氏度）
- 风向和风力
- 湿度百分比
- 数据发布时间

### 使用示例
用户询问"今天北京天气怎么样？"时，系统会：
1. 识别这是天气查询需求
2. 调用amapWeatherFunction
3. 传入城市参数"北京"
4. 返回详细的天气信息
5. 以友好的方式展示给用户
