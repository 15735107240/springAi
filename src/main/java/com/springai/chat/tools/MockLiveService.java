package com.springai.chat.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 租房信息服务工具
 * 使用 @Tool 注解实现工具调用
 * 
 * @author yanwenjie
 */
@Slf4j
@Service
public class MockLiveService {

    /**
     * 查询用户租房信息
     * 
     * @param roomType 租房的类型, 比如合租、整租、一居、二居
     * @param money 租房预算范围, 比如3000到4000
     * @param address 租房的地址, 比如徐汇区漕河泾
     * @return 房源匹配结果描述
     */
    @Tool(name = "getLiveRoomFunction", description = "根据租房需求匹配房源信息。触发条件：用户询问包含'租房'、'找房'、'房源'、'租房信息'、'我要租房'、'想租房'、'找房子'等关键词的问题。关键词：租房、找房、房源、租房信息、我要租房、想租房、找房子。需要三个必需参数：1) roomType（租房类型）：合租、整租、一居、一居室、二居、二居室、三居室、单间等，从用户描述中提取；2) money（预算范围）：从用户输入提取，格式可以是'3000到4000'、'3000-4000'、'5000以下'、'8000以上'或单个数字；3) address（租房地址）：区名、街道名或具体地址，如徐汇区漕河泾、北京朝阳区、上海浦东新区。如果用户未提供完整信息，必须一次性询问所有缺失参数，不要分多次询问。")
    public String getLiveRoomFunction(
            @ToolParam(description = "租房类型，例如：'合租'、'整租'、'一居'、'一居室'、'二居'、'二居室'、'三居室'、'单间'等。从用户描述中提取，如果用户未明确说明，可询问") String roomType,
            @ToolParam(description = "租房预算范围，例如：'3000到4000'、'3000-4000'、'5000以下'、'8000以上'等。从用户描述中提取数字范围，格式可以是'X到Y'、'X-Y'或单个数字") String money,
            @ToolParam(description = "租房地址，例如：'徐汇区漕河泾'、'北京朝阳区'、'上海浦东新区'等。从用户描述中提取，可以是区名、街道名或具体地址") String address) {
        String result = String.format("您的需求为%s, 预算范围是%s，居住地址是%s,现在为您匹配合适的房源", 
                roomType, money, address);
        log.info("执行租房信息查询 - 类型: {}, 预算: {}, 地址: {}", roomType, money, address);
        return result;
    }
}
