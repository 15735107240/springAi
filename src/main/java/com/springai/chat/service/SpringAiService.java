package com.springai.chat.service;

import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

public interface SpringAiService {

    Flux<String> simpleChat(HttpServletResponse response, String query, String conversantId);

    String simpleImage(HttpServletResponse response, String query);
}
