package com.springai.chat.controller;

import org.springframework.web.bind.annotation.*;
import com.springai.chat.tools.AmapMapsService;
import lombok.extern.slf4j.Slf4j;

/**
 * 高德地图测试控制器
 * 用于测试高德地图MCP集成功能
 */
@Slf4j
@RestController
@RequestMapping("/api/amap")
public class AmapTestController {

    private final AmapMapsService amapMapsService;

    public AmapTestController(AmapMapsService amapMapsService) {
        this.amapMapsService = amapMapsService;
    }

    /**
     * 测试地理编码功能
     */
    @GetMapping("/geo")
    public AmapMapsService.GeoResponse testGeo(@RequestParam String address, 
                                               @RequestParam(required = false) String city) {
        log.info("测试地理编码: address={}, city={}", address, city);
        AmapMapsService.GeoRequest request = new AmapMapsService.GeoRequest(address, city);
        return amapMapsService.geoFunction().apply(request);
    }

    /**
     * 测试逆地理编码功能
     */
    @GetMapping("/regeo")
    public AmapMapsService.RegeoResponse testRegeo(@RequestParam String location) {
        log.info("测试逆地理编码: location={}", location);
        AmapMapsService.RegeoRequest request = new AmapMapsService.RegeoRequest(location);
        return amapMapsService.regeoFunction().apply(request);
    }

    /**
     * 测试路径规划功能
     */
    @GetMapping("/direction")
    public AmapMapsService.DirectionResponse testDirection(@RequestParam String origin,
                                                           @RequestParam String destination,
                                                           @RequestParam(defaultValue = "1") String type) {
        log.info("测试路径规划: origin={}, destination={}, type={}", origin, destination, type);
        AmapMapsService.DirectionRequest request = new AmapMapsService.DirectionRequest(origin, destination, type);
        return amapMapsService.directionFunction().apply(request);
    }

    /**
     * 测试周边搜索功能
     */
    @GetMapping("/around")
    public AmapMapsService.AroundSearchResponse testAroundSearch(@RequestParam String location,
                                                                 @RequestParam String keywords,
                                                                 @RequestParam(defaultValue = "1000") String radius) {
        log.info("测试周边搜索: location={}, keywords={}, radius={}", location, keywords, radius);
        AmapMapsService.AroundSearchRequest request = new AmapMapsService.AroundSearchRequest(location, keywords, radius);
        return amapMapsService.aroundSearchFunction().apply(request);
    }

    /**
     * 测试天气查询功能
     */
    @GetMapping("/weather")
    public AmapMapsService.WeatherResponse testWeather(@RequestParam String city) {
        log.info("测试天气查询: city={}", city);
        AmapMapsService.WeatherRequest request = new AmapMapsService.WeatherRequest(city);
        return amapMapsService.weatherFunction().apply(request);
    }
}
