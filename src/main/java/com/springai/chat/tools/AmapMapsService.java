package com.springai.chat.tools;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;

/**
 * 高德地图MCP服务工具类
 * 提供地理编码、逆地理编码、路径规划、周边搜索等功能
 */
@Slf4j
@Component
public class AmapMapsService {

    private final RestTemplate restTemplate;

    // 高德地图API基础URL
    private static final String AMAP_BASE_URL = "https://restapi.amap.com/v3";
    private static final String ERROR_MESSAGE_PREFIX = "调用失败: ";

    @Value("${spring.amap.api-key}")
    private String amapApiKey;

    public AmapMapsService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 地理编码 - 将地址转换为经纬度
     */
    public static class GeoRequest {
        public String address;
        public String city;

        @JsonCreator
        public GeoRequest(@JsonProperty("address") String address, @JsonProperty("city") String city) {
            this.address = address;
            this.city = city;
        }
    }

    public static class GeoResponse {
        public String status;
        public String info;
        public String count; // 返回结果的数目
        public String locations; // 所有地址的详细信息
    }

    /**
     * 逆地理编码 - 将经纬度转换为地址
     */
    public static class RegeoRequest {
        public String location; // 格式: "经度,纬度"

        @JsonCreator
        public RegeoRequest(@JsonProperty("location") String location) {
            this.location = location;
        }
    }

    public static class RegeoResponse {
        public String status;
        public String info;
        public String formatted_address;
    }

    /**
     * 路径规划请求
     */
    public static class DirectionRequest {
        public String origin; // 起点（地址或经纬度）
        public String destination; // 终点（地址或经纬度）
        public String type; // 1-驾车, 2-公交, 3-步行

        @JsonCreator
        public DirectionRequest(@JsonProperty("origin") String origin,
                @JsonProperty("destination") String destination,
                @JsonProperty("type") String type) {
            this.origin = origin;
            this.destination = destination;
            this.type = type;
        }
    }

    public static class DirectionResponse {
        public String status;
        public String info;
        public String distance; // 距离(米)
        public String duration; // 时间(秒)
        public String route; // 路线描述
    }

    /**
     * 周边搜索请求
     */
    public static class AroundSearchRequest {
        public String location; // 中心点经纬度
        public String keywords; // 搜索关键词
        public String radius; // 搜索半径(米)

        @JsonCreator
        public AroundSearchRequest(@JsonProperty("location") String location,
                @JsonProperty("keywords") String keywords,
                @JsonProperty("radius") String radius) {
            this.location = location;
            this.keywords = keywords;
            this.radius = radius;
        }
    }

    public static class AroundSearchResponse {
        public String status;
        public String info;
        public String pois; // POI信息
    }

    /**
     * 天气查询请求
     */
    public static class WeatherRequest {
        public String city; // 城市名称或adcode

        @JsonCreator
        public WeatherRequest(@JsonProperty("city") String city) {
            this.city = city;
        }
    }

    public static class WeatherResponse {
        public String status;
        public String info;
        public String city; // 城市名称
        public String weather; // 天气状况
        public String temperature; // 温度
        public String windDirection; // 风向
        public String windPower; // 风力
        public String humidity; // 湿度
        public String reportTime; // 发布时间
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
     * 地理编码功能
     */
    public Function<GeoRequest, GeoResponse> geoFunction() {
        return request -> {
            try {
                String url = String.format("%s/geocode/geo?key=%s&address=%s&city=%s",
                        AMAP_BASE_URL, amapApiKey, request.address, request.city);

                ResponseEntity<JSONObject> response = restTemplate.getForEntity(url, JSONObject.class);
                log.info("地理编码API响应: {}", response.getBody());
                JSONObject body = response.getBody();
                
                GeoResponse geoResponse = new GeoResponse();
                geoResponse.status = body.getString("status");
                geoResponse.info = body.getString("info");
                
                if ("1".equals(geoResponse.status)) {
                    String countStr = body.getString("count");
                    geoResponse.count = countStr;
                    
                    // 解析所有返回的地址信息
                    JSONArray geocodes = body.getJSONArray("geocodes");
                    if (geocodes != null && !geocodes.isEmpty()) {
                        StringBuilder locationsBuilder = new StringBuilder();
                        for (int i = 0; i < geocodes.size(); i++) {
                            JSONObject geocode = geocodes.getJSONObject(i);
                            if (locationsBuilder.length() > 0) {
                                locationsBuilder.append("; ");
                            }
                            locationsBuilder.append(String.format("地址%d: %s (坐标: %s, 详细: %s)",
                                    i + 1,
                                    geocode.getString("formatted_address"),
                                    geocode.getString("location"),
                                    geocode.getString("level")));
                        }
                        geoResponse.locations = locationsBuilder.toString();
                    }
                }
                
                return geoResponse;
            } catch (Exception e) {
                log.error("地理编码调用失败", e);
                GeoResponse errorResponse = new GeoResponse();
                errorResponse.status = "0";
                errorResponse.info = ERROR_MESSAGE_PREFIX + e.getMessage();
                return errorResponse;
            }
        };
    }

    /**
     * 逆地理编码功能
     */
    public Function<RegeoRequest, RegeoResponse> regeoFunction() {
        return request -> {
            try {
                String url = String.format("%s/geocode/regeo?key=%s&location=%s",
                        AMAP_BASE_URL, amapApiKey, request.location);

                ResponseEntity<JSONObject> response = restTemplate.getForEntity(url, JSONObject.class);
                log.info("逆地理编码API响应: {}", response.getBody());
                JSONObject body = response.getBody();
                // 这里简化处理，实际应该解析JSON响应
                RegeoResponse regeoResponse = new RegeoResponse();
                regeoResponse.status = body.getString("status");
                regeoResponse.info = body.getString("info");
                JSONObject regeocode = body.getJSONObject("regeocode");
                regeoResponse.formatted_address =regeocode.getString("formatted_address");
                return regeoResponse;
            } catch (Exception e) {
                log.error("逆地理编码调用失败", e);
                RegeoResponse errorResponse = new RegeoResponse();
                errorResponse.status = "0";
                errorResponse.info = ERROR_MESSAGE_PREFIX + e.getMessage();
                return errorResponse;
            }
        };
    }

    /**
     * 路径规划功能
     */
    public Function<DirectionRequest, DirectionResponse> directionFunction() {
        return request -> {
            try {
                // 判断起点是否为地址，如果是则转换为坐标
                String originCoordinate = request.origin;
                if (!isCoordinate(originCoordinate)) {
                    log.info("起点是地址，转换为坐标: {}", originCoordinate);
                    originCoordinate = addressToCoordinate(originCoordinate);
                    if (originCoordinate == null) {
                        DirectionResponse errorResponse = new DirectionResponse();
                        errorResponse.status = "0";
                        errorResponse.info = "无法找到起点坐标: " + request.origin;
                        return errorResponse;
                    }
                }
                
                // 判断终点是否为地址，如果是则转换为坐标
                String destinationCoordinate = request.destination;
                if (!isCoordinate(destinationCoordinate)) {
                    log.info("终点是地址，转换为坐标: {}", destinationCoordinate);
                    destinationCoordinate = addressToCoordinate(destinationCoordinate);
                    if (destinationCoordinate == null) {
                        DirectionResponse errorResponse = new DirectionResponse();
                        errorResponse.status = "0";
                        errorResponse.info = "无法找到终点坐标: " + request.destination;
                        return errorResponse;
                    }
                }
                
                log.info("路径规划: {} -> {}", originCoordinate, destinationCoordinate);
                
                String url = String.format("%s/direction/driving?key=%s&origin=%s&destination=%s",
                        AMAP_BASE_URL, amapApiKey, originCoordinate, destinationCoordinate);

                ResponseEntity<JSONObject> response = restTemplate.getForEntity(url, JSONObject.class);
                log.info("路径规划API响应: {}", response.getBody());
                JSONObject body = response.getBody();
                
                DirectionResponse directionResponse = new DirectionResponse();
                directionResponse.status = body.getString("status");
                directionResponse.info = body.getString("info");
                
                if ("1".equals(directionResponse.status)) {
                    JSONObject route = body.getJSONObject("route");
                    if (route != null) {
                        // 获取第一条路径
                        JSONArray paths = route.getJSONArray("paths");
                        if (paths != null && paths.size() > 0) {
                            JSONObject firstPath = paths.getJSONObject(0);
                            directionResponse.distance = firstPath.getString("distance");
                            directionResponse.duration = firstPath.getString("duration");
                            
                            // 解析路径信息
                            StringBuilder routeBuilder = new StringBuilder();
                            routeBuilder.append(String.format("总距离: %s米, 预计时间: %s秒。", 
                                    directionResponse.distance, directionResponse.duration));
                            
                            JSONArray steps = firstPath.getJSONArray("steps");
                            if (steps != null && !steps.isEmpty()) {
                                routeBuilder.append(" 路线: ");
                                for (int i = 0; i < Math.min(steps.size(), 5); i++) {
                                    JSONObject step = steps.getJSONObject(i);
                                    if (i > 0) routeBuilder.append(" -> ");
                                    routeBuilder.append(step.getString("instruction"));
                                }
                            }
                            directionResponse.route = routeBuilder.toString();
                        }
                    }
                }
                
                return directionResponse;
            } catch (Exception e) {
                log.error("路径规划调用失败", e);
                DirectionResponse errorResponse = new DirectionResponse();
                errorResponse.status = "0";
                errorResponse.info = ERROR_MESSAGE_PREFIX + e.getMessage();
                return errorResponse;
            }
        };
    }

    /**
     * 周边搜索功能
     */
    public Function<AroundSearchRequest, AroundSearchResponse> aroundSearchFunction() {
        return request -> {
            try {
                // 判断中心点是否为地址，如果是则转换为坐标
                String locationCoordinate = request.location;
                if (!isCoordinate(locationCoordinate)) {
                    log.info("中心点是地址，转换为坐标: {}", locationCoordinate);
                    locationCoordinate = addressToCoordinate(locationCoordinate);
                    if (locationCoordinate == null) {
                        AroundSearchResponse errorResponse = new AroundSearchResponse();
                        errorResponse.status = "0";
                        errorResponse.info = "无法找到中心点坐标: " + request.location;
                        return errorResponse;
                    }
                }
                
                log.info("周边搜索: location={}, keywords={}", locationCoordinate, request.keywords);
                
                String url = String.format("%s/place/around?key=%s&location=%s&keywords=%s&radius=%s",
                        AMAP_BASE_URL, amapApiKey, locationCoordinate, request.keywords, request.radius);

                ResponseEntity<JSONObject> response = restTemplate.getForEntity(url, JSONObject.class);
                log.info("周边搜索API响应: {}", response.getBody());
                JSONObject body = response.getBody();
                
                AroundSearchResponse searchResponse = new AroundSearchResponse();
                searchResponse.status = body.getString("status");
                searchResponse.info = body.getString("info");
                
                if ("1".equals(searchResponse.status)) {
                    JSONArray pois = body.getJSONArray("pois");
                    if (pois != null && !pois.isEmpty()) {
                        StringBuilder poisBuilder = new StringBuilder();
                        for (int i = 0; i < Math.min(pois.size(), 10); i++) {
                            JSONObject poi = pois.getJSONObject(i);
                            if (poisBuilder.length() > 0) {
                                poisBuilder.append("; ");
                            }
                            poisBuilder.append(String.format("%s (%s, 距离: %s米)", 
                                    poi.getString("name"),
                                    poi.getString("address"),
                                    poi.getString("distance")));
                        }
                        searchResponse.pois = poisBuilder.toString();
                    }
                }
                
                return searchResponse;
            } catch (Exception e) {
                log.error("周边搜索调用失败", e);
                AroundSearchResponse errorResponse = new AroundSearchResponse();
                errorResponse.status = "0";
                errorResponse.info = ERROR_MESSAGE_PREFIX + e.getMessage();
                return errorResponse;
            }
        };
    }

    /**
     * 天气查询功能
     */
    public Function<WeatherRequest, WeatherResponse> weatherFunction() {
        return request -> {
            try {
                String url = String.format("%s/weather/weatherInfo?key=%s&city=%s",
                        AMAP_BASE_URL, amapApiKey, request.city);

                ResponseEntity<JSONObject> response = restTemplate.getForEntity(url, JSONObject.class);
                log.info("天气查询API响应: {}", response.getBody());
                JSONObject body = response.getBody();
                
                WeatherResponse weatherResponse = new WeatherResponse();
                weatherResponse.status = body.getString("status");
                weatherResponse.info = body.getString("info");
                
                if ("1".equals(weatherResponse.status)) {
                    JSONArray lives = body.getJSONArray("lives");
                    if (lives != null && !lives.isEmpty()) {
                        JSONObject first = lives.getJSONObject(0);
                        weatherResponse.city = first.getString("city");
                        weatherResponse.weather = first.getString("weather");
                        weatherResponse.temperature = first.getString("temperature");
                        weatherResponse.windDirection = first.getString("winddirection");
                        weatherResponse.windPower = first.getString("windpower");
                        weatherResponse.humidity = first.getString("humidity");
                        weatherResponse.reportTime = first.getString("reporttime");
                    }
                }
                
                return weatherResponse;
            } catch (Exception e) {
                log.error("天气查询调用失败", e);
                WeatherResponse errorResponse = new WeatherResponse();
                errorResponse.status = "0";
                errorResponse.info = ERROR_MESSAGE_PREFIX + e.getMessage();
                return errorResponse;
            }
        };
    }
}
