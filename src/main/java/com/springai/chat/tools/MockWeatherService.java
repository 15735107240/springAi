package com.springai.chat.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 天气查询服务工具（Mock）
 * 使用 @Tool 注解实现工具调用
 * 
 * @author yanwenjie
 */
@Slf4j
@Service
public class MockWeatherService {

    /**
     * 获取本地天气信息
     * 
     * @param location 位置信息
     * @param unit 温度单位，C 表示摄氏度，F 表示华氏度
     * @return 天气信息描述
     */
    @Tool(name = "weatherFunction1", description = "获取指定位置的天气信息（Mock服务，返回模拟数据）。当amapWeatherFunction不可用时作为备选方案使用。通常优先使用amapWeatherFunction获取真实天气数据。")
    public String weatherFunction1(
            @ToolParam(description = "位置信息，例如：'北京'、'上海'、'广州'等城市名称") String location,
            @ToolParam(description = "温度单位，'C'表示摄氏度（默认），'F'表示华氏度。从用户问题中提取，如果用户未指定，默认使用'C'") String unit) {
        log.info("执行天气查询 - 位置: {}, 单位: {}", location, unit);
        // Mock 数据，返回固定温度
        return String.format("位置: %s, 温度: 30.0%s", location, unit != null ? unit : "C");
    }
}
