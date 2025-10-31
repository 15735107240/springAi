package com.springai.chat.controller;

import com.springai.chat.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 聊天控制器
 * 提供 HTTP 接口进行对话，支持对话记忆
 * 
 * @author yanwenjie
 */
@Slf4j
@RestController
@RequestMapping("/api/simple")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 流式对话接口
     * @param query 用户消息内容
     * @param conversantId 用户标识，用于保存对话记忆
     * @return 流式响应 Flux<ChatResponse>
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> chatStream(
            @RequestParam String query,
            @RequestParam String conversantId) {
        log.info("收到聊天请求 - 消息: {}, 用户标识: {}", query, conversantId);
        return chatService.chat(query, conversantId);
    }

    /**
     * 普通对话接口（非流式）
     * @param query 用户消息内容
     * @param conversantId 用户标识，用于保存对话记忆
     * @return 流式响应
     */
    @GetMapping("/chat")
    public Flux<ChatResponse> chat(
            @RequestParam String query,
            @RequestParam String conversantId) {
        log.info("收到聊天请求 - 消息: {}, 用户标识: {}", query, conversantId);
        return chatService.chat(query, conversantId);
    }

    /**
     * POST 方式的对话接口
     * @param request 包含 query 和 conversantId 字段的请求体
     * @return 流式响应
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> chatStreamPost(@RequestBody ChatRequest request) {
        log.info("收到 POST 聊天请求 - 消息: {}, 用户标识: {}", 
                request.getQuery(), request.getConversantId());
        return chatService.chat(request.getQuery(), request.getConversantId());
    }

    /**
     * 请求体类
     */
    public static class ChatRequest {
        private String query;
        private String conversantId;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getConversantId() {
            return conversantId;
        }

        public void setConversantId(String conversantId) {
            this.conversantId = conversantId;
        }
    }
}

