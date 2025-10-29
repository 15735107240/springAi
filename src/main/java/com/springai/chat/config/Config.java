package com.springai.chat.config;

import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import com.springai.chat.tools.AmapMapsService;
import com.springai.chat.tools.MockLiveService;
import com.springai.chat.tools.MockOrderService;
import com.springai.chat.tools.MockWeatherService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class Config {

    @Bean
    @Description("获取本地的天气") // function description
    public Function<MockWeatherService.Request, MockWeatherService.Response> weatherFunction1() {

        return new MockWeatherService();
    }

    @Bean
    @Description("根据用户编号和订单编号查询订单信息") // function的描述
    public Function<MockOrderService.Request, MockOrderService.Response> getOrderFunction(
            MockOrderService mockOrderService) {
        return mockOrderService::getOrder;
    }

    @Bean
    @Description("查询用户租房信息") // function的描述
    public Function<MockLiveService.Request, MockLiveService.Response> getLiveRoomFunction(
            MockLiveService mockLiveService) {
        return mockLiveService::getRoomList;
    }

    @Bean
    @Description("地理编码：将地址转换为经纬度坐标")
    public Function<AmapMapsService.GeoRequest, AmapMapsService.GeoResponse> geoFunction(
            AmapMapsService amapMapsService) {
        return amapMapsService.geoFunction();
    }

    @Bean
    @Description("逆地理编码：将经纬度坐标转换为地址信息")
    public Function<AmapMapsService.RegeoRequest, AmapMapsService.RegeoResponse> regeoFunction(
            AmapMapsService amapMapsService) {
        return amapMapsService.regeoFunction();
    }

    @Bean
    @Description("路径规划：规划从起点到终点的驾车路线")
    public Function<AmapMapsService.DirectionRequest, AmapMapsService.DirectionResponse> directionFunction(
            AmapMapsService amapMapsService) {
        return amapMapsService.directionFunction();
    }

    @Bean
    @Description("周边搜索：搜索指定位置周边的兴趣点")
    public Function<AmapMapsService.AroundSearchRequest, AmapMapsService.AroundSearchResponse> aroundSearchFunction(
            AmapMapsService amapMapsService) {
        return amapMapsService.aroundSearchFunction();
    }

    @Bean
    @Description("天气查询：查询指定城市的实时天气信息")
    public Function<AmapMapsService.WeatherRequest, AmapMapsService.WeatherResponse> amapWeatherFunction(
            AmapMapsService amapMapsService) {
        return amapMapsService.weatherFunction();
    }
}
