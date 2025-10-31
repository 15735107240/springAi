package com.springai.chat.service;

import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * 聊天服务接口
 * 提供基本的对话功能，支持对话记忆
 * 
 * @author yanwenjie
 */
public interface ChatService {

    /**
     * 流式对话方法
     * @param query 用户消息内容
     * @param conversantId 用户标识，用于保存对话记忆
     * @return 流式响应 Flux<ChatResponse>
     */
    Flux<ChatResponse> chat(String query, String conversantId);
}

