package com.springai.chat.controller;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.*;

import com.springai.chat.service.SpringAiService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * @program: springAI
 *
 * @description: Spring AI 控制器
 *
 * @author: yanwenjie
 *
 * @create: 2025-10-28 09:56
 **/
@Slf4j
@RestController
@RequestMapping("/api")
public class SpringAiController {

    private final SpringAiService springAiService;

    public SpringAiController(SpringAiService springAiService) {
        this.springAiService = springAiService;
    }

    @GetMapping("/simple/chat")
    public Flux<ChatResponse> simpleChat(HttpServletResponse response, String query, String conversantId) {

        return springAiService.simpleChat(response, query, conversantId);
    }

    @GetMapping("/simple/image")
    public String simpleImage(HttpServletResponse response, String query) {

        return springAiService.simpleImage(response, query);
    }

}
