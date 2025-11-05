package com.springai.chat.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

/**
 * 高德地图工具服务
 * 使用 @Tool 注解实现工具调用
 * 提供地理编码、逆地理编码、路径规划、周边搜索、天气查询等功能
 *
 * @author yanwenjie
 */
@Slf4j
@Service
public class AmapMapsService {

    private final RestTemplate restTemplate;

    // 高德地图API基础URL
    private static final String AMAP_BASE_URL = "https://restapi.amap.com/v3";

    @Value("${spring.amap.api-key}")
    private String amapApiKey;

    public AmapMapsService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 判断字符串是否为经纬度格式
     */
    private boolean isCoordinate(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        // 经纬度格式: 数字,数字
        return input.matches("^-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?$");
    }

    /**
     * 将地址转换为经纬度坐标
     */
    private String addressToCoordinate(String address) {
        try {
            String url = String.format("%s/geocode/geo?key=%s&address=%s",
                    AMAP_BASE_URL, amapApiKey, address);

            ResponseEntity<JSONObject> response = restTemplate.getForEntity(url, JSONObject.class);
            JSONObject body = response.getBody();

            if (body != null && "1".equals(body.getString("status"))) {
                JSONArray geocodes = body.getJSONArray("geocodes");
                if (geocodes != null && !geocodes.isEmpty()) {
                    JSONObject first = geocodes.getJSONObject(0);
                    return first.getString("location");
                }
            }
        } catch (Exception e) {
            log.error("地址转坐标失败: {}", address, e);
        }
        return null;
    }

    /**
     * 地理编码 - 将地址转换为经纬度
     */
    @Tool(name = "geoFunction", description = "将地址转换为经纬度坐标。触发条件：用户明确要求'地址转坐标'、'获取XX的坐标'、'XX的经纬度'，或为路径规划、周边搜索等工具提供坐标时自动调用。关键词：地址转坐标、获取坐标、经纬度、坐标查询。返回多个匹配结果，优先使用第一个。")
    public String geoFunction(
            @ToolParam(description = "要转换的地址信息（必需）。可以是具体地址、地标名称、区名+街道名等，例如：'北京市朝阳区天安门广场'、'上海人民广场'、'广州天河城'、'徐汇区漕河泾'。从用户输入中提取地址信息。") String address,
            @ToolParam(description = "城市名称（可选），用于缩小搜索范围，提高准确性。例如：'北京'、'上海'、'广州'、'深圳'。如果用户提到了城市名，务必传入此参数。如果不提供，会在全国范围内搜索，可能返回多个结果。") String city) {
        try {
            String url = String.format("%s/geocode/geo?key=%s&address=%s&city=%s",
                    AMAP_BASE_URL, amapApiKey, address, city != null ? city : "");

            ResponseEntity<JSONObject> response = restTemplate.getForEntity(url, JSONObject.class);
            log.info("地理编码API响应: {}", response.getBody());
            JSONObject body = response.getBody();

            if (body == null || !"1".equals(body.getString("status"))) {
                return "地理编码失败: " + body.getString("info");
            }

            StringBuilder result = new StringBuilder();
            result.append("找到 ").append(body.getString("count")).append(" 个结果: ");

            JSONArray geocodes = body.getJSONArray("geocodes");
            if (geocodes != null && !geocodes.isEmpty()) {
                for (int i = 0; i < geocodes.size(); i++) {
                    JSONObject geocode = geocodes.getJSONObject(i);
                    if (i > 0)
                        result.append("; ");
                    result.append(String.format("地址%d: %s (坐标: %s)",
                            i + 1,
                            geocode.getString("formatted_address"),
                            geocode.getString("location")));
                }
            }

            return result.toString();
        } catch (Exception e) {
            log.error("地理编码调用失败", e);
            return "调用失败: " + e.getMessage();
        }
    }

    @Tool(name = "regeoFunction", description = "将经纬度坐标转换为详细地址。触发条件：用户输入包含经纬度坐标格式（如121.47,31.23、116.397128,39.916527），或询问'坐标是哪里'、'这个坐标'、'经纬度对应的地址'。关键词：坐标转地址、坐标是哪里、经纬度地址、坐标查询位置。输入格式：必须是'经度,纬度'（逗号分隔，无空格），例如：116.397128,39.916527。")
    public String regeoFunction(
            @ToolParam(description = "经纬度坐标（必需）。格式必须为：经度,纬度（逗号分隔，无空格），例如：'116.397128,39.916527'、'121.473701,31.230416'、'121.47,31.23'。从用户输入中提取坐标字符串，确保格式正确。如果格式不正确，提醒用户正确格式。") String location) {
        try {
            String url = String.format("%s/geocode/regeo?key=%s&location=%s",
                    AMAP_BASE_URL, amapApiKey, location);

            ResponseEntity<JSONObject> response = restTemplate.getForEntity(url, JSONObject.class);
            log.info("逆地理编码API响应: {}", response.getBody());
            JSONObject body = response.getBody();

            if (body == null || !"1".equals(body.getString("status"))) {
                return "逆地理编码失败: " + body.getString("info");
            }

            JSONObject regeocode = body.getJSONObject("regeocode");
            if (regeocode != null) {
                return regeocode.getString("formatted_address");
            }

            return "未找到地址信息";
        } catch (Exception e) {
            log.error("逆地理编码调用失败", e);
            return "调用失败: " + e.getMessage();
        }
    }

    /**
     * 路径规划 - 驾车路线规划
     */
    @Tool(name = "directionFunction", description = "规划从起点到终点的驾车路线。触发条件：用户询问'怎么走'、'路线'、'导航'、'路程'、'距离'、'多远'、'耗时'、'多长时间'、'从XX到XX'、'去XX怎么走'、'XX到XX的距离'。关键词：怎么走、路线、导航、路程、距离、多远、耗时、多长时间、路线规划、路径规划。支持输入地址或坐标，系统自动转换。返回：总距离（米）、预计时间（秒）、关键路线步骤。")
    public String directionFunction(
            @ToolParam(description = "起点位置（必需）。可以是地址（如：'北京市天安门'、'上海人民广场'、'人民广场'）或经纬度坐标（如：'116.397128,39.916527'）。从用户输入中提取起点，支持地址或坐标，系统会自动转换。如果用户只说'去XX'或'到XX'，从对话历史中查找最近提到的位置作为起点。") String origin,
            @ToolParam(description = "终点位置（必需）。可以是地址（如：'北京首都机场'、'上海虹桥站'、'天安门'）或经纬度坐标（如：'116.584626,40.081956'）。从用户输入中提取终点，支持地址或坐标，系统会自动转换。如果用户未明确终点，询问用户。") String destination) {
        try {
            // 判断起点是否为地址，如果是则转换为坐标
            String originCoordinate = origin;
            if (!isCoordinate(originCoordinate)) {
                log.info("起点是地址，转换为坐标: {}", originCoordinate);
                originCoordinate = addressToCoordinate(originCoordinate);
                if (originCoordinate == null) {
                    return "无法找到起点坐标: " + origin;
                }
            }

            // 判断终点是否为地址，如果是则转换为坐标
            String destinationCoordinate = destination;
            if (!isCoordinate(destinationCoordinate)) {
                log.info("终点是地址，转换为坐标: {}", destinationCoordinate);
                destinationCoordinate = addressToCoordinate(destinationCoordinate);
                if (destinationCoordinate == null) {
                    return "无法找到终点坐标: " + destination;
                }
            }

            log.info("路径规划: {} -> {}", originCoordinate, destinationCoordinate);

            String url = String.format("%s/direction/driving?key=%s&origin=%s&destination=%s",
                    AMAP_BASE_URL, amapApiKey, originCoordinate, destinationCoordinate);

            ResponseEntity<JSONObject> response = restTemplate.getForEntity(url, JSONObject.class);
            log.info("路径规划API响应: {}", response.getBody());
            JSONObject body = response.getBody();

            if (!"1".equals(body.getString("status"))) {
                return "路径规划失败: " + body.getString("info");
            }

            JSONObject route = body.getJSONObject("route");
            if (route != null) {
                JSONArray paths = route.getJSONArray("paths");
                if (paths != null && paths.size() > 0) {
                    JSONObject firstPath = paths.getJSONObject(0);
                    String distance = firstPath.getString("distance");
                    String duration = firstPath.getString("duration");

                    StringBuilder result = new StringBuilder();
                    result.append(String.format("总距离: %s米, 预计时间: %s秒。", distance, duration));

                    JSONArray steps = firstPath.getJSONArray("steps");
                    if (steps != null && !steps.isEmpty()) {
                        result.append(" 路线: ");
                        for (int i = 0; i < steps.size(); i++) {
                            JSONObject step = steps.getJSONObject(i);
                            if (i > 0) {
                                result.append(" -> ");
                            }
                            result.append(step.getString("instruction"));
                        }
                    }

                    return result.toString();
                }
            }

            return "未找到路径信息";
        } catch (Exception e) {
            log.error("路径规划调用失败", e);
            return "调用失败: " + e.getMessage();
        }
    }

    /**
     * 周边搜索 - POI搜索
     */
    @Tool(name = "aroundSearchFunction", description = "搜索指定位置周边的兴趣点（POI）。触发条件：用户询问'附近有什么'、'周边有什么'、'周围有什么'、'最近的XX'、'XX附近'、'XX旁边有什么'。关键词：附近、周边、周围、最近的、附近有什么、周边XX、周围XX。示例：附近有什么餐厅、人民广场周边有什么、最近的医院在哪里。支持输入地址或坐标，系统自动转换。搜索关键词：餐厅、地铁站、医院、银行、加油站、停车场、超市、咖啡厅、酒店、学校等。默认搜索半径3000米（3公里）。")
    public String aroundSearchFunction(
            @ToolParam(description = "中心点位置（必需）。可以是地址（如：'上海人民广场'、'北京中关村'、'人民广场'）或经纬度坐标（如：'121.473701,31.230416'）。从用户输入或对话历史中提取中心点，支持地址或坐标，系统会自动转换。如果用户未明确位置，从对话历史中查找最近提到的位置。") String location,
            @ToolParam(description = "搜索关键词（必需）。从用户输入中提取关键词，例如：'餐厅'、'地铁站'、'医院'、'银行'、'加油站'、'停车场'、'超市'、'咖啡厅'、'酒店'、'学校'等。可以是单个词或短语。如果用户未明确关键词（如只问'附近有什么'），询问用户要搜索什么类型的POI。") String keywords,
            @ToolParam(description = "搜索半径（可选，单位：米）。默认3000米（3公里）。建议范围：500-5000米。如果用户明确说了范围（如'1公里内'、'500米内'），转换为米（1公里=1000米）并传入。例如：用户说'1公里内'则传入'1000'，用户说'500米'则传入'500'。如果用户未指定，可以不传或传入'3000'。") String radius) {
        try {
            // 判断中心点是否为地址，如果是则转换为坐标
            String locationCoordinate = location;
            if (!isCoordinate(locationCoordinate)) {
                log.info("中心点是地址，转换为坐标: {}", locationCoordinate);
                locationCoordinate = addressToCoordinate(locationCoordinate);
                if (locationCoordinate == null) {
                    return "无法找到中心点坐标: " + location;
                }
            }

            String radiusStr = radius != null && !radius.trim().isEmpty() ? radius : "3000";
            log.info("周边搜索: location={}, keywords={}, radius={}", locationCoordinate, keywords, radiusStr);

            String url = String.format("%s/place/around?key=%s&location=%s&keywords=%s&radius=%s",
                    AMAP_BASE_URL, amapApiKey, locationCoordinate, keywords, radiusStr);

            ResponseEntity<JSONObject> response = restTemplate.getForEntity(url, JSONObject.class);
            log.info("周边搜索API响应: {}", response.getBody());
            JSONObject body = response.getBody();

            if (body == null || !"1".equals(body.getString("status"))) {
                return "周边搜索失败: " + body.getString("info");
            }

            JSONArray pois = body.getJSONArray("pois");
            if (pois != null && !pois.isEmpty()) {
                StringBuilder result = new StringBuilder();
                result.append("找到 ").append(pois.size()).append(" 个结果: ");
                for (int i = 0; i < Math.min(pois.size(), 10); i++) {
                    JSONObject poi = pois.getJSONObject(i);
                    if (i > 0)
                        result.append("; ");
                    result.append(String.format("%s (%s, 距离: %s米)",
                            poi.getString("name"),
                            poi.getString("address"),
                            poi.getString("distance")));
                }
                return result.toString();
            }

            return "未找到相关POI";
        } catch (Exception e) {
            log.error("周边搜索调用失败", e);
            return "调用失败: " + e.getMessage();
        }
    }

    /**
     * 天气查询
     */
    @Tool(name = "amapWeatherFunction", description = "查询指定城市的实时天气信息。触发条件：用户询问包含'天气'、'温度'、'气温'、'风力'、'风向'、'下雨'、'降雨'、'晴天'、'阴天'、'多云'、'湿度'、'天气预报'、'XX天气怎么样'、'XX今天天气'等关键词的问题。关键词：天气、温度、气温、风力、风向、下雨、降雨、晴天、阴天、多云、湿度、天气预报。示例：北京天气、上海今天温度、明天会下雨吗、广州风力多大。优先使用城市名称（北京、上海、广州），不要使用城市编码。返回：城市、天气状况、温度、风向、风力、湿度、发布时间。")
    public String amapWeatherFunction(
            @ToolParam(description = "城市名称（必需，优先使用城市名称，不要使用城市编码）。从用户输入中提取城市名称，例如：'北京'、'上海'、'广州'、'深圳'、'杭州'、'成都'等。如果用户未明确城市，从对话历史中查找最近提到的城市。如果都没有，询问用户要查询哪个城市的天气。不要使用城市编码（如110000、310000），始终使用中文城市名称。") String city) {
        try {
            String url = String.format("%s/weather/weatherInfo?key=%s&city=%s",
                    AMAP_BASE_URL, amapApiKey, city);
            log.info("天气查询URL: {}", url);
            ResponseEntity<JSONObject> response = restTemplate.getForEntity(url, JSONObject.class);
            log.info("天气查询API响应: {}", response.getBody());
            JSONObject body = response.getBody();

            if (body == null || !"1".equals(body.getString("status"))) {
                return "天气查询失败: " + body.getString("info");
            }

            JSONArray lives = body.getJSONArray("lives");
            if (lives != null && !lives.isEmpty()) {
                JSONObject first = lives.getJSONObject(0);
                return String.format("城市: %s, 天气: %s, 温度: %s°C, 风向: %s, 风力: %s级, 湿度: %s%%, 发布时间: %s",
                        first.getString("city"),
                        first.getString("weather"),
                        first.getString("temperature"),
                        first.getString("winddirection"),
                        first.getString("windpower"),
                        first.getString("humidity"),
                        first.getString("reporttime"));
            }

            return "未找到天气信息";
        } catch (Exception e) {
            log.error("天气查询调用失败", e);
            return "调用失败: " + e.getMessage();
        }
    }
}
