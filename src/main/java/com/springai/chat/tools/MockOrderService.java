package com.springai.chat.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 订单查询服务工具
 * 使用 @Tool 注解实现工具调用
 * 
 * @author yanwenjie
 */
@Slf4j
@Service
public class MockOrderService {

    /**
     * 根据用户编号和订单编号查询订单信息
     * 
     * @param orderId 订单编号, 比如1001***
     * @param userId 用户编号, 比如2001***
     * @return 订单信息描述
     */
    @Tool(name = "getOrderFunction", description = "根据用户编号和订单编号查询订单详细信息。当用户询问'我的订单'、'订单查询'、'订单信息'、'订单详情'等问题时使用。需要用户提供用户编号和订单编号。")
    public String getOrderFunction(
            @ToolParam(description = "订单编号，从用户输入中提取，例如：'100123456'、'1001***'、'ORD20240101001'等格式。如果用户只提供了部分订单号，使用提供的部分") String orderId,
            @ToolParam(description = "用户编号，从用户输入或对话历史中提取，例如：'200123456'、'2001***'、'USER001'等格式。如果对话历史中有提到过用户编号，可以复用") String userId) {
        String productName = "尤尼克斯羽毛球拍";
        String result = String.format("%s的订单编号为%s, 购买的商品为: %s", userId, orderId, productName);
        log.info("执行订单查询 - 用户编号: {}, 订单编号: {}", userId, orderId);
        return result;
    }
}
