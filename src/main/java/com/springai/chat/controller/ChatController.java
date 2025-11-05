package com.springai.chat.controller;

import com.springai.chat.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
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
     * 流式对话接口（SSE格式）
     * 
     * @param query        用户消息内容
     * @param conversantId 用户标识，用于保存对话记忆
     * @return SSE格式的流式响应
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> chatStream(
            @RequestParam String query,
            @RequestParam String conversantId) {
        return chatService.chat(query, conversantId);
    }

    /**
     * POST 方式的对话接口（SSE格式）
     * 如果没有 Accept 头或 Accept 头不匹配，也允许访问
     * 
     * @param request 包含 query 和 conversantId 字段的请求体
     * @return SSE格式的流式响应
     */
    @PostMapping(value = "/chat", produces = { MediaType.TEXT_EVENT_STREAM_VALUE,
            MediaType.ALL_VALUE }, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ChatResponse> chatStreamPost(@RequestBody ChatRequest request) {
        log.info("收到 POST 聊天请求 - 消息: {}, 用户标识: {}",
                request.getQuery(), request.getConversantId());
        return chatService.chat(request.getQuery(), request.getConversantId());

    }

    /**
     * 从ChatResponse中提取文本内容
     */
    private String extractTextFromResponse(ChatResponse response) {
        if (response.getResult() != null && response.getResult().getOutput() != null) {
            var output = response.getResult().getOutput();
            if (output instanceof AssistantMessage) {
                return ((AssistantMessage) output).getText();
            }
        }
        return null;
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
