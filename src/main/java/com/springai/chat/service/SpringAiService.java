package com.springai.chat.service;

import org.springframework.ai.chat.model.ChatResponse;

import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

public interface SpringAiService {

    Flux<ChatResponse> simpleChat(HttpServletResponse response, String query, String conversantId);

    String simpleImage(HttpServletResponse response, String query);
}
